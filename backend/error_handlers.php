<?php
/**
 * TheStudent — Fatal error safety net & error log location
 * =======================================================
 *
 * Shared by config.php, api/auth.php and api/health.php so that a PHP fatal
 * NEVER produces an empty `500` with `content-length: 0`.
 *
 * On shared hosting there is no terminal, so an empty 500 is undebuggable: the
 * app can only say "Request failed (500)" and the response tells you nothing.
 * With this file loaded:
 *
 *   • every uncaught exception and every fatal error is written to a log file
 *     you can open in the DirectAdmin File Manager, and
 *   • API callers get JSON containing a short reference id that matches the log
 *     line, so the cause can be found without exposing internals.
 *
 * This file has no dependencies and no side effects other than installing the
 * handlers, so it is safe to require from anywhere (PHP 7.4+).
 */

declare(strict_types=1);

// ---------------------------------------------------------------------------
// Log location: one level above public_html so it is never web-reachable.
//   /home/USER/domains/app.quizontal.site/php-error.log
// Override with define('ERROR_LOG_PATH', '…') in config.secrets.php.
// ---------------------------------------------------------------------------
if (!defined('ERROR_LOG_PATH')) {
    $tsLogDir = dirname(__DIR__);
    define('ERROR_LOG_PATH', $tsLogDir . '/php-error.log');
}
if (is_dir(dirname(ERROR_LOG_PATH))) {
    @ini_set('log_errors', '1');
    @ini_set('error_log', ERROR_LOG_PATH);
}

if (!function_exists('ts_is_api_request')) {
    /** True when the current request targets a JSON API endpoint. */
    function ts_is_api_request(): bool
    {
        $script = (string) ($_SERVER['SCRIPT_NAME'] ?? $_SERVER['PHP_SELF'] ?? '');
        return strpos($script, '/api/') !== false
            || (isset($_GET['action']) && $script !== '' && strpos($script, 'auth.php') !== false);
    }
}

if (!function_exists('ts_emit_fatal_json')) {
    /**
     * Log the detail, answer with JSON + a reference id (API only).
     * Safe to call more than once.
     */
    function ts_emit_fatal_json(string $detail): void
    {
        static $alreadyEmitted = false;

        $ref = bin2hex(random_bytes(4));
        error_log('[TheStudent][fatal][' . $ref . '] ' . $detail);

        if ($alreadyEmitted || headers_sent() || !ts_is_api_request()) {
            return;
        }
        $alreadyEmitted = true;

        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        header('X-TheStudent-Error-Ref: ' . $ref);
        echo json_encode([
            'status'  => 'error',
            'message' => 'The server hit an unexpected error. Reference: ' . $ref,
            'ref'     => $ref,
        ]);
    }
}

if (!function_exists('ts_install_error_handlers')) {
    function ts_install_error_handlers(): void
    {
        static $installed = false;
        if ($installed) {
            return;
        }
        $installed = true;

        set_exception_handler(static function (Throwable $e): void {
            ts_emit_fatal_json(
                get_class($e) . ': ' . $e->getMessage()
                . ' in ' . $e->getFile() . ':' . $e->getLine()
            );
        });

        register_shutdown_function(static function (): void {
            $error = error_get_last();
            if ($error === null) {
                return;
            }
            if (!in_array(
                $error['type'],
                [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR, E_USER_ERROR, E_RECOVERABLE_ERROR],
                true
            )) {
                return;
            }
            ts_emit_fatal_json($error['message'] . ' in ' . $error['file'] . ':' . $error['line']);
        });
    }
}
