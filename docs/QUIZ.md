# Quizzes — plan

A student-facing quiz system: admins author quizzes in the web console, students
take them in the app, and a passed quiz pays EXP through the existing ledger.
Nothing is bundled with the APK — like the papers library after turn 74, an empty
quiz list means "nothing published yet", never sample content.

Status: **implemented** (turns 76–77). See §13 for what shipped and the two
places where the build deviated from this plan.

## Locked decisions (2026-10-28)

| Decision | Choice |
| --- | --- |
| Question type | Single-answer MCQ, four options, one correct |
| EXP | 20 for a pass + 10 for 100%, **once per quiz**; daily cap still applies |
| Authoring | **In the in-app admin panel only** — no web quiz builder |
| Student entry | A **Quizzes segment inside the Papers tab**, under the same header |

The in-app choice is the one that changes the shape of the work: the write API
has to be complete and safe, because a phone is now the authoring tool.

---

## 1. Goals

1. An admin can write a quiz (title, subject, grade, questions, options,
   explanations) and publish it without touching the database by hand.
2. A student can find it, take it, see exactly what they got right and wrong, and
   come back later to retake it.
3. Passing pays EXP once, through `user_exp` — no new reward mechanism, no
   optimistic "+20" on the phone.
4. Every screen follows the app's existing rules: our own header (never the
   default app bar), `statusBarsPadding`, black section headings, chips that fit a
   360 dp phone, skeleton while loading, real empty/error states, one snackbar
   host.

## 2. What v1 is, and what it deliberately is not

| In v1 | Not in v1 |
| --- | --- |
| Single-answer multiple choice (4 options, one correct) | Multi-select, matching, structured essay, short answer |
| Optional image on a question (URL, uploaded in admin) | LaTeX rendering, audio questions |
| Optional time limit, auto-submit at zero | Per-question timers, adaptive difficulty |
| Retakes, best score kept | Live multiplayer/classroom mode |
| Authoring on the phone, drafts autosaved | Editing a published quiz mid-attempt window |
| Explanation shown after submitting | Hints during the attempt |
| EXP on first pass and first perfect | Leaderboards, prizes, badges beyond the current ladder |

Basis: the Sri Lankan school papers this app already carries are MCQ + structured.
Automatic grading of structured answers is a different project; MCQ is the part
that can be marked instantly and honestly.

---

## 3. Data model

New file `backend/database_quizzes.sql` (safe to re-run, `IF NOT EXISTS`, same
`utf8mb4_unicode_ci` / InnoDB conventions as `database.sql`).

```sql
CREATE TABLE IF NOT EXISTS `quizzes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(64) NOT NULL UNIQUE,          -- stable id for exports/refs
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `subject` VARCHAR(100) NOT NULL,
  `grade` VARCHAR(50) NOT NULL,                -- Grade 6 … Grade 13, Undergraduate
  `category` VARCHAR(50) NOT NULL,             -- A/L, O/L, Junior, Provincial, University
  `stream` VARCHAR(100) DEFAULT NULL,
  `medium` VARCHAR(50) NOT NULL DEFAULT 'English',
  `time_limit_seconds` INT NOT NULL DEFAULT 0, -- 0 = untimed
  `pass_percent` TINYINT NOT NULL DEFAULT 60,
  `shuffle_questions` TINYINT(1) NOT NULL DEFAULT 1,
  `shuffle_options` TINYINT(1) NOT NULL DEFAULT 1,
  `exp_reward` INT NOT NULL DEFAULT 20,        -- capped by TS_EXP_RULES
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft', -- draft | published | archived
  `created_by` INT DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_status` (`status`), INDEX `idx_grade` (`grade`),
  INDEX `idx_subject` (`subject`), INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_questions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `quiz_id` INT NOT NULL,
  `position` INT NOT NULL DEFAULT 0,
  `prompt` TEXT NOT NULL,
  `media_url` VARCHAR(500) DEFAULT NULL,
  `explanation` TEXT DEFAULT NULL,
  `marks` TINYINT NOT NULL DEFAULT 1,
  INDEX `idx_quiz` (`quiz_id`),
  CONSTRAINT `fk_quiz_questions_quiz` FOREIGN KEY (`quiz_id`)
    REFERENCES `quizzes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_options` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `question_id` INT NOT NULL,
  `position` TINYINT NOT NULL DEFAULT 0,
  `label` TEXT NOT NULL,
  `is_correct` TINYINT(1) NOT NULL DEFAULT 0,
  INDEX `idx_question` (`question_id`),
  CONSTRAINT `fk_quiz_options_question` FOREIGN KEY (`question_id`)
    REFERENCES `quiz_questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_attempts` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `quiz_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `submitted_at` TIMESTAMP NULL,
  `score` INT NOT NULL DEFAULT 0,
  `max_score` INT NOT NULL DEFAULT 0,
  `percent` TINYINT NOT NULL DEFAULT 0,
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `duration_seconds` INT NOT NULL DEFAULT 0,
  `answers_json` TEXT DEFAULT NULL,            -- audit trail of what was chosen
  `exp_awarded` INT NOT NULL DEFAULT 0,
  INDEX `idx_quiz_user` (`quiz_id`, `user_id`),
  INDEX `idx_user` (`user_id`),
  CONSTRAINT `fk_quiz_attempts_quiz` FOREIGN KEY (`quiz_id`)
    REFERENCES `quizzes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

No quizzes are seeded. A production server starts empty and the app says so.

`question_count` is never stored — it is `COUNT(*)` per quiz at read time, so the
number on the card can never drift from the questions the student is given.

---

## 4. Authoring — in the app's admin panel

The builder lives in `AdminPanelScreen` (the screen that already uploads papers),
as a fourth tab next to Upload / Catalog / Database settings:

```kotlin
enum class AdminTab { UPLOAD, CATALOG, QUIZZES, DATABASE_SETTINGS }
```

Because a phone is a cramped place to write twelve questions, the tab itself is
just a **list**, and editing opens a full-screen route (`isQuizEditorOpen`), so the
form gets the whole display and the admin panel's tab bar isn't competing with a
keyboard.

### 4.1 Quizzes tab (list)

- Our header (back, "Quizzes", "12 published · 3 drafts"), plus a green
  "New quiz" pill.
- Rows: title, "Physics · Grade 13 · 12 questions", a status badge
  (Draft / Published / Archived) and a small overflow with Publish / Archive /
  Duplicate / Delete (delete only when the quiz has no attempts — otherwise the
  server refuses and the app says "Archive it instead").
- Empty state: "No quizzes yet. Create the first one." with the New quiz button.

### 4.2 Editor route

One screen, scrolling, our header with a "Save draft" text action and a green
"Publish" action:

1. **Details card** — title, subject, grade (chips), category (chips: A/L, O/L,
   Junior, Provincial, University), stream (A/L only, chips), medium (chips),
   time limit (Off / 5–120 min step 5), pass mark (slider 30–90%), shuffle toggles,
   EXP reward (number field, clamped 0–40).
2. **Question cards**, numbered, each with: prompt (multiline), optional image
   (pick from gallery, uploaded to `uploads/quiz/` with the same MIME + size
   validation as paper uploads), four option fields each with a radio to mark the
   correct one, an optional explanation (multiline, shown after grading), marks
   (1–3, default 1), and Up / Down / Duplicate / Delete controls.
3. **Footer summary**: "12 questions · 14 marks · ~15 min", then Save draft /
   Publish.

Behaviour that matters:

- **Autosave.** Every change writes the whole quiz to a local
  `quiz_admin_draft` (SharedPreferences JSON). If Android kills the app or the
  admin backs out, reopening the editor offers "Continue where you left off".
  Nothing is sent to the server until Save/Publish, so a half-written quiz can
  never appear in the catalog.
- **Validation before any network call**, mirroring the server: title ≥ 3 chars,
  at least one question, every question has a prompt and exactly one correct
  option, no empty option labels. Errors point at the offending question, not a
  generic banner.
- **Publishing is explicit.** Save keeps `status = draft`; Publish flips it. A
  published quiz can still be edited, but the edit is what students see next time
  they open it — an in-progress attempt keeps the question set it started with.
- **Admin only.** Every write goes through `ts_require_admin_api($pdo)` (the same
  guard `upload_paper.php` uses) plus the app's existing
  `adminAuthorizationHeader()` bearer session.

The web console keeps its paper tools untouched. A read-only "Quizzes" list may be
added there later purely so a teacher can review results on a laptop — it is not
part of this plan.

## 5. API (`backend/api/quizzes.php`)

Same shape as `chat.php`: bearer session token, JSON envelope
`{status, message, data}`, `X-API-Version: 1.0.0` (the app refuses older).

| Method | Action | Auth | Returns |
| --- | --- | --- | --- |
| GET | `list` | student | published quizzes: id, code, title, subject, grade, category, medium, minutes, pass percent, question count, exp reward, my best percent/passed |
| GET | `get&id=` | student | quiz header + questions with option **labels only** (positions shuffled per attempt) |
| POST | `start` | student | `attempt_id`, question set (shuffled if enabled), deadline server time if timed |
| POST | `submit` | student | grading (score, percent, passed), `review[]` with the correct option, chosen option and explanation, `exp_awarded`, fresh `exp` block |
| GET | `history` | student | my attempts, newest first (quiz, score, percent, date) |
| GET | `state` | student | quiz-specific summary for the hub: `completed`, `passed`, `average`, `exp_total_from_quizzes` |
| GET | `admin_list` | **admin** | every quiz whatever its status, with question count, attempts and average percent |
| GET | `admin_get&id=` | **admin** | the full quiz including `is_correct` and explanations, for editing |
| POST | `admin_save` | **admin** | create or update a quiz + its questions/options in one transaction; returns the quiz id |
| POST | `admin_status` | **admin** | publish / draft / archive |
| POST | `admin_delete` | **admin** | delete only when `quiz_attempts` has no row for it, else 409 "archive instead" |

Admin actions sit behind `ts_require_admin_api($pdo)` (already used by
`upload_paper.php`) and answer 403 to a normal student session. `admin_save`
writes inside a transaction: either the quiz, its questions and their options all
land, or nothing does — no half-saved quiz can be published.

**The client never receives `is_correct`.** Grading happens in PHP on `submit`;
`list`/`get`/`start` omit the flag entirely, so opening the app with a proxy or a
debugger reveals nothing.

**Grading rules**

- One mark per correct option by default (question `marks` respected).
- `percent = round(score / max_score * 100)`; `passed = percent >= pass_percent`.
- Empty answers count as wrong, and are reported as "not answered" in the review.
- Submit is idempotent per attempt: a retried POST returns the same result and
  never awards twice (the ledger's `ref_key` is the second guard).

---

## 6. EXP integration

`user_exp` already does the hard part. Quizzes add two kinds to
`TS_EXP_RULES` in `backend/api/_exp.php`:

| kind | EXP | ref_key | when |
| --- | --- | --- | --- |
| `quiz_passed` | 20 | `quiz:{quiz_id}:pass` | first time you meet the pass mark on that quiz |
| `quiz_perfect` | 10 | `quiz:{quiz_id}:perfect` | first 100% on that quiz (on top of the pass award) |

- The pass amount comes from the quiz row (`exp_reward`, default 20, clamped by
  the server to 0–40) through `ts_exp_award(... , amountOverride: ...)`; the perfect
  bonus is half of it (10 by default). A quiz the admin sets to 0 EXP still grades
  and still shows in the review, it just pays nothing.
- Retakes are free and always allowed, but they never pay twice — the ref key is
  the quiz, not the attempt.
- The daily ceiling still applies (`ts_exp_award` trims or skips), and a trimmed
  award is recorded as the smaller real number. The result screen reads
  `exp_awarded` from the server, so it can honestly show "+0 today's cap reached".
- The quiz card and the result screen show the *configured* `exp_reward` and the
  breakdown; the EXP screen gains the two new rules in its "how to earn" list and
  the history entries carry notes like "Passed · Physics: Magnetism (80%)".
- `TheStudentViewModel.refreshExp()` stays the only writer of the client total:
  the submit response carries the fresh `exp` block, which is handed to the
  existing parser rather than summed locally.
- Badges: no new badge in v1. If wanted later, a "Quiz champion" badge (10 quizzes
  passed) fits the existing five-badge ladder without new machinery.

---

## 7. App architecture

```
data/model/QuizModels.kt          QuizSummary, QuizQuestion, QuizOption,
                                  QuizAttemptResult, QuizReview, QuizState
data/remote/QuizzesApiService.kt  Retrofit + Moshi, same client builder as papers
data/remote/QuizzesRepository.kt  list / get / start / submit / history, Room cache
data/local/…                      Room v8: quiz_result_cache, quiz_draft
viewmodel/QuizViewModel.kt        (or a QuizSlice inside TheStudentViewModel,
                                   matching how papers/EXP already work)
ui/screens/QuizzesScreen.kt       hub
ui/screens/QuizRunnerScreen.kt    question by question
ui/screens/QuizResultScreen.kt    score, review, EXP
```

**Why a slice in `TheStudentViewModel` rather than a second ViewModel:** the app
already drives papers, EXP, chat and profile from one VM; a quiz submit has to
call `refreshExp()` and the snackbar host, both of which live there.

**Room v8** (current version 7, destructive fallback already configured):

- `quiz_result_cache` — last known result per quiz, so the hub shows "Best 80%"
  with no network.
- `quiz_draft` — answers so far for an unfinished attempt, restored if the app is
  killed mid-quiz.

The admin's in-progress quiz is **not** in Room: it is one JSON document in
SharedPreferences (`quiz_admin_draft`), which survives process death and is
deleted the moment the quiz is saved to the server.

**Navigation.** Quizzes are not a new destination — they are a second segment of
the Papers tab, so the bottom bar and the tab state do not change at all:

- `PapersScreen` gains a segment control directly under its header
  (`Papers | Quizzes`), two pills with the active one in ink. Everything below it
  (search, chips, result line, list) belongs to the selected segment, and the
  filter sheet, sort sheet and search query are kept per segment so switching
  back does not lose a search.
- New `UiState` flags for the deeper screens: `activeQuiz` (`QuizSummary?`, opens
  the runner full-screen and hides the bottom bar) and
  `submitResult` (`QuizAttemptResult?`, the result page — a sub-state of the
  runner, so back from the result returns to the quiz list, not to question 1).
- Admin: `activeAdminQuizId` (editor open) alongside the existing admin flags.
- `MainActivity`'s top-bar exclusion list already skips the Papers tab; the runner
  and the editor draw their own headers too, so nothing else changes there.

`MainActivity`'s top-bar exclusion list gains the quiz screens (same reason as
Papers, Profile, EXP, Account), and the bottom bar hides while a quiz is running.

**Entry points**

1. The `Quizzes` segment of the Papers tab — the only browser.
2. The EXP screen's rules list gains the two quiz rows so the reward is
   discoverable where the number is read; it links to the Papers tab with the
   Quizzes segment already selected (`viewModel.openQuizzesSegment()`).
3. When the papers library is empty, its empty state offers "Take a quiz instead"
   as a second action — a student who lands on nothing still has something to do.
4. No new bottom tab: five is already the maximum and none of the current five is
   disposable.

---

## 8. Screens and states

### 8.1 Quiz hub

- Our header: back, "Quizzes", subtitle "{n} published · {m} passed".
- Search pill (title, subject, grade).
- Chips derived from the data: All, A/L, O/L, Grades 6–9, plus "Not started" and
  "Passed" toggles — same chip pattern as Papers.
- Cards: subject-coloured cover tile (reuse `PaperCover` styling), title, subject ·
  grade · medium, badges: `{n} questions`, `{m} min` (if timed), `Pass {p}%`,
  green `+{exp} EXP`, and either "Best 82%" or "New".
- States: skeleton (3 cards), empty ("No quizzes published yet — your teacher's
  quizzes will appear here"), offline strip with retry, no-match state.

### 8.2 Runner

- Progress: "Question 4 of 10", a thin progress bar, and a timer pill that turns
  amber under 60 s (auto-submits at zero).
- Question card: large prompt, optional image, "Marks: 1".
- Options: four tall, full-width rows with letter circles (A–D); the selected row
  fills ink, and the choice is remembered if you go back a question.
- Footer: Previous / Next, and on the last question "Submit" (with a confirm sheet
  if any question is unanswered). Leaving mid-quiz asks "Leave quiz? Your answers
  stay on this phone."
- Exit is always available; answers persist to Room as you go.

### 8.3 Result

- Score ring (percent) on a white card, "Passed" or "Not passed" pill against the
  pass mark, correct/incorrect/unanswered counts, time taken.
- EXP: "+20 EXP earned" from the server's `exp_awarded`, or a quiet "No EXP this
  time — you've already earned it for this quiz", plus the new total.
- Review list: each question with your answer, the correct answer, and the
  explanation; a filter pill for "Wrong only".
- Actions: "Try again", "Back to quizzes".

Palette and typography reuse the app's FormInk / FormMuted / hairline scale; no new
colours except one warm amber used only for the timer warning.

---

## 9. Integrity and abuse

| Risk | Guard |
| --- | --- |
| Answers visible in the response | `is_correct` never serialised outside `submit` |
| Instantly submitted quiz | `duration_seconds` recorded; submissions under 3 s rejected as a bad request |
| Same quiz farmed for EXP | award ref key is the quiz, `UNIQUE (user_id, ref_key)` |
| Running past the daily cap | `ts_exp_award` trims/skips and returns the paid amount |
| Retried submit | idempotent per attempt; returns stored result |
| Editing someone else's attempt | attempts are read/written for `session user` only |
| Admin-only writes | console pages sit behind `ts_require_admin` + CSRF, same as papers |
| Deleting a quiz with results | `archived` status instead of delete; delete only when no attempts exist |

---

## 10. Delivery plan

| Phase | Work | Output |
| --- | --- | --- |
| **1** | `database_quizzes.sql`, `quizzes.php` read path (`list`/`get`/`start`/`submit`/`history`/`state`), grading, `quiz_passed` / `quiz_perfect` in `_exp.php` | an API a curl session can take a quiz with |
| **2** | Admin write path (`admin_list` / `admin_get` / `admin_save` / `admin_status` / `admin_delete`), transactions, guard checks | a quiz can be created by an admin token |
| **3** | App: `QuizModels`, `QuizzesApiService`, `QuizzesRepository`; the `Papers \| Quizzes` segment; the quiz hub with skeletons, chips, empty/offline states | students can browse real quizzes |
| **4** | Runner (timer, selection memory, leave confirm), result (score ring, review, wrong-only filter), EXP refresh through `refreshExp()` | the full student loop |
| **5** | Admin panel: `QUIZZES` tab list + full-screen editor with autosave draft, publish/archive, duplicate | an admin can author on the phone end to end |
| **6** | Room v8 (`quiz_result_cache`, `quiz_draft`), best-score badges, retake flow, quiz insights (attempts, average, hardest question) | offline tolerance + teacher feedback |

Order matters: phase 5 could ship before 3/4, but the reviewer of the builder is
the person who also takes the quiz — building the student side first means the
admin can test what they wrote immediately.

Every phase is one commit (plus fixes) and updates this document as it lands.

**Deploy checklist (once, on the live server):** import `database_quizzes.sql`,
then (optionally) `seed_quizzes.sql` for the three starter quizzes,
upload `api/quizzes.php`, add the two EXP kinds to `_exp.php`. No new web pages —
authoring is in the app.

## 11. Test plan

- API: list filters, absence of `is_correct` in list/get, grading matrix
  (all right / all wrong / partial / unanswered), retake pays once, double submit
  pays once, cap trimming, archive hides the quiz.
- App: hub empty vs populated, skeleton, timer auto-submit, leaving mid-quiz,
  back from result to hub, restored draft after process death, EXP number matches
  the server after submit (never summed on the phone).
- Visual: 360 dp and 412 dp widths; long Sinhala/Tamil prompts; 4-option and
  long-option wrapping; header clears the status bar.

---

## 12. Remaining small choices (defaults proposed)

1. **Pass mark** — 60% default per quiz, editable in the builder.
2. **Timer** — off by default; when on, the quiz auto-submits at zero and the
   remaining time is shown as a pill.
3. **Question images** — allowed, uploaded to `uploads/quiz/`, same validation as
   paper PDFs.
4. **Question count per quiz** — no hard limit; the builder warns above 40 because
   a phone quiz that long is rarely finished.
5. **Grade targeting** — one quiz belongs to one grade (chips in the builder), not
   a range. A quiz for Grades 12 and 13 is created twice, or with grade "A/L".
6. **Deleting a published quiz with attempts** — refused; the admin archives it
   instead, so no student loses their score history.

Say the word if any default is wrong; otherwise phase 1 starts from this document
as written.

---

## 13. What shipped

| Layer | File | Notes |
| --- | --- | --- |
| Schema | `backend/database_quizzes.sql` | four tables, nothing seeded |
| API | `backend/api/quizzes.php` | student read path + admin write path, grading server-side |
| EXP | `backend/api/_exp.php` | `quiz_passed` 20, `quiz_perfect` 10 |
| Models | `app/.../data/model/QuizModels.kt` | summaries, attempt, result, review, authoring draft |
| Wire | `app/.../data/remote/QuizzesApiService.kt` | Moshi DTOs + envelope |
| Data | `app/.../data/remote/QuizzesRepository.kt` | `QuizOutcome.Ok/Err`, offline kept separate |
| Local | `TheStudentEntities/Dao/Database` v8 | `quiz_results` mirror, `MIGRATION_7_8` |
| State | `TheStudentViewModel` | quizzes, attempt, answers, result, admin list, saving |
| Screens | `QuizzesScreen.kt`, `QuizRunnerScreen.kt`, `QuizAdminScreen.kt` | hub, runner + result, list + builder |
| Entry | `PapersScreen.kt` | `Papers \| Quizzes` segments under the existing header |
| Admin | `AdminPanelScreen.kt` | new `QUIZZES` tab, scrollable tab row |

Deviations from the plan, both deliberate:

1. **One `quiz_results` table, not three.** Attempt answers live in the server's
   `quiz_attempts`; the phone only mirrors best scores. A local attempt draft was
   dropped because the runner keeps its answers in the view model for the length
   of a session, which is how long an attempt realistically lives.
2. **The builder stores its draft in SharedPreferences, not Room.** One JSON
   document per quiz, keyed `new_quiz` / `quiz_{id}`, deleted on success. It
   survives process death, which is all the plan promised, without a schema
   migration for a document the app reads exactly once.

Still to do before students see it: import `database_quizzes.sql`, then
`seed_quizzes.sql` for the three starter quizzes, and upload
`api/quizzes.php` (see the deploy checklist in §10). The app shows "Quizzes are
not set up on this server yet" until it is there.

---

## Design references

- `designs/18-quiz-hub.png` — the quiz list (cards, badges, best-score chips).
- `designs/18-quiz-runner.png` — one question at a time, selected option in ink,
  timer pill, progress bar.
- `designs/1x-papers-screen.png` — the header the segment control sits under; the
  hub mockup predates the "second segment" decision, so in the app the
  `Papers | Quizzes` pills replace its standalone header.

---

---

---

## 14. Starter quizzes (`backend/seed_quizzes.sql`)

Three real quizzes ship as an optional SQL seed, so a fresh install is not an
empty screen. They are ordinary rows — the same ones the in-app builder writes —
and can be edited or deleted from **Admin panel → Quizzes** afterwards.

| # | Quiz | Subject / grade | Category | Time | Questions |
| --- | --- | --- | --- | --- | --- |
| 1 | Current Electricity — O/L Science | Science, Grade 11 | O/L | 15 min | 10 |
| 2 | Magnetism & Electromagnetic Induction — A/L Physics | Physics, Grade 13 (Physical Science) | A/L | 20 min | 10 |
| 3 | Nutrition & Digestion — Grade 9 Science | Science, Grade 9 | Junior | 12 min | 10 |

Content basis:

- **Quiz 1** follows the NIE Grade 10/11 Science syllabus unit *"uses fundamental
  principles and laws of current electricity to understand and control the action
  of simple circuits"* — current and its unit, e.m.f. and potential difference,
  Ohm's law, the factors affecting resistance, series and parallel combinations,
  electrical power, and household wiring safety with kW h as the billing unit.
- **Quiz 2** covers the A/L Physics magnetism/induction unit: flux and flux
  density and their units, F = BIl, Faraday's law, Lenz's law and eddy currents,
  the ideal transformer equation and Fleming's left-hand rule. Two numerical
  items (flux and induced emf) are included so the quiz tests arithmetic, not only
  recall.
- **Quiz 3** covers the junior secondary nutrition unit: organs and enzymes of
  digestion, absorption in the small and large intestine, roughage, protein
  sources, and the deficiency diseases (scurvy, goitre, kwashiorkor) that the
  Grade 8–9 syllabus names.

Every question has exactly four options, one correct answer, and a worked
explanation that the app reveals after grading. Distractors are plausible
misconceptions (for example "resistance increases when the wire is made thicker",
"current flows from the negative terminal", "bile is produced in the gall
bladder") rather than random wrong answers.

Import order and notes:

1. `database_quizzes.sql` — creates the four tables.
2. `seed_quizzes.sql` — inserts the three quizzes as `published`. Safe to re-run:
   each quiz is deleted by its stable `code` first, and the table's foreign keys
   cascade to its questions and options.

Because the seed deletes by `code`, re-running it after students have attempted a
quiz would also remove those attempts (`quiz_attempts` cascades). Run it once on
a new server, or edit the quizzes in the app after that.

---

## 15. Attempt limits and "enrolled" quizzes

A quiz is not endlessly retakeable. Each quiz carries `max_attempts` (1–10,
default 3) and the **server** enforces it:

- `start` counts the student's submitted attempts and refuses a *new* one with
  `409` once the allowance is gone. An attempt that is already open can still be
  resumed and finished — the limit is on attempts, not on sessions.
- `list` returns `max_attempts`, `attempts_used`, `attempts_left` and `enrolled`
  for the signed-in student, computed from `quiz_attempts`, so a card can never
  disagree with what `start` will allow.
- `submit` returns `attempts_left` and `max_attempts` with the graded result, so
  the result page knows whether "Try again" is still meaningful.

In the app, an **enrolled** quiz (`attempts_left == 0`):

- is listed under the `Enrolled (n)` chip and sorts to the bottom of the hub;
- renders dimmed with a lock and the "Enrolled · N of N attempts used" badges;
- is not tappable (the `Surface` is disabled, and `openQuiz` refuses it as a
  second guard);
- on the result page, "Try again" is disabled and replaced with "No attempts
  left", with the note "All N attempts used · added to your enrolled quizzes".

EXP is unaffected: a pass pays once per quiz whatever the attempt number, and
retakes never pay again.

**Giving attempts back.** The admin panel's quiz row has *Reset attempts* (shown
only when students have submitted), which deletes the attempts for that quiz so
everyone can take it again. It is confirmation-guarded because best scores and
quiz badges go with the attempts.

**Deploying this change on an existing server:**

1. import `backend/database_quizzes_v2.sql` — adds the column idempotently
   (it checks `information_schema` first, so re-running is safe);
2. re-upload `backend/api/quizzes.php` (now `1.1.0`).

Without the migration the API still behaves as three attempts per quiz: the
column is read as `3` when absent, and `admin_save` simply omits it (see
`ts_quiz_has_max_attempts`). Run the migration to make the per-quiz setting
editable.

---

## 16. Progress (`action=progress`, API `1.2.0`)

The quizzes were complete but silent: a student could take all three and the
app still showed the same Home card, `Circular Motion · Lesson 6 of 12 · 38%`,
which was mock data from `SampleEducationalData`. **Your progress** replaces it
with numbers that come from submitted attempts.

### The rule

Nothing on the progress page is stored as a statistic. Every value is derived,
on every request, from `quiz_attempts` joined to `quizzes`:

| Value | How it is computed |
| --- | --- |
| `average_percent` | `SUM(score) / SUM(max_score)` over all submitted attempts — total marks, not the mean of percentages, so a 10-mark and a 20-mark quiz weigh honestly |
| `best_percent` | highest single attempt `percent` |
| `quizzes_passed` | distinct quizzes with at least one `passed` attempt |
| `quizzes_published` | `COUNT(*)` of published quizzes |
| `duration_seconds` | sum of attempt durations — surfaced as `1h 42m`, never raw seconds |
| `exp_from_quizzes` | sum of `exp_awarded` actually paid by the ledger |
| `streak_days` | consecutive server-local dates with a submitted attempt, counted back from today, **yesterday-tolerant** so a streak is not lost before the student studies today |
| `subjects[].accuracy` | the same sum-of-marks arithmetic, per subject |
| `subjects[]` order | **weakest first** — the top of the list is what to work on |

A deleted quiz, a reset attempt or an archived quiz moves all of it, because
there is no second copy to drift.

### `practice` — the "practise this next" deep link

The server picks it, so the app never has to guess:

1. only quizzes that are `published` **and still have attempts left** are
   eligible (an enrolled quiz cannot be suggested — the button would be dead);
2. among those, the weakest by accuracy (`-1` when never attempted);
3. if the student has attempted nothing, the first available quiz is offered
   instead of nothing.

The app then opens that quiz through the normal `openQuiz` path, so attempt
limits, EXP and the runner behave exactly as they do from the hub.

### App surface

- **`ProgressScreen`** (`ui/screens/ProgressScreen.kt`) — hero average with a
  passed-quizzes bar, three stat tiles (streak · best · EXP), subject accuracy
  bars tinted by strength (≥75 strong, ≥50 steady, else amber "work on this"),
  the practise card, and the last five attempts with pass/fail chips.
  Tags: `progress_content`, `progress_practice`, `progress_practice_button`,
  `progress_subject_*`, `progress_refresh`, `progress_empty`, `progress_back`.
- **Profile → "Your progress"** row (`profile_row_progress`) and a
  **My progress** pill on the quizzes hub (`quizzes_open_progress`).
- **Home → Continue Learning** now reads the same snapshot: the weakest quiz
  that is still open, its accuracy, the average in the corner and the streak in
  the badge. With no attempts it says *Take your first quiz* rather than
  inventing a half-finished lesson. Tags: `continue_learning_card`,
  `resume_lesson_button`, `home_open_progress`.
- The view model holds `quizProgress` and reloads it after every graded submit,
  so finishing a quiz immediately moves Home and the progress page.

### Deploying

Re-upload `backend/api/quizzes.php` (`1.2.0`). No schema change — the table
already records everything the page needs.
