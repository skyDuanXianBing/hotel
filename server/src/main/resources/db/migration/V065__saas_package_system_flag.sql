-- ============================================================================
-- SaaS 系统兜底套餐标记与审计备注（P9：到期回退默认版 + 管理端等级调控）
-- 说明：
--   1. saas_package.is_system 标记系统兜底套餐（默认版）：不可上架、不可经接口授予，
--      仅供订阅到期后的自动回退（SaasDefaultPackageFallbackService）按 is_system 查找。
--   2. 种子行「默认版」回填 is_system=1（按 name 定位，幂等可重入）。
--   3. saas_billing_order.remark 记录人工开通/调控的备注与操作人（管理端 grant）。
--   4. saas_subscription.remark 标记自动兜底来源（'auto-fallback-after-expiry'），
--      便于审计与对账区分人工/自动订阅。
-- 数据库：MySQL 8（utf8mb4）；不支持 ADD COLUMN IF NOT EXISTS，
--       沿用 V064 的 information_schema 幂等守卫写法，脚本可重入。
-- ============================================================================

-- 1. saas_package.is_system（幂等守卫）
SET @saas_package_has_is_system := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'saas_package' AND COLUMN_NAME = 'is_system'
);
SET @saas_package_add_is_system := IF(
    @saas_package_has_is_system = 0,
    'ALTER TABLE saas_package ADD COLUMN is_system TINYINT(1) NOT NULL DEFAULT 0 AFTER status',
    'SELECT 1'
);
PREPARE stmt_saas_package_is_system FROM @saas_package_add_is_system;
EXECUTE stmt_saas_package_is_system;
DEALLOCATE PREPARE stmt_saas_package_is_system;

-- 2. 种子「默认版」回填系统标记（UPDATE 本身幂等，可重入）
UPDATE saas_package SET is_system = 1 WHERE name = '默认版' AND is_system = 0;

-- 3. saas_billing_order.remark（幂等守卫）
SET @saas_billing_order_has_remark := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'saas_billing_order' AND COLUMN_NAME = 'remark'
);
SET @saas_billing_order_add_remark := IF(
    @saas_billing_order_has_remark = 0,
    'ALTER TABLE saas_billing_order ADD COLUMN remark VARCHAR(500) NULL AFTER idempotency_key',
    'SELECT 1'
);
PREPARE stmt_saas_billing_order_remark FROM @saas_billing_order_add_remark;
EXECUTE stmt_saas_billing_order_remark;
DEALLOCATE PREPARE stmt_saas_billing_order_remark;

-- 4. saas_subscription.remark（幂等守卫）
SET @saas_subscription_has_remark := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'saas_subscription' AND COLUMN_NAME = 'remark'
);
SET @saas_subscription_add_remark := IF(
    @saas_subscription_has_remark = 0,
    'ALTER TABLE saas_subscription ADD COLUMN remark VARCHAR(500) NULL AFTER status',
    'SELECT 1'
);
PREPARE stmt_saas_subscription_remark FROM @saas_subscription_add_remark;
EXECUTE stmt_saas_subscription_remark;
DEALLOCATE PREPARE stmt_saas_subscription_remark;
