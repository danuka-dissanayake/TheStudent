<?php
/**
 * TheStudent - Quiz hero image upload
 *
 * URL: /api/quiz_image.php
 * Method: POST (multipart/form-data)
 * Field:  image   — a JPEG, PNG or WebP file, up to 5 MB
 *
 * Admin only. Stores the file under `uploads/quizzes/` and replies with the path
 * to save on the quiz:
 *
 *   { "status": "success", "hero_image_url": "/uploads/quizzes/quiz-7f3a91c2.jpg" }
 *
 * Why an upload instead of a link: the quiz hero is part of the product, so the
 * image has to live on our own server. A URL pointing at someone else's site can
 * disappear, change, or silently start showing a different picture — none of
 * which an admin would notice until a student did.
 */

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/_admin_guard.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

const TS_QUIZ_IMAGE_MAX_BYTES = 5_242_880; // 5 MB
const TS_QUIZ_IMAGE_MIME = [
    'image/jpeg' => 'jpg',
    'image/png'  => 'png',
    'image/webp' => 'webp',
];

try {
    $pdo = getDbConnection();

    // Same guard as every other admin write: a student cannot upload artwork.
    $adminUser = ts_require_admin_api($pdo);

    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        http_response_code(405);
        header('Allow: POST');
        echo json_encode(['status' => 'error', 'message' => 'Only POST requests are allowed.']);
        exit();
    }

    if (!isset($_FILES['image'])) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'Choose an image to upload.']);
        exit();
    }

    $file = $_FILES['image'];
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_NO_FILE) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'Choose an image to upload.']);
        exit();
    }
    if ($file['error'] !== UPLOAD_ERR_OK) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'The image upload failed. Please try again.']);
        exit();
    }
    if (($file['size'] ?? 0) > TS_QUIZ_IMAGE_MAX_BYTES) {
        http_response_code(413);
        echo json_encode(['status' => 'error', 'message' => 'That image is larger than 5 MB. Please use a smaller one.']);
        exit();
    }

    // Never trust the browser's Content-Type: read the bytes.
    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mime = $finfo->file($file['tmp_name']);
    if (!isset(TS_QUIZ_IMAGE_MIME[$mime])) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'The image must be a JPEG, PNG or WebP file.']);
        exit();
    }
    if (@getimagesize($file['tmp_name']) === false) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'That file is not a readable image.']);
        exit();
    }

    $uploadDir = __DIR__ . '/../uploads/quizzes/';
    if (!file_exists($uploadDir) && !@mkdir($uploadDir, 0755, true) && !is_dir($uploadDir)) {
        http_response_code(500);
        echo json_encode(['status' => 'error', 'message' => 'The uploads folder could not be created.']);
        exit();
    }

    $extension = TS_QUIZ_IMAGE_MIME[$mime];
    $fileName = 'quiz-' . bin2hex(random_bytes(4)) . '.' . $extension;
    $target = $uploadDir . $fileName;

    if (!@move_uploaded_file($file['tmp_name'], $target)) {
        http_response_code(500);
        echo json_encode(['status' => 'error', 'message' => 'The image could not be saved on the server.']);
        exit();
    }
    @chmod($target, 0644);

    // Read back the size the app will actually download, so the admin panel can
    // tell the admin what they just stored.
    $bytes = @filesize($target) ?: 0;

    echo json_encode([
        'status' => 'success',
        'message' => 'Image uploaded.',
        'hero_image_url' => '/uploads/quizzes/' . $fileName,
        'bytes' => $bytes,
    ]);
} catch (Throwable $e) {
    error_log('[TheStudent] quiz image upload failed: ' . $e->getMessage());
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => 'The image could not be uploaded.']);
}
