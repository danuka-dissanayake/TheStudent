<?php
/**
 * TheStudent - Past Paper Delete API Endpoint
 * 
 * URL: /api/delete_paper.php
 * Method: POST
 * Accepts: JSON {"paper_id": "12"} or POST field paper_id
 */

require_once __DIR__ . '/../config.php';

header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = getDbConnection();

    $rawInput = file_get_contents('php://input');
    $data = json_decode($rawInput, true);
    if (!is_array($data)) {
        $data = $_POST;
    }

    $paperId = trim($data['paper_id'] ?? '');

    if (empty($paperId)) {
        http_response_code(400);
        echo json_encode([
            'status' => 'error',
            'message' => 'Missing required paper_id.'
        ]);
        exit();
    }

    $stmt = $pdo->prepare("DELETE FROM `past_papers` WHERE `id` = :id");
    $stmt->execute([':id' => $paperId]);

    if ($stmt->rowCount() > 0) {
        echo json_encode([
            'status' => 'success',
            'message' => 'Paper deleted successfully.',
            'paper_id' => $paperId
        ]);
    } else {
        echo json_encode([
            'status' => 'warning',
            'message' => 'No paper found with the given ID, or already deleted.',
            'paper_id' => $paperId
        ]);
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'Delete failed: ' . $e->getMessage()
    ]);
}
