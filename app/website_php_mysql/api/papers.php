<?php
/**
 * DZD Learn - Past Papers Dynamic REST API
 * Returns dynamic grades, subjects, and papers for web & mobile clients.
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/../config.php';

$pdo = getDbConnection();
$action = $_GET['action'] ?? 'get_papers';

try {
    // 1. Get all Grade categories (Grade 6 to Grade 13)
    if ($action === 'get_grades') {
        $stmt = $pdo->query("SELECT id, name, slug, sort_order FROM grades ORDER BY sort_order ASC");
        $grades = $stmt->fetchAll();
        echo json_encode(['status' => 'success', 'data' => $grades]);
        exit;
    }

    // 2. Get available Subjects for a specific Grade
    if ($action === 'get_subjects') {
        $gradeId = isset($_GET['grade_id']) ? (int)$_GET['grade_id'] : 0;
        
        // Dynamically find distinct subjects associated with this grade in papers or defaults
        $sql = "SELECT DISTINCT s.id, s.name, s.slug, s.icon,
                COUNT(p.id) as paper_count
                FROM subjects s
                LEFT JOIN papers p ON p.subject_id = s.id AND p.grade_id = :grade_id AND p.status = 'Published'
                GROUP BY s.id
                ORDER BY s.name ASC";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute(['grade_id' => $gradeId]);
        $subjects = $stmt->fetchAll();
        
        echo json_encode(['status' => 'success', 'data' => $subjects]);
        exit;
    }

    // 3. Get Filtered Past Papers
    if ($action === 'get_papers') {
        $gradeId = isset($_GET['grade_id']) && $_GET['grade_id'] !== '' ? (int)$_GET['grade_id'] : null;
        $subjectId = isset($_GET['subject_id']) && $_GET['subject_id'] !== '' ? (int)$_GET['subject_id'] : null;
        $year = isset($_GET['year']) && $_GET['year'] !== 'All Years' && $_GET['year'] !== '' ? (int)$_GET['year'] : null;
        $medium = isset($_GET['medium']) && $_GET['medium'] !== 'All Mediums' && $_GET['medium'] !== '' ? $_GET['medium'] : null;
        $examType = isset($_GET['exam_type']) && $_GET['exam_type'] !== 'All Types' && $_GET['exam_type'] !== '' ? $_GET['exam_type'] : null;
        $search = isset($_GET['search']) ? trim($_GET['search']) : '';

        $sql = "SELECT p.*, g.name as grade_name, s.name as subject_name, s.icon as subject_icon
                FROM papers p
                JOIN grades g ON p.grade_id = g.id
                JOIN subjects s ON p.subject_id = s.id
                WHERE p.status = 'Published'";
        
        $params = [];

        if ($gradeId) {
            $sql .= " AND p.grade_id = :grade_id";
            $params['grade_id'] = $gradeId;
        }

        if ($subjectId) {
            $sql .= " AND p.subject_id = :subject_id";
            $params['subject_id'] = $subjectId;
        }

        if ($year) {
            $sql .= " AND p.year = :year";
            $params['year'] = $year;
        }

        if ($medium) {
            $sql .= " AND p.medium LIKE :medium";
            $params['medium'] = "%$medium%";
        }

        if ($examType) {
            $sql .= " AND (p.exam_type LIKE :exam_type OR p.title LIKE :exam_type_title)";
            $params['exam_type'] = "%$examType%";
            $params['exam_type_title'] = "%$examType%";
        }

        if ($search !== '') {
            $sql .= " AND (p.title LIKE :search OR s.name LIKE :search_subj OR p.year LIKE :search_yr)";
            $params['search'] = "%$search%";
            $params['search_subj'] = "%$search%";
            $params['search_yr'] = "%$search%";
        }

        $sql .= " ORDER BY p.year DESC, p.id DESC";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $papers = $stmt->fetchAll();

        echo json_encode([
            'status' => 'success',
            'count' => count($papers),
            'data' => $papers
        ]);
        exit;
    }

    echo json_encode(['status' => 'error', 'message' => 'Invalid action']);
} catch (\Exception $e) {
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
}
