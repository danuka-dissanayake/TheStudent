# EXP (experience points)

EXP is what makes helping worth something beyond a thank-you. It is deliberately
small, slow and auditable: a student cannot talk their way to the top, and every
point on the home screen can be traced to the thing that earned it.

Code: `backend/api/_exp.php` (the ledger), `backend/database_exp.sql` (the table),
`app/.../data/model/ExpModels.kt`, `app/.../ui/screens/ExpScreen.kt`.

## 1. The ledger, not a counter

There is no `users.points` column and no cached total anywhere. EXP lives in
`user_exp`, one row per award:

| Column | Meaning |
| --- | --- |
| `user_id` | who earned it |
| `kind` | what kind of award (`help_given`, `rated`, …) |
| `amount` | EXP **actually paid** — a trimmed award records the smaller number |
| `ref_key` | the *identity* of the award: `help:41:helper`, `rating:17:ratee` |
| `note` | one short human line for the history screen |
| `created_at` | when |

`UNIQUE KEY (user_id, ref_key)` is the whole anti-cheat story. A double tap, a
retried request, two phones racing, a replayed close — the second `INSERT IGNORE`
is simply ignored. The database decides, not the app.

The total is `SUM(amount)`. That means a rule can be corrected by inserting a new
row, and history is never silently rewritten.

## 2. What things are worth

| kind | EXP | awarded when |
| --- | --- | --- |
| `help_given` | 25 | a request you answered was closed as solved (`help_close`, to the helper) |
| `help_closed` | 10 | you closed a request you got help with (to the asker) |
| `rated` | 3 | you rated the help you received |
| `rating_received` | 5 | somebody rated you — once per rating, whatever the stars |
| `rating_five` | 5 | …and it was five stars |
| `paper_uploaded` | 15 | a past paper you contributed is accepted |
| `profile_complete` | 20 | your first profile setup, once ever |

Editing a rating inside its 24-hour window does **not** pay again: the rating's
own id is the ref key.

## 3. Tiers

| Tier | From |
| --- | --- |
| Learner | 0 |
| Helper | 200 |
| Guide | 600 |
| Mentor | 1500 |
| Legend | 3000 |

`ts_exp_tier()` returns the current tier plus `next_min`, `to_next` and a 0…1
`progress` for the bar, so no client has to re-derive thresholds. The app keeps a
read-only mirror of the ladder (`ExpTier.forTotal`) purely so the home screen can
name a tier before the first network call lands — it never awards anything.

## 4. The daily ceiling

`TS_EXP_DAILY_CAP = 120` per student per server-local day. Before each insert the
award is trimmed to what is left of the allowance (and skipped entirely at 0).
A trimmed award is still written down — as the real, smaller number — so the
ledger never claims more than was paid.

## 5. Deploy

Import `backend/database_exp.sql` in phpMyAdmin (safe to re-run: `IF NOT EXISTS`).
`_exp.php` caches a `SELECT 1 FROM user_exp` probe per request, and if the table is
missing every function returns a neutral value — total 0, tier "Learner", no
recent awards — instead of throwing. EXP can never be the reason a student sees
an error.

## 6. API

| Method | Endpoint | Returns |
| --- | --- | --- |
| GET | `auth.php?action=me` / `profile` | the user object now carries `exp_total`, `exp_today`, `exp_daily_cap`, `exp_tier` |
| GET | `chat.php?action=exp_state` | `exp: { total, today, daily_cap, tier, rules, recent[] }` |
| POST | `chat.php?action=rate` | response now includes `exp` for the rater, so a rating refreshes the number immediately |

`chat.php` is **2.9.0**; the app refuses older chat servers.

## 7. In the app

- The XP pill on the home screen shows `exp_total` (cached in `UserProfile.points`)
  and opens **Your EXP**.
- That screen shows the total, the tier bar with "N EXP to Guide", today's
  allowance bar with "N EXP still available today", and the newest awards with a
  relative time — all straight from the ledger, never summed on the phone.
- `TheStudentViewModel.refreshExp()` is the only writer of the client total; there
  is no optimistic "+25" anywhere.
