-- ============================================================================
-- 推送设备语言（按设备 App 语言渲染推送文案）
-- 说明：push_device_tokens 增加 locale 列，App 注册/刷新令牌时上传当前语言；
--       空值回退 zh-CN。沿用 information_schema 幂等守卫写法，脚本可重入。
-- ============================================================================

SET @push_device_locale_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'push_device_tokens'
      AND COLUMN_NAME = 'locale'
);
SET @push_device_locale_add := IF(
    @push_device_locale_exists = 0,
    'ALTER TABLE push_device_tokens ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT ''zh-CN'' AFTER platform',
    'SELECT 1'
);
PREPARE stmt_push_device_locale_add FROM @push_device_locale_add;
EXECUTE stmt_push_device_locale_add;
DEALLOCATE PREPARE stmt_push_device_locale_add;
