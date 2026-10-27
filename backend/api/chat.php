<?php
/**
 * TheStudent - Student Chat API (v1.0.0)
 * ======================================
 *
 * Endpoint: /api/chat.php?action=<action>
 *
 * Every action requires `Authorization: Bearer <session token>` issued by
 * api/auth.php. Chat is student-to-student only: there are no public rooms,
 * no group chats and no anonymous senders.
 *
 * Actions
 * -------
 *   GET  threads                        Directory of registered students +
 *                                       presence, last message, unread count.
 *   GET  messages&peer_id=&after_id=    Message history (incremental when
 *                                       after_id is supplied).
 *   POST send        {peer_id, body}
 *   POST typing      {peer_id, is_typing?}
 *   POST read        {peer_id}
 *   POST clear       {peer_id}
 *   POST ack_safety                     First-visit chat safety notice.
 *   POST heartbeat                      Presence ping (every ~20 s).
 *   GET  version                        Deploy fingerprint.
 *
 * Privacy model
 * -------------
 *  • Only `users.full_name` and the profile picture are ever exposed. E-mail,
 *    phone, institution and any other profile column stays server-side.
 *  • Presence is coarse on purpose: "online" (active in the last 45 s) or a
 *    "last seen …" label. No IP addresses, no device data, no read receipts
 *    beyond `read_at` for the sender's own messages.
 *
 * Security properties
 * -------------------
 *  • Bearer token only (never a query parameter), stored as a SHA-256 digest
 *    by auth.php, so a database dump cannot be replayed.
 *  • Every query is parameterised; peer ids are validated as integers.
 *  • Per-user send throttle, body length cap and control-character stripping.
 *  • `users.is_active` / `is_email_verified` are enforced on every call.
 *  • All responses are `Cache-Control: no-store`.
 *  • The schema is self-healing (tables + the two `users` columns are created
 *    on first use), so a fresh deployment works without phpMyAdmin.
 */

declare(strict_types=1);

require_once __DIR__ . '/../error_handlers.php';
ts_install_error_handlers();

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/_avatar.php';   // ts_avatar_public_url()

// ---------------------------------------------------------------------------
// Tunables
// ---------------------------------------------------------------------------
const TS_CHAT_API_VERSION          = '1.0.0';
const TS_CHAT_ONLINE_WINDOW_SEC    = 45;    // "online" = seen in the last N s
const TS_CHAT_TYPING_WINDOW_SEC    = 6;     // typing indicator lifetime
const TS_CHAT_MAX_BODY_CHARS       = 2000;
const TS_CHAT_MAX_MESSAGES_PER_MIN = 30;    // per student
const TS_CHAT_PAGE_SIZE            = 120;   // history window per request
const TS_CHAT_MAX_BODY_BYTES       = 16384;

// ---------------------------------------------------------------------------
// Bootstrap
// ---------------------------------------------------------------------------
ts_apply_security_headers();
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

if (ts_requires_https() && !ts_is_https()) {
    ts_chat_json('error', 'Secure connection required.', [], 400);
}

$pdo      = getDbConnection();
$action   = isset($_GET['action']) ? trim((string) $_GET['action']) : '';

// ---------------------------------------------------------------------------
// Response helper
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
        'id'                 => (int) $row['id'],
        'name'               => (string) ($row['full_name'] ?? 'Student'),
        'avatar_url'         => ts_avatar_public_url($row['profile_picture'] ?? null),
        'is_online'          => $isOnline,
        'last_seen_at'       => $lastSeen > 0 ? gmdate('c', $lastSeen) : null,
        'last_seen_seconds'  => $lastSeen > 0 ? max(0, $now - $lastSeen) : null,
    ];
}

function ts_chat_safety_acknowledged(array $user): bool
{
    return !empty($user['chat_safety_ack_at']);
}

/**
 * Create the chat schema when it is missing. Safe to run on every request.
 */
function ts_chat_ensure_schema(PDO $pdo): void
{
    static $done = false;
    if ($done) {
        return;
    }
    $done = true;

    try {
        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_messages` (
                `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                `sender_id` INT UNSIGNED NOT NULL,
                `recipient_id` INT UNSIGNED NOT NULL,
                `body` TEXT NOT NULL,
                `read_at` DATETIME NULL DEFAULT NULL,
                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX `idx_chat_pair` (`sender_id`, `recipient_id`, `id`),
                INDEX `idx_chat_recipient_unread` (`recipient_id`, `read_at`),
                CONSTRAINT `fk_chat_sender` FOREIGN KEY (`sender_id`)
                    REFERENCES `users` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_chat_recipient` FOREIGN KEY (`recipient_id`)
                    REFERENCES `users` (`id`) ON DELETE CASCADE
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

        $pdo->exec(
            "CREATE TABLE IF NOT EXISTS `chat_clears` (
                `user_id` INT UNSIGNED NOT NULL,
                `peer_id` INT UNSIGNED NOT NULL,
                `cleared_seq` BIGINT UNSIGNED NOT NULL DEFAULT 0,
                `cleared_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (`user_id`, `peer_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );
    } catch (Throwable $e) {
        error_log('[TheStudent] chat schema creation failed: ' . $e->getMessage());
    }

    // Columns live on `users` so presence / the safety notice survive a chat
    // table being rebuilt.
    ts_ensure_column($pdo, 'users', 'last_seen_at', 'DATETIME NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'chat_safety_ack_at', 'DATETIME NULL DEFAULT NULL');
}

// ---------------------------------------------------------------------------
// Dispatch
// ---------------------------------------------------------------------------
if ($action === 'version') {
    ts_chat_json('success', 'TheStudent chat API', [
        'api_version' => TS_CHAT_API_VERSION,
        'php_version' => PHP_VERSION,
        'chat_php_modified' => gmdate('c', (int) filemtime(__FILE__)),
    ]);
}

ts_chat_ensure_schema($pdo);

$me    = ts_chat_require_user($pdo);
$meId  = (int) $me['id'];

// Single clock for presence + message timestamps: the database's.
try {
    $nowRow = $pdo->query('SELECT UNIX_TIMESTAMP(NOW()) AS now_epoch')->fetch();
    $now = (int) ($nowRow['now_epoch'] ?? time());
} catch (Throwable $e) {
    $now = time();
}

switch ($action) {

    // -----------------------------------------------------------------------
    // Student directory + conversation list
    // -----------------------------------------------------------------------
    case 'threads': {
        // Every active, verified student — full name and avatar only.
        $stmt = $pdo->prepare(
            'SELECT u.id, u.full_name, u.profile_picture, u.last_seen_at,
                    UNIX_TIMESTAMP(u.last_seen_at) AS last_seen_epoch,
                    COALESCE(cc.cleared_seq, 0) AS cleared_seq
             FROM users u
             LEFT JOIN chat_clears cc ON cc.user_id = :me AND cc.peer_id = u.id
             WHERE u.id <> :me2
               AND u.is_active = 1
               AND u.is_email_verified = 1
             ORDER BY u.full_name ASC
             LIMIT 500'
        );
        $stmt->execute(['me' => $meId, 'me2' => $meId]);
        $peers = $stmt->fetchAll();

        // Last message of every conversation the caller is part of
        // (positional placeholders: native prepares cannot reuse a named one).
        $lastById = [];
        try {
            $lastStmt = $pdo->prepare(
                'SELECT t.peer_id, t.id, t.sender_id, t.body, t.created_at, t.created_epoch
                 FROM (
                     SELECT m.id, m.sender_id,
                            IF(m.sender_id = ?, m.recipient_id, m.sender_id) AS peer_id,
                            m.body, m.created_at,
                            UNIX_TIMESTAMP(m.created_at) AS created_epoch
                     FROM chat_messages m
                     WHERE m.sender_id = ? OR m.recipient_id = ?
                 ) t
                 JOIN (
                     SELECT peer_id, MAX(id) AS max_id FROM (
                         SELECT m2.id,
                                IF(m2.sender_id = ?, m2.recipient_id, m2.sender_id) AS peer_id
                         FROM chat_messages m2
                         WHERE m2.sender_id = ? OR m2.recipient_id = ?
                     ) grouped
                     GROUP BY peer_id
                 ) latest ON latest.max_id = t.id'
            );
            $lastStmt->execute([$meId, $meId, $meId, $meId, $meId, $meId]);
            foreach ($lastStmt->fetchAll() as $row) {
                $lastById[(int) $row['peer_id']] = $row;
            }
        } catch (Throwable $e) {
            error_log('[TheStudent] chat last-message query failed: ' . $e->getMessage());
        }

        // Unread counts (respecting per-user "clear chat")
        $unreadBy = [];
        try {
            $unreadStmt = $pdo->prepare(
                'SELECT m.sender_id AS peer_id, COUNT(*) AS unread
                 FROM chat_messages m
                 LEFT JOIN chat_clears cc ON cc.user_id = ? AND cc.peer_id = m.sender_id
                 WHERE m.recipient_id = ?
                   AND m.read_at IS NULL
                   AND m.id > COALESCE(cc.cleared_seq, 0)
                 GROUP BY m.sender_id'
            );
            $unreadStmt->execute([$meId, $meId]);
            foreach ($unreadStmt->fetchAll() as $row) {
                $unreadBy[(int) $row['peer_id']] = (int) $row['unread'];
            }
        } catch (Throwable $e) {
            error_log('[TheStudent] chat unread query failed: ' . $e->getMessage());
        }

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

        $threads = [];
        $onlineCount = 0;
        foreach ($peers as $peerRow) {
            $peerId = (int) $peerRow['id'];
            $peer   = ts_chat_public_peer($peerRow, $now);
            if ($peer['is_online']) {
                $onlineCount++;
            }

            $cleared = (int) $peerRow['cleared_seq'];
            $last    = $lastById[$peerId] ?? null;
            $lastPayload = null;
            if ($last !== null && (int) $last['id'] > $cleared) {
                $sentAt = (int) $last['created_epoch'];
                $lastPayload = [
                    'body'      => (string) $last['body'],
                    'is_mine'   => (int) $last['sender_id'] === $meId,
                    'sent_at'   => $sentAt > 0 ? gmdate('c', $sentAt) : null,
                    'sent_epoch' => $sentAt > 0 ? $sentAt : 0,
                    'message_id' => (int) $last['id'],
                ];
            }

            $threads[] = [
                'peer'         => $peer,
                'is_typing'    => isset($typingBy[$peerId]),
                'unread_count' => $unreadBy[$peerId] ?? 0,
                'last_message' => $lastPayload,
            ];
        }

        // Conversations with actual history float to the top, newest first.
        usort($threads, function (array $a, array $b): int {
            $aTime = $a['last_message']['sent_epoch'] ?? 0;
            $bTime = $b['last_message']['sent_epoch'] ?? 0;
            if ($aTime === $bTime) {
                return strcasecmp($a['peer']['name'], $b['peer']['name']);
            }
            return $bTime <=> $aTime;
        });

        ts_chat_json('success', 'Chat directory loaded.', [
            'threads'            => $threads,
            'online_count'       => $onlineCount,
            'total_students'     => count($threads),
            'safety_acknowledged' => ts_chat_safety_acknowledged($me),
            'online_window_seconds' => TS_CHAT_ONLINE_WINDOW_SEC,
            'server_epoch'       => $now,
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // Message history
    // -----------------------------------------------------------------------
    case 'messages': {
        $peerId  = ts_chat_peer_id([]);
        $afterId = isset($_GET['after_id']) && is_numeric($_GET['after_id'])
            ? max(0, (int) $_GET['after_id'])
            : 0;

        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot chat with yourself.', [], 422);
        }

        $peerStmt = $pdo->prepare(
            'SELECT id, full_name, profile_picture, last_seen_at,
                    UNIX_TIMESTAMP(last_seen_at) AS last_seen_epoch
             FROM users WHERE id = :id AND is_active = 1 LIMIT 1'
        );
        $peerStmt->execute(['id' => $peerId]);
        $peerRow = $peerStmt->fetch();
        if (!$peerRow) {
            ts_chat_json('error', 'That student is no longer available.', [], 404);
        }

        $clearedStmt = $pdo->prepare(
            'SELECT cleared_seq FROM chat_clears WHERE user_id = :me AND peer_id = :peer LIMIT 1'
        );
        $clearedStmt->execute(['me' => $meId, 'peer' => $peerId]);
        $cleared         = (int) ($clearedStmt->fetchColumn() ?: 0);
        $clearedSeqBody  = max($cleared, $afterId);

        $stmt = $pdo->prepare(
            'SELECT id, sender_id, body, read_at, created_at,
                    UNIX_TIMESTAMP(created_at) AS created_epoch
             FROM chat_messages
             WHERE ((sender_id = :a AND recipient_id = :b)
                 OR (sender_id = :c AND recipient_id = :d))
               AND id > :after
             ORDER BY id DESC
             LIMIT ' . TS_CHAT_PAGE_SIZE
        );
        $stmt->execute([
            'a'     => $meId,
            'b'     => $peerId,
            'c'     => $peerId,
            'd'     => $meId,
            'after' => $clearedSeqBody,
        ]);
        $rows = array_reverse($stmt->fetchAll());

        $messages = [];
        foreach ($rows as $row) {
            $sentAt = (int) $row['created_epoch'];
            $messages[] = [
                'id'         => (int) $row['id'],
                'sender_id'  => (int) $row['sender_id'],
                'body'       => (string) $row['body'],
                'is_mine'    => (int) $row['sender_id'] === $meId,
                'sent_at'    => $sentAt > 0 ? gmdate('c', $sentAt) : null,
                'sent_epoch' => $sentAt > 0 ? $sentAt : 0,
                'is_read'    => $row['read_at'] !== null,
            ];
        }

        // Anything we just read from the peer is no longer unread.
        try {
            $pdo->prepare(
                'UPDATE chat_messages SET read_at = NOW()
                 WHERE recipient_id = :me AND sender_id = :peer AND read_at IS NULL'
            )->execute(['me' => $meId, 'peer' => $peerId]);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat read update failed: ' . $e->getMessage());
        }

        $typingStmt = $pdo->prepare(
            'SELECT 1 FROM chat_typing
             WHERE user_id = :peer AND peer_id = :me
               AND updated_at > (NOW() - INTERVAL ' . TS_CHAT_TYPING_WINDOW_SEC . ' SECOND)
             LIMIT 1'
        );
        $typingStmt->execute(['peer' => $peerId, 'me' => $meId]);

        ts_chat_json('success', 'Messages loaded.', [
            'peer'                => ts_chat_public_peer($peerRow, $now),
            'messages'            => $messages,
            'peer_is_typing'      => (bool) $typingStmt->fetchColumn(),
            'safety_acknowledged' => ts_chat_safety_acknowledged($me),
            'server_epoch'        => $now,
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // Send a message
    // -----------------------------------------------------------------------
    case 'send': {
        ts_chat_require_post('send');
        $body   = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);

        if ($peerId === $meId) {
            ts_chat_json('error', 'You cannot chat with yourself.', [], 422);
        }

        $text = isset($body['body']) && is_string($body['body']) ? $body['body'] : '';
        // Strip control characters (except newlines) and collapse runs of blank lines.
        $text = preg_replace('/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/u', '', $text) ?? '';
        $text = trim($text);
        $text = preg_replace("/\n{3,}/", "\n\n", $text) ?? $text;

        if ($text === '') {
            ts_chat_json('error', 'Type a message first.', [], 422);
        }
        if (mb_strlen($text, 'UTF-8') > TS_CHAT_MAX_BODY_CHARS) {
            ts_chat_json('error', 'That message is too long (max ' . TS_CHAT_MAX_BODY_CHARS . ' characters).', [], 422);
        }

        $peerStmt = $pdo->prepare(
            'SELECT id, full_name, profile_picture, last_seen_at,
                    UNIX_TIMESTAMP(last_seen_at) AS last_seen_epoch
             FROM users WHERE id = :id AND is_active = 1 AND is_email_verified = 1 LIMIT 1'
        );
        $peerStmt->execute(['id' => $peerId]);
        $peerRow = $peerStmt->fetch();
        if (!$peerRow) {
            ts_chat_json('error', 'That student is no longer available.', [], 404);
        }

        // Simple anti-spam: no more than N messages per rolling minute.
        $rateStmt = $pdo->prepare(
            'SELECT COUNT(*) FROM chat_messages
             WHERE sender_id = :me AND created_at > (NOW() - INTERVAL 60 SECOND)'
        );
        $rateStmt->execute(['me' => $meId]);
        if ((int) $rateStmt->fetchColumn() >= TS_CHAT_MAX_MESSAGES_PER_MIN) {
            header('Retry-After: 30');
            ts_chat_json('error', 'You are sending messages too quickly. Please slow down.', [], 429);
        }

        $insert = $pdo->prepare(
            'INSERT INTO chat_messages (sender_id, recipient_id, body) VALUES (:me, :peer, :body)'
        );
        $insert->execute(['me' => $meId, 'peer' => $peerId, 'body' => $text]);
        $messageId = (int) $pdo->lastInsertId();

        // Sending a message means the sender is no longer "typing".
        try {
            $pdo->prepare('DELETE FROM chat_typing WHERE user_id = :me AND peer_id = :peer')
                ->execute(['me' => $meId, 'peer' => $peerId]);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat typing clear failed: ' . $e->getMessage());
        }

        ts_chat_json('success', 'Message sent.', [
            'message' => [
                'id'         => $messageId,
                'sender_id'  => $meId,
                'body'       => $text,
                'is_mine'    => true,
                'sent_at'    => gmdate('c', $now),
                'sent_epoch' => $now,
                'is_read'    => false,
            ],
            'server_epoch' => $now,
        ]);
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
    // Mark the peer's messages as read
    // -----------------------------------------------------------------------
    case 'read': {
        ts_chat_require_post('read');
        $body   = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);

        try {
            $stmt = $pdo->prepare(
                'UPDATE chat_messages SET read_at = NOW()
                 WHERE recipient_id = :me AND sender_id = :peer AND read_at IS NULL'
            );
            $stmt->execute(['me' => $meId, 'peer' => $peerId]);
            ts_chat_json('success', 'Marked as read.', ['updated' => $stmt->rowCount()]);
        } catch (Throwable $e) {
            error_log('[TheStudent] chat read failed: ' . $e->getMessage());
            ts_chat_json('error', 'Could not update the conversation.', [], 500);
        }
        break;
    }

    // -----------------------------------------------------------------------
    // Clear this student's copy of a conversation
    // -----------------------------------------------------------------------
    case 'clear': {
        ts_chat_require_post('clear');
        $body   = ts_chat_read_body();
        $peerId = ts_chat_peer_id($body);

        $maxStmt = $pdo->prepare(
            'SELECT COALESCE(MAX(id), 0) FROM chat_messages
             WHERE (sender_id = :a AND recipient_id = :b)
                OR (sender_id = :c AND recipient_id = :d)'
        );
        $maxStmt->execute(['a' => $meId, 'b' => $peerId, 'c' => $peerId, 'd' => $meId]);
        $clearedSeq = (int) $maxStmt->fetchColumn();

        $pdo->prepare(
            'INSERT INTO chat_clears (user_id, peer_id, cleared_seq) VALUES (:me, :peer, :seq)
             ON DUPLICATE KEY UPDATE cleared_seq = GREATEST(cleared_seq, VALUES(cleared_seq)),
                                     cleared_at = CURRENT_TIMESTAMP'
        )->execute(['me' => $meId, 'peer' => $peerId, 'seq' => $clearedSeq]);

        // Read state follows the clear so unread badges disappear immediately.
        $pdo->prepare(
            'UPDATE chat_messages SET read_at = NOW()
             WHERE recipient_id = :me AND sender_id = :peer AND read_at IS NULL'
        )->execute(['me' => $meId, 'peer' => $peerId]);

        ts_chat_json('success', 'Conversation cleared.', ['cleared_seq' => $clearedSeq]);
        break;
    }

    // -----------------------------------------------------------------------
    // Chat safety notice acknowledgement
    // -----------------------------------------------------------------------
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
        $onlineStmt = $pdo->prepare(
            'SELECT COUNT(*) FROM users
             WHERE is_active = 1 AND is_email_verified = 1
               AND last_seen_at > (NOW() - INTERVAL ' . TS_CHAT_ONLINE_WINDOW_SEC . ' SECOND)'
        );
        $onlineStmt->execute();
        ts_chat_json('success', 'Presence updated.', [
            'online_count' => (int) $onlineStmt->fetchColumn(),
            'server_epoch' => $now,
        ]);
        break;
    }

    default:
        ts_chat_json('error', 'Unknown action.', [
            'actions' => [
                'threads', 'messages', 'send', 'typing', 'read', 'clear', 'ack_safety', 'heartbeat',
            ],
        ], 400);
}
