<?php
/**
 * TheStudent — Deployment self-check ("why am I getting a 500?")
 * =============================================================
 *
 * A read-only diagnostic you can open in a browser, for shared hosting where
 * there is no SSH and no way to tail a log.
 *
 *   https://app.quizontal.site/api/health.php?token=YOUR_DIAG_TOKEN
 *
 * Setup (one time): add this line to config.secrets.php
 *
 *   define('DIAG_TOKEN', '<32+ random characters>');
 *
 * Without that constant — or with the wrong token — this file answers 404, so
 * it is invisible to everyone else. It never prints a password or a key: only
 * whether each setting is present, and the database/table state.
 *
 * DELETE or empty DIAG_TOKEN when you are done debugging.
 */

declare(strict_types=1);

// 404 unless the caller knows the token defined in config.secrets.php.
$__expected = defined('DIAG_TOKEN') ? (string) DIAG_TOKEN : '';
if ($__expected === '' || strlen($__expected) < 24) {
    if (!headers_sent()) {
        http_response_code(404);
        header('Content-Type: text/plain; charset=utf-8');
    }
    echo 'Not found';
    exit();
}

$__provided = isset($_GET['token']) ? (string) $_GET['token'] : '';
if (!hash_equals($__expected, $__provided)) {
    if (!headers_sent()) {
        http_response_code(404);
        header('Content-Type: text/plain; charset=utf-8');
    }
    echo 'Not found';
    exit();
}

require_once __DIR__ . '/../error_handlers.php';
ts_install_error_handlers();

require_once __DIR__ . '/../config.php';

if (!headers_sent()) {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
}

$report = [
    'generated_at' => date('c'),
    'php' => [
        'version'      => PHP_VERSION,
        'sapi'         => PHP_SAPI,
        'memory_limit' => ini_get('memory_limit'),
        'max_execution_time' => ini_get('max_execution_time'),
    ],
    'extensions' => [
        'pdo'        => extension_loaded('pdo'),
        'pdo_mysql'  => extension_loaded('pdo_mysql'),
        'openssl'    => extension_loaded('openssl'),
        'curl'       => extension_loaded('curl'),
        'json'       => extension_loaded('json'),
        'mbstring'   => extension_loaded('mbstring'),
    ],
    'config' => [
        'secrets_file' => [
            'loaded'   => defined('DB_NAME') && (string) DB_NAME !== '',
            'location' => (static function (): string {
                foreach ([
                    dirname(__DIR__) . '/config.secrets.php',
                    dirname(__DIR__, 2) . '/config.secrets.php',
                    __DIR__ . '/config.secrets.php',
                ] as $candidate) {
                    if (is_file($candidate)) {
                        return $candidate;
                    }
                }
                return 'NOT FOUND';
            })(),
            'inside_web_root' => is_file(__DIR__ . '/../config.secrets.php'),
        ],
        'app_env'          => defined('APP_ENV') ? APP_ENV : '(unset)',
        'app_key_length'   => defined('APP_KEY') ? strlen((string) APP_KEY) : 0,
        'db_name_set'      => defined('DB_NAME') && (string) DB_NAME !== '',
        'db_user_set'      => defined('DB_USER') && (string) DB_USER !== '',
        'db_pass_set'      => defined('DB_PASS') && (string) DB_PASS !== '',
        'smtp_user_set'    => defined('SMTP_USER') && (string) SMTP_USER !== '',
        'smtp_pass_set'    => defined('SMTP_PASS') && (string) SMTP_PASS !== '',
        'google_client_set'=> defined('GOOGLE_WEB_CLIENT_ID') && strpos((string) GOOGLE_WEB_CLIENT_ID, 'apps.googleusercontent.com') !== false,
        'admin_emails'     => defined('ADMIN_EMAILS') ? (string) ADMIN_EMAILS : '',
        'require_https'    => defined('REQUIRE_HTTPS') ? (string) REQUIRE_HTTPS : '1',
        'trust_proxy'      => defined('TRUST_PROXY_HEADERS') ? (string) TRUST_PROXY_HEADERS : '0',
    ],
    'request' => [
        'https_detected' => ts_is_https(),
        'client_ip'      => ts_client_ip(),
        'script'         => $_SERVER['SCRIPT_NAME'] ?? '',
    ],
    'database' => [],
    'error_log' => [],
];

// --- Database ---------------------------------------------------------------
$requiredTables = ['users', 'email_verifications', 'user_tokens', 'login_attempts', 'password_resets'];
$requiredColumns = [
    'users'               => ['id', 'email', 'password_hash', 'full_name', 'role', 'is_active', 'is_email_verified', 'auth_provider', 'last_login_ip', 'suspicious_flag'],
    'email_verifications' => ['id', 'user_id', 'email', 'otp_code', 'otp_type', 'pending_data', 'requested_ip', 'attempts', 'is_used', 'expires_at'],
    'user_tokens'         => ['id', 'user_id', 'token', 'expires_at', 'last_used_at'],
    'login_attempts'      => ['id', 'ip_address', 'email', 'attempted_at'],
    'password_resets'     => ['id', 'user_id', 'token_hash', 'expires_at', 'used_at'],
];

try {
    // Own connection on purpose: getDbConnection() answers a generic 503 and
    // exits on failure, which would hide the reason we are here to find.
    $pdo = new PDO(
        sprintf('mysql:host=%s;dbname=%s;charset=%s', DB_HOST, DB_NAME, DB_CHARSET),
        (string) DB_USER,
        (string) DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_TIMEOUT => 5]
    );
    $report['database']['connected'] = true;

    $existing = [];
    $stmt = $pdo->query('SHOW TABLES');
    foreach ($stmt->fetchAll(PDO::FETCH_NUM) as $row) {
        $existing[] = (string) $row[0];
    }

    $missingTables = array_values(array_diff($requiredTables, $existing));
    $report['database']['tables_found']   = $existing;
    $report['database']['tables_missing'] = $missingTables;

    $columnState = [];
    foreach ($requiredColumns as $table => $columns) {
        if (in_array($table, $existing, true)) {
            try {
                $cols = [];
                foreach ($pdo->query('SHOW COLUMNS FROM `' . $table . '`')->fetchAll() as $col) {
                    $cols[] = (string) $col['Field'];
                }
                $columnState[$table] = [
                    'missing_columns' => array_values(array_diff($columns, $cols)),
                ];
            } catch (Throwable $e) {
                $columnState[$table] = ['error' => $e->getMessage()];
            }
        }
    }
    $report['database']['columns'] = $columnState;

    // Can we actually write? (CREATE privilege on shared hosting is not a given.)
    try {
        $pdo->exec('CREATE TABLE IF NOT EXISTS `login_attempts` (
            `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            `ip_address` VARCHAR(45) NOT NULL,
            `email` VARCHAR(191) NOT NULL,
            `attempted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
        $report['database']['can_create_tables'] = true;
    } catch (Throwable $e) {
        $report['database']['can_create_tables'] = false;
        $report['database']['create_error'] = $e->getMessage();
    }

    $report['database']['server_version'] = $pdo->getAttribute(PDO::ATTR_SERVER_VERSION);
} catch (Throwable $e) {
    $report['database']['connected'] = false;
    $report['database']['error']     = $e->getMessage();
    $report['database']['error_code'] = $e instanceof PDOException ? ($e->errorInfo[1] ?? null) : null;
}

// --- Sign-up dry run --------------------------------------------------------
/**
 * Replays the exact queries `register` performs, in order, against a throw-away
 * e-mail address. The first step that throws is the cause of the 500 — this is
 * the whole point of this endpoint on a host with no shell.
 */
$report['signup_dry_run'] = [];
if (!empty($report['database']['connected'])) {
    $probeEmail = 'health-probe@example.invalid';
    $steps = [
        '1. rate-limit read (login_attempts)' => [
            "SELECT COUNT(*) AS c FROM login_attempts
             WHERE email = :value AND attempted_at >= (NOW() - INTERVAL 15 MINUTE)",
            ['value' => $probeEmail],
        ],
        '2. OTP throttle read (email_verifications)' => [
            "SELECT COUNT(*) AS c FROM email_verifications
             WHERE email = :value AND created_at >= (NOW() - INTERVAL 15 MINUTE)",
            ['value' => $probeEmail],
        ],
        '3. existing-user lookup (users)' => [
            'SELECT id, is_email_verified, is_active FROM users WHERE email = :email LIMIT 1',
            ['email' => $probeEmail],
        ],
        '4. OTP column width check' => [
            "SHOW COLUMNS FROM email_verifications LIKE 'otp_code'",
            [],
        ],
        '5. password_resets table' => [
            'SELECT COUNT(*) AS c FROM password_resets',
            [],
        ],
        '6. user_tokens table' => [
            'SELECT COUNT(*) AS c FROM user_tokens',
            [],
        ],
    ];

    foreach ($steps as $label => $definition) {
        [$sql, $params] = $definition;
        try {
            $stmt = $pdo->prepare($sql);
            $stmt->execute($params);
            $stmt->fetchAll();
            $report['signup_dry_run'][$label] = 'ok';
        } catch (Throwable $e) {
            $report['signup_dry_run'][$label] = 'FAILED: ' . $e->getMessage();
        }
    }
}

// --- Uploads directory ------------------------------------------------------
$uploads = dirname(__DIR__) . '/uploads';
$report['uploads'] = [
    'path'      => $uploads,
    'exists'    => is_dir($uploads),
    'writable'  => is_dir($uploads) ? is_writable($uploads) : false,
    'htaccess'  => is_file($uploads . '/.htaccess'),
];

// --- Tail of the PHP error log ---------------------------------------------
$logPath = defined('ERROR_LOG_PATH') ? ERROR_LOG_PATH : dirname(__DIR__) . '/php-error.log';
$report['error_log']['path']   = $logPath;
$report['error_log']['exists'] = is_file($logPath);
if (is_file($logPath) && is_readable($logPath)) {
    $lines = @file($logPath);
    if (is_array($lines)) {
        $report['error_log']['last_lines'] = array_slice($lines, -25);
    }
}

// --- Verdict ----------------------------------------------------------------
$problems = [];
if (!$report['php']['version'] || version_compare(PHP_VERSION, '7.4.0', '<')) {
    $problems[] = 'PHP is older than 7.4 — ask your host to switch the domain to PHP 8.1+.';
}
if (!extension_loaded('pdo_mysql')) {
    $problems[] = 'The pdo_mysql extension is missing.';
}
if ($report['config']['secrets_file']['location'] === 'NOT FOUND') {
    $problems[] = 'config.secrets.php was not found (expected one level above public_html).';
}
if ($report['config']['app_key_length'] < 32) {
    $problems[] = 'APP_KEY is missing or shorter than 32 characters.';
}
if (empty($report['database']['connected'])) {
    $problems[] = 'Database connection failed — check DB_NAME / DB_USER / DB_PASS.';
} elseif (!empty($report['database']['tables_missing'])) {
    $problems[] = 'Missing tables: ' . implode(', ', $report['database']['tables_missing'])
        . '. Sign-up will fail until they exist — auth.php now creates them on demand, so re-run a request and check again.';
}
foreach (($report['database']['columns'] ?? []) as $table => $state) {
    if (!empty($state['missing_columns'])) {
        $problems[] = 'Missing columns in ' . $table . ': ' . implode(', ', $state['missing_columns']);
    }
}
if (empty($report['config']['smtp_pass_set'])) {
    $problems[] = 'SMTP_PASS is empty — verification e-mails will not be sent.';
}
foreach (($report['signup_dry_run'] ?? []) as $step => $outcome) {
    if (strpos((string) $outcome, 'FAILED') === 0) {
        $problems[] = 'Sign-up step "' . $step . '" failed: ' . substr((string) $outcome, 8);
    }
}
$report['problems'] = $problems;
$report['ok'] = empty($problems);

echo json_encode($report, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
