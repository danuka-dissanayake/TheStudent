# TheStudent — Authentication & Session Security

_Rewritten 2026-10-27 · branch `arena/e7cd0804-thestudent`_

This document describes the production authentication model that replaces the previous
"OTP + hardcoded admin" implementation, what was wrong with it, and how to deploy the new
backend safely.

---

## 1. What was broken (and is now fixed)

| # | Severity | Issue | Status |
|---|---|---|---|
| 1 | **Critical** | Client-side admin bypass in `AuthScreen.kt`: typing `admin@gmail.com` / `admin123` created a local session with the literal token `admin_master_token` and opened the admin panel — no server involved, worked offline. | Removed. Admin is a server-issued role only. |
| 2 | **Critical** | `TheStudentViewModel` also treated the token `admin_master_token` (or the e-mail `admin@gmail.com`) as an administrator. | Removed; role comes from `users.role` via `/me`. |
| 3 | **Critical** | Backend had a hardcoded `admin@gmail.com` / `admin123` login that auto-created an admin account. | Removed. |
| 4 | **Critical** | `/admin/index.php` and `api/upload_paper.php` / `api/delete_paper.php` were **completely unauthenticated** — anyone could upload/delete papers and files. | Admin session (panel) and `Authorization: Bearer` + admin role (API) required. |
| 5 | **Critical** | Live DB password + Gmail app password committed in `backend/config.php`, and the same Gmail credentials compiled into the APK (`TheStudentEmailSender.kt`). | Secrets moved to env / git-ignored `config.secrets.php`; client SMTP sender deleted. **Rotate both credentials.** |
| 6 | **Critical** | SMTP client disabled TLS verification (`verify_peer=false`, `allow_self_signed=true`) → credentials interceptable. | Strict TLS + hostname verification. |
| 7 | High | Session tokens stored in plaintext in MySQL; accepted from `?token=` query strings (leaks into logs/Referer). | Only SHA-256 digests stored; Bearer header only. |
| 8 | High | OTP codes stored in plaintext and compared with `!==`; attempts incremented non-atomically (race → unlimited guesses). | HMAC-SHA256 with `APP_KEY`, `hash_equals`, atomic guarded increment, single-use consumption. |
| 9 | High | `is_active` was never checked on login/`me` — banned accounts kept full access. | Enforced on login, OTP, Google, `me` and admin guard. |
| 10 | High | `X-Forwarded-For` / `CF-Connecting-IP` trusted blindly → trivial rate-limit + "new location" OTP bypass. | Proxy headers only when explicitly enabled + trusted-proxy list. |
| 11 | High | Client logged the user out on *any* network error during session validation (unusable offline). | `Valid` / `Invalid` / `Unreachable` are now distinct; only 401/403 clear the session. |
| 12 | Medium | Session token stored in plain SharedPreferences (readable from backups / root). | AES-256-GCM via Android Keystore; ciphertext only on disk. |
| 13 | Medium | Login revealed whether an e-mail exists (timing + wording); unverified state leaked. | Dummy bcrypt verification + generic messages. |
| 14 | Medium | Unlimited OTP e-mails per address (mail bombing + SMTP quota exhaustion). | Per-email and per-IP throttles (3/15 min, 8/hour, 10 IP/15 min). |
| 15 | Medium | Password policy 6 chars, no complexity, `trim()` applied to passwords. | 8–72 bytes, letter + digit, common-password blocklist, no trimming. |
| 16 | Medium | `resend_otp` always re-issued a *registration* code, breaking the IP-change/password-reset challenges. | Purpose preserved; pending data carried correctly. |
| 17 | Medium | No password reset, no password change, no "sign out everywhere", no session cap. | `request_password_reset`, `reset_password`, `change_password`, `logout_all`, 10-session cap. |
| 18 | Medium | OTP code embedded in the e-mail **subject** (`Code: 123456`). | Generic subject; code lives only in the body. |
| 19 | Low | DB connection errors returned PDO messages to clients; prepared-statement API keys not validated. | Generic 503, details to `error_log` only. |
| 20 | Low | Google tokens: `aud` checked only if configured; `iss`, `exp`, `email_verified` ignored. | All four validated, fails closed when the client ID is unset. |
| 21 | Low | Legacy `app/website_php_mysql/admin/login.php` accepted `admin123password` and printed it on the page. | Backdoor removed, throttling + session regeneration added. |
| 22 | **Critical** | `app/website_php_mysql/database.sql` seeded an admin account with a published password hash for `admin123password`. | Seed row deleted; file now documents how to create the first admin safely. |
| 23 | High | Legacy site `session_start()` used PHP defaults (no `HttpOnly`, no `SameSite`, no `Secure`). | Cookie hardened before `session_start()`. |
| 24 | High | `api/download.php` built the file path from the DB value (`../` traversal) and put the paper title straight into `Content-Disposition` (header injection). | Path pinned to the site root via `realpath`, filename sanitised, `nosniff` added. |
| 25 | Medium | Legacy `api/papers.php` and `config.php` echoed PDO exception text to clients. | Generic messages; real error goes to `error_log`. |
| 26 | Medium | Uploads folder was writable and executable from the web root. | `.htaccess` created on boot: denies script extensions, disables indexes/CGI. |
| 27 | Low | `api/download.php` redirected to any URL scheme stored in the DB (`javascript:`/`data:`). | Only `http`/`https` redirects allowed. |
| 28 | **High** | The "new sign-in location" challenge compared raw IP strings. On Sri Lankan mobile networks (CGNAT) the address changes constantly, so students would be e-mailed a code on nearly every login — and locked out completely whenever SMTP was down. | Challenge only when the network really differs (`/16` for IPv4, `/48` for IPv6); private/loopback addresses never challenge. |
| 29 | Medium | Re-registering an e-mail whose first sign-up was never verified kept the **old** password (the new one was thrown away), so the student could not sign in with the password they just chose. | The pending registration now updates the unverified account's password/name/role. Already-verified accounts are never overwritten. |
| 30 | **High** | `android:usesCleartextTraffic="true"` — the app would happily send the Bearer token and passwords over plain HTTP if someone typed an `http://` server URL. | Cleartext disabled app-wide; `network_security_config.xml` allows it only for localhost/emulator/LAN. Any other `http://` URL is upgraded to `https://`, and the session store is excluded from cloud backups. |

---

## 2. New model

### 2.1 Sessions
```
POST /api/auth.php?action=login  ──►  { token: 64-hex, expires_at, user{…, role} }
```
* 256-bit random opaque token, **stored as `sha256(token)`** in `user_tokens`.
* 30-day expiry, **sliding**: `/me` renews when < 15 days remain.
* Max 10 concurrent sessions (oldest trimmed); `logout_all` / password change wipes all.
* Transport: `Authorization: Bearer <token>` **only**. Query-string tokens removed.
* Client stores it AES-GCM-encrypted through the Android Keystore.

### 2.2 OTP e-mail verification
* 6 digits from `random_int`, **stored as `hmac_sha256(code, APP_KEY)`**, 10 min TTL, single use.
* Max 5 attempts per code, atomic `UPDATE … WHERE attempts < 5`.
* Issuing a new code invalidates every other unused code for that address.
* Flow purposes: `registration`, `unverified_account`, `ip_change`, `login_challenge`, `password_reset`.
* Delivery failure now returns 503 instead of pretending success (the user is not stranded).

### 2.3 Password reset
```
request_password_reset(email)  → always generic success
verify_otp(email, code)        → { status: "reset_required", reset_token }   (purpose = password_reset)
reset_password(reset_token, new_password) → sets hash, revokes ALL sessions
```

### 2.4 Roles
`users.role ∈ {student, admin}`. `ADMIN_EMAILS` (server secrets) grants `admin` the first time that
address completes verification. **No password or token can grant admin from the client.**
The drawer entry and `openAdminPanel()` are additionally gated, and writes are re-checked server-side.

### 2.5 Admin console
`/admin/index.php` now requires a password login against `users` (role `admin`, active, verified),
with HttpOnly/SameSite=Strict/Secure cookies, session-id regeneration, per-IP+account throttling,
2-hour idle timeout, and a CSRF token on every POST. Uploads are restricted to real PDFs ≤ 25 MB.

---

## 3. API reference (`/api/auth.php`)

| Action | Method | Auth | Body | Returns |
|---|---|---|---|---|
| `register` | POST | – | `full_name, email, password, target_exam?` | `requires_verification` or `error` |
| `login` | POST | – | `email, password` | `success` (token) / `requires_verification` |
| `verify_otp` | POST | – | `email, otp_code` | `success` (token) / `reset_required` |
| `resend_otp` | POST | – | `email` | `success` + `resend_cooldown` |
| `request_password_reset` | POST | – | `email` | generic `success` |
| `reset_password` | POST | – | `reset_token, new_password` | `success` |
| `change_password` | POST | Bearer | `current_password, new_password` | `success` (new token) |
| `google_login` | POST | – | `id_token, target_exam?` | `success` / `requires_verification` |
| `me` (alias `profile`) | GET | Bearer | – | `success` + `user`, `expires_at` |
| `logout` | POST | Bearer | – | `success` |
| `logout_all` | POST | Bearer | – | `success` |

Status codes: `400` validation, `401` bad credentials/session, `403` disabled/unverified/not admin,
`405` wrong method, `409` conflict, `413` body too large, `429` throttled (+`retry_after`), `503` mail/db down.

Papers: `GET api/get_papers.php` stays public; `POST api/upload_paper.php` and `api/delete_paper.php`
require an admin bearer token.

---

## 4. Deploying (DirectAdmin)

1. **Rotate the leaked credentials** (they are in git history forever):
   * MySQL password for `quizontalcloud_app` (DirectAdmin → MySQL Management).
   * Gmail app password for the sender account (Google Account → Security → App passwords → revoke).
2. Create `backend/config.secrets.php` from `config.secrets.example.php`:
   ```bash
   php -r "echo bin2hex(random_bytes(32));"   # APP_KEY
   chmod 600 backend/config.secrets.php
   ```
   The file is git-ignored; `config.php` refuses to serve the API if `APP_KEY`/DB settings are missing.
3. Upload `backend/` (config, smtp_mailer, api/*, admin/*) to the host. `auth.php` self-migrates:
   widens `otp_code`, adds `requested_ip` / `last_used_at`, creates `password_resets`.
4. Grant yourself admin:
   ```sql
   UPDATE users SET role='admin' WHERE email='you@example.com';
   ```
   (or list the address in `ADMIN_EMAILS` before first login).
5. Set `TRUST_PROXY_HEADERS=1` **only** behind Cloudflare/a reverse proxy.
6. Smoke test:
   ```bash
   curl -s -X POST 'https://app.quizontal.site/api/auth.php?action=login' \
        -H 'Content-Type: application/json' \
        -d '{"email":"you@example.com","password":"…"}'
   curl -s 'https://app.quizontal.site/api/auth.php?action=me' -H "Authorization: Bearer <token>"
   ```
7. Rebuild the app (`upload_paper.php` / `delete_paper.php` now require the header the new client sends).

> **Expected side effect:** every existing session token and pending OTP stops working after this
> deploy (only digests are stored now). Users simply sign in again.

---

## 5. Client changes

| File | Change |
|---|---|
| `data/remote/TheStudentAuthClient.kt` | Rewritten: typed results (`Success` / `RequiresOtp` / `ResetRequired` / `Error`), `ApiOutcome` for non-session calls, `SessionValidation` (valid / invalid / unreachable), Bearer-only, exhaustive error mapping, reset/change/logout-all endpoints. |
| `data/local/TheStudentPreferences.kt` | Token encrypted with an Android Keystore AES-256-GCM key; `userRole`, `sessionExpiresAt`, `hasUsableSession()`, `isAdmin`. Plaintext tokens from older builds are discarded. |
| `ui/screens/AuthScreen.kt` | Admin bypass deleted; password policy mirrored client-side; passwords never trimmed; e-mail validated; cooldowns from the server; **real "Forgot password?" flow** (e-mail → code → new password); role + expiry persisted. |
| `viewmodel/TheStudentViewModel.kt` | Session validation distinguishes invalid vs offline; `isAdmin` exposed from the server role; `openAdminPanel()` refused for non-admins; logout revokes server-side. |
| `ui/components/ModernSidebarDrawer.kt` | Admin entry shown only for `isAdmin` (no e-mail comparison). |
| `data/remote/PastPapers*.kt` | Upload/delete send `Authorization: Bearer`; calls are skipped without an admin session. |
| `data/remote/TheStudentEmailSender.kt` | **Deleted** — the APK no longer contains SMTP credentials. |
| `AndroidManifest.xml` + `res/xml/network_security_config.xml` | `usesCleartextTraffic` off; cleartext only for localhost / emulator / LAN hosts. |
| `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | The session store is excluded from cloud backup and device transfer. |

---

### 5.1 Legacy PHP website (`app/website_php_mysql/`)

The old site still ships alongside the new backend, so it was hardened too: the seeded
`admin / admin123password` account is gone, its session cookie is `HttpOnly` + `SameSite=Lax`
(+ `Secure` on HTTPS), DB credentials come from env / `config.secrets.php`, driver errors are
logged instead of printed, the downloads handler can no longer be walked out of the site root
or inject response headers, and the uploads folder is made non-executable.

To use the legacy panel, create the admin yourself:

```bash
php -r "echo password_hash('Your-Strong-Password', PASSWORD_BCRYPT), PHP_EOL;"
# then: INSERT INTO admins (username, password_hash, email) VALUES ('admin','<hash>','you@thestudent.lk');
```

---

## 6. Deliberate trade-offs / next steps

* `register` still answers `409` for an existing verified address (needed for a usable sign-up UX).
  Login and password reset are enumeration-resistant.
* OTP throttling is database-backed; for very large scale move it to Redis or the edge (Cloudflare WAF).
* Consider adding: e-mail change flow, TOTP/2FA for admins, refresh-token rotation for very long
  sessions, and `Strict-Transport-Security` at the web server.
* Password reset tokens are single-use and expire in 15 minutes; they are stored hashed like sessions.
