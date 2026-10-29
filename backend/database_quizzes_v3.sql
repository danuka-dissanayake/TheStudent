-- ─────────────────────────────────────────────────────────────────────────────
-- TheStudent · quizzes: a hero image per quiz
--
-- Adds `quizzes.hero_image_url`, the picture shown at the top of the quiz detail
-- page. It stores a path to an image the admin **uploaded** (not a link to
-- someone else's site), e.g. `/uploads/quizzes/quiz-7f3a91c2.jpg`.
--
-- Safe to run more than once: the ALTER only fires when the column is missing.
-- Run it in phpMyAdmin against the same database as `database_quizzes.sql`.
--
-- After running this, upload the image files themselves to
--   <public_html>/uploads/quizzes/
-- (create the folder if it does not exist; 0755, and it must be writable by PHP
-- if you would rather upload through the admin panel).
--
-- Leaving the column NULL is fine: the app falls back to a curated Pexels photo
-- chosen from the quiz's subject, exactly as it does today.
-- ─────────────────────────────────────────────────────────────────────────────

SET NAMES utf8mb4;

SET @has_hero := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quizzes'
      AND COLUMN_NAME = 'hero_image_url'
);

SET @sql := IF(
    @has_hero = 0,
    'ALTER TABLE `quizzes` ADD COLUMN `hero_image_url` VARCHAR(255) NULL DEFAULT NULL COMMENT ''admin-uploaded hero image path, e.g. /uploads/quizzes/x.jpg'' AFTER `description`',
    'SELECT ''quizzes.hero_image_url already exists'' AS note'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verification: should list exactly one row, type varchar(255).
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'quizzes'
  AND COLUMN_NAME = 'hero_image_url';
