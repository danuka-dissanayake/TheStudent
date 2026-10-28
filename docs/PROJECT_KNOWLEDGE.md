# TheStudent — Codebase Knowledge Base

_Last read: 2026-10-27 · branch `arena/e7cd0804-thestudent` (base `a9719da` on `feature/thestudent-v2`)_

TheStudent is an **all-in-one Sri Lankan e-learning Android app** (Jetpack Compose, single module `:app`) with a
companion **PHP/MySQL backend** (DirectAdmin shared hosting). It ships past papers, an AI study assistant with
photo/PDF OCR, an English voice tutor, a peer-chat UI, and an in-app admin panel for uploading papers.

---

## 1. At a glance

| Item | Value |
|---|---|
| Language / UI | Kotlin, 100 % Jetpack Compose (Material 3), no XML screens |
| Module | `:app` only, namespace `com.example`, `applicationId = com.thestudent.sl` |
| SDK | min 24 · target/compile 36 · versionName 1.0 (versionCode 1) |
| Architecture | Single `AndroidViewModel` + `StateFlow` UI state (no Hilt/Koin), Repository + Room + SharedPreferences |
| Size | ~30,900 lines of Kotlin across 62 files; biggest files: `AiAssistantScreen.kt` (4.5k), `AuthScreen.kt` (1.9k), `TheStudentViewModel.kt` (1.66k) |
| Backend | PHP 8 + PDO/MySQL (DirectAdmin), plus 2 legacy copies of the same API |
| Secrets | `secrets-gradle-plugin` reads `.env` → `BuildConfig` (`GEMINI_API_KEY`, `ZAI_API_KEY`, `Z_AI_API_KEY`, `HF_API_TOKEN`) |

Build is **not runnable in this sandbox** (no JDK / Android SDK present), so all of the below is from static reading.

---

## 2. App flow (`MainActivity` + `TheStudentUiState.screenFlow`)

```
SPLASH ──► ONBOARDING ──► AUTH ──► MAIN
   │            │            │        └─ bottom nav: HOME · CHAT · AI_TUTOR · PAPERS · PROFILE
   │            │            └─ AdminPanelScreen (hidden console, admin@gmail.com)
   └─ finishSplash() waits ≤2 s for remote session validation, then routes
```

- `SplashScreen` animates, then `viewModel.finishSplash()` decides: authenticated → `MAIN`, onboarded → `AUTH`, else `ONBOARDING`.
- `OnboardingScreen`: 3 marketing cards + Google sign-in + "sign up with email" + academic region picker.
- `AuthScreen`: Sign In / Sign Up / 6-digit OTP (`ModernPasscodeKeypad`), Google via Credential Manager, 950 ms skeleton shimmer between modes.
- **Guest mode is deprecated** — `continueAsGuest()` just opens the auth screen; the dashboard is protected.
- Root-level `AnimatedContent` + a `NavigationTransitionShimmer` (1050 ms) whenever AUTH/ONBOARDING → MAIN.
- Overlay screens live in `uiState` flags rather than NavHost: `isAiAssistantOpen`, `isLanguageLabOpen`,
  `isAdminPanelOpen`, `activePaperDetail`, `activeCourseDetail`, `activeChatConversation`, `isAuthDialogOpen`.
  `BackHandler`s unwind drawer → chat → tab → Home.

### Bottom navigation (`TheStudentNavTab`)
`HOME` · `CHAT` · `AI_TUTOR` · `PAPERS` · `PROFILE` — floating bar with an elevated center action
(`ui/components/TheStudentBottomNav.kt`). Drawer (`ModernSidebarDrawer`) adds AI Assistant, Language Lab, Admin Panel.

---

> **EXP** — points are a server-side ledger (`backend/api/_exp.php`,
> `backend/database_exp.sql`), never a client counter. Read `docs/EXP.md` before
> touching anything that awards or displays them.

## 3. Screens (`com.example.ui.screens`)

| File | Lines | What it does |
|---|---|---|
| `SplashScreen.kt` | 555 | Tri-spinner logo, cycling word loader, feature chips |
| `OnboardingScreen.kt` | 1097 | 3 intro cards, trust badges, Google/email entry, region picker |
| `AuthScreen.kt` | 1921 | Sign-in / sign-up / OTP keypad, Google & Apple buttons, shimmer transitions |
| `HomeScreen.kt` | 875 | Greeting, stats (streak, hours, papers, points), courses, papers, notes, AI entry points |
| `ChatScreen.kt` | ~1050 | **Production 1:1 student chat** — directory of registered students (full name + avatar only), live online rail/count, unread badges, typing + last-seen, one-time safety notice, clear chat. Data from `api/chat.php` via `TheStudentChatClient`; see `docs/CHAT.md` |
| `PapersScreen.kt` | 1423 | Grade/stream/medium/year filters (`FilterRightSidebar`), search, skeleton shimmer, save/download, server-URL sheet |
| `AiTutorScreen.kt` | 603 | English voice tutor: visualizer, transcript, grammar tip, scenario pills, hands-free, HF settings |
| `AiLanguageScreen.kt` | 786 | 4 sub-tabs: Voice Conversation, Vocabulary, Grammar quiz, Pronunciation |
| `AiAssistantScreen.kt` | 4526 | **Flagship**: ChatGPT-style chat, markdown/table/link rendering, chain-of-thought reasoning blocks, model picker, OCR preview, document cards, stop/retry/rate |
| `ProfileScreen.kt` | 517 | Profile, saved papers/courses, activity history, theme toggle, (logout in VM) |
| `AdminPanelScreen.kt` | 1103 | Upload form (thumbnail/PDF/marking-scheme pickers), paper list + delete, backend URL tester |
| `LearnScreen.kt` | 428 | **Orphaned** — never referenced from `MainActivity` |

Components (`ui/components`): logo, top bar, bottom nav, sidebar drawer, auth dialog, paper/course dialogs,
audio visualizers (`CentralizedAudioVisualizer`, `PlasmaVisualizer`, `UiverseLoaderVisualizer`),
`AnimatedVoiceButton`, `ModernVoiceIcons`, `RevealingSpeechCard`, `ShimmerEffect`, `StudentLoadingSpinner`,
`TheStudentSnackbarHost`.

**Messages (`TheStudentSnackbarHost`)** — every snackbar in the app renders through this one composable:
a floating **pill** (fully rounded, soft shadow, max 460 dp) with a tinted icon disc — emerald tick when
something worked, amber mark when it did not (the sentence is checked for words like *offline*, *failed*,
*could not*). The stock Material snackbar (full-width dark rectangle) is never used: it read as a system
alert pasted over the design. In an open conversation the pill floats above the composer.

Theme (`ui/theme`): emerald/teal/lime brand palette, light + dark schemes, custom fonts
(Plus Jakarta Sans, Outfit, DM Sans, Roboto) bundled in `res/font`.

---

### 3b. Chat data flow (added with the chat batch)

`ChatScreen` → `TheStudentViewModel.chatState` → `TheStudentChatClient` → `api/chat.php`,
plus a local Room table that owns the actual history.

* **The server is a relay + presence board, never an archive.** Encrypted
  envelopes sit in `chat_relay` only until the recipient's `inbox` fetch, which
  deletes exactly the rows it handed out; anything older than 7 days is purged
  on the next request. There is no message, clear or key-backup table.
* **History lives on the device**: `chat_messages` (Room **v4**,
  `MIGRATION_2_3` + `MIGRATION_3_4` + `MIGRATION_4_5`, entity
  `ChatMessageEntity`) stores decrypted bubbles with
  `localId` PK (`srv-<id>` for delivered rows, a UUID for outgoing pending
  ones), and `ChatSummaryRow` drives the thread previews. `endSession()`
  wipes the table on sign-out. A reinstall loses the history by design — there
  is no backup/restore feature (removed on request).
* Polling: directory/presence 3 s, relay inbox 2 s, heartbeat 2 s (online
  window on the server is 10 s, caller excluded, counted on the DB clock by one
  helper shared by `threads` and `heartbeat`).
* End-to-end encryption: `data/local/ChatCrypto.kt` (EC P-256 ECDH + AES-256-GCM,
  Keystore-sealed private key). The server rejects anything that is not
  `e2ee:v1:…`.
* **Attachments** (photos, documents, voice notes, ≤25 MB): photos are
  re-encoded first (`ChatMediaPrep`, 1600 px / q85, EXIF-aware); uploads and
  downloads report real progress; the file is encrypted with the
  same ECDH conversation key, uploaded as an opaque blob to
  `upload_attachment`, and deleted from the server when the recipient downloads
  it. Bubbles carry only an encrypted JSON envelope with the caption and file
  metadata; decrypted bytes live in `filesDir/chat_attachments` through
  `ChatAttachmentStore` (120 MB cap, wiped on sign-out) and open through a
  `FileProvider`.
* Opening a conversation holds the orbit loader until the list has been jumped to
  the last message underneath it, so nothing visibly scrolls on entry; later
  auto-scroll only happens while the reader is near the bottom.
* Bubbles render **Markdown** through `ui/screens/ChatMarkdown.kt` (bold, italic,
  strike, code, headings, lists, quotes, links) as a single `AnnotatedString`.
* **Replies** (left→right swipe) quote a message through the encrypted envelope (`r`), and
  in-conversation search jumps between green-highlighted hits in the local
  table — nothing extra leaves the phone. Voice notes are AAC recordings sent as `kind = audio`.
* **Delete for everyone**: long-press → the row is removed locally (and its cached
  file), then `delete_message` queues a `delete` envelope the peer applies to its
  own copy on the next inbox poll.
* The chat list shows exactly one badge: the emerald count of messages I have
  not read. A second "my messages they have not opened" counter was tried and
  removed at the user's request — the single/double tick inside the conversation
  already carries that information.
* Device notifications live in the `notifications/` package: `ChatNotifications`
  (channel `chat_messages`, collapsed-by-default MessagingStyle per conversation,
  grouped summary only when 2+ chats are unread, tap → `MainActivity` + peer id),
  `ChatNotificationAvatars` (circular profile photo as large icon, coloured
  initial as fallback) and `ChatNotifier` (reconciliation from the unread rows,
  shared by the view model and the FCM service). Delivery is FCM data messages
  (`ChatMessagingService`) with a foreground/background poll as fallback; the app
  being open suppresses nothing except the conversation on screen. See
  `docs/CHAT.md` §1d.
* A reply quote stores `isMine` **relative to the author of the carrying
  message**, so the renderer must mirror it on the other device
  (`if (bubble.isMine) reply.isMine else !reply.isMine`); otherwise every quote
  reads "You" on both phones.
* **Academic profile is structured on purpose.** `users.stream_code`,
  `exam_level` (`AL`/`OL`), `exam_year`, `medium` + one row per subject in
  `user_subjects` (`UNIQUE(user_id, subject_code)`). Free-text
  `stream_or_degree` stays for display only — it can never be matched on,
  because two "Technology" students may share zero subjects.
  `POST ?action=academic_profile` replaces the subject list inside a
  transaction; the app owns the catalog (`AcademicCatalog`), the server owns
  the shape (exactly 3 distinct subjects, one per O/L bucket, sane year).
  Matching must JOIN `user_subjects`, never `LIKE` on the free text.
* **Never repeat a named placeholder in one statement.** PDO runs with
  `ATTR_EMULATE_PREPARES = false`, so `... (a = :me AND b = :peer) OR (a = :peer
  AND b = :me)` throws *Invalid parameter number* and the endpoint 500s. Use
  `:me_a/:peer_a/:me_b/:peer_b`. This is what broke help requests (and hid the
  help card) until chat.php 2.7.1.
* **The chat purpose is a label, never a gate.** It lives in `chat_purposes`
  (per owner + peer) and is mirrored into the chat list. The ask is always
  *after* the action: the app never blocks the composer, and the offer to track
  a question appears only once the message has been sent.
* **Help requests are the only path to a rating.** `chat_help_requests`:
  pending → accepted → completed (or cancelled / expired). The helper accepts,
  **only the seeker can close** (they are the only person who knows whether the
  question was answered), both may cancel, and closing is refused inside 10
  minutes of acceptance so two friends cannot farm it. One open request per
  pair, 5 per seeker per day, pending expires after 72 h.
* `action=profile` returns an allow-list only (name, photo, target exam, stream,
  institution, presence, joined month). Nothing account-identifying is in the
  SELECT at all, so the chat profile screen cannot leak contact details.
* Read receipts update **my** messages only (`markMyMessagesRead`) — marking the
  incoming rows there is what used to clear unread badges on the chat list.
* **Never touch a property from the ViewModel `init` block without suspending
  first.** `viewModelScope` runs on `Dispatchers.Main.immediate`, so a `launch`
  started in `init` executes its body inline — before any `val` declared below
  the `init` block exists. `observeChatDatabase()` did exactly that, hit a null
  `chatDao`, and its `runCatching` swallowed the `NullPointerException`: the
  summaries collector never started, so the chat list had no previews and no
  unread badges at all, while conversations (which open later) worked fine.
  A single `yield()` at the top of the launched body, or `Dispatchers.IO`,
  avoids the whole class of bug.
* Only full names and avatars are ever exposed about another student.

## 4. State layer — `TheStudentViewModel` (1660 lines)

`TheStudentUiState` (≈70 fields) covers: screen flow, tab, filters, overlays, TTS speaking, snackbar,
dark theme, auth/OTP pending state, voice-tutor state, the whole AI-assistant state, OCR state, admin panel.

Key flows exposed: `uiState`, `savedPapers`, `savedCourses`, `recentActivities`, `userProfile`, `chatMessages`,
`dynamicPapers`, `isLiveDatabaseConnected`, `serverUrlState` (backend base URL, default
`https://app.quizontal.site/`), `isTestingConnection`, `connectionTestResult`.

Notable behaviour:
- `init` → seeds Room, `loadDynamicPapers()` (Retrofit → falls back to `DefaultPastPapersSeed.allPapers`, 50 papers), `verifySessionWithBackend()`.
- `verifySessionWithBackend()` validates the stored token via `api/auth.php?action=me`. Only a definitive
  401/403 clears the session; network failures keep the user signed in (offline-tolerant). Administrator
  access is `preferences.userRole == "admin"`, refreshed from the server on every validation.
- `TheStudentPreferences` (SharedPreferences `thestudent_user_session`) stores `sessionToken`, `userEmail`, `isLoggedIn`, `isOnboarded`.
- TTS (`TextToSpeech`, `Locale.US`, rate 0.95) + `SpeechRecognizer` are used for both the Language Lab and AI Tutor; hands-free mode auto-relistens after speaking.
- `simulateSkeletonRefresh()` = fake 450 ms loading for shimmer polish.

---

## 5. Data layer

**Room** (`the_student_database`, v1, `fallbackToDestructiveMigration`)
- `saved_papers`, `saved_courses`, `learning_activities`, `user_profile` (single row id = 1)
- `TheStudentDao` → flows + insert/replace/delete + course progress + activity history (last 20)
- `TheStudentRepository` maps entities ↔ domain models, records activity entries.

**Domain models** (`data/model/TheStudentModels.kt`): `PastPaper`, `Course`, `Lesson`, `StudyNote`,
`AiChatMessage`, `VocabularyWord`, `GrammarPractice`, `ConversationScenario`, `UserProfile`,
`OnlineStudent`, `ChatMessageItem`, `ChatConversation`; enums `EducationLevel`, `ExamType`, `MessageSender`.

**Bundled content**
- `SampleEducationalData.kt` (1264 lines): courses, notes, vocabulary, grammar exercises, scenarios, pronunciation sentences (+ its own small paper list).
- `DefaultPastPapersSeed.kt` (1015 lines): 50 papers — Grade 6-13, O/L, A/L (Physical/Bio/Commerce/Arts/Tech), Sinhala/English/Tamil, archive.org PDF + marking-scheme URLs.
- Curricula helpers: `GradeCurriculum` (grades/mediums/exam types/years/subject lists), `SriLankanCurriculumConfig`.

**Remote papers API** (`PastPapersRemoteRepository` + `PastPapersApiService`, Retrofit + Moshi)
- `GET api/get_papers.php?category&grade&stream&subject&year&medium&search&status`
- `POST api/upload_paper.php`, `POST api/delete_paper.php`
- `PastPaperApiDto.toDomain()` repairs missing grade/category/stream/year/pages.
- PDF download via `DownloadManager` into public Downloads (browser fallback).
- Backend URL is user-editable and persisted separately (`thestudent_backend_prefs`).

---

## 6. AI stack

### 6.1 Study assistant — `TheStudentStudyAiClient` (1420 lines)
`streamStudyResponse()` routing:
1. **Z.AI GLM-4.7-Flash** (`glm-4.7-flash`, `api.z.ai/api/paas/v4`, fallback `open.bigmodel.cn`) — SSE streaming + **native tool calling loop**; key from user setting → `BuildConfig.ZAI_API_KEY` / `Z_AI_API_KEY` → env.
2. **Google Gemini** (`gemini-3.8-flash`, `:generateContent`) when the model choice is Gemini — non-stream (chunk emitted at once).
3. **Hugging Face Space fallback** `https://danuka21-dzd-learn-ai.hf.space` (GPT-OSS 20B, SSE, reasoning channel).

Features: reasoning ("thinking") token channel surfaced as chain-of-thought UI, markdown system prompt (Sinhala/Tamil/English,
step-by-step maths, numbered-list rules, pastpapers.wiki → govdoc.lk → other sources search hierarchy),
jargon cleaner, suggested follow-up prompts, attachment handling (image → base64 + on-device OCR text, PDF → rendered page text).

### 6.2 Tools — `TheStudentStudyTools` (942 lines)
`calculator`, `lookup_curriculum_reference`, `get_exam_countdown`, `create_comprehension_quiz`,
`Tools_Web_Search`, `Tools_Web_Fetch` (MCP streamable-HTTP at `https://alyxsis-tools.hf.space/gradio_api/mcp/`),
`read_document`, `download_and_share_document`.

### 6.3 Documents — `TheStudentDocumentHandler` (1220 lines)
`PdfRenderer` page rasterisation + OCR, verified PDF generation, MediaStore/FileProvider save, share intents,
`[DOWNLOAD_DOCUMENT:...]` chat action tags rendered as download cards.

### 6.4 OCR — three engines
`TheStudentOcrEngine` orchestrates:
1. **Embedded Tesseract** (`data/local/TesseractOcrEngine`, `tesseract4android`, bundled `tessdata` fast models for `sin`, `tam`, `eng`) — 100 % on-device, cached `TessBaseAPI` under a `Mutex`, 1600 px downsample + contrast boost.
2. **ML Kit text-recognition** (Latin, ~30 ms).
3. **Gemini Vision** for hard handwriting/diagrams.
Language auto-detected from Unicode ranges (Sinhala 0D80–0DFF, Tamil 0B80–0BFF). Results shown in an editable OCR panel before sending.

### 6.5 Voice tutor
`HuggingFaceSpaceClient` → `https://danuka21-qwen3-8-27b.hf.space/v1/chat/completions` (OpenAI-compatible),
token priority custom → `BuildConfig.HF_API_TOKEN`; falls back to `EnglishVoiceTutorEngine` — a rule-based
local conversational engine (grammar corrections, scenario greetings, reply pills, vocabulary highlights).

### 6.6 Model choices (`AiModelChoice`)
`GLM_4_7_FLASH` (default), `HUGGING_FACE`, `GEMINI_FLASH`. Selectable in-app via `AiModelSelectionDialog`; custom Z.AI key + custom HF token/base-URL are configurable at runtime.

---

## 7. Auth & backend

`data/remote/TheStudentAuthClient` → `api/auth.php` (v3, hardened — see `docs/AUTH_SECURITY.md`):
`register`, `login`, `verify_otp`, `resend_otp`, `request_password_reset`, `reset_password`,
`change_password`, `google_login`, `me|profile`, `logout`, `logout_all`, `validateSession`.
Returns `AuthApiResult.Success / RequiresOtp / ResetRequired / Error` and, for session checks,
`SessionValidation.Valid / Invalid / Unreachable`. Sessions are opaque 64-hex tokens sent **only**
in `Authorization: Bearer`; the server stores SHA-256 digests, OTPs are HMAC digests, and the token is
AES-GCM encrypted at rest on the device via the Android Keystore.

Backend (`backend/`, mirrored in `app/website_php_mysql/` and `app/directadmin_backend/`; zipped in `backend_directadmin.zip`):
- `config.php` — PDO connection, CORS `*`, Google web client id, Gmail SMTP constants.
- `api/auth.php` (895 lines) — bcrypt cost 12, prepared statements only, pending accounts until OTP verified,
  6-digit OTP with expiry/attempts, IP + email rate limiting (`login_attempts`), IP-change challenge,
  Google ID-token `aud` check, 64-hex session tokens, auto-migration `ALTER TABLE` on boot.
- `smtp_mailer.php` — dependency-free Gmail SMTP (ssl://465) with branded HTML template.
- `api/get_papers.php` / `upload_paper.php` / `delete_paper.php` (+ `backend/admin/index.php` web console, 1085 lines).
- Schema: `database_auth.sql` (`users`, `email_verifications`, `user_tokens`, `login_attempts`), `database.sql` (`past_papers`).
- There is also a native client-side SMTP sender (`TheStudentEmailSender.kt`) — an offline fallback for OTP mail.

---

## 8. Build config notes

- Plugins: Android application, Kotlin Compose, KSP, Roborazzi, Secrets, Google Services (`MissingGoogleServicesStrategy.WARN`, `googleServices.missing.passthrough=true`, so no `google-services.json` needed).
- Key deps: Compose BOM + Material3 + Nav, Room (KSP), Retrofit + Moshi (codegen + reflection), OkHttp logging, Coil, ML Kit text-recognition, tesseract4android, Firebase AI, Credential Manager + googleid, Firebase App Check (reCAPTCHA/debug), Robolectric/Roborazzi tests.
- Release signing reads `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD` env (default `my-upload-key.jks`, absent from repo); debug uses the committed `debug.keystore`.
- Tests: 3 unit/robolectric stubs + 1 screenshot test — effectively no real coverage.
- Repo also carries `TheStudent_Logo.png`, `example_thumbnail.png`, screenshots in `public/assets/aistudio/`, and an empty `untitled.tsx`.

---

## 8b. Mandatory profile setup (added with the compliance/production batch)

No account reaches the dashboard until `users.profile_completed_at` is set.

* `POST api/auth.php?action=complete_profile` (multipart or JSON) collects phone,
  curriculum, institution, stream/degree, profile photo and — for Google-only
  accounts — a first password.
* `api/_avatar.php` validates uploads (real image via `getimagesize`, size,
  dimensions, MIME), re-encodes them to JPEG with GD (kills polyglots/EXIF),
  stores a random 32-hex name in `uploads/avatars/`, and that folder denies
  script execution.
* Android: `AppScreenFlow.PROFILE_SETUP` between AUTH and MAIN,
  `ui/screens/ProfileSetupScreen.kt`, `data/remote/AvatarPreparer.kt`,
  `TheStudentAuthClient.completeProfile()`, Room v2 migration for the new
  profile columns.
* Full details: `docs/PROFILE_ONBOARDING.md`.

## 9. Findings worth fixing (ranked)

1. ✅ **FIXED — live secrets were committed to git.** `backend/config.php` now reads credentials from
   environment variables or a git-ignored `config.secrets.php`; the client-side SMTP sender
   (`TheStudentEmailSender.kt`) with the Gmail app password baked into the APK was deleted.
   **Still required:** rotate the leaked MySQL + Gmail passwords (they remain in git history).
2. **`markPaperDownloaded()` looks the paper up in `SampleEducationalData.pastPapers`**, not the seeded/remote list — so "downloaded" state never persists for backend papers (silent no-op).
3. **Build/plugin setup is half-wired:** the `google-services` Gradle plugin and `firebase-ai` are enabled while Firestore is commented out and no `google-services.json` exists (only `passthrough`), so Firebase never actually initialises — harmless but confusing dead weight.
4. **Dead code:** `LearnScreen.kt` (428 lines) is unreachable; `SampleEducationalData.pastPapers` duplicates `DefaultPastPapersSeed`; `TheStudentEmailSender` duplicates backend SMTP; empty `untitled.tsx`.
5. **Mic visualizer is static** — `onRmsChanged` throws away RMS to avoid recomposition, so `micRmsDb` is always 0 and the voice visualizer never reacts to actual volume (fix with a throttled/`snapshotFlow` publish).
6. **Hard-verify model IDs** — `gemini-3.8-flash`, `glm-4.7-flash`, `glm-4-flash` should be checked against the providers' current catalogues; a wrong ID silently degrades to the HF fallback.
7. **Security hardening — done (see `docs/AUTH_SECURITY.md`):** session token AES-GCM encrypted via the
   Keystore; admin bypass removed (client *and* server); CORS restricted by `ALLOWED_ORIGINS`; the admin
   console and the paper upload/delete APIs require authentication; cleartext HTTP disabled app-wide
   (`network_security_config.xml` allows localhost/emulator/LAN only); session store excluded from backups.
   Outstanding operator action: **rotate the MySQL and Gmail passwords that were committed to git history.**
   UI side (`AuthScreen.kt`, `SecurityScreen.kt`, drawer) is Play-store ready too: account deletion,
   Terms/Privacy consent, sign-out, change-password, autofill content types, OTP paste/auto-submit,
   retry-able errors and a live password-strength meter. Compose BOM was raised to `2025.06.01`
   (1.8) for `androidx.compose.ui.autofill.ContentType`. Legal page templates live in
   `backend/legal/` and must be filled in and uploaded to `/legal/`.

   **Secrets & configuration:** every credential now lives in exactly two git-ignored files —
   `.env` (app; read by the Secrets Gradle Plugin → `BuildConfig`) and `backend/config.secrets.php`
   (server; or `THESTUDENT_*` / plain env vars). Committed templates: `.env.example`,
   `backend/config.secrets.example.php`. Full instructions, rotation table and the
   "new feature needs a key" checklist: **`docs/SECRETS.md`**. A pre-commit leak scanner lives at
   `scripts/check-secrets.sh`, and for **GUI-only DirectAdmin deploys** (no SSH) follow
   `docs/DEPLOY_DIRECTADMIN_GUI.md` (debugging a 500: `docs/DEBUGGING.md` + `api/health.php`) —
   preferred secrets location is one level above `public_html`
   (`domains/app.quizontal.site/config.secrets.php`); `config.php` searches there first.
8. **No real chat backend** — the CHAT tab is entirely local demo data despite looking production-ready.
9. **Auth is mandatory** (guest deprecated) yet `MainActivity` still contains a fallback white-box + `openAuthScreen()` effect — a slightly awkward safety net.
10. **Test coverage ≈ 0**, and nothing guards the 70-field UI state or the AI routing.

---

## 10. Quick file map

```
app/src/main/java/com/example/
├── MainActivity.kt                     # root composable, flow switching, overlays
├── viewmodel/TheStudentViewModel.kt    # all app state + AI/TTS/Speech logic
├── data/
│   ├── model/       TheStudentModels, AiAssistantModels, GradeCurriculum, SriLankanCurriculumConfig
│   ├── local/       Room DB/DAO/entities, TheStudentPreferences (Keystore-encrypted token), TesseractOcrEngine
│   ├── remote/      PastPapers*, AuthClient, GoogleAuthClient, StudyAiClient,
│   │                StudyTools, DocumentHandler, OcrEngine, HuggingFaceSpaceClient,
│   │                EnglishVoiceTutorEngine, DefaultPastPapersSeed
│   └── repository/  TheStudentRepository, SampleEducationalData
└── ui/
    ├── screens/     Splash, Onboarding, Auth, Home, Chat, Papers, AiTutor, AiLanguage,
    │                AiAssistant, Profile, AdminPanel, Learn(unused)
    ├── components/  nav, drawer, top bar, dialogs, visualizers, shimmer, logo
    └── theme/       Color, Theme, Type
backend/                # live PHP/MySQL API (auth, papers, admin console, schema, legal pages)
app/website_php_mysql/  # older copy of the API + web portal
app/directadmin_backend/ # minimal papers-only API copy
docs/                   # AUTH_SECURITY.md, SECRETS.md, PROJECT_KNOWLEDGE.md
scripts/                # check-secrets.sh (pre-commit leak guard)
.env.example            # app secrets template (copy to .env, never commit .env)
```


---

## 11. Authentication deep-dive

The complete auth model, threat list and deployment runbook live in **`docs/AUTH_SECURITY.md`**.
Short version: server-issued roles only, hashed session tokens + HMAC OTPs, per-email/-IP throttles,
enumeration-resistant login & reset, locked-down admin console and paper write APIs, and a Keystore-encrypted
token on the device.
