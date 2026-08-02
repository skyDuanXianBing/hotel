-- ============================================================================
-- 移动推送设备令牌（iOS APNs / 预留 Android FCM）
-- 说明：push_device_token 一台设备一行，uk(device_token)；换账号/门店登录时
--       应用层 upsert 更新 user_id/store_id 并重新启用，退出登录时整行删除。
-- 数据库：MySQL 8（utf8mb4）；沿用 information_schema 幂等守卫写法，脚本可重入。
-- ============================================================================

SET @push_device_tokens_exists := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'push_device_tokens'
);
SET @push_device_tokens_create := IF(
    @push_device_tokens_exists = 0,
    'CREATE TABLE push_device_tokens (
        id BIGINT NOT NULL AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        store_id BIGINT NOT NULL,
        platform VARCHAR(20) NOT NULL,
        device_token VARCHAR(512) NOT NULL,
        enabled TINYINT(1) NOT NULL DEFAULT 1,
        last_seen_at DATETIME NOT NULL,
        created_at DATETIME NOT NULL,
        updated_at DATETIME NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_push_device_token (device_token),
        KEY idx_push_device_user (user_id, enabled),
        KEY idx_push_device_store (store_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT 1'
);
PREPARE stmt_push_device_tokens_create FROM @push_device_tokens_create;
EXECUTE stmt_push_device_tokens_create;
DEALLOCATE PREPARE stmt_push_device_tokens_create;
