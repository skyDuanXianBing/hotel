CREATE TABLE IF NOT EXISTS su_message_translations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    thread_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    target_language VARCHAR(20) NOT NULL,
    source_content_hash CHAR(64) NOT NULL,
    translated_content TEXT NOT NULL,
    translation_status VARCHAR(20) NOT NULL,
    translated_at DATETIME NOT NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_su_msg_trans_store_msg_lang_hash (
        store_id,
        message_id,
        target_language,
        source_content_hash
    ),
    INDEX idx_su_msg_trans_store_message_lang (
        store_id,
        message_id,
        target_language
    ),
    INDEX idx_su_msg_trans_store_thread (
        store_id,
        thread_id
    ),
    CONSTRAINT fk_su_msg_trans_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_su_msg_trans_thread FOREIGN KEY (thread_id) REFERENCES su_message_threads(id),
    CONSTRAINT fk_su_msg_trans_message FOREIGN KEY (message_id) REFERENCES su_messages(id)
);
