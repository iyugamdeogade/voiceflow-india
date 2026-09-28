CREATE TABLE transcription_history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL DEFAULT 1,
    language      VARCHAR(8)   NOT NULL,
    original_text CLOB         NOT NULL,
    edited_text   CLOB         NOT NULL,
    created_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
