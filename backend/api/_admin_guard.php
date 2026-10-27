<?php
/**
 * TheStudent - Admin API guard
 * ===========================
 *
 * Write endpoints (upload_paper.php / delete_paper.php) used to be completely
 * unauthenticated: anybody who found the URL could publish or delete papers and
 * upload arbitrary files to the public web root.
 *
 * They now require a valid session token (Authorization: Bearer …) belonging to
 * an active, verified user with the `admin` role.
 */

declare(strict_types=1);

require_once __DIR__ . '/../config.php';

function ts_api_json_error(string $message, int $status): void
{
    http_response_code($status);
    if (!headers_sent()) {
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
    }
    echo json_encode(['status' => 'error', 'message' => $message]);
    exit();
}

function ts_api_bearer_token(): ?string
{
    $header = '';
    if (!empty($_SERVER['HTTP_AUTHORIZATION'])) {
        $header = trim((string) $_SERVER['HTTP_AUTHORIZATION']);
    } elseif (!empty($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $header = trim((string) $_SERVER['REDIRECT_HTTP_AUTHORIZATION']);
    } elseif (function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        $header = (string) ($headers['Authorization'] ?? $headers['authorization'] ?? '');
    }
    if ($header !== '' && preg_match('/^Bearer\s+([A-Fa-f0-9]{32,256})$/', $header, $m)) {
        return $m[1];
    }
    return null;
}

/**
 * Terminate the request unless the caller is an authenticated administrator.
 */
function ts_require_admin_api(PDO $pdo): array
{
    $token = ts_api_bearer_token();
    if ($token === null) {
        ts_api_json_error('Administrator authentication required.', 401);
    }

    $stmt = $pdo->prepare(
        'SELECT u.* FROM user_tokens t
         JOIN users u ON u.id = t.user_id
         WHERE t.token = :hash AND t.expires_at > NOW() AND u.is_active = 1
         LIMIT 1'
    );
    $stmt->execute(['hash' => hash('sha256', $token)]);
    $user = $stmt->fetch();

    if (!$user) {
        ts_api_json_error('Administrator authentication required.', 401);
    }
    if (($user['role'] ?? '') !== 'admin') {
        ts_api_json_error('This action requires administrator privileges.', 403);
    }

    $pdo->prepare('UPDATE user_tokens SET last_used_at = NOW() WHERE token = :hash')
        ->execute(['hash' => hash('sha256', $token)]);

    return $user;
}

/**
 * Validate an uploaded PDF file ($_FILES entry).
 *
 * @return string|null Error message, or null when the file is acceptable.
 */
function ts_validate_uploaded_pdf(array $file, int $maxBytes = 26214400): ?string
{
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
        return 'Upload failed (error code ' . (int) ($file['error'] ?? -1) . ').';
    }
    $size = (int) ($file['size'] ?? 0);
    if ($size <= 0) {
        return 'The uploaded file is empty.';
    }
    if ($size > $maxBytes) {
        return 'The uploaded file exceeds the ' . round($maxBytes / 1048576) . ' MB limit.';
    }
    if (!is_uploaded_file($file['tmp_name'] ?? '')) {
        return 'Invalid upload source.';
    }
    if (function_exists('finfo_open')) {
        $finfo = finfo_open(FILEINFO_MIME_TYPE);
        $mime = $finfo ? (string) finfo_file($finfo, $file['tmp_name']) : '';
        if ($finfo) {
            finfo_close($finfo);
        }
        if ($mime !== 'application/pdf') {
            return 'Only PDF documents may be uploaded.';
        }
    }
    // Magic-number check (defence in depth if fileinfo is unavailable).
    $handle = @fopen($file['tmp_name'], 'rb');
    $magic = $handle ? (string) fread($handle, 5) : '';
    if ($handle) {
        fclose($handle);
    }
    if (strncmp($magic, '%PDF-', 5) !== 0) {
        return 'Only PDF documents may be uploaded.';
    }
    return null;
}
