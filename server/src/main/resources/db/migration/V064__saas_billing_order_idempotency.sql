-- ============================================================================
-- SaaS 计费幂等（审查 B1/B2/F6 修复）
-- 说明：saas_billing_order 增加客户端生成的幂等键，uk(store_id, idempotency_key)
--       兜底"双击 / 网络重试 / 成功响应丢失后重试"不产生重复 PAID 订单。
--       MySQL 唯一索引允许多个 NULL，存量订单行（idempotency_key=NULL）互不影响。
-- 应用层配合：SaasBillingService 先按 (store_id, idempotency_key) 查询命中即幂等重放
--       （返回该门店当前生效订阅，不再新建订单/订阅）；同门店开通/购买经 stores 行
--       悲观锁串行化，锁内复查将并发冲突转化为重放，uk 仅为最终兜底。
-- 数据库：MySQL 8（utf8mb4）；MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，
--       沿用 V063 §12 的 information_schema 幂等守卫写法，脚本可重入。
-- ============================================================================

-- 1. idempotency_key 列（幂等守卫）
SET @saas_billing_order_has_idempotency_key := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'saas_billing_order' AND COLUMN_NAME = 'idempotency_key'
);
SET @saas_billing_order_add_idempotency_key := IF(
    @saas_billing_order_has_idempotency_key = 0,
    'ALTER TABLE saas_billing_order ADD COLUMN idempotency_key VARCHAR(64) NULL AFTER status',
    'SELECT 1'
);
PREPARE stmt_saas_billing_order_idempotency_key FROM @saas_billing_order_add_idempotency_key;
EXECUTE stmt_saas_billing_order_idempotency_key;
DEALLOCATE PREPARE stmt_saas_billing_order_idempotency_key;

-- 2. uk(store_id, idempotency_key)（幂等守卫；多个 NULL 不冲突，存量行无影响）
SET @saas_billing_order_has_idempotency_uk := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'saas_billing_order'
      AND INDEX_NAME = 'uk_saas_billing_order_idempotency'
);
SET @saas_billing_order_add_idempotency_uk := IF(
    @saas_billing_order_has_idempotency_uk = 0,
    'ALTER TABLE saas_billing_order ADD UNIQUE KEY uk_saas_billing_order_idempotency (store_id, idempotency_key)',
    'SELECT 1'
);
PREPARE stmt_saas_billing_order_idempotency_uk FROM @saas_billing_order_add_idempotency_uk;
EXECUTE stmt_saas_billing_order_idempotency_uk;
DEALLOCATE PREPARE stmt_saas_billing_order_idempotency_uk;
