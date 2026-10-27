<?php
/**
 * TheStudent - Database & System Configuration
 * Compatible with InfinityFree, standard cPanel, and local development.
 */

// Report errors during development (change to 0 in live production)
error_reporting(E_ALL);
ini_set('display_errors', 0);

// Session initialization
// Harden the session cookie before the session starts: HttpOnly stops JS from
// reading it, SameSite=Lax blocks cross-site form posts (CSRF), and Secure is
// enabled automatically when the request is served over HTTPS.
if (session_status() === PHP_SESSION_NONE) {
    $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https')
        || (int)($_SERVER['SERVER_PORT'] ?? 0) === 443;

    @ini_set('session.use_strict_mode', '1');
    @ini_set('session.use_only_cookies', '1');
    @ini_set('session.cookie_httponly', '1');
    session_set_cookie_params([
        'lifetime' => 0,
        'path'     => '/',
        'httponly' => true,
        'secure'   => $isHttps,
        'samesite' => 'Lax',
    ]);
    session_start();
}

// --------------------------------------------------------------------------
// DATABASE CONFIGURATION (Fill these with details from your InfinityFree vPanel)
// --------------------------------------------------------------------------
// On InfinityFree: Find these under 'MySQL Databases' in your account panel
// Credentials come from the environment when available, so they never need to be
// committed. A local config.secrets.php (git-ignored) can define them too.
if (is_file(__DIR__ . '/config.secrets.php')) {
    require_once __DIR__ . '/config.secrets.php';
}
define('DB_HOST', getenv('DB_HOST') ?: (defined('TS_DB_HOST') ? TS_DB_HOST : 'sql300.infinityfree.com'));
define('DB_NAME', getenv('DB_NAME') ?: (defined('TS_DB_NAME') ? TS_DB_NAME : 'if0_38000000_thestudent'));
define('DB_USER', getenv('DB_USER') ?: (defined('TS_DB_USER') ? TS_DB_USER : 'if0_38000000'));
define('DB_PASS', getenv('DB_PASS') ?: (defined('TS_DB_PASS') ? TS_DB_PASS : ''));
define('DB_CHARSET', 'utf8mb4');

// Base Paths
define('SITE_NAME', 'TheStudent - Past Papers');
define('BASE_URL', '/'); // Set to your domain root, e.g., 'http://yourdomain.infinityfreeapp.com/'
define('UPLOAD_DIR', __DIR__ . '/uploads/papers/');

// Ensure upload directory exists
if (!file_exists(UPLOAD_DIR)) {
    @mkdir(UPLOAD_DIR, 0755, true);
}

// Block script execution inside the uploads folder. Without this, anything that
// lands there (even a "PDF" with a .php extension) can be executed by the server.
$htaccessPath = UPLOAD_DIR . '.htaccess';
if (!file_exists($htaccessPath)) {
    @file_put_contents(
        $htaccessPath,
        "<FilesMatch \"\.(php|phtml|php3|php4|php5|php7|phps|pl|py|cgi|sh|shtml)$\">\n"
        . "    Require all denied\n"
        . "</FilesMatch>\n"
        . "Options -ExecCGI -Indexes\n"
    );
}

// PDO Database connection singleton
function getDbConnection() {
    static $pdo = null;
    if ($pdo === null) {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];
        try {
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (\PDOException $e) {
            // Never leak driver details / credentials to the browser: log the
            // real reason, return a generic message.
            error_log('TheStudent DB connect failed: ' . $e->getMessage());
            if (!headers_sent()) {
                header('Content-Type: application/json; charset=utf-8');
                http_response_code(500);
            }
            die(json_encode([
                'status'  => 'error',
                'message' => 'The service is temporarily unavailable. Please try again later.'
            ]));
        }
    }
    return $pdo;
}

// Helper: Escape HTML
function e($string) {
    return htmlspecialchars($string ?? '', ENT_QUOTES, 'UTF-8');
}

// Helper: Check admin authentication
function checkAdminAuth() {
    if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) {
        header('Location: login.php');
        exit;
    }
}
