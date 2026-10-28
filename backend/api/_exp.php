<?php
/**
 * TheStudent — EXP ledger
 * =======================
 *
 * Every EXP a student owns comes from one row in `user_exp`. There is no running
 * total column anywhere: the total is `SUM(amount)`, so it can always be audited,
 * and a rule can be replayed or corrected without silently rewriting history.
 *
 * Two things keep EXP honest:
 *
 *   1. **A unique key on `(user_id, ref_key)`.** Each award names the thing that
 *      earned it ("help:41:helper", "rating:17:ratee"), so a double tap, a
 *      retried request or two devices racing cannot pay twice. `INSERT IGNORE`
 *      makes the DB — not the app — the judge.
 *   2. **A daily ceiling.** A cap is checked against today's rows before every
 *      insert, so a bad day of farming cannot run away with the leaderboard. When
 *      a single award would cross the cap it is trimmed to what is left, and a
 *      trimmed award is still recorded (as the real, smaller number) so the
 *      ledger never lies about what was paid. The ceiling rises with the tier:
 *      that is the concrete reward for climbing, and it is the only one the app
 *      is allowed to promise.
 *
 * Missing table, older database, read-only account: every function here returns a
 * neutral value instead of throwing. EXP is a nice-to-have, never a reason for a
 * student to see an error.
 */

declare(strict_types=1);

/**
 * Most EXP a Learner can earn in a single day (server-local date).
 *
 * Each tier raises it — see `cap` in `TS_EXP_TIERS`. Kept as a constant so
 * anything that only knows about "the base cap" still has a sane number.
 */
const TS_EXP_DAILY_CAP = 120;

/**
 * What each kind of contribution is worth.
 *
 * These are deliberately small: helping is worth more than being helped, and a
 * five-star contribution is worth more than a polite four. Nothing here rewards
 * volume alone — there is no "sent 100 messages" award.
 */
const TS_EXP_RULES = [
    // Somebody asked for help and you answered it, and it was closed as solved.
    'help_given'      => 25,
    // You asked, got help and closed the request — the loop worked.
    'help_closed'     => 10,
    // You took the time to rate the help you got.
    'rated'           => 3,
    // Somebody rated you (paid once per rating, whatever the stars).
    'rating_received' => 5,
    // …and it was a perfect score.
    'rating_five'     => 5,
    // Profile/paper contributions (kept here so every award is one mechanism).
    'paper_uploaded'  => 15,
    'profile_complete' => 20,
    // Quizzes. Paid once per quiz: a pass, and a bonus for a perfect score on
    // top of it. Retakes are free but never pay again (see the ref keys the
    // quizzes API uses: quiz:{id}:pass, quiz:{id}:perfect).
    'quiz_passed'     => 20,
    'quiz_perfect'    => 10,
];

/**
 * Tiers, lowest first. `min` is the total EXP needed to *be* that tier.
 *
 * Names are things a student would say about a classmate, not RPG ranks.
 */
const TS_EXP_TIERS = [
    ['code' => 'learner', 'name' => 'Learner', 'min' => 0,    'cap' => 120],
    ['code' => 'helper',  'name' => 'Helper',  'min' => 200,  'cap' => 160],
    ['code' => 'guide',   'name' => 'Guide',   'min' => 600,  'cap' => 200],
    ['code' => 'mentor',  'name' => 'Mentor',  'min' => 1500, 'cap' => 260],
    ['code' => 'legend',  'name' => 'Legend',  'min' => 3000, 'cap' => 320],
];

/**
 * The daily ceiling for a student whose lifetime total is `$total`.
 *
 * Read off the tier table, so climbing a tier is felt the same day: the ceiling
 * itself goes up.
 */
function ts_exp_cap_for_total(int $total): int
{
    $cap = TS_EXP_DAILY_CAP;
    foreach (TS_EXP_TIERS as $tier) {
        if ($total >= $tier['min']) {
            $cap = (int) $tier['cap'];
        }
    }
    return $cap;
}

/** Has the ledger table been created on this database yet? Cached per request. */
function ts_exp_table_ready(PDO $pdo): bool
{
    static $ready = null;
    if ($ready !== null) {
        return $ready;
    }
    try {
        $pdo->query('SELECT 1 FROM user_exp LIMIT 1');
        $ready = true;
    } catch (Throwable $e) {
        $ready = false;
    }
    return $ready;
}

/** Exp already earned today (server-local date). */
function ts_exp_today(PDO $pdo, int $userId): int
{
    if (!ts_exp_table_ready($pdo)) {
        return 0;
    }
    try {
        $stmt = $pdo->prepare(
            'SELECT COALESCE(SUM(amount), 0) FROM user_exp
             WHERE user_id = :uid AND created_at >= CURDATE()'
        );
        $stmt->execute(['uid' => $userId]);
        return (int) $stmt->fetchColumn();
    } catch (Throwable $e) {
        error_log('[TheStudent] exp today failed: ' . $e->getMessage());
        return 0;
    }
}

/** All the EXP a student has ever earned. */
function ts_exp_total(PDO $pdo, int $userId): int
{
    if (!ts_exp_table_ready($pdo)) {
        return 0;
    }
    try {
        $stmt = $pdo->prepare(
            'SELECT COALESCE(SUM(amount), 0) FROM user_exp WHERE user_id = :uid'
        );
        $stmt->execute(['uid' => $userId]);
        return (int) $stmt->fetchColumn();
    } catch (Throwable $e) {
        error_log('[TheStudent] exp total failed: ' . $e->getMessage());
        return 0;
    }
}

/**
 * Pay EXP for something that happened.
 *
 * @return int The amount actually paid (0 when the award was a duplicate, hit
 *             the daily ceiling, or the ledger is unavailable).
 */
function ts_exp_award(
    PDO $pdo,
    int $userId,
    string $kind,
    string $refKey,
    string $note = '',
    ?int $amountOverride = null
): int {
    if ($userId <= 0 || $refKey === '' || !ts_exp_table_ready($pdo)) {
        return 0;
    }
    $amount = $amountOverride ?? (TS_EXP_RULES[$kind] ?? 0);
    if ($amount <= 0) {
        return 0;
    }

    // The ceiling is the *current* tier's, read from the lifetime total.
    $cap = ts_exp_cap_for_total(ts_exp_total($pdo, $userId));
    $remaining = $cap - ts_exp_today($pdo, $userId);
    if ($remaining <= 0) {
        return 0;
    }
    $amount = min($amount, $remaining);

    try {
        $stmt = $pdo->prepare(
            'INSERT IGNORE INTO user_exp (user_id, kind, amount, ref_key, note)
             VALUES (:uid, :kind, :amount, :ref, :note)'
        );
        $stmt->execute([
            'uid'    => $userId,
            'kind'   => $kind,
            'amount' => $amount,
            'ref'    => substr($refKey, 0, 64),
            'note'   => substr($note, 0, 120),
        ]);
        return $stmt->rowCount() > 0 ? $amount : 0;
    } catch (Throwable $e) {
        error_log('[TheStudent] exp award failed: ' . $e->getMessage());
        return 0;
    }
}

/**
 * The tier a total EXP falls in, plus how far the next one is.
 *
 * `progress` is 0…1 inside the current tier, so a bar can be drawn without the
 * client re-deriving the thresholds.
 */
function ts_exp_tier(int $total): array
{
    $current = TS_EXP_TIERS[0];
    $next = null;
    foreach (TS_EXP_TIERS as $index => $tier) {
        if ($total >= $tier['min']) {
            $current = $tier;
            $next = TS_EXP_TIERS[$index + 1] ?? null;
        }
    }
    $span = $next !== null ? max(1, $next['min'] - $current['min']) : 0;
    $into = $total - $current['min'];

    // Levels are the ladder read top-down: Learner is 1, Legend is 5. The app
    // shows "Level N" so a climb reads like progress rather than jargon.
    $level = 1;
    foreach (TS_EXP_TIERS as $index => $tier) {
        if ($tier['code'] === $current['code']) {
            $level = $index + 1;
        }
    }

    return [
        'code'          => $current['code'],
        'name'          => $current['name'],
        'level'         => $level,
        'min'           => $current['min'],
        'cap'           => (int) $current['cap'],
        'next_code'     => $next['code'] ?? null,
        'next_name'     => $next['name'] ?? null,
        'next_min'      => $next['min'] ?? null,
        'next_cap'      => isset($next['cap']) ? (int) $next['cap'] : null,
        'next_level'    => $next !== null ? $level + 1 : null,
        'to_next'       => $next !== null ? max(0, $next['min'] - $total) : 0,
        'progress'      => $next !== null ? min(1.0, round($into / $span, 4)) : 1.0,
    ];
}

/** The newest awards, for a history screen. */
function ts_exp_recent(PDO $pdo, int $userId, int $limit = 20): array
{
    if (!ts_exp_table_ready($pdo)) {
        return [];
    }
    try {
        $stmt = $pdo->prepare(
            'SELECT kind, amount, note, UNIX_TIMESTAMP(created_at) AS created_epoch
             FROM user_exp WHERE user_id = :uid
             ORDER BY id DESC LIMIT ' . max(1, min(50, $limit))
        );
        $stmt->execute(['uid' => $userId]);
        $out = [];
        foreach ($stmt->fetchAll() as $row) {
            $out[] = [
                'kind'          => (string) $row['kind'],
                'amount'        => (int) $row['amount'],
                'note'          => (string) ($row['note'] ?? ''),
                'created_epoch' => (int) ($row['created_epoch'] ?? 0),
            ];
        }
        return $out;
    } catch (Throwable $e) {
        error_log('[TheStudent] exp recent failed: ' . $e->getMessage());
        return [];
    }
}

/** Everything the app needs to draw: total, today, tier, recent awards. */
function ts_exp_summary(PDO $pdo, int $userId, bool $withRecent = true): array
{
    $total = ts_exp_total($pdo, $userId);
    return [
        'total'     => $total,
        'today'     => ts_exp_today($pdo, $userId),
        'daily_cap' => ts_exp_cap_for_total($total),
        'tier'      => ts_exp_tier($total),
        'rules'     => TS_EXP_RULES,
        'recent'    => $withRecent ? ts_exp_recent($pdo, $userId) : [],
    ];
}
