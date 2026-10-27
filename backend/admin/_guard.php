<?php
/**
 * TheStudent Admin Console - Authentication Guard
 * ==============================================
 *
 * The paper administration dashboard used to be world-readable AND world-writable
 * (anyone could upload or delete papers). It now requires a real administrator
 * account from the `users` table:
 *
 *   • Password login (bcrypt) against `users` where role = 'admin'
 *   • Account must be active + email verified
 *   • Session cookie: HttpOnly, SameSite=Strict, Secure on HTTPS
 *   • Session id regenerated on login (fixation defence)
 *   • CSRF token required on every POST
 *   • Per-IP + per-account login throttling (login_attempts table)
 *   • Role/active state re-checked on every request, not just at login
 */

declare(strict_types=1);

require_once __DIR__ . '/../config.php';

const TS_ADMIN_SESSION_NAME = 'ts_admin_sid';
const TS_ADMIN_MAX_FAILURES = 5;
const TS_ADMIN_LOCK_SECONDS = 900;

function ts_admin_session_start(): void
{
    if (session_status() === PHP_SESSION_ACTIVE) {
        return;
    }
    session_set_cookie_params([
        'lifetime' => 0,
        'path'     => '/',
        'secure'   => ts_is_https(),
        'httponly' => true,
        'samesite' => 'Strict',
    ]);
    session_name(TS_ADMIN_SESSION_NAME);
    session_start();
}

function ts_csrf_token(): string
{
    ts_admin_session_start();
    if (empty($_SESSION['ts_admin_csrf']) || !is_string($_SESSION['ts_admin_csrf'])) {
        $_SESSION['ts_admin_csrf'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['ts_admin_csrf'];
}

function ts_csrf_field(): string
{
    return '<input type="hidden" name="csrf_token" value="' . htmlspecialchars(ts_csrf_token(), ENT_QUOTES, 'UTF-8') . '">';
}

function ts_verify_csrf(): void
{
    $provided = (string) ($_POST['csrf_token'] ?? '');
    $expected = (string) ($_SESSION['ts_admin_csrf'] ?? '');
    if ($expected === '' || $provided === '' || !hash_equals($expected, $provided)) {
        http_response_code(403);
        header('Content-Type: text/plain; charset=utf-8');
        echo 'Invalid or expired form token. Please reload the dashboard and try again.';
        exit();
    }
}

function ts_admin_login_failures(PDO $pdo, string $ip, string $email): int
{
    try {
        $stmt = $pdo->prepare(
            "SELECT COUNT(*) AS c FROM login_attempts
             WHERE (ip_address = :ip OR email = :email)
               AND attempted_at >= (NOW() - INTERVAL 15 MINUTE)"
        );
        $stmt->execute(['ip' => $ip, 'email' => $email]);
        return (int) ($stmt->fetch()['c'] ?? 0);
    } catch (Throwable $e) {
        error_log('[TheStudent][admin] rate check failed: ' . $e->getMessage());
        return 0;
    }
}

function ts_admin_record_failure(PDO $pdo, string $ip, string $email): void
{
    try {
        $pdo->prepare('INSERT INTO login_attempts (ip_address, email) VALUES (:ip, :email)')
            ->execute(['ip' => substr($ip, 0, 45), 'email' => substr($email, 0, 191)]);
    } catch (Throwable $e) {
        error_log('[TheStudent][admin] failed to record attempt: ' . $e->getMessage());
    }
}

function ts_admin_clear_failures(PDO $pdo, string $ip, string $email): void
{
    try {
        $pdo->prepare('DELETE FROM login_attempts WHERE ip_address = :ip OR email = :email')
            ->execute(['ip' => $ip, 'email' => $email]);
    } catch (Throwable $e) {
        error_log('[TheStudent][admin] failed to clear attempts: ' . $e->getMessage());
    }
}

/**
 * Render the standalone login screen and stop execution.
 */
function ts_admin_render_login(string $error = ''): void
{
    http_response_code(401);
    $safeError = htmlspecialchars($error, ENT_QUOTES, 'UTF-8');
    $csrf = ts_csrf_field();
    ?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>TheStudent • Admin Sign In</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: radial-gradient(circle at 20% 20%, #064e3b 0%, #022c22 55%, #01130f 100%);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        .card-auth {
            width: min(430px, 92vw);
            border: 1px solid rgba(148, 163, 184, .18);
            border-radius: 24px;
            background: rgba(15, 23, 42, .82);
            backdrop-filter: blur(18px);
            box-shadow: 0 30px 60px rgba(0, 0, 0, .45);
        }
        .brand-dot {
            width: 46px; height: 46px; border-radius: 14px;
            background: linear-gradient(135deg, #10b981, #047857);
            display: flex; align-items: center; justify-content: center;
            font-size: 22px;
        }
        .form-control {
            background: rgba(15, 23, 42, .6);
            border-color: rgba(148, 163, 184, .25);
            color: #e2e8f0;
        }
        .form-control:focus {
            background: rgba(15, 23, 42, .85);
            color: #f8fafc;
            border-color: #10b981;
            box-shadow: 0 0 0 .2rem rgba(16, 185, 129, .18);
        }
        .btn-emerald { background: #059669; border-color: #059669; color: #fff; font-weight: 600; }
        .btn-emerald:hover { background: #047857; border-color: #047857; color: #fff; }
    </style>
</head>
<body>
    <div class="card card-auth text-white">
        <div class="card-body p-4 p-md-5">
            <div class="d-flex align-items-center gap-3 mb-4">
                <div class="brand-dot">🎓</div>
                <div>
                    <div class="fs-5 fw-bold lh-1">TheStudent</div>
                    <div class="text-white-50 small">Administrator Console</div>
                </div>
            </div>

            <?php if ($safeError !== ''): ?>
                <div class="alert alert-danger py-2 small rounded-3"><?= $safeError ?></div>
            <?php endif; ?>

            <form method="POST" autocomplete="off">
                <?= $csrf ?>
                <input type="hidden" name="action" value="login">
                <div class="mb-3">
                    <label class="form-label small text-white-50">Admin email</label>
                    <input type="email" name="email" class="form-control rounded-3" required autofocus>
                </div>
                <div class="mb-4">
                    <label class="form-label small text-white-50">Password</label>
                    <input type="password" name="password" class="form-control rounded-3" required>
                </div>
                <button type="submit" class="btn btn-emerald w-100 rounded-3 py-2">
                    Sign in to dashboard
                </button>
            </form>

            <p class="text-white-50 small mt-4 mb-0">
                Only accounts with the <code>admin</code> role can access this console.
                Grant it in MySQL: <code>UPDATE users SET role='admin' WHERE email='…';</code>
            </p>
        </div>
    </div>
</body>
</html>
    <?php
    exit();
}

/**
 * Authenticate the current request or stop with the login screen.
 * Returns the authenticated admin user row on success.
 */
function ts_require_admin(PDO $pdo): array
{
    ts_admin_session_start();

    $postAction = (string) ($_POST['action'] ?? '');

    // ---- Sign out -------------------------------------------------------
    if ($postAction === 'logout') {
        ts_verify_csrf();
        $_SESSION = [];
        if (ini_get('session.use_cookies')) {
            $params = session_get_cookie_params();
            setcookie(session_name(), '', time() - 42000, $params['path'], $params['domain'] ?? '', (bool) $params['secure'], (bool) $params['httponly']);
        }
        session_destroy();
        header('Location: ' . strtok($_SERVER['REQUEST_URI'] ?? 'index.php', '?'));
        exit();
    }

    // ---- Sign in --------------------------------------------------------
    if ($postAction === 'login') {
        ts_verify_csrf();

        $ip = ts_client_ip();
        $email = strtolower(trim((string) ($_POST['email'] ?? '')));
        $password = (string) ($_POST['password'] ?? '');

        if ($email === '' || $password === '' || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            ts_admin_render_login('Please enter your admin email and password.');
        }

        if (ts_admin_login_failures($pdo, $ip, $email) >= TS_ADMIN_MAX_FAILURES) {
            ts_admin_render_login('Too many failed attempts. Please wait 15 minutes and try again.');
        }

        $stmt = $pdo->prepare('SELECT * FROM users WHERE email = :email LIMIT 1');
        $stmt->execute(['email' => $email]);
        $user = $stmt->fetch();

        $hash = ($user && !empty($user['password_hash']))
            ? (string) $user['password_hash']
            : '$2b$12$GzWllz3yIrCQigBsL2LStuX.RarOSsiry9XR6/pv9sd0lbIiA2g5i';
        $passwordOk = password_verify($password, $hash);
        $isAdmin = $user
            && ($user['role'] ?? '') === 'admin'
            && (int) $user['is_active'] === 1
            && (int) $user['is_email_verified'] === 1;

        if (!$passwordOk || !$isAdmin) {
            ts_admin_record_failure($pdo, $ip, $email);
            // Deliberately generic: never reveal whether the account exists or
            // whether it simply lacks the admin role.
            ts_admin_render_login('Invalid administrator credentials.');
        }

        ts_admin_clear_failures($pdo, $ip, $email);
        session_regenerate_id(true);
        $_SESSION['ts_admin_user_id'] = (int) $user['id'];
        $_SESSION['ts_admin_email'] = (string) $user['email'];
        $_SESSION['ts_admin_last_seen'] = time();
        $_SESSION['ts_admin_csrf'] = bin2hex(random_bytes(32));

        $pdo->prepare('UPDATE users SET last_login_ip = :ip, last_login_at = NOW() WHERE id = :id')
            ->execute(['ip' => substr($ip, 0, 45), 'id' => (int) $user['id']]);

        header('Location: ' . strtok($_SERVER['REQUEST_URI'] ?? 'index.php', '?'));
        exit();
    }

    // ---- Existing session ----------------------------------------------
    $userId = (int) ($_SESSION['ts_admin_user_id'] ?? 0);
    if ($userId <= 0) {
        ts_admin_render_login();
    }

    $stmt = $pdo->prepare('SELECT * FROM users WHERE id = :id AND role = :role AND is_active = 1 AND is_email_verified = 1 LIMIT 1');
    $stmt->execute(['id' => $userId, 'role' => 'admin']);
    $user = $stmt->fetch();

    if (!$user) {
        $_SESSION = [];
        session_destroy();
        ts_admin_render_login('Your administrator session is no longer valid. Please sign in again.');
    }

    // Idle timeout: 2 hours
    if (time() - (int) ($_SESSION['ts_admin_last_seen'] ?? 0) > 7200) {
        $_SESSION = [];
        session_destroy();
        ts_admin_render_login('Your session expired due to inactivity. Please sign in again.');
    }
    $_SESSION['ts_admin_last_seen'] = time();

    // Any state-changing request must carry a valid CSRF token.
    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
        ts_verify_csrf();
    }

    return $user;
}
