-- ====================================================================
-- TheStudent — fix generated SFT (Sinhala) quizzes so the app can find them
-- --------------------------------------------------------------------
-- The generated quizzes were inserted with
--     grade = 'A/L', category = 'Practice'
-- which are not values the app matches. QuizzesScreen.matchesChip() only
-- matches category in (A/L, O/L, Junior, Provincial, University) and grade
-- exactly "Grade 12" / "Grade 13" for A/L, so those quizzes appeared under no
-- filter at all (and the editor showed no selected category chip).
--
-- This corrects the whole batch:  grade 'A/L' → 'Grade 13'
--                                 category 'Practice' → 'A/L'
--
-- Run it AFTER importing the generated quiz file. It is safe to re-run: the
-- WHERE clauses only match rows that still carry the wrong values.
-- Nothing else is touched — questions, options, explanations and the
-- exp_reward = 20 / status = 'draft' contract stay exactly as imported.
-- ====================================================================

SET NAMES utf8mb4;

UPDATE `quizzes`
   SET `grade` = 'Grade 13'
 WHERE `grade` = 'A/L'
   AND `code` LIKE 'SFT-AL-%';

UPDATE `quizzes`
   SET `category` = 'A/L'
 WHERE `category` = 'Practice'
   AND `code` LIKE 'SFT-AL-%';

-- ── verify: every SFT-AL quiz must now read Grade 13 / A/L / draft ──
SELECT `id`, `code`, `grade`, `category`, `medium`, `status`, `exp_reward`,
       (SELECT COUNT(*) FROM `quiz_questions` q WHERE q.`quiz_id` = `quizzes`.`id`)
           AS `questions`
  FROM `quizzes`
 WHERE `code` LIKE 'SFT-AL-%'
 ORDER BY `code`;

-- Expected: 5 rows, each grade = 'Grade 13', category = 'A/L',
--           medium = 'Sinhala', status = 'draft', exp_reward = 20, questions = 10.
