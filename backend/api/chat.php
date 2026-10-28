<?php
/**
 * TheStudent - Student Chat API (v2.0.0)
 * ======================================
 *
 * Endpoint: /api/chat.php?action=<action>
 *
 * Design: the server is a **relay and a presence board — never a message
 * archive**. Chat history lives only in the student's own device database
 * (SQLite / Room). A message sits in `chat_relay` only until the recipient's
 * app fetches it, and then the row is deleted. That keeps the cloud footprint
 * tiny and means a database dump contains almost no conversation content.
 *
 * Actions
 * -------
 *   GET  threads      Student directory: full name, avatar, presence, typing
 *                     flag, published encryption key. No message content.
 *   GET  inbox        Fetch (and immediately delete) messages + read receipts
 *                     waiting for the caller.
 *   GET  profile      ?peer_id=N  Public student profile for the chat profile
 *                     screen: education fields + presence. Never e-mail, phone,
 *                     role or anything else that identifies the account.
 *   POST send         {peer_id, body}          body is E2EE ciphertext
 *   POST read         {peer_id, up_to_server_id}  read receipt for the sender
 *   POST delete_message  {peer_id, server_id}  delete one of my messages for
 *                     both sides (relayed as a 'delete' envelope)
 *   POST typing       {peer_id, is_typing?}
 *   POST register_device  {fcm_token, platform}  Push token for this install
 *                     (optional: without a service-account key nothing is sent
 *                     and the app falls back to polling).
 *   POST unregister_device  Drop this account's push tokens (sign-out).
 *   POST ack_safety   First-visit chat safety notice acknowledgement.
 *   POST heartbeat    Presence ping (every ~2 s from the app).
 *   POST set_key      {public_key}  publish the E2EE identity public key.
 *   POST upload_attachment  multipart {peer_id, file}  Encrypted file blob
 *                     (already AES-GCM sealed by the sender's phone).
 *   GET  attachment   ?id=…       Stream one blob to its sender or recipient;
 *                     the recipient's fetch deletes it.
 *   GET  version      Deploy fingerprint.
 *
 * Security properties
 * -------------------
 *  • Bearer token only (never a query parameter); tokens are stored as SHA-256
 *    digests by auth.php, so a database dump cannot be replayed.
 *  • Message bodies are encrypted on the sender's phone (ECDH P-256 + AES-256-
 *    GCM). The server only ever handles opaque ciphertext and never the keys.
 *  • Every query is parameterised; peer ids are validated as integers.
 *  • Per-user send throttle, body length cap, control-character stripping.
 *  • `is_active` / `is_email_verified` are enforced on every call.
 *  • Responses are `Cache-Control: no-store`; relay rows expire after 7 days.
 *  • Privacy: only `full_name`, `profile_picture` and coarse presence leave the
 *    server. E-mail, phone, institution and role never do.
 *
 * No new configuration, secrets or environment variables are required.
 */

declare(strict_types=1);

require_once __DIR__ . '/../error_handlers.php';
ts_install_error_handlers();

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/_avatar.php';   // ts_avatar_public_url()

// ---------------------------------------------------------------------------
// Tunables
// ---------------------------------------------------------------------------
const TS_CHAT_API_VERSION          = '2.8.0';
const TS_CHAT_HELP_TTL_HOURS       = 72;   // a pending request nobody answers fades away
const TS_CHAT_HELP_MIN_CLOSE_SEC   = 600;  // accept -> complete cannot happen instantly
const TS_CHAT_HELP_MAX_PER_DAY     = 5;    // new requests per seeker per day
const TS_CHAT_RATING_MAX_TAGS      = 3;    // tags a rating may carry
const TS_CHAT_RATING_MAX_NOTE      = 40;   // characters in the private note
const TS_CHAT_RATING_EDIT_SEC      = 86400; // a rating can be corrected for 24 h
const TS_CHAT_RATING_TALK_MIN_EACH = 20;   // "just talking" needs 20 messages each way
const TS_CHAT_ONLINE_WINDOW_SEC    = 10;    // online = seen in the last N s (2 s pings)
const TS_CHAT_TYPING_WINDOW_SEC    = 6;     // typing indicator lifetime
const TS_CHAT_MAX_BODY_CHARS       = 6000;  // ciphertext is base64 (~1.4x plain text)
const TS_CHAT_MAX_MESSAGES_PER_MIN = 30;    // per student
const TS_CHAT_MAX_BODY_BYTES       = 16384;
const TS_CHAT_INBOX_LIMIT          = 200;   // rows handed out per fetch
const TS_CHAT_RELAY_TTL_DAYS       = 7;     // undelivered messages expire
const TS_CHAT_MAX_ATTACHMENT_BYTES = 27262976;  // 26 MB encrypted blob (25 MB file)
const TS_CHAT_ATTACHMENT_DIR       = 'uploads/chat/';  // encrypted blobs only
const TS_CHAT_MAX_UPLOADS_PER_MIN  = 12;    // per student
/** Push (FCM) is optional: without a service-account key the app still polls. */
const TS_CHAT_FCM_SCOPE            = 'https://www.googleapis.com/auth/firebase.messaging';
const TS_CHAT_FCM_TIMEOUT_SEC       = 4;

// ---------------------------------------------------------------------------
// Bootstrap
// ---------------------------------------------------------------------------
ts_apply_security_headers();
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

if (ts_requires_https() && !ts_is_https()) {
    ts_chat_json('error', 'Secure connection required.', [], 400);
}

$pdo    = getDbConnection();
$action = isset($_GET['action']) ? trim((string) $_GET['action']) : '';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function ts_chat_json(string $status, string $message, array $data = [], int $code = 200): void
{
    http_response_code($code);
    if (!headers_sent()) {
        header('Cache-Control: no-store');
    }
    echo json_encode(
        array_merge(['status' => $status, 'message' => $message], $data),
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
    );
    exit();
}

function ts_chat_require_post(string $action): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        header('Allow: POST');
        ts_chat_json('error', 'Only POST requests are allowed for ' . $action . '.', [], 405);
    }
}

/**
 * Read a JSON (or form-encoded) request body with a hard size limit.
 */
function ts_chat_read_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false) {
        return [];
    }
    if (strlen($raw) > TS_CHAT_MAX_BODY_BYTES) {
        ts_chat_json('error', 'Request body is too large.', [], 413);
    }
    if (trim($raw) === '') {
        return $_POST; // form-encoded fallback
    }
    $decoded = json_decode($raw, true);
    if (!is_array($decoded)) {
        ts_chat_json('error', 'Malformed JSON body.', [], 400);
    }
    return $decoded;
}

function ts_chat_bearer_token(): ?string
{
    $header = '';
    if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $header = (string) $_SERVER['HTTP_AUTHORIZATION'];
    } elseif (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $header = (string) $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
    } elseif (function_exists('apache_request_headers')) {
        foreach ((array) apache_request_headers() as $name => $value) {
            if (strcasecmp((string) $name, 'Authorization') === 0) {
                $header = (string) $value;
                break;
            }
        }
    }

    if ($header === '' || !preg_match('/^Bearer\s+([A-Fa-f0-9]{32,128})$/', trim($header), $m)) {
        return null;
    }
    return $m[1];
}

/**
 * Resolve the caller and refresh their presence in one step.
 *
 * @return array<string,mixed> the `users` row of the caller
 */
function ts_chat_require_user(PDO $pdo): array
{
    $token = ts_chat_bearer_token();
    if ($token === null) {
        ts_chat_json('error', 'Authentication required.', [], 401);
    }

    $stmt = $pdo->prepare(
        'SELECT u.*, t.expires_at
         FROM user_tokens t
         JOIN users u ON u.id = t.user_id
         WHERE t.token = :token AND t.expires_at > NOW()
         LIMIT 1'
    );
    $stmt->execute(['token' => hash('sha256', $token)]);
    $user = $stmt->fetch();

    if (!$user) {
        ts_chat_json('error', 'Session expired or invalid. Please sign in again.', [], 401);
    }
    if ((int) $user['is_active'] !== 1) {
        ts_chat_json('error', 'This account has been disabled. Please contact support.', [], 403);
    }
    if ((int) $user['is_email_verified'] !== 1) {
        ts_chat_json('error', 'Email address is not yet verified.', [], 403);
    }

    ts_chat_touch_presence($pdo, (int) $user['id']);

    return $user;
}

function ts_chat_touch_presence(PDO $pdo, int $userId): void
{
    try {
        $pdo->prepare('UPDATE users SET last_seen_at = NOW() WHERE id = :id')
            ->execute(['id' => $userId]);
    } catch (Throwable $e) {
        error_log('[TheStudent] chat presence update failed: ' . $e->getMessage());
    }
}

function ts_chat_peer_id(array $body): int
{
    $raw = $body['peer_id'] ?? ($_GET['peer_id'] ?? '');
    if (!is_numeric($raw)) {
        ts_chat_json('error', 'A valid recipient is required.', [], 422);
    }
    $peerId = (int) $raw;
    if ($peerId <= 0) {
        ts_chat_json('error', 'A valid recipient is required.', [], 422);
    }
    return $peerId;
}

/**
 * The only tags a rating may carry. Slugs on the wire, words on the phone —
 * so a translation never breaks the count.
 */
function ts_chat_rating_tag_list(): array
{
    return ['clear', 'patient', 'fast', 'notes', 'friendly', 'syllabus'];
}

/** Columns + epochs for every rating read. */
function ts_chat_rating_select(): string
{
    return 'SELECT id, rater_id, ratee_id, subject_code, subject_label, kind, stars, tags, note,
                   help_request_id,
                   UNIX_TIMESTAMP(created_at) AS created_epoch,
                   UNIX_TIMESTAMP(updated_at) AS updated_epoch
            FROM chat_ratings';
}

/**
 * One rating, as its owner sees it.
 *
 * The note is the private half: it is only ever returned to the student who
 * wrote it and the student it is about. Nobody browsing profiles gets it.
 */
function ts_chat_rating_public(array $row, int $meId, int $now): array
{
    $tags = array_values(array_filter(array_map(
        'trim',
        explode(',', (string) ($row['tags'] ?? ''))
    ), static function ($t) { return $t !== ''; }));

    $createdEpoch = isset($row['created_epoch']) ? (int) $row['created_epoch'] : 0;
    $updatedEpoch = isset($row['updated_epoch']) && $row['updated_epoch'] !== null
        ? (int) $row['updated_epoch']
        : 0;
    $baseEpoch = $updatedEpoch > 0 ? $updatedEpoch : $createdEpoch;
    $editable = $baseEpoch > 0 && ($now - $baseEpoch) < TS_CHAT_RATING_EDIT_SEC;

    return [
        'id'            => (int) $row['id'],
        'rater_id'      => (int) $row['rater_id'],
        'ratee_id'      => (int) $row['ratee_id'],
        'is_mine'       => ((int) $row['rater_id']) === $meId,
        'subject_code'  => (string) ($row['subject_code'] ?? ''),
        'subject_label' => (string) ($row['subject_label'] ?? ''),
        'kind'          => (string) ($row['kind'] ?? 'help'),
        'stars'         => (int) $row['stars'],
        'tags'          => $tags,
        'note'          => (string) ($row['note'] ?? ''),
        'created_epoch' => $createdEpoch,
        'editable'      => $editable,
        'locks_epoch'   => $baseEpoch > 0 ? $baseEpoch + TS_CHAT_RATING_EDIT_SEC : null,
    ];
}

/**
 * What everybody else is allowed to see about a student's ratings: the
 * average, the number of ratings, the per-subject split and the tag counts.
 * Never a note, never who said what.
 */
function ts_chat_rating_summary(PDO $pdo, int $rateeId): array
{
    $summary = [
        'count'    => 0,
        'average'  => null,
        'subjects' => [],
        'tags'     => [],
    ];

    try {
        $stmt = $pdo->prepare(
            'SELECT COUNT(*) AS c, AVG(stars) AS a FROM chat_ratings WHERE ratee_id = :me'
        );
        $stmt->execute(['me' => $rateeId]);
        $totals = $stmt->fetch() ?: [];
        $count = (int) ($totals['c'] ?? 0);
        $summary['count'] = $count;
        if ($count > 0 && $totals['a'] !== null) {
            $summary['average'] = round((float) $totals['a'], 1);
        }

        $stmt = $pdo->prepare(
            'SELECT subject_code, subject_label, COUNT(*) AS c, AVG(stars) AS a
             FROM chat_ratings
             WHERE ratee_id = :me
             GROUP BY subject_code, subject_label
             ORDER BY c DESC, subject_label ASC'
        );
        $stmt->execute(['me' => $rateeId]);
        foreach ($stmt->fetchAll() as $sub) {
            $summary['subjects'][] = [
                'code'    => (string) $sub['subject_code'],
                'label'   => (string) $sub['subject_label'],
                'count'   => (int) $sub['c'],
                'average' => $sub['a'] !== null ? round((float) $sub['a'], 1) : null,
            ];
        }

        // Tags are at most three short slugs per rating, so counting them here
        // beats another table and keeps the write path to a single row.
        $stmt = $pdo->prepare('SELECT tags FROM chat_ratings WHERE ratee_id = :me');
        $stmt->execute(['me' => $rateeId]);
        $counts = [];
        foreach ($stmt->fetchAll() as $row) {
            $parts = array_filter(array_map('trim', explode(',', (string) $row['tags'])));
            foreach ($parts as $tag) {
                $counts[$tag] = ($counts[$tag] ?? 0) + 1;
            }
        }
        arsort($counts);
        foreach (array_slice($counts, 0, 6, true) as $tag => $n) {
            $summary['tags'][] = ['tag' => (string) $tag, 'count' => (int) $n];
        }
    } catch (Throwable $e) {
        error_log('[TheStudent] rating summary failed: ' . $e->getMessage());
    }

    return $summary;
}

/** The latest closed help request between two students, or false. */
function ts_chat_rating_closed_help(PDO $pdo, int $meId, int $peerId)
{
    $stmt = $pdo->prepare(
        ts_chat_help_select() . "
         WHERE status = 'completed'
           AND ((seeker_id = :me_a AND helper_id = :peer_a)
             OR (seeker_id = :peer_b AND helper_id = :me_b))
         ORDER BY id DESC LIMIT 1"
    );
    $stmt->execute([
        'me_a'   => $meId,
        'peer_a' => $peerId,
        'peer_b' => $peerId,
        'me_b'   => $meId,
    ]);
    return $stmt->fetch();
}

/**
 * Compose the public, privacy-safe view of another student.
 */
function ts_chat_public_peer(array $row, int $now): array
{
    // The epoch comes from MySQL (UNIX_TIMESTAMP) so the calculation never
    // depends on the PHP default timezone matching the database server's.
    $lastSeen = isset($row['last_seen_epoch']) && $row['last_seen_epoch'] !== null
        ? (int) $row['last_seen_epoch']
        : 0;
    $isOnline = $lastSeen > 0 && ($now - $lastSeen) <= TS_CHAT_ONLINE_WINDOW_SEC;

    return [
        'id'                => (int) $row['id'],
        'name'              => (string) ($row['full_name'] ?? 'Student'),
        'avatar_url'        => ts_avatar_public_url($row['profile_picture'] ?? null),
        'is_online'         => $isOnline,
        'last_seen_at'      => $lastSeen > 0 ? gmdate('c', $lastSeen) : null,
        'last_seen_seconds' => $lastSeen > 0 ? max(0, $now - $lastSeen) : null,
        // Public half of their chat key. Without it this student cannot receive
        // end-to-end encrypted messages yet (they never opened secure chat).
        'public_key'        => $row['chat_public_key'] ?? null,
        'key_ready'         => !empty($row['chat_public_key']),
    ];
}

/**
 * The shape the app sees for a help request.
 *
 * `can_*` flags are computed here rather than in the app: only the helper may
 * accept, only the seeker may close (the seeker is the only person who knows
 * whether the question was answered), and either side may cancel.
 */
function ts_chat_help_public(array $row, int $meId): array
{
    $seeker = (int) $row['seeker_id'];
    $helper = (int) $row['helper_id'];
    $status = (string) $row['status'];
    $isSeeker = $seeker === $meId;

    return [
        'id'            => (int) $row['id'],
        'seeker_id'     => $seeker,
        'helper_id'     => $helper,
        'peer_id'       => $isSeeker ? $helper : $seeker,
        'subject_code'  => (string) ($row['subject_code'] ?? ''),
        'subject_label' => (string) ($row['subject_label'] ?? ''),
        'purposes'      => array_values(array_filter(array_map(
            'trim',
            explode(',', (string) ($row['purposes'] ?? ''))
        ), static function ($p) { return $p !== ''; })),
        'status'        => $status,
        'is_seeker'     => $isSeeker,
        'created_epoch' => isset($row['created_epoch']) ? (int) $row['created_epoch'] : 0,
        'accepted_epoch' => isset($row['accepted_epoch']) && $row['accepted_epoch'] !== null
            ? (int) $row['accepted_epoch'] : null,
        'completed_epoch' => isset($row['completed_epoch']) && $row['completed_epoch'] !== null
            ? (int) $row['completed_epoch'] : null,
        'can_accept'    => $status === 'pending' && !$isSeeker,
        'can_close'     => $status === 'accepted' && $isSeeker,
        'can_cancel'    => $status === 'pending' || $status === 'accepted',
    ];
}

/** Columns + epochs for every help-request read. */
function ts_chat_help_select(): string
{
    return 'SELECT id, seeker_id, helper_id, subject_code, subject_label, purposes, status,
                   UNIX_TIMESTAMP(created_at) AS created_epoch,
                   UNIX_TIMESTAMP(accepted_at) AS accepted_epoch,
                   UNIX_TIMESTAMP(completed_at) AS completed_epoch
            FROM chat_help_requests';
}

/** Flip stale pending rows to expired so the UI never waits forever. */
function ts_chat_help_expire(PDO $pdo): void
{
    try {
        $pdo->prepare(
            "UPDATE chat_help_requests SET status = 'expired'
             WHERE status = 'pending'
               AND created_at < (NOW() - INTERVAL " . TS_CHAT_HELP_TTL_HOURS . ' HOUR)'
        )->execute();
    } catch (Throwable $e) {
        // Never fail a read because housekeeping failed.
    }
}

/**
 * Push notification helpers (Firebase Cloud Messaging, HTTP v1).
 *
 * Push is strictly optional. The service account key is looked up in this order:
 *
 *   1. the TS_FCM_SERVICE_ACCOUNT environment variable (inline JSON),
 *   2. the TS_FCM_SERVICE_ACCOUNT_FILE environment variable (path),
 *   3. `backend/api/chat-fcm-service-account.json`,
 *   4. `backend/fcm-service-account.json`.
 *
 * With no key the functions below do nothing at all and the app simply keeps
 * polling. The key must never be committed to the repository.
 */
function ts_chat_fcm_config(): ?array
{
    static $config = null;
    static $loaded = false;
    if ($loaded) {
        return $config;
    }
    $loaded = true;

    $raw = getenv('TS_FCM_SERVICE_ACCOUNT') ?: '';
    if ($raw === '') {
        $path = getenv('TS_FCM_SERVICE_ACCOUNT_FILE') ?: '';
        $candidates = $path !== '' ? [$path] : [];
        $candidates[] = __DIR__ . '/chat-fcm-service-account.json';
        $candidates[] = dirname(__DIR__) . '/fcm-service-account.json';
        foreach ($candidates as $candidate) {
            if (is_readable($candidate)) {
                $raw = (string) file_get_contents($candidate);
                break;
            }
        }
    }
    if ($raw === '') {
        return null;
    }

    $decoded = json_decode($raw, true);
    if (!is_array($decoded)
        || empty($decoded['client_email'])
        || empty($decoded['private_key'])
        || empty($decoded['project_id'])
    ) {
        error_log('[TheStudent] FCM service account JSON is incomplete; push disabled.');
        return null;
    }
    $decoded['token_uri'] = $decoded['token_uri'] ?? 'https://oauth2.googleapis.com/token';
    $config = $decoded;
    return $config;
}

/** Base64url without padding, as JWT requires. */
function ts_chat_b64url(string $bytes): string
{
    return rtrim(strtr(base64_encode($bytes), '+/', '-_'), '=');
}

/**
 * OAuth2 access token for FCM, signed with the service account key (RS256).
 *
 * Cached in the system temp directory until shortly before it expires, because
 * minting one costs a network round trip.
 */
function ts_chat_fcm_access_token(array $config): ?string
{
    $cacheFile = sys_get_temp_dir() . '/ts_fcm_' . md5((string) $config['client_email']) . '.json';
    if (is_readable($cacheFile)) {
        $cached = json_decode((string) file_get_contents($cacheFile), true);
        if (is_array($cached)
            && !empty($cached['token'])
            && (int) ($cached['expires_at'] ?? 0) > time() + 60
        ) {
            return (string) $cached['token'];
        }
    }

    $now = time();
    $header = ts_chat_b64url(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
    $claims = ts_chat_b64url(json_encode([
        'iss'   => $config['client_email'],
        'scope' => TS_CHAT_FCM_SCOPE,
        'aud'   => $config['token_uri'],
        'iat'   => $now,
        'exp'   => $now + 3600,
    ]));
    $signingInput = $header . '.' . $claims;

    $signature = '';
    if (!openssl_sign($signingInput, $signature, (string) $config['private_key'], OPENSSL_ALGO_SHA256)) {
        error_log('[TheStudent] FCM JWT signing failed.');
        return null;
    }
    $assertion = $signingInput . '.' . ts_chat_b64url($signature);

    $response = ts_chat_http_post_form((string) $config['token_uri'], [
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion'  => $assertion,
    ]);
    if ($response === null) {
        return null;
    }
    $json = json_decode($response, true);
    $token = is_array($json) ? (string) ($json['access_token'] ?? '') : '';
    if ($token === '') {
        error_log('[TheStudent] FCM token exchange failed: ' . substr($response, 0, 300));
        return null;
    }
    $expiresIn = is_array($json) ? (int) ($json['expires_in'] ?? 3600) : 3600;
    @file_put_contents(
        $cacheFile,
        json_encode(['token' => $token, 'expires_at' => time() + $expiresIn]),
        LOCK_EX
    );
    return $token;
}

/** POST a form body; returns the response body or null. */
function ts_chat_http_post_form(string $url, array $fields): ?string
{
    $payload = http_build_query($fields);
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST           => true,
            CURLOPT_POSTFIELDS     => $payload,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT        => TS_CHAT_FCM_TIMEOUT_SEC,
            CURLOPT_CONNECTTIMEOUT => 2,
            CURLOPT_HTTPHEADER     => ['Content-Type: application/x-www-form-urlencoded'],
        ]);
        $out = curl_exec($ch);
        $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($out === false || $code >= 400) {
            return null;
        }
        return (string) $out;
    }
    $context = stream_context_create(['http' => [
        'method'        => 'POST',
        'header'        => "Content-Type: application/x-www-form-urlencoded\r\n",
        'content'       => $payload,
        'timeout'       => TS_CHAT_FCM_TIMEOUT_SEC,
        'ignore_errors' => true,
    ]]);
    $out = @file_get_contents($url, false, $context);
    return $out === false ? null : (string) $out;
}

/** POST a JSON body with a bearer token; returns [httpCode, body]. */
function ts_chat_http_post_json(string $url, string $json, string $bearer): array
{
    $headers = [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $bearer,
    ];
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST           => true,
            CURLOPT_POSTFIELDS     => $json,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT        => TS_CHAT_FCM_TIMEOUT_SEC,
            CURLOPT_CONNECTTIMEOUT => 2,
            CURLOPT_HTTPHEADER     => $headers,
        ]);
        $out = curl_exec($ch);
        $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        return [$code, $out === false ? '' : (string) $out];
    }
    $context = stream_context_create(['http' => [
        'method'        => 'POST',
        'header'        => implode("\r\n", $headers) . "\r\n",
        'content'       => $json,
        'timeout'       => TS_CHAT_FCM_TIMEOUT_SEC,
        'ignore_errors' => true,
    ]]);
    $out = @file_get_contents($url, false, $context);
    $code = 0;
    if (isset($http_response_header[0]) && preg_match('#\s(\d{3})\s#', $http_response_header[0], $m)) {
        $code = (int) $m[1];
    }
    return [$code, $out === false ? '' : (string) $out];
}

/**
 * Wake the recipient's device(s) so the app can fetch and decrypt.
 *
 * The payload deliberately carries **no message text**: only "there is a chat
 * message waiting". The app pulls it from the relay and decrypts on the device,
 * so Google never sees anything readable and the E2EE promise survives push.
 * Never throws: a failed push must not fail the send.
 */
function ts_chat_push_message(PDO $pdo, int $recipientId, int $senderId, int $serverId): void
{
    try {
        $config = ts_chat_fcm_config();
        if ($config === null) {
            return;
        }
        $stmt = $pdo->prepare('SELECT token FROM chat_devices WHERE user_id = :id LIMIT 10');
        $stmt->execute(['id' => $recipientId]);
        $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);
        if (empty($tokens)) {
            return;
        }

        $accessToken = ts_chat_fcm_access_token($config);
        if ($accessToken === null) {
            return;
        }

        $endpoint = 'https://fcm.googleapis.com/v1/projects/' . $config['project_id'] . '/messages:send';
        foreach ($tokens as $deviceToken) {
            $payload = json_encode([
                'message' => [
                    'token'        => (string) $deviceToken,
                    // Data-only: the system must not render anything itself.
                    'data'         => [
                        'type'      => 'chat_message',
                        'peer_id'   => (string) $senderId,
                        'server_id' => (string) $serverId,
                    ],
                    'android'      => [
                        'priority' => 'high',
                        'ttl'      => '600s',
                    ],
                ],
            ]);
            [$code, $body] = ts_chat_http_post_json($endpoint, (string) $payload, $accessToken);
            if ($code === 200 || $code === 201) {
                continue;
            }
            // The token is dead (app uninstalled, data cleared, rotated): drop it
            // so the next send does not wait on it.
            if ($code === 404 || $code === 400 || stripos($body, 'UNREGISTERED') !== false
                || stripos($body, 'INVALID_ARGUMENT') !== false
            ) {
                try {
                    $pdo->prepare('DELETE FROM chat_devices WHERE token = :token')
                        ->execute(['token' => (string) $deviceToken]);
                } catch (Throwable $e) {
                    error_log('[TheStudent] FCM dead token cleanup failed: ' . $e->getMessage());
                }
            }
            error_log('[TheStudent] FCM push failed (' . $code . '): ' . substr($body, 0, 300));
        }
    } catch (Throwable $e) {
        error_log('[TheStudent] FCM push error: ' . $e->getMessage());
    }
}

function ts_chat_safety_acknowledged(array $user): bool
{
    return !empty($user['chat_safety_ack_at']);
}

/**
 * How many OTHER students are online right now.
 *
 * Single definition used by both `threads` and `heartbeat`, so the counter on
 * the app can never jump between two different numbers (the caller is always
 * excluded — "1 online" while nobody else is there is misleading).
 */
function ts_chat_online_peer_count(PDO $pdo, int $excludeUserId): int
{
    try {
        $stmt = $pdo->prepare(
            'SELECT COUNT(*) FROM users
             WHERE id <> :me
               AND is_active = 1
               AND is_email_verified = 1
               AND last_seen_at > (NOW() - INTERVAL ' . TS_CHAT_ONLINE_WINDOW_SEC . ' SECOND)'
        );
        $stmt->execute(['me' => $excludeUserId]);
        return (int) $stmt->fetchColumn();
    } catch (Throwable $e) {
        error_log('[TheStudent] online count failed: ' . $e->getMessage());
        return 0;
    }
}

/**
 * Folder that holds encrypted attachment blobs.
 *
 * `uploads/` already ships an .htaccess that stops PHP from executing anything
 * in it; this adds a deny-all rule for the subfolder as well, so a blob can
 * only ever leave through the authenticated `attachment` action. The files are
 * AES-GCM ciphertext anyway — the web server could serve them and nobody could
 * read them without the conversation key.
 */
function ts_chat_attachment_dir(): string
{
    $dir = dirname(__DIR__) . '/' . TS_CHAT_ATTACHMENT_DIR;
    if (!is_dir($dir)) {
        @mkdir($dir, 0755, true);
    }
    $htaccess = $dir . '.htaccess';
    if (is_dir($dir) && !file_exists($htaccess)) {
        @file_put_contents(
            $htaccess,
            "Require all denied\n<IfModule !mod_authz_core.c>\n    Deny from all\n</IfModule>\n"
        );
    }
    return $dir;
}

/**
 * Create the relay schema when it is missing. Safe to run on every request.
 */
function ts_chat_ensure_schema(PDO $pdo): void
{
    static $done = false;
    if ($done) {
        return;
    }
    $done = true;

    try {
        // The relay table: rows exist only until the recipient fetches them.
        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_relay` (
                `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `sender_id` INT UNSIGNED NOT NULL,
                `recipient_id` INT UNSIGNED NOT NULL,
                `kind` ENUM('msg','read','delete') NOT NULL DEFAULT 'msg',
                `body` TEXT NULL DEFAULT NULL,
                `ref_id` BIGINT UNSIGNED NULL DEFAULT NULL,
                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX `idx_relay_inbox` (`recipient_id`, `id`),
                INDEX `idx_relay_sender` (`sender_id`, `created_at`),
                INDEX `idx_relay_created` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        // Push tokens: one row per install. A token can move between accounts
        // (same phone, different student signing in), so it is UNIQUE and the
        // owner is updated on every registration.
        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_devices` (
                `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `user_id` INT UNSIGNED NOT NULL,
                `token` VARCHAR(255) NOT NULL,
                `platform` VARCHAR(16) NOT NULL DEFAULT 'android',
                `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY `uniq_chat_device_token` (`token`),
                INDEX `idx_chat_device_user` (`user_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        // What a conversation is *about*, per side. One row per (owner, peer):
        // 'help' when the owner is asking for help, 'talk', 'notes', 'partner',
        // or '' when nothing has been declared yet. Kept out of the message
        // stream on purpose — it is a label that can change without sending a
        // message, and the chat list reads it to show 'Question · Physics'.
        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_purposes` (
                `user_id` INT UNSIGNED NOT NULL,
                `peer_id` INT UNSIGNED NOT NULL,
                `purpose` VARCHAR(16) NOT NULL DEFAULT '',
                `subject_code` VARCHAR(64) NOT NULL DEFAULT '',
                `subject_label` VARCHAR(120) NOT NULL DEFAULT '',
                `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (`user_id`, `peer_id`),
                INDEX `idx_purpose_user` (`user_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        // Help requests: the transaction that makes a rating meaningful.
        // A seeker opens one, the helper accepts, the seeker closes it when the
        // question is actually answered. Ratings (next step) hang off a
        // completed row, so a star can never be given to someone you never
        // talked to.
        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_help_requests` (
                `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `seeker_id` INT UNSIGNED NOT NULL,
                `helper_id` INT UNSIGNED NOT NULL,
                `subject_code` VARCHAR(64) NOT NULL DEFAULT '',
                `subject_label` VARCHAR(120) NOT NULL DEFAULT '',
                `purposes` VARCHAR(120) NOT NULL DEFAULT '',
                `status` ENUM('pending','accepted','completed','cancelled','expired')
                    NOT NULL DEFAULT 'pending',
                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                `accepted_at` DATETIME NULL DEFAULT NULL,
                `completed_at` DATETIME NULL DEFAULT NULL,
                INDEX `idx_help_seeker` (`seeker_id`, `status`),
                INDEX `idx_help_helper` (`helper_id`, `status`),
                INDEX `idx_help_pair` (`seeker_id`, `helper_id`, `id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        // Ratings: one per (rater, ratee, subject). The unique key is the whole
        // anti-abuse story — a second tap, a reinstall or a second device can
        // never add a second star for the same help. `help_request_id` records
        // which closed request earned it, so a rating is traceable to real help.
        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_ratings` (
                `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `rater_id` INT UNSIGNED NOT NULL,
                `ratee_id` INT UNSIGNED NOT NULL,
                `subject_code` VARCHAR(64) NOT NULL DEFAULT '',
                `subject_label` VARCHAR(120) NOT NULL DEFAULT '',
                `kind` VARCHAR(12) NOT NULL DEFAULT 'help',
                `stars` TINYINT UNSIGNED NOT NULL DEFAULT 0,
                `tags` VARCHAR(120) NOT NULL DEFAULT '',
                `note` VARCHAR(80) NOT NULL DEFAULT '',
                `help_request_id` BIGINT UNSIGNED NULL DEFAULT NULL,
                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                `updated_at` DATETIME NULL DEFAULT NULL,
                UNIQUE KEY `uniq_rating_pair_subject` (`rater_id`, `ratee_id`, `subject_code`),
                INDEX `idx_rating_ratee` (`ratee_id`, `subject_code`),
                INDEX `idx_rating_ratee_all` (`ratee_id`),
                INDEX `idx_rating_rater` (`rater_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );

        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_typing` (
                `user_id` INT UNSIGNED NOT NULL,
                `peer_id` INT UNSIGNED NOT NULL,
                `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (`user_id`, `peer_id`),
                INDEX `idx_chat_typing_peer` (`peer_id`, `updated_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );
    } catch (Throwable $e) {
        error_log('[TheStudent] chat schema creation failed: ' . $e->getMessage());
    }

    // Older installs created `kind` as ENUM('msg','read'); widen it once so a
    // delete envelope can be relayed. Checked first, because widening an ENUM
    // rebuilds the table on some MySQL versions and this runs on every request.
    try {
        $kindStmt = $pdo->query("SHOW COLUMNS FROM `chat_relay` LIKE 'kind'");
        $kindRow  = $kindStmt ? $kindStmt->fetch() : null;
        if ($kindRow && stripos((string) $kindRow['Type'], 'delete') === false) {
            $pdo->exec(
                "ALTER TABLE `chat_relay` MODIFY COLUMN `kind` ENUM('msg','read','delete')
                 NOT NULL DEFAULT 'msg'"
            );
        }
    } catch (Throwable $e) {
        error_log('[TheStudent] chat_relay kind widen skipped: ' . $e->getMessage());
    }

    // Encrypted attachment blobs (never readable by the server).
    try {
        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_attachments` (
                `id` VARCHAR(64) NOT NULL PRIMARY KEY,
                `sender_id` INT UNSIGNED NOT NULL,
                `recipient_id` INT UNSIGNED NOT NULL,
                `bytes` BIGINT UNSIGNED NOT NULL DEFAULT 0,
                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX `idx_chat_att_recipient` (`recipient_id`, `created_at`),
                INDEX `idx_chat_att_created` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );
    } catch (Throwable $e) {
        error_log('[TheStudent] chat attachment table creation failed: ' . $e->getMessage());
    }

    ts_chat_attachment_dir();

    // Columns live on `users` so presence / keys survive a relay table rebuild.
    ts_ensure_column($pdo, 'users', 'last_seen_at', 'DATETIME NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'chat_safety_ack_at', 'DATETIME NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'chat_public_key', 'TEXT NULL DEFAULT NULL');
}

// ---------------------------------------------------------------------------
// Dispatch
// ---------------------------------------------------------------------------
if ($action === 'version') {
    ts_chat_json('success', 'TheStudent chat API', [
        'api_version'       => TS_CHAT_API_VERSION,
        'php_version'       => PHP_VERSION,
        'storage'           => 'relay-only (history lives on the device)',
        'attachments'       => 'encrypted blobs, ' . (TS_CHAT_MAX_ATTACHMENT_BYTES / 1048576) .
                               ' MB max, deleted on delivery or after ' . TS_CHAT_RELAY_TTL_DAYS . ' days',
        'delete_for_all'    => true,
        'chat_php_modified' => gmdate('c', (int) filemtime(__FILE__)),
    ]);
}

ts_chat_ensure_schema($pdo);

$me   = ts_chat_require_user($pdo);
$meId = (int) $me['id'];

// Single clock for presence + timestamps: the database's.
try {
    $nowRow = $pdo->query('SELECT UNIX_TIMESTAMP(NOW()) AS now_epoch')->fetch();
    $now = (int) ($nowRow['now_epoch'] ?? time());
} catch (Throwable $e) {
    $now = time();
}

// Housekeeping: undelivered messages expire, so the relay can never grow into
// an archive (this is what keeps cloud usage flat).
try {
    $pdo->prepare(
        'DELETE FROM chat_relay WHERE created_at < (NOW() - INTERVAL ' . TS_CHAT_RELAY_TTL_DAYS . ' DAY)'
    )->execute();
    $pdo->prepare(
        'DELETE FROM chat_typing WHERE updated_at < (NOW() - INTERVAL 1 DAY)'
    )->execute();

    // Attachment blobs follow the same 7-day rule as the relay, and the files
    // are removed with their rows so the folder can never grow.
    $expired = $pdo->prepare(
        'SELECT id FROM chat_attachments WHERE created_at < (NOW() - INTERVAL ' .
            TS_CHAT_RELAY_TTL_DAYS . ' DAY)'
    );
    $expired->execute();
    foreach ($expired->fetchAll() as $oldRow) {
        @unlink(ts_chat_attachment_dir() . $oldRow['id'] . '.bin');
    }
    $pdo->prepare(
        'DELETE FROM chat_attachments WHERE created_at < (NOW() - INTERVAL ' .
            TS_CHAT_RELAY_TTL_DAYS . ' DAY)'
    )->execute();
} catch (Throwable $e) {
    error_log('[TheStudent] chat relay cleanup skipped: ' . $e->getMessage());
}

try {
switch ($action) {

    // -----------------------------------------------------------------------
    // Student directory (no message content — the app reads its own database)
    // -----------------------------------------------------------------------
    case 'threads': {
        $stmt = $pdo->prepare(
            'SELECT u.id, u.full_name, u.profile_picture, u.last_seen_at,
                    UNIX_TIMESTAMP(u.last_seen_at) AS last_seen_epoch,
                    u.chat_public_key
             FROM users u
             WHERE u.id <> :me
               AND u.is_active = 1
               AND u.is_email_verified = 1
             ORDER BY u.full_name ASC
             LIMIT 500'
        );
        $stmt->execute(['me' => $meId]);
        $peerRows = $stmt->fetchAll();

        // Who is typing to me right now
        $typingBy = [];
        try {
            $typingStmt = $pdo->prepare(
                'SELECT user_id FROM chat_typing
                 WHERE peer_id = :peer
                   AND updated_at > (NOW() - INTERVAL ' . TS_CHAT_TYPING_WINDOW_SEC . ' SECOND)'
            );
            $typingStmt->execute(['peer' => $meId]);
            foreach ($typingStmt->fetchAll() as $row) {
                $typingBy[(int) $row['user_id']] = true;
            }
        } catch (Throwable $e) {
            error_log('[TheStudent] chat typing query failed: ' . $e->getMessage());
        }

        // My own declared purposes, read once for the whole directory.
        $purposes = [];
        try {
            $purposeStmt = $pdo->prepare(
                'SELECT peer_id, purpose, subject_label
                 FROM chat_purposes WHERE user_id = :me'
            );
            $purposeStmt->execute(['me' => $meId]);
            foreach ($purposeStmt->fetchAll() as $row) {
                $purposes[(int) $row['peer_id']] = [
                    'purpose'       => (string) $row['purpose'],
                    'subject_label' => (string) $row['subject_label'],
                ];
            }
        } catch (Throwable $e) {
            // Table missing on an older schema: threads simply carry no purpose.
        }

        $threads = [];
        foreach ($peerRows as $peerRow) {
            $peerId = (int) $peerRow['id'];
            $threads[] = [
                'peer'      => ts_chat_public_peer($peerRow, $now),
                'is_typing' => isset($typingBy[$peerId]),
                'purpose'   => $purposes[$peerId]['purpose'] ?? '',
                'purpose_subject' => $purposes[$peerId]['subject_label'] ?? '',
            ];
        }

        ts_chat_json('success', 'Chat directory loaded.', [
            'threads'               => $threads,
            'online_count'          => ts_chat_online_peer_count($pdo, $meId),
            'total_students'        => count($threads),
            'safety_acknowledged'   => ts_chat_safety_acknowledged($me),
            'online_window_seconds' => TS_CHAT_ONLINE_WINDOW_SEC,
            'server_epoch'          => $now,
            'my_public_key'         => $me['chat_public_key'] ?? null,
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // Public profile of one student (the chat profile screen)
    // -----------------------------------------------------------------------
    case 'profile': {
        $peerId = ts_chat_peer_id($_GET);
        if ($peerId <= 0) {
            ts_chat_json('error', 'Choose a student first.', [], 422);
        }
        if ($peerId === $meId) {
            ts_chat_json('error', 'That is your own profile.', [], 422);
        }

        try {
            $stmt = $pdo->prepare(
                'SELECT id, full_name, profile_picture, target_exam, institution,
                        stream_or_degree, last_seen_at,
                        UNIX_TIMESTAMP(last_seen_at) AS last_seen_epoch,
                        UNIX_TIMESTAMP(created_at) AS joined_epoch
                 FROM users
                 WHERE id = :id AND is_active = 1 AND is_email_verified = 1
                 LIMIT 1'
            );
            $stmt->execute(['id' => $peerId]);
            $row = $stmt->fetch();
            if (!$row) {
                ts_chat_json('error', 'That student is no longer available.', [], 404);
            }

            // Structured academic profile (stream / level / year / medium +
            // the three subjects). Read in its own guarded query so a relay
            // deployed before the auth schema migration still returns a profile
            // instead of a 500.
            $academic = ['stream_code' => '', 'exam_level' => '', 'exam_year' => 0, 'medium' => ''];
            $subjects = [];
            try {
                $acStmt = $pdo->prepare(
                    'SELECT stream_code, exam_level, exam_year, medium
                     FROM users WHERE id = :id LIMIT 1'
                );
                $acStmt->execute(['id' => $peerId]);
                $acRow = $acStmt->fetch();
                if ($acRow) {
                    $academic = [
                        'stream_code' => (string) ($acRow['stream_code'] ?? ''),
                        'exam_level'  => (string) ($acRow['exam_level'] ?? ''),
                        'exam_year'   => (int) ($acRow['exam_year'] ?? 0),
                        'medium'      => (string) ($acRow['medium'] ?? ''),
                    ];
                }
            } catch (Throwable $e) {
                // Older schema: fall back to the free-text fields only.
            }
            try {
                $subStmt = $pdo->prepare(
                    'SELECT subject_code, subject_label
                     FROM user_subjects
                     WHERE user_id = :id
                     ORDER BY position ASC, id ASC'
                );
                $subStmt->execute(['id' => $peerId]);
                foreach ($subStmt->fetchAll() as $sub) {
                    $subjects[] = [
                        'code'  => (string) $sub['subject_code'],
                        'label' => (string) $sub['subject_label'],
                    ];
                }
            } catch (Throwable $e) {
                // No subjects table yet: the client simply shows nothing.
            }

            // Allow-list only: the SELECT above never touches email, phone,
            // password_hash, role, auth_provider or any login metadata.
            $joinedEpoch = (int) ($row['joined_epoch'] ?? 0);
            ts_chat_json('success', 'Profile loaded.', [
                'peer' => ts_chat_public_peer($row, $now),
                'education' => [
                    'target_exam'      => trim((string) ($row['target_exam'] ?? '')),
                    'stream_or_degree' => trim((string) ($row['stream_or_degree'] ?? '')),
                    'institution'      => trim((string) ($row['institution'] ?? '')),
                    'stream_code'      => $academic['stream_code'],
                    'exam_level'       => $academic['exam_level'],
                    'exam_year'        => $academic['exam_year'],
                    'medium'           => $academic['medium'],
                    'subjects'         => $subjects,
                ],
                'joined_epoch' => $joinedEpoch > 0 ? $joinedEpoch : null,
                // Public reputation: average, count, per-subject split and the
                // tag counts. Notes are never included here.
                'ratings'      => ts_chat_rating_summary($pdo, $peerId),
                'server_epoch' => $now,
            ]);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat profile failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not load that profile right now.', [], 500);
        }
        break;
    }

    // -----------------------------------------------------------------------
    // Conversation purpose ("what's this chat about")
    // -----------------------------------------------------------------------
    case 'set_purpose': {
        ts_chat_require_post('set_purpose');
        $body   = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);

        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot label a chat with yourself.', [], 422);
        }

        $purpose = strtolower(trim((string) ($body['purpose'] ?? '')));
        $allowed = ['', 'help', 'talk', 'notes', 'partner'];
        if (!in_array($purpose, $allowed, true)) {
            ts_chat_json('error', 'That is not a chat purpose.', [], 422);
        }

        $subjectLabel = substr(trim((string) ($body['subject_label'] ?? '')), 0, 120);
        $subjectCode = substr(trim((string) ($body['subject_code'] ?? '')), 0, 64);
        if ($subjectCode === '' && $subjectLabel !== '') {
            $subjectCode = substr(
                strtolower((string) preg_replace('/[^A-Za-z0-9]+/', '_', $subjectLabel)),
                0,
                64
            );
        }

        try {
            $pdo->prepare(
                'INSERT INTO chat_purposes (user_id, peer_id, purpose, subject_code, subject_label)
                 VALUES (:me, :peer, :purpose, :code, :label)
                 ON DUPLICATE KEY UPDATE purpose = VALUES(purpose),
                     subject_code = VALUES(subject_code),
                     subject_label = VALUES(subject_label)'
            )->execute([
                'me'      => $meId,
                'peer'    => $peerId,
                'purpose' => $purpose,
                'code'    => $subjectCode,
                'label'   => $subjectLabel,
            ]);
        } catch (Throwable $e) {
            error_log('[TheStudent] set_purpose failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not save that right now.', [], 500);
        }

        ts_chat_json('success', 'Chat purpose saved.', [
            'peer_id'         => $peerId,
            'purpose'         => $purpose,
            'subject_label'   => $subjectLabel,
        ]);
    }

    // -----------------------------------------------------------------------
    // Help requests: seeker opens -> helper accepts -> seeker closes
    // -----------------------------------------------------------------------
    case 'help_request': {
        ts_chat_require_post('help_request');
        $body   = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);

        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot ask yourself for help.', [], 422);
        }

        $peerStmt = $pdo->prepare(
            'SELECT id FROM users WHERE id = :id AND is_active = 1 AND is_email_verified = 1 LIMIT 1'
        );
        $peerStmt->execute(['id' => $peerId]);
        if (!$peerStmt->fetch()) {
            ts_chat_json('error', 'That student is no longer available.', [], 404);
        }

        ts_chat_help_expire($pdo);

        // One open request per pair: asking twice is noise, not signal.
        // NOTE: PDO runs with ATTR_EMULATE_PREPARES = false, so a named
        // placeholder may appear only ONCE per statement. Repeating :me/:peer
        // on both sides of a pair check throws "Invalid parameter number" and
        // the whole request dies with a 500 — so each side gets its own name.
        $openStmt = $pdo->prepare(
            ts_chat_help_select() . "
             WHERE ((seeker_id = :me_a AND helper_id = :peer_a)
                 OR (seeker_id = :peer_b AND helper_id = :me_b))
               AND status IN ('pending','accepted')
             ORDER BY id DESC LIMIT 1"
        );
        $openStmt->execute([
            'me_a'   => $meId,
            'peer_a' => $peerId,
            'peer_b' => $peerId,
            'me_b'   => $meId,
        ]);
        $open = $openStmt->fetch();
        if ($open) {
            ts_chat_json(
                'success',
                'You already have an open request with this student.',
                ['request' => ts_chat_help_public($open, $meId), 'existing' => true]
            );
        }

        // Throttle: a student who asks everyone every day is spamming.
        $countStmt = $pdo->prepare(
            'SELECT COUNT(*) FROM chat_help_requests
             WHERE seeker_id = :me AND created_at > (NOW() - INTERVAL 1 DAY)'
        );
        $countStmt->execute(['me' => $meId]);
        if ((int) $countStmt->fetchColumn() >= TS_CHAT_HELP_MAX_PER_DAY) {
            ts_chat_json(
                'error',
                'That is a lot of requests for one day. Try again tomorrow.',
                [],
                429
            );
        }

        $subjectLabel = substr(trim((string) ($body['subject_label'] ?? '')), 0, 120);
        $subjectCode = substr(trim((string) ($body['subject_code'] ?? '')), 0, 64);
        if ($subjectCode === '' && $subjectLabel !== '') {
            $subjectCode = substr(strtolower((string) preg_replace('/[^A-Za-z0-9]+/', '_', $subjectLabel)), 0, 64);
        }

        $rawPurposes = $body['purposes'] ?? [];
        if (is_array($rawPurposes)) {
            $purposes = [];
            foreach ($rawPurposes as $purpose) {
                $clean = substr(trim((string) $purpose), 0, 40);
                if ($clean !== '' && !in_array($clean, $purposes, true)) {
                    $purposes[] = $clean;
                }
            }
            $purposesCsv = substr(implode(',', $purposes), 0, 120);
        } else {
            $purposesCsv = substr(trim((string) $rawPurposes), 0, 120);
        }

        $insert = $pdo->prepare(
            'INSERT INTO chat_help_requests
                (seeker_id, helper_id, subject_code, subject_label, purposes, status)
             VALUES (:seeker, :helper, :code, :label, :purposes, \'pending\')'
        );
        $insert->execute([
            'seeker'   => $meId,
            'helper'   => $peerId,
            'code'     => $subjectCode,
            'label'    => $subjectLabel,
            'purposes' => $purposesCsv,
        ]);
        $requestId = (int) $pdo->lastInsertId();

        // The chat list draws its label from `chat_purposes`, so a question must
        // leave a label behind for BOTH students. Without this the list only
        // ever said "Just talking", and a Physics question looked like small talk.
        try {
            $purposeStmt = $pdo->prepare(
                "INSERT INTO chat_purposes (user_id, peer_id, purpose, subject_code, subject_label)
                 VALUES (:owner, :other, 'help', :code, :label)
                 ON DUPLICATE KEY UPDATE purpose = 'help',
                     subject_code = VALUES(subject_code),
                     subject_label = VALUES(subject_label)"
            );
            $purposeStmt->execute([
                'owner' => $meId,
                'other' => $peerId,
                'code'  => $subjectCode,
                'label' => $subjectLabel,
            ]);
            $purposeStmt->execute([
                'owner' => $peerId,
                'other' => $meId,
                'code'  => $subjectCode,
                'label' => $subjectLabel,
            ]);
        } catch (Throwable $e) {
            error_log('[TheStudent] help purpose label skipped: ' . $e->getMessage());
        }

        $rowStmt = $pdo->prepare(ts_chat_help_select() . ' WHERE id = :id LIMIT 1');
        $rowStmt->execute(['id' => $requestId]);
        $row = $rowStmt->fetch();

        ts_chat_json('success', 'Request sent.', [
            'request' => $row ? ts_chat_help_public($row, $meId) : null,
            'existing' => false,
        ]);
    }

    case 'help_state': {
        $peerId = ts_chat_peer_id($_GET);
        ts_chat_help_expire($pdo);

        // Same one-placeholder-per-statement rule as in help_request.
        $stmt = $pdo->prepare(
            ts_chat_help_select() . "
             WHERE ((seeker_id = :me_a AND helper_id = :peer_a)
                 OR (seeker_id = :peer_b AND helper_id = :me_b))
               AND (status IN ('pending','accepted')
                    OR (status = 'completed' AND completed_at > (NOW() - INTERVAL 7 DAY)))
             ORDER BY id DESC LIMIT 1"
        );
        $stmt->execute([
            'me_a'   => $meId,
            'peer_a' => $peerId,
            'peer_b' => $peerId,
            'me_b'   => $meId,
        ]);
        $row = $stmt->fetch();

        // Students need help more than once: show how often this pair has
        // already closed a request, so the card can offer "ask again" instead of
        // looking like a one-shot feature.
        $closedWithPeer = 0;
        $openForMe = 0;
        $purpose = '';
        $purposeSubject = '';
        try {
            $countStmt = $pdo->prepare(
                "SELECT COUNT(*) FROM chat_help_requests
                 WHERE status = 'completed'
                   AND ((seeker_id = :me_a AND helper_id = :peer_a)
                     OR (seeker_id = :peer_b AND helper_id = :me_b))"
            );
            $countStmt->execute([
                'me_a'   => $meId,
                'peer_a' => $peerId,
                'peer_b' => $peerId,
                'me_b'   => $meId,
            ]);
            $closedWithPeer = (int) $countStmt->fetchColumn();

            $openStmt = $pdo->prepare(
                "SELECT COUNT(*) FROM chat_help_requests
                 WHERE seeker_id = :me AND status IN ('pending','accepted')"
            );
            $openStmt->execute(['me' => $meId]);
            $openForMe = (int) $openStmt->fetchColumn();

            $purposeStmt = $pdo->prepare(
                'SELECT purpose, subject_label FROM chat_purposes
                 WHERE user_id = :me AND peer_id = :peer LIMIT 1'
            );
            $purposeStmt->execute(['me' => $meId, 'peer' => $peerId]);
            $purposeRow = $purposeStmt->fetch();
            if ($purposeRow) {
                $purpose = (string) $purposeRow['purpose'];
                $purposeSubject = (string) $purposeRow['subject_label'];
            } elseif ($row) {
                // No label stored (an older request, or a row written before the
                // list learned to label): the request itself is the truth.
                $purpose = 'help';
                $purposeSubject = (string) ($row['subject_label'] ?? '');
            }
        } catch (Throwable $e) {
            // Counts are a nicety; never fail the state read for them.
        }

        ts_chat_json('success', 'Help state.', [
            'request' => $row ? ts_chat_help_public($row, $meId) : null,
            'completed_with_peer' => $closedWithPeer,
            'my_open_requests'    => $openForMe,
            'purpose'             => $purpose,
            'purpose_subject'     => $purposeSubject,
        ]);
    }

    case 'help_accept':
    case 'help_close':
    case 'help_cancel': {
        ts_chat_require_post($action);
        $body = ts_chat_read_body();
        $requestId = (int) ($body['request_id'] ?? 0);
        if ($requestId <= 0) {
            ts_chat_json('error', 'That request no longer exists.', [], 422);
        }

        ts_chat_help_expire($pdo);

        $stmt = $pdo->prepare(ts_chat_help_select() . ' WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $requestId]);
        $row = $stmt->fetch();
        if (!$row) {
            ts_chat_json('error', 'That request no longer exists.', [], 404);
        }

        $seeker = (int) $row['seeker_id'];
        $helper = (int) $row['helper_id'];
        $status = (string) $row['status'];

        if ($action === 'help_accept') {
            if ($meId !== $helper) {
                ts_chat_json('error', 'Only the student who was asked can accept.', [], 403);
            }
            if ($status !== 'pending') {
                ts_chat_json('error', 'This request is no longer open.', [], 409);
            }
            $pdo->prepare(
                "UPDATE chat_help_requests SET status = 'accepted', accepted_at = NOW() WHERE id = :id"
            )->execute(['id' => $requestId]);
        } elseif ($action === 'help_close') {
            if ($meId !== $seeker) {
                ts_chat_json('error', 'Only the student who asked can close the request.', [], 403);
            }
            if ($status !== 'accepted') {
                ts_chat_json('error', 'Accept the help before closing the request.', [], 409);
            }
            $acceptedEpoch = $row['accepted_epoch'] !== null ? (int) $row['accepted_epoch'] : 0;
            if ($acceptedEpoch > 0 && ($now - $acceptedEpoch) < TS_CHAT_HELP_MIN_CLOSE_SEC) {
                header('Retry-After: 60');
                ts_chat_json(
                    'error',
                    'Give it a few minutes of real conversation first.',
                    [],
                    429
                );
            }
            $pdo->prepare(
                "UPDATE chat_help_requests SET status = 'completed', completed_at = NOW() WHERE id = :id"
            )->execute(['id' => $requestId]);
        } else {
            if ($meId !== $seeker && $meId !== $helper) {
                ts_chat_json('error', 'That request is not yours.', [], 403);
            }
            if ($status !== 'pending' && $status !== 'accepted') {
                ts_chat_json('error', 'This request is already closed.', [], 409);
            }
            $pdo->prepare(
                "UPDATE chat_help_requests SET status = 'cancelled' WHERE id = :id"
            )->execute(['id' => $requestId]);
        }

        $freshStmt = $pdo->prepare(ts_chat_help_select() . ' WHERE id = :id LIMIT 1');
        $freshStmt->execute(['id' => $requestId]);
        $fresh = $freshStmt->fetch();

        ts_chat_json('success', 'Request updated.', [
            'request' => $fresh ? ts_chat_help_public($fresh, $meId) : null,
        ]);
    }

    // -----------------------------------------------------------------------
    // Inbox: hand over everything waiting for me, then delete it
    // -----------------------------------------------------------------------
    case 'inbox': {
        try {
            $stmt = $pdo->prepare(
                'SELECT r.id, r.sender_id, r.kind, r.body, r.ref_id,
                        UNIX_TIMESTAMP(r.created_at) AS created_epoch,
                        u.full_name, u.profile_picture, u.last_seen_at,
                        UNIX_TIMESTAMP(u.last_seen_at) AS last_seen_epoch,
                        u.chat_public_key
                 FROM chat_relay r
                 JOIN users u ON u.id = r.sender_id
                 WHERE r.recipient_id = :me
                 ORDER BY r.id ASC
                 LIMIT ' . TS_CHAT_INBOX_LIMIT
            );
            $stmt->execute(['me' => $meId]);
            $rows = $stmt->fetchAll();

            $messages = [];
            $receipts = [];
            $deletions = [];
            $ids = [];
            foreach ($rows as $row) {
                $ids[] = (int) $row['id'];
                if ($row['kind'] === 'read') {
                    $receipts[] = [
                        'peer_id'        => (int) $row['sender_id'],
                        'up_to_server_id' => (int) ($row['ref_id'] ?? 0),
                    ];
                    continue;
                }
                if ($row['kind'] === 'delete') {
                    // The sender removed a message of theirs that I already have.
                    $deletions[] = [
                        'peer_id'   => (int) $row['sender_id'],
                        'server_id' => (int) ($row['ref_id'] ?? 0),
                    ];
                    continue;
                }
                $sentAt = (int) $row['created_epoch'];
                $messages[] = [
                    'server_id'   => (int) $row['id'],
                    'sender_id'   => (int) $row['sender_id'],
                    'body'        => (string) $row['body'],
                    'sent_at'     => $sentAt > 0 ? gmdate('c', $sentAt) : null,
                    'sent_epoch'  => $sentAt,
                    'sender'      => ts_chat_public_peer($row, $now),
                ];
            }

            // Delete exactly what was handed out — the relay never keeps a copy
            // of a delivered message.
            if (!empty($ids)) {
                $placeholders = implode(',', array_fill(0, count($ids), '?'));
                $del = $pdo->prepare("DELETE FROM chat_relay WHERE id IN ($placeholders)");
                $del->execute($ids);
            }

            ts_chat_json('success', 'Inbox fetched.', [
                'messages'     => $messages,
                'receipts'     => $receipts,
                'deletions'    => $deletions,
                'server_epoch' => $now,
            ]);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat inbox failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not fetch your messages right now.', [], 500);
        }
        break;
    }

    // -----------------------------------------------------------------------
    // Queue a message for the recipient (ciphertext in, ciphertext out)
    // -----------------------------------------------------------------------
    case 'send': {
        ts_chat_require_post('send');
        $body   = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);

        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot chat with yourself.', [], 422);
        }

        $text = isset($body['body']) && is_string($body['body']) ? trim($body['body']) : '';
        if ($text === '') {
            ts_chat_json('error', 'Type a message first.', [], 422);
        }
        if (mb_strlen($text, 'UTF-8') > TS_CHAT_MAX_BODY_CHARS) {
            ts_chat_json('error', 'That message is too long (max ' . TS_CHAT_MAX_BODY_CHARS . ' characters).', [], 422);
        }
        if (!preg_match('#^e2ee:v1:[A-Za-z0-9+/=]+$#', $text)) {
            // The app refuses to send plain text; reject it server-side too so a
            // modified client cannot quietly downgrade the conversation.
            ts_chat_json('error', 'Messages must be end-to-end encrypted.', [], 422);
        }

        $peerStmt = $pdo->prepare(
            'SELECT id, chat_public_key FROM users
             WHERE id = :id AND is_active = 1 AND is_email_verified = 1 LIMIT 1'
        );
        $peerStmt->execute(['id' => $peerId]);
        $peer = $peerStmt->fetch();
        if (!$peer) {
            ts_chat_json('error', 'That student is no longer available.', [], 404);
        }
        if (empty($peer['chat_public_key'])) {
            ts_chat_json(
                'error',
                'That student has not set up secure chat yet, so messages cannot be delivered.',
                [],
                409
            );
        }

        // Anti-spam: no more than N messages per rolling minute.
        $rateStmt = $pdo->prepare(
            'SELECT COUNT(*) FROM chat_relay
             WHERE sender_id = :me AND kind = \'msg\' AND created_at > (NOW() - INTERVAL 60 SECOND)'
        );
        $rateStmt->execute(['me' => $meId]);
        if ((int) $rateStmt->fetchColumn() >= TS_CHAT_MAX_MESSAGES_PER_MIN) {
            header('Retry-After: 30');
            ts_chat_json('error', 'You are sending messages too quickly. Please slow down.', [], 429);
        }

        $insert = $pdo->prepare(
            'INSERT INTO chat_relay (sender_id, recipient_id, kind, body) VALUES (:me, :peer, \'msg\', :body)'
        );
        $insert->execute(['me' => $meId, 'peer' => $peerId, 'body' => $text]);
        $relayId = (int) $pdo->lastInsertId();

        try {
            $pdo->prepare('DELETE FROM chat_typing WHERE user_id = :me AND peer_id = :peer')
                ->execute(['me' => $meId, 'peer' => $peerId]);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat typing clear failed: ' . $e->getMessage());
        }

        $response = [
            'server_id'    => $relayId,
            'sent_epoch'   => $now,
            'server_epoch' => $now,
        ];

        // Wake the recipient's phone. Data-only push, so the app fetches and
        // decrypts and no message content ever passes through Firebase.
        //
        // The push costs a round trip to Google. Where the server supports it
        // (PHP-FPM, which is what shared hosting runs) the reply goes out first
        // and the push happens afterwards, so sending stays instant even if
        // Firebase is slow or unreachable.
        if (function_exists('fastcgi_finish_request')) {
            http_response_code(200);
            if (!headers_sent()) {
                header('Content-Type: application/json');
                header('Cache-Control: no-store');
            }
            echo json_encode(
                array_merge(['status' => 'success', 'message' => 'Message queued.'], $response),
                JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
            );
            @fastcgi_finish_request();
            ts_chat_push_message($pdo, (int) $peerId, $meId, $relayId);
            exit();
        }

        ts_chat_push_message($pdo, (int) $peerId, $meId, $relayId);
        ts_chat_json('success', 'Message queued.', $response);
        break;
    }

    // -----------------------------------------------------------------------
    // Attachment upload (opaque AES-GCM blob produced on the sender's phone)
    // -----------------------------------------------------------------------
    case 'upload_attachment': {
        ts_chat_require_post('upload_attachment');

        // peer_id can arrive either as a form field (multipart) or in the query.
        $body   = array_merge($_GET, $_POST);
        $peerId = ts_chat_peer_id($body);

        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot send a file to yourself.', [], 422);
        }

        $peerStmt = $pdo->prepare(
            'SELECT id, chat_public_key FROM users
             WHERE id = :id AND is_active = 1 AND is_email_verified = 1 LIMIT 1'
        );
        $peerStmt->execute(['id' => $peerId]);
        $peer = $peerStmt->fetch();
        if (!$peer) {
            ts_chat_json('error', 'That student is no longer available.', [], 404);
        }
        if (empty($peer['chat_public_key'])) {
            ts_chat_json(
                'error',
                'That student has not set up secure chat yet, so files cannot be sent.',
                [],
                409
            );
        }

        if (!isset($_FILES['file'])) {
            // When post_max_size is exceeded PHP throws the whole body away and
            // $_FILES is empty, so say that instead of "no file received".
            $contentLength = (int) ($_SERVER['CONTENT_LENGTH'] ?? 0);
            if ($contentLength > 0) {
                ts_chat_json(
                    'error',
                    'That file is larger than this server accepts (post_max_size ' .
                        ini_get('post_max_size') . '). Please send a smaller one.',
                    [],
                    413
                );
            }
            ts_chat_json('error', 'No file was received.', [], 422);
        }
        $file = $_FILES['file'];
        $uploadError = (int) ($file['error'] ?? UPLOAD_ERR_NO_FILE);
        if ($uploadError !== UPLOAD_ERR_OK) {
            if ($uploadError === UPLOAD_ERR_INI_SIZE || $uploadError === UPLOAD_ERR_FORM_SIZE) {
                ts_chat_json(
                    'error',
                    'That file is larger than this server accepts (' .
                        ini_get('upload_max_filesize') . '). Please send a smaller one.',
                    [],
                    413
                );
            }
            ts_chat_json('error', 'The upload did not complete. Please try again.', [], 500);
        }

        $size = (int) ($file['size'] ?? 0);
        if ($size <= 0) {
            ts_chat_json('error', 'That file is empty.', [], 422);
        }
        if ($size > TS_CHAT_MAX_ATTACHMENT_BYTES) {
            ts_chat_json(
                'error',
                'That file is too large (max ' .
                    round(TS_CHAT_MAX_ATTACHMENT_BYTES / 1048576) . ' MB).',
                [],
                413
            );
        }

        // Anti-abuse: a rolling cap on how many blobs one student can push.
        try {
            $countStmt = $pdo->prepare(
                'SELECT COUNT(*) FROM chat_attachments
                 WHERE sender_id = :me AND created_at > (NOW() - INTERVAL 60 SECOND)'
            );
            $countStmt->execute(['me' => $meId]);
            if ((int) $countStmt->fetchColumn() >= TS_CHAT_MAX_UPLOADS_PER_MIN) {
                header('Retry-After: 30');
                ts_chat_json('error', 'You are sending files too quickly. Please slow down.', [], 429);
            }
        } catch (Throwable $e) {
            error_log('[TheStudent] attachment rate check failed: ' . $e->getMessage());
        }

        $id   = bin2hex(random_bytes(16));
        $dir  = ts_chat_attachment_dir();
        $path = $dir . $id . '.bin';
        if (!is_dir($dir) || !@move_uploaded_file($file['tmp_name'], $path)) {
            error_log('[TheStudent] attachment move failed for user ' . $meId);
            ts_chat_json('error', 'The file could not be stored. Please try again.', [], 500);
        }
        @chmod($path, 0640);

        try {
            $pdo->prepare(
                'INSERT INTO chat_attachments (id, sender_id, recipient_id, bytes)
                 VALUES (:id, :me, :peer, :bytes)'
            )->execute(['id' => $id, 'me' => $meId, 'peer' => $peerId, 'bytes' => $size]);
        } catch (Throwable $e) {
            @unlink($path);
            error_log('[TheStudent] attachment row insert failed: ' . $e->getMessage());
            ts_chat_json('error', 'The file could not be stored. Please try again.', [], 500);
        }

        ts_chat_json('success', 'File queued.', [
            'attachment_id' => $id,
            'bytes'         => $size,
            'server_epoch'  => $now,
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // Attachment download (sender or recipient only; delivered once)
    // -----------------------------------------------------------------------
    case 'attachment': {
        $id = isset($_GET['id']) ? trim((string) $_GET['id']) : '';
        if (!preg_match('/^[A-Fa-f0-9]{8,64}$/', $id)) {
            ts_chat_json('error', 'That file reference is not valid.', [], 422);
        }

        $stmt = $pdo->prepare('SELECT * FROM chat_attachments WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch();
        if (!$row) {
            ts_chat_json('error', 'That file is no longer available.', [], 404);
        }

        $senderId    = (int) $row['sender_id'];
        $recipientId = (int) $row['recipient_id'];
        if ($meId !== $senderId && $meId !== $recipientId) {
            // Same answer as "not found": never confirm that a blob exists.
            ts_chat_json('error', 'That file is no longer available.', [], 404);
        }

        $path = ts_chat_attachment_dir() . $id . '.bin';
        if (!is_file($path)) {
            $pdo->prepare('DELETE FROM chat_attachments WHERE id = :id')->execute(['id' => $id]);
            ts_chat_json('error', 'That file is no longer available.', [], 404);
        }

        $bytes = (int) @filesize($path);
        if (!headers_sent()) {
            header('Content-Type: application/octet-stream');
            header('Content-Length: ' . $bytes);
            header('Content-Disposition: attachment; filename="' . $id . '.bin"');
            header('Cache-Control: no-store');
            header('X-Content-Type-Options: nosniff');
        }
        readfile($path);

        // The recipient getting the file is the delivery: drop both the blob and
        // the row, exactly like a relayed message. The sender keeps the copy
        // that is already in their own device database.
        if ($meId === $recipientId) {
            @unlink($path);
            try {
                $pdo->prepare('DELETE FROM chat_attachments WHERE id = :id')->execute(['id' => $id]);
            } catch (Throwable $e) {
                error_log('[TheStudent] attachment row delete failed: ' . $e->getMessage());
            }
        }
        exit();
    }

    // -----------------------------------------------------------------------
    // Delete one of my messages for both sides
    // -----------------------------------------------------------------------
    case 'delete_message': {
        ts_chat_require_post('delete_message');
        $body     = ts_chat_read_body();
        $peerId   = ts_chat_peer_id($body);
        $serverId = isset($body['server_id']) && is_numeric($body['server_id'])
            ? (int) $body['server_id']
            : 0;

        if ($peerId === $meId || $serverId <= 0) {
            ts_chat_json('success', 'Nothing to delete.', []);
        }

        // The relay never keeps a message after delivery, so ownership cannot be
        // re-verified here. The envelope only ever removes a message from the
        // peer's device; it cannot expose or alter anything else.
        try {
            $pdo->prepare(
                'INSERT INTO chat_relay (sender_id, recipient_id, kind, ref_id)
                 VALUES (:me, :peer, \'delete\', :ref)'
            )->execute(['me' => $meId, 'peer' => $peerId, 'ref' => $serverId]);
            ts_chat_json('success', 'Message deleted for both sides.', []);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat delete relay failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not delete that message right now.', [], 500);
        }
        break;
    }

    // -----------------------------------------------------------------------
    // Read receipt (delivered to the sender through the relay)
    // -----------------------------------------------------------------------
    case 'read': {
        ts_chat_require_post('read');
        $body   = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);
        $upTo   = isset($body['up_to_server_id']) && is_numeric($body['up_to_server_id'])
            ? max(0, (int) $body['up_to_server_id'])
            : 0;

        if ($upTo <= 0 || $peerId === $meId) {
            ts_chat_json('success', 'Nothing to acknowledge.', []);
        }

        try {
            $pdo->prepare(
                'INSERT INTO chat_relay (sender_id, recipient_id, kind, ref_id)
                 VALUES (:me, :peer, \'read\', :ref)'
            )->execute(['me' => $meId, 'peer' => $peerId, 'ref' => $upTo]);
            ts_chat_json('success', 'Read receipt sent.', []);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat read receipt failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not send the read receipt.', [], 500);
        }
        break;
    }

    // -----------------------------------------------------------------------
    // Typing indicator
    // -----------------------------------------------------------------------
    case 'typing': {
        ts_chat_require_post('typing');
        $body     = ts_chat_read_body();
        $peerId   = ts_chat_peer_id($body);
        $isTyping = !isset($body['is_typing']) || (bool) $body['is_typing'];

        if ($peerId === $meId) {
            ts_chat_json('success', 'Ignored.', []);
        }

        try {
            if ($isTyping) {
                $pdo->prepare(
                    'INSERT INTO chat_typing (user_id, peer_id) VALUES (:me, :peer)
                     ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP'
                )->execute(['me' => $meId, 'peer' => $peerId]);
            } else {
                $pdo->prepare('DELETE FROM chat_typing WHERE user_id = :me AND peer_id = :peer')
                    ->execute(['me' => $meId, 'peer' => $peerId]);
            }
        } catch (Throwable $e) {
            error_log('[TheStudent] chat typing write failed: ' . $e->getMessage());
        }

        ts_chat_json('success', 'OK', []);
        break;
    }

    // -----------------------------------------------------------------------
    // Chat safety notice acknowledgement
    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Push token registration (Firebase Cloud Messaging)
    // -----------------------------------------------------------------------
    case 'register_device': {
        ts_chat_require_post('register_device');
        $body     = ts_chat_read_body();
        $fcmToken = isset($body['fcm_token']) && is_string($body['fcm_token'])
            ? trim($body['fcm_token'])
            : '';
        $platform = isset($body['platform']) && is_string($body['platform'])
            ? substr(preg_replace('/[^a-z]/', '', strtolower($body['platform'])) ?? 'android', 0, 16)
            : 'android';

        if ($fcmToken === '' || strlen($fcmToken) > 255) {
            ts_chat_json('error', 'A valid push token is required.', [], 422);
        }

        try {
            // One row per install: the same phone signing in as another student
            // takes the token over instead of leaving a stale owner behind.
            $stmt = $pdo->prepare(
                'INSERT INTO chat_devices (user_id, token, platform)
                 VALUES (:user, :token, :platform)
                 ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), platform = VALUES(platform)'
            );
            $stmt->execute([
                'user'     => $meId,
                'token'    => $fcmToken,
                'platform' => $platform,
            ]);
            ts_chat_json('success', 'Push enabled.', []);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat device registration failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not enable push right now.', [], 500);
        }
        break;
    }

    case 'unregister_device': {
        ts_chat_require_post('unregister_device');
        try {
            // Signing out must stop pushes to this phone.
            $pdo->prepare('DELETE FROM chat_devices WHERE user_id = :user')
                ->execute(['user' => $meId]);
            ts_chat_json('success', 'Push disabled for this account.', []);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat device unregister failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not disable push right now.', [], 500);
        }
        break;
    }

    case 'ack_safety': {
        ts_chat_require_post('ack_safety');
        try {
            $pdo->prepare('UPDATE users SET chat_safety_ack_at = NOW() WHERE id = :id')
                ->execute(['id' => $meId]);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat safety ack failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not save that right now. Please try again.', [], 500);
        }
        ts_chat_json('success', 'Thanks for keeping chats safe.', ['safety_acknowledged' => true]);
        break;
    }

    // -----------------------------------------------------------------------
    // Presence heartbeat
    // -----------------------------------------------------------------------
    case 'heartbeat': {
        ts_chat_require_post('heartbeat');
        // ts_chat_require_user() already refreshed last_seen_at.
        ts_chat_json('success', 'Presence updated.', [
            // Same definition as `threads`, so the counter cannot flicker.
            'online_count' => ts_chat_online_peer_count($pdo, $meId),
            'server_epoch' => $now,
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // Publish the end-to-end encryption identity
    // -----------------------------------------------------------------------
    case 'set_key': {
        ts_chat_require_post('set_key');
        $body = ts_chat_read_body();

        $publicKey = isset($body['public_key']) && is_string($body['public_key'])
            ? trim($body['public_key'])
            : '';
        // A base64 SPKI P-256 key is ~124 characters; allow headroom but never
        // store something that is obviously not a key.
        if ($publicKey === '' || strlen($publicKey) > 512
            || !preg_match('#^[A-Za-z0-9+/=]+$#', $publicKey)) {
            ts_chat_json('error', 'A valid public key is required.', [], 422);
        }

        // Never silently replace a published key: messages other students
        // already encrypted to the old key would become unreadable.
        $currentKey   = $me['chat_public_key'] ?? null;
        $allowReplace = !empty($body['allow_replace']);
        if (!empty($currentKey) && $currentKey !== $publicKey && !$allowReplace) {
            ts_chat_json(
                'error',
                'This account already has an encryption key.',
                ['existing_key' => $currentKey],
                409
            );
        }

        $pdo->prepare('UPDATE users SET chat_public_key = :pk WHERE id = :id')
            ->execute(['pk' => $publicKey, 'id' => $meId]);

        ts_chat_json('success', 'Encryption key published.', ['public_key' => $publicKey]);
        break;
    }

    // -----------------------------------------------------------------------
    // Ratings — a star can only come from real help (or a long conversation)
    // -----------------------------------------------------------------------
    case 'rate_state': {
        $peerId = ts_chat_peer_id($_GET);
        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot rate yourself.', [], 422);
        }

        $help = ts_chat_rating_closed_help($pdo, $meId, $peerId);
        $kind = $help ? 'help' : 'talk';
        $subjectCode = $help ? (string) ($help['subject_code'] ?? '') : 'talk';
        $subjectLabel = $help ? (string) ($help['subject_label'] ?? '') : 'Just talking';

        $out = [
            'peer_id'            => $peerId,
            'kind'               => $kind,
            'subject_code'       => $subjectCode,
            'subject_label'      => $subjectLabel,
            'help_request_id'    => $help ? (int) $help['id'] : null,
            // 'help' means the card can open now. 'talk' means the phone decides:
            // the server never sees messages, so it only checks the attested
            // count when the rating is actually sent.
            'can_rate_now'       => $help ? true : false,
            'talk_min_messages'  => TS_CHAT_RATING_TALK_MIN_EACH,
            'tags_allowed'       => ts_chat_rating_tag_list(),
            'max_tags'           => TS_CHAT_RATING_MAX_TAGS,
            'max_note'           => TS_CHAT_RATING_MAX_NOTE,
            'edit_window_sec'    => TS_CHAT_RATING_EDIT_SEC,
            'existing'           => null,
        ];

        $stmt = $pdo->prepare(
            ts_chat_rating_select() .
            ' WHERE rater_id = :me AND ratee_id = :peer AND subject_code = :sc LIMIT 1'
        );
        $stmt->execute(['me' => $meId, 'peer' => $peerId, 'sc' => $subjectCode]);
        $mine = $stmt->fetch();
        if ($mine) {
            $out['existing'] = ts_chat_rating_public($mine, $meId, $now);
        }

        ts_chat_json('success', 'Rating state loaded.', $out);
        break;
    }

    case 'rate': {
        ts_chat_require_post('rate');
        $body = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);
        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot rate yourself.', [], 422);
        }

        $stars = (int) ($body['stars'] ?? 0);
        if ($stars < 1 || $stars > 5) {
            ts_chat_json('error', 'Pick between 1 and 5 stars.', [], 422);
        }

        $allowed = ts_chat_rating_tag_list();
        $tags = [];
        $rawTags = $body['tags'] ?? [];
        if (is_array($rawTags)) {
            foreach ($rawTags as $tag) {
                $tag = strtolower(trim((string) $tag));
                if ($tag !== '' && in_array($tag, $allowed, true) && !in_array($tag, $tags, true)) {
                    $tags[] = $tag;
                }
            }
        }
        if (count($tags) > TS_CHAT_RATING_MAX_TAGS) {
            $tags = array_slice($tags, 0, TS_CHAT_RATING_MAX_TAGS);
        }

        $note = isset($body['note']) ? trim((string) $body['note']) : '';
        if (mb_strlen($note, 'UTF-8') > TS_CHAT_RATING_MAX_NOTE) {
            ts_chat_json(
                'error',
                'Keep the note to ' . TS_CHAT_RATING_MAX_NOTE . ' characters.',
                [],
                422
            );
        }

        $help = ts_chat_rating_closed_help($pdo, $meId, $peerId);
        if ($help) {
            $kind         = 'help';
            $helpId       = (int) $help['id'];
            $subjectCode  = (string) ($help['subject_code'] ?? '');
            $subjectLabel = (string) ($help['subject_label'] ?? '');
        } else {
            // "Just talking": no help was ever closed. The phone is the only
            // place that knows how many messages went each way, so it attests
            // the count and anything under the floor is refused.
            $attested = (int) ($body['talk_messages_each_way'] ?? 0);
            if ($attested < TS_CHAT_RATING_TALK_MIN_EACH) {
                ts_chat_json(
                    'error',
                    'Ratings come from closed help, or from a chat with at least '
                        . TS_CHAT_RATING_TALK_MIN_EACH . ' messages each way.',
                    ['talk_min_messages' => TS_CHAT_RATING_TALK_MIN_EACH],
                    403
                );
            }
            $kind         = 'talk';
            $helpId       = null;
            $subjectCode  = 'talk';
            $subjectLabel = 'Just talking';
        }

        $stmt = $pdo->prepare(
            ts_chat_rating_select() .
            ' WHERE rater_id = :me AND ratee_id = :peer AND subject_code = :sc LIMIT 1'
        );
        $stmt->execute(['me' => $meId, 'peer' => $peerId, 'sc' => $subjectCode]);
        $existing = $stmt->fetch();

        $tagList = implode(',', $tags);

        if ($existing) {
            $rating = ts_chat_rating_public($existing, $meId, $now);
            if (!$rating['editable']) {
                ts_chat_json(
                    'error',
                    'You already rated this student in that subject, and the 24 hours to change it are over.',
                    ['locks_epoch' => $rating['locks_epoch']],
                    409
                );
            }
            $pdo->prepare(
                "UPDATE chat_ratings
                 SET stars = :stars, tags = :tags, note = :note, updated_at = NOW()
                 WHERE id = :id"
            )->execute([
                'stars' => $stars,
                'tags'  => $tagList,
                'note'  => $note,
                'id'    => (int) $existing['id'],
            ]);
            $savedId = (int) $existing['id'];
        } else {
            $pdo->prepare(
                "INSERT INTO chat_ratings
                    (rater_id, ratee_id, subject_code, subject_label, kind, stars, tags, note, help_request_id)
                 VALUES (:rater, :ratee, :sc, :sl, :kind, :stars, :tags, :note, :hid)"
            )->execute([
                'rater'  => $meId,
                'ratee'  => $peerId,
                'sc'     => $subjectCode,
                'sl'     => $subjectLabel,
                'kind'   => $kind,
                'stars'  => $stars,
                'tags'   => $tagList,
                'note'   => $note,
                'hid'    => $helpId,
            ]);
            $savedId = (int) $pdo->lastInsertId();
        }

        $fresh = $pdo->prepare(ts_chat_rating_select() . ' WHERE id = :id LIMIT 1');
        $fresh->execute(['id' => $savedId]);
        $freshRow = $fresh->fetch();

        ts_chat_json('success', 'Rating saved.', [
            'rating'  => $freshRow ? ts_chat_rating_public($freshRow, $meId, $now) : null,
            'summary' => ts_chat_rating_summary($pdo, $peerId),
        ]);
        break;
    }

    case 'rating_summary': {
        $peerId = ts_chat_peer_id($_GET);

        // What this student has earned, plus what *I* have already said about
        // them, so the app can show "you rated 4 stars in Physics" instead of
        // offering the same card twice.
        $stmt = $pdo->prepare(
            ts_chat_rating_select() .
            ' WHERE rater_id = :me AND ratee_id = :peer ORDER BY id DESC'
        );
        $stmt->execute(['me' => $meId, 'peer' => $peerId]);
        $mine = [];
        foreach ($stmt->fetchAll() as $row) {
            $mine[] = ts_chat_rating_public($row, $meId, $now);
        }

        ts_chat_json('success', 'Ratings loaded.', [
            'peer_id'    => $peerId,
            'summary'    => ts_chat_rating_summary($pdo, $peerId),
            'my_ratings' => $mine,
        ]);
        break;
    }

    case 'my_ratings': {
        // The student's own page: their average and the private notes students
        // left for them. Nobody else can ever call this for somebody else.
        $stmt = $pdo->prepare(
            'SELECT r.stars, r.tags, r.note, r.subject_label, r.kind,
                    UNIX_TIMESTAMP(r.created_at) AS created_epoch,
                    u.full_name, u.profile_picture
             FROM chat_ratings r
             JOIN users u ON u.id = r.rater_id
             WHERE r.ratee_id = :me
             ORDER BY r.id DESC
             LIMIT 50'
        );
        $stmt->execute(['me' => $meId]);

        $notes = [];
        foreach ($stmt->fetchAll() as $row) {
            $note = trim((string) ($row['note'] ?? ''));
            if ($note === '') {
                continue;
            }
            $firstName = trim(explode(' ', trim((string) ($row['full_name'] ?? '')))[0] ?? '');
            $notes[] = [
                'from'           => $firstName !== '' ? $firstName : 'A student',
                'stars'          => (int) $row['stars'],
                'tags'           => array_values(array_filter(array_map(
                    'trim',
                    explode(',', (string) ($row['tags'] ?? ''))
                ))),
                'note'           => $note,
                'subject_label'  => (string) ($row['subject_label'] ?? ''),
                'kind'           => (string) ($row['kind'] ?? 'help'),
                'created_epoch'  => (int) ($row['created_epoch'] ?? 0),
            ];
        }

        ts_chat_json('success', 'Your ratings loaded.', [
            'summary' => ts_chat_rating_summary($pdo, $meId),
            'notes'   => $notes,
        ]);
        break;
    }

    default:
        ts_chat_json('error', 'Unknown action.', [
            'actions' => [
                'threads', 'profile', 'set_purpose',
                'help_request', 'help_state',
                'help_accept', 'help_close', 'help_cancel',
                'rate', 'rate_state', 'rating_summary', 'my_ratings',
                'inbox', 'send', 'read', 'typing', 'ack_safety',
                'heartbeat', 'set_key', 'version',
            ],
        ], 400);
}
} catch (Throwable $e) {
    // A JSON answer with something to act on beats a bare 500: the app shows
    // this message, the log keeps the detail, and the deploy reference lets us
    // find it afterwards.
    $ref = bin2hex(random_bytes(4));
    error_log('[TheStudent][chat][' . $ref . '] action=' . $action . ' failed: ' . $e->getMessage());
    ts_chat_json(
        'error',
        'The chat server could not complete that (' . $action . '). Reference: ' . $ref,
        ['ref' => $ref, 'action' => $action],
        500
    );
}
