<?php
/**
 * TheStudent - Past Paper Upload API Endpoint
 * 
 * URL: /api/upload_paper.php
 * Method: POST
 * Accepts: JSON or Multipart Form-Data (with PDF file upload)
 */

require_once __DIR__ . '/../config.php';

header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = getDbConnection();

    // Support both raw JSON body and standard Form POST
    $rawInput = file_get_contents('php://input');
    $data = json_decode($rawInput, true);
    if (!is_array($data)) {
        $data = $_POST;
    }

    // Ensure uploads directory exists
    $uploadDir = __DIR__ . '/../uploads/';
    if (!file_exists($uploadDir)) {
        @mkdir($uploadDir, 0755, true);
    }

    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
    $host = $_SERVER['HTTP_HOST'];
    $baseUrl = $protocol . "://" . $host . rtrim(dirname(dirname($_SERVER['SCRIPT_NAME'])), '/');

    // Handle PDF file upload if provided via multipart/form-data
    $pdfUrl = trim($data['pdf_url'] ?? '');
    if (isset($_FILES['pdf_file']) && $_FILES['pdf_file']['error'] === UPLOAD_ERR_OK) {
        $cleanFileName = time() . '_' . preg_replace('/[^a-zA-Z0-9_\.-]/', '_', basename($_FILES['pdf_file']['name']));
        $targetFile = $uploadDir . $cleanFileName;
        if (move_uploaded_file($_FILES['pdf_file']['tmp_name'], $targetFile)) {
            $pdfUrl = $baseUrl . '/uploads/' . $cleanFileName;
        }
    }

    // Handle Marking Scheme file upload if provided
    $schemeUrl = !empty($data['marking_scheme_url']) ? trim($data['marking_scheme_url']) : null;
    if (isset($_FILES['scheme_file']) && $_FILES['scheme_file']['error'] === UPLOAD_ERR_OK) {
        $cleanSchemeName = time() . '_scheme_' . preg_replace('/[^a-zA-Z0-9_\.-]/', '_', basename($_FILES['scheme_file']['name']));
        $targetSchemeFile = $uploadDir . $cleanSchemeName;
        if (move_uploaded_file($_FILES['scheme_file']['tmp_name'], $targetSchemeFile)) {
            $schemeUrl = $baseUrl . '/uploads/' . $cleanSchemeName;
        }
    }

    // Validate required fields
    $title = trim($data['title'] ?? '');
    if (empty($title)) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Paper title is required.'
        ]);
        exit();
    }

    $grade = trim($data['grade'] ?? 'Grade 13');
    $category = trim($data['category'] ?? 'A/L');
    $stream = !empty($data['stream']) ? trim($data['stream']) : null;
    $subject = trim($data['subject'] ?? 'General');
    $year = isset($data['year']) && is_numeric($data['year']) ? (int)$data['year'] : (int)date('Y');
    $examType = trim($data['exam_type'] ?? 'G.C.E. Examination');
    $medium = trim($data['medium'] ?? 'English');
    $description = trim($data['description'] ?? '');
    $fileSize = trim($data['file_size'] ?? '3.5 MB');
    $pages = isset($data['pages']) && is_numeric($data['pages']) ? (int)$data['pages'] : 16;
    $status = trim($data['status'] ?? 'published');

    // Auto-detect file size if local upload was made or remote URL passed
    if (!empty($targetFile) && file_exists($targetFile)) {
        $bytes = filesize($targetFile);
        if ($bytes >= 1048576) {
            $fileSize = round($bytes / 1048576, 2) . ' MB';
        } elseif ($bytes >= 1024) {
            $fileSize = round($bytes / 1024, 1) . ' KB';
        }
    } elseif (!empty($pdfUrl) && filter_var($pdfUrl, FILTER_VALIDATE_URL) && (empty($fileSize) || $fileSize === '3.5 MB')) {
        $ch = curl_init($pdfUrl);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HEADER, true);
        curl_setopt($ch, CURLOPT_NOBODY, true);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 3);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_exec($ch);
        $remoteBytes = curl_getinfo($ch, CURLINFO_CONTENT_LENGTH_DOWNLOAD);
        curl_close($ch);
        if ($remoteBytes > 0) {
            if ($remoteBytes >= 1048576) {
                $fileSize = round($remoteBytes / 1048576, 2) . ' MB';
            } elseif ($remoteBytes >= 1024) {
                $fileSize = round($remoteBytes / 1024, 1) . ' KB';
            }
        }
    }

    $sql = "INSERT INTO `past_papers` 
            (`grade`, `category`, `subject`, `stream`, `year`, `exam_type`, `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`, `file_size`, `pages`, `status`)
            VALUES 
            (:grade, :category, :subject, :stream, :year, :exam_type, :medium, :title, :pdf_url, :marking_scheme_url, :description, :file_size, :pages, :status)";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':grade'              => $grade,
        ':category'           => $category,
        ':subject'            => $subject,
        ':stream'             => $stream,
        ':year'               => $year,
        ':exam_type'          => $examType,
        ':medium'             => $medium,
        ':title'              => $title,
        ':pdf_url'            => $pdfUrl,
        ':marking_scheme_url' => $schemeUrl,
        ':description'        => $description,
        ':file_size'          => $fileSize,
        ':pages'              => $pages,
        ':status'             => $status
    ]);

    $insertedId = $pdo->lastInsertId();

    echo json_encode([
        'status'   => 'success',
        'message'  => 'Paper uploaded and recorded successfully.',
        'paper_id' => (string)$insertedId,
        'pdf_url'  => $pdfUrl
    ], JSON_UNESCAPED_SLASHES);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status'  => 'error',
        'message' => 'Upload failed: ' . $e->getMessage()
    ]);
}
