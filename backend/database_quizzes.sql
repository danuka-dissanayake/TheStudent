-- ====================================================================
-- TheStudent — Quizzes schema (MySQL / MariaDB, InfinityFree compatible)
-- --------------------------------------------------------------------
-- Safe to re-run: every statement is IF NOT EXISTS. Import this once in
-- phpMyAdmin before the quizzes API is used.
--
-- Nothing is seeded. A fresh server has zero quizzes; the app says so.
-- ====================================================================

CREATE TABLE IF NOT EXISTS `quizzes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(64) NOT NULL COMMENT 'stable, human readable id used in refs and exports',
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `subject` VARCHAR(100) NOT NULL,
  `grade` VARCHAR(50) NOT NULL COMMENT 'Grade 6 … Grade 13, Undergraduate',
  `category` VARCHAR(50) NOT NULL COMMENT 'A/L, O/L, Junior, Provincial, University',
  `stream` VARCHAR(100) DEFAULT NULL,
  `medium` VARCHAR(50) NOT NULL DEFAULT 'English',
  `time_limit_seconds` INT NOT NULL DEFAULT 0 COMMENT '0 = untimed',
  `pass_percent` TINYINT NOT NULL DEFAULT 60,
  `shuffle_questions` TINYINT(1) NOT NULL DEFAULT 1,
  `shuffle_options` TINYINT(1) NOT NULL DEFAULT 1,
  `exp_reward` INT NOT NULL DEFAULT 20 COMMENT 'EXP for a first pass; server clamps to 0…40',
  `max_attempts` TINYINT NOT NULL DEFAULT 3 COMMENT 'attempts a student gets; 1…10',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'draft, published, archived',
  `created_by` INT DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uniq_quiz_code` (`code`),
  INDEX `idx_quiz_status` (`status`),
  INDEX `idx_quiz_grade` (`grade`),
  INDEX `idx_quiz_subject` (`subject`),
  INDEX `idx_quiz_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_questions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `quiz_id` INT NOT NULL,
  `position` INT NOT NULL DEFAULT 0,
  `prompt` TEXT NOT NULL,
  `media_url` VARCHAR(500) DEFAULT NULL,
  `explanation` TEXT DEFAULT NULL COMMENT 'shown after the attempt is graded',
  `marks` TINYINT NOT NULL DEFAULT 1,
  INDEX `idx_quiz_questions_quiz` (`quiz_id`),
  CONSTRAINT `fk_quiz_questions_quiz` FOREIGN KEY (`quiz_id`)
    REFERENCES `quizzes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_options` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `question_id` INT NOT NULL,
  `position` TINYINT NOT NULL DEFAULT 0,
  `label` TEXT NOT NULL,
  `is_correct` TINYINT(1) NOT NULL DEFAULT 0,
  INDEX `idx_quiz_options_question` (`question_id`),
  CONSTRAINT `fk_quiz_options_question` FOREIGN KEY (`question_id`)
    REFERENCES `quiz_questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_attempts` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `quiz_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `submitted_at` TIMESTAMP NULL DEFAULT NULL,
  `score` INT NOT NULL DEFAULT 0,
  `max_score` INT NOT NULL DEFAULT 0,
  `percent` TINYINT NOT NULL DEFAULT 0,
  `passed` TINYINT(1) NOT NULL DEFAULT 0,
  `duration_seconds` INT NOT NULL DEFAULT 0,
  `answers_json` TEXT DEFAULT NULL COMMENT 'audit trail of the chosen option ids',
  `exp_awarded` INT NOT NULL DEFAULT 0,
  INDEX `idx_quiz_attempts_quiz_user` (`quiz_id`, `user_id`),
  INDEX `idx_quiz_attempts_user` (`user_id`),
  INDEX `idx_quiz_attempts_submitted` (`submitted_at`),
  CONSTRAINT `fk_quiz_attempts_quiz` FOREIGN KEY (`quiz_id`)
    REFERENCES `quizzes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
