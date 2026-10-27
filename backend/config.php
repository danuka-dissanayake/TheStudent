<?php
/**
 * TheStudent - DirectAdmin MySQL Database Configuration
 * 
 * Configured with live DirectAdmin database and Gmail SMTP credentials.
 */

// Error reporting (0 for production)
error_reporting(E_ALL);
ini_set('display_errors', 0);

// --- DIRECTADMIN DATABASE CREDENTIALS ---
$DB_HOST = 'localhost';             // In DirectAdmin, this is usually 'localhost' or '127.0.0.1'
$DB_NAME = 'quizontalcloud_app';     // DirectAdmin database name
$DB_USER = 'quizontalcloud_app';     // DirectAdmin database user
$DB_PASS = 'GrjSmXMjva7mseBA3CG4'; // Password created when making the database user
$DB_CHARSET = 'utf8mb4';

// --- GOOGLE OAUTH CREDENTIALS ---
define('GOOGLE_WEB_CLIENT_ID', '849765952531-7bnroph62m0homvnpfa3nngkmcorb4mf.apps.googleusercontent.com');

// --- SMTP EMAIL CREDENTIALS (GMAIL SMTP) ---
define('SMTP_HOST', 'ssl://smtp.gmail.com');
define('SMTP_PORT', 465);
define('SMTP_USER', 'yourownwpbot@gmail.com');
define('SMTP_PASS', 'tllmuempbiotaltb');
define('SMTP_FROM_NAME', 'TheStudent Security');

// Allowed origins for CORS (enabling Android mobile app & web dashboard requests)
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

// Handle preflight OPTIONS requests immediately
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

/**
 * Returns a PDO database connection instance.
 */
function getDbConnection() {
    global $DB_HOST, $DB_NAME, $DB_USER, $DB_PASS, $DB_CHARSET;
    
    $dsn = "mysql:host={$DB_HOST};dbname={$DB_NAME};charset={$DB_CHARSET}";
    $options = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ];
    
    try {
        return new PDO($dsn, $DB_USER, $DB_PASS, $options);
    } catch (PDOException $e) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode([
            'status'  => 'error',
            'message' => 'Database connection failed: ' . $e->getMessage(),
            'hint'    => 'Check your database host, name, username, and password in config.php'
        ]);
        exit();
    }
}
