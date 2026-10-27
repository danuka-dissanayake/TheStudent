<?php
/**
 * TheStudent - Database & System Configuration
 * Compatible with InfinityFree, standard cPanel, and local development.
 */

// Report errors during development (change to 0 in live production)
error_reporting(E_ALL);
ini_set('display_errors', 0);

// Session initialization
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

// --------------------------------------------------------------------------
// DATABASE CONFIGURATION (Fill these with details from your InfinityFree vPanel)
// --------------------------------------------------------------------------
// On InfinityFree: Find these under 'MySQL Databases' in your account panel
define('DB_HOST', 'sql300.infinityfree.com'); // e.g. sql300.infinityfree.com or localhost
define('DB_NAME', 'if0_38000000_thestudent');  // e.g. if0_38000000_thestudent
define('DB_USER', 'if0_38000000');           // e.g. if0_38000000
define('DB_PASS', 'your_vpanel_password');   // your vPanel account password
define('DB_CHARSET', 'utf8mb4');

// Base Paths
define('SITE_NAME', 'TheStudent - Past Papers');
define('BASE_URL', '/'); // Set to your domain root, e.g., 'http://yourdomain.infinityfreeapp.com/'
define('UPLOAD_DIR', __DIR__ . '/uploads/papers/');

// Ensure upload directory exists
if (!file_exists(UPLOAD_DIR)) {
    @mkdir(UPLOAD_DIR, 0755, true);
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
            // Friendly error message for users
            die(json_encode([
                'status' => 'error',
                'message' => 'Database connection failed. Please verify DB credentials in config.php. ' . $e->getMessage()
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
