<?php
/**
 * TheStudent - Local secrets template
 * ==================================
 *
 * 1. Copy this file to `config.secrets.php` and NEVER commit it to git.
 *
 *    WHERE to put it (first match wins, see config.php):
 *      A. one level ABOVE public_html  <-- RECOMMENDED, never web-reachable
 *         /home/USER/domains/app.quizontal.site/config.secrets.php
 *      B. two levels above             /home/USER/domains/config.secrets.php
 *      C. next to this file            public_html/config.secrets.php
 *                                      (only safe because .htaccess denies it)
 *
 * 2. Fill in real values and set permissions to 600.
 * 3. Rotate any credential that has ever been committed to the repository.
 *
 * No SSH needed: docs/DEPLOY_DIRECTADMIN_GUI.md walks through all of this in the
 * DirectAdmin File Manager.
 *
 * ---------------------------------------------------------------------------
 * REQUIRED IMMEDIATE ACTION (credentials that leaked in git history):
 *   - DirectAdmin MySQL password for `quizontalcloud_app`
 *   - Gmail app password for the SMTP sender account (and revoke it in
 *     Google Account → Security → App passwords)
 *   - Any personal Google OAuth client secret (client IDs are public, secrets are not)
 * ---------------------------------------------------------------------------
 */

declare(strict_types=1);

// 'production' | 'local'  (never leave 'local' on a public host)
define('APP_ENV', 'production');

/**
 * Master key used for HMAC-SHA256 hashing of OTP codes and password reset tokens.
 * Generate once with:  php -r "echo bin2hex(random_bytes(32));"
 * Changing it invalidates all pending OTP / reset codes (sessions are unaffected).
 */
define('APP_KEY', 'REPLACE_WITH_64_HEX_CHARACTERS');

// --- MySQL (DirectAdmin) ----------------------------------------------------
define('DB_HOST', 'localhost');
define('DB_NAME', 'your_db_name');
define('DB_USER', 'your_db_user');
define('DB_PASS', 'your_db_password_here');
define('DB_CHARSET', 'utf8mb4');

// --- Google Sign-In ---------------------------------------------------------
// Must match GOOGLE_WEB_CLIENT_ID in TheStudentGoogleAuthClient.kt (the *Web*
// client ID used as serverClientId by Credential Manager).
define('GOOGLE_WEB_CLIENT_ID', 'xxxxxxxx.apps.googleusercontent.com');

/**
 * Comma separated list of emails that are granted the `admin` role the first
 * time they register / sign in with Google (after email verification).
 * There is no password-based admin bypass anymore.
 */
define('ADMIN_EMAILS', 'you@example.com');

// --- SMTP (Gmail app password recommended) ----------------------------------
define('SMTP_HOST', 'ssl://smtp.gmail.com');
define('SMTP_PORT', 465);
define('SMTP_USER', 'your-sender@gmail.com');
define('SMTP_PASS', 'your_16_char_app_password');
define('SMTP_FROM_NAME', 'TheStudent Security');

// --- HTTP -------------------------------------------------------------------
// Comma separated allowed browser origins (the Android app needs none).
define('ALLOWED_ORIGINS', 'https://app.quizontal.site');

// --- Assistant single sign-on ------------------------------------------------
// Dedicated secret shared ONLY between this PHP API and the AI assistant
// website's server. Generate with: php -r "echo bin2hex(random_bytes(32));"
// Do NOT reuse APP_KEY, and do NOT copy this into the app, an .env file, or
// any client-side bundle - it authenticates server-to-server calls.
define('ASSISTANT_SSO_SECRET', 'REPLACE_WITH_64_HEX_CHARACTERS_DIFFERENT_FROM_APP_KEY');

// The STABLE production origin of the AI assistant website (no path, no
// trailing slash). It must be HTTPS and must be the exact origin allow-listed
// by WebAssistantConfig / addWebMessageListener. Never a preview deployment URL.
// MUST match WebAssistantConfig.PRODUCTION_ORIGIN in the Android app.
// config.php already defaults this to the same value; override it here only if
// you move the site to a custom domain (then change PRODUCTION_ORIGIN too).
define('ASSISTANT_WEB_ORIGIN', 'https://chatbot-liart-rho-81.vercel.app');

// Website route that receives the POSTed one-time code (default is fine).
define('ASSISTANT_EXCHANGE_PATH', '/api/app-auth');

// --- Diagnostics -------------------------------------------------------------
// Token for api/health.php?token=... — a read-only self-check that reports PHP
// version, DB connectivity, missing tables/columns and the tail of the error log.
// Any 24+ random characters. LEAVE IT EMPTY when you are done debugging: the
// endpoint answers 404 while this is empty.
define('DIAG_TOKEN', '');

// Reject every plain-HTTP request (default 1). Set to 0 only for local dev.
// If your host terminates TLS at a proxy that does not forward HTTPS=on, enable
// TRUST_PROXY_HEADERS below instead of disabling this flag.
define('REQUIRE_HTTPS', '1');

// ONLY enable when the origin server is behind Cloudflare / a trusted reverse
// proxy. Enabling it without a proxy lets clients spoof their IP and evade
// rate limiting + new-location OTP challenges.
define('TRUST_PROXY_HEADERS', '0');
// Optional comma separated list of proxy IPs; empty means "any proxy".
define('TRUSTED_PROXIES', '');
