<?php
/**
 * TheStudent — Quizzes API
 * ========================
 *
 * Students read published quizzes, start an attempt, submit it and get it graded
 * here. Admins (the in-app admin panel) create, edit, publish and archive them.
 *
 * Two rules shape this file:
 *
 *   1. **`is_correct` never leaves the server** on any student-facing action. The
 *      app cannot mark its own paper, so opening the APK with a proxy or a
 *      debugger reveals nothing.
 *   2. **EXP is paid by the ledger, once.** `ts_exp_award()` is called with a
 *      ref key built from the quiz id, so retakes, double taps and retried
 *      submits cannot pay twice, and the daily ceiling still applies.
 *
 * Deploy: import `database_quizzes.sql` beside this file, then upload this file
 * to `public_html/api/quizzes.php`.
 */

declare(strict_types=1);

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/_admin_guard.php';   // bearer token, json error, admin guard
require_once __DIR__ . '/_exp.php';           // the EXP ledger

const TS_QUIZ_API_VERSION = '1.0.0';
const TS_QUIZ_EXP_MAX_REWARD = 40;
const TS_QUIZ_MAX_QUESTIONS = 200;

// ---------------------------------------------------------------------------
// Plumbing
// ---------------------------------------------------------------------------

function ts_quiz_json(string $status, string $message, array $data = [], int $code = 200): void
{
    http_response_code($code);
    if (!headers_sent()) {
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        header('X-Quizzes-Api: ' . TS_QUIZ_API_VERSION);
    }
    echo json_encode(
        [
            'status' => $status,
            'message' => $message,
            'api_version' => TS_QUIZ_API_VERSION,
            'data' => $data,
        ],
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
    );
    exit();
}

function ts_quiz_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return $_POST;
    }
    $decoded = json_decode($raw, true);
    if (!is_array($decoded)) {
        ts_quiz_json('error', 'Malformed JSON body.', [], 400);
    }
    return $decoded;
}

/** The signed-in student, or a 401. */
function ts_quiz_user(PDO $pdo): array
{
    $token = ts_api_bearer_token();
    if ($token === null) {
        ts_quiz_json('error', 'Sign in to take quizzes.', [], 401);
    }

    $stmt = $pdo->prepare(
        'SELECT u.* FROM user_tokens t
         JOIN users u ON u.id = t.user_id
         WHERE t.token = :hash AND t.expires_at > NOW()
         LIMIT 1'
    );
    $stmt->execute(['hash' => hash('sha256', $token)]);
    $user = $stmt->fetch();

    if (!$user) {
        ts_quiz_json('error', 'Session expired. Please sign in again.', [], 401);
    }
    if ((int) ($user['is_active'] ?? 0) !== 1) {
        ts_quiz_json('error', 'This account has been disabled.', [], 403);
    }
    return $user;
}

/** True when every quiz table exists. Cached per request. */
function ts_quiz_ready(PDO $pdo): bool
{
    static $ready = null;
    if ($ready !== null) {
        return $ready;
    }
    try {
        $pdo->query('SELECT 1 FROM quiz_options LIMIT 1');
        $ready = true;
    } catch (Throwable $e) {
        $ready = false;
    }
    return $ready;
}

function ts_quiz_require_ready(PDO $pdo): void
{
    if (ts_quiz_ready($pdo)) {
        return;
    }
    ts_quiz_json(
        'error',
        'Quizzes are not set up on this server yet. Import database_quizzes.sql.',
        [],
        503
    );
}

function ts_quiz_public(array $row, int $best = -1, bool $passed = false, int $attempts = 0): array
{
    $minutes = (int) round(((int) $row['time_limit_seconds']) / 60);
    return [
        'id' => (int) $row['id'],
        'code' => (string) $row['code'],
        'title' => (string) $row['title'],
        'description' => (string) ($row['description'] ?? ''),
        'subject' => (string) $row['subject'],
        'grade' => (string) $row['grade'],
        'category' => (string) $row['category'],
        'stream' => $row['stream'] ?? null,
        'medium' => (string) $row['medium'],
        'minutes' => $minutes,
        'time_limit_seconds' => (int) $row['time_limit_seconds'],
        'pass_percent' => (int) $row['pass_percent'],
        'shuffle_questions' => (int) $row['shuffle_questions'] === 1,
        'shuffle_options' => (int) $row['shuffle_options'] === 1,
        'exp_reward' => ts_quiz_reward($row),
        'status' => (string) $row['status'],
        'question_count' => (int) ($row['question_count'] ?? 0),
        'best_percent' => $best >= 0 ? $best : null,
        'passed' => $passed,
        'attempts' => $attempts,
    ];
}

/** A stable, URL-safe id for a new quiz ("physics-magnetism-a1b2c"). */
function ts_quiz_slug(string $title): string
{
    $slug = strtolower($title);
    $slug = preg_replace('/[^a-z0-9]+/', '-', $slug) ?? '';
    $slug = trim($slug, '-');
    if ($slug === '') {
        $slug = 'quiz';
    }
    if (strlen($slug) > 40) {
        $slug = rtrim(substr($slug, 0, 40), '-');
    }
    return $slug . '-' . substr(bin2hex(random_bytes(3)), 0, 5);
}

/** Clamped EXP for a first pass on this quiz. */
function ts_quiz_reward(array $row): int
{
    $reward = (int) ($row['exp_reward'] ?? 20);
    return max(0, min(TS_QUIZ_EXP_MAX_REWARD, $reward));
}

/** The `exp` block the app already knows how to read (see chat.php). */
function ts_quiz_exp_block(PDO $pdo, int $userId): array
{
    $total = ts_exp_total($pdo, $userId);
    return [
        'total' => $total,
        'today' => ts_exp_today($pdo, $userId),
        'daily_cap' => ts_exp_cap_for_total($total),
        'tier' => ts_exp_tier($total),
    ];
}

/** Questions + options for one quiz, ordered by position. */
function ts_quiz_questions(PDO $pdo, int $quizId, bool $withAnswers): array
{
    $stmt = $pdo->prepare(
        'SELECT id, position, prompt, media_url, explanation, marks
         FROM quiz_questions WHERE quiz_id = :id ORDER BY position ASC, id ASC'
    );
    $stmt->execute(['id' => $quizId]);
    $questions = $stmt->fetchAll();

    if (!$questions) {
        return [];
    }

    $ids = array_map(static fn ($q) => (int) $q['id'], $questions);
    $placeholders = implode(',', array_fill(0, count($ids), '?'));
    $optStmt = $pdo->prepare(
        "SELECT id, question_id, position, label, is_correct
         FROM quiz_options WHERE question_id IN ($placeholders)
         ORDER BY position ASC, id ASC"
    );
    $optStmt->execute($ids);
    $options = $optStmt->fetchAll();

    $byQuestion = [];
    foreach ($options as $option) {
        $qid = (int) $option['question_id'];
        $entry = [
            'id' => (int) $option['id'],
            'label' => (string) $option['label'],
        ];
        if ($withAnswers) {
            $entry['is_correct'] = (int) $option['is_correct'] === 1;
        }
        $byQuestion[$qid][] = $entry;
    }

    $out = [];
    foreach ($questions as $question) {
        $qid = (int) $question['id'];
        $entry = [
            'id' => $qid,
            'position' => (int) $question['position'],
            'prompt' => (string) $question['prompt'],
            'media_url' => $question['media_url'] ?? null,
            'marks' => max(1, (int) $question['marks']),
            'options' => $byQuestion[$qid] ?? [],
        ];
        if ($withAnswers) {
            $entry['explanation'] = $question['explanation'] ?? null;
        }
        $out[] = $entry;
    }
    return $out;
}

/** Grade a set of answers against the stored questions. */
function ts_quiz_grade(array $questions, array $answers): array
{
    $chosen = [];
    foreach ($answers as $answer) {
        if (!is_array($answer)) {
            continue;
        }
        $qid = (int) ($answer['question_id'] ?? 0);
        $oid = (int) ($answer['option_id'] ?? 0);
        if ($qid > 0 && $oid > 0) {
            $chosen[$qid] = $oid;
        }
    }

    $score = 0;
    $max = 0;
    $review = [];
    $answered = 0;

    foreach ($questions as $question) {
        $qid = (int) $question['id'];
        $marks = max(1, (int) $question['marks']);
        $max += $marks;

        $correctId = 0;
        $correctLabel = '';
        $chosenLabel = '';
        foreach ($question['options'] as $option) {
            if (!empty($option['is_correct'])) {
                $correctId = (int) $option['id'];
                $correctLabel = (string) $option['label'];
            }
            if (isset($chosen[$qid]) && (int) $option['id'] === $chosen[$qid]) {
                $chosenLabel = (string) $option['label'];
            }
        }

        $isCorrect = $correctId > 0 && isset($chosen[$qid]) && $chosen[$qid] === $correctId;
        if ($isCorrect) {
            $score += $marks;
        }
        if (isset($chosen[$qid])) {
            $answered++;
        }

        $review[] = [
            'question_id' => $qid,
            'prompt' => (string) $question['prompt'],
            'chosen_option_id' => $chosen[$qid] ?? null,
            'chosen_label' => $chosenLabel !== '' ? $chosenLabel : null,
            'correct_option_id' => $correctId,
            'correct_label' => $correctLabel,
            'explanation' => $question['explanation'] ?? null,
            'marks' => $marks,
            'correct' => $isCorrect,
            'answered' => isset($chosen[$qid]),
        ];
    }

    $percent = $max > 0 ? (int) round(($score / $max) * 100) : 0;
    return [
        'score' => $score,
        'max_score' => $max,
        'percent' => $percent,
        'answered' => $answered,
        'review' => $review,
        'answers' => $chosen,
    ];
}

// ---------------------------------------------------------------------------
// Routing
// ---------------------------------------------------------------------------

$pdo = getDbConnection();

$method = strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET'));
$body = $method === 'POST' ? ts_quiz_body() : [];
$action = strtolower(trim((string) ($_GET['action'] ?? ($body['action'] ?? 'list'))));

try {
    switch ($action) {
        // ------------------------------------------------------------------
        // Student: browse
        // ------------------------------------------------------------------
        case 'list':
            ts_quiz_require_ready($pdo);
            $user = ts_quiz_user($pdo);

            $stmt = $pdo->query(
                "SELECT q.*, (SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id = q.id) AS question_count
                 FROM quizzes q
                 WHERE q.status = 'published'
                 ORDER BY q.subject ASC, q.title ASC"
            );
            $rows = $stmt->fetchAll();

            $mine = [];
            $bestStmt = $pdo->prepare(
                'SELECT quiz_id, MAX(percent) AS best, MAX(passed) AS passed, COUNT(*) AS attempts
                 FROM quiz_attempts
                 WHERE user_id = :uid AND submitted_at IS NOT NULL
                 GROUP BY quiz_id'
            );
            $bestStmt->execute(['uid' => (int) $user['id']]);
            foreach ($bestStmt->fetchAll() as $row) {
                $mine[(int) $row['quiz_id']] = $row;
            }

            $out = [];
            foreach ($rows as $row) {
                $qid = (int) $row['id'];
                $stat = $mine[$qid] ?? null;
                $out[] = ts_quiz_public(
                    $row,
                    $stat === null ? -1 : (int) $stat['best'],
                    $stat !== null && (int) $stat['passed'] === 1,
                    $stat === null ? 0 : (int) $stat['attempts']
                );
            }
            ts_quiz_json('success', 'Quizzes loaded.', ['quizzes' => $out]);

            // no break — ts_quiz_json exits
        case 'state':
            ts_quiz_require_ready($pdo);
            $user = ts_quiz_user($pdo);
            $uid = (int) $user['id'];

            $stmt = $pdo->prepare(
                'SELECT COUNT(*) AS attempts, COALESCE(MAX(percent), 0) AS best,
                        COALESCE(AVG(percent), 0) AS average, MAX(passed) AS passed
                 FROM quiz_attempts WHERE user_id = :uid AND submitted_at IS NOT NULL'
            );
            $stmt->execute(['uid' => $uid]);
            $row = $stmt->fetch() ?: [];

            $quizCount = (int) $pdo->query("SELECT COUNT(*) FROM quizzes WHERE status = 'published'")->fetchColumn();

            $quizExp = 0;
            if (ts_exp_table_ready($pdo)) {
                $expStmt = $pdo->prepare(
                    "SELECT COALESCE(SUM(amount), 0) FROM user_exp
                     WHERE user_id = :uid AND kind IN ('quiz_passed', 'quiz_perfect')"
                );
                $expStmt->execute(['uid' => $uid]);
                $quizExp = (int) $expStmt->fetchColumn();
            }

            ts_quiz_json('success', 'Quiz state loaded.', [
                'published' => $quizCount,
                'attempts' => (int) ($row['attempts'] ?? 0),
                'best' => (int) ($row['best'] ?? 0),
                'average' => (int) round((float) ($row['average'] ?? 0)),
                'passed' => (int) ($row['passed'] ?? 0) === 1,
                'exp_from_quizzes' => $quizExp,
                'exp' => ts_quiz_exp_block($pdo, $uid),
            ]);

        case 'get':
            ts_quiz_require_ready($pdo);
            ts_quiz_user($pdo);
            $id = (int) ($_GET['id'] ?? 0);
            if ($id <= 0) {
                ts_quiz_json('error', 'Which quiz?', [], 400);
            }
            $stmt = $pdo->prepare(
                "SELECT q.*, (SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id = q.id) AS question_count
                 FROM quizzes q WHERE q.id = :id AND q.status = 'published' LIMIT 1"
            );
            $stmt->execute(['id' => $id]);
            $row = $stmt->fetch();
            if (!$row) {
                ts_quiz_json('error', 'That quiz is no longer available.', [], 404);
            }
            ts_quiz_json('success', 'Quiz loaded.', [
                'quiz' => ts_quiz_public($row),
                'questions' => ts_quiz_questions($pdo, $id, false),
            ]);

        // ------------------------------------------------------------------
        // Student: attempt
        // ------------------------------------------------------------------
        case 'start':
            ts_quiz_require_ready($pdo);
            if ($method !== 'POST') {
                ts_quiz_json('error', 'Only POST requests are allowed.', [], 405);
            }
            $user = ts_quiz_user($pdo);
            $uid = (int) $user['id'];
            $quizId = (int) ($body['quiz_id'] ?? 0);
            if ($quizId <= 0) {
                ts_quiz_json('error', 'Which quiz?', [], 400);
            }

            $stmt = $pdo->prepare(
                "SELECT q.*, (SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id = q.id) AS question_count
                 FROM quizzes q WHERE q.id = :id AND q.status = 'published' LIMIT 1"
            );
            $stmt->execute(['id' => $quizId]);
            $quiz = $stmt->fetch();
            if (!$quiz) {
                ts_quiz_json('error', 'That quiz is no longer available.', [], 404);
            }

            $questions = ts_quiz_questions($pdo, $quizId, false);
            if (!$questions) {
                ts_quiz_json('error', 'This quiz has no questions yet.', [], 409);
            }

            // Resume an attempt started in the last three hours instead of
            // piling up abandoned rows every time the screen is reopened.
            $open = $pdo->prepare(
                'SELECT * FROM quiz_attempts
                 WHERE quiz_id = :qid AND user_id = :uid AND submitted_at IS NULL
                   AND started_at > (NOW() - INTERVAL 3 HOUR)
                 ORDER BY id DESC LIMIT 1'
            );
            $open->execute(['qid' => $quizId, 'uid' => $uid]);
            $attempt = $open->fetch();

            if (!$attempt) {
                $insert = $pdo->prepare('INSERT INTO quiz_attempts (quiz_id, user_id) VALUES (:qid, :uid)');
                $insert->execute(['qid' => $quizId, 'uid' => $uid]);
                $attemptId = (int) $pdo->lastInsertId();
                $startedAt = time();
            } else {
                $attemptId = (int) $attempt['id'];
                $startedAt = strtotime((string) $attempt['started_at']) ?: time();
            }

            if ((int) $quiz['shuffle_questions'] === 1) {
                shuffle($questions);
            }
            if ((int) $quiz['shuffle_options'] === 1) {
                foreach ($questions as &$question) {
                    shuffle($question['options']);
                }
                unset($question);
            }

            $limit = (int) $quiz['time_limit_seconds'];
            ts_quiz_json('success', 'Attempt started.', [
                'attempt_id' => $attemptId,
                'quiz' => ts_quiz_public($quiz),
                'questions' => $questions,
                'started_at' => $startedAt,
                'deadline_at' => $limit > 0 ? $startedAt + $limit : 0,
                'server_time' => time(),
            ]);

        case 'submit':
            ts_quiz_require_ready($pdo);
            if ($method !== 'POST') {
                ts_quiz_json('error', 'Only POST requests are allowed.', [], 405);
            }
            $user = ts_quiz_user($pdo);
            $uid = (int) $user['id'];
            $attemptId = (int) ($body['attempt_id'] ?? 0);
            $answers = $body['answers'] ?? [];
            if (!is_array($answers)) {
                $answers = [];
            }
            if ($attemptId <= 0) {
                ts_quiz_json('error', 'Which attempt?', [], 400);
            }

            $stmt = $pdo->prepare('SELECT * FROM quiz_attempts WHERE id = :id AND user_id = :uid LIMIT 1');
            $stmt->execute(['id' => $attemptId, 'uid' => $uid]);
            $attempt = $stmt->fetch();
            if (!$attempt) {
                ts_quiz_json('error', 'That attempt was not found.', [], 404);
            }

            $quizStmt = $pdo->prepare('SELECT * FROM quizzes WHERE id = :id LIMIT 1');
            $quizStmt->execute(['id' => (int) $attempt['quiz_id']]);
            $quiz = $quizStmt->fetch();
            if (!$quiz) {
                ts_quiz_json('error', 'That quiz was removed.', [], 404);
            }

            $questions = ts_quiz_questions($pdo, (int) $quiz['id'], true);
            if (!$questions) {
                ts_quiz_json('error', 'This quiz has no questions.', [], 409);
            }

            // Already submitted: replay the stored result, pay nothing.
            if ($attempt['submitted_at'] !== null) {
                $stored = json_decode((string) $attempt['answers_json'], true);
                $storedAnswers = is_array($stored) ? ($stored['answers'] ?? []) : [];
                $graded = ts_quiz_grade($questions, is_array($storedAnswers) ? $storedAnswers : []);
                ts_quiz_json('success', 'This attempt was already graded.', [
                    'result' => [
                        'attempt_id' => $attemptId,
                        'quiz_id' => (int) $quiz['id'],
                        'quiz_title' => (string) $quiz['title'],
                        'score' => (int) $attempt['score'],
                        'max_score' => (int) $attempt['max_score'],
                        'percent' => (int) $attempt['percent'],
                        'passed' => (int) $attempt['passed'] === 1,
                        'pass_percent' => (int) $quiz['pass_percent'],
                        'duration_seconds' => (int) $attempt['duration_seconds'],
                        'exp_awarded' => 0,
                        'answered' => $graded['answered'],
                        'review' => $graded['review'],
                    ],
                    'exp' => ts_quiz_exp_block($pdo, $uid),
                ]);
            }

            $duration = max(0, time() - (strtotime((string) $attempt['started_at']) ?: time()));
            if ($duration < 3 && count($questions) >= 3) {
                ts_quiz_json('error', 'That was too quick to be a real attempt. Please try again.', [], 400);
            }
            $limit = (int) $quiz['time_limit_seconds'];
            if ($limit > 0 && $duration > $limit + 30) {
                $duration = $limit;
            }

            $graded = ts_quiz_grade($questions, $answers);
            $percent = (int) $graded['percent'];
            $passed = $percent >= (int) $quiz['pass_percent'];

            $update = $pdo->prepare(
                'UPDATE quiz_attempts
                 SET submitted_at = NOW(), score = :score, max_score = :max, percent = :percent,
                     passed = :passed, duration_seconds = :duration, answers_json = :answers
                 WHERE id = :id'
            );
            // Stored as a list of {question_id, option_id} so a replayed result can
            // be re-graded from the database alone.
            $answerList = [];
            foreach ($graded['answers'] as $qid => $oid) {
                $answerList[] = ['question_id' => (int) $qid, 'option_id' => (int) $oid];
            }

            $update->execute([
                'score' => (int) $graded['score'],
                'max' => (int) $graded['max_score'],
                'percent' => $percent,
                'passed' => $passed ? 1 : 0,
                'duration' => $duration,
                'answers' => json_encode(['answers' => $answerList], JSON_UNESCAPED_UNICODE),
                'id' => $attemptId,
            ]);

            // EXP — the ledger decides whether this pays, not the app.
            $awarded = 0;
            $reward = ts_quiz_reward($quiz);
            if ($reward > 0 && $passed) {
                $awarded += ts_exp_award(
                    $pdo,
                    $uid,
                    'quiz_passed',
                    'quiz:' . (int) $quiz['id'] . ':pass',
                    'Passed · ' . (string) $quiz['title'] . ' (' . $percent . '%)',
                    $reward
                );
            }
            if ($reward > 0 && $percent === 100) {
                $bonus = (int) round($reward / 2);
                if ($bonus > 0) {
                    $awarded += ts_exp_award(
                        $pdo,
                        $uid,
                        'quiz_perfect',
                        'quiz:' . (int) $quiz['id'] . ':perfect',
                        'Perfect score · ' . (string) $quiz['title'],
                        $bonus
                    );
                }
            }

            if ($awarded > 0) {
                $pdo->prepare('UPDATE quiz_attempts SET exp_awarded = :exp WHERE id = :id')
                    ->execute(['exp' => $awarded, 'id' => $attemptId]);
            }

            ts_quiz_json('success', 'Attempt graded.', [
                'result' => [
                    'attempt_id' => $attemptId,
                    'quiz_id' => (int) $quiz['id'],
                    'quiz_title' => (string) $quiz['title'],
                    'score' => (int) $graded['score'],
                    'max_score' => (int) $graded['max_score'],
                    'percent' => $percent,
                    'passed' => $passed,
                    'pass_percent' => (int) $quiz['pass_percent'],
                    'duration_seconds' => $duration,
                    'exp_awarded' => $awarded,
                    'answered' => (int) $graded['answered'],
                    'review' => $graded['review'],
                ],
                'exp' => ts_quiz_exp_block($pdo, $uid),
            ]);

        case 'history':
            ts_quiz_require_ready($pdo);
            $user = ts_quiz_user($pdo);
            $stmt = $pdo->prepare(
                'SELECT a.id, a.quiz_id, a.score, a.max_score, a.percent, a.passed,
                        a.duration_seconds, a.exp_awarded, a.submitted_at, q.title, q.subject, q.grade
                 FROM quiz_attempts a
                 JOIN quizzes q ON q.id = a.quiz_id
                 WHERE a.user_id = :uid AND a.submitted_at IS NOT NULL
                 ORDER BY a.submitted_at DESC, a.id DESC
                 LIMIT 100'
            );
            $stmt->execute(['uid' => (int) $user['id']]);
            $out = [];
            foreach ($stmt->fetchAll() as $row) {
                $out[] = [
                    'attempt_id' => (int) $row['id'],
                    'quiz_id' => (int) $row['quiz_id'],
                    'quiz_title' => (string) $row['title'],
                    'subject' => (string) $row['subject'],
                    'grade' => (string) $row['grade'],
                    'score' => (int) $row['score'],
                    'max_score' => (int) $row['max_score'],
                    'percent' => (int) $row['percent'],
                    'passed' => (int) $row['passed'] === 1,
                    'duration_seconds' => (int) $row['duration_seconds'],
                    'exp_awarded' => (int) $row['exp_awarded'],
                    'submitted_at' => (string) $row['submitted_at'],
                ];
            }
            ts_quiz_json('success', 'History loaded.', ['attempts' => $out]);

        // ------------------------------------------------------------------
        // Admin: authoring (in-app admin panel)
        // ------------------------------------------------------------------
        case 'admin_list':
            ts_quiz_require_ready($pdo);
            ts_require_admin_api($pdo);
            $stmt = $pdo->query(
                "SELECT q.*,
                        (SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id = q.id) AS question_count,
                        (SELECT COUNT(*) FROM quiz_attempts qa WHERE qa.quiz_id = q.id AND qa.submitted_at IS NOT NULL) AS attempt_count,
                        (SELECT COALESCE(AVG(qa.percent), 0) FROM quiz_attempts qa WHERE qa.quiz_id = q.id AND qa.submitted_at IS NOT NULL) AS average_percent
                 FROM quizzes q ORDER BY q.status ASC, q.updated_at DESC, q.id DESC"
            );
            $out = [];
            foreach ($stmt->fetchAll() as $row) {
                $entry = ts_quiz_public($row);
                $entry['attempt_count'] = (int) ($row['attempt_count'] ?? 0);
                $entry['average_percent'] = (int) round((float) ($row['average_percent'] ?? 0));
                $out[] = $entry;
            }
            ts_quiz_json('success', 'Quizzes loaded.', ['quizzes' => $out]);

        case 'admin_get':
            ts_quiz_require_ready($pdo);
            ts_require_admin_api($pdo);
            $id = (int) ($_GET['id'] ?? 0);
            if ($id <= 0) {
                ts_quiz_json('error', 'Which quiz?', [], 400);
            }
            $stmt = $pdo->prepare('SELECT * FROM quizzes WHERE id = :id LIMIT 1');
            $stmt->execute(['id' => $id]);
            $row = $stmt->fetch();
            if (!$row) {
                ts_quiz_json('error', 'That quiz does not exist.', [], 404);
            }
            ts_quiz_json('success', 'Quiz loaded.', [
                'quiz' => ts_quiz_public($row),
                'questions' => ts_quiz_questions($pdo, $id, true),
            ]);

        case 'admin_save':
            ts_quiz_require_ready($pdo);
            if ($method !== 'POST') {
                ts_quiz_json('error', 'Only POST requests are allowed.', [], 405);
            }
            $admin = ts_require_admin_api($pdo);

            $id = (int) ($body['id'] ?? 0);
            $title = trim((string) ($body['title'] ?? ''));
            $subject = trim((string) ($body['subject'] ?? ''));
            $grade = trim((string) ($body['grade'] ?? ''));
            $category = trim((string) ($body['category'] ?? ''));
            $questions = $body['questions'] ?? [];

            if (mb_strlen($title) < 3) {
                ts_quiz_json('error', 'Give the quiz a title (at least 3 characters).', [], 400);
            }
            if ($subject === '') {
                ts_quiz_json('error', 'Choose a subject.', [], 400);
            }
            if ($grade === '') {
                ts_quiz_json('error', 'Choose a grade.', [], 400);
            }
            if ($category === '') {
                ts_quiz_json('error', 'Choose a category.', [], 400);
            }
            if (!is_array($questions) || count($questions) === 0) {
                ts_quiz_json('error', 'A quiz needs at least one question.', [], 400);
            }
            if (count($questions) > TS_QUIZ_MAX_QUESTIONS) {
                ts_quiz_json('error', 'A quiz cannot have more than ' . TS_QUIZ_MAX_QUESTIONS . ' questions.', [], 400);
            }

            $cleanQuestions = [];
            foreach ($questions as $index => $question) {
                $number = $index + 1;
                if (!is_array($question)) {
                    ts_quiz_json('error', 'Question ' . $number . ' is malformed.', [], 400);
                }
                $prompt = trim((string) ($question['prompt'] ?? ''));
                if ($prompt === '') {
                    ts_quiz_json('error', 'Question ' . $number . ' has no question text.', [], 400);
                }
                $options = $question['options'] ?? [];
                if (!is_array($options) || count($options) < 2) {
                    ts_quiz_json('error', 'Question ' . $number . ' needs at least two options.', [], 400);
                }
                $cleanOptions = [];
                $correctCount = 0;
                foreach ($options as $optionIndex => $option) {
                    $label = trim((string) (is_array($option) ? ($option['label'] ?? '') : $option));
                    if ($label === '') {
                        ts_quiz_json(
                            'error',
                            'Question ' . $number . ', option ' . ((int) $optionIndex + 1) . ' is empty.',
                            [],
                            400
                        );
                    }
                    $isCorrect = is_array($option) && !empty($option['is_correct']);
                    if ($isCorrect) {
                        $correctCount++;
                    }
                    $cleanOptions[] = ['label' => $label, 'is_correct' => $isCorrect];
                }
                if ($correctCount !== 1) {
                    ts_quiz_json(
                        'error',
                        'Question ' . $number . ' must have exactly one correct answer.',
                        [],
                        400
                    );
                }
                $cleanQuestions[] = [
                    'prompt' => $prompt,
                    'media_url' => trim((string) ($question['media_url'] ?? '')) ?: null,
                    'explanation' => trim((string) ($question['explanation'] ?? '')) ?: null,
                    'marks' => max(1, min(3, (int) ($question['marks'] ?? 1))),
                    'options' => $cleanOptions,
                ];
            }

            $status = strtolower(trim((string) ($body['status'] ?? 'draft')));
            if (!in_array($status, ['draft', 'published', 'archived'], true)) {
                $status = 'draft';
            }
            if ($status === 'published') {
                // Publishing with no questions is already impossible above.
                $status = 'published';
            }

            $payload = [
                'title' => $title,
                'description' => trim((string) ($body['description'] ?? '')),
                'subject' => $subject,
                'grade' => $grade,
                'category' => $category,
                'stream' => trim((string) ($body['stream'] ?? '')) ?: null,
                'medium' => trim((string) ($body['medium'] ?? 'English')) ?: 'English',
                'time_limit_seconds' => max(0, min(7200, (int) ($body['time_limit_seconds'] ?? 0))),
                'pass_percent' => max(30, min(90, (int) ($body['pass_percent'] ?? 60))),
                'shuffle_questions' => !empty($body['shuffle_questions']) ? 1 : 0,
                'shuffle_options' => !empty($body['shuffle_options']) ? 1 : 0,
                'exp_reward' => max(0, min(TS_QUIZ_EXP_MAX_REWARD, (int) ($body['exp_reward'] ?? 20))),
                'status' => $status,
            ];

            $pdo->beginTransaction();
            try {
                if ($id > 0) {
                    $exists = $pdo->prepare('SELECT id FROM quizzes WHERE id = :id LIMIT 1');
                    $exists->execute(['id' => $id]);
                    if (!$exists->fetch()) {
                        $pdo->rollBack();
                        ts_quiz_json('error', 'That quiz does not exist.', [], 404);
                    }
                    $sets = implode(', ', array_map(static fn ($k) => "`$k` = :$k", array_keys($payload)));
                    $stmt = $pdo->prepare("UPDATE quizzes SET $sets WHERE id = :id");
                    $stmt->execute($payload + ['id' => $id]);
                    $pdo->prepare('DELETE FROM quiz_questions WHERE quiz_id = :id')->execute(['id' => $id]);
                } else {
                    $code = ts_quiz_slug($title);
                    $unique = $pdo->prepare('SELECT id FROM quizzes WHERE code = :code LIMIT 1');
                    $unique->execute(['code' => $code]);
                    if ($unique->fetch()) {
                        $code .= '-' . substr(bin2hex(random_bytes(3)), 0, 5);
                    }
                    $columns = array_keys($payload);
                    $placeholders = array_map(static fn ($c) => ":$c", $columns);
                    $stmt = $pdo->prepare(
                        'INSERT INTO quizzes (`code`, `created_by`, ' . implode(', ', array_map(static fn ($c) => "`$c`", $columns)) . ')
                         VALUES (:code, :created_by, ' . implode(', ', $placeholders) . ')'
                    );
                    $stmt->execute($payload + [
                        'code' => $code,
                        'created_by' => (int) $admin['id'],
                    ]);
                    $id = (int) $pdo->lastInsertId();
                }

                $insertQuestion = $pdo->prepare(
                    'INSERT INTO quiz_questions (quiz_id, position, prompt, media_url, explanation, marks)
                     VALUES (:quiz_id, :position, :prompt, :media_url, :explanation, :marks)'
                );
                $insertOption = $pdo->prepare(
                    'INSERT INTO quiz_options (question_id, position, label, is_correct)
                     VALUES (:question_id, :position, :label, :is_correct)'
                );

                foreach ($cleanQuestions as $position => $question) {
                    $insertQuestion->execute([
                        'quiz_id' => $id,
                        'position' => $position,
                        'prompt' => $question['prompt'],
                        'media_url' => $question['media_url'],
                        'explanation' => $question['explanation'],
                        'marks' => $question['marks'],
                    ]);
                    $questionId = (int) $pdo->lastInsertId();
                    foreach ($question['options'] as $optionPosition => $option) {
                        $insertOption->execute([
                            'question_id' => $questionId,
                            'position' => $optionPosition,
                            'label' => $option['label'],
                            'is_correct' => $option['is_correct'] ? 1 : 0,
                        ]);
                    }
                }

                $pdo->commit();
            } catch (Throwable $e) {
                if ($pdo->inTransaction()) {
                    $pdo->rollBack();
                }
                error_log('[TheStudent] quiz save failed: ' . $e->getMessage());
                ts_quiz_json('error', 'The quiz could not be saved. Please try again.', [], 500);
            }

            ts_quiz_json('success', $status === 'published' ? 'Quiz published.' : 'Quiz saved.', [
                'quiz_id' => $id,
            ]);

        case 'admin_status':
            ts_quiz_require_ready($pdo);
            if ($method !== 'POST') {
                ts_quiz_json('error', 'Only POST requests are allowed.', [], 405);
            }
            ts_require_admin_api($pdo);
            $id = (int) ($body['id'] ?? 0);
            $status = strtolower(trim((string) ($body['status'] ?? '')));
            if ($id <= 0 || !in_array($status, ['draft', 'published', 'archived'], true)) {
                ts_quiz_json('error', 'Nothing to change.', [], 400);
            }
            if ($status === 'published') {
                $count = $pdo->prepare('SELECT COUNT(*) FROM quiz_questions WHERE quiz_id = :id');
                $count->execute(['id' => $id]);
                if ((int) $count->fetchColumn() === 0) {
                    ts_quiz_json('error', 'Add at least one question before publishing.', [], 409);
                }
            }
            $pdo->prepare('UPDATE quizzes SET status = :status WHERE id = :id')
                ->execute(['status' => $status, 'id' => $id]);
            ts_quiz_json('success', 'Quiz updated.', ['quiz_id' => $id, 'status' => $status]);

        case 'admin_delete':
            ts_quiz_require_ready($pdo);
            if ($method !== 'POST') {
                ts_quiz_json('error', 'Only POST requests are allowed.', [], 405);
            }
            ts_require_admin_api($pdo);
            $id = (int) ($body['id'] ?? 0);
            if ($id <= 0) {
                ts_quiz_json('error', 'Which quiz?', [], 400);
            }
            $count = $pdo->prepare('SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id = :id');
            $count->execute(['id' => $id]);
            if ((int) $count->fetchColumn() > 0) {
                ts_quiz_json(
                    'error',
                    'Students have already attempted this quiz. Archive it instead.',
                    [],
                    409
                );
            }
            $pdo->prepare('DELETE FROM quizzes WHERE id = :id')->execute(['id' => $id]);
            ts_quiz_json('success', 'Quiz deleted.', ['quiz_id' => $id]);

        default:
            ts_quiz_json('error', 'Unknown action.', [], 400);
    }
} catch (Throwable $e) {
    error_log('[TheStudent] quizzes API failed: ' . $e->getMessage());
    ts_quiz_json('error', 'The quiz service is unavailable right now.', [], 500);
}
