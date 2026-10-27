<?php
/**
 * TheStudent - Backend Bootstrap & Configuration
 * ===============================================
 *
 * SECURITY MODEL
 * --------------
 * This file NEVER contains credentials. Secrets are loaded (in order) from:
 *
 *   1. Environment variables (preferred on DirectAdmin / cPanel / Docker):
 *        THESTUDENT_APP_ENV, THESTUDENT_APP_KEY,
 *        THESTUDENT_DB_HOST, THESTUDENT_DB_NAME, THESTUDENT_DB_USER, THESTUDENT_DB_PASS,
 *        THESTUDENT_ADMIN_EMAILS, THESTUDENT_GOOGLE_CLIENT_ID,
 *        THESTUDENT_SMTP_HOST, THESTUDENT_SMTP_PORT, THESTUDENT_SMTP_USER,
 *        THESTUDENT_SMTP_PASS, THESTUDENT_SMTP_FROM_NAME,
 *        THESTUDENT_ALLOWED_ORIGINS, THESTUDENT_TRUST_PROXY_HEADERS
 *
 *   2. A sibling file `config.secrets.php` (git-ignored, chmod 600) that defines
 *      the same values as constants. Copy `config.secrets.example.php` to start.
 *
 * If credentials are missing the API fails closed with a generic 503 - it never
 * leaks database or SMTP details to the caller.
 */

declare(strict_types=1);

error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

// PHP errors go to <domain>/php-error.log (one level above public_html) and a
// fatal-error handler turns blank 500s into JSON + a reference id.
require_once __DIR__ . '/error_handlers.php';
ts_install_error_handlers();

// ---------------------------------------------------------------------------
// Environment loading
// ---------------------------------------------------------------------------
if (!function_exists('ts_env')) {
    /**
     * Read an environment variable, falling back to a value defined by the
     * local secrets file (constant) when available.
     */
    function ts_env(string $key, $default = null)
    {
        $value = getenv($key);
        if ($value !== false && $value !== '') {
            return $value;
        }
        $constName = 'THESTUDENT_' . $key;
        if (defined($constName)) {
            $constValue = constant($constName);
            if ($constValue !== null && $constValue !== '') {
                return $constValue;
            }
        }
        return $default;
    }
}

/*
 * Secrets file discovery, in priority order:
 *
 *   1. one level ABOVE public_html  <- recommended: the web server never serves it
 *      e.g. /home/USER/domains/app.quizontal.site/config.secrets.php
 *   2. two levels above            e.g. /home/USER/domains/config.secrets.php
 *   3. next to this file           <- legacy; only safe thanks to .htaccess
 *
 * DirectAdmin's File Manager can edit files in the domain folder, so option 1
 * needs no SSH at all. See docs/DEPLOY_DIRECTADMIN_GUI.md.
 */
$__tsSecretsCandidates = [
    dirname(__DIR__) . '/config.secrets.php',
    dirname(__DIR__, 2) . '/config.secrets.php',
    __DIR__ . '/config.secrets.php',
];
$__tsSecretsFile = null;
foreach ($__tsSecretsCandidates as $__tsCandidate) {
    if (is_file($__tsCandidate)) {
        $__tsSecretsFile = $__tsCandidate;
        break;
    }
}
if ($__tsSecretsFile !== null) {
    require_once $__tsSecretsFile;
    // Warn (server log only) when the secrets sit inside the web root.
    if (strpos((string) realpath($__tsSecretsFile), (string) realpath(__DIR__)) === 0) {
        error_log('[TheStudent] config.secrets.php is inside the web root - move it one level above public_html.');
    }
}

// Legacy constant names (kept so older deploy scripts keep working).
$__tsLegacyMap = [
    'DB_HOST'      => 'DB_HOST',
    'DB_NAME'      => 'DB_NAME',
    'DB_USER'      => 'DB_USER',
    'DB_PASS'      => 'DB_PASS',
    'DB_CHARSET'   => 'DB_CHARSET',
    'APP_ENV'      => 'APP_ENV',
    'APP_KEY'      => 'APP_KEY',
    'SMTP_HOST'    => 'SMTP_HOST',
    'SMTP_PORT'    => 'SMTP_PORT',
    'SMTP_USER'    => 'SMTP_USER',
    'SMTP_PASS'    => 'SMTP_PASS',
    'SMTP_FROM_NAME' => 'SMTP_FROM_NAME',
    'GOOGLE_WEB_CLIENT_ID' => 'GOOGLE_WEB_CLIENT_ID',
    'ADMIN_EMAILS' => 'ADMIN_EMAILS',
    'ALLOWED_ORIGINS' => 'ALLOWED_ORIGINS',
    'TRUST_PROXY_HEADERS' => 'TRUST_PROXY_HEADERS',
    'TRUSTED_PROXIES' => 'TRUSTED_PROXIES',
    'REQUIRE_HTTPS' => 'REQUIRE_HTTPS',
];

if (!defined('APP_ENV')) {
    define('APP_ENV', (string) ts_env('APP_ENV', 'production'));
}
if (!defined('APP_DEBUG')) {
    define('APP_DEBUG', APP_ENV === 'local');
}

foreach ($__tsLegacyMap as $constName => $envSuffix) {
    if (defined($constName)) {
        continue;
    }
    $fallback = '';
    if ($constName === 'DB_HOST')      { $fallback = 'localhost'; }
    if ($constName === 'DB_CHARSET')   { $fallback = 'utf8mb4'; }
    if ($constName === 'APP_KEY')      { $fallback = ''; }
    if ($constName === 'SMTP_HOST')    { $fallback = 'ssl://smtp.gmail.com'; }
    if ($constName === 'SMTP_PORT')    { $fallback = 465; }
    if ($constName === 'SMTP_FROM_NAME') { $fallback = 'TheStudent Security'; }
    if ($constName === 'ALLOWED_ORIGINS') { $fallback = 'https://app.quizontal.site'; }
    if ($constName === 'TRUST_PROXY_HEADERS') { $fallback = '0'; }
    if ($constName === 'TRUSTED_PROXIES') { $fallback = ''; }
    if ($constName === 'REQUIRE_HTTPS') { $fallback = '1'; }
    if ($constName === 'ADMIN_EMAILS') { $fallback = ''; }
    define($constName, ts_env($envSuffix, $fallback));
}

if (!defined('DB_NAME'))     { define('DB_NAME', ''); }
if (!defined('DB_USER'))     { define('DB_USER', ''); }
if (!defined('DB_PASS'))     { define('DB_PASS', ''); }

// ---------------------------------------------------------------------------
// Fail closed when the deployment is not configured
// ---------------------------------------------------------------------------
$__tsMissing = [];
if ((string) DB_NAME === '') { $__tsMissing[] = 'DB_NAME'; }
if ((string) DB_USER === '') { $__tsMissing[] = 'DB_USER'; }
if ((string) APP_KEY === '' || strlen((string) APP_KEY) < 32) { $__tsMissing[] = 'APP_KEY (min 32 chars)'; }

if (!empty($__tsMissing)) {
    error_log('[TheStudent] Missing configuration: ' . implode(', ', $__tsMissing));
    http_response_code(503);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    header('Retry-After: 300');
    echo json_encode([
        'status'  => 'error',
        'message' => 'Service temporarily unavailable. Please try again later.',
    ]);
    exit();
}

// ---------------------------------------------------------------------------
// Transport helpers
// ---------------------------------------------------------------------------
function ts_is_https(): bool
{
    if (!empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off') {
        return true;
    }
    if (isset($_SERVER['SERVER_PORT']) && (int) $_SERVER['SERVER_PORT'] === 443) {
        return true;
    }
    if (ts_trusts_proxy_headers()) {
        $proto = $_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '';
        if (strtolower(trim(explode(',', (string) $proto)[0])) === 'https') {
            return true;
        }
        if (strtolower((string) ($_SERVER['HTTP_CF_VISITOR'] ?? '')) === '{"scheme":"https"}') {
            return true;
        }
    }
    return false;
}

/**
 * Whether plain-HTTP requests must be rejected (default: yes in production).
 * Set REQUIRE_HTTPS=0 only for local development.
 */
function ts_requires_https(): bool
{
    if (APP_ENV === 'local') {
        return false;
    }
    $flag = (string) (defined('REQUIRE_HTTPS') ? REQUIRE_HTTPS : '1');
    return in_array(strtolower($flag), ['1', 'true', 'yes', 'on'], true);
}

function ts_trusts_proxy_headers(): bool
{
    $flag = (string) (defined('TRUST_PROXY_HEADERS') ? TRUST_PROXY_HEADERS : '0');
    return in_array(strtolower($flag), ['1', 'true', 'yes', 'on'], true);
}

/**
 * Resolve the real client IP.
 *
 * Forwarded headers are ONLY honoured when TRUST_PROXY_HEADERS is explicitly
 * enabled in the server secrets. Blindly trusting X-Forwarded-For lets an
 * attacker rotate the header to bypass per-IP rate limits and the
 * "new location" OTP challenge.
 */
function ts_client_ip(): string
{
    $remote = $_SERVER['REMOTE_ADDR'] ?? '';
    if (!ts_trusts_proxy_headers()) {
        return $remote !== '' ? $remote : '0.0.0.0';
    }

    $trusted = array_filter(array_map('trim', explode(',', (string) (defined('TRUSTED_PROXIES') ? TRUSTED_PROXIES : ''))));
    if (!empty($trusted) && !in_array($remote, $trusted, true)) {
        return $remote !== '' ? $remote : '0.0.0.0';
    }

    $candidates = [
        $_SERVER['HTTP_CF_CONNECTING_IP'] ?? '',
        $_SERVER['HTTP_X_REAL_IP'] ?? '',
        $_SERVER['HTTP_X_FORWARDED_FOR'] ?? '',
    ];
    foreach ($candidates as $candidate) {
        if ($candidate === '') {
            continue;
        }
        $ip = trim(explode(',', (string) $candidate)[0]);
        if (filter_var($ip, FILTER_VALIDATE_IP)) {
            return $ip;
        }
    }
    return $remote !== '' ? $remote : '0.0.0.0';
}

/**
 * Emit standard security headers + a strict CORS policy.
 */
function ts_apply_security_headers(): void
{
    if (!headers_sent()) {
        header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
        header('Pragma: no-cache');
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: DENY');
        header('Referrer-Policy: no-referrer');
        header('X-XSS-Protection: 0'); // Modern browsers: rely on CSP, disable legacy filter.
    }

    $allowed = array_filter(array_map('trim', explode(',', (string) ALLOWED_ORIGINS)));
    $origin = $_SERVER['HTTP_ORIGIN'] ?? '';

    if ($origin !== '' && !empty($allowed) && in_array('*', $allowed, true)) {
        // Wide-open CORS is only acceptable for the public read-only papers API.
        header('Access-Control-Allow-Origin: *');
    } elseif ($origin !== '' && in_array($origin, $allowed, true)) {
        header('Access-Control-Allow-Origin: ' . $origin);
        header('Vary: Origin');
        header('Access-Control-Allow-Credentials: false');
    }

    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
    header('Access-Control-Max-Age: 600');

    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
        http_response_code(204);
        exit();
    }
}

// ---------------------------------------------------------------------------
// Database
// ---------------------------------------------------------------------------
/**
 * Returns a shared PDO connection.
 *
 * Errors are logged server-side and reported to the client as a generic 503 so
 * database host/name/user details are never exposed.
 */
function getDbConnection(): PDO
{
    static $connection = null;
    if ($connection instanceof PDO) {
        return $connection;
    }

    $dsn = sprintf('mysql:host=%s;dbname=%s;charset=%s', DB_HOST, DB_NAME, DB_CHARSET);
    $options = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
        PDO::ATTR_STRINGIFY_FETCHES  => false,
    ];

    try {
        $connection = new PDO($dsn, DB_USER, DB_PASS, $options);
        return $connection;
    } catch (Throwable $e) {
        error_log('[TheStudent] DB connection failed: ' . $e->getMessage());
        http_response_code(503);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        echo json_encode([
            'status'  => 'error',
            'message' => 'Service temporarily unavailable. Please try again later.',
        ]);
        exit();
    }
}

/**
 * Ensure a column exists on a table (idempotent, safe to run on every boot).
 */
function ts_ensure_column(PDO $pdo, string $table, string $column, string $definition): void
{
    try {
        $stmt = $pdo->prepare("SHOW COLUMNS FROM `$table` LIKE :column");
        $stmt->execute(['column' => $column]);
        if ($stmt->fetch()) {
            return;
        }
        $pdo->exec("ALTER TABLE `$table` ADD COLUMN `$column` $definition");
    } catch (Throwable $e) {
        error_log("[TheStudent] ensure_column($table.$column) skipped: " . $e->getMessage());
    }
}
