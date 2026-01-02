-- ==========================================
-- PriceLabs 集成修复: 添加价格计划支持到渠道价格表
-- 修复门店级数据隔离和价格计划隔离
-- ==========================================

-- 1. 添加 price_plan_id 字段
ALTER TABLE channel_prices
ADD COLUMN price_plan_id BIGINT AFTER room_type_id;

-- 2. 为现有数据设置默认价格计划 (如果有数据的话)
-- 注意: 这里假设每个门店都有一个默认价格计划
-- 如果没有数据,这个语句会跳过
UPDATE channel_prices cp
INNER JOIN room_type_price_plans rtpp ON cp.room_type_id = rtpp.room_type_id
INNER JOIN price_plans pp ON rtpp.price_plan_id = pp.id
SET cp.price_plan_id = pp.id
WHERE cp.price_plan_id IS NULL
  AND pp.is_default = true;

-- 3. 设置字段为 NOT NULL (在填充数据后)
ALTER TABLE channel_prices
MODIFY COLUMN price_plan_id BIGINT NOT NULL;

-- 4. 添加外键约束
ALTER TABLE channel_prices
ADD CONSTRAINT fk_channel_price_price_plan
FOREIGN KEY (price_plan_id) REFERENCES price_plans(id) ON DELETE CASCADE;

-- 5. 删除旧的唯一约束
-- 注意: 约束名称可能因数据库版本而异,如果失败请手动调整
ALTER TABLE channel_prices
DROP INDEX IF EXISTS uk_channel_price_unique;

-- 尝试其他可能的约束名称
ALTER TABLE channel_prices
DROP INDEX IF EXISTS uk_store_room_channel_date;

-- 6. 添加新的唯一约束 (包含 price_plan_id)
ALTER TABLE channel_prices
ADD CONSTRAINT uk_store_room_plan_channel_date
UNIQUE (store_id, room_type_id, price_plan_id, channel_id, price_date);

-- 7. 添加索引优化查询性能
CREATE INDEX idx_channel_price_plan_date
ON channel_prices(price_plan_id, price_date);

-- 8. 添加检查约束确保关联实体属于同一门店 (MySQL 8.0.16+)
-- 注意: 如果 MySQL 版本较低,请注释掉这部分
-- ALTER TABLE channel_prices
-- ADD CONSTRAINT chk_channel_price_same_store
-- CHECK (
--     -- 确保 room_type 属于同一门店
--     store_id = (SELECT store_id FROM room_types WHERE id = room_type_id) AND
--     -- 确保 price_plan 属于同一门店
--     store_id = (SELECT store_id FROM price_plans WHERE id = price_plan_id) AND
--     -- 确保 channel 属于同一门店
--     store_id = (SELECT store_id FROM channels WHERE id = channel_id)
-- );

-- ==========================================
-- 迁移完成说明
-- ==========================================
--
-- 修复内容:
-- 1. ✅ 添加 price_plan_id 字段 - 防止同一房型不同价格计划的价格相互覆盖
-- 2. ✅ 更新唯一约束 - 包含 store_id 和 price_plan_id,确保门店级和价格计划级隔离
-- 3. ✅ 添加外键约束 - 确保数据完整性
-- 4. ✅ 添加索引 - 优化查询性能
--
-- 注意事项:
-- - 如果表中已有数据,确保每个房型都有默认价格计划
-- - 如果 UPDATE 语句失败,说明缺少默认价格计划,需要先创建
-- - 检查约束需要 MySQL 8.0.16+,低版本请注释掉
--
-- ==========================================
