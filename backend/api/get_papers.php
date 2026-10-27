<?php
/**
 * TheStudent - Past Papers Retrieval API Endpoint
 * 
 * URL: /api/get_papers.php
 * Methods: GET
 * 
 * Query Parameters:
 * - category (string, e.g. "Junior Secondary", "O/L", "A/L")
 * - grade    (string, e.g. "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "Grade 13")
 * - stream   (string, e.g. "Physical Science", "Biological Science", "Commerce", "Arts", "Technology")
 * - subject  (string, e.g. "Combined Mathematics", "Science")
 * - year     (int, e.g. 2024)
 * - medium   (string, e.g. "English", "Sinhala", "Tamil")
 * - search   (string, full-text search across title, subject, grade, stream, year, description)
 * - status   (string, default "published")
 */

require_once __DIR__ . '/../config.php';

header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = getDbConnection();

    // Extract query parameters
    $category = isset($_GET['category']) ? trim($_GET['category']) : null;
    $grade    = isset($_GET['grade'])    ? trim($_GET['grade'])    : null;
    $stream   = isset($_GET['stream'])   ? trim($_GET['stream'])   : null;
    $subject  = isset($_GET['subject'])  ? trim($_GET['subject'])  : null;
    $year     = isset($_GET['year']) && is_numeric($_GET['year']) ? (int)$_GET['year'] : null;
    $medium   = isset($_GET['medium'])   ? trim($_GET['medium'])   : null;
    $search   = isset($_GET['search'])   ? trim($_GET['search'])   : null;
    $status   = isset($_GET['status'])   ? trim($_GET['status'])   : 'published';

    // Build SQL query dynamically
    $conditions = [];
    $params     = [];

    if ($status !== 'all') {
        $conditions[] = "`status` = :status";
        $params[':status'] = $status;
    }

    if (!empty($category) && $category !== 'All' && $category !== 'All Categories') {
        // Support matching either direct category or grade prefix
        $conditions[] = "(`category` = :category OR `grade` LIKE :category_like)";
        $params[':category'] = $category;
        $params[':category_like'] = "%" . $category . "%";
    }

    if (!empty($grade) && $grade !== 'All' && $grade !== 'All Grades') {
        $conditions[] = "`grade` = :grade";
        $params[':grade'] = $grade;
    }

    if (!empty($stream) && $stream !== 'All' && $stream !== 'All Streams') {
        $conditions[] = "`stream` = :stream";
        $params[':stream'] = $stream;
    }

    if (!empty($subject) && $subject !== 'All' && $subject !== 'All Subjects') {
        $conditions[] = "`subject` = :subject";
        $params[':subject'] = $subject;
    }

    if (!empty($year)) {
        $conditions[] = "`year` = :year";
        $params[':year'] = $year;
    }

    if (!empty($medium) && $medium !== 'All' && $medium !== 'All Mediums') {
        $conditions[] = "`medium` = :medium";
        $params[':medium'] = $medium;
    }

    if (!empty($search)) {
        $conditions[] = "(`title` LIKE :search OR `subject` LIKE :search OR `grade` LIKE :search OR (`stream` IS NOT NULL AND `stream` LIKE :search) OR `year` LIKE :search OR `description` LIKE :search)";
        $params[':search'] = '%' . $search . '%';
    }

    $whereSql = !empty($conditions) ? 'WHERE ' . implode(' AND ', $conditions) : '';
    $sql = "SELECT id, grade, category, subject, stream, year, exam_type, medium, title, 
                   pdf_url, marking_scheme_url, description, file_size, pages, created_at, status 
            FROM `past_papers` 
            {$whereSql} 
            ORDER BY `year` DESC, `subject` ASC, `title` ASC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $papers = $stmt->fetchAll();

    // Fetch distinct metadata to assist client navigation
    $catStmt = $pdo->query("SELECT DISTINCT `category` FROM `past_papers` WHERE `status` = 'published' ORDER BY `category` ASC");
    $categories = $catStmt->fetchAll(PDO::FETCH_COLUMN);

    $streamStmt = $pdo->query("SELECT DISTINCT `stream` FROM `past_papers` WHERE `status` = 'published' AND `stream` IS NOT NULL AND `stream` != '' ORDER BY `stream` ASC");
    $allStreams = $streamStmt->fetchAll(PDO::FETCH_COLUMN);

    $subStmt = $pdo->query("SELECT DISTINCT `subject` FROM `past_papers` WHERE `status` = 'published' ORDER BY `subject` ASC");
    $allSubjects = $subStmt->fetchAll(PDO::FETCH_COLUMN);

    $yrStmt = $pdo->query("SELECT DISTINCT `year` FROM `past_papers` WHERE `status` = 'published' ORDER BY `year` DESC");
    $allYears = $yrStmt->fetchAll(PDO::FETCH_COLUMN);

    echo json_encode([
        'status'             => 'success',
        'count'              => count($papers),
        'filter_applied'     => [
            'category' => $category,
            'grade'    => $grade,
            'stream'   => $stream,
            'subject'  => $subject,
            'year'     => $year,
            'search'   => $search
        ],
        'available_categories' => !empty($categories) ? $categories : ["Junior Secondary", "O/L", "A/L", "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "Grade 13"],
        'available_streams'    => !empty($allStreams) ? $allStreams : ["Physical Science", "Biological Science", "Commerce", "Arts", "Technology"],
        'available_subjects'   => $allSubjects,
        'available_years'      => $allYears,
        'data'                 => $papers
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status'  => 'error',
        'message' => $e->getMessage()
    ]);
}
