<?php
// Database configuration for DirectAdmin MySQL Host
$db_host = "localhost";
$db_user = "YOUR_DATABASE_USER";      // e.g. thestudent_user
$db_pass = "YOUR_DATABASE_PASSWORD";  // e.g. YourSecretPassword123!
$db_name = "YOUR_DATABASE_NAME";      // e.g. thestudent_papers_db

$conn = new mysqli($db_host, $db_user, $db_pass, $db_name);

if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database Connection Failed: " . $conn->connect_error
    ]);
    exit();
}

$conn->set_charset("utf8mb4");
?>
