# Debugging a failing API call (500 / "Request failed (500)")

You have three windows onto the problem. Use them in this order — **(1)** tells you what the
server thinks is wrong without any guessing, **(2)** tells you what the app saw, and **(3)**
catches everything else.

---

## 0. First: is my upload actually live?

```
https://app.quizontal.site/api/auth.php?action=version
```

```json
{ "status": "success", "api_version": "3.1.0", "php_version": "8.1.x",
  "features": { "fatal_handler": true, "schema_selfheal": true,
                "secrets_outside_webroot": true },
  "auth_php_modified": "2026-10-27T12:10:00+00:00" }
```

* `404` / "Invalid API action" → the **old** `auth.php` is still on the server (upload did not
  take effect: wrong folder, FTP failed, or a different docroot).
* `fatal_handler: false` → the **old** `config.php` / missing `error_handlers.php` is deployed.
* `secrets_outside_webroot: false` → `config.secrets.php` is still inside `public_html`.

Checking this first saves an hour of chasing a bug that is really a stale file.

### If `health.php` answers 404

It now says **why** in the body (and in the `X-Diag-Status` header):

| `code` | Meaning | Fix |
|---|---|---|
| `token_not_configured` | `DIAG_TOKEN` is empty or undefined | add `define('DIAG_TOKEN', '32-random-chars');` to **`config.secrets.php`** — the one *above* `public_html`, not `config.secrets.example.php` |
| `token_too_short` | fewer than 24 characters | use 32+ letters/digits, no quotes or spaces |
| `token_mismatch` | the URL token ≠ the server value (the message shows both lengths) | copy the value exactly; the length tells you it is a different string |
| plain HTML "404 Not Found" (no JSON) | `health.php` is **not on the server** | upload `api/health.php` |

Cross-check with the ungated endpoint: `api/auth.php?action=version` reports
`diag_token_length` — `0` means PHP does not see your `DIAG_TOKEN` at all.

## 1. The self-check endpoint (no SSH, 30 seconds)

Add a diagnostic token to `config.secrets.php` (File Manager → Edit), then open the URL:

```php
define('DIAG_TOKEN', 'paste-any-32-random-characters-here');
```

```
https://app.quizontal.site/api/health.php?token=paste-any-32-random-characters-here
```

It answers with a JSON report:

* **`error_log.last_lines`** — the tail of `php-error.log`, including the
  `[TheStudent][fatal][<ref>]` line that matches the reference the API returned.
  A `dir_writable: false` here means logging is impossible and you must fix
  `ERROR_LOG_PATH`.
* **`signup_dry_run`** — the exact queries `register` runs, in order, against a throw-away
  address. The first entry that says `FAILED: …` is your 500. Example:
  `"1. rate-limit read (login_attempts)": "FAILED: SQLSTATE[42S02]: Base table or view not found: 1146 Table 'db.login_attempts' doesn't exist"`
* PHP version + loaded extensions (`pdo_mysql`, `openssl`, `curl`…)
* where `config.secrets.php` was found, and whether `APP_KEY` is long enough
* DB connection result — **including the exact MySQL error**
* **which tables and columns are missing** ← almost always the 500 you are chasing
* whether the host lets PHP create tables
* `uploads/` writability
* **the last 25 lines of the PHP error log**
* a `problems` array with plain-English causes, and `"ok": true/false`

Set `DIAG_TOKEN` back to `''` when you are done so the endpoint answers 404 again.

---

## 2. Watch the app in real time (USB)

```bash
adb logcat -c        # clear the buffer
adb logcat -s TheStudentAuthClient:V TheStudentViewModel:V AndroidRuntime:E OkHttp:V
```

Leave it running, then tap **Register** in the app. You will see one of:

```
E TheStudentAuthClient: Non-JSON error response: HTTP 500 from https://…/api/auth.php?action=register body=<empty>
```

That means PHP died before it could reply. Since the new `config.php` installs a fatal-error
handler, a 500 from the API now also returns JSON like:

```json
{"status":"error","message":"The server hit an unexpected error. Reference: 3f9ac1d2","ref":"3f9ac1d2"}
```

The same `[TheStudent][fatal][3f9ac1d2]` prefix is written to the error log — search for it in
`php-error.log` (step 3) and you get the file, line and message.

**Useful variants**

```bash
# everything, then grep as it streams
adb logcat | grep -iE "thestudent|fatal|exception|error"

# save a session to a file and share it
adb logcat -d > device.log

# only crashes
adb logcat -b crash
```

A **client-side** network failure looks different and is not a server bug:

```
E TheStudentAuthClient: Non-JSON error response: HTTP 404 …     → wrong URL / files not uploaded
```

---

## 3. The server error log (File Manager, GUI)

`config.php` now writes PHP errors to a file **outside** the web root:

```
/home/USER/domains/app.quizontal.site/php-error.log
```

DirectAdmin → **File Manager** → open `domains/app.quizontal.site` → right-click
`php-error.log` → **Edit** (or **View**). Newest lines are at the bottom.

DirectAdmin also keeps the host-level log: **Advanced Features → Error Logs** (or
*System Info & Files → Logs*).

If your host ignores `ini_set('error_log', …)`, override it explicitly in `config.secrets.php`:

```php
define('ERROR_LOG_PATH', '/home/USER/domains/app.quizontal.site/php-error.log');
```

---

## 4. Reproduce it from your PC (fastest loop)

You do not need the phone to debug the server. Run this in a terminal on your computer:

```bash
curl -i -X POST 'https://app.quizontal.site/api/auth.php?action=register' \
  -H 'Content-Type: application/json' \
  -d '{"full_name":"Test Student","email":"test+'"$(date +%s)"'@example.com","password":"TestPass123"}'
```

* `-i` shows the status line **and** the body — that is the whole point.
* Use a new e-mail each run (`test+…@`), otherwise the second attempt correctly answers
  `409` / `requires_verification`.

Then check the log:

```bash
# if your host exposes the log over the diagnostic endpoint, this prints it
curl -s 'https://app.quizontal.site/api/health.php?token=YOUR_DIAG_TOKEN' | tail -40
```

---

## 5. What a 500 on **sign-up** usually means

| Cause | How to confirm | Fix |
|---|---|---|
| **Missing auth tables** (`login_attempts`, `email_verifications`, `user_tokens`) — the DB was built from the old `database.sql` and `database_auth.sql` was never imported | `health.php` → `database.tables_missing` | The new `auth.php` creates them on the next request. If `can_create_tables` is `false`, import `database_auth.sql` in phpMyAdmin |
| **Old `users` table** without `role` / `is_active` / `is_email_verified` | `health.php` → `database.columns` | Same: the code adds the columns; otherwise import the SQL |
| Wrong DB credentials | `health.php` → `database.error` (`Access denied for user …`) | Re-check `DB_USER` / `DB_PASS`, and that the user is **attached** to the database in *MySQL Management* |
| `APP_KEY` missing | `health.php` → `config.app_key_length` = 0 | Set 64 hex chars |
| PHP 7.3 or older | `health.php` → `php.version` | Switch the domain to PHP 8.1+ in DirectAdmin → *Domain Setup → PHP Version* |
| Server can't send mail | `health.php` → `problems` mentions `SMTP_PASS` | That gives a `503`, not a `500`, but fix it too |
| `.htaccess` 500 (syntax error) | every URL on the site is 500, `php-error.log` is empty | Check *Advanced Features → Error Logs*; remove the last `.htaccess` edit |

---

### Sign-up no longer dies silently

* A missing `login_attempts` table now only disables rate limiting (logged, request continues).
* A failure to **store** the OTP answers
  `503 {"status":"error","message":"Verification is temporarily unavailable. Reference: <ref>","ref":"<ref>"}`
  instead of a blank 500 — search that ref in `php-error.log` (or in `health.php`) for the SQL error.
* Anything still fatal returns `{"status":"error", "ref": "<8 hex>"}` plus the header
  `X-TheStudent-Error-Ref`, and the same ref is written to the log.

## 6. The `.htaccess` trap

If **every** request (even `https://app.quizontal.site/`) returns 500 while `health.php` is
unreachable, the web server itself is failing, not PHP. That is almost always an `.htaccess`
directive your host does not allow (`Options -Indexes`, `Require all denied` on old Apache).
Comment the block out with `#`, save, and reload.
