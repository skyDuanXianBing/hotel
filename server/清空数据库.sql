-- 清空现有数据的SQL脚本
-- 开发环境使用: 清空所有业务数据表,保留用户表

-- 1. 清空预订表(会级联清空相关的支付、消费记录等)
TRUNCATE TABLE reservations CASCADE;

-- 2. 清空渠道表
TRUNCATE TABLE channels CASCADE;

-- 3. 清空房型表(会级联清空房间表)
TRUNCATE TABLE room_types CASCADE;

-- 4. 清空房间表(如果没有被级联清空)
TRUNCATE TABLE rooms CASCADE;

-- 5. 清空其他可能的业务数据表(如果存在)
DO $$
BEGIN
    -- 安全地清空可能存在的表
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments') THEN
        TRUNCATE TABLE payments CASCADE;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'consumptions') THEN
        TRUNCATE TABLE consumptions CASCADE;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'room_prices') THEN
        TRUNCATE TABLE room_prices CASCADE;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notes') THEN
        TRUNCATE TABLE notes CASCADE;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_box') THEN
        TRUNCATE TABLE order_box CASCADE;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'room_status_shares') THEN
        TRUNCATE TABLE room_status_shares CASCADE;
    END IF;
END $$;

-- 重置序列(如果需要ID从1重新开始)
DO $$
BEGIN
    -- 安全地重置序列
    IF EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'reservations_id_seq') THEN
        ALTER SEQUENCE reservations_id_seq RESTART WITH 1;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'channels_id_seq') THEN
        ALTER SEQUENCE channels_id_seq RESTART WITH 1;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'room_types_id_seq') THEN
        ALTER SEQUENCE room_types_id_seq RESTART WITH 1;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'rooms_id_seq') THEN
        ALTER SEQUENCE rooms_id_seq RESTART WITH 1;
    END IF;
END $$;

-- 查看清空结果
SELECT 'reservations' as table_name, COUNT(*) as count FROM reservations
UNION ALL
SELECT 'channels', COUNT(*) FROM channels
UNION ALL
SELECT 'room_types', COUNT(*) FROM room_types
UNION ALL
SELECT 'rooms', COUNT(*) FROM rooms;

-- 查看用户表数据(验证用户数据未被删除)
SELECT id, username, email FROM users;
