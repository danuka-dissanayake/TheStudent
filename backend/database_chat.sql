-- =====================================================================
-- TheStudent - Student Chat Database Schema (v1.0.0)
-- =====================================================================
-- Import this file in phpMyAdmin (DirectAdmin -> MySQL Databases ->
-- phpMyAdmin -> select your database -> Import) AFTER database_auth.sql.
--
-- You only need to do this once. If you cannot reach phpMyAdmin, simply open
-- the chat tab in the app: api/chat.php creates anything that is missing on
-- its first request (that is the "self-healing schema" path).
--
-- Tables
-- ------
--   chat_messages  every 1:1 message between two registered students
--   chat_typing    short-lived "X is typing…" markers (6 second lifetime)
--   chat_clears    per-student "clear chat" marker (messages are never
--                  destroyed for the other side)
-- and two columns on `users`:
--   last_seen_at        presence heartbeat (online = seen in the last 45 s)
--   chat_safety_ack_at  when the student acknowledged the safety notice
-- =====================================================================

CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `sender_id` INT UNSIGNED NOT NULL,
  `recipient_id` INT UNSIGNED NOT NULL,
  `body` TEXT NOT NULL,
  `read_at` DATETIME NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_chat_pair` (`sender_id`, `recipient_id`, `id`),
  INDEX `idx_chat_recipient_unread` (`recipient_id`, `read_at`),
  CONSTRAINT `fk_chat_sender` FOREIGN KEY (`sender_id`)
    REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chat_recipient` FOREIGN KEY (`recipient_id`)
    REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_typing` (
  `user_id` INT UNSIGNED NOT NULL,
  `peer_id` INT UNSIGNED NOT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `peer_id`),
  INDEX `idx_chat_typing_peer` (`peer_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_clears` (
  `user_id` INT UNSIGNED NOT NULL,
  `peer_id` INT UNSIGNED NOT NULL,
  `cleared_seq` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `cleared_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `peer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Presence + the one-time safety notice live on the users table.
-- (api/chat.php adds these automatically when they are missing.)
-- ---------------------------------------------------------------------
ALTER TABLE `users`
  ADD COLUMN `last_seen_at` DATETIME NULL DEFAULT NULL,
  ADD COLUMN `chat_safety_ack_at` DATETIME NULL DEFAULT NULL;

ALTER TABLE `users` ADD INDEX `idx_last_seen` (`last_seen_at`);

-- =====================================================================
-- Housekeeping (optional, run occasionally or schedule as a MySQL event)
-- =====================================================================
-- DELETE FROM `chat_typing` WHERE `updated_at` < (NOW() - INTERVAL 1 HOUR);
-- Students inactive for a year are not "online"; presence resets itself.
