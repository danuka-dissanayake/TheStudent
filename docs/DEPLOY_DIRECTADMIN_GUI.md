# Deploying TheStudent backend on DirectAdmin — **GUI only, no SSH**

Everything below is done with clicks in the DirectAdmin panel and the browser.
No terminal, no `php -r`, no `chmod` command.

**Your setup**

```
Home  /home/USER/
└── domains/
    └── app.quizontal.site/
        ├── config.secrets.php      ← WE CREATE THIS (outside the web root)
        └── public_html/            ← the website document root = https://app.quizontal.site/
            ├── config.php
            ├── smtp_mailer.php
            ├── api/
            ├── admin/
            ├── uploads/
            └── legal/
```

The panel skin is called **Evolution** on most DirectAdmin hosts. Menu names can differ
slightly — the folder **File Manager**, **MySQL Management** and **phpMyAdmin** are the only
three screens you need.

---

## Step 0 — Do these first (they are bleeding right now)

These three credentials were committed to GitHub. Changing the files does not un-leak them;
you must change them **at the source**. All three are GUI-only.

### 0.1 MySQL password

1. DirectAdmin → **Account Manager → MySQL Management** (sometimes **Databases → MySQL Management**).
2. Click the database **`quizontalcloud_app`** (or whatever yours is called).
3. Find the user row → **Modify Password** (or the key icon) → type a new strong password → **Save**.
4. Copy it somewhere safe for a minute — you need it in Step 2.

> Use a password with **no `$` or `'` characters** (e.g. 24+ letters/numbers/symbols like
> `K7mQ2xR9tLp4Wz8nBv3cFd6h`). `$` and `'` inside a PHP string need escaping and are the #1
> cause of "my config file looks right but the DB fails".

### 0.2 Gmail app password

1. Sign in as the sender account (the one used in `SMTP_USER`).
2. <https://myaccount.google.com/security> → **2-Step Verification** must be **on**.
3. <https://myaccount.google.com/apppasswords> → **revoke** the old TheStudent entry.
4. Create a new one (name it `TheStudent SMTP`) → copy the **16-character** code.
5. Also open <https://myaccount.google.com/permissions> and remove anything you don't recognise.

> If `apppasswords` says "not available", 2-Step Verification is off — turn it on first.

### 0.3 Hugging Face token

1. <https://huggingface.co/settings/tokens>
2. **Delete** the token starting with `hf_aOQ…`.
3. **Create new token** → type **Read** → copy it (used in the app's `.env`, Step 7).

---

## Step 1 — Create the secrets file (File Manager)

DirectAdmin → **System Info & Files → File Manager**.

1. In the path/URL bar at the top, navigate to:
   `domains/app.quizontal.site`
   (you should see the `public_html` folder in the list — that means you are one level **above**
   the web root, which is exactly where we want the file).
2. Click **+ New File** (or **Create File**).
3. Name it exactly: `config.secrets.php` → confirm.
   Ignore any warning about an unusual extension.
4. Select the new file → **Set Permissions** (right-click → *Permissions* / *Change Permissions*)
   → set to **600** → Save.

### 1.1 Generate `APP_KEY` without a terminal

This produces 64 random hex characters **on your own machine** — nothing is sent to any website.

1. Open <https://app.quizontal.site> in Chrome/Edge (any page).
2. Press **F12** → **Console** tab.
3. Paste this and press Enter:

```js
Array.from(crypto.getRandomValues(new Uint8Array(32))).map(b=>b.toString(16).padStart(2,'0')).join('')
```

4. Copy the 64-character result. That is your `APP_KEY`.

> Chrome may say "Warning: don't paste code you don't understand" — type `allow pasting` and press
> Enter if it blocks you. This code only generates random bytes locally.
>
> **Fallback** if the console is blocked: create `keygen.php` in `public_html`, put this in it,
> open `https://app.quizontal.site/keygen.php`, copy the key, then **delete `keygen.php`
> immediately**:
> ```php
> <?php echo bin2hex(random_bytes(32));
> ```

### 1.2 Paste the full file

Still in File Manager: select `config.secrets.php` → **Edit** → paste this, replacing the values in
`<<< >>>`:

```php
<?php
/**
 * TheStudent - REAL secrets. Never commit this file.
 * Location: /home/USER/domains/app.quizontal.site/config.secrets.php
 */

declare(strict_types=1);

define('APP_ENV', 'production');
define('APP_KEY', '<<<PASTE THE 64 HEX CHARS>>>');

// --- MySQL (from Step 0.1) ---------------------------------------------------
define('DB_HOST', 'localhost');
define('DB_NAME', 'quizontalcloud_app');
define('DB_USER', 'quizontalcloud_app');
define('DB_PASS', '<<<NEW DB PASSWORD>>>');
define('DB_CHARSET', 'utf8mb4');

// --- Google Sign-In ----------------------------------------------------------
// Must be byte-identical to GOOGLE_WEB_CLIENT_ID in the app's .env file.
define('GOOGLE_WEB_CLIENT_ID', '<<<xxxx-xxxx.apps.googleusercontent.com>>>');

// --- Who is an administrator -------------------------------------------------
// Comma separated. These accounts get role=admin after verifying their email.
define('ADMIN_EMAILS', '<<<your-email@example.com>>>');

// --- SMTP (Gmail app password from Step 0.2) ---------------------------------
define('SMTP_HOST', 'ssl://smtp.gmail.com');
define('SMTP_PORT', 465);
define('SMTP_USER', '<<<sender@gmail.com>>>');
define('SMTP_PASS', '<<<16 CHAR APP PASSWORD, NO SPACES>>>');
define('SMTP_FROM_NAME', 'TheStudent Security');

// --- HTTP --------------------------------------------------------------------
define('ALLOWED_ORIGINS', 'https://app.quizontal.site');
define('REQUIRE_HTTPS', '1');

// Leave 0 unless the server sits behind Cloudflare / a reverse proxy.
define('TRUST_PROXY_HEADERS', '0');
define('TRUSTED_PROXIES', '');
```

Click **Save**.

**Do not add a closing `?>`** — it causes "headers already sent" errors.

### 1.3 Verify it is not reachable

Open in a **new private window**:

```
https://app.quizontal.site/config.secrets.php
```

* ✅ `403 Forbidden`, `404 Not Found`, or a blank page → good.
* ❌ You see PHP source code → **stop**, the file is in the wrong place or PHP isn't running.
* ❌ You see the *login page / homepage* → the file is inside `public_html` and being processed as
  a page; move it one level up.

---

## Step 2 — Upload the backend files

The GitHub repo is local to you; the host has no git. Use **File Manager upload** (or FTP).

### 2.1 Prepare a zip on your computer

1. Open the repo folder.
2. Go into `backend/`.
3. Select **everything inside it** (`api`, `admin`, `legal`, `uploads`, `config.php`,
   `config.secrets.example.php`, `error_handlers.php`, `smtp_mailer.php`) and compress it to
   `backend-upload.zip`.

   > `error_handlers.php` and `api/health.php` are **required** — without the first, errors come
   > back as an empty `500` you cannot debug; without the second you have no way to see the
   > server error log over the web.

   > Updating an existing install? Re-upload at minimum: `config.php`, `error_handlers.php`,
   > `api/auth.php`, `api/health.php`, `api/_avatar.php` (profile-photo uploads), and make sure
   > `uploads/avatars/.htaccess` exists. Then confirm with
   > `https://app.quizontal.site/api/auth.php?action=version` (see `docs/DEBUGGING.md`).

**Do not** include `database.sql`, `database_auth.sql` or `README_INFINITYFREE.md` — they don't
need to be on the web server at all.

### 2.2 Upload + extract

1. DirectAdmin → **File Manager** → open **`public_html`**.
2. Click **Upload files** → choose `backend-upload.zip` → wait for 100%.
3. Select the uploaded zip → **Extract** (or *Unarchive*) → confirm.
4. Delete the zip afterwards.
5. Confirm the structure looks like this (no extra `backend/` folder inside `public_html`):

```
public_html/
├── config.php
├── config.secrets.example.php
├── error_handlers.php
├── smtp_mailer.php
├── api/       (auth.php, health.php, get_papers.php, upload_paper.php,
│               delete_paper.php, _admin_guard.php)
├── admin/     (index.php, _guard.php)
├── legal/     (terms.html, privacy.html)
└── uploads/   (.htaccess, avatars/ for profile photos, + the paper PDFs)
```

### 2.3 Permissions

In File Manager, select a folder → **Set Permissions**:

| Item | Permissions |
|---|---|
| Folders (`api`, `admin`, `legal`, `uploads`) | **755** |
| `.php` / `.html` files | **644** |
| `uploads/` contents | 644 |
| the secrets file (Step 1) | **600** |

### 2.4 Do **not** overwrite the root `.htaccess`

If `public_html` already has an `.htaccess`, leave it alone. Our secret-protection rules are
shipped as `backend/.htaccess`, but because the secrets file now lives **above** `public_html`
they are optional. If you want them anyway, open the existing `.htaccess` → **Edit** → paste the
contents of `backend/.htaccess` **at the bottom** → Save.

> DirectAdmin's File Manager hides dotfiles by default. Click the **settings / "show hidden
> files"** toggle (an eye icon, or *Options → Show hidden files*) to see `.htaccess`.

---

## Step 3 — Database (phpMyAdmin)

1. DirectAdmin → **Databases → phpMyAdmin** (or *MySQL Management → phpMyAdmin*).
2. In the left sidebar click your database (`quizontalcloud_app`).
3. **Import** tab → **Choose File** → pick `database_auth.sql` from your computer →
   **Import** (bottom right).
   * It only creates tables that are missing, so running it twice is safe.
4. Your existing tables (`users`, `papers`, …) stay as they are.

### 3.1 Make yourself an administrator

`api/auth.php` also creates the new columns/tables on its own the first time it runs, so the import
is a convenience. The one thing you **must** do:

1. Go to the **SQL** tab and run (replace the e-mail):

```sql
UPDATE users SET role = 'admin' WHERE email = 'your-email@example.com';
```

2. Check it worked: **Browse** the `users` table → your row → `role` = `admin`.

> There is no default admin login any more. `ADMIN_EMAILS` in the secrets file also grants the role
> automatically the first time that address registers, then keeps it in sync.

---

## Step 4 — Smoke test (just your browser)

Open each URL. You don't need curl — the browser shows the JSON.

| Open this | Expected |
|---|---|
| `https://app.quizontal.site/api/auth.php?action=me` | `{"status":"error","message":"Authentication required."}` and HTTP 401 |
| `https://app.quizontal.site/api/get_papers.php` | JSON list of papers |
| `https://app.quizontal.site/legal/terms.html` | the Terms page |
| `https://app.quizontal.site/legal/privacy.html` | the Privacy page |
| `https://app.quizontal.site/api/auth.php?action=nope` | `{"status":"error","message":"Invalid API action."}` |
| `https://app.quizontal.site/config.secrets.php` | 403 / 404, never source code |

**Reading the failures** (a `500` instead of JSON? see `docs/DEBUGGING.md`, or open
`https://app.quizontal.site/api/health.php?token=…` after setting `DIAG_TOKEN` in
`config.secrets.php` — it names the exact missing table/column)

| What you see | Meaning | Fix |
|---|---|---|
| `503` + "Missing configuration: APP_KEY…" | secrets file not found or `APP_KEY` too short | Step 1 — check location (`domains/app.quizontal.site/`), name, and that `APP_KEY` is 64 chars |
| `500` + white page | PHP error | DirectAdmin → **Advanced Features → Error Logs** (or `error_log` in `public_html`) |
| `{"status":"error","message":"The service is temporarily unavailable"}` | DB connect failed | wrong `DB_USER`/`DB_PASS`, or the DB user isn't attached to the database (MySQL Management → *Add user to database*) |
| `400 Secure connection required.` | HTTPS not detected | normally Cloudflare: set `TRUST_PROXY_HEADERS` to `1` **and** `TRUSTED_PROXIES` to the proxy IPs |
| `404` on `/api/auth.php` | files uploaded into a subfolder | re-check Step 2.2 tree |
| e-mails never arrive | SMTP blocked/credentials | `SMTP_PASS` must be the 16-char app password with **no spaces**; check the error log; test at Step 5 |

---

## Step 5 — Test the real flows

Use the app (or the `curl` in `docs/AUTH_SECURITY.md` if you ever get shell access):

1. **Register** with a fresh e-mail → you should receive a 6-digit code within ~10 s.
2. Enter the code → you land in the app.
3. **Sign out** (drawer → Sign out) → sign back in.
4. **Forgot password** → code → new password → sign in with the new one.
5. Signed in → drawer → **Account & Security** → change password → you stay signed in with a new
   session; other devices are logged out.
6. `https://app.quizontal.site/admin/` → sign in with your **admin** account (`role = 'admin'`) →
   the console loads. A normal student account must be refused.

---

## Step 6 — Legal pages

Both files are already in `backend/legal/` and were uploaded in Step 2.2.

1. Open `https://app.quizontal.site/legal/terms.html` and `.../privacy.html`.
2. Replace every `[square-bracket]` placeholder (business name, address, support/privacy e-mail).
   * Fastest GUI way: File Manager → select the file → **Edit** → edit → Save.
3. If your domain is **not** `app.quizontal.site`, change the two links in the app:
   `TERMS_URL` / `PRIVACY_URL` at the top of
   `app/src/main/java/com/example/ui/screens/AuthScreen.kt`.

---

## Step 7 — The Android app side (on your PC)

The `.env` file is **not** uploaded anywhere — it is read by Gradle when you build the APK.

1. Open the project in **Android Studio**.
2. In the project tree, right-click the **root folder** → **New → File** → name it `.env`.
3. Paste (values from Steps 0.2/0.3 and the Google console):

```properties
GEMINI_API_KEY=...
ZAI_API_KEY=...
Z_AI_API_KEY=...
HF_API_TOKEN=hf_...
GOOGLE_WEB_CLIENT_ID=849765952531-7bnroph62m0homvnpfa3nngkmcorb4mf.apps.googleusercontent.com
```

4. **Build → Clean Project**, then **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
   (BuildConfig is generated at build time — a plain reinstall does **nothing**.)
5. `GOOGLE_WEB_CLIENT_ID` here and in `config.secrets.php` must be **identical**, or Google
   sign-in fails with `Token audience mismatch`.

### 7.1 Google Cloud Console (GUI)

1. <https://console.cloud.google.com/apis/credentials> → your **Web application** OAuth client.
2. **Authorised JavaScript origins**: `https://app.quizontal.site`
3. **Authorised redirect URIs**: `https://app.quizontal.site/`
4. For the Android client: add package name `com.thestudent.sl` and your release **SHA-1**
   (Android Studio → Gradle panel → *android → signingReport*, or `keytool` output).
5. If you ever generated a **client secret** for this app, rotate it — it should not be used by an
   Android app at all.

### 7.2 Enable GitHub leak protection (GUI, replaces a pre-commit hook)

Because you can't run the `scripts/check-secrets.sh` hook, use GitHub's built-in guard:

1. GitHub → your repo → **Settings** → **Code security and analysis**.
2. Enable **Secret scanning** and **Push protection**.
3. From then on GitHub blocks a push that contains a recognised key.

Also add the same list of keys under **Settings → Secrets and variables → Actions** if you ever
build in CI.

---

## Step 8 — Backups (do this once, now)

File Manager → select `public_html` → **Backup / Compress** → download the zip.
DirectAdmin → **Advanced Features → Create/Restore Backups** → also download a full account backup.
Keep the previous `config.php` / `api/auth.php` copies so you can roll back by re-uploading.

> Before any future deploy: download the current files, *then* upload. File Manager has no undo.

---

## Quick reference — what changed vs the old deploy

| Old | Now |
|---|---|
| Credentials typed into `config.php` | `config.secrets.php` one level above `public_html` |
| `.zip` of the backend committed to git | deleted; `*.zip` ignored |
| `admin@gmail.com` / `admin123` bypass | gone; `role='admin'` in the DB, no default account |
| Unauthenticated `admin/` + upload/delete APIs | need an admin session / Bearer token |
| OTP + session tokens in plain text | hashed; every old session/OTP stops working on deploy |
| `backend_directadmin.zip` on your laptop | delete it — it contains the old leaked passwords |

**After deploying**: every user is signed out once (tokens are hashed now) and any pending
verification codes are invalid. That is expected — they just sign in again.
