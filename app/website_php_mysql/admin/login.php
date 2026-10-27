<?php
/**
 * TheStudent - Admin Login Screen
 */
require_once __DIR__ . '/../config.php';

$error = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';

    // Simple per-session throttle to slow down credential stuffing.
    $_SESSION['login_failures'] = (int) ($_SESSION['login_failures'] ?? 0);
    if ($_SESSION['login_failures'] >= 5) {
        $error = 'Too many failed attempts. Please wait a few minutes and try again.';
    } elseif ($username === '' || $password === '') {
        $error = 'Please fill in both fields.';
    } else {
        $pdo = getDbConnection();
        $stmt = $pdo->prepare("SELECT * FROM admins WHERE username = :u LIMIT 1");
        $stmt->execute(['u' => $username]);
        $admin = $stmt->fetch();

        // Only a stored bcrypt hash is accepted - there is no default/backdoor
        // password. Create/reset the admin with:
        //   php -r "echo password_hash('your-new-password', PASSWORD_BCRYPT);"
        //   UPDATE admins SET password_hash = '<hash>' WHERE username = 'admin';
        if ($admin && !empty($admin['password_hash']) && password_verify($password, $admin['password_hash'])) {
            session_regenerate_id(true);
            $_SESSION['login_failures'] = 0;
            $_SESSION['admin_logged_in'] = true;
            $_SESSION['admin_username'] = $admin['username'];
            header('Location: index.php');
            exit;
        }

        $_SESSION['login_failures']++;
        $error = 'Invalid username or password.';
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login | TheStudent</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body class="bg-slate-50 min-h-screen flex items-center justify-center p-4">
    <div class="max-w-md w-full bg-white rounded-3xl border border-slate-200 shadow-xl p-8">
        <div class="text-center mb-6">
            <div class="w-14 h-14 bg-emerald-600 rounded-2xl flex items-center justify-center text-white mx-auto mb-3 shadow-lg shadow-emerald-600/20 text-2xl">
                <i class="fa-solid fa-shield-halved"></i>
            </div>
            <h1 class="text-2xl font-extrabold text-slate-900">Admin Authentication</h1>
            <p class="text-xs text-slate-500 mt-1">TheStudent Past Papers Management System</p>
        </div>

        <?php if ($error): ?>
            <div class="mb-4 p-3.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl font-semibold flex items-center">
                <i class="fa-solid fa-triangle-exclamation mr-2"></i> <?= e($error) ?>
            </div>
        <?php endif; ?>

        <form method="POST" class="space-y-4">
            <div>
                <label class="block text-xs font-bold text-slate-700 uppercase mb-1">Username</label>
                <input type="text" name="username" value="admin" required
                       class="w-full px-4 py-3 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500">
            </div>

            <div>
                <label class="block text-xs font-bold text-slate-700 uppercase mb-1">Password</label>
                <input type="password" name="password" placeholder="••••••••••••" required
                       class="w-full px-4 py-3 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500">
                <p class="text-[11px] text-slate-400 mt-1">Credentials are managed in the <code class="bg-slate-100 px-1 py-0.5 rounded text-slate-700">admins</code> table.</p>
            </div>

            <button type="submit" class="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-bold shadow-lg shadow-emerald-600/20 transition">
                Sign In to Admin Panel
            </button>
        </form>

        <div class="mt-6 text-center">
            <a href="../index.php" class="text-xs font-bold text-slate-500 hover:text-emerald-600">
                <i class="fa-solid fa-arrow-left mr-1"></i> Back to Public Portal
            </a>
        </div>
    </div>
</body>
</html>
