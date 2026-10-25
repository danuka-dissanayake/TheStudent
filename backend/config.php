<?php
/**
 * DZD Learn - InfinityFree MySQL Configuration
 * 
 * Instructions for InfinityFree:
 * 1. Log in to your InfinityFree Control Panel (cPanel / vPanel).
 * 2. Click on "MySQL Databases".
 * 3. Copy your MySQL Host (e.g., sql108.infinityfree.com or sql202.epizy.com).
 * 4. Copy your MySQL Username (e.g., epiz_12345678).
 * 5. Use your vPanel account password.
 * 6. Copy your Database Name (e.g., epiz_12345678_dzdlearn).
 * 7. Update the variables below.
 */

// Enable error reporting during setup (turn off in production if desired)
error_reporting(E_ALL);
ini_set('display_errors', 0);

// --- INFINITYFREE DATABASE CREDENTIALS ---
$DB_HOST = 'sql108.infinityfree.com'; // Replace with your InfinityFree MySQL Host
$DB_NAME = 'epiz_xxxxxxx_dzdlearn';    // Replace with your Database Name
$DB_USER = 'epiz_xxxxxxx';            // Replace with your MySQL Username
$DB_PASS = 'your_infinityfree_password'; // Replace with your Account Password
$DB_CHARSET = 'utf8mb4';

// Allowed origins for CORS (allowing mobile app & web requests)
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
        // Return a clean JSON response if connection fails
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode([
            'status' => 'error',
            'message' => 'Database connection failed: ' . $e->getMessage(),
            'hint' => 'Check your database host, name, username, and password in config.php'
        ]);
        exit();
    }
}
