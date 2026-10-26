<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once 'db_config.php';

$category = isset($_GET['category']) ? trim($_GET['category']) : null;
$grade = isset($_GET['grade']) ? trim($_GET['grade']) : null;
$stream = isset($_GET['stream']) ? trim($_GET['stream']) : null;
$subject = isset($_GET['subject']) ? trim($_GET['subject']) : null;
$year = isset($_GET['year']) ? intval($_GET['year']) : null;
$medium = isset($_GET['medium']) ? trim($_GET['medium']) : null;
$search = isset($_GET['search']) ? trim($_GET['search']) : null;

$sql = "SELECT * FROM past_papers WHERE status = 'published'";
$params = [];
$types = "";

if ($category && $category !== "All Categories" && $category !== "All") {
    $sql .= " AND (category = ? OR grade LIKE ?)";
    $params[] = $category;
    $params[] = "%" . $category . "%";
    $types .= "ss";
}

if ($grade && $grade !== "All Grades" && $grade !== "All") {
    $sql .= " AND grade = ?";
    $params[] = $grade;
    $types .= "s";
}

if ($stream && $stream !== "All Streams" && $stream !== "All") {
    $sql .= " AND stream = ?";
    $params[] = $stream;
    $types .= "s";
}

if ($subject && $subject !== "All Subjects" && $subject !== "All") {
    $sql .= " AND subject LIKE ?";
    $params[] = "%" . $subject . "%";
    $types .= "s";
}

if ($year && $year > 0) {
    $sql .= " AND year = ?";
    $params[] = $year;
    $types .= "i";
}

if ($medium && $medium !== "All Mediums" && $medium !== "All") {
    $sql .= " AND medium = ?";
    $params[] = $medium;
    $types .= "s";
}

if ($search) {
    $sql .= " AND (title LIKE ? OR subject LIKE ? OR description LIKE ? OR stream LIKE ?)";
    $searchTerm = "%" . $search . "%";
    $params[] = $searchTerm;
    $params[] = $searchTerm;
    $params[] = $searchTerm;
    $params[] = $searchTerm;
    $types .= "ssss";
}

$sql .= " ORDER BY year DESC, created_at DESC";

$stmt = $conn->prepare($sql);

if (!empty($params)) {
    $stmt->bind_param($types, ...$params);
}

$stmt->execute();
$result = $stmt->get_result();

$papers = [];
while ($row = $result->fetch_assoc()) {
    $row['has_marking_scheme'] = (bool)$row['has_marking_scheme'];
    $papers[] = $row;
}

echo json_encode([
    "status" => "success",
    "count" => count($papers),
    "data" => $papers
], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>
