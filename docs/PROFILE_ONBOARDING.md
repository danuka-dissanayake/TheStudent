# Mandatory profile setup (phone, school, photo, password)

Why this exists: an email sign-up gives us a name, an email and a password; a
Google sign-in gives us a name, an email and a picture. Neither gives us a phone
number, a school/stream, or a usable password for Google-only accounts. Before
this step, students could reach the dashboard with an empty profile, which broke
personalisation (papers by stream, progress, certificates).

Now **no account can reach the dashboard with an incomplete profile**.

---

## 1. The flow

```
Splash → Onboarding → Auth ──(success, profile_completed=false)──► ProfileSetup ──► Main
                              │                                        ▲
                              └──(success, profile_completed=true)─────┘
                                                                        │
                        every cold start: GET ?action=me ───────────────┘
                        (profile_completed=false → back to setup)
```

* `AppScreenFlow.PROFILE_SETUP` sits **between** `AUTH` and `MAIN`
  (`TheStudentViewModel`), so there is no route that skips it.
* The system back gesture is consumed on that screen; the only exit is
  **Sign out** in the top-right.
* The server is the source of truth: `users.profile_completed_at` is `NULL`
  until `?action=complete_profile` succeeds.

## 2. What is collected

| Field | Rule (server) | Notes |
|---|---|---|
| `full_name` | 3–80 chars, letters/space/`.`/`'`/`-`, tags stripped, control chars removed | pre-filled from Google/e-mail, still editable |
| `phone` | `0712345678`, `071 234 5678`, `+94712345678`, `94712345678` → stored **E.164** `+94712345678` | other countries accepted only in full `+` form, 8–15 digits |
| `target_exam` | one of `G.C.E. O/L`, `G.C.E. A/L`, `University`, `Professional` | whitelist, not free text |
| `institution` | 2–120 chars | school / university |
| `stream_or_degree` | 2–150 chars | chips + free text |
| `password` | required **only** when the account has no password (Google-only) | never changes an existing password — that is `?action=change_password` |
| `avatar` | JPG/PNG/WEBP, 64–6000 px, ≤ 24 MP, ≤ 5 MB | re-encoded to JPEG ≤ 1024 px |

The photo is required if the account has no picture yet (a Google avatar counts).

**Nothing is pre-filled.** Only the full name is seeded (from Google / the e-mail
address). Phone, curriculum, institution and stream start empty so their
placeholders are visible and the student makes a real choice — an unset value is
never rendered as invented text such as "Physical Science" or a literal `null`.
`UserProfile` defaults are empty strings for the same reason, and
`TheStudentAuthClient` maps JSON `null` (which `org.json` would otherwise turn
into the string `"null"`) to `""`.

The empty avatar shows "Upload your profile pic" **inside** the disc, so the call
to action occupies the space that will hold the photo.

## 3. API

```
POST https://app.quizontal.site/api/auth.php?action=complete_profile
Authorization: Bearer <session token>
Content-Type: multipart/form-data        (or application/json without a photo)

full_name=Danuka Dissanayake
phone=0712345678
target_exam=G.C.E. A/L
institution=Ananda College
stream_or_degree=Physical Science
password=…            # Google-only accounts
avatar=@photo.jpg     # optional if one is already stored
```

Success:

```json
{
  "status": "success",
  "message": "Profile complete. Welcome to TheStudent!",
  "user": {
    "id": 7, "name": "Danuka Dissanayake", "email": "…",
    "phone": "+94712345678",
    "avatar_url": "https://app.quizontal.site/uploads/avatars/9f2c….jpg",
    "profile_completed": true,
    "missing_profile_fields": [],
    "has_password": true,
    "target_exam": "G.C.E. A/L",
    "institution": "Ananda College",
    "stream_or_degree": "Physical Science",
    "role": "student", "auth_provider": "email"
  }
}
```

`?action=me` returns the same `user` object, which is how the app decides whether
to show the setup screen after a restart.

Test with curl:

```bash
TOKEN=…   # from a login/verify_otp response

curl -i -X POST 'https://app.quizontal.site/api/auth.php?action=complete_profile' \
  -H "Authorization: Bearer $TOKEN" \
  -F 'full_name=Test Student' \
  -F 'phone=0712345678' \
  -F 'target_exam=G.C.E. A/L' \
  -F 'institution=Ananda College' \
  -F 'stream_or_degree=Physical Science' \
  -F 'avatar=@/path/to/photo.jpg'
```

## 4. Upload hardening (why an attacker cannot own the server)

`backend/api/_avatar.php` is the only code that writes uploads. In order:

1. **PHP upload error codes** — partial uploads, `UPLOAD_ERR_INI_SIZE`, etc.
2. **Size** — > 0 bytes and ≤ 5 MB (`TS_AVATAR_MAX_BYTES`).
3. **Origin** — `is_uploaded_file()`, so a path like `/etc/passwd` (or any LFI
   trick) is not a file we will read.
4. **Real content** — `getimagesize()` must succeed and the type must be JPEG,
   PNG or WEBP. The filename and the browser's `Content-Type` are never trusted.
5. **MIME cross-check** — `finfo` must agree when the extension exists.
6. **Dimensions** — 64–6000 px and ≤ 24 MP, which stops decompression bombs
   (`decode` would otherwise eat all RAM).
7. **Re-encode with GD** — decode → downscale to 1024 px → `imagejpeg()` at
   quality 88. This destroys EXIF (including GPS), ICC payloads and any appended
   PHP such as `GIF89a;<?php system($_GET['c']); ?>`. Only pixel data survives.
   If GD is missing the bytes are moved with an extension derived from the
   *detected* type — never from the client name.
8. **Random name** — `bin2hex(random_bytes(16)).jpg`; the client filename is
   discarded, so `../../config.php` and `shell.php.jpg` are impossible.
9. **Non-executable folder** — `uploads/avatars/.htaccess` denies `php|phtml|pl|
   py|jsp|asp|sh|cgi` and disables directory listing. It is written
   automatically if missing.
10. **Cleanup** — the previous avatar is unlinked only after the DB row points at
    the new one, and only if it matches our own random-name pattern inside the
    avatar directory. Deleting the account deletes the photo.

SQL injection: every value goes through `PDO::prepare()` with named parameters.
The only dynamic SQL is the optional `, profile_picture = :avatar` fragment,
which is chosen by code, never from input. Column/table names are never taken
from the request.

XSS / HTML injection: `strip_tags()`, control characters removed, `<>` rejected,
length limits applied, and `json_encode` with `JSON_UNESCAPED_*` flags — the app
renders the value as text, and the admin panel escapes its output.

Client-side, `AvatarPreparer` decodes with `BitmapFactory` (a renamed script
simply fails), downscales to ≤ 1024 px, flattens alpha and re-encodes as JPEG so
uploads are normally 40–200 KB instead of 8 MB.

## 5. Deploy (DirectAdmin, GUI only)

1. Upload/replace in `public_html`:
   `api/auth.php`, `api/_avatar.php`, `api/health.php`, `config.php`,
   `database_auth.sql` (for reference).
2. Create `public_html/uploads/avatars/` if it does not exist, and upload
   `uploads/avatars/.htaccess` into it. Permission 755 for the folder, 644 for
   the file.
3. No SQL import is needed — `auth.php` adds `users.phone` and
   `users.profile_completed_at` on the first request.
4. Check `api/health.php?token=…`:

```json
"uploads": {
  "avatars": { "exists": true, "writable": true, "htaccess": true, "gd": true }
}
```

`gd: false` still works (the raw-file fallback), but re-encoding is stronger —
ask the host to enable the GD extension.

5. `https://app.quizontal.site/api/auth.php?action=version` should report
   `"api_version": "3.2.0"`.

## 6. Rollout order (important)

Deploy the backend **before** shipping the new APK: an old app ignores
`profile_completed` and keeps working, while a new app against an old backend
would see no `profile_completed` flag and treat the profile as complete (the
client defaults to `true` when the field is absent).

## 7. Android files that make this work

| File | Role |
|---|---|
| `ui/screens/ProfileSetupScreen.kt` | the screen (photo picker, validation, progress, sticky CTA) |
| `data/remote/AvatarPreparer.kt` | decode/validate/downscale/re-encode before upload |
| `data/remote/TheStudentAuthClient.kt` | `completeProfile()` multipart call + new profile fields |
| `data/model/TheStudentModels.kt` | `UserProfile.phone/avatarUrl/profileCompleted/hasPassword`, `ProfileSetupSubmission` |
| `viewmodel/TheStudentViewModel.kt` | `PROFILE_SETUP` flow, `submitProfileSetup()`, cold-start gate |
| `data/local/TheStudentEntities.kt` + `TheStudentDatabase.kt` + repository | cache the new fields (Room v2 migration) |
| `MainActivity.kt` | route + back-gesture block |
| `ui/screens/ProfileScreen.kt` | shows the uploaded photo |

## 8. Branding

The header uses the real artwork, `res/drawable-nodpi/thestudent_logo_full.png`
(generated from the repository-root `TheStudent_logo_2.png`, 1959×541, trimmed and
resized to 1080 px wide). The logo already contains the wordmark and the
"Everything a Student Needs" tagline, so the screen draws **no text or icon**
beside it — just the image at a fixed height with `ContentScale.Fit`.
`TheStudentLogo` (the old icon + typed text composable) is untouched and still
used by the other screens.

## 9. Layout rules for this screen

* One column owns the horizontal padding (16 dp) and the vertical rhythm
  (14 dp via `Arrangement.spacedBy`); cards never add outer margins.
* No `animateContentSize` on cards and no height-animating `Spacer` between
  sections — animating a card's height inside a scrollable column makes the next
  section overlap for a frame, which is what caused the dirty edges.
* The error ring on the avatar is drawn inside the disc so it can never paint
  over the camera badge.
* The **Finish setup** button is the last item *inside* the scroll container, so
  it sits under the "Where you study" card and is only reachable by scrolling.
  It is not pinned to the screen bottom and never floats over the cards; the
  scroll content ends with a navigation-bar inset so the button clears the
  gesture bar when you reach it.
