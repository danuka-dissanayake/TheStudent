-- ====================================================================
-- TheStudent — Quizzes migration 1.1 (attempt limits)
-- --------------------------------------------------------------------
-- Adds `quizzes.max_attempts`, the number of attempts a student gets on a quiz
-- before it is marked enrolled and disabled in the app.
--
-- Run this once if you imported `database_quizzes.sql` before version 1.1.
-- Safe to re-run: the ALTER only fires when the column is missing. A fresh
-- install that already used the current `database_quizzes.sql` has nothing to do.
-- ====================================================================

SET @has_column := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'quizzes'
    AND COLUMN_NAME = 'max_attempts'
);

SET @sql := IF(
  @has_column = 0,
  'ALTER TABLE `quizzes` ADD COLUMN `max_attempts` TINYINT NOT NULL DEFAULT 3 COMMENT ''attempts a student gets; 1...10'' AFTER `exp_reward`',
  'DO 0'
);

PREPARE ts_stmt FROM @sql;
EXECUTE ts_stmt;
DEALLOCATE PREPARE ts_stmt;

-- Existing rows keep the default (3). Tighten or loosen per quiz from the app:
--   Admin panel -> Quizzes -> Edit.
SELECT id, title, max_attempts FROM `quizzes` ORDER BY id;
