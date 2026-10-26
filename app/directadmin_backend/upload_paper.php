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

if (empty($data['title'])) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Paper title is required."]);
    exit();
}

$id = "paper-" . time() . "-" . rand(1000, 9999);
$title = trim($data['title']);
$year = isset($data['year']) ? intval($data['year']) : date("Y");
$duration = isset($data['duration']) ? trim($data['duration']) : "3 Hours";
$pages = isset($data['pages']) ? intval($data['pages']) : 16;
$medium = isset($data['medium']) ? trim($data['medium']) : "English";
$hasMarkingScheme = isset($data['has_marking_scheme']) ? ($data['has_marking_scheme'] ? 1 : 0) : 1;
$thumbnailUrl = isset($data['thumbnail_url']) ? trim($data['thumbnail_url']) : "";
$category = isset($data['category']) ? trim($data['category']) : "A/L";
$grade = isset($data['grade']) ? trim($data['grade']) : "Grade 13";
$stream = isset($data['stream']) ? trim($data['stream']) : "Physical Science";
$subject = isset($data['subject']) ? trim($data['subject']) : "General";
$pdfUrl = isset($data['pdf_url']) ? trim($data['pdf_url']) : "";
$markingSchemeUrl = isset($data['marking_scheme_url']) ? trim($data['marking_scheme_url']) : null;
$description = isset($data['description']) ? trim($data['description']) : "";

$sql = "INSERT INTO past_papers (id, title, year, duration, pages, medium, has_marking_scheme, thumbnail_url, category, grade, stream, subject, pdf_url, marking_scheme_url, description) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

$stmt = $conn->prepare($sql);
$stmt->bind_param(
    "ssisissssssssss",
    $id, $title, $year, $duration, $pages, $medium, $hasMarkingScheme, 
    $thumbnailUrl, $category, $grade, $stream, $subject, $pdfUrl, $markingSchemeUrl, $description
);

if ($stmt->execute()) {
    echo json_encode([
        "status" => "success",
        "message" => "Paper uploaded successfully",
        "paper_id" => $id
    ]);
} else {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Failed to insert paper into MySQL: " . $stmt->error
    ]);
}

$stmt->close();
$conn->close();
?>
