<?php
/**
 * TheStudent - Local secrets template
 * ==================================
 *
 * 1. Copy this file to `config.secrets.php` (same folder, NOT committed to git).
 * 2. Fill in real values and `chmod 600 config.secrets.php`.
 * 3. Rotate any credential that has ever been committed to the repository.
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
