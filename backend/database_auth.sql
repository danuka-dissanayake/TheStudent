-- --------------------------------------------------------
-- TheStudent - Production Authentication Database Schema
-- Add this to your DirectAdmin MySQL Database (via phpMyAdmin)
-- --------------------------------------------------------

-- 1. Users Table (Stores student profiles, email accounts, and Google accounts)
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `google_id` VARCHAR(100) NULL UNIQUE,
  `email` VARCHAR(191) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NULL,
  `full_name` VARCHAR(150) NOT NULL,
  `profile_picture` TEXT NULL,
  `auth_provider` ENUM('email', 'google') NOT NULL DEFAULT 'email',
  `target_exam` VARCHAR(100) NOT NULL DEFAULT 'G.C.E. A/L',
  `institution` VARCHAR(191) NULL DEFAULT 'Sri Lanka Education System',
  `stream_or_degree` VARCHAR(150) NULL DEFAULT 'Physical Science',
  `role` ENUM('student', 'admin') NOT NULL DEFAULT 'student',
  `is_email_verified` TINYINT(1) NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `last_login_ip` VARCHAR(45) NULL,
  `last_login_at` DATETIME NULL,
  `suspicious_flag` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_email` (`email`),
  INDEX `idx_google_id` (`google_id`),
  INDEX `idx_last_login_ip` (`last_login_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Email Verifications Table (Stores 6-digit OTP codes for account activation & IP change security)
CREATE TABLE IF NOT EXISTS `email_verifications` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NULL DEFAULT NULL,
  `email` VARCHAR(191) NOT NULL,
  `otp_code` VARCHAR(10) NOT NULL,
  `otp_type` ENUM('registration', 'unverified_account', 'login_challenge', 'ip_change', 'password_reset') NOT NULL DEFAULT 'registration',
  `pending_data` LONGTEXT NULL DEFAULT NULL,
  `attempts` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `is_used` TINYINT(1) NOT NULL DEFAULT 0,
  `expires_at` DATETIME NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_email_otp` (`email`, `otp_code`, `is_used`),
  INDEX `idx_user_verification` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. User Tokens Table (Stores active API authentication tokens for Android sessions)
CREATE TABLE IF NOT EXISTS `user_tokens` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL,
  `token` VARCHAR(128) NOT NULL UNIQUE,
  `device_info` VARCHAR(255) NULL,
  `ip_address` VARCHAR(45) NULL,
  `expires_at` DATETIME NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_token` (`token`),
  INDEX `idx_user_token` (`user_id`),
  CONSTRAINT `fk_user_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Rate Limiting Table (Prevents brute-force dictionary attacks against user logins & OTPs)
CREATE TABLE IF NOT EXISTS `login_attempts` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `ip_address` VARCHAR(45) NOT NULL,
  `email` VARCHAR(191) NOT NULL,
  `attempted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_ip_attempt` (`ip_address`, `attempted_at`),
  INDEX `idx_email_attempt` (`email`, `attempted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
