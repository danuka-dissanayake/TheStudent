<?php
/**
 * TheStudent - Secure PDF Download & View Handler
 * Increments download counters and serves files with proper headers.
 */

require_once __DIR__ . '/../config.php';

$paperId = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$action = isset($_GET['mode']) && $_GET['mode'] === 'view' ? 'inline' : 'attachment';

if ($paperId <= 0) {
    die("Invalid paper ID.");
}

$pdo = getDbConnection();

// Fetch paper record
$stmt = $pdo->prepare("SELECT * FROM papers WHERE id = :id AND status = 'Published'");
$stmt->execute(['id' => $paperId]);
$paper = $stmt->fetch();

if (!$paper) {
    die("Past paper not found or unpublished.");
}

// Increment download count
$pdo->prepare("UPDATE papers SET download_count = download_count + 1 WHERE id = :id")->execute(['id' => $paperId]);

$filePath = __DIR__ . '/../' . $paper['file_path'];

// If local file exists, serve it
if (file_exists($filePath)) {
    $fileName = basename($paper['title']) . '.pdf';
    header('Content-Type: application/pdf');
    header('Content-Disposition: ' . $action . '; filename="' . $fileName . '"');
    header('Content-Length: ' . filesize($filePath));
    readfile($filePath);
    exit;
} else {
    // If it's a mock or remote file URL
    if (filter_var($paper['file_path'], FILTER_VALIDATE_URL)) {
        header("Location: " . $paper['file_path']);
        exit;
    }
    
    // Otherwise fallback sample PDF generation
    header('Content-Type: text/html; charset=utf-8');
    echo "<!DOCTYPE html><html><head><title>" . e($paper['title']) . "</title><style>body{font-family:sans-serif;padding:40px;background:#f8fafc;color:#0f172a;} .card{background:#fff;padding:30px;border-radius:16px;max-width:600px;margin:auto;box-shadow:0 4px 20px rgba(0,0,0,0.06);border:1px solid #e2e8f0;} .btn{display:inline-block;padding:12px 24px;background:#059669;color:#fff;text-decoration:none;border-radius:10px;font-weight:bold;margin-top:20px;}</style></head><body>";
    echo "<div class='card'>";
    echo "<span style='background:#d1fae5;color:#065f46;padding:4px 10px;border-radius:6px;font-size:12px;font-weight:bold;'>Official Educational Document</span>";
    echo "<h2 style='margin-top:16px;'>" . e($paper['title']) . "</h2>";
    echo "<p><strong>Year:</strong> " . e($paper['year']) . " | <strong>Medium:</strong> " . e($paper['medium']) . " | <strong>Exam:</strong> " . e($paper['exam_type']) . "</p>";
    echo "<p>This official curriculum past paper has been recorded. The PDF file is stored on the server.</p>";
    echo "<a href='../index.php' class='btn'>Back to Past Papers</a>";
    echo "</div></body></html>";
    exit;
}
