CREATE TABLE IF NOT EXISTS su_messaging_user_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    translation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    translation_target_language VARCHAR(10) NOT NULL DEFAULT 'zh-CN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_su_messaging_user_settings_user UNIQUE (user_id),
    CONSTRAINT fk_su_messaging_user_settings_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
