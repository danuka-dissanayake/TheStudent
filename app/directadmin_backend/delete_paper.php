<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'db_config.php';

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput, true) ?: $_POST;

$paperId = isset($data['paper_id']) ? trim($data['paper_id']) : null;

if (!$paperId) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "paper_id parameter is required"]);
    exit();
}

$stmt = $conn->prepare("DELETE FROM past_papers WHERE id = ?");
$stmt->bind_param("s", $paperId);

if ($stmt->execute()) {
    echo json_encode([
        "status" => "success",
        "message" => "Paper deleted successfully"
    ]);
} else {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database delete failed: " . $stmt->error
    ]);
}

$stmt->close();
$conn->close();
?>
