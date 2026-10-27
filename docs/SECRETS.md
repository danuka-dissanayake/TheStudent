# TheStudent — Secrets & Configuration Guide

Everything sensitive in this repo lives in **two files, one per side**, and neither of them is
in git:

| Where | File | Used by | Format |
|---|---|---|---|
| Repo root | **`.env`** | the Android app (build time) | `KEY=value` |
| `backend/` | **`config.secrets.php`** | the PHP backend (runtime) | `define('KEY', 'value');` |

They are separated because they ship to different places: `.env` is compiled into the APK,
`config.secrets.php` stays on the server. A value that must exist on **both** sides (the Google
Web client ID) is written in both, and the two must match exactly.

Templates that *are* committed: `.env.example` and `backend/config.secrets.example.php`.

> **Rule of thumb:** if a value would cost you money, access, or reputation when a stranger reads
> it, it is a secret. Session tokens, OTPs and password hashes belong in the database — never in
> a config file.

---

## 1. Android app — `.env`

### 1.1 Create it

```bash
cd /path/to/TheStudent
cp .env.example .env
chmod 600 .env          # optional but tidy on a shared machine
$EDITOR .env            # fill in the real values
```

`.env` is already in `.gitignore`. Verify:

```bash
git check-ignore -v .env      # must print a .gitignore:…:.env line
```

### 1.2 How the values reach the code

`app/build.gradle.kts`:

```kotlin
secrets {
  propertiesFileName          = ".env"           // your real values
  defaultPropertiesFileName   = ".env.example"   // fallback so a fresh clone still builds
}
```

The Secrets Gradle Plugin turns each line into a `BuildConfig` constant at **build time**:

```
GEMINI_API_KEY=abc123        →   BuildConfig.GEMINI_API_KEY
```

Kotlin reads them like this:

```kotlin
BuildConfig.GEMINI_API_KEY
BuildConfig.HF_API_TOKEN
BuildConfig.GOOGLE_WEB_CLIENT_ID
```

### 1.3 Keys

| Key | Needed for | Where to get it |
|---|---|---|
| `GEMINI_API_KEY` | AI tutor text/vision, OCR | <https://aistudio.google.com/apikey> |
| `ZAI_API_KEY` / `Z_AI_API_KEY` | GLM free-tier chat | <https://z.ai> → API keys |
| `HF_API_TOKEN` | GPT-OSS space + Hugging Face client | <https://huggingface.co/settings/tokens> (read scope) |
| `GOOGLE_WEB_CLIENT_ID` | Google Sign-In (must equal the backend value) | Google Cloud Console → Credentials → OAuth client ID → **Web application** |

### 1.4 Two things you must understand

1. **These are not really secret in the APK.** `BuildConfig` strings are plain constants inside
   the compiled app; anyone can `unzip app-release.apk` and read them. The `.env` route protects
   the **repository**, not the shipped binary.
   For anything that costs real money, the right pattern is: the app talks to *your* PHP backend,
   the backend holds the provider key and proxies the request. The Gemini/OpenAI/HF keys are
   currently app-side; move them behind `backend/api/` before you scale.
2. **Changing `.env` requires a rebuild.** It is read by Gradle, not at runtime. If you only
   reinstall the old APK, nothing changes.

### 1.5 CI / GitHub Actions

Do not commit `.env` there either. Add each key as a repository secret
(*Settings → Secrets and variables → Actions*) and generate the file in the workflow:

```yaml
- name: Write .env
  run: |
    cat > .env <<'EOF'
    GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}
    ZAI_API_KEY=${{ secrets.ZAI_API_KEY }}
    Z_AI_API_KEY=${{ secrets.Z_AI_API_KEY }}
    HF_API_TOKEN=${{ secrets.HF_API_TOKEN }}
    GOOGLE_WEB_CLIENT_ID=${{ secrets.GOOGLE_WEB_CLIENT_ID }}
    EOF
- run: ./gradlew assembleRelease
```

Release signing passwords are already read from environment variables in
`app/build.gradle.kts` (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) — set those the same
way, and keep the `.jks` outside the repo.

---

> **DirectAdmin hosting without SSH?** Every server-side step in this section has a click-through
> version in **`docs/DEPLOY_DIRECTADMIN_GUI.md`** (File Manager, phpMyAdmin, no terminal).

## 2. Backend — `backend/config.secrets.php`

### 2.1 Where it goes (first match wins, see `config.php`)

| Priority | Path | Web-reachable? |
|---|---|---|
| 1 (recommended) | `/home/USER/domains/app.quizontal.site/config.secrets.php` — one level **above** `public_html` | **no** |
| 2 | `/home/USER/domains/config.secrets.php` (two levels above) | no |
| 3 | `public_html/config.secrets.php` | yes — only safe because `.htaccess` denies it |

`config.php` logs a warning if it finds the file inside the web root.

### 2.2 Create it on the server (not in the repo)

```bash
cd /home/USER/domains/app.quizontal.site/public_html/backend
cp config.secrets.example.php config.secrets.php
chmod 600 config.secrets.php
php -r "echo bin2hex(random_bytes(32)), PHP_EOL;"      # → APP_KEY (64 hex chars)
```

`config.secrets.php` is git-ignored, and `backend/.htaccess` **denies web access** to it, to
`*.sql`, to `README*` and to dotfiles, so it cannot be downloaded even if PHP is misconfigured.

### 2.3 Fill in

| Constant | Notes |
|---|---|
| `APP_ENV` | `production` on the host. `local` disables the HTTPS requirement. |
| `APP_KEY` | 64 hex chars. HMAC key for OTP/reset codes. **Changing it invalidates pending codes.** |
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS` | DirectAdmin → MySQL Management. |
| `GOOGLE_WEB_CLIENT_ID` | Must be identical to the app's value, or `google_login` answers "Token audience mismatch". |
| `ADMIN_EMAILS` | Comma-separated. Grants the `admin` role on first verified sign-in. **This is the only way to get admin** — there is no default admin account any more. |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM_NAME` | Gmail app password (16 chars, no spaces) or any SMTP provider. Without it, no OTP e-mails are delivered. |
| `ALLOWED_ORIGINS` | Comma-separated browser origins for CORS. The Android app needs none. |
| `REQUIRE_HTTPS` | `1` (default). Set `0` only for local testing. |
| `TRUST_PROXY_HEADERS` | `1` **only** behind Cloudflare/a reverse proxy, otherwise clients can spoof IPs. |
| `TRUSTED_PROXIES` | Optional comma-separated proxy IPs. |
| `DIAG_TOKEN` | 24+ random chars to unlock `api/health.php?token=…` (read-only self-check: DB, tables, error log). Leave empty to disable. |

`config.php` fails closed: if `DB_NAME`, `DB_USER` or a ≥32-character `APP_KEY` is missing, every
API call returns `503` instead of running with an empty key.

### 2.4 Alternative: real environment variables

Every constant can also come from the environment, so you never need a file at all. The lookup
order per setting is: **`getenv(NAME)` → constant `THESTUDENT_NAME` → built-in default**.

```apache
# .htaccess or the DirectAdmin "Environment variables" panel
SetEnv DB_HOST       localhost
SetEnv DB_NAME       quizontalcloud_app
SetEnv DB_USER       quizontalcloud_app
SetEnv DB_PASS       <new-password>
SetEnv APP_KEY       <64-hex>
SetEnv SMTP_USER     you@yourdomain.com
SetEnv SMTP_PASS     <16-char-app-password>
SetEnv GOOGLE_WEB_CLIENT_ID xxxx.apps.googleusercontent.com
SetEnv ADMIN_EMAILS  you@example.com
```

Pick **one** mechanism per server. If both exist, the environment wins.

### 2.5 Verify

```bash
php -r "require 'config.php'; echo 'APP_KEY length: ', strlen(APP_KEY), PHP_EOL;"
curl -s 'https://app.quizontal.site/api/auth.php?action=me'   # → 401 Authentication required
```

A `503` here means something in §2.2 is still empty — check `error_log`, not the response body.

---

## 3. Rotate these **now** (they were committed in git history)

Deleting the file does not un-leak a value: it is still in old commits, in forks and in clones.
Change each one at its source, then update `.env` / `config.secrets.php`:

| Credential | Where it leaked | Rotate at |
|---|---|---|
| MySQL password for `quizontalcloud_app` | `backend_directadmin.zip`, old `backend/config.php` | DirectAdmin → MySQL Management → change password |
| Gmail app password | same, plus the old `TheStudentEmailSender.kt` in the APK | Google Account → Security → App passwords → **revoke**, create new |
| Hugging Face token `hf_aOQ…` | `TheStudentStudyAiClient.kt` | <https://huggingface.co/settings/tokens> → revoke, create new |
| `APP_KEY` | never published — but generate a fresh one for the first deploy | `php -r "echo bin2hex(random_bytes(32));"` |
| Google OAuth **client secret** (if you ever created one) | not in this repo — check the console anyway | Google Cloud Console → Credentials → reset |

After rotating, redeploy and restart PHP so no opcode cache holds the old file.

---

## 4. Adding a new feature — checklist

### Needs a third-party key in the app

1. Add `MY_KEY=…` to `.env.example` (committed, placeholder).
2. Add the real value to `.env` (never committed).
3. Use `BuildConfig.MY_KEY` in Kotlin — never a string literal.
4. Guard the "not configured" case (`BuildConfig.MY_KEY.isBlank()` → show a friendly message or
   fall back), because `.env.example` is what a fresh clone builds with.
5. Rebuild.

```kotlin
// ✅ correct
private val apiKey: String = runCatching { BuildConfig.MY_KEY.trim() }.getOrDefault("")

// ❌ never
private val apiKey = "sk-live-9f8a7b6c5d4e3f2a1b0c"
```

### Needs a credential on the server

1. Add `define('MY_SECRET', 'REPLACE_ME');` to `backend/config.secrets.example.php`.
2. Add `'MY_SECRET' => 'MY_SECRET',` to the `$__tsLegacyMap` in `backend/config.php`.
3. If the feature cannot work without it, add it to the `$__tsMissing` check so the deploy fails
   loudly instead of silently.
4. Read it with `MY_SECRET` — never inline a literal.

### Verify before you push

```bash
./scripts/check-secrets.sh --all        # scans every tracked file
./scripts/check-secrets.sh              # scans only what is staged
```

Install it as a pre-commit hook so a leak cannot reach GitHub:

```bash
ln -sf ../../scripts/check-secrets.sh .git/hooks/pre-commit
```

---

## 5. If you do commit a secret

1. **Rotate it first.** Assume it is already public; purging history does not un-copy it.
2. Remove it from the file and commit that.
3. Purge it from history (rewrites history — coordinate with anyone who clones the repo):

```bash
pip install git-filter-repo
git filter-repo --replace-text <(echo 'literal:THE-SECRET-VALUE==>REMOVED')
git push --force-with-lease origin <branch>
```

4. Check GitHub → *Security → Secret scanning alerts* and dismiss/revoke anything reported.

---

## 6. Current state of the repo

| Check | Status |
|---|---|
| Hardcoded Hugging Face token in `TheStudentStudyAiClient.kt` | **Removed** — now `.env` / `BuildConfig.HF_API_TOKEN`, no fallback |
| Google Web client ID in `TheStudentGoogleAuthClient.kt` | Moved to `BuildConfig.GOOGLE_WEB_CLIENT_ID` (public value, kept as last-resort default) |
| `backend_directadmin.zip` (contained DB + Gmail passwords and a pre-hardening `auth.php`) | **Deleted from the repo** and `*.zip` is ignored |
| Backend DB/SMTP/APP_KEY | `backend/config.secrets.php` (git-ignored) or environment variables |
| Legacy site `app/website_php_mysql/` | Same pattern via its own `config.secrets.php` |
| Session tokens / OTPs / password hashes | Database only, hashed (see `docs/AUTH_SECURITY.md`) |
| Pre-commit leak scan | `scripts/check-secrets.sh` |

Related reading: `docs/AUTH_SECURITY.md` (auth model + deploy runbook).
