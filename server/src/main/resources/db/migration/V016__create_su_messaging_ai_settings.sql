CREATE TABLE IF NOT EXISTS su_messaging_ai_settings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    auto_reply_enabled BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_su_messaging_ai_settings_store UNIQUE (store_id),
    CONSTRAINT fk_su_messaging_ai_settings_store FOREIGN KEY (store_id) REFERENCES stores(id)
);
