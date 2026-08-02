-- ============================================================================
-- 登记表两段式审查门店设置
-- 说明：初审通过（REVIEWED）后，系统在入住日前 lead_days 天自动完成终审，
--       并通过 Su 消息向客人发送 final_message（为空时使用 i18n 默认模板）。
-- 数据库：MySQL 8（utf8mb4）；沿用 information_schema 幂等守卫写法，脚本可重入。
-- ============================================================================

SET @registration_review_settings_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'registration_review_settings'
);
SET @registration_review_settings_create := IF(
    @registration_review_settings_exists = 0,
    'CREATE TABLE registration_review_settings (
        id BIGINT NOT NULL AUTO_INCREMENT,
        store_id BIGINT NOT NULL,
        auto_finalize_enabled TINYINT(1) NOT NULL DEFAULT 1,
        lead_days INT NOT NULL DEFAULT 7,
        final_message TEXT NULL,
        created_at DATETIME NOT NULL,
        updated_at DATETIME NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_registration_review_settings_store (store_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT 1'
);
PREPARE stmt_registration_review_settings_create FROM @registration_review_settings_create;
EXECUTE stmt_registration_review_settings_create;
DEALLOCATE PREPARE stmt_registration_review_settings_create;
