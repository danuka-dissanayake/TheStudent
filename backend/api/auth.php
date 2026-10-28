<?php
/**
 * TheStudent - Authentication & Session API (v3, hardened)
 * =======================================================
 *
 * Endpoint: /api/auth.php?action=<action>
 *
 * Actions
 * -------
 *   POST register                {full_name, email, password, target_exam?}
 *   POST login                   {email, password}
 *   POST verify_otp              {email, otp_code}
 *   POST resend_otp              {email}
 *   POST request_password_reset  {email}
 *   POST reset_password          {reset_token, new_password}
 *   POST change_password         {current_password, new_password}   [Bearer]
 *   POST google_login            {id_token, target_exam?}
 *   POST complete_profile        multipart or JSON {full_name, phone, target_exam,
 *                                institution, stream_or_degree, password?, avatar?}
 *   GET  me                      [Bearer]  (alias: profile)
 *   POST delete_account          {password?}                        [Bearer]
 *   POST logout                  [Bearer]
 *   POST logout_all              [Bearer]
 *
 * Security properties
 * -------------------
 *  • No hard-coded administrator credentials or client-side bypasses. Admin is
 *    a server-side role (`users.role`) granted via ADMIN_EMAILS on first verify.
 *  • Session tokens are opaque 256-bit values; only their SHA-256 digest is
 *    stored, so a database leak cannot be replayed against the API.
 *  • Tokens are accepted ONLY via `Authorization: Bearer` - never query params
 *    (URLs end up in access logs, browser history and Referer headers).
 *  • OTP codes are stored as HMAC-SHA256(APP_KEY) and compared in constant time.
 *  • OTP attempts are incremented atomically (no race window) and capped.
 *  • Per-email and per-IP throttles on OTP issuance, verification and login.
 *  • Account enumeration resistant login (dummy hash verification) and generic
 *    password-reset responses.
 *  • `is_active` is enforced on every authentication and session lookup.
 *  • Bcrypt cost 12 with transparent re-hashing when the cost changes.
 *  • Every response is `Cache-Control: no-store`; HTTPS is required.
 */

declare(strict_types=1);

// Install the fatal-error safety net BEFORE anything else so that even an
// uncaught PDOException becomes JSON + a log line instead of an empty 500.
// (config.php does this too; requiring it here keeps this endpoint safe even
// when an older config.php is still deployed.)
require_once __DIR__ . '/../error_handlers.php';
ts_install_error_handlers();

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../smtp_mailer.php';
require_once __DIR__ . '/_avatar.php';   // hardened profile-picture pipeline

// ---------------------------------------------------------------------------
// Tunables
// ---------------------------------------------------------------------------
const TS_OTP_TTL_MINUTES            = 10;
const TS_OTP_MAX_ATTEMPTS           = 5;
const TS_OTP_RESEND_COOLDOWN_SEC    = 60;
const TS_OTP_EMAIL_MAX_PER_15MIN    = 3;
const TS_OTP_EMAIL_MAX_PER_HOUR     = 8;
const TS_OTP_IP_MAX_PER_15MIN       = 10;
const TS_SESSION_TTL_DAYS           = 30;
const TS_SESSION_RENEW_WHEN_DAYS    = 15;   // slide expiry when fewer days remain
const TS_MAX_SESSIONS_PER_USER      = 10;
const TS_LOGIN_MAX_PER_EMAIL_15MIN  = 5;
const TS_LOGIN_MAX_PER_IP_15MIN     = 20;
const TS_BCRYPT_COST                = 12;
const TS_PASSWORD_MIN_LENGTH        = 8;
const TS_PASSWORD_MAX_BYTES         = 72;   // bcrypt hard limit
const TS_RESET_TOKEN_TTL_MINUTES    = 15;
const TS_MAX_BODY_BYTES             = 65536;
const TS_PROFILE_NAME_MIN           = 3;
const TS_PROFILE_NAME_MAX           = 80;
const TS_PROFILE_INSTITUTION_MIN    = 2;
const TS_PROFILE_INSTITUTION_MAX    = 120;
const TS_PROFILE_STREAM_MIN         = 2;
const TS_PROFILE_STREAM_MAX         = 150;

/** Curricula the profile screen offers. Anything else is rejected. */
const TS_ALLOWED_TARGET_EXAMS = ['G.C.E. O/L', 'G.C.E. A/L', 'University', 'Professional'];

/** Mediums the academic step offers. Free text here would split matching later. */
const TS_ALLOWED_MEDIUMS = ['Sinhala', 'Tamil', 'English'];

/** Bump whenever the API contract changes; exposed by ?action=version. */
const TS_API_VERSION                = '3.3.0';

/**
 * Pre-computed bcrypt (cost 12) hash used to equalise the response time of
 * logins for unknown e-mail addresses. It never matches any password.
 */
const TS_DUMMY_BCRYPT = '$2b$12$GzWllz3yIrCQigBsL2LStuX.RarOSsiry9XR6/pv9sd0lbIiA2g5i';

// ---------------------------------------------------------------------------
// Bootstrap
// ---------------------------------------------------------------------------
ts_apply_security_headers();
header('Content-Type: application/json; charset=utf-8');

if (ts_requires_https() && !ts_is_https()) {
    http_response_code(400);
    echo json_encode([
        'status'  => 'error',
        'message' => 'Secure connection required.',
    ]);
    exit();
}

$pdo      = getDbConnection();
$clientIp = ts_client_ip();
$action   = isset($_GET['action']) ? trim((string) $_GET['action']) : '';

/**
 * Deploy fingerprint — lets you confirm from a browser that the files you
 * uploaded are the files being executed (FTP half-uploads and cached copies are
 * the most common reason a fix "does not work").
 *
 *   GET /api/auth.php?action=version
 */
if ($action === 'version') {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode([
        'status'      => 'success',
        'api_version' => TS_API_VERSION,
        'php_version' => PHP_VERSION,
        'features'    => [
            'fatal_handler'  => function_exists('ts_install_error_handlers'),
            'schema_selfheal' => function_exists('ensureAuthSchema'),
            'secrets_outside_webroot' => !is_file(__DIR__ . '/../config.secrets.php'),
            'diag_token_length' => defined('DIAG_TOKEN') ? strlen(trim((string) DIAG_TOKEN)) : 0,
        ],
        'auth_php_modified' => gmdate('c', (int) filemtime(__FILE__)),
    ], JSON_PRETTY_PRINT);
    exit();
}

/**
 * Standard JSON envelope.
 */
function sendJsonResponse(string $status, string $message, array $data = [], int $statusCode = 200): void
{
    http_response_code($statusCode);
    if (!headers_sent()) {
        header('Cache-Control: no-store');
    }
    echo json_encode(array_merge([
        'status'  => $status,
        'message' => $message,
    ], $data), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit();
}

function requirePost(string $action): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        header('Allow: POST');
        sendJsonResponse('error', 'Only POST requests are allowed for ' . $action . '.', [], 405);
    }
}

/**
 * Read and validate the JSON request body.
 */
function readJsonBody(): array
{
    // multipart/form-data (profile photo upload): PHP has already parsed the
    // fields into $_POST and the file into $_FILES, and php://input is empty.
    // The JSON size limit does not apply — _avatar.php enforces its own.
    $contentType = strtolower((string) ($_SERVER['CONTENT_TYPE'] ?? ''));
    if (strpos($contentType, 'multipart/form-data') === 0) {
        return is_array($_POST) ? $_POST : [];
    }

    $length = (int) ($_SERVER['CONTENT_LENGTH'] ?? 0);
    if ($length > TS_MAX_BODY_BYTES) {
        sendJsonResponse('error', 'Request body too large.', [], 413);
    }

    $raw = file_get_contents('php://input');
    if ($raw === false || $raw === '') {
        return [];
    }
    if (strlen($raw) > TS_MAX_BODY_BYTES) {
        sendJsonResponse('error', 'Request body too large.', [], 413);
    }

    $decoded = json_decode($raw, true);
    if (is_array($decoded)) {
        return $decoded;
    }
    // Tolerate classic form posts (used by the PHP admin panel).
    if (!empty($_POST)) {
        return $_POST;
    }
    sendJsonResponse('error', 'Malformed JSON request body.', [], 400);
}

// ---------------------------------------------------------------------------
// Schema self-healing (idempotent)
// ---------------------------------------------------------------------------
/**
 * Widen the OTP column so it can hold HMAC digests, and make sure the
 * verification records can exist without a linked user (pending sign-ups).
 * Only alters the table when the current definition is insufficient, so this
 * is cheap to run on every request.
 */
function ensureAuthSchema(PDO $pdo): void
{
    // -----------------------------------------------------------------------
    // Create anything that is missing.
    //
    // A fresh DirectAdmin install often has the old `users` table but none of
    // the auth tables, and importing database_auth.sql needs phpMyAdmin access.
    // Without this, the very first sign-up died on the first query
    // (`login_attempts` for rate limiting) with a fatal PDOException -> an
    // empty 500 response -> "Request failed (500)" in the app.
    // -----------------------------------------------------------------------
    $tables = [
        'users' => "CREATE TABLE IF NOT EXISTS `users` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `google_id` VARCHAR(100) NULL UNIQUE,
            `email` VARCHAR(191) NOT NULL UNIQUE,
            `password_hash` VARCHAR(255) NULL,
            `full_name` VARCHAR(150) NOT NULL,
            `profile_picture` TEXT NULL,
            `auth_provider` ENUM('email','google') NOT NULL DEFAULT 'email',
            `target_exam` VARCHAR(100) NOT NULL DEFAULT 'G.C.E. A/L',
            `institution` VARCHAR(191) NULL,
            `stream_or_degree` VARCHAR(150) NULL,
            `role` ENUM('student','admin') NOT NULL DEFAULT 'student',
            `is_email_verified` TINYINT(1) NOT NULL DEFAULT 0,
            `is_active` TINYINT(1) NOT NULL DEFAULT 1,
            `last_login_ip` VARCHAR(45) NULL,
            `last_login_at` DATETIME NULL,
            `suspicious_flag` TINYINT(1) NOT NULL DEFAULT 0,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX `idx_email` (`email`),
            INDEX `idx_last_login_ip` (`last_login_ip`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        'email_verifications' => "CREATE TABLE IF NOT EXISTS `email_verifications` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `user_id` INT UNSIGNED NULL DEFAULT NULL,
            `email` VARCHAR(191) NOT NULL,
            `otp_code` VARCHAR(128) NOT NULL,
            `otp_type` ENUM('registration','unverified_account','login_challenge','ip_change','password_reset') NOT NULL DEFAULT 'registration',
            `pending_data` LONGTEXT NULL DEFAULT NULL,
            `requested_ip` VARCHAR(45) NULL DEFAULT NULL,
            `attempts` TINYINT UNSIGNED NOT NULL DEFAULT 0,
            `is_used` TINYINT(1) NOT NULL DEFAULT 0,
            `expires_at` DATETIME NOT NULL,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX `idx_email_otp` (`email`, `otp_code`, `is_used`),
            INDEX `idx_user_verification` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        'user_tokens' => "CREATE TABLE IF NOT EXISTS `user_tokens` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `user_id` INT UNSIGNED NOT NULL,
            `token` VARCHAR(128) NOT NULL UNIQUE,
            `last_used_at` DATETIME NULL DEFAULT NULL,
            `device_info` VARCHAR(255) NULL,
            `ip_address` VARCHAR(45) NULL,
            `expires_at` DATETIME NOT NULL,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX `idx_user_token` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        // Structured academic profile (step 1 of the help-matching work).
        // One row per subject, so the matching query can JOIN on overlap
        // instead of trying to parse free text.
        'user_subjects' => "CREATE TABLE IF NOT EXISTS `user_subjects` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `user_id` INT UNSIGNED NOT NULL,
            `subject_code` VARCHAR(64) NOT NULL,
            `subject_label` VARCHAR(120) NOT NULL,
            `exam_level` VARCHAR(16) NOT NULL DEFAULT 'AL',
            `bucket_no` TINYINT UNSIGNED NULL DEFAULT NULL,
            `position` TINYINT UNSIGNED NOT NULL DEFAULT 0,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY `uniq_user_subject` (`user_id`, `subject_code`),
            KEY `idx_subject_code` (`subject_code`),
            KEY `idx_subject_user` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        'login_attempts' => "CREATE TABLE IF NOT EXISTS `login_attempts` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `ip_address` VARCHAR(45) NOT NULL,
            `email` VARCHAR(191) NOT NULL,
            `attempted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX `idx_ip_attempt` (`ip_address`, `attempted_at`),
            INDEX `idx_email_attempt` (`email`, `attempted_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
    ];

    foreach ($tables as $tableName => $ddl) {
        try {
            $pdo->exec($ddl);
        } catch (Throwable $e) {
            // 1142 = no CREATE privilege on a shared host: report it clearly
            // instead of failing later with a confusing query error.
            error_log('[TheStudent][auth] could not create table ' . $tableName . ': ' . $e->getMessage());
        }
    }

    // Columns an older `users` table is likely to be missing.
    ts_ensure_column($pdo, 'users', 'role', "ENUM('student','admin') NOT NULL DEFAULT 'student'");
    ts_ensure_column($pdo, 'users', 'is_active', 'TINYINT(1) NOT NULL DEFAULT 1');
    ts_ensure_column($pdo, 'users', 'is_email_verified', 'TINYINT(1) NOT NULL DEFAULT 0');
    ts_ensure_column($pdo, 'users', 'suspicious_flag', 'TINYINT(1) NOT NULL DEFAULT 0');
    ts_ensure_column($pdo, 'users', 'last_login_ip', 'VARCHAR(45) NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'last_login_at', 'DATETIME NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'google_id', 'VARCHAR(100) NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'auth_provider', "ENUM('email','google') NOT NULL DEFAULT 'email'");
    ts_ensure_column($pdo, 'users', 'target_exam', "VARCHAR(100) NOT NULL DEFAULT 'G.C.E. A/L'");
    ts_ensure_column($pdo, 'users', 'updated_at', 'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
    // Profile completion (added with the mandatory profile-setup screen).
    ts_ensure_column($pdo, 'users', 'phone', 'VARCHAR(20) NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'profile_completed_at', 'DATETIME NULL DEFAULT NULL');
    // Structured academic profile: stream / level / exam year / medium.
    ts_ensure_column($pdo, 'users', 'stream_code', 'VARCHAR(64) NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'exam_level', 'VARCHAR(16) NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'exam_year', 'SMALLINT UNSIGNED NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'medium', 'VARCHAR(16) NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'users', 'academic_updated_at', 'DATETIME NULL DEFAULT NULL');

    ts_ensure_column($pdo, 'email_verifications', 'attempts', 'TINYINT UNSIGNED NOT NULL DEFAULT 0');
    ts_ensure_column($pdo, 'email_verifications', 'is_used', 'TINYINT(1) NOT NULL DEFAULT 0');
    ts_ensure_column($pdo, 'email_verifications', 'otp_type', "ENUM('registration','unverified_account','login_challenge','ip_change','password_reset') NOT NULL DEFAULT 'registration'");
    ts_ensure_column($pdo, 'email_verifications', 'pending_data', 'LONGTEXT NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'email_verifications', 'requested_ip', 'VARCHAR(45) NULL DEFAULT NULL');
    ts_ensure_column($pdo, 'user_tokens', 'last_used_at', 'DATETIME NULL DEFAULT NULL');

    try {
        $stmt = $pdo->query("SHOW COLUMNS FROM `email_verifications` LIKE 'otp_code'");
        $col = $stmt ? $stmt->fetch() : null;
        if ($col && preg_match('/^varchar\((\d+)\)/i', (string) $col['Type'], $m) && (int) $m[1] < 128) {
            $pdo->exec('ALTER TABLE `email_verifications` MODIFY COLUMN `otp_code` VARCHAR(128) NOT NULL');
        }
    } catch (Throwable $e) {
        error_log('[TheStudent][auth] otp_code width check failed: ' . $e->getMessage());
    }

    try {
        $stmt = $pdo->query("SHOW COLUMNS FROM `email_verifications` LIKE 'user_id'");
        $col = $stmt ? $stmt->fetch() : null;
        if ($col && strtoupper((string) $col['Null']) === 'NO') {
            $pdo->exec('ALTER TABLE `email_verifications` MODIFY COLUMN `user_id` INT UNSIGNED NULL DEFAULT NULL');
        }
    } catch (Throwable $e) {
        error_log('[TheStudent][auth] user_id nullability check failed: ' . $e->getMessage());
    }

    try {
        $pdo->exec("CREATE TABLE IF NOT EXISTS `password_resets` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `user_id` INT UNSIGNED NOT NULL,
            `token_hash` VARCHAR(128) NOT NULL,
            `requested_ip` VARCHAR(45) NULL,
            `expires_at` DATETIME NOT NULL,
            `used_at` DATETIME NULL,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY `uniq_reset_token` (`token_hash`),
            KEY `idx_reset_user` (`user_id`),
            CONSTRAINT `fk_reset_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    } catch (Throwable $e) {
        error_log('[TheStudent][auth] password_resets bootstrap failed: ' . $e->getMessage());
    }
}

try {
    ensureAuthSchema($pdo);
} catch (Throwable $e) {
    error_log('[TheStudent][auth] schema self-heal warning: ' . $e->getMessage());
}

// ---------------------------------------------------------------------------
// Validation helpers
// ---------------------------------------------------------------------------
function normalizeEmail($value): string
{
    $email = strtolower(trim((string) $value));
    if ($email === '' || strlen($email) > 191 || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        sendJsonResponse('error', 'Please enter a valid email address.', [], 400);
    }
    return $email;
}

function validatePassword($password, string $field = 'Password'): string
{
    $password = (string) $password;

    if ($password === '' || trim($password) === '') {
        sendJsonResponse('error', $field . ' cannot be empty.', [], 400);
    }
    if (strlen($password) < TS_PASSWORD_MIN_LENGTH) {
        sendJsonResponse('error', $field . ' must be at least ' . TS_PASSWORD_MIN_LENGTH . ' characters long.', [], 400);
    }
    // bcrypt silently truncates after 72 bytes - reject instead of accepting a
    // password whose tail is ignored.
    if (strlen($password) > TS_PASSWORD_MAX_BYTES) {
        sendJsonResponse('error', $field . ' must be at most ' . TS_PASSWORD_MAX_BYTES . ' characters long.', [], 400);
    }
    if (!preg_match('/[A-Za-z]/', $password) || !preg_match('/\d/', $password)) {
        sendJsonResponse('error', $field . ' must include at least one letter and one number.', [], 400);
    }
    if (preg_match('/^(password|passw0rd|12345678|qwerty12|thestudent|admin123|11111111)$/i', $password)) {
        sendJsonResponse('error', 'This password is too common. Please choose a stronger one.', [], 400);
    }
    return $password;
}

function validateFullName($value): string
{
    $name = trim(preg_replace('/\s+/', ' ', (string) $value));
    if (strlen($name) < 2) {
        sendJsonResponse('error', 'Please enter your full name.', [], 400);
    }
    if (strlen($name) > 150) {
        $name = substr($name, 0, 150);
    }
    return $name;
}

/**
 * Strip control characters and tags, collapse whitespace, and enforce a length
 * range. Prepared statements already stop SQL injection; this keeps the stored
 * value clean and predictable for the admin panel and the app.
 */
function validateProfileText($value, string $label, int $min, int $max): string
{
    $text = (string) $value;
    $text = strip_tags($text);
    // Drop control characters; the /u flag is dropped on invalid UTF-8 input
    // (preg_* returns null then), which is exactly the junk we want gone.
    $text = (string) preg_replace('/[\x00-\x1F\x7F]/', ' ', $text);
    $collapsed = preg_replace('/\s+/u', ' ', $text);
    $text = trim($collapsed !== null ? $collapsed : (string) preg_replace('/\s+/', ' ', $text));

    // mbstring is not guaranteed on shared hosting; fall back to byte length.
    $length = function_exists('mb_strlen') ? mb_strlen($text, 'UTF-8') : strlen($text);

    if ($length < $min) {
        sendJsonResponse('error', $label . ' must be at least ' . $min . ' characters.', [], 400);
    }
    if ($length > $max) {
        sendJsonResponse('error', $label . ' must be at most ' . $max . ' characters.', [], 400);
    }
    if (preg_match('/[<>]/', $text)) {
        sendJsonResponse('error', $label . ' contains characters that are not allowed.', [], 400);
    }
    return $text;
}

/**
 * Accept the ways a Sri Lankan number is written and store E.164 (+94XXXXXXXXX).
 * 0712345678 / 071 234 5678 / +94712345678 / 94712345678 all normalise to
 * +94712345678. Other countries are accepted only in full international form.
 */
function validatePhoneNumber($value): string
{
    $raw = trim((string) $value);
    if ($raw === '') {
        sendJsonResponse('error', 'Please enter your phone number.', [], 400);
    }

    $digits = (string) preg_replace('/[^0-9]/', '', $raw);
    $hadPlus = strncmp($raw, '+', 1) === 0;

    if ($digits === '') {
        sendJsonResponse('error', 'Please enter a valid phone number.', [], 400);
    }

    // Local Sri Lankan mobile/landline: 0 + 9 digits.
    if (!$hadPlus && strlen($digits) === 10 && $digits[0] === '0') {
        $digits = '94' . substr($digits, 1);
    }

    if (strlen($digits) === 11 && strncmp($digits, '94', 2) === 0) {
        // Sri Lankan mobile numbers start with 7 after the country code.
        if ($digits[2] !== '7' && $digits[2] !== '1' && $digits[2] !== '2'
            && $digits[2] !== '3' && $digits[2] !== '4' && $digits[2] !== '5'
            && $digits[2] !== '6' && $digits[2] !== '8' && $digits[2] !== '9') {
            sendJsonResponse('error', 'Please enter a valid Sri Lankan phone number.', [], 400);
        }
        return '+' . $digits;
    }

    // Any other country: full international form only, 8-15 digits.
    if ($hadPlus && strlen($digits) >= 8 && strlen($digits) <= 15) {
        return '+' . $digits;
    }

    sendJsonResponse('error', 'Enter a valid phone number, for example 071 234 5678.', [], 400);
}

/** Whitelist the curriculum. */
function validateTargetExam($value): string
{
    $exam = trim((string) $value);
    if (!in_array($exam, TS_ALLOWED_TARGET_EXAMS, true)) {
        sendJsonResponse(
            'error',
            'Please choose your curriculum: ' . implode(', ', TS_ALLOWED_TARGET_EXAMS) . '.',
            [],
            400
        );
    }
    return $exam;
}

/** Has the student finished the mandatory profile-setup step? */
function isProfileComplete(array $user): bool
{
    return !empty($user['profile_completed_at']);
}

/** Fields the app should still ask for (drives the setup screen). */
function missingProfileFields(array $user): array
{
    $missing = [];
    if (trim((string) ($user['full_name'] ?? '')) === '') {
        $missing[] = 'full_name';
    }
    if (trim((string) ($user['phone'] ?? '')) === '') {
        $missing[] = 'phone';
    }
    if (trim((string) ($user['institution'] ?? '')) === '') {
        $missing[] = 'institution';
    }
    if (trim((string) ($user['stream_or_degree'] ?? '')) === '') {
        $missing[] = 'stream_or_degree';
    }
    if (empty($user['profile_picture'])) {
        $missing[] = 'profile_picture';
    }
    if (empty($user['password_hash'])) {
        $missing[] = 'password';
    }
    return $missing;
}

function hashPassword(string $password): string
{
    return password_hash($password, PASSWORD_BCRYPT, ['cost' => TS_BCRYPT_COST]);
}

/**
 * Constant-time-ish equality for 6-digit codes handled via hash_equals on the
 * HMAC digests (the digest is secret-keyed, so length/format issues disappear).
 */
function hmacOtp(string $code): string
{
    return hash_hmac('sha256', $code, (string) APP_KEY);
}

function hashToken(string $token): string
{
    return hash('sha256', $token);
}

/**
 * Should a login from $currentIp be treated as a "new location"?
 *
 * Comparing raw IPs is unusable in the real world: Sri Lankan mobile carriers
 * hand out addresses from large CGNAT pools, so a student on 4G would get an
 * e-mailed challenge on almost every login (and would be locked out entirely
 * whenever SMTP hiccups). We therefore only challenge when the networks really
 * differ: the first two octets of IPv4 (/16) or the first four hextets of IPv6
 * (/48). Private and loopback addresses never trigger a challenge.
 */
function isNewNetwork(string $previousIp, string $currentIp): bool
{
    if ($previousIp === '' || $currentIp === '') {
        return false;
    }
    if ($previousIp === $currentIp) {
        return false;
    }
    if (ts_is_private_ip($previousIp) || ts_is_private_ip($currentIp)) {
        return false;
    }
    return ts_network_key($previousIp) !== ts_network_key($currentIp);
}

function ts_is_private_ip(string $ip): bool
{
    return filter_var(
        $ip,
        FILTER_VALIDATE_IP,
        FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE
    ) === false;
}

function ts_network_key(string $ip): string
{
    if (strpos($ip, ':') !== false) {
        $groups = explode(':', $ip);
        return 'v6:' . implode(':', array_slice($groups, 0, 4));
    }
    $parts = explode('.', $ip);
    return 'v4:' . implode('.', array_slice($parts, 0, 2));
}

function maskEmail(string $email): string
{
    $parts = explode('@', $email);
    if (count($parts) !== 2) {
        return $email;
    }
    [$name, $domain] = $parts;
    $len = strlen($name);
    $masked = $len <= 2
        ? substr($name, 0, 1) . '*'
        : substr($name, 0, 2) . str_repeat('*', max(1, $len - 3)) . substr($name, -1);
    return $masked . '@' . $domain;
}

function adminEmailList(): array
{
    $raw = (string) (defined('ADMIN_EMAILS') ? ADMIN_EMAILS : '');
    return array_values(array_filter(array_map(
        static fn ($e) => strtolower(trim($e)),
        explode(',', $raw)
    )));
}

function roleForEmail(string $email, string $currentRole = 'student'): string
{
    if ($currentRole === 'admin') {
        return 'admin';
    }
    return in_array(strtolower($email), adminEmailList(), true) ? 'admin' : 'student';
}

// ---------------------------------------------------------------------------
// Rate limiting
// ---------------------------------------------------------------------------
function countLoginAttempts(PDO $pdo, string $column, string $value, int $minutes): int
{
    $allowed = ['ip_address', 'email'];
    if (!in_array($column, $allowed, true)) {
        return 0;
    }
    $safeMinutes = max(1, (int) $minutes);
    try {
        $stmt = $pdo->prepare(
            "SELECT COUNT(*) AS c FROM login_attempts
             WHERE `$column` = :value AND attempted_at >= (NOW() - INTERVAL {$safeMinutes} MINUTE)"
        );
        $stmt->execute(['value' => $value]);
        return (int) ($stmt->fetch()['c'] ?? 0);
    } catch (Throwable $e) {
        // A missing `login_attempts` table must not turn every sign-up into a
        // blank 500: fail open (no limit known) and log the real reason.
        error_log('[TheStudent][auth] rate-limit check failed (' . $column . '): ' . $e->getMessage());
        return 0;
    }
}

function recordFailedAttempt(PDO $pdo, string $ip, string $email): void
{
    try {
        $stmt = $pdo->prepare('INSERT INTO login_attempts (ip_address, email) VALUES (:ip, :email)');
        $stmt->execute(['ip' => substr($ip, 0, 45), 'email' => substr($email, 0, 191)]);
    } catch (Throwable $e) {
        error_log('[TheStudent][auth] failed to record attempt: ' . $e->getMessage());
    }
}

function clearFailedAttempts(PDO $pdo, string $ip, string $email): void
{
    try {
        $stmt = $pdo->prepare('DELETE FROM login_attempts WHERE email = :email OR ip_address = :ip');
        $stmt->execute(['email' => $email, 'ip' => $ip]);
    } catch (Throwable $e) {
        error_log('[TheStudent][auth] failed to clear attempts: ' . $e->getMessage());
    }
}

/**
 * Enforce the login/verification brute-force limits before doing any work.
 */
function enforceLoginRateLimit(PDO $pdo, string $ip, string $email): void
{
    $emailAttempts = $email !== '' ? countLoginAttempts($pdo, 'email', $email, 15) : 0;
    if ($emailAttempts >= TS_LOGIN_MAX_PER_EMAIL_15MIN) {
        sendJsonResponse(
            'error',
            'Too many failed attempts for this account. Please wait 15 minutes and try again.',
            ['retry_after' => 900],
            429
        );
    }
    $ipAttempts = countLoginAttempts($pdo, 'ip_address', $ip, 15);
    if ($ipAttempts >= TS_LOGIN_MAX_PER_IP_15MIN) {
        sendJsonResponse(
            'error',
            'Too many failed attempts from this network. Please wait 15 minutes and try again.',
            ['retry_after' => 900],
            429
        );
    }
}

/**
 * Throttle OTP delivery: protects the SMTP quota and prevents mail bombing.
 */
/**
 * @return int Seconds the caller must wait before another OTP may be sent
 *             (0 when the request is allowed).
 */
function otpSendThrottleWait(PDO $pdo, string $ip, string $email): int
{
    $checks = [
        ['SELECT COUNT(*) AS c FROM email_verifications WHERE email = :v AND created_at >= (NOW() - INTERVAL 15 MINUTE)', $email, TS_OTP_EMAIL_MAX_PER_15MIN, 600],
        ['SELECT COUNT(*) AS c FROM email_verifications WHERE email = :v AND created_at >= (NOW() - INTERVAL 1 HOUR)', $email, TS_OTP_EMAIL_MAX_PER_HOUR, 1800],
        ['SELECT COUNT(*) AS c FROM email_verifications WHERE requested_ip = :v AND created_at >= (NOW() - INTERVAL 15 MINUTE)', substr($ip, 0, 45), TS_OTP_IP_MAX_PER_15MIN, 900],
    ];

    foreach ($checks as [$sql, $value, $limit, $wait]) {
        try {
            $stmt = $pdo->prepare($sql);
            $stmt->execute(['v' => $value]);
            if ((int) ($stmt->fetch()['c'] ?? 0) >= $limit) {
                return $wait;
            }
        } catch (Throwable $e) {
            error_log('[TheStudent][auth] throttle check failed: ' . $e->getMessage());
        }
    }
    return 0;
}

function enforceOtpSendThrottle(PDO $pdo, string $ip, string $email): void
{
    $wait = otpSendThrottleWait($pdo, $ip, $email);
    if ($wait > 0) {
        sendJsonResponse(
            'error',
            'Too many verification codes requested. Please wait a few minutes and try again.',
            ['retry_after' => $wait],
            429
        );
    }
}

// ---------------------------------------------------------------------------
// Sessions
// ---------------------------------------------------------------------------
/**
 * Create a session and return the RAW token (shown to the client exactly once).
 * Only its SHA-256 digest is persisted.
 */
function createSession(PDO $pdo, int $userId, string $ip, string $deviceInfo = 'Android'): array
{
    $token = bin2hex(random_bytes(32));
    $expiresAt = date('Y-m-d H:i:s', strtotime('+' . TS_SESSION_TTL_DAYS . ' days'));

    $stmt = $pdo->prepare(
        'INSERT INTO user_tokens (user_id, token, device_info, ip_address, expires_at, last_used_at)
         VALUES (:user_id, :token, :device, :ip, :expires_at, NOW())'
    );
    $stmt->execute([
        'user_id'    => $userId,
        'token'      => hashToken($token),
        'device'     => substr($deviceInfo, 0, 255),
        'ip'         => substr($ip, 0, 45),
        'expires_at' => $expiresAt,
    ]);

    // Cap concurrent sessions per user (drop the oldest ones).
    $trim = $pdo->prepare(
        'DELETE FROM user_tokens
         WHERE user_id = :uid
           AND id NOT IN (
               SELECT id FROM (
                   SELECT id FROM user_tokens WHERE user_id = :uid2 ORDER BY created_at DESC LIMIT ' . TS_MAX_SESSIONS_PER_USER . '
               ) AS keep
           )'
    );
    $trim->execute(['uid' => $userId, 'uid2' => $userId]);

    return ['token' => $token, 'expires_at' => $expiresAt];
}

/**
 * Resolve the Bearer token from headers only (never query strings).
 */
function extractBearerToken(): ?string
{
    $header = '';
    if (!empty($_SERVER['HTTP_AUTHORIZATION'])) {
        $header = trim((string) $_SERVER['HTTP_AUTHORIZATION']);
    } elseif (!empty($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $header = trim((string) $_SERVER['REDIRECT_HTTP_AUTHORIZATION']);
    } elseif (function_exists('apache_request_headers')) {
        $req = apache_request_headers();
        $header = (string) ($req['Authorization'] ?? $req['authorization'] ?? '');
    }

    if ($header !== '' && preg_match('/^Bearer\s+([A-Fa-f0-9]{32,256})$/', $header, $m)) {
        return $m[1];
    }
    return null;
}

/**
 * Validate a session and optionally slide its expiry.
 * Returns [userRow, expiresAt] or terminates with 401/403.
 */
function requireSession(PDO $pdo, bool $renew = true): array
{
    $token = extractBearerToken();
    if ($token === null) {
        sendJsonResponse('error', 'Authentication required.', [], 401);
    }

    $stmt = $pdo->prepare(
        'SELECT t.id AS token_id, t.expires_at, u.*
         FROM user_tokens t
         JOIN users u ON u.id = t.user_id
         WHERE t.token = :token AND t.expires_at > NOW()
         LIMIT 1'
    );
    $stmt->execute(['token' => hashToken($token)]);
    $row = $stmt->fetch();

    if (!$row) {
        sendJsonResponse('error', 'Session expired or invalid. Please sign in again.', [], 401);
    }
    if ((int) $row['is_active'] !== 1) {
        sendJsonResponse('error', 'This account has been disabled. Please contact support.', [], 403);
    }
    if ((int) $row['is_email_verified'] !== 1) {
        sendJsonResponse('error', 'Email address is not yet verified.', [], 403);
    }

    $expiresAt = $row['expires_at'];
    $renewThreshold = date('Y-m-d H:i:s', strtotime('+' . TS_SESSION_RENEW_WHEN_DAYS . ' days'));
    if ($renew && $expiresAt < $renewThreshold) {
        $expiresAt = date('Y-m-d H:i:s', strtotime('+' . TS_SESSION_TTL_DAYS . ' days'));
        $upd = $pdo->prepare('UPDATE user_tokens SET expires_at = :exp, last_used_at = NOW() WHERE id = :id');
        $upd->execute(['exp' => $expiresAt, 'id' => $row['token_id']]);
    } else {
        $pdo->prepare('UPDATE user_tokens SET last_used_at = NOW() WHERE id = :id')
            ->execute(['id' => $row['token_id']]);
    }

    return [$row, $expiresAt];
}

function revokeAllSessions(PDO $pdo, int $userId): void
{
    $pdo->prepare('DELETE FROM user_tokens WHERE user_id = :uid')->execute(['uid' => $userId]);
}

// ---------------------------------------------------------------------------
// User payload
// ---------------------------------------------------------------------------
/**
 * The subject list for a student, newest schema first.
 *
 * Returns [] instead of throwing when the table is missing, so a half-applied
 * deploy degrades to "no subjects yet" rather than a 500 on every login.
 */
function ts_user_subjects(PDO $pdo, int $userId): array
{
    try {
        $stmt = $pdo->prepare(
            'SELECT subject_code, subject_label, exam_level, bucket_no
             FROM user_subjects
             WHERE user_id = :id
             ORDER BY position ASC, id ASC'
        );
        $stmt->execute(['id' => $userId]);
        $out = [];
        foreach ($stmt->fetchAll() as $row) {
            $out[] = [
                'code'        => (string) $row['subject_code'],
                'label'       => (string) $row['subject_label'],
                'exam_level'  => (string) $row['exam_level'],
                'bucket'      => $row['bucket_no'] !== null ? (int) $row['bucket_no'] : null,
            ];
        }
        return $out;
    } catch (Throwable $e) {
        error_log('[TheStudent][auth] could not read subjects: ' . $e->getMessage());
        return [];
    }
}

/** True once level + year + medium + all three subjects are on file. */
function ts_academic_complete(array $user, array $subjects): bool
{
    return trim((string) ($user['exam_level'] ?? '')) !== ''
        && trim((string) ($user['medium'] ?? '')) !== ''
        && (int) ($user['exam_year'] ?? 0) > 0
        && count($subjects) === 3;
}

function publicUser(array $user, array $subjects = []): array
{
    return [
        'id'              => (int) $user['id'],
        'name'            => $user['full_name'] ?? 'Student',
        'email'           => $user['email'] ?? '',
        'profile_picture' => $user['profile_picture'] ?? null,
        // Absolute URL the app can load directly (Google URL or our upload).
        'avatar_url'      => ts_avatar_public_url($user['profile_picture'] ?? null),
        'phone'           => $user['phone'] ?? null,
        'profile_completed' => isProfileComplete($user),
        'missing_profile_fields' => isProfileComplete($user) ? [] : missingProfileFields($user),
        // A Google-only account has no password yet: the setup screen asks for
        // one so the student can also sign in with email.
        'has_password'    => !empty($user['password_hash']),
        'target_exam'     => $user['target_exam'] ?? 'G.C.E. A/L',
        'institution'     => $user['institution'] ?? null,
        'stream_or_degree' => $user['stream_or_degree'] ?? null,
        // Structured academic profile used by matching + the chat profile.
        'exam_level'      => $user['exam_level'] ?? null,
        'stream_code'     => $user['stream_code'] ?? null,
        'exam_year'       => isset($user['exam_year']) ? (int) $user['exam_year'] : null,
        'medium'          => $user['medium'] ?? null,
        'subjects'        => $subjects,
        'academic_complete' => ts_academic_complete($user, $subjects),
        'role'            => $user['role'] ?? 'student',
        'auth_provider'   => $user['auth_provider'] ?? 'email',
        'is_verified'     => (bool) ($user['is_email_verified'] ?? false),
    ];
}

// ---------------------------------------------------------------------------
// OTP issuance & delivery
// ---------------------------------------------------------------------------
/**
 * Create a new OTP for $email, invalidate previous unused codes and email it.
 *
 * @return array{code:string, sent:bool}
 */
function issueEmailOtp(PDO $pdo, ?int $userId, string $email, string $type = 'registration', ?array $pendingData = null, string $ip = ''): array
{
    $code = (string) random_int(100000, 999999);
    $expiresAt = date('Y-m-d H:i:s', strtotime('+' . TS_OTP_TTL_MINUTES . ' minutes'));

    try {
        // Only one live code per email - prevents "try every code ever sent".
        $pdo->prepare('UPDATE email_verifications SET is_used = 1 WHERE email = :email AND is_used = 0')
            ->execute(['email' => $email]);

        $stmt = $pdo->prepare(
            'INSERT INTO email_verifications (user_id, email, otp_code, otp_type, pending_data, requested_ip, expires_at)
             VALUES (:user_id, :email, :code, :type, :pending, :ip, :expires_at)'
        );
        $stmt->execute([
            'user_id'    => $userId,
            'email'      => $email,
            'code'       => hmacOtp($code),
            'type'       => $type,
            'pending'    => $pendingData !== null ? json_encode($pendingData, JSON_UNESCAPED_UNICODE) : null,
            'ip'         => substr($ip, 0, 45),
            'expires_at' => $expiresAt,
        ]);
    } catch (Throwable $e) {
        // Never let a schema problem become an empty 500. Log the SQL error
        // against a short reference and hand the same reference to the client.
        $ref = bin2hex(random_bytes(4));
        error_log('[TheStudent][auth][' . $ref . '] could not store OTP: ' . $e->getMessage());
        sendJsonResponse(
            'error',
            'Verification is temporarily unavailable. Reference: ' . $ref,
            ['ref' => $ref, 'retry_after' => 60],
            503
        );
    }

    $sent = sendVerificationEmail($email, $code, $type, TS_OTP_TTL_MINUTES);

    return ['code' => $code, 'sent' => $sent];
}

/**
 * Deliver the branded verification email.
 * The OTP is NEVER placed in the subject line.
 *
 * @return bool true when the message was accepted by the SMTP server.
 */
function sendVerificationEmail(string $recipientEmail, string $otpCode, string $type = 'registration', int $ttlMinutes = 10): bool
{
    $subject = 'Your TheStudent verification code';

    $title = 'Verify your email';
    $subtitle = 'Enter the 6-digit code below to finish signing in to TheStudent.';
    if ($type === 'ip_change') {
        $subject = 'TheStudent security check';
        $title = 'New sign-in location';
        $subtitle = 'We noticed a sign-in from a new network. Enter the code below to authorise it. If this was not you, change your password immediately.';
    } elseif ($type === 'unverified_account') {
        $subject = 'Verify your TheStudent account';
        $title = 'Account verification required';
        $subtitle = 'Your account still needs email verification. Enter the code below to continue.';
    } elseif ($type === 'password_reset') {
        $subject = 'Reset your TheStudent password';
        $title = 'Password reset requested';
        $subtitle = 'Enter the code below to choose a new password. If you did not request this, you can safely ignore this email.';
    }

    $year = date('Y');
    $htmlContent = '<!DOCTYPE html><html><head><meta charset="utf-8"></head>
    <body style="font-family:-apple-system,BlinkMacSystemFont,\'Segoe UI\',Roboto,sans-serif;background:#f8fafc;margin:0;padding:24px;color:#1e293b;">
      <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:20px;overflow:hidden;border:1px solid #e2e8f0;">
        <div style="background:#047857;padding:28px 24px;text-align:center;">
          <h1 style="margin:0;color:#fff;font-size:24px;font-weight:800;">TheStudent</h1>
          <p style="margin:6px 0 0;color:#a7f3d0;font-size:13px;">Sri Lanka Education &amp; AI Study Suite</p>
        </div>
        <div style="padding:32px 28px;text-align:center;">
          <div style="font-size:20px;font-weight:700;color:#0f172a;margin-bottom:10px;">' . htmlspecialchars($title, ENT_QUOTES, 'UTF-8') . '</div>
          <div style="font-size:14px;line-height:1.6;color:#475569;margin-bottom:24px;">' . htmlspecialchars($subtitle, ENT_QUOTES, 'UTF-8') . '</div>
          <div style="background:#ecfdf5;border:2px dashed #047857;border-radius:14px;padding:18px 24px;display:inline-block;margin-bottom:24px;">
            <div style="font-size:34px;font-weight:800;letter-spacing:8px;color:#047857;font-family:monospace,sans-serif;">' . htmlspecialchars($otpCode, ENT_QUOTES, 'UTF-8') . '</div>
          </div>
          <div style="font-size:13px;color:#64748b;">This code expires in <strong>' . (int) $ttlMinutes . ' minutes</strong> and can be used once.<br>Never share this code with anyone - TheStudent staff will never ask for it.</div>
        </div>
        <div style="background:#f8fafc;padding:20px;text-align:center;font-size:12px;color:#94a3b8;border-top:1px solid #e2e8f0;">
          &copy; ' . $year . ' TheStudent. All rights reserved.
        </div>
      </div>
    </body></html>';

    $textBody = "TheStudent - {$title}\n\n{$subtitle}\n\nYour verification code: {$otpCode}\n\n"
        . "This code expires in {$ttlMinutes} minutes and can be used once.\n"
        . "Never share this code with anyone.\n";

    if ((string) SMTP_USER === '' || (string) SMTP_PASS === '') {
        error_log('[TheStudent][auth] SMTP is not configured - verification email not sent.');
        return false;
    }

    try {
        $mailer = new TheStudentSmtpMailer(
            (string) SMTP_USER,
            (string) SMTP_PASS,
            (string) SMTP_HOST,
            (int) SMTP_PORT,
            (string) SMTP_FROM_NAME
        );
        if ($mailer->send($recipientEmail, $subject, $htmlContent, $textBody)) {
            return true;
        }
        error_log('[TheStudent][auth] SMTP delivery failed: ' . $mailer->getLastError());
    } catch (Throwable $e) {
        error_log('[TheStudent][auth] SMTP exception: ' . $e->getMessage());
    }

    return false;
}

// ---------------------------------------------------------------------------
// Google ID token verification
// ---------------------------------------------------------------------------
/**
 * Verify a Google ID token server-side and return its trusted claims.
 * Rejects wrong issuer/audience, expired tokens and unverified emails.
 */
function verifyGoogleIdToken(string $idToken): array
{
    if (!function_exists('curl_init')) {
        error_log('[TheStudent][auth] cURL extension missing - cannot verify Google tokens.');
        sendJsonResponse('error', 'Google sign-in is temporarily unavailable.', [], 503);
    }

    $ch = curl_init('https://oauth2.googleapis.com/tokeninfo?id_token=' . urlencode($idToken));
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_TIMEOUT        => 10,
        CURLOPT_CONNECTTIMEOUT => 5,
    ]);
    $response = curl_exec($ch);
    $httpCode = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode !== 200 || !is_string($response) || $response === '') {
        sendJsonResponse('error', 'Invalid or expired Google token.', [], 401);
    }

    $claims = json_decode($response, true);
    if (!is_array($claims) || empty($claims['sub']) || empty($claims['email'])) {
        sendJsonResponse('error', 'Invalid or expired Google token.', [], 401);
    }

    $issuer = (string) ($claims['iss'] ?? '');
    if (!in_array($issuer, ['accounts.google.com', 'https://accounts.google.com'], true)) {
        sendJsonResponse('error', 'Untrusted Google token issuer.', [], 401);
    }

    $expectedAud = (string) GOOGLE_WEB_CLIENT_ID;
    if ($expectedAud === '' || ($claims['aud'] ?? '') !== $expectedAud) {
        sendJsonResponse('error', 'Token audience mismatch: untrusted client ID.', [], 401);
    }

    if ((int) ($claims['exp'] ?? 0) <= time()) {
        sendJsonResponse('error', 'Invalid or expired Google token.', [], 401);
    }

    $emailVerified = $claims['email_verified'] ?? false;
    if (!($emailVerified === true || $emailVerified === 'true' || $emailVerified === 1 || $emailVerified === '1')) {
        sendJsonResponse('error', 'Your Google email address is not verified.', [], 403);
    }

    return $claims;
}

// ---------------------------------------------------------------------------
// Request body (only parsed for write actions)
// ---------------------------------------------------------------------------
$body = in_array($action, ['me', 'profile', 'logout', 'logout_all'], true) ? [] : readJsonBody();

// ===========================================================================
// 1. REGISTER
// ===========================================================================
if ($action === 'register') {
    requirePost('register');

    $email    = normalizeEmail($body['email'] ?? '');
    $password = validatePassword($body['password'] ?? '');
    $fullName = validateFullName($body['full_name'] ?? '');
    $targetExam = substr(trim((string) ($body['target_exam'] ?? 'G.C.E. A/L')), 0, 100);

    enforceLoginRateLimit($pdo, $clientIp, $email);
    enforceOtpSendThrottle($pdo, $clientIp, $email);

    $stmt = $pdo->prepare('SELECT id, is_email_verified, is_active FROM users WHERE email = :email LIMIT 1');
    $stmt->execute(['email' => $email]);
    $existing = $stmt->fetch();

    if ($existing) {
        if ((int) $existing['is_active'] !== 1) {
            // Do not reveal account state.
            sendJsonResponse('error', 'Unable to create an account with these details. Please contact support.', [], 403);
        }
        if ((int) $existing['is_email_verified'] === 0) {
            $delivery = issueEmailOtp($pdo, (int) $existing['id'], $email, 'unverified_account', null, $clientIp);
            if (!$delivery['sent']) {
                sendJsonResponse('error', 'We could not send the verification email right now. Please try again in a minute.', ['retry_after' => 60], 503);
            }
            sendJsonResponse('requires_verification', 'This email is registered but not verified yet. We sent a fresh 6-digit code to your inbox.', [
                'email'               => $email,
                'masked_email'        => maskEmail($email),
                'verification_reason' => 'unverified_account',
                'resend_cooldown'     => TS_OTP_RESEND_COOLDOWN_SEC,
            ]);
        }
        // Verified account: neutral message, no account details leaked.
        sendJsonResponse('error', 'This email cannot be used to create a new account. Try signing in instead.', [], 409);
    }

    $pendingData = [
        'email'         => $email,
        'password_hash' => hashPassword($password),
        'full_name'     => $fullName,
        'target_exam'   => $targetExam,
        'auth_provider' => 'email',
        'role'          => roleForEmail($email),
    ];

    $delivery = issueEmailOtp($pdo, null, $email, 'registration', $pendingData, $clientIp);
    if (!$delivery['sent']) {
        sendJsonResponse('error', 'We could not send the verification email right now. Please try again in a minute.', ['retry_after' => 60], 503);
    }

    sendJsonResponse('requires_verification', 'We sent a 6-digit code to your email. Enter it to finish creating your account.', [
        'email'               => $email,
        'masked_email'        => maskEmail($email),
        'verification_reason' => 'registration',
        'resend_cooldown'     => TS_OTP_RESEND_COOLDOWN_SEC,
    ]);
}

// ===========================================================================
// 1b. COMPLETE PROFILE (mandatory after sign-up / Google sign-in)
// ===========================================================================
/**
 * The details Google (or a bare e-mail sign-up) cannot give us: phone number,
 * school/institution, stream, a profile photo and — for Google-only accounts —
 * a password.
 *
 * Accepts JSON (no photo) or multipart/form-data (`avatar` file + fields), so a
 * single request stores everything and the student never lands in the app with
 * half a profile.
 *
 * Security notes:
 *  • session required (bearer token), so you can only edit your own profile;
 *  • every value is validated server-side — the app's checks are only UX;
 *  • SQL uses prepared statements with named parameters (no string building);
 *  • the photo goes through api/_avatar.php (type sniffing + GD re-encode);
 *  • a password is only ever SET here when the account has none.
 */
if ($action === 'complete_profile') {
    requirePost('complete_profile');

    [$user] = requireSession($pdo, false);
    $userId = (int) $user['id'];
    $alreadyComplete = isProfileComplete($user);
    $hadPassword = !empty($user['password_hash']);
    $previousPicture = $user['profile_picture'] ?? null;

    // ---- text fields -----------------------------------------------------
    $fullName = validateProfileText(
        $body['full_name'] ?? '',
        'Full name',
        TS_PROFILE_NAME_MIN,
        TS_PROFILE_NAME_MAX
    );
    $phone = validatePhoneNumber($body['phone'] ?? '');
    $targetExam = validateTargetExam($body['target_exam'] ?? '');
    $institution = validateProfileText(
        $body['institution'] ?? '',
        'School or institution',
        TS_PROFILE_INSTITUTION_MIN,
        TS_PROFILE_INSTITUTION_MAX
    );
    $stream = validateProfileText(
        $body['stream_or_degree'] ?? '',
        'Stream or programme',
        TS_PROFILE_STREAM_MIN,
        TS_PROFILE_STREAM_MAX
    );

    // ---- password (Google-only accounts) ---------------------------------
    $newPasswordHash = null;
    if (!$hadPassword) {
        $plainPassword = (string) ($body['password'] ?? '');
        if ($plainPassword === '') {
            sendJsonResponse(
                'error',
                'Please choose a password so you can also sign in with your email.',
                ['missing_profile_fields' => ['password']],
                400
            );
        }
        validatePassword($plainPassword, 'Password');
        $newPasswordHash = hashPassword($plainPassword);
    }

    // ---- profile photo ---------------------------------------------------
    $avatarPath = null;
    $avatarFile = $_FILES['avatar'] ?? null;
    if (is_array($avatarFile) && ($avatarFile['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_NO_FILE) {
        try {
            $avatarPath = ts_store_avatar($avatarFile, $userId);
        } catch (RuntimeException $e) {
            // Message is written for students, never a stack trace.
            sendJsonResponse('error', $e->getMessage(), ['field' => 'avatar'], 400);
        }
    }

    if ($avatarPath === null && empty($previousPicture)) {
        sendJsonResponse(
            'error',
            'Please add a profile photo to finish setting up your account.',
            ['field' => 'avatar', 'missing_profile_fields' => ['profile_picture']],
            400
        );
    }

    // ---- persist (single UPDATE, prepared) -------------------------------
    $sql = 'UPDATE users SET
                full_name = :full_name,
                phone = :phone,
                target_exam = :target_exam,
                institution = :institution,
                stream_or_degree = :stream,
                profile_completed_at = NOW(),
                updated_at = NOW()';
    $params = [
        'full_name'   => $fullName,
        'phone'       => $phone,
        'target_exam' => $targetExam,
        'institution' => $institution,
        'stream'      => $stream,
        'id'          => $userId,
    ];

    if ($avatarPath !== null) {
        $sql .= ', profile_picture = :avatar';
        $params['avatar'] = $avatarPath;
    }
    if ($newPasswordHash !== null) {
        $sql .= ', password_hash = :password_hash';
        $params['password_hash'] = $newPasswordHash;
    }
    $sql .= ' WHERE id = :id';

    try {
        $pdo->prepare($sql)->execute($params);
    } catch (Throwable $e) {
        // Roll back a stored file so we never keep an orphaned avatar.
        if ($avatarPath !== null) {
            ts_avatar_delete($avatarPath);
        }
        $ref = bin2hex(random_bytes(4));
        error_log('[TheStudent][auth][' . $ref . '] complete_profile failed: ' . $e->getMessage());
        sendJsonResponse(
            'error',
            'We could not save your profile right now. Reference: ' . $ref,
            ['ref' => $ref],
            500
        );
    }

    // The old photo is only removed once the row actually points at the new one.
    if ($avatarPath !== null) {
        ts_avatar_delete($previousPicture);
    }

    $fresh = $pdo->prepare('SELECT * FROM users WHERE id = :id');
    $fresh->execute(['id' => $userId]);
    $updatedUser = $fresh->fetch() ?: $user;

    $message = $alreadyComplete ? 'Profile updated.' : 'Profile complete. Welcome to TheStudent!';
    sendJsonResponse('success', $message, [
        'user'       => publicUser($updatedUser, ts_user_subjects($pdo, (int) $updatedUser['id'])),
        'role'       => $updatedUser['role'] ?? 'student',
        'expires_at' => null,
    ]);
}

// ===========================================================================
// 2. LOGIN (email + password)
// ===========================================================================
// 5b. STRUCTURED ACADEMIC PROFILE  (?action=academic_profile)
// ===========================================================================
/**
 * Save the stream / level / exam year / medium / three subjects.
 *
 * This is the data the help-matching algorithm reads. It lives in its own
 * endpoint (not complete_profile) for two reasons:
 *   • a student whose profile is already complete must be able to edit it
 *     later without re-sending phone, photo or password;
 *   • replacing the subject list is a transaction (delete + insert), which
 *     must never be able to leave half a profile behind.
 *
 * The subject catalog itself lives in the app (SriLankanCurriculumConfig) so
 * the syllabus can change without a server deploy. The server enforces the
 * *shape*: level, year range, medium, exactly three distinct subjects, and —
 * for O/L — one subject per bucket.
 */
if ($action === 'academic_profile') {
    requirePost('academic_profile');

    [$academicUser] = requireSession($pdo, false);
    $academicUserId = (int) $academicUser['id'];

    $level = strtoupper(trim((string) ($body['exam_level'] ?? '')));
    if ($level !== 'AL' && $level !== 'OL') {
        sendJsonResponse('error', 'Choose A/L or O/L first.', ['field' => 'exam_level'], 400);
    }

    $medium = trim((string) ($body['medium'] ?? ''));
    if (!in_array($medium, TS_ALLOWED_MEDIUMS, true)) {
        sendJsonResponse('error', 'Choose your medium of study.', ['field' => 'medium'], 400);
    }

    // Two years back (repeating a paper) to six years ahead (Grade 12).
    $examYear = (int) ($body['exam_year'] ?? 0);
    $thisYear = (int) date('Y');
    if ($examYear < $thisYear - 2 || $examYear > $thisYear + 6) {
        sendJsonResponse('error', 'Choose a valid exam year.', ['field' => 'exam_year'], 400);
    }

    $streamCode = '';
    if ($level === 'AL') {
        $streamCode = validateProfileText($body['stream_code'] ?? '', 'Stream', 2, 64);
    }

    $rawSubjects = $body['subjects'] ?? [];
    if (!is_array($rawSubjects)) {
        $rawSubjects = [];
    }
    if (count($rawSubjects) !== 3) {
        sendJsonResponse('error', 'Choose exactly 3 subjects.', ['field' => 'subjects'], 400);
    }

    $subjectRows = [];
    $seenCodes = [];
    $seenBuckets = [];
    foreach (array_values($rawSubjects) as $position => $item) {
        $label = '';
        $bucket = null;
        if (is_array($item)) {
            $label = (string) ($item['label'] ?? '');
            if (isset($item['bucket']) && $item['bucket'] !== null && $item['bucket'] !== '') {
                $bucket = (int) $item['bucket'];
            }
        } else {
            $label = (string) $item;
        }

        $label = validateProfileText($label, 'Subject', 2, 120);

        // A stable slug is the join key for matching; the label stays human.
        $slug = strtolower((string) preg_replace('/[^A-Za-z0-9]+/', '_', $label));
        $slug = trim($slug, '_');
        if ($slug === '') {
            $slug = 'subject_' . $position;
        }
        $slug = substr($slug, 0, 64);

        if (isset($seenCodes[$slug])) {
            sendJsonResponse('error', 'That subject is already selected.', ['field' => 'subjects'], 400);
        }
        $seenCodes[$slug] = true;

        if ($level === 'OL') {
            if ($bucket === null || $bucket < 1 || $bucket > 3) {
                sendJsonResponse(
                    'error',
                    'Each O/L optional subject must come from one of the three buckets.',
                    ['field' => 'subjects'],
                    400
                );
            }
            if (isset($seenBuckets[$bucket])) {
                sendJsonResponse(
                    'error',
                    'Choose only one subject from each bucket.',
                    ['field' => 'subjects'],
                    400
                );
            }
            $seenBuckets[$bucket] = true;
        } else {
            $bucket = null;
        }

        $subjectRows[] = [
            'code'     => $slug,
            'label'    => $label,
            'bucket'   => $bucket,
            'position' => (int) $position,
        ];
    }

    if ($level === 'OL' && count($seenBuckets) !== 3) {
        sendJsonResponse('error', 'Choose one subject from each bucket.', ['field' => 'subjects'], 400);
    }

    try {
        $pdo->beginTransaction();

        $update = $pdo->prepare(
            'UPDATE users SET
                exam_level = :level,
                stream_code = :stream,
                exam_year = :year,
                medium = :medium,
                academic_updated_at = NOW(),
                updated_at = NOW()
             WHERE id = :id'
        );
        $update->execute([
            'level'  => $level,
            'stream' => $streamCode,
            'year'   => $examYear,
            'medium' => $medium,
            'id'     => $academicUserId,
        ]);

        $pdo->prepare('DELETE FROM user_subjects WHERE user_id = :id')
            ->execute(['id' => $academicUserId]);

        $insert = $pdo->prepare(
            'INSERT INTO user_subjects
                (user_id, subject_code, subject_label, exam_level, bucket_no, position)
             VALUES (:user_id, :code, :label, :level, :bucket, :position)'
        );
        foreach ($subjectRows as $row) {
            $insert->execute([
                'user_id'  => $academicUserId,
                'code'     => $row['code'],
                'label'    => $row['label'],
                'level'    => $level,
                'bucket'   => $row['bucket'],
                'position' => $row['position'],
            ]);
        }

        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        $ref = bin2hex(random_bytes(4));
        error_log('[TheStudent][auth][' . $ref . '] academic_profile failed: ' . $e->getMessage());
        sendJsonResponse(
            'error',
            'We could not save your subjects right now. Reference: ' . $ref,
            ['ref' => $ref],
            500
        );
    }

    $fresh = $pdo->prepare(
        'SELECT * FROM users WHERE id = :id LIMIT 1'
    );
    $fresh->execute(['id' => $academicUserId]);
    $freshUser = $fresh->fetch() ?: $academicUser;
    $freshSubjects = ts_user_subjects($pdo, $academicUserId);

    sendJsonResponse('success', 'Your academic details are saved.', [
        'user' => publicUser($freshUser, $freshSubjects),
        'academic_complete' => ts_academic_complete($freshUser, $freshSubjects),
    ]);
}

// ===========================================================================
if ($action === 'login') {
    requirePost('login');

    $email    = normalizeEmail($body['email'] ?? '');
    $password = (string) ($body['password'] ?? '');

    if ($password === '') {
        sendJsonResponse('error', 'Please enter your password.', [], 400);
    }
    if (strlen($password) > TS_PASSWORD_MAX_BYTES) {
        // Do not leak policy details to credential-stuffing probes.
        sendJsonResponse('error', 'Invalid email or password.', [], 401);
    }

    enforceLoginRateLimit($pdo, $clientIp, $email);

    $stmt = $pdo->prepare('SELECT * FROM users WHERE email = :email LIMIT 1');
    $stmt->execute(['email' => $email]);
    $user = $stmt->fetch();

    // Always perform a bcrypt verification so response timing does not reveal
    // whether the account exists.
    $storedHash = ($user && !empty($user['password_hash'])) ? $user['password_hash'] : TS_DUMMY_BCRYPT;
    $passwordOk = password_verify($password, $storedHash);

    if (!$user || empty($user['password_hash']) || !$passwordOk) {
        recordFailedAttempt($pdo, $clientIp, $email);
        sendJsonResponse('error', 'Invalid email or password.', [], 401);
    }

    if ((int) $user['is_active'] !== 1) {
        recordFailedAttempt($pdo, $clientIp, $email);
        sendJsonResponse('error', 'This account has been disabled. Please contact support.', [], 403);
    }

    // Transparently upgrade hashes when the cost factor changes.
    if (password_needs_rehash($user['password_hash'], PASSWORD_BCRYPT, ['cost' => TS_BCRYPT_COST])) {
        try {
            $pdo->prepare('UPDATE users SET password_hash = :hash WHERE id = :id')
                ->execute(['hash' => hashPassword($password), 'id' => $user['id']]);
        } catch (Throwable $e) {
            error_log('[TheStudent][auth] rehash failed: ' . $e->getMessage());
        }
    }

    $userId = (int) $user['id'];

    if ((int) $user['is_email_verified'] === 0) {
        enforceOtpSendThrottle($pdo, $clientIp, $email);
        $delivery = issueEmailOtp($pdo, $userId, $email, 'unverified_account', null, $clientIp);
        if (!$delivery['sent']) {
            sendJsonResponse('error', 'We could not send the verification email right now. Please try again in a minute.', ['retry_after' => 60], 503);
        }
        sendJsonResponse('requires_verification', 'Your email is not verified yet. We sent a fresh code to your inbox.', [
            'email'               => $email,
            'masked_email'        => maskEmail($email),
            'verification_reason' => 'unverified_account',
            'resend_cooldown'     => TS_OTP_RESEND_COOLDOWN_SEC,
        ]);
    }

    $lastIp = (string) ($user['last_login_ip'] ?? '');
    $ipChanged = isNewNetwork($lastIp, $clientIp);
    $isSuspicious = (int) ($user['suspicious_flag'] ?? 0) === 1;

    if ($ipChanged || $isSuspicious) {
        enforceOtpSendThrottle($pdo, $clientIp, $email);
        $delivery = issueEmailOtp($pdo, $userId, $email, 'ip_change', null, $clientIp);
        if (!$delivery['sent']) {
            sendJsonResponse('error', 'We could not send the security code right now. Please try again in a minute.', ['retry_after' => 60], 503);
        }
        sendJsonResponse('requires_verification', 'Sign-in from a new location detected. Enter the 6-digit code we emailed you to authorise this device.', [
            'email'               => $email,
            'masked_email'        => maskEmail($email),
            'verification_reason' => 'ip_change',
            'resend_cooldown'     => TS_OTP_RESEND_COOLDOWN_SEC,
        ]);
    }

    clearFailedAttempts($pdo, $clientIp, $email);
    $pdo->prepare('UPDATE users SET last_login_ip = :ip, last_login_at = NOW(), suspicious_flag = 0 WHERE id = :id')
        ->execute(['ip' => substr($clientIp, 0, 45), 'id' => $userId]);

    $session = createSession($pdo, $userId, $clientIp, 'Android App');

    sendJsonResponse('success', 'Login successful', [
        'token'      => $session['token'],
        'expires_at' => $session['expires_at'],
        'user'       => publicUser($user + ['is_email_verified' => 1], ts_user_subjects($pdo, (int) $user['id'])),
    ]);
}

// ===========================================================================
// 3. VERIFY OTP
// ===========================================================================
if ($action === 'verify_otp') {
    requirePost('verify_otp');

    $email = normalizeEmail($body['email'] ?? '');
    $otpCode = preg_replace('/\D/', '', (string) ($body['otp_code'] ?? ''));

    if (!is_string($otpCode) || strlen($otpCode) !== 6) {
        sendJsonResponse('error', 'Please enter the 6-digit verification code.', [], 400);
    }

    enforceLoginRateLimit($pdo, $clientIp, $email);

    $stmt = $pdo->prepare(
        'SELECT * FROM email_verifications
         WHERE email = :email AND is_used = 0
         ORDER BY created_at DESC, id DESC
         LIMIT 1'
    );
    $stmt->execute(['email' => $email]);
    $record = $stmt->fetch();

    if (!$record) {
        recordFailedAttempt($pdo, $clientIp, $email);
        sendJsonResponse('error', 'This code is no longer valid. Please request a new one.', [], 400);
    }

    if (strtotime((string) $record['expires_at']) < time()) {
        $pdo->prepare('UPDATE email_verifications SET is_used = 1 WHERE id = :id')->execute(['id' => $record['id']]);
        sendJsonResponse('error', 'This code has expired. Please request a new one.', [], 400);
    }

    if ((int) $record['attempts'] >= TS_OTP_MAX_ATTEMPTS) {
        $pdo->prepare('UPDATE email_verifications SET is_used = 1 WHERE id = :id')->execute(['id' => $record['id']]);
        sendJsonResponse('error', 'Too many incorrect attempts. Please request a new code.', [], 429);
    }

    if (!hash_equals((string) $record['otp_code'], hmacOtp((string) $otpCode))) {
        // Atomic increment: concurrent guesses cannot exceed the attempt cap.
        $inc = $pdo->prepare('UPDATE email_verifications SET attempts = attempts + 1 WHERE id = :id AND is_used = 0 AND attempts < :max');
        $inc->execute(['id' => $record['id'], 'max' => TS_OTP_MAX_ATTEMPTS]);
        recordFailedAttempt($pdo, $clientIp, $email);

        $remaining = max(0, TS_OTP_MAX_ATTEMPTS - ((int) $record['attempts'] + 1));
        sendJsonResponse(
            'error',
            $remaining > 0
                ? "Incorrect code. {$remaining} attempt" . ($remaining === 1 ? '' : 's') . ' remaining.'
                : 'Incorrect code. Please request a new one.',
            ['attempts_remaining' => $remaining],
            400
        );
    }

    // Consume the code atomically so a race cannot reuse it.
    $consume = $pdo->prepare('UPDATE email_verifications SET is_used = 1 WHERE id = :id AND is_used = 0');
    $consume->execute(['id' => $record['id']]);
    if ($consume->rowCount() !== 1) {
        sendJsonResponse('error', 'This code has already been used. Please request a new one.', [], 400);
    }

    $otpType = (string) $record['otp_type'];
    $pendingData = !empty($record['pending_data']) ? json_decode((string) $record['pending_data'], true) : null;
    $userId = (int) ($record['user_id'] ?? 0);

    // ------ Password reset branch: no session is issued -----------------
    if ($otpType === 'password_reset') {
        $stmt = $pdo->prepare('SELECT id, is_active FROM users WHERE email = :email LIMIT 1');
        $stmt->execute(['email' => $email]);
        $user = $stmt->fetch();

        if (!$user || (int) $user['is_active'] !== 1) {
            sendJsonResponse('error', 'Unable to reset this account. Please contact support.', [], 403);
        }

        $resetToken = bin2hex(random_bytes(32));
        $pdo->prepare(
            'INSERT INTO password_resets (user_id, token_hash, requested_ip, expires_at)
             VALUES (:uid, :hash, :ip, :expires)'
        )->execute([
            'uid'     => (int) $user['id'],
            'hash'    => hashToken($resetToken),
            'ip'      => substr($clientIp, 0, 45),
            'expires' => date('Y-m-d H:i:s', strtotime('+' . TS_RESET_TOKEN_TTL_MINUTES . ' minutes')),
        ]);

        sendJsonResponse('reset_required', 'Code verified. Choose a new password.', [
            'reset_token'       => $resetToken,
            'reset_expires_in'  => TS_RESET_TOKEN_TTL_MINUTES * 60,
        ]);
    }

    // ------ Registration / activation ------------------------------------
    if ($otpType === 'registration' && is_array($pendingData)) {
        if (empty($pendingData['email']) || strtolower((string) $pendingData['email']) !== $email) {
            sendJsonResponse('error', 'Verification state mismatch. Please start again.', [], 400);
        }

        $pdo->beginTransaction();
        try {
            $chk = $pdo->prepare('SELECT * FROM users WHERE email = :email LIMIT 1');
            $chk->execute(['email' => $email]);
            $existing = $chk->fetch();

            if ($existing) {
                $userId = (int) $existing['id'];

                // The person just proved ownership of the mailbox with a fresh
                // OTP, so apply the details from THIS registration attempt.
                // Without this, someone who registered twice (first attempt
                // never verified) would silently keep the password from the
                // abandoned attempt and be unable to sign in.
                $updates = [
                    'is_email_verified = 1',
                    'last_login_ip = :ip',
                    'last_login_at = NOW()',
                    'suspicious_flag = 0',
                ];
                $params = ['ip' => substr($clientIp, 0, 45), 'id' => $userId];

                // Never overwrite credentials of an account that is already
                // verified - only finish the abandoned sign-up.
                if ((int) $existing['is_email_verified'] === 0) {
                    if (!empty($pendingData['password_hash'])) {
                        $updates[] = 'password_hash = :password_hash';
                        $params['password_hash'] = (string) $pendingData['password_hash'];
                    }
                    if (!empty($pendingData['full_name'])) {
                        $updates[] = 'full_name = :full_name';
                        $params['full_name'] = substr((string) $pendingData['full_name'], 0, 150);
                    }
                    $updates[] = 'role = :role';
                    $params['role'] = roleForEmail($email, (string) ($pendingData['role'] ?? 'student'));
                }

                $pdo->prepare(
                    'UPDATE users SET ' . implode(', ', $updates) . ' WHERE id = :id'
                )->execute($params);
            } else {
                $ins = $pdo->prepare(
                    'INSERT INTO users (google_id, email, password_hash, full_name, profile_picture, auth_provider, target_exam, role, is_email_verified, is_active, last_login_ip, last_login_at, created_at, updated_at)
                     VALUES (:google_id, :email, :password_hash, :name, :picture, :provider, :exam, :role, 1, 1, :ip, NOW(), NOW(), NOW())'
                );
                $ins->execute([
                    'google_id'     => $pendingData['google_id'] ?? null,
                    'email'         => $email,
                    'password_hash' => $pendingData['password_hash'] ?? null,
                    'name'          => substr((string) ($pendingData['full_name'] ?? 'Student'), 0, 150),
                    'picture'       => $pendingData['profile_picture'] ?? null,
                    'provider'      => ($pendingData['auth_provider'] ?? 'email') === 'google' ? 'google' : 'email',
                    'exam'          => substr((string) ($pendingData['target_exam'] ?? 'G.C.E. A/L'), 0, 100),
                    'role'          => roleForEmail($email, (string) ($pendingData['role'] ?? 'student')),
                    'ip'            => substr($clientIp, 0, 45),
                ]);
                $userId = (int) $pdo->lastInsertId();
            }

            $pdo->prepare('UPDATE email_verifications SET user_id = :uid WHERE id = :id')
                ->execute(['uid' => $userId, 'id' => $record['id']]);

            $pdo->commit();
        } catch (Throwable $e) {
            $pdo->rollBack();
            error_log('[TheStudent][auth] registration commit failed: ' . $e->getMessage());
            sendJsonResponse('error', 'We could not complete your registration. Please try again.', [], 500);
        }
    } else {
        // unverified_account / ip_change (or any code tied to an existing user)
        if ($userId === 0) {
            $stmt = $pdo->prepare('SELECT id FROM users WHERE email = :email LIMIT 1');
            $stmt->execute(['email' => $email]);
            $found = $stmt->fetch();
            $userId = $found ? (int) $found['id'] : 0;
        }
        if ($userId === 0) {
            sendJsonResponse('error', 'Verification could not be linked to an account. Please sign in again.', [], 400);
        }
        if (!in_array($otpType, ['unverified_account', 'ip_change', 'login_challenge'], true)) {
            sendJsonResponse('error', 'Unexpected verification type. Please start again.', [], 400);
        }
        $pdo->prepare(
            'UPDATE users SET is_email_verified = 1, last_login_ip = :ip, last_login_at = NOW(), suspicious_flag = 0 WHERE id = :id'
        )->execute(['ip' => substr($clientIp, 0, 45), 'id' => $userId]);
    }

    $stmt = $pdo->prepare('SELECT * FROM users WHERE id = :id LIMIT 1');
    $stmt->execute(['id' => $userId]);
    $user = $stmt->fetch();

    if (!$user) {
        sendJsonResponse('error', 'Account not found. Please contact support.', [], 404);
    }
    if ((int) $user['is_active'] !== 1) {
        sendJsonResponse('error', 'This account has been disabled. Please contact support.', [], 403);
    }

    // Keep the admin role in sync with ADMIN_EMAILS (never demote silently).
    $resolvedRole = roleForEmail($email, (string) $user['role']);
    if ($resolvedRole !== $user['role']) {
        $pdo->prepare('UPDATE users SET role = :role WHERE id = :id')->execute(['role' => $resolvedRole, 'id' => $userId]);
        $user['role'] = $resolvedRole;
    }

    clearFailedAttempts($pdo, $clientIp, $email);
    $session = createSession($pdo, $userId, $clientIp, 'Android OTP Verification');

    sendJsonResponse('success', 'Email verified. Welcome to TheStudent!', [
        'token'      => $session['token'],
        'expires_at' => $session['expires_at'],
        'user'       => publicUser($user, ts_user_subjects($pdo, (int) $user['id'])),
    ]);
}

// ===========================================================================
// 4. RESEND OTP
// ===========================================================================
if ($action === 'resend_otp') {
    requirePost('resend_otp');

    $email = normalizeEmail($body['email'] ?? '');
    enforceLoginRateLimit($pdo, $clientIp, $email);
    enforceOtpSendThrottle($pdo, $clientIp, $email);

    $stmt = $pdo->prepare('SELECT id, is_active FROM users WHERE email = :email LIMIT 1');
    $stmt->execute(['email' => $email]);
    $user = $stmt->fetch();

    $stmt = $pdo->prepare(
        'SELECT * FROM email_verifications WHERE email = :email ORDER BY created_at DESC, id DESC LIMIT 1'
    );
    $stmt->execute(['email' => $email]);
    $recent = $stmt->fetch();

    if (!$user && !$recent) {
        // Do not disclose whether the account exists.
        sendJsonResponse('success', 'If a verification is pending for this email, a new code has been sent.', [
            'resend_cooldown' => TS_OTP_RESEND_COOLDOWN_SEC,
        ]);
    }

    if ($recent) {
        $secondsSince = time() - strtotime((string) $recent['created_at']);
        if ($secondsSince < TS_OTP_RESEND_COOLDOWN_SEC) {
            $wait = TS_OTP_RESEND_COOLDOWN_SEC - $secondsSince;
            sendJsonResponse('error', "Please wait {$wait} seconds before requesting another code.", ['retry_after' => $wait], 429);
        }
    }

    if ($user && (int) $user['is_active'] !== 1) {
        sendJsonResponse('success', 'If a verification is pending for this email, a new code has been sent.', [
            'resend_cooldown' => TS_OTP_RESEND_COOLDOWN_SEC,
        ]);
    }

    // Preserve the original purpose of the pending verification instead of
    // silently downgrading every resend to "registration".
    $type = $recent ? (string) $recent['otp_type'] : ($user ? 'unverified_account' : 'registration');
    if (!in_array($type, ['registration', 'unverified_account', 'ip_change', 'login_challenge', 'password_reset'], true)) {
        $type = 'registration';
    }

    $pendingData = ($recent && !empty($recent['pending_data']))
        ? json_decode((string) $recent['pending_data'], true)
        : null;
    if ($type !== 'registration') {
        $pendingData = null;
    }

    $userId = $user ? (int) $user['id'] : ($recent ? (int) ($recent['user_id'] ?? 0) : 0);
    $delivery = issueEmailOtp($pdo, $userId > 0 ? $userId : null, $email, $type, is_array($pendingData) ? $pendingData : null, $clientIp);

    if (!$delivery['sent']) {
        sendJsonResponse('error', 'We could not send the email right now. Please try again shortly.', ['retry_after' => 60], 503);
    }

    sendJsonResponse('success', 'A new 6-digit code has been sent to your email.', [
        'email'           => $email,
        'masked_email'    => maskEmail($email),
        'resend_cooldown' => TS_OTP_RESEND_COOLDOWN_SEC,
    ]);
}

// ===========================================================================
// 5. REQUEST PASSWORD RESET
// ===========================================================================
if ($action === 'request_password_reset') {
    requirePost('request_password_reset');

    $email = normalizeEmail($body['email'] ?? '');

    // Never reveal whether the account exists: identical response + timing.
    $genericResponse = static function (): void {
        sendJsonResponse('success', 'If an account exists for this email, a password reset code has been sent.', [
            'resend_cooldown' => TS_OTP_RESEND_COOLDOWN_SEC,
        ]);
    };

    // Throttling is applied silently here: returning 429 only for real accounts
    // would itself be an enumeration oracle.
    if (otpSendThrottleWait($pdo, $clientIp, $email) > 0) {
        $genericResponse();
    }

    $stmt = $pdo->prepare('SELECT id, is_active, is_email_verified FROM users WHERE email = :email LIMIT 1');
    $stmt->execute(['email' => $email]);
    $user = $stmt->fetch();

    if (!$user || (int) $user['is_active'] !== 1) {
        $genericResponse();
    }

    issueEmailOtp($pdo, (int) $user['id'], $email, 'password_reset', null, $clientIp);
    $genericResponse();
}

// ===========================================================================
// 6. RESET PASSWORD (with reset token from verify_otp)
// ===========================================================================
if ($action === 'reset_password') {
    requirePost('reset_password');

    $resetToken = trim((string) ($body['reset_token'] ?? ''));
    $newPassword = validatePassword($body['new_password'] ?? '', 'New password');

    if (!preg_match('/^[a-f0-9]{64}$/i', $resetToken)) {
        sendJsonResponse('error', 'This reset link is invalid. Please start again.', [], 400);
    }

    $stmt = $pdo->prepare(
        'SELECT * FROM password_resets
         WHERE token_hash = :hash AND used_at IS NULL AND expires_at > NOW()
         LIMIT 1'
    );
    $stmt->execute(['hash' => hashToken($resetToken)]);
    $reset = $stmt->fetch();

    if (!$reset) {
        sendJsonResponse('error', 'This reset request has expired. Please start again.', [], 400);
    }

    $userId = (int) $reset['user_id'];
    $consume = $pdo->prepare('UPDATE password_resets SET used_at = NOW() WHERE id = :id AND used_at IS NULL');
    $consume->execute(['id' => $reset['id']]);
    if ($consume->rowCount() !== 1) {
        sendJsonResponse('error', 'This reset request has already been used.', [], 400);
    }

    $pdo->prepare('UPDATE users SET password_hash = :hash, updated_at = NOW() WHERE id = :id')
        ->execute(['hash' => hashPassword($newPassword), 'id' => $userId]);

    // Password change invalidates every existing session.
    revokeAllSessions($pdo, $userId);

    $emailStmt = $pdo->prepare('SELECT email FROM users WHERE id = :id LIMIT 1');
    $emailStmt->execute(['id' => $userId]);
    $resetEmail = (string) ($emailStmt->fetch()['email'] ?? '');
    if ($resetEmail !== '') {
        clearFailedAttempts($pdo, $clientIp, $resetEmail);
    }

    error_log('[TheStudent][auth] password reset completed for user ' . $userId);

    sendJsonResponse('success', 'Your password has been updated. Please sign in with your new password.');
}

// ===========================================================================
// 7. CHANGE PASSWORD (authenticated)
// ===========================================================================
if ($action === 'change_password') {
    requirePost('change_password');

    [$user] = requireSession($pdo, false);
    $currentPassword = (string) ($body['current_password'] ?? '');
    $newPassword = validatePassword($body['new_password'] ?? '', 'New password');

    if ($currentPassword === '' || empty($user['password_hash']) || !password_verify($currentPassword, $user['password_hash'])) {
        recordFailedAttempt($pdo, $clientIp, (string) $user['email']);
        sendJsonResponse('error', 'Your current password is incorrect.', [], 401);
    }

    if (password_verify($newPassword, $user['password_hash'])) {
        sendJsonResponse('error', 'Your new password must be different from the current one.', [], 400);
    }

    $pdo->prepare('UPDATE users SET password_hash = :hash, updated_at = NOW() WHERE id = :id')
        ->execute(['hash' => hashPassword($newPassword), 'id' => $user['id']]);

    revokeAllSessions($pdo, (int) $user['id']);
    $session = createSession($pdo, (int) $user['id'], $clientIp, 'Android Password Change');

    sendJsonResponse('success', 'Password updated. Other devices have been signed out.', [
        'token'      => $session['token'],
        'expires_at' => $session['expires_at'],
        'user'       => publicUser($user, ts_user_subjects($pdo, (int) $user['id'])),
    ]);
}

// ===========================================================================
// 8. GOOGLE LOGIN
// ===========================================================================
if ($action === 'google_login') {
    requirePost('google_login');

    $idToken = trim((string) ($body['id_token'] ?? ''));
    if ($idToken === '') {
        sendJsonResponse('error', 'Missing Google ID token.', [], 400);
    }

    enforceLoginRateLimit($pdo, $clientIp, 'google:' . substr(hash('sha256', $idToken), 0, 32));

    $claims = verifyGoogleIdToken($idToken);

    $googleId = (string) $claims['sub'];
    $email = strtolower(trim((string) $claims['email']));
    $fullName = trim((string) ($claims['name'] ?? 'Google Student'));
    $profilePicture = isset($claims['picture']) ? (string) $claims['picture'] : null;
    $targetExam = substr(trim((string) ($body['target_exam'] ?? 'G.C.E. A/L')), 0, 100);

    $stmt = $pdo->prepare('SELECT * FROM users WHERE google_id = :gid OR email = :email LIMIT 1');
    $stmt->execute(['gid' => $googleId, 'email' => $email]);
    $user = $stmt->fetch();

    if ($user) {
        $userId = (int) $user['id'];

        if ((int) $user['is_active'] !== 1) {
            sendJsonResponse('error', 'This account has been disabled. Please contact support.', [], 403);
        }

        // Link the Google identity only for the same, verified email address.
        if (!empty($user['google_id']) && $user['google_id'] !== $googleId) {
            sendJsonResponse('error', 'This account is linked to a different Google identity.', [], 409);
        }
        if (strcasecmp((string) $user['email'], $email) !== 0) {
            sendJsonResponse('error', 'Google account does not match this TheStudent account.', [], 409);
        }

        $pdo->prepare(
            'UPDATE users
             SET google_id = COALESCE(google_id, :gid),
                 full_name = :name,
                 profile_picture = COALESCE(:picture, profile_picture),
                 updated_at = NOW()
             WHERE id = :id'
        )->execute([
            'gid'     => $googleId,
            'name'    => substr($fullName, 0, 150),
            'picture' => $profilePicture,
            'id'      => $userId,
        ]);

        if ((int) $user['is_email_verified'] === 0) {
            enforceOtpSendThrottle($pdo, $clientIp, $email);
            $delivery = issueEmailOtp($pdo, $userId, $email, 'unverified_account', null, $clientIp);
            if (!$delivery['sent']) {
                sendJsonResponse('error', 'We could not send the verification email right now. Please try again shortly.', ['retry_after' => 60], 503);
            }
            sendJsonResponse('requires_verification', 'Your account still needs email verification. We sent a fresh code.', [
                'email'               => $email,
                'masked_email'        => maskEmail($email),
                'verification_reason' => 'unverified_account',
                'resend_cooldown'     => TS_OTP_RESEND_COOLDOWN_SEC,
            ]);
        }

        $lastIp = (string) ($user['last_login_ip'] ?? '');
        if (isNewNetwork($lastIp, $clientIp) || (int) ($user['suspicious_flag'] ?? 0) === 1) {
            enforceOtpSendThrottle($pdo, $clientIp, $email);
            $delivery = issueEmailOtp($pdo, $userId, $email, 'ip_change', null, $clientIp);
            if (!$delivery['sent']) {
                sendJsonResponse('error', 'We could not send the security code right now. Please try again shortly.', ['retry_after' => 60], 503);
            }
            sendJsonResponse('requires_verification', 'Sign-in from a new location detected. Enter the 6-digit code emailed to you.', [
                'email'               => $email,
                'masked_email'        => maskEmail($email),
                'verification_reason' => 'ip_change',
                'resend_cooldown'     => TS_OTP_RESEND_COOLDOWN_SEC,
            ]);
        }

        $pdo->prepare('UPDATE users SET last_login_ip = :ip, last_login_at = NOW(), suspicious_flag = 0 WHERE id = :id')
            ->execute(['ip' => substr($clientIp, 0, 45), 'id' => $userId]);

        $resolvedRole = roleForEmail($email, (string) $user['role']);
        if ($resolvedRole !== $user['role']) {
            $pdo->prepare('UPDATE users SET role = :role WHERE id = :id')->execute(['role' => $resolvedRole, 'id' => $userId]);
            $user['role'] = $resolvedRole;
        }

        clearFailedAttempts($pdo, $clientIp, $email);
        $session = createSession($pdo, $userId, $clientIp, 'Android Google Login');

        sendJsonResponse('success', 'Google sign-in successful', [
            'token'      => $session['token'],
            'expires_at' => $session['expires_at'],
            'user'       => publicUser(array_merge($user, [
                'full_name'       => $fullName !== '' ? $fullName : ($user['full_name'] ?? 'Student'),
                'profile_picture' => $profilePicture ?? ($user['profile_picture'] ?? null),
            ]), ts_user_subjects($pdo, $userId)),
        ]);
    }

    // New Google account: hold everything in pending_data until the emailed
    // code is verified, so no account row exists for unverified addresses.
    enforceOtpSendThrottle($pdo, $clientIp, $email);

    $pendingData = [
        'google_id'       => $googleId,
        'email'           => $email,
        'full_name'       => substr($fullName, 0, 150),
        'profile_picture' => $profilePicture,
        'target_exam'     => $targetExam,
        'auth_provider'   => 'google',
        'role'            => roleForEmail($email),
    ];

    $delivery = issueEmailOtp($pdo, null, $email, 'registration', $pendingData, $clientIp);
    if (!$delivery['sent']) {
        sendJsonResponse('error', 'We could not send the verification email right now. Please try again shortly.', ['retry_after' => 60], 503);
    }

    sendJsonResponse('requires_verification', 'We sent a 6-digit code to your Google email. Enter it to finish creating your account.', [
        'email'               => $email,
        'masked_email'        => maskEmail($email),
        'verification_reason' => 'registration',
        'resend_cooldown'     => TS_OTP_RESEND_COOLDOWN_SEC,
    ]);
}

// ===========================================================================
// 9. DELETE ACCOUNT (self-service - required by Google Play policy)
// ===========================================================================
if ($action === 'delete_account') {
    requirePost('delete_account');

    [$user] = requireSession($pdo, false);
    $userId   = (int) $user['id'];
    $password = (string) ($body['password'] ?? '');

    // Re-authenticate before an irreversible action. Accounts created through
    // Google may have no local password; for those the valid session is the
    // proof of ownership.
    if (!empty($user['password_hash'])) {
        if ($password === '' || !password_verify($password, (string) $user['password_hash'])) {
            recordFailedAttempt($pdo, $clientIp, (string) $user['email']);
            sendJsonResponse('error', 'Your password is incorrect.', [], 401);
        }
    }

    $email = strtolower((string) $user['email']);

    try {
        $pdo->beginTransaction();
        $pdo->prepare('DELETE FROM password_resets WHERE user_id = :uid')->execute(['uid' => $userId]);
        $pdo->prepare('DELETE FROM user_tokens WHERE user_id = :uid')->execute(['uid' => $userId]);
        $pdo->prepare('DELETE FROM email_verifications WHERE email = :email OR user_id = :uid')
            ->execute(['email' => $email, 'uid' => $userId]);
        $pdo->prepare('DELETE FROM login_attempts WHERE email = :email')->execute(['email' => $email]);
        $pdo->prepare('DELETE FROM users WHERE id = :uid')->execute(['uid' => $userId]);
        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log('[TheStudent][auth] account deletion failed: ' . $e->getMessage());
        sendJsonResponse('error', 'We could not delete your account right now. Please try again.', [], 500);
    }

    // Remove the uploaded profile photo too - "your data is deleted" must be
    // literally true, and orphaned avatars are still personal data.
    ts_avatar_delete($user['profile_picture'] ?? null);

    // No personal data is kept; the id only goes to the server log for auditing.
    error_log('[TheStudent][auth] account deleted: user ' . $userId);

    sendJsonResponse('success', 'Your account and personal data have been deleted.');
}

// ===========================================================================
// 10. ME / PROFILE
// ===========================================================================
if ($action === 'me' || $action === 'profile') {
    [$user, $expiresAt] = requireSession($pdo);

    sendJsonResponse('success', 'User profile retrieved', [
        'expires_at' => $expiresAt,
        'user'       => publicUser($user, ts_user_subjects($pdo, (int) $user['id'])),
    ]);
}

// ===========================================================================
// 11. LOGOUT
// ===========================================================================
if ($action === 'logout') {
    $token = extractBearerToken();
    if ($token !== null) {
        // Idempotent: deleting an unknown token is still a success.
        $pdo->prepare('DELETE FROM user_tokens WHERE token = :hash')->execute(['hash' => hashToken($token)]);
    }
    sendJsonResponse('success', 'Logged out successfully');
}

if ($action === 'logout_all') {
    [$user] = requireSession($pdo, false);
    revokeAllSessions($pdo, (int) $user['id']);
    sendJsonResponse('success', 'Signed out of all devices.');
}

// ---------------------------------------------------------------------------
// Unknown action
// ---------------------------------------------------------------------------
sendJsonResponse('error', 'Invalid API action.', [], 400);
