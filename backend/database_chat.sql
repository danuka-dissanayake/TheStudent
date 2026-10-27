-- ═══════════════════════════════════════════════════════════════════════════
--  TheStudent — chat schema (relay only, never an archive)
--  Backend: backend/api/chat.php  (v2.0.0)
--
--  Privacy model
--  -------------
--  Message bodies are encrypted end-to-end on the student's phone. The server
--  only ever holds opaque `e2ee:v1:` blobs, and only until the recipient's
--  phone fetches them — every `inbox` call deletes the rows it hands out, and
--  anything older than 7 days is purged on the next request. There is no
--  message-history table, no per-thread archive, no key backup table, and no
--  way for the operator to read a conversation.
--
--  Conversation history lives in the student's own SQLite database on the
--  device (`chat_messages` via Room). A dump of this database contains no
--  conversation content at all.
--
--  Run once in phpMyAdmin. Safe to re-run: every statement is IF NOT EXISTS.
-- ═══════════════════════════════════════════════════════════════════════════

-- ── 1. Presence + encryption identity columns on the existing users table ───
ALTER TABLE `users`
    ADD COLUMN IF NOT EXISTS `last_seen_at`      DATETIME NULL DEFAULT NULL
        COMMENT 'Last chat heartbeat; online = within 10 s',
    ADD COLUMN IF NOT EXISTS `chat_safety_ack_at` DATETIME NULL DEFAULT NULL
        COMMENT 'When the student dismissed the one-time safety notice',
    ADD COLUMN IF NOT EXISTS `chat_public_key`   VARCHAR(255) NULL DEFAULT NULL
        COMMENT 'Base64 uncompressed EC P-256 public key';

CREATE INDEX IF NOT EXISTS `idx_users_last_seen_at` ON `users` (`last_seen_at`);

-- ── 2. Relay queue: encrypted envelopes waiting to be collected ─────────────
--     kind = 'msg'    → body is an e2ee:v1:… payload for recipient_id
--     kind = 'read'   → body is empty, ref_id points at the read up to that id
--     kind = 'delete' → body is empty, ref_id is a message the sender deleted
--                      for both sides
CREATE TABLE IF NOT EXISTS `chat_relay` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `sender_id`    INT UNSIGNED    NOT NULL,
    `recipient_id` INT UNSIGNED    NOT NULL,
    `kind`         ENUM('msg','read','delete') NOT NULL DEFAULT 'msg',
    `body`         TEXT            NULL,
    `ref_id`       BIGINT UNSIGNED NULL DEFAULT NULL,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chat_relay_recipient` (`recipient_id`, `id`),
    KEY `idx_chat_relay_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. Typing flags (overwritten in place, never appended) ──────────────────
CREATE TABLE IF NOT EXISTS `chat_typing` (
    `user_id`    INT UNSIGNED NOT NULL,
    `peer_id`    INT UNSIGNED NOT NULL,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `peer_id`),
    KEY `idx_chat_typing_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 4. Attachment blobs (images / documents), encrypted on the sender's phone ─
--     The file itself lives in backend/uploads/chat/<id>.bin and is AES-256-GCM
--     ciphertext: neither this table nor the folder can be read by the operator.
--     A blob is deleted as soon as the recipient downloads it.
CREATE TABLE IF NOT EXISTS `chat_attachments` (
    `id`           VARCHAR(64)     NOT NULL,
    `sender_id`    INT UNSIGNED    NOT NULL,
    `recipient_id` INT UNSIGNED    NOT NULL,
    `bytes`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chat_att_recipient` (`recipient_id`, `created_at`),
    KEY `idx_chat_att_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
--  Housekeeping: chat.php purges expired relay rows and stale typing flags at
--  the start of every request, so no cron job is required. The queries below
--  are listed only so the operator can run them by hand if the queue ever
--  needs clearing (e.g. before handing the site to someone else).
--
--      DELETE FROM `chat_relay`  WHERE `created_at` < NOW() - INTERVAL 7 DAY;
--      DELETE FROM `chat_typing` WHERE `updated_at` < NOW() - INTERVAL 1 HOUR;
--
--  Attachment blobs follow the same rule and chat.php removes the files too:
--
--      DELETE FROM `chat_attachments` WHERE `created_at` < NOW() - INTERVAL 7 DAY;
--
--  Legacy cleanup (only if you upgraded from an older chat.php version — the
--  current API never reads or writes these tables):
--
--      DROP TABLE IF EXISTS `chat_messages`;
--      DROP TABLE IF EXISTS `chat_attachments`;
--      DROP TABLE IF EXISTS `chat_key_backup`;
--      DROP TABLE IF EXISTS `chat_clears`;
--      DROP TABLE IF EXISTS `chat_history_backup`;
-- ═══════════════════════════════════════════════════════════════════════════
