-- VoiceFlow India schema (MySQL 8, utf8mb4).
-- Idempotent: safe to run on every start. For future changes, add new files
-- (e.g. V2__add_column.sql) and apply them manually or adopt Flyway later.

CREATE TABLE IF NOT EXISTS transcription_history (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL DEFAULT 1,          -- single local user for now; ready for auth later
    language      VARCHAR(8)   NOT NULL,
    original_text MEDIUMTEXT   NOT NULL,                    -- exactly what the speech provider returned
    edited_text   MEDIUMTEXT   NOT NULL,                    -- what the user kept after editing/cleanup
    created_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_history_user_created (user_id, created_at),
    CONSTRAINT chk_history_language CHECK (language IN ('en', 'hi', 'mr'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_settings (
    user_id            BIGINT     NOT NULL DEFAULT 1,
    default_language   VARCHAR(8) NOT NULL DEFAULT 'en',
    fix_whitespace     BOOLEAN    NOT NULL DEFAULT TRUE,
    fix_punctuation    BOOLEAN    NOT NULL DEFAULT TRUE,
    capitalize         BOOLEAN    NOT NULL DEFAULT TRUE,
    remove_fillers     BOOLEAN    NOT NULL DEFAULT FALSE,
    auto_cleanup       BOOLEAN    NOT NULL DEFAULT FALSE,
    updated_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT chk_settings_language CHECK (default_language IN ('en', 'hi', 'mr'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
