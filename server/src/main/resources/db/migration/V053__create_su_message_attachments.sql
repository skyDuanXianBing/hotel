CREATE TABLE IF NOT EXISTS su_message_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    thread_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    external_attachment_id VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NULL,
    file_name VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_su_msg_attachment_external
        UNIQUE (store_id, message_id, external_attachment_id),
    CONSTRAINT fk_su_msg_attachment_thread
        FOREIGN KEY (thread_id) REFERENCES su_message_threads (id) ON DELETE CASCADE,
    CONSTRAINT fk_su_msg_attachment_message
        FOREIGN KEY (message_id) REFERENCES su_messages (id) ON DELETE CASCADE,
    INDEX idx_su_msg_attachment_store_thread (store_id, thread_id),
    INDEX idx_su_msg_attachment_message (message_id)
);
