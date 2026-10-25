<?php
/**
 * DZD Learn - Past Papers Management Web Admin Panel
 * 
 * Upload PDF, add/edit/delete papers, select grade, category, subject, year, medium,
 * add marking scheme, and publish/unpublish papers directly to MySQL on InfinityFree.
 */

require_once __DIR__ . '/../config.php';

$pdo = getDbConnection();
$message = '';
$messageType = '';

// Ensure uploads folder exists
$uploadDir = __DIR__ . '/../uploads/';
if (!file_exists($uploadDir)) {
    @mkdir($uploadDir, 0755, true);
}

// Handle Form Submissions
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    // Handle Upload PDF helper
    $uploadedPdfUrl = '';
    if (isset($_FILES['pdf_file']) && $_FILES['pdf_file']['error'] === UPLOAD_ERR_OK) {
        $fileName = time() . '_' . preg_replace('/[^a-zA-Z0-9_\.-]/', '_', basename($_FILES['pdf_file']['name']));
        $targetPath = $uploadDir . $fileName;
        if (move_uploaded_file($_FILES['pdf_file']['tmp_name'], $targetPath)) {
            $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
            $host = $_SERVER['HTTP_HOST'];
            $scriptDir = dirname(dirname($_SERVER['SCRIPT_NAME']));
            $uploadedPdfUrl = rtrim($protocol . "://" . $host . $scriptDir, '/') . '/uploads/' . $fileName;
        }
    }

    $uploadedSchemeUrl = '';
    if (isset($_FILES['scheme_file']) && $_FILES['scheme_file']['error'] === UPLOAD_ERR_OK) {
        $schemeFileName = time() . '_scheme_' . preg_replace('/[^a-zA-Z0-9_\.-]/', '_', basename($_FILES['scheme_file']['name']));
        $targetSchemePath = $uploadDir . $schemeFileName;
        if (move_uploaded_file($_FILES['scheme_file']['tmp_name'], $targetSchemePath)) {
            $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
            $host = $_SERVER['HTTP_HOST'];
            $scriptDir = dirname(dirname($_SERVER['SCRIPT_NAME']));
            $uploadedSchemeUrl = rtrim($protocol . "://" . $host . $scriptDir, '/') . '/uploads/' . $schemeFileName;
        }
    }

    if ($action === 'add') {
        $grade = trim($_POST['grade'] ?? '');
        $category = trim($_POST['category'] ?? '');
        $stream = trim($_POST['stream'] ?? '');
        $subject = trim($_POST['subject'] ?? '');
        $year = (int)($_POST['year'] ?? date('Y'));
        $examType = trim($_POST['exam_type'] ?? 'G.C.E. Examination');
        $medium = trim($_POST['medium'] ?? 'English');
        $title = trim($_POST['title'] ?? '');
        $pdfUrl = !empty($uploadedPdfUrl) ? $uploadedPdfUrl : trim($_POST['pdf_url'] ?? '');
        $schemeUrl = !empty($uploadedSchemeUrl) ? $uploadedSchemeUrl : trim($_POST['marking_scheme_url'] ?? '');
        $description = trim($_POST['description'] ?? '');
        $status = trim($_POST['status'] ?? 'published');

        if (!empty($title) && !empty($pdfUrl) && !empty($subject)) {
            $stmt = $pdo->prepare("INSERT INTO `past_papers` 
                (`grade`, `category`, `stream`, `subject`, `year`, `exam_type`, `medium`, `title`, `pdf_url`, `marking_scheme_url`, `description`, `status`)
                VALUES (:grade, :category, :stream, :subject, :year, :exam_type, :medium, :title, :pdf_url, :marking_scheme_url, :description, :status)");
            $stmt->execute([
                ':grade' => $grade,
                ':category' => $category,
                ':stream' => !empty($stream) ? $stream : null,
                ':subject' => $subject,
                ':year' => $year,
                ':exam_type' => $examType,
                ':medium' => $medium,
                ':title' => $title,
                ':pdf_url' => $pdfUrl,
                ':marking_scheme_url' => !empty($schemeUrl) ? $schemeUrl : null,
                ':description' => $description,
                ':status' => $status
            ]);
            $message = "Paper added successfully to MySQL database!";
            $messageType = "success";
        } else {
            $message = "Please fill in all required fields (Title, Subject, PDF URL/File).";
            $messageType = "danger";
        }
    } elseif ($action === 'edit') {
        $id = (int)($_POST['id'] ?? 0);
        $grade = trim($_POST['grade'] ?? '');
        $category = trim($_POST['category'] ?? '');
        $stream = trim($_POST['stream'] ?? '');
        $subject = trim($_POST['subject'] ?? '');
        $year = (int)($_POST['year'] ?? date('Y'));
        $examType = trim($_POST['exam_type'] ?? 'G.C.E. Examination');
        $medium = trim($_POST['medium'] ?? 'English');
        $title = trim($_POST['title'] ?? '');
        $pdfUrl = !empty($uploadedPdfUrl) ? $uploadedPdfUrl : trim($_POST['pdf_url'] ?? '');
        $schemeUrl = !empty($uploadedSchemeUrl) ? $uploadedSchemeUrl : trim($_POST['marking_scheme_url'] ?? '');
        $description = trim($_POST['description'] ?? '');
        $status = trim($_POST['status'] ?? 'published');

        if ($id > 0 && !empty($title) && !empty($pdfUrl)) {
            $stmt = $pdo->prepare("UPDATE `past_papers` SET 
                `grade` = :grade, `category` = :category, `stream` = :stream, `subject` = :subject, `year` = :year, 
                `exam_type` = :exam_type, `medium` = :medium, `title` = :title, `pdf_url` = :pdf_url, 
                `marking_scheme_url` = :marking_scheme_url, `description` = :description, `status` = :status 
                WHERE `id` = :id");
            $stmt->execute([
                ':grade' => $grade,
                ':category' => $category,
                ':stream' => !empty($stream) ? $stream : null,
                ':subject' => $subject,
                ':year' => $year,
                ':exam_type' => $examType,
                ':medium' => $medium,
                ':title' => $title,
                ':pdf_url' => $pdfUrl,
                ':marking_scheme_url' => !empty($schemeUrl) ? $schemeUrl : null,
                ':description' => $description,
                ':status' => $status,
                ':id' => $id
            ]);
            $message = "Paper updated successfully!";
            $messageType = "success";
        }
    } elseif ($action === 'delete') {
        $id = (int)($_POST['id'] ?? 0);
        if ($id > 0) {
            $stmt = $pdo->prepare("DELETE FROM `past_papers` WHERE `id` = :id");
            $stmt->execute([':id' => $id]);
            $message = "Paper deleted successfully.";
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

// Fetch all papers
$searchFilter = isset($_GET['search']) ? trim($_GET['search']) : '';
$categoryFilter = isset($_GET['filter_category']) ? trim($_GET['filter_category']) : '';

$query = "SELECT * FROM `past_papers` WHERE 1=1";
$params = [];
if (!empty($searchFilter)) {
    $query .= " AND (`title` LIKE :s OR `subject` LIKE :s OR `grade` LIKE :s OR `year` LIKE :s)";
    $params[':s'] = "%$searchFilter%";
}
if (!empty($categoryFilter)) {
    $query .= " AND `category` = :cat";
    $params[':cat'] = $categoryFilter;
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
    <title>DZD Learn - Past Papers Admin Panel</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
    <style>
        :root {
            --primary: #059669;
            --primary-dark: #047857;
            --teal: #0F766E;
            --bg-light: #F8FAFC;
        }
        body {
            background-color: var(--bg-light);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            color: #0F172A;
        }
        .navbar-custom {
            background: linear-gradient(135deg, #059669 0%, #0F766E 100%);
            box-shadow: 0 4px 12px rgba(5, 150, 105, 0.15);
        }
        .btn-dzd {
            background-color: var(--primary);
            color: white;
            border: none;
            border-radius: 8px;
            font-weight: 600;
        }
        .btn-dzd:hover {
            background-color: var(--primary-dark);
            color: white;
        }
        .badge-published {
            background-color: #D1FAE5;
            color: #064E3B;
            font-weight: 600;
        }
        .badge-draft {
            background-color: #FEE2E2;
            color: #991B1B;
            font-weight: 600;
        }
        .card-table {
            border-radius: 16px;
            border: 1px solid #E2E8F0;
            overflow: hidden;
            box-shadow: 0 4px 16px rgba(0,0,0,0.03);
        }
    </style>
</head>
<body>

<nav class="navbar navbar-dark navbar-custom py-3 mb-4">
    <div class="container-fluid px-4">
        <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="index.php">
            <i class="bi bi-file-earmark-pdf-fill fs-3"></i>
            <span>DZD Learn - Past Papers Admin</span>
        </a>
        <div class="d-flex align-items-center gap-3">
            <a href="../api/get_papers.php" target="_blank" class="btn btn-sm btn-outline-light">
                <i class="bi bi-code-slash me-1"></i> View JSON API
            </a>
            <button class="btn btn-light btn-sm fw-bold px-3" data-bs-toggle="modal" data-bs-target="#addPaperModal">
                <i class="bi bi-plus-lg me-1"></i> Add Paper
            </button>
        </div>
    </div>
</nav>

<div class="container-fluid px-4 pb-5">
    <?php if (!empty($message)): ?>
        <div class="alert alert-<?= $messageType ?> alert-dismissible fade show" role="alert">
            <i class="bi bi-info-circle me-2"></i> <?= htmlspecialchars($message) ?>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    <?php endif; ?>

    <!-- Filter Bar -->
    <div class="row g-3 mb-4">
        <div class="col-md-5">
            <form method="GET" class="d-flex gap-2">
                <input type="text" name="search" class="form-control" placeholder="Search by title, subject, year, grade..." value="<?= htmlspecialchars($searchFilter) ?>">
                <button type="submit" class="btn btn-dzd px-3"><i class="bi bi-search"></i></button>
                <?php if (!empty($searchFilter) || !empty($categoryFilter)): ?>
                    <a href="index.php" class="btn btn-outline-secondary">Reset</a>
                <?php endif; ?>
            </form>
        </div>
        <div class="col-md-7 text-md-end">
            <div class="btn-group">
                <a href="index.php" class="btn btn-sm <?= empty($categoryFilter) ? 'btn-dzd' : 'btn-outline-secondary' ?>">All (<?= count($papers) ?>)</a>
                <?php 
                $categoriesList = ["Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "O/L", "A/L"];
                foreach ($categoriesList as $cat): 
                ?>
                    <a href="index.php?filter_category=<?= urlencode($cat) ?>" class="btn btn-sm <?= ($categoryFilter === $cat) ? 'btn-dzd' : 'btn-outline-secondary' ?>"><?= $cat ?></a>
                <?php endforeach; ?>
            </div>
        </div>
    </div>

    <!-- Papers Table -->
    <div class="card card-table bg-white">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th style="width: 50px;">ID</th>
                        <th>Year</th>
                        <th>Grade & Category</th>
                        <th>Subject</th>
                        <th>Title</th>
                        <th>Medium</th>
                        <th>PDF & Scheme</th>
                        <th>Status</th>
                        <th class="text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($papers)): ?>
                        <tr>
                            <td colspan="9" class="text-center py-5 text-muted">
                                <i class="bi bi-folder-x fs-1 d-block mb-2"></i>
                                No past papers found. Click <strong>Add Paper</strong> above to insert your first paper!
                            </td>
                        </tr>
                    <?php else: ?>
                        <?php foreach ($papers as $p): ?>
                            <tr>
                                <td class="fw-bold text-muted">#<?= $p['id'] ?></td>
                                <td><span class="badge bg-secondary"><?= htmlspecialchars($p['year']) ?></span></td>
                                <td>
                                    <span class="badge bg-primary-subtle text-primary fw-bold"><?= htmlspecialchars($p['category']) ?></span>
                                    <small class="text-muted d-block"><?= htmlspecialchars($p['grade']) ?></small>
                                </td>
                                <td><strong><?= htmlspecialchars($p['subject']) ?></strong></td>
                                <td>
                                    <div class="fw-semibold"><?= htmlspecialchars($p['title']) ?></div>
                                    <small class="text-muted"><?= htmlspecialchars(substr($p['description'] ?? '', 0, 60)) ?>...</small>
                                </td>
                                <td><span class="badge bg-info-subtle text-info-emphasis"><?= htmlspecialchars($p['medium']) ?></span></td>
                                <td>
                                    <div class="d-flex gap-2">
                                        <a href="<?= htmlspecialchars($p['pdf_url']) ?>" target="_blank" class="btn btn-sm btn-outline-danger" title="View PDF">
                                            <i class="bi bi-file-earmark-pdf"></i> PDF
                                        </a>
                                        <?php if (!empty($p['marking_scheme_url'])): ?>
                                            <a href="<?= htmlspecialchars($p['marking_scheme_url']) ?>" target="_blank" class="btn btn-sm btn-outline-success" title="View Marking Scheme">
                                                <i class="bi bi-check-circle"></i> Scheme
                                            </a>
                                        <?php endif; ?>
                                    </div>
                                </td>
                                <td>
                                    <form method="POST" class="d-inline">
                                        <input type="hidden" name="action" value="toggle_status">
                                        <input type="hidden" name="id" value="<?= $p['id'] ?>">
                                        <input type="hidden" name="current_status" value="<?= $p['status'] ?>">
                                        <button type="submit" class="btn btn-sm border-0 <?= ($p['status'] === 'published') ? 'badge-published' : 'badge-draft' ?>">
                                            <?= ucfirst($p['status']) ?>
                                        </button>
                                    </form>
                                </td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-outline-primary me-1" 
                                            onclick="openEditModal(<?= htmlspecialchars(json_encode($p), ENT_QUOTES, 'UTF-8') ?>)">
                                        <i class="bi bi-pencil"></i>
                                    </button>
                                    <form method="POST" class="d-inline" onsubmit="return confirm('Delete paper #<?= $p['id'] ?>?');">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="<?= $p['id'] ?>">
                                        <button type="submit" class="btn btn-sm btn-outline-danger">
                                            <i class="bi bi-trash"></i>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal: Add Paper -->
<div class="modal fade" id="addPaperModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <form method="POST" enctype="multipart/form-data" class="modal-content">
            <input type="hidden" name="action" value="add">
            <div class="modal-header">
                <h5 class="modal-title fw-bold"><i class="bi bi-plus-circle-fill text-success me-2"></i> Add New Past Paper</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body row g-3">
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Category *</label>
                    <select name="category" class="form-select" required id="modalCategory" onchange="updateGradesAndSubjects(this.value, 'modalGrade', 'modalSubject')">
                        <option value="A/L">Grade 12 / 13 – A/L</option>
                        <option value="O/L">Grade 11 – O/L</option>
                        <option value="Grade 10">Grade 10</option>
                        <option value="Grade 9">Grade 9</option>
                        <option value="Grade 8">Grade 8</option>
                        <option value="Grade 7">Grade 7</option>
                        <option value="Grade 6">Grade 6</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Grade *</label>
                    <select name="grade" class="form-select" id="modalGrade" required>
                        <option value="Grade 13">Grade 13</option>
                        <option value="Grade 12">Grade 12</option>
                        <option value="Grade 11">Grade 11</option>
                        <option value="Grade 10">Grade 10</option>
                        <option value="Grade 9">Grade 9</option>
                        <option value="Grade 8">Grade 8</option>
                        <option value="Grade 7">Grade 7</option>
                        <option value="Grade 6">Grade 6</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">A/L Stream (If Grade 12 or 13)</label>
                    <select name="stream" class="form-select" id="modalStream">
                        <option value="">-- None / N/A --</option>
                        <option value="Physical Science">Physical Science</option>
                        <option value="Biological Science">Biological Science</option>
                        <option value="Commerce">Commerce</option>
                        <option value="Arts">Arts</option>
                        <option value="Technology">Technology</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Subject *</label>
                    <input type="text" name="subject" class="form-control" list="subjectsList" placeholder="e.g. Combined Mathematics" required id="modalSubject">
                    <datalist id="subjectsList">
                        <option value="Combined Mathematics">
                        <option value="Physics">
                        <option value="Chemistry">
                        <option value="Biology">
                        <option value="ICT">
                        <option value="Accounting">
                        <option value="Economics">
                        <option value="Business Studies">
                        <option value="Mathematics">
                        <option value="Science">
                        <option value="English">
                        <option value="Sinhala">
                        <option value="History">
                    </datalist>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-semibold">Year *</label>
                    <input type="number" name="year" class="form-control" value="2024" min="2000" max="2030" required>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-semibold">Medium *</label>
                    <select name="medium" class="form-select" required>
                        <option value="English">English</option>
                        <option value="Sinhala">Sinhala</option>
                        <option value="Tamil">Tamil</option>
                    </select>
                </div>
                <div class="col-md-8">
                    <label class="form-label fw-semibold">Paper Title *</label>
                    <input type="text" name="title" class="form-control" placeholder="e.g. Combined Mathematics 2024 (English Medium)" required>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Exam Type</label>
                    <input type="text" name="exam_type" class="form-control" value="G.C.E. Advanced Level">
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">PDF URL *</label>
                    <input type="url" name="pdf_url" class="form-control" placeholder="https://.../paper.pdf">
                    <small class="text-muted">Or upload a PDF file below:</small>
                    <input type="file" name="pdf_file" accept=".pdf" class="form-control mt-1">
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Marking Scheme URL</label>
                    <input type="url" name="marking_scheme_url" class="form-control" placeholder="https://.../scheme.pdf">
                    <small class="text-muted">Or upload Scheme PDF below:</small>
                    <input type="file" name="scheme_file" accept=".pdf" class="form-control mt-1">
                </div>
                <div class="col-12">
                    <label class="form-label fw-semibold">Description / Syllabus Summary</label>
                    <textarea name="description" class="form-control" rows="2" placeholder="Brief outline of syllabus covered in paper..."></textarea>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Publication Status</label>
                    <select name="status" class="form-select">
                        <option value="published">Published (Visible in App)</option>
                        <option value="draft">Draft (Hidden)</option>
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-dzd px-4">Save Paper to MySQL</button>
            </div>
        </form>
    </div>
</div>

<!-- Modal: Edit Paper -->
<div class="modal fade" id="editPaperModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <form method="POST" enctype="multipart/form-data" class="modal-content" id="editPaperForm">
            <input type="hidden" name="action" value="edit">
            <input type="hidden" name="id" id="editId">
            <div class="modal-header">
                <h5 class="modal-title fw-bold"><i class="bi bi-pencil-square text-primary me-2"></i> Edit Past Paper</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body row g-3">
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Category *</label>
                    <select name="category" id="editCategory" class="form-select" required>
                        <option value="A/L">A/L</option>
                        <option value="O/L">O/L</option>
                        <option value="Grade 10">Grade 10</option>
                        <option value="Grade 9">Grade 9</option>
                        <option value="Grade 8">Grade 8</option>
                        <option value="Grade 7">Grade 7</option>
                        <option value="Grade 6">Grade 6</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Grade *</label>
                    <input type="text" name="grade" id="editGrade" class="form-control" required>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Subject *</label>
                    <input type="text" name="subject" id="editSubject" class="form-control" required>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-semibold">Year *</label>
                    <input type="number" name="year" id="editYear" class="form-control" required>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-semibold">Medium *</label>
                    <select name="medium" id="editMedium" class="form-select" required>
                        <option value="English">English</option>
                        <option value="Sinhala">Sinhala</option>
                        <option value="Tamil">Tamil</option>
                    </select>
                </div>
                <div class="col-md-8">
                    <label class="form-label fw-semibold">Paper Title *</label>
                    <input type="text" name="title" id="editTitle" class="form-control" required>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Exam Type</label>
                    <input type="text" name="exam_type" id="editExamType" class="form-control">
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">PDF URL *</label>
                    <input type="url" name="pdf_url" id="editPdfUrl" class="form-control" required>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Marking Scheme URL</label>
                    <input type="url" name="marking_scheme_url" id="editSchemeUrl" class="form-control">
                </div>
                <div class="col-12">
                    <label class="form-label fw-semibold">Description</label>
                    <textarea name="description" id="editDescription" class="form-control" rows="2"></textarea>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Status</label>
                    <select name="status" id="editStatus" class="form-select">
                        <option value="published">Published</option>
                        <option value="draft">Draft</option>
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-primary px-4">Update Paper</button>
            </div>
        </form>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
function openEditModal(paper) {
    document.getElementById('editId').value = paper.id;
    document.getElementById('editCategory').value = paper.category;
    document.getElementById('editGrade').value = paper.grade;
    document.getElementById('editSubject').value = paper.subject;
    document.getElementById('editYear').value = paper.year;
    document.getElementById('editMedium').value = paper.medium;
    document.getElementById('editTitle').value = paper.title;
    document.getElementById('editExamType').value = paper.exam_type;
    document.getElementById('editPdfUrl').value = paper.pdf_url;
    document.getElementById('editSchemeUrl').value = paper.marking_scheme_url || '';
    document.getElementById('editDescription').value = paper.description || '';
    document.getElementById('editStatus').value = paper.status;
    
    new bootstrap.Modal(document.getElementById('editPaperModal')).show();
}

function updateGradesAndSubjects(cat, gradeSelectId, subjectInputId) {
    const gradeSelect = document.getElementById(gradeSelectId);
    gradeSelect.innerHTML = '';
    if (cat === 'A/L') {
        gradeSelect.innerHTML = '<option value="Grade 13">Grade 13</option><option value="Grade 12">Grade 12</option>';
    } else if (cat === 'O/L') {
        gradeSelect.innerHTML = '<option value="Grade 11">Grade 11</option>';
    } else {
        gradeSelect.innerHTML = `<option value="${cat}">${cat}</option>`;
    }
}
</script>
</body>
</html>
