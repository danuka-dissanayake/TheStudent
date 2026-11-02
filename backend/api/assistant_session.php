<?php
/**
 * TheStudent - Assistant Single-Sign-On endpoint (one-time code exchange)
 * =====================================================================
 *
 * Endpoint: /api/assistant_session.php?action=<action>   (all actions are POST)
 *
 *   POST mint      [Bearer app session] {code_challenge, code_challenge_method, state}
 *                  -> {code, exchange_url, sub, expires_in}
 *   POST exchange  [HMAC: website server] {code, code_verifier, state}
 *                  -> {sub, name, email, email_verified, revoked_at}
 *   POST validate  [HMAC: website server] {sub}
 *                  -> {active, revoked_at}
 *   POST revoke    [Bearer app session] -> invalidates every assistant session
 *
 * Why this exists
 * ---------------
 * A student who registers/logs in inside the Android app must be signed in to
 * the AI assistant website without typing a password twice. Passing the app's
 * Bearer token (or a password) into a WebView would expose a long-lived, fully
 * privileged credential to web content and to anything that can read history,
 * logs or JavaScript. Instead:
 *
 *   1. The app proves its own session to THIS endpoint and receives a random
 *      256-bit, 90-second, single-use code. A PKCE challenge and a state hash
 *      are stored server-side.
 *   2. The app POSTs the code + verifier + state in a request body (never a URL)
 *      to the website's /api/app-auth route via WebView.postUrl.
 *   3. The website's SERVER exchanges the code with this endpoint over a signed
 *      HMAC request. The website then provisions/links by the opaque `sub`
 *      (users.id), rotates its session id, and sets its own
 *      `HttpOnly; Secure; SameSite=Lax; Path=/` session cookie.
 *   4. App sign-out calls `revoke`, so website sessions are invalidated too.
 *
 * Security properties
 * -------------------
 *  • The app session token never reaches the WebView or the website.
 *  • Codes are single-use, short-lived, bound to one user AND one app token,
 *    stored only as SHA-256 digests, and never logged.
 *  • PKCE (S256) means a stolen code is useless without the verifier; `state`
 *    binds this exchange to one app-side attempt.
 *  • Website calls are authenticated with a dedicated HMAC secret + timestamp +
 *    single-use nonce, NOT with APP_KEY and NOT with anything shipped in an APK.
 *  • `sub` is the immutable numeric users.id, never an email address (emails
 *    change; IDs do not). Never trust a userId supplied by JavaScript.
 *  • Every response is `Cache-Control: no-store` and `Referrer-Policy:
 *    no-referrer` so codes cannot leak into caches or Referer headers.
 *  • Fails closed: a missing/weak secret or a missing website origin makes mint
 *    return an error instead of silently building an insecure flow.
 *
 * Configuration (backend/config.secrets.php - server-side only, never in git)
 * --------------------------------------------------------------------------
 *   define('ASSISTANT_SSO_SECRET',  '<64+ random hex characters>');
 *   define('ASSISTANT_WEB_ORIGIN',  'https://your-production-domain.example');
 *   define('ASSISTANT_EXCHANGE_PATH', '/api/app-auth'); // optional
 *
 * The SAME ASSISTANT_SSO_SECRET must be set on the website's server. Generate
 * it with `php -r 'echo bin2hex(random_bytes(32)), PHP_EOL;'`.
 *
 * Schema is created lazily by this endpoint (see assistant_ensure_schema), so
 * no manual migration step is required.
 */

declare(strict_types=1);

require_once __DIR__ . '/../error_handlers.php';
ts_install_error_handlers();
require_once __DIR__ . '/../config.php';

// ---------------------------------------------------------------------------
// Response / request helpers (kept local so this endpoint never executes the
// auth.php router, which has its own dispatch side effects).
// ---------------------------------------------------------------------------

function assistant_json(string $status, string $message, array $data = [], int $code = 200): void
{
    http_response_code($code);
    if (!headers_sent()) {
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store, no-cache, must-revalidate, private');
        header('Pragma: no-cache');
        header('Referrer-Policy: no-referrer');
        header('X-Content-Type-Options: nosniff');
    }
    echo json_encode(array_merge([
        'status'  => $status,
        'message' => $message,
    ], $data), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit();
}

function assistant_require_post(string $action): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        if (!headers_sent()) {
            header('Allow: POST');
        }
        assistant_json('error', 'Only POST requests are allowed for ' . $action . '.', [], 405);
    }
}

function assistant_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || strlen($raw) > 8192) {
        assistant_json('error', 'Invalid request body.', [], 400);
    }
    $data = json_decode((string) $raw, true);
    if (!is_array($data)) {
        assistant_json('error', 'Expected a JSON object.', [], 400);
    }
    return $data;
}

function assistant_bearer(): string
{
    $header = '';
    foreach (['HTTP_AUTHORIZATION', 'REDIRECT_HTTP_AUTHORIZATION'] as $key) {
        if (!empty($_SERVER[$key])) {
            $header = (string) $_SERVER[$key];
            break;
        }
    }
    if ($header === '' && function_exists('getallheaders')) {
        foreach (getallheaders() as $name => $value) {
            if (strcasecmp((string) $name, 'Authorization') === 0) {
                $header = (string) $value;
                break;
            }
        }
    }
    // Same token shape auth.php accepts (opaque 256-bit hex session tokens).
    if (!preg_match('/^Bearer\s+([A-Fa-f0-9]{32,256})$/', trim($header), $matches)) {
        assistant_json('error', 'Sign in to the app first.', [], 401);
    }
    return $matches[1];
}

/**
 * Validate the app session exactly like auth.php does: opaque token, SHA-256
 * digest at rest, joined to an active user, and bound to this token id.
 */
function assistant_session(PDO $pdo, string $token): array
{
    $stmt = $pdo->prepare(
        'SELECT u.id, u.full_name, u.email, u.is_active, u.is_email_verified,
                t.id AS token_id
           FROM user_tokens t
           JOIN users u ON u.id = t.user_id
          WHERE t.token = :token AND t.expires_at > NOW()
          LIMIT 1'
    );
    $stmt->execute([':token' => hash('sha256', $token)]);
    $row = $stmt->fetch();
    if (!$row || (int) $row['is_active'] !== 1 || (int) $row['is_email_verified'] !== 1) {
        assistant_json('error', 'Your app session has expired. Please sign in again.', [], 401);
    }
    return $row;
}

function assistant_secret(): string
{
    $secret = defined('ASSISTANT_SSO_SECRET') ? (string) constant('ASSISTANT_SSO_SECRET') : '';
    if (strlen($secret) < 32) {
        error_log('[TheStudent] assistant SSO is not configured (missing/weak ASSISTANT_SSO_SECRET).');
        assistant_json('error', 'Assistant sign-in is not configured on this server.', [], 503);
    }
    return $secret;
}

function assistant_web_origin(): string
{
    $origin = defined('ASSISTANT_WEB_ORIGIN') ? trim((string) constant('ASSISTANT_WEB_ORIGIN'), " \t\n\r\0\x0B/") : '';
    if ($origin === '' || !preg_match('#^https://[a-z0-9.\-]+(:\d{2,5})?$#i', $origin)) {
        error_log('[TheStudent] assistant SSO is not configured (missing/https-invalid ASSISTANT_WEB_ORIGIN).');
        assistant_json('error', 'Assistant sign-in is not configured on this server.', [], 503);
    }
    return $origin;
}

function assistant_ensure_schema(PDO $pdo): void
{
    static $ready = false;
    if ($ready) {
        return;
    }
    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS `assistant_exchange_codes` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `user_id` INT UNSIGNED NOT NULL,
            `token_id` INT UNSIGNED NOT NULL,
            `code_hash` VARCHAR(128) NOT NULL,
            `code_challenge` VARCHAR(128) NOT NULL,
            `state_hash` VARCHAR(128) NOT NULL,
            `expires_at` DATETIME NOT NULL,
            `used_at` DATETIME NULL DEFAULT NULL,
            `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `ip_address` VARCHAR(45) NULL DEFAULT NULL,
            UNIQUE KEY `uniq_assistant_code_hash` (`code_hash`),
            KEY `idx_assistant_user` (`user_id`),
            KEY `idx_assistant_expiry` (`expires_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    );
    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS `assistant_sso_nonces` (
            `nonce_hash` VARCHAR(128) NOT NULL PRIMARY KEY,
            `expires_at` DATETIME NOT NULL,
            KEY `idx_assistant_nonce_expiry` (`expires_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    );
    if (function_exists('ts_ensure_column')) {
        ts_ensure_column($pdo, 'users', 'assistant_revoked_at', 'DATETIME NULL DEFAULT NULL');
    }
    $pdo->exec('DELETE FROM `assistant_sso_nonces` WHERE `expires_at` < NOW()');
    $pdo->exec('DELETE FROM `assistant_exchange_codes` WHERE `expires_at` < DATE_SUB(NOW(), INTERVAL 1 DAY)');
    $ready = true;
}

/**
 * Authenticate a server-to-server call from the website.
 *
 * Canonical string (documented for the website implementation):
 *   "POST\n/api/assistant_session.php?action=<action>\n<timestamp>\n<nonce>\n" .
 *   hash('sha256', $rawRequestBody)
 *
 * Headers:
 *   X-TheStudent-Timestamp   unix seconds, within +/- 5 minutes
 *   X-TheStudent-Nonce       random, single use
 *   X-TheStudent-Signature   lowercase hex HMAC-SHA256(canonical, secret)
 */
function assistant_verify_hmac(string $rawBody, string $action): void
{
    $secret = assistant_secret();
    $timestamp = trim((string) ($_SERVER['HTTP_X_THESTUDENT_TIMESTAMP'] ?? ''));
    $nonce = trim((string) ($_SERVER['HTTP_X_THESTUDENT_NONCE'] ?? ''));
    $signature = strtolower(trim((string) ($_SERVER['HTTP_X_THESTUDENT_SIGNATURE'] ?? '')));

    if (!ctype_digit($timestamp) || abs(time() - (int) $timestamp) > 300) {
        assistant_json('error', 'Expired or missing request signature.', [], 401);
    }
    if (!preg_match('/^[A-Za-z0-9\-_]{16,128}$/', $nonce)) {
        assistant_json('error', 'Invalid request signature.', [], 401);
    }
    if (!preg_match('/^[a-f0-9]{64}$/', $signature)) {
        assistant_json('error', 'Invalid request signature.', [], 401);
    }

    $canonical = "POST\n/api/assistant_session.php?action=" . $action . "\n"
        . $timestamp . "\n" . $nonce . "\n" . hash('sha256', $rawBody);
    $expected = hash_hmac('sha256', $canonical, $secret);
    if (!hash_equals($expected, $signature)) {
        assistant_json('error', 'Invalid request signature.', [], 401);
    }

    // Replay protection: the same signed request can never be used twice.
    $pdo = getDbConnection();
    assistant_ensure_schema($pdo);
    $stmt = $pdo->prepare(
        'INSERT IGNORE INTO `assistant_sso_nonces` (`nonce_hash`, `expires_at`) VALUES (:hash, :expires)'
    );
    $stmt->execute([
        ':hash'    => hash('sha256', $nonce),
        ':expires' => date('Y-m-d H:i:s', time() + 600),
    ]);
    if ($stmt->rowCount() !== 1) {
        assistant_json('error', 'Request already used.', [], 401);
    }
}

function assistant_b64url(string $binary): string
{
    return rtrim(strtr(base64_encode($binary), '+/', '-_'), '=');
}

// ---------------------------------------------------------------------------
// Dispatch
// ---------------------------------------------------------------------------

$action = (string) ($_GET['action'] ?? '');
ts_apply_security_headers();
assistant_require_post($action);

if (ts_requires_https() && !ts_is_https()) {
    assistant_json('error', 'A secure HTTPS connection is required.', [], 403);
}

$pdo = getDbConnection();
assistant_ensure_schema($pdo);

switch ($action) {

    // -----------------------------------------------------------------------
    // 1. App -> backend: mint the one-time code.
    // -----------------------------------------------------------------------
    case 'mint': {
        $token = assistant_bearer();
        $user = assistant_session($pdo, $token);
        $body = assistant_body();

        $challenge = (string) ($body['code_challenge'] ?? '');
        $state = (string) ($body['state'] ?? '');
        if ((string) ($body['code_challenge_method'] ?? '') !== 'S256'
            || !preg_match('/^[A-Za-z0-9\-._~]{43,128}$/', $challenge)
            || !preg_match('/^[A-Za-z0-9\-._~]{16,128}$/', $state)) {
            assistant_json('error', 'Invalid sign-in request parameters.', [], 400);
        }

        $origin = assistant_web_origin();
        $path = defined('ASSISTANT_EXCHANGE_PATH')
            ? (string) constant('ASSISTANT_EXCHANGE_PATH')
            : '/api/app-auth';
        if ($path === '' || $path[0] !== '/') {
            $path = '/api/app-auth';
        }

        // Brute-force / abuse throttle: at most 5 codes per user per minute.
        $throttle = $pdo->prepare(
            'SELECT COUNT(*) FROM `assistant_exchange_codes`
              WHERE `user_id` = :uid AND `created_at` > DATE_SUB(NOW(), INTERVAL 1 MINUTE)'
        );
        $throttle->execute([':uid' => $user['id']]);
        if ((int) $throttle->fetchColumn() >= 5) {
            assistant_json('error', 'Too many sign-in attempts. Try again in a minute.', [], 429);
        }

        $code = bin2hex(random_bytes(32));
        $insert = $pdo->prepare(
            'INSERT INTO `assistant_exchange_codes`
                (`user_id`, `token_id`, `code_hash`, `code_challenge`, `state_hash`,
                 `expires_at`, `ip_address`)
             VALUES (:uid, :tid, :code_hash, :challenge, :state_hash, :expires, :ip)'
        );
        $insert->execute([
            ':uid'        => $user['id'],
            ':tid'        => $user['token_id'],
            ':code_hash'  => hash('sha256', $code),
            ':challenge'  => $challenge,
            ':state_hash' => hash('sha256', $state),
            ':expires'    => date('Y-m-d H:i:s', time() + 90),
            ':ip'         => ts_client_ip(),
        ]);

        // Never log $code. This is the only moment it is transmitted, in a
        // no-store JSON body over HTTPS to the authenticated app.
        assistant_json('success', 'Assistant sign-in code issued.', [
            'code'         => $code,
            'exchange_url' => $origin . $path,
            'sub'          => (string) $user['id'],
            'expires_in'   => 90,
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // 2. Website server -> backend: exchange code for the student's identity.
    // -----------------------------------------------------------------------
    case 'exchange': {
        $rawBody = (string) file_get_contents('php://input');
        assistant_verify_hmac($rawBody, 'exchange');
        $body = json_decode($rawBody, true);
        if (!is_array($body)) {
            assistant_json('error', 'Expected a JSON object.', [], 400);
        }

        $code = (string) ($body['code'] ?? '');
        $verifier = (string) ($body['code_verifier'] ?? '');
        $state = (string) ($body['state'] ?? '');
        if (!preg_match('/^[a-f0-9]{64}$/', $code)
            || !preg_match('/^[A-Za-z0-9\-._~]{43,128}$/', $verifier)
            || !preg_match('/^[A-Za-z0-9\-._~]{16,128}$/', $state)) {
            assistant_json('error', 'Invalid sign-in code.', [], 400);
        }

        $lookup = $pdo->prepare(
            'SELECT * FROM `assistant_exchange_codes`
              WHERE `code_hash` = :hash AND `used_at` IS NULL AND `expires_at` > NOW()
              LIMIT 1'
        );
        $lookup->execute([':hash' => hash('sha256', $code)]);
        $row = $lookup->fetch();
        if (!$row) {
            assistant_json('error', 'Sign-in code is invalid or has expired.', [], 400);
        }

        // Verify BOTH PKCE and state in constant time before consuming anything.
        if (!hash_equals((string) $row['code_challenge'], assistant_b64url(hash('sha256', $verifier, true)))
            || !hash_equals((string) $row['state_hash'], hash('sha256', $state))) {
            assistant_json('error', 'Sign-in code verification failed.', [], 400);
        }

        // Atomic single-use claim: two concurrent exchanges cannot both win.
        $claim = $pdo->prepare(
            'UPDATE `assistant_exchange_codes` SET `used_at` = NOW()
              WHERE `id` = :id AND `used_at` IS NULL'
        );
        $claim->execute([':id' => $row['id']]);
        if ($claim->rowCount() !== 1) {
            assistant_json('error', 'Sign-in code has already been used.', [], 400);
        }

        // Re-check the app session: if the student signed out in the 90s window,
        // the code must not create a website session.
        $session = $pdo->prepare(
            'SELECT u.id, u.full_name, u.email, u.is_active, u.is_email_verified, u.assistant_revoked_at
               FROM users u
               JOIN user_tokens t ON t.user_id = u.id
              WHERE u.id = :uid AND t.id = :tid AND t.expires_at > NOW()
              LIMIT 1'
        );
        $session->execute([':uid' => $row['user_id'], ':tid' => $row['token_id']]);
        $user = $session->fetch();
        if (!$user || (int) $user['is_active'] !== 1 || (int) $user['is_email_verified'] !== 1) {
            assistant_json('error', 'The app session is no longer active.', [], 401);
        }

        assistant_json('success', 'Assistant identity verified.', [
            'sub'            => (string) $user['id'],
            'name'           => (string) ($user['full_name'] ?? 'Student'),
            'email'          => (string) ($user['email'] ?? ''),
            'email_verified' => 1,
            'revoked_at'     => $user['assistant_revoked_at'],
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // 3. Website server -> backend: check whether a `sub` is still signed in.
    // -----------------------------------------------------------------------
    case 'validate': {
        $rawBody = (string) file_get_contents('php://input');
        assistant_verify_hmac($rawBody, 'validate');
        $body = json_decode($rawBody, true);
        $sub = is_array($body) ? (string) ($body['sub'] ?? '') : '';
        if (!ctype_digit($sub) || strlen($sub) > 20) {
            assistant_json('error', 'Invalid subject.', [], 400);
        }

        $stmt = $pdo->prepare(
            'SELECT `id`, `is_active`, `assistant_revoked_at` FROM `users` WHERE `id` = :uid LIMIT 1'
        );
        $stmt->execute([':uid' => $sub]);
        $user = $stmt->fetch();
        assistant_json('success', 'Session status.', [
            'active'     => $user !== false && (int) $user['is_active'] === 1,
            'revoked_at' => $user ? $user['assistant_revoked_at'] : null,
        ]);
        break;
    }

    // -----------------------------------------------------------------------
    // 4. App -> backend: sign-out revocation (best effort from the app).
    // -----------------------------------------------------------------------
    case 'revoke': {
        $token = assistant_bearer();
        $user = assistant_session($pdo, $token);
        $pdo->prepare('UPDATE `users` SET `assistant_revoked_at` = NOW() WHERE `id` = :uid')
            ->execute([':uid' => $user['id']]);
        $pdo->prepare('DELETE FROM `assistant_exchange_codes` WHERE `user_id` = :uid AND `used_at` IS NULL')
            ->execute([':uid' => $user['id']]);
        assistant_json('success', 'Assistant sessions signed out.');
        break;
    }

    default:
        assistant_json('error', 'Unknown assistant session action.', [], 404);
}
