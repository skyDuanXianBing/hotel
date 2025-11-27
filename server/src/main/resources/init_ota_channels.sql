-- ==========================================
-- 初始化 OTA 渠道数据（为所有门店批量创建）
-- ==========================================
-- 说明：
-- 1. 此脚本会为数据库中所有门店创建相同的 OTA 渠道列表
-- 2. 这些渠道将默认启用，价格调整类型为百分比（0%，即等同于基础价格）
-- 3. 每个门店可以独立配置各自渠道的价格调整设置
-- 4. 使用 INSERT IGNORE 避免重复插入（如果渠道代码已存在则跳过）
-- ==========================================

-- 清空所有门店的现有 OTA 渠道数据（可选，谨慎使用！）
-- DELETE FROM channels WHERE type = 'OTA';

-- ==========================================
-- 方式1：为所有门店批量创建渠道（推荐）
-- ==========================================

-- 创建临时表存储渠道模板数据
CREATE TEMPORARY TABLE IF NOT EXISTS temp_ota_channels (
    name VARCHAR(100),
    code VARCHAR(20),
    type VARCHAR(50),
    color VARCHAR(7),
    description VARCHAR(500)
);

-- 插入渠道模板数据
INSERT INTO temp_ota_channels (name, code, type, color, description) VALUES
('Agoda', 'agoda', 'OTA', '#E74C3C', 'Agoda是全球领先的在线旅游平台之一'),
('Airbnb', 'airbnb', 'OTA', '#FF5A5F', 'Airbnb是全球知名的民宿预订平台'),
('Booking.com', 'booking', 'OTA', '#003580', 'Booking.com是全球最大的在线住宿预订平台'),
('Traveloka', 'traveloka', 'OTA', '#2C8DED', 'Traveloka是东南亚领先的在线旅游平台'),
('Trip.com', 'trip', 'OTA', '#2577E3', 'Trip.com（携程）是中国领先的在线旅游平台'),
('Expedia', 'expedia', 'OTA', '#FFCB08', 'Expedia是全球知名的在线旅游预订平台'),
('Tiket.com', 'tiket', 'OTA', '#0064D2', 'Tiket.com是印度尼西亚领先的在线旅游平台'),
('HostelWorld', 'hostelworld', 'OTA', '#2B3E50', 'HostelWorld是全球领先的青年旅舍预订平台'),
('TuJia', 'tujia', 'OTA', '#FF6B35', '途家是中国领先的民宿短租平台'),
('Neppan', 'neppan', 'OTA', '#FF6B35', 'Neppan渠道管理平台');

-- 为所有门店创建渠道（使用笛卡尔积）
INSERT INTO channels (
    store_id,
    name,
    code,
    type,
    is_active,
    enabled,
    color,
    price_adjustment_type,
    price_adjustment_value,
    auto_sync_price,
    description,
    created_at,
    updated_at
)
SELECT
    s.id AS store_id,
    t.name,
    CONCAT(t.code, '_', s.id) AS code,  -- 每个门店的渠道代码唯一：code_storeId
    t.type,
    TRUE AS is_active,
    TRUE AS enabled,
    t.color,
    'PERCENTAGE' AS price_adjustment_type,
    0 AS price_adjustment_value,
    TRUE AS auto_sync_price,
    t.description,
    NOW() AS created_at,
    NOW() AS updated_at
FROM stores s
CROSS JOIN temp_ota_channels t
WHERE NOT EXISTS (
    -- 避免重复插入：检查该门店是否已有该渠道代码
    SELECT 1 FROM channels c
    WHERE c.store_id = s.id AND c.code = CONCAT(t.code, '_', s.id)
);

-- 清理临时表
DROP TEMPORARY TABLE IF EXISTS temp_ota_channels;

-- 验证插入结果
SELECT
    s.name AS store_name,
    c.name AS channel_name,
    c.code,
    c.type,
    c.is_active,
    c.enabled,
    c.price_adjustment_type,
    c.price_adjustment_value
FROM channels c
JOIN stores s ON c.store_id = s.id
WHERE c.type = 'OTA'
ORDER BY s.id, c.id;

-- ==========================================
-- 方式2：仅为指定门店创建渠道
-- ==========================================
-- 如果您只想为特定门店创建渠道，请取消注释以下代码并设置门店ID

/*
-- 设置门店ID
SET @STORE_ID = 1;

-- 插入渠道数据
INSERT INTO channels (
    store_id,
    name,
    code,
    type,
    is_active,
    enabled,
    color,
    price_adjustment_type,
    price_adjustment_value,
    auto_sync_price,
    description,
    created_at,
    updated_at
) VALUES
-- 1. Agoda
(@STORE_ID, 'Agoda', CONCAT('agoda_', @STORE_ID), 'OTA', TRUE, TRUE, '#E74C3C', 'PERCENTAGE', 0, TRUE, 'Agoda是全球领先的在线旅游平台之一', NOW(), NOW()),
-- 2. Airbnb
(@STORE_ID, 'Airbnb', CONCAT('airbnb_', @STORE_ID), 'OTA', TRUE, TRUE, '#FF5A5F', 'PERCENTAGE', 0, TRUE, 'Airbnb是全球知名的民宿预订平台', NOW(), NOW()),
-- 3. Booking.com
(@STORE_ID, 'Booking.com', CONCAT('booking_', @STORE_ID), 'OTA', TRUE, TRUE, '#003580', 'PERCENTAGE', 0, TRUE, 'Booking.com是全球最大的在线住宿预订平台', NOW(), NOW()),
-- 4. Traveloka
(@STORE_ID, 'Traveloka', CONCAT('traveloka_', @STORE_ID), 'OTA', TRUE, TRUE, '#2C8DED', 'PERCENTAGE', 0, TRUE, 'Traveloka是东南亚领先的在线旅游平台', NOW(), NOW()),
-- 5. Trip.com
(@STORE_ID, 'Trip.com', CONCAT('trip_', @STORE_ID), 'OTA', TRUE, TRUE, '#2577E3', 'PERCENTAGE', 0, TRUE, 'Trip.com（携程）是中国领先的在线旅游平台', NOW(), NOW()),
-- 6. Expedia
(@STORE_ID, 'Expedia', CONCAT('expedia_', @STORE_ID), 'OTA', TRUE, TRUE, '#FFCB08', 'PERCENTAGE', 0, TRUE, 'Expedia是全球知名的在线旅游预订平台', NOW(), NOW()),
-- 7. Tiket.com
(@STORE_ID, 'Tiket.com', CONCAT('tiket_', @STORE_ID), 'OTA', TRUE, TRUE, '#0064D2', 'PERCENTAGE', 0, TRUE, 'Tiket.com是印度尼西亚领先的在线旅游平台', NOW(), NOW()),
-- 8. HostelWorld
(@STORE_ID, 'HostelWorld', CONCAT('hostelworld_', @STORE_ID), 'OTA', TRUE, TRUE, '#2B3E50', 'PERCENTAGE', 0, TRUE, 'HostelWorld是全球领先的青年旅舍预订平台', NOW(), NOW()),
-- 9. TuJia（途家）
(@STORE_ID, 'TuJia', CONCAT('tujia_', @STORE_ID), 'OTA', TRUE, TRUE, '#FF6B35', 'PERCENTAGE', 0, TRUE, '途家是中国领先的民宿短租平台', NOW(), NOW()),
-- 10. Neppan
(@STORE_ID, 'Neppan', CONCAT('neppan_', @STORE_ID), 'OTA', TRUE, TRUE, '#FF6B35', 'PERCENTAGE', 0, TRUE, 'Neppan渠道管理平台', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = name;  -- 如果已存在则不更新

-- 验证插入结果
SELECT
    id,
    store_id,
    name,
    code,
    type,
    is_active,
    enabled,
    price_adjustment_type,
    price_adjustment_value
FROM channels
WHERE store_id = @STORE_ID
ORDER BY id;
*/
