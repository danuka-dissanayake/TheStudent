-- ═══════════════════════════════════════════════════════════════════════════
--  TheStudent — EXP ledger
--  Backend: backend/api/_exp.php  (awarded from chat.php, read by auth.php,
--                                 chat.php?action=exp_state)
--
--  One row per thing a student earned. There is no cached total column: the
--  total is SUM(amount), so the number on the home screen can always be
--  explained by the rows underneath it.
--
--  `ref_key` is the award's identity, not its description — "help:41:helper",
--  "rating:17:ratee". The unique key is what makes a retried request, a double
--  tap or two phones racing harmless: the second INSERT is ignored.
--
--  Run once in phpMyAdmin. Safe to re-run: every statement is IF NOT EXISTS.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS `user_exp` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`    INT UNSIGNED    NOT NULL
        COMMENT 'The student who earned it',
    `kind`       VARCHAR(32)     NOT NULL
        COMMENT 'help_given | help_closed | rated | rating_received | rating_five | paper_uploaded | profile_complete',
    `amount`     SMALLINT        NOT NULL
        COMMENT 'EXP actually paid (may be trimmed by the daily cap)',
    `ref_key`    VARCHAR(64)     NOT NULL
        COMMENT 'Identity of the award, e.g. help:41:helper — unique per student',
    `note`       VARCHAR(120)    NOT NULL DEFAULT ''
        COMMENT 'Short human line for the history screen',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_user_exp_ref` (`user_id`, `ref_key`),
    KEY `idx_user_exp_day` (`user_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
