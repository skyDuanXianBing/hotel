-- ==========================================
-- PriceLabs 集成修复: 添加价格计划支持
-- 执行时间: 请在应用停止时执行
-- ==========================================

USE booking_system_db;

-- 步骤 1: 添加 price_plan_id 字段
ALTER TABLE channel_prices
ADD COLUMN price_plan_id BIGINT AFTER room_type_id;

-- 步骤 2: 为现有数据填充默认价格计划
-- 注意: 如果表为空可以跳过此步骤
UPDATE channel_prices cp
INNER JOIN room_type_price_plans rtpp
    ON cp.room_type_id = rtpp.room_type_id
    AND cp.store_id = rtpp.store_id
INNER JOIN price_plans pp
    ON rtpp.price_plan_id = pp.id
SET cp.price_plan_id = pp.id
WHERE cp.price_plan_id IS NULL
  AND pp.is_default = true
LIMIT 10000;

-- 检查是否还有未填充的记录
SELECT COUNT(*) as unfilled_count
FROM channel_prices
WHERE price_plan_id IS NULL;

-- 如果上面的查询返回 > 0,说明有记录没有默认价格计划
-- 需要手动处理或删除这些记录

-- 步骤 3: 设置字段为 NOT NULL
ALTER TABLE channel_prices
MODIFY COLUMN price_plan_id BIGINT NOT NULL;

-- 步骤 4: 添加外键约束
ALTER TABLE channel_prices
ADD CONSTRAINT fk_channel_price_price_plan
FOREIGN KEY (price_plan_id) REFERENCES price_plans(id) ON DELETE CASCADE;

-- 步骤 5: 查看现有的唯一约束名称
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'booking_system_db'
  AND TABLE_NAME = 'channel_prices'
  AND CONSTRAINT_TYPE = 'UNIQUE';

-- 步骤 6: 删除旧的唯一约束
-- 根据步骤 5 的结果,替换下面的约束名称
-- ALTER TABLE channel_prices DROP INDEX <旧约束名称>;

-- 示例 (请根据实际情况调整):
-- ALTER TABLE channel_prices DROP INDEX uk_channel_price_unique;

-- 步骤 7: 添加新的唯一约束 (包含 price_plan_id)
ALTER TABLE channel_prices
ADD CONSTRAINT uk_store_room_plan_channel_date
UNIQUE (store_id, room_type_id, price_plan_id, channel_id, price_date);

-- 步骤 8: 添加索引优化查询
CREATE INDEX idx_channel_price_plan_date
ON channel_prices(price_plan_id, price_date);

-- 步骤 9: 验证迁移结果
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    IS_NULLABLE,
    COLUMN_TYPE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'booking_system_db'
  AND TABLE_NAME = 'channel_prices'
  AND COLUMN_NAME = 'price_plan_id';

-- 步骤 10: 查看新的约束
SHOW CREATE TABLE channel_prices;

-- ==========================================
-- 迁移完成!
-- ==========================================
