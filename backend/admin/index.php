<?php
/**
 * TheStudent - Modern Sri Lankan Past Papers & Notes Admin Dashboard
 * 
 * Specifically customized for Sri Lankan school curriculum (A/L, O/L, Scholarship, Term Tests)
 * Auto-detects real file size, page count, and handles PDF/Marking Scheme uploads to DirectAdmin.
 */

require_once __DIR__ . '/_guard.php';

$pdo = getDbConnection();

// Authentication gate: the console is administrators-only and every POST is
// CSRF protected. This MUST stay immediately after the DB connection.
$adminUser = ts_require_admin($pdo);

$message = '';
$messageType = '';

// Ensure uploads folder exists and is writable
$uploadDir = __DIR__ . '/../uploads/';
if (!file_exists($uploadDir)) {
    @mkdir($uploadDir, 0755, true);
}

/**
 * Helper to format bytes to human readable string (KB / MB)
 */
function formatBytes($bytes) {
    if ($bytes <= 0) return '0 KB';
    if ($bytes >= 1048576) {
        return round($bytes / 1048576, 2) . ' MB';
    }
    return round($bytes / 1024, 1) . ' KB';
}

/**
 * Helper to detect remote file size via HTTP HEAD request
 */
function getRemoteFileSize($url) {
    if (!filter_var($url, FILTER_VALIDATE_URL)) return null;
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HEADER, true);
    curl_setopt($ch, CURLOPT_NOBODY, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 4);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 2);
    curl_exec($ch);
    $bytes = curl_getinfo($ch, CURLINFO_CONTENT_LENGTH_DOWNLOAD);
    curl_close($ch);
    return ($bytes > 0) ? formatBytes($bytes) : null;
}

/**
 * Helper to estimate PDF page count from binary data
 */
function estimatePdfPages($filePath) {
    if (!file_exists($filePath)) return 16;
    $content = @file_get_contents($filePath, false, null, 0, 500000);
    if ($content && preg_match_all("/\/Type\s*\/Page[^s]/", $content, $matches)) {
        $count = count($matches[0]);
        if ($count > 0) return $count;
    }
    return 16;
}

// -------------------------------------------------------------
// Handle Form Submissions (Add, Edit, Delete, Toggle Status)
// -------------------------------------------------------------
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
    $host = $_SERVER['HTTP_HOST'];
    $baseUrl = $protocol . "://" . $host . rtrim(dirname(dirname($_SERVER['SCRIPT_NAME'])), '/');

    // 1. Handle PDF Upload
    $uploadedPdfUrl = '';
    $computedFileSize = '';
    $computedPages = 16;

    if (isset($_FILES['pdf_file']) && $_FILES['pdf_file']['error'] === UPLOAD_ERR_OK) {
        $rawBytes = (int) $_FILES['pdf_file']['size'];
        $uploadAllowed = true;
        $rejectReason = '';

        if ($rawBytes <= 0) {
            $uploadAllowed = false;
            $rejectReason = 'The uploaded PDF is empty.';
        } elseif ($rawBytes > 25 * 1024 * 1024) {
            $uploadAllowed = false;
            $rejectReason = 'PDF rejected: file is larger than the 25 MB limit.';
        } else {
            $finfo = function_exists('finfo_open') ? finfo_open(FILEINFO_MIME_TYPE) : null;
            $mime = $finfo ? (string) finfo_file($finfo, $_FILES['pdf_file']['tmp_name']) : '';
            if ($finfo) {
                finfo_close($finfo);
            }
            // Only genuine PDFs may be stored in the public uploads folder.
            if ($finfo && $mime !== 'application/pdf') {
                $uploadAllowed = false;
                $rejectReason = 'PDF rejected: the uploaded file is not a valid PDF document.';
            }
        }

        if (!$uploadAllowed) {
            $message = $rejectReason;
            $messageType = 'danger';
            $action = '';
        } else {
            $computedFileSize = formatBytes($rawBytes);
            $fileName = time() . '_' . preg_replace('/[^a-zA-Z0-9_\.-]/', '_', basename($_FILES['pdf_file']['name']));
            $targetPath = $uploadDir . $fileName;
            if (move_uploaded_file($_FILES['pdf_file']['tmp_name'], $targetPath)) {
                $uploadedPdfUrl = $baseUrl . '/uploads/' . $fileName;
                $computedPages = estimatePdfPages($targetPath);
            }
        }
    }

    // 2. Handle Marking Scheme Upload
    $uploadedSchemeUrl = '';
    if ($action !== '' && isset($_FILES['scheme_file']) && $_FILES['scheme_file']['error'] === UPLOAD_ERR_OK) {
        $schemeFileName = time() . '_scheme_' . preg_replace('/[^a-zA-Z0-9_\.-]/', '_', basename($_FILES['scheme_file']['name']));
        $targetSchemePath = $uploadDir . $schemeFileName;
        if (move_uploaded_file($_FILES['scheme_file']['tmp_name'], $targetSchemePath)) {
            $uploadedSchemeUrl = $baseUrl . '/uploads/' . $schemeFileName;
        }
    }

    // Process 'add'
    if ($action === 'add') {
        $title = trim($_POST['title'] ?? '');
        $subject = trim($_POST['subject'] ?? '');
        $category = trim($_POST['category'] ?? 'A/L');
        $grade = trim($_POST['grade'] ?? 'Grade 13');
        $stream = !empty($_POST['stream']) ? trim($_POST['stream']) : null;
        $year = (int)($_POST['year'] ?? date('Y'));
        $examType = trim($_POST['exam_type'] ?? 'G.C.E. Advanced Level');
        $medium = trim($_POST['medium'] ?? 'Sinhala');
        $pdfUrl = !empty($uploadedPdfUrl) ? $uploadedPdfUrl : trim($_POST['pdf_url'] ?? '');
        $schemeUrl = !empty($uploadedSchemeUrl) ? $uploadedSchemeUrl : trim($_POST['marking_scheme_url'] ?? '');
        $description = trim($_POST['description'] ?? '');
        $status = trim($_POST['status'] ?? 'published');
        $pages = !empty($_POST['pages']) ? (int)$_POST['pages'] : $computedPages;

        // Auto-detect file size if given a remote URL and no local upload
        if (empty($computedFileSize) && !empty($pdfUrl)) {
            $remoteSize = getRemoteFileSize($pdfUrl);
            $computedFileSize = $remoteSize ?: (!empty($_POST['file_size']) ? trim($_POST['file_size']) : '3.5 MB');
        } elseif (empty($computedFileSize)) {
            $computedFileSize = !empty($_POST['file_size']) ? trim($_POST['file_size']) : '3.5 MB';
        }

        if (!empty($title) && !empty($pdfUrl) && !empty($subject)) {
            $stmt = $pdo->prepare("INSERT INTO `past_papers` 
                (`grade`, `category`, `stream`, `subject`, `year`, `exam_type`, `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`, `file_size`, `pages`, `status`)
                VALUES (:grade, :category, :stream, :subject, :year, :exam_type, :medium, :title, :pdf_url, :marking_scheme_url, :description, :file_size, :pages, :status)");
            
            $stmt->execute([
                ':grade'              => $grade,
                ':category'           => $category,
                ':stream'             => $stream,
                ':subject'            => $subject,
                ':year'               => $year,
                ':exam_type'          => $examType,
                ':medium'             => $medium,
                ':title'              => $title,
                ':pdf_url'            => $pdfUrl,
                ':marking_scheme_url' => !empty($schemeUrl) ? $schemeUrl : null,
                ':description'        => $description,
                ':file_size'          => $computedFileSize,
                ':pages'              => $pages,
                ':status'             => $status
            ]);
            $message = "Paper '{$title}' uploaded successfully! File size: {$computedFileSize}.";
            $messageType = "success";
        } else {
            $message = "Please fill in all required fields (Title, Subject, PDF File/URL).";
            $messageType = "danger";
        }

    } elseif ($action === 'edit') {
        $id = (int)($_POST['id'] ?? 0);
        $title = trim($_POST['title'] ?? '');
        $subject = trim($_POST['subject'] ?? '');
        $category = trim($_POST['category'] ?? 'A/L');
        $grade = trim($_POST['grade'] ?? 'Grade 13');
        $stream = !empty($_POST['stream']) ? trim($_POST['stream']) : null;
        $year = (int)($_POST['year'] ?? date('Y'));
        $examType = trim($_POST['exam_type'] ?? 'G.C.E. Advanced Level');
        $medium = trim($_POST['medium'] ?? 'Sinhala');
        $pdfUrl = !empty($uploadedPdfUrl) ? $uploadedPdfUrl : trim($_POST['pdf_url'] ?? '');
        $schemeUrl = !empty($uploadedSchemeUrl) ? $uploadedSchemeUrl : trim($_POST['marking_scheme_url'] ?? '');
        $description = trim($_POST['description'] ?? '');
        $status = trim($_POST['status'] ?? 'published');
        $fileSize = !empty($computedFileSize) ? $computedFileSize : trim($_POST['file_size'] ?? '3.5 MB');
        $pages = (int)($_POST['pages'] ?? 16);

        if ($id > 0 && !empty($title) && !empty($pdfUrl)) {
            $stmt = $pdo->prepare("UPDATE `past_papers` SET 
                `grade` = :grade, `category` = :category, `stream` = :stream, `subject` = :subject, `year` = :year, 
                `exam_type` = :exam_type, `medium` = :medium, `title` = :title, `pdf_url` = :pdf_url, 
                `marking_scheme_url` = :marking_scheme_url, `description` = :description, `file_size` = :file_size, `pages` = :pages, `status` = :status 
                WHERE `id` = :id");
            $stmt->execute([
                ':grade'              => $grade,
                ':category'           => $category,
                ':stream'             => $stream,
                ':subject'            => $subject,
                ':year'               => $year,
                ':exam_type'          => $examType,
                ':medium'             => $medium,
                ':title'              => $title,
                ':pdf_url'            => $pdfUrl,
                ':marking_scheme_url' => !empty($schemeUrl) ? $schemeUrl : null,
                ':description'        => $description,
                ':file_size'          => $fileSize,
                ':pages'              => $pages,
                ':status'             => $status,
                ':id'                 => $id
            ]);
            $message = "Paper updated successfully!";
            $messageType = "success";
        }

    } elseif ($action === 'delete') {
        $id = (int)($_POST['id'] ?? 0);
        if ($id > 0) {
            $stmt = $pdo->prepare("DELETE FROM `past_papers` WHERE `id` = :id");
            $stmt->execute([':id' => $id]);
            $message = "Paper ID #{$id} deleted successfully.";
            $messageType = "warning";
        }

    } elseif ($action === 'toggle_status') {
        $id = (int)($_POST['id'] ?? 0);
        $current = trim($_POST['current_status'] ?? 'published');
        $newStatus = ($current === 'published') ? 'draft' : 'published';
        if ($id > 0) {
            $stmt = $pdo->prepare("UPDATE `past_papers` SET `status` = :status WHERE `id` = :id");
            $stmt->execute([':status' => $newStatus, ':id' => $id]);
            $message = "Paper status switched to " . strtoupper($newStatus);
            $messageType = "info";
        }
    }
}

// -------------------------------------------------------------
// Fetch Stats & Papers
// -------------------------------------------------------------
$totalPapersCount = $pdo->query("SELECT COUNT(*) FROM `past_papers`")->fetchColumn();
$alCount = $pdo->query("SELECT COUNT(*) FROM `past_papers` WHERE `category` = 'A/L' OR `grade` LIKE '%13%' OR `grade` LIKE '%12%'")->fetchColumn();
$olCount = $pdo->query("SELECT COUNT(*) FROM `past_papers` WHERE `category` = 'O/L' OR `grade` LIKE '%11%' OR `grade` LIKE '%10%'")->fetchColumn();
$draftCount = $pdo->query("SELECT COUNT(*) FROM `past_papers` WHERE `status` = 'draft'")->fetchColumn();

// Compute Uploads Folder Storage
$totalUploadBytes = 0;
if (file_exists($uploadDir)) {
    foreach (scandir($uploadDir) as $f) {
        if ($f !== '.' && $f !== '..' && is_file($uploadDir . $f)) {
            $totalUploadBytes += filesize($uploadDir . $f);
        }
    }
}
$storageFormatted = formatBytes($totalUploadBytes);

// Search & Filter
$searchFilter = isset($_GET['search']) ? trim($_GET['search']) : '';
$categoryFilter = isset($_GET['filter_category']) ? trim($_GET['filter_category']) : '';
$mediumFilter = isset($_GET['filter_medium']) ? trim($_GET['filter_medium']) : '';

$query = "SELECT * FROM `past_papers` WHERE 1=1";
$params = [];

if (!empty($searchFilter)) {
    // One distinct placeholder per occurrence - see the note in api/get_papers.php.
    $query .= " AND (`title` LIKE :s1 OR `subject` LIKE :s2 OR `stream` LIKE :s3 OR `grade` LIKE :s4 OR `year` LIKE :s5)";
    for ($i = 1; $i <= 5; $i++) {
        $params[':s' . $i] = "%$searchFilter%";
    }
}
if (!empty($categoryFilter)) {
    $query .= " AND `category` = :cat";
    $params[':cat'] = $categoryFilter;
}
if (!empty($mediumFilter)) {
    $query .= " AND `medium` = :med";
    $params[':med'] = $mediumFilter;
}

$query .= " ORDER BY `year` DESC, `id` DESC";
$stmt = $pdo->prepare($query);
$stmt->execute($params);
$papers = $stmt->fetchAll();
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TheStudent | Sri Lanka Past Papers & Notes Admin Panel</title>
    <!-- Modern Fonts & Frameworks -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css">
    <style>
        :root {
            --primary: #059669;
            --primary-hover: #047857;
            --primary-light: #ecfdf5;
            --primary-border: #a7f3d0;
            --dark-card: #0f172a;
            --bg-body: #f8fafc;
            --card-border: #e2e8f0;
            --text-main: #0f172a;
            --text-muted: #64748b;
        }

        body {
            background-color: var(--bg-body);
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            color: var(--text-main);
            letter-spacing: -0.01em;
        }

        /* Top Header */
        .navbar-custom {
            background: linear-gradient(135deg, #064e3b 0%, #065f46 50%, #047857 100%);
            box-shadow: 0 10px 25px -5px rgba(6, 78, 59, 0.25);
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }

        .brand-badge {
            background: rgba(255, 255, 255, 0.15);
            backdrop-filter: blur(10px);
            font-size: 11px;
            letter-spacing: 0.5px;
            border-radius: 999px;
            padding: 4px 10px;
        }

        /* KPI Stat Cards */
        .stat-card {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 18px;
            padding: 20px 22px;
            box-shadow: 0 4px 20px -4px rgba(0, 0, 0, 0.04);
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }
        .stat-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 30px -4px rgba(0, 0, 0, 0.08);
        }
        .stat-icon {
            width: 48px;
            height: 48px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
        }

        /* Modern Table Card */
        .main-card {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 20px;
            box-shadow: 0 6px 30px -6px rgba(0, 0, 0, 0.04);
            overflow: hidden;
        }

        .table > :not(caption) > * > * {
            padding: 14px 18px;
            vertical-align: middle;
        }
        .table thead th {
            background-color: #f8fafc;
            color: #475569;
            font-size: 12px;
            text-transform: uppercase;
            font-weight: 700;
            letter-spacing: 0.05em;
            border-bottom: 1px solid #e2e8f0;
        }

        /* Badges */
        .badge-sri-al {
            background: #dbeafe;
            color: #1e40af;
            font-weight: 600;
            border-radius: 6px;
            padding: 4px 8px;
        }
        .badge-sri-ol {
            background: #fef3c7;
            color: #92400e;
            font-weight: 600;
            border-radius: 6px;
            padding: 4px 8px;
        }
        .badge-sri-other {
            background: #f1f5f9;
            color: #475569;
            font-weight: 600;
            border-radius: 6px;
            padding: 4px 8px;
        }
        .badge-medium-si {
            background: #fce7f3;
            color: #9d174d;
            font-weight: 600;
            border-radius: 6px;
            padding: 4px 8px;
        }
        .badge-medium-en {
            background: #e0e7ff;
            color: #3730a3;
            font-weight: 600;
            border-radius: 6px;
            padding: 4px 8px;
        }
        .badge-medium-ta {
            background: #ffedd5;
            color: #9a3412;
            font-weight: 600;
            border-radius: 6px;
            padding: 4px 8px;
        }

        /* Modern Form Styling */
        .form-label {
            font-size: 13px;
            font-weight: 600;
            color: #334155;
            margin-bottom: 6px;
        }
        .form-control, .form-select {
            border: 1px solid #cbd5e1;
            border-radius: 10px;
            padding: 10px 14px;
            font-size: 14px;
            transition: all 0.15s ease-in-out;
        }
        .form-control:focus, .form-select:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(5, 150, 105, 0.18);
        }

        /* File Upload Dropzone */
        .dropzone-box {
            border: 2px dashed #cbd5e1;
            border-radius: 14px;
            background: #f8fafc;
            padding: 22px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .dropzone-box:hover {
            border-color: var(--primary);
            background: var(--primary-light);
        }
        .size-indicator {
            display: inline-block;
            background: #ecfdf5;
            color: #065f46;
            font-weight: 700;
            font-size: 12px;
            padding: 4px 12px;
            border-radius: 999px;
            border: 1px solid #a7f3d0;
            margin-top: 8px;
        }

        .btn-emerald {
            background-color: var(--primary);
            color: #ffffff;
            font-weight: 600;
            border-radius: 10px;
            padding: 10px 20px;
            border: none;
        }
        .btn-emerald:hover {
            background-color: var(--primary-hover);
            color: #ffffff;
        }
    </style>
</head>
<body>

<!-- Header Navigation -->
<nav class="navbar navbar-dark navbar-custom py-3 mb-4">
    <div class="container-fluid px-lg-5">
        <a class="navbar-brand d-flex align-items-center gap-3 fw-bold" href="index.php">
            <div class="bg-white rounded-3 p-2 d-flex align-items-center justify-content-center text-success shadow-sm" style="width: 40px; height: 40px;">
                <i class="bi bi-mortarboard-fill fs-4 text-emerald"></i>
            </div>
            <div>
                <div class="fs-5 lh-1 text-white">TheStudent</div>
                <div class="text-white-50 small" style="font-size: 11px;">Sri Lanka Past Papers Cloud Admin</div>
            </div>
            <span class="brand-badge text-white d-none d-md-inline-block">
                <i class="bi bi-hdd-network me-1"></i> DirectAdmin MySQL
            </span>
        </a>

        <div class="d-flex align-items-center gap-2">
            <span class="text-white-50 small d-none d-lg-inline"><?= htmlspecialchars((string) ($adminUser['email'] ?? '')) ?></span>
            <form method="POST" class="d-inline">
                <?= ts_csrf_field() ?>
                <input type="hidden" name="action" value="logout">
                <button type="submit" class="btn btn-sm btn-outline-light rounded-pill px-3">
                    <i class="bi bi-box-arrow-right me-1"></i> Sign out
                </button>
            </form>
            <a href="../api/get_papers.php" target="_blank" class="btn btn-sm btn-outline-light rounded-pill px-3">
                <i class="bi bi-braces me-1"></i> Live JSON API
            </a>
            <button class="btn btn-light btn-sm fw-bold rounded-pill px-3 shadow-sm" data-bs-toggle="modal" data-bs-target="#paperModal" onclick="openAddModal()">
                <i class="bi bi-cloud-arrow-up-fill text-success me-1"></i> Upload New Paper
            </button>
        </div>
    </div>
</nav>

<div class="container-fluid px-lg-5 pb-5">

    <!-- Flash Message -->
    <?php if (!empty($message)): ?>
        <div class="alert alert-<?= $messageType ?> alert-dismissible fade show rounded-4 shadow-sm mb-4" role="alert">
            <div class="d-flex align-items-center">
                <i class="bi bi-check-circle-fill fs-5 me-2"></i>
                <div class="fw-semibold"><?= htmlspecialchars($message) ?></div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    <?php endif; ?>

    <!-- KPI Metric Cards -->
    <div class="row g-3 mb-4">
        <div class="col-sm-6 col-xl-3">
            <div class="stat-card d-flex align-items-center justify-content-between">
                <div>
                    <div class="text-muted small fw-semibold text-uppercase">Total Catalog Papers</div>
                    <div class="fs-3 fw-bold mt-1"><?= number_format($totalPapersCount) ?></div>
                    <div class="text-success small fw-medium mt-1">
                        <i class="bi bi-check-circle-fill"></i> Synced to Android App
                    </div>
                </div>
                <div class="stat-icon bg-success-subtle text-success">
                    <i class="bi bi-journal-text"></i>
                </div>
            </div>
        </div>

        <div class="col-sm-6 col-xl-3">
            <div class="stat-card d-flex align-items-center justify-content-between">
                <div>
                    <div class="text-muted small fw-semibold text-uppercase">G.C.E. A/L Papers</div>
                    <div class="fs-3 fw-bold mt-1"><?= number_format($alCount) ?></div>
                    <div class="text-primary small fw-medium mt-1">
                        <i class="bi bi-mortarboard"></i> Science, Comm, Arts, Tech
                    </div>
                </div>
                <div class="stat-icon bg-primary-subtle text-primary">
                    <i class="bi bi-award-fill"></i>
                </div>
            </div>
        </div>

        <div class="col-sm-6 col-xl-3">
            <div class="stat-card d-flex align-items-center justify-content-between">
                <div>
                    <div class="text-muted small fw-semibold text-uppercase">O/L & Scholarship</div>
                    <div class="fs-3 fw-bold mt-1"><?= number_format($olCount) ?></div>
                    <div class="text-warning small fw-medium mt-1">
                        <i class="bi bi-book"></i> Grade 11 & Grade 5
                    </div>
                </div>
                <div class="stat-icon bg-warning-subtle text-warning">
                    <i class="bi bi-backpack4-fill"></i>
                </div>
            </div>
        </div>

        <div class="col-sm-6 col-xl-3">
            <div class="stat-card d-flex align-items-center justify-content-between">
                <div>
                    <div class="text-muted small fw-semibold text-uppercase">DirectAdmin Storage Used</div>
                    <div class="fs-3 fw-bold mt-1"><?= $storageFormatted ?></div>
                    <div class="text-secondary small fw-medium mt-1">
                        <i class="bi bi-infinity"></i> Unlimited Hosting Capacity
                    </div>
                </div>
                <div class="stat-icon bg-info-subtle text-info">
                    <i class="bi bi-hdd-fill"></i>
                </div>
            </div>
        </div>
    </div>

    <!-- Filter and Search Header -->
    <div class="main-card p-4 mb-4">
        <form method="GET" class="row g-3 align-items-center">
            <div class="col-md-5">
                <div class="input-group">
                    <span class="input-group-text bg-light border-end-0 text-muted"><i class="bi bi-search"></i></span>
                    <input type="text" name="search" class="form-control border-start-0" placeholder="Search by paper title, subject, stream, year..." value="<?= htmlspecialchars($searchFilter) ?>">
                </div>
            </div>

            <div class="col-md-3">
                <select name="filter_category" class="form-select" onchange="this.form.submit()">
                    <option value="">All Exam Categories (A/L, O/L, etc.)</option>
                    <option value="A/L" <?= ($categoryFilter === 'A/L') ? 'selected' : '' ?>>G.C.E. A/L (උසස් පෙළ)</option>
                    <option value="O/L" <?= ($categoryFilter === 'O/L') ? 'selected' : '' ?>>G.C.E. O/L (සාමාන්‍ය පෙළ)</option>
                    <option value="Scholarship" <?= ($categoryFilter === 'Scholarship') ? 'selected' : '' ?>>Grade 5 Scholarship (ශිෂ්‍යත්වය)</option>
                    <option value="Junior Secondary" <?= ($categoryFilter === 'Junior Secondary') ? 'selected' : '' ?>>Junior Secondary (Grade 6-9)</option>
                </select>
            </div>

            <div class="col-md-2">
                <select name="filter_medium" class="form-select" onchange="this.form.submit()">
                    <option value="">All Mediums</option>
                    <option value="Sinhala" <?= ($mediumFilter === 'Sinhala') ? 'selected' : '' ?>>සිංහල (Sinhala)</option>
                    <option value="English" <?= ($mediumFilter === 'English') ? 'selected' : '' ?>>English Medium</option>
                    <option value="Tamil" <?= ($mediumFilter === 'Tamil') ? 'selected' : '' ?>>தமிழ் (Tamil)</option>
                </select>
            </div>

            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-emerald flex-grow-1">Filter</button>
                <?php if (!empty($searchFilter) || !empty($categoryFilter) || !empty($mediumFilter)): ?>
                    <a href="index.php" class="btn btn-outline-secondary" title="Reset Filters"><i class="bi bi-x-lg"></i></a>
                <?php endif; ?>
            </div>
        </form>
    </div>

    <!-- Past Papers Table -->
    <div class="main-card">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th style="width: 50px;">#</th>
                        <th>Past Paper Title</th>
                        <th>Category / Grade</th>
                        <th>Stream / Subject</th>
                        <th>Medium</th>
                        <th>Year</th>
                        <th>Real File Size</th>
                        <th>Status</th>
                        <th class="text-end" style="width: 170px;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($papers)): ?>
                        <tr>
                            <td colspan="9" class="text-center py-5 text-muted">
                                <i class="bi bi-file-earmark-x fs-1 d-block mb-2 text-secondary"></i>
                                <div class="fw-semibold">No past papers found matching your filters.</div>
                                <div class="small">Click "Upload New Paper" above to add your first past paper.</div>
                            </td>
                        </tr>
                    <?php else: ?>
                        <?php foreach ($papers as $p): ?>
                            <tr>
                                <td class="text-muted fw-bold"><?= htmlspecialchars($p['id']) ?></td>
                                <td>
                                    <div class="fw-bold text-dark"><?= htmlspecialchars($p['title']) ?></div>
                                    <div class="small text-muted d-flex align-items-center gap-2 mt-1">
                                        <span><i class="bi bi-filetype-pdf text-danger"></i> <?= htmlspecialchars($p['pages'] ?? 16) ?> pages</span>
                                        <?php if (!empty($p['marking_scheme_url'])): ?>
                                            <span class="badge bg-success-subtle text-success border border-success-subtle">
                                                <i class="bi bi-check2-all"></i> Scheme Included
                                            </span>
                                        <?php endif; ?>
                                    </div>
                                </td>
                                <td>
                                    <?php 
                                        $catClass = ($p['category'] === 'A/L') ? 'badge-sri-al' : (($p['category'] === 'O/L') ? 'badge-sri-ol' : 'badge-sri-other');
                                    ?>
                                    <span class="<?= $catClass ?>"><?= htmlspecialchars($p['category']) ?></span>
                                    <div class="small text-muted mt-1"><?= htmlspecialchars($p['grade']) ?></div>
                                </td>
                                <td>
                                    <div class="fw-semibold"><?= htmlspecialchars($p['subject']) ?></div>
                                    <?php if (!empty($p['stream'])): ?>
                                        <div class="small text-muted"><?= htmlspecialchars($p['stream']) ?></div>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <?php 
                                        $med = $p['medium'] ?? 'English';
                                        $medClass = ($med === 'Sinhala') ? 'badge-medium-si' : (($med === 'English') ? 'badge-medium-en' : 'badge-medium-ta');
                                    ?>
                                    <span class="<?= $medClass ?>"><?= htmlspecialchars($med) ?></span>
                                </td>
                                <td>
                                    <span class="badge bg-light text-dark border"><?= htmlspecialchars($p['year']) ?></span>
                                </td>
                                <td>
                                    <span class="badge bg-secondary-subtle text-secondary fw-semibold">
                                        <i class="bi bi-floppy2"></i> <?= htmlspecialchars($p['file_size'] ?: '3.5 MB') ?>
                                    </span>
                                </td>
                                <td>
                                    <form method="POST" class="d-inline">
                                        <?= ts_csrf_field() ?>
                                        <input type="hidden" name="action" value="toggle_status">
                                        <input type="hidden" name="id" value="<?= $p['id'] ?>">
                                        <input type="hidden" name="current_status" value="<?= $p['status'] ?>">
                                        <?php if ($p['status'] === 'published'): ?>
                                            <button type="submit" class="badge bg-success-subtle text-success border border-success-subtle text-decoration-none btn p-1 px-2" title="Click to Unpublish">
                                                <i class="bi bi-check-circle-fill me-1"></i> Published
                                            </button>
                                        <?php else: ?>
                                            <button type="submit" class="badge bg-warning-subtle text-warning border border-warning-subtle text-decoration-none btn p-1 px-2" title="Click to Publish">
                                                <i class="bi bi-pause-circle me-1"></i> Draft
                                            </button>
                                        <?php endif; ?>
                                    </form>
                                </td>
                                <td class="text-end">
                                    <div class="btn-group btn-group-sm">
                                        <a href="<?= htmlspecialchars($p['pdf_url']) ?>" target="_blank" class="btn btn-outline-secondary" title="View PDF">
                                            <i class="bi bi-eye-fill"></i>
                                        </a>
                                        <?php if (!empty($p['marking_scheme_url'])): ?>
                                            <a href="<?= htmlspecialchars($p['marking_scheme_url']) ?>" target="_blank" class="btn btn-outline-success" title="View Marking Scheme">
                                                <i class="bi bi-file-earmark-check-fill"></i>
                                            </a>
                                        <?php endif; ?>
                                        <button class="btn btn-outline-primary" title="Edit Paper" onclick='openEditModal(<?= json_encode($p) ?>)'>
                                            <i class="bi bi-pencil-fill"></i>
                                        </button>
                                        <form method="POST" class="d-inline" onsubmit="return confirm('Are you sure you want to delete this paper?')">
                                        <?= ts_csrf_field() ?>
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="<?= $p['id'] ?>">
                                            <button type="submit" class="btn btn-outline-danger" title="Delete">
                                                <i class="bi bi-trash3-fill"></i>
                                            </button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- ==================================================================== -->
<!-- Dynamic Sri Lankan Past Paper Modal Form (Upload & Edit)             -->
<!-- ==================================================================== -->
<div class="modal fade" id="paperModal" tabindex="-1" aria-labelledby="paperModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content rounded-4 border-0 shadow-lg">
            <form method="POST" enctype="multipart/form-data" id="paperForm">
                <?= ts_csrf_field() ?>
                <input type="hidden" name="action" id="formAction" value="add">
                <input type="hidden" name="id" id="paperId" value="">

                <div class="modal-header border-bottom py-3 px-4 bg-light rounded-top-4">
                    <div class="d-flex align-items-center gap-2">
                        <div class="stat-icon bg-success-subtle text-success" style="width: 36px; height: 36px;">
                            <i class="bi bi-file-earmark-plus-fill"></i>
                        </div>
                        <h5 class="modal-title fw-bold" id="paperModalLabel">Upload Sri Lankan Past Paper</h5>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>

                <div class="modal-body p-4">
                    <div class="row g-3">

                        <!-- 1. Exam Level / Category -->
                        <div class="col-md-6">
                            <label class="form-label">Exam Level / Category <span class="text-danger">*</span></label>
                            <select name="category" id="modalCategory" class="form-select" onchange="handleCategoryChange()" required>
                                <option value="A/L">G.C.E. Advanced Level (A/L) - උසස් පෙළ</option>
                                <option value="O/L">G.C.E. Ordinary Level (O/L) - සාමාන්‍ය පෙළ</option>
                                <option value="Scholarship">Grade 5 Scholarship - ශිෂ්‍යත්වය</option>
                                <option value="Junior Secondary">Junior Secondary (Grade 6 - 9)</option>
                                <option value="Primary">Primary (Grade 1 - 5)</option>
                            </select>
                        </div>

                        <!-- 2. Target Grade -->
                        <div class="col-md-6">
                            <label class="form-label">Grade <span class="text-danger">*</span></label>
                            <select name="grade" id="modalGrade" class="form-select" required>
                                <option value="Grade 13">Grade 13</option>
                                <option value="Grade 12">Grade 12</option>
                                <option value="Grade 11">Grade 11</option>
                                <option value="Grade 10">Grade 10</option>
                                <option value="Grade 9">Grade 9</option>
                                <option value="Grade 8">Grade 8</option>
                                <option value="Grade 7">Grade 7</option>
                                <option value="Grade 6">Grade 6</option>
                                <option value="Grade 5">Grade 5</option>
                            </select>
                        </div>

                        <!-- 3. A/L Stream (Shows only for A/L) -->
                        <div class="col-md-6" id="streamGroup">
                            <label class="form-label">A/L Subject Stream (විෂය ධාරාව)</label>
                            <select name="stream" id="modalStream" class="form-select" onchange="handleStreamChange()">
                                <option value="Physical Science">Physical Science / Combined Maths (ගණිත අංශය)</option>
                                <option value="Biological Science">Biological Science (ජීව විද්‍යා අංශය)</option>
                                <option value="Commerce">Commerce (වාණිජ අංශය)</option>
                                <option value="Technology">Technology (තාක්ෂණවේදය - ET, BST, SFT)</option>
                                <option value="Arts">Arts (කලා අංශය)</option>
                                <option value="Common">Common General Examination (පොදු පරීක්ෂණය)</option>
                            </select>
                        </div>

                        <!-- 4. Subject Selection with Smart Presets -->
                        <div class="col-md-6">
                            <label class="form-label">Subject Name <span class="text-danger">*</span></label>
                            <div class="input-group">
                                <select id="subjectPresetSelect" class="form-select" onchange="handleSubjectPresetChange()">
                                    <!-- Populated dynamically by JavaScript -->
                                </select>
                                <input type="text" name="subject" id="modalSubject" class="form-control" placeholder="Or enter subject name" required>
                            </div>
                        </div>

                        <!-- 5. Examination Year & Medium -->
                        <div class="col-md-4">
                            <label class="form-label">Examination Year <span class="text-danger">*</span></label>
                            <select name="year" id="modalYear" class="form-select" required>
                                <?php for ($y = date('Y') + 1; $y >= 2010; $y--): ?>
                                    <option value="<?= $y ?>" <?= ($y == 2024) ? 'selected' : '' ?>><?= $y ?></option>
                                <?php endfor; ?>
                            </select>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label">Medium Language <span class="text-danger">*</span></label>
                            <select name="medium" id="modalMedium" class="form-select" required>
                                <option value="Sinhala">සිංහල (Sinhala)</option>
                                <option value="English">English Medium</option>
                                <option value="Tamil">தமிழ் (Tamil)</option>
                            </select>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label">Exam Type / Authority</label>
                            <select name="exam_type" id="modalExamType" class="form-select">
                                <option value="G.C.E. Advanced Level">G.C.E. National Exam (DoE)</option>
                                <option value="Provincial Evaluation">Provincial Term Test (පළාත් විභාග)</option>
                                <option value="School Examination">School Term Test (පාසල් වාර විභාග)</option>
                                <option value="Model Paper">Model / Pilot Examination (ආදර්ශ ප්‍රශ්න)</option>
                            </select>
                        </div>

                        <!-- 6. Paper Title with Auto-Generator Helper -->
                        <div class="col-12">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <label class="form-label mb-0">Paper Title <span class="text-danger">*</span></label>
                                <button type="button" class="btn btn-sm btn-light border text-primary py-0 px-2 fw-semibold" onclick="generateSmartTitle()" style="font-size: 11px;">
                                    <i class="bi bi-magic me-1"></i> Auto-Generate Title
                                </button>
                            </div>
                            <input type="text" name="title" id="modalTitle" class="form-control" placeholder="e.g. 2024 G.C.E. A/L Combined Mathematics Paper I & II (Sinhala Medium)" required>
                        </div>

                        <!-- 7. PDF Upload Dropzone with Real-Time File Size Auto Detection -->
                        <div class="col-12">
                            <label class="form-label">Past Paper PDF File <span class="text-danger">*</span></label>
                            <div class="dropzone-box" onclick="document.getElementById('pdfFileInput').click()">
                                <i class="bi bi-cloud-arrow-up text-success fs-1"></i>
                                <div class="fw-bold mt-2" id="pdfLabel">Click to browse or drop Past Paper PDF here</div>
                                <div class="text-muted small">Supports all PDF documents. Real file size is auto-detected and saved to MySQL.</div>
                                <div id="pdfSizeBadge" class="size-indicator d-none">
                                    <i class="bi bi-file-earmark-check"></i> <span id="pdfSizeText">0 MB</span>
                                </div>
                                <input type="file" name="pdf_file" id="pdfFileInput" class="d-none" accept="application/pdf" onchange="handleFileSelect(this, 'pdfLabel', 'pdfSizeBadge', 'pdfSizeText')">
                            </div>

                            <!-- Or paste URL -->
                            <div class="mt-2">
                                <div class="small text-muted fw-semibold mb-1">Or paste a Direct PDF Download URL:</div>
                                <input type="url" name="pdf_url" id="modalPdfUrl" class="form-control form-control-sm" placeholder="https://example.com/papers/2024-maths.pdf">
                            </div>
                        </div>

                        <!-- 8. Marking Scheme Section -->
                        <div class="col-12">
                            <div class="border rounded-3 p-3 bg-light">
                                <div class="form-check form-switch mb-2">
                                    <input class="form-check-input" type="checkbox" id="hasSchemeToggle" onchange="toggleSchemeInput()">
                                    <label class="form-check-label fw-bold" for="hasSchemeToggle">
                                        Attach Official Marking Scheme / Answers (ලකුණු දීමේ පටිපාටිය)
                                    </label>
                                </div>

                                <div id="schemeSection" class="d-none mt-3">
                                    <div class="dropzone-box py-3" onclick="document.getElementById('schemeFileInput').click()">
                                        <i class="bi bi-file-earmark-check-fill text-primary fs-3"></i>
                                        <div class="fw-bold mt-1" id="schemeLabel">Click to browse Marking Scheme PDF</div>
                                        <div id="schemeSizeBadge" class="size-indicator d-none">
                                            <i class="bi bi-check2"></i> <span id="schemeSizeText">0 MB</span>
                                        </div>
                                        <input type="file" name="scheme_file" id="schemeFileInput" class="d-none" accept="application/pdf" onchange="handleFileSelect(this, 'schemeLabel', 'schemeSizeBadge', 'schemeSizeText')">
                                    </div>
                                    <div class="mt-2">
                                        <input type="url" name="marking_scheme_url" id="modalSchemeUrl" class="form-control form-control-sm" placeholder="Or enter marking scheme URL">
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 9. Optional Meta (Pages & Description) -->
                        <div class="col-md-4">
                            <label class="form-label">Estimated Total Pages</label>
                            <input type="number" name="pages" id="modalPages" class="form-control" value="16">
                        </div>

                        <div class="col-md-4">
                            <label class="form-label">Status</label>
                            <select name="status" id="modalStatus" class="form-select">
                                <option value="published">Published (Visible in App)</option>
                                <option value="draft">Draft (Hidden)</option>
                            </select>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label">Real File Size</label>
                            <input type="text" name="file_size" id="modalFileSize" class="form-control bg-light" placeholder="Auto calculated on upload" readonly>
                        </div>

                        <div class="col-12">
                            <label class="form-label">Description / Syllabus Breakdown</label>
                            <textarea name="description" id="modalDescription" class="form-control" rows="2" placeholder="e.g. Pure Mathematics & Applied Mechanics, Paper I & II with full marking scheme."></textarea>
                        </div>

                    </div>
                </div>

                <div class="modal-footer border-top py-3 px-4 bg-light rounded-bottom-4">
                    <button type="button" class="btn btn-outline-secondary rounded-pill px-4" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-emerald rounded-pill px-5 shadow-sm" id="submitBtn">
                        <i class="bi bi-cloud-arrow-up-fill me-1"></i> Save Past Paper
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // Sri Lankan Curriculum Subject Dictionary
    const sriLankanSubjects = {
        "A/L": {
            "Physical Science": ["Combined Mathematics", "Physics", "Chemistry", "Information & Communication Technology (ICT)"],
            "Biological Science": ["Biology", "Chemistry", "Physics", "Agricultural Science"],
            "Commerce": ["Accounting", "Business Studies", "Economics", "Business Statistics", "Information & Communication Technology (ICT)"],
            "Technology": ["Science for Technology (SFT)", "Engineering Technology (ET)", "Bio Systems Technology (BST)", "Information & Communication Technology (ICT)"],
            "Arts": ["Sinhala", "Tamil", "English", "Buddhist Civilization", "Christian Civilization", "Political Science", "History", "Logic & Scientific Method", "Geography", "Communication & Media Studies"],
            "Common": ["Common General Examination (සාමාන්‍ය පොදු පරීක්ෂණය)", "General English (සාමාන්‍ය ඉංග්‍රීසි)"]
        },
        "O/L": ["Mathematics", "Science", "English", "Sinhala Language & Literature", "Tamil Language", "History", "Buddhism", "Catholicism / Christianity", "Islam", "Commerce & Accounting", "Information & Communication Technology (ICT)", "Health & Physical Education", "Geography", "Art", "Eastern Music", "Western Music", "Dancing", "Drama & Theatre"],
        "Scholarship": ["Scholarship Paper I (Mental Ability & General)", "Scholarship Paper II (Language & Environment)"],
        "Junior Secondary": ["Mathematics", "Science", "English", "Sinhala", "Tamil", "History", "Buddhism", "Geography", "Health & Physical Education", "Practical & Technical Studies (PTS)", "Civic Education"],
        "Primary": ["Sinhala / Tamil Language", "Mathematics", "English", "Environmental Studies (පරිසරය)"]
    };

    function updateSubjectPresets() {
        const cat = document.getElementById('modalCategory').value;
        const stream = document.getElementById('modalStream').value;
        const select = document.getElementById('subjectPresetSelect');
        select.innerHTML = '<option value="">-- Choose Subject Preset --</option>';

        let list = [];
        if (cat === "A/L") {
            list = (sriLankanSubjects["A/L"][stream]) || sriLankanSubjects["A/L"]["Physical Science"];
        } else {
            list = sriLankanSubjects[cat] || sriLankanSubjects["O/L"];
        }

        list.forEach(subj => {
            const opt = document.createElement('option');
            opt.value = subj;
            opt.textContent = subj;
            select.appendChild(opt);
        });
    }

    function handleSubjectPresetChange() {
        const select = document.getElementById('subjectPresetSelect');
        if (select.value) {
            document.getElementById('modalSubject').value = select.value;
        }
    }

    function handleCategoryChange() {
        const cat = document.getElementById('modalCategory').value;
        const streamGroup = document.getElementById('streamGroup');
        const gradeSelect = document.getElementById('modalGrade');

        if (cat === "A/L") {
            streamGroup.classList.remove('d-none');
            gradeSelect.value = "Grade 13";
        } else if (cat === "O/L") {
            streamGroup.classList.add('d-none');
            gradeSelect.value = "Grade 11";
        } else if (cat === "Scholarship") {
            streamGroup.classList.add('d-none');
            gradeSelect.value = "Grade 5";
        } else {
            streamGroup.classList.add('d-none');
            gradeSelect.value = "Grade 9";
        }
        updateSubjectPresets();
    }

    function handleStreamChange() {
        updateSubjectPresets();
    }

    function generateSmartTitle() {
        const year = document.getElementById('modalYear').value;
        const cat = document.getElementById('modalCategory').value;
        const subj = document.getElementById('modalSubject').value || 'Subject';
        const med = document.getElementById('modalMedium').value;

        let examPrefix = (cat === 'A/L') ? 'G.C.E. A/L' : ((cat === 'O/L') ? 'G.C.E. O/L' : cat);
        let medStr = (med === 'Sinhala') ? 'Sinhala Medium (සිංහල)' : ((med === 'Tamil') ? 'Tamil Medium (தமிழ்)' : 'English Medium');

        document.getElementById('modalTitle').value = `${year} ${examPrefix} ${subj} (${medStr})`;
    }

    // Real-Time File Size Auto Detection
    function handleFileSelect(input, labelId, badgeId, textId) {
        if (input.files && input.files[0]) {
            const file = input.files[0];
            const bytes = file.size;
            let formatted = '';
            if (bytes >= 1048576) {
                formatted = (bytes / 1048576).toFixed(2) + ' MB';
            } else {
                formatted = (bytes / 1024).toFixed(1) + ' KB';
            }

            document.getElementById(labelId).innerText = file.name;
            document.getElementById(badgeId).classList.remove('d-none');
            document.getElementById(textId).innerText = 'Detected Size: ' + formatted;

            if (labelId === 'pdfLabel') {
                document.getElementById('modalFileSize').value = formatted;
            }
        }
    }

    function toggleSchemeInput() {
        const toggle = document.getElementById('hasSchemeToggle');
        const section = document.getElementById('schemeSection');
        if (toggle.checked) {
            section.classList.remove('d-none');
        } else {
            section.classList.add('d-none');
        }
    }

    function openAddModal() {
        document.getElementById('paperForm').reset();
        document.getElementById('formAction').value = 'add';
        document.getElementById('paperId').value = '';
        document.getElementById('paperModalLabel').innerText = 'Upload Sri Lankan Past Paper';
        document.getElementById('pdfLabel').innerText = 'Click to browse or drop Past Paper PDF here';
        document.getElementById('pdfSizeBadge').classList.add('d-none');
        document.getElementById('schemeSizeBadge').classList.add('d-none');
        document.getElementById('hasSchemeToggle').checked = false;
        document.getElementById('schemeSection').classList.add('d-none');
        handleCategoryChange();
    }

    function openEditModal(paper) {
        openAddModal();
        document.getElementById('formAction').value = 'edit';
        document.getElementById('paperId').value = paper.id;
        document.getElementById('paperModalLabel').innerText = 'Edit Past Paper #' + paper.id;
        document.getElementById('modalCategory').value = paper.category || 'A/L';
        handleCategoryChange();

        document.getElementById('modalGrade').value = paper.grade || 'Grade 13';
        if (paper.stream) {
            document.getElementById('modalStream').value = paper.stream;
            handleStreamChange();
        }
        document.getElementById('modalSubject').value = paper.subject || '';
        document.getElementById('modalYear').value = paper.year || 2024;
        document.getElementById('modalMedium').value = paper.medium || 'Sinhala';
        document.getElementById('modalExamType').value = paper.exam_type || 'G.C.E. Advanced Level';
        document.getElementById('modalTitle').value = paper.title || '';
        document.getElementById('modalPdfUrl').value = paper.pdf_url || '';
        document.getElementById('modalSchemeUrl').value = paper.marking_scheme_url || '';
        document.getElementById('modalPages').value = paper.pages || 16;
        document.getElementById('modalStatus').value = paper.status || 'published';
        document.getElementById('modalFileSize').value = paper.file_size || '';
        document.getElementById('modalDescription').value = paper.description || '';

        if (paper.marking_scheme_url) {
            document.getElementById('hasSchemeToggle').checked = true;
            document.getElementById('schemeSection').classList.remove('d-none');
        }

        const modal = new bootstrap.Modal(document.getElementById('paperModal'));
        modal.show();
    }

    // Initialize on page load
    document.addEventListener('DOMContentLoaded', function() {
        handleCategoryChange();
    });
</script>
</body>
</html>
