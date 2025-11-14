-- 修复门店级实体的数据库架构
-- 从用户级架构迁移到门店级架构：将user_id从NOT NULL改为NULL

-- 1. 修改memos表的user_id列为可空
ALTER TABLE memos MODIFY COLUMN user_id BIGINT NULL;

-- 2. 修改consumption_items表的user_id列为可空
ALTER TABLE consumption_items MODIFY COLUMN user_id BIGINT NULL;

-- 3. 修改channels表的user_id列为可空
ALTER TABLE channels MODIFY COLUMN user_id BIGINT NULL;

-- 4. 修改auto_messages表的user_id列为可空
ALTER TABLE auto_messages MODIFY COLUMN user_id BIGINT NULL;

-- 验证修改
DESCRIBE memos;
DESCRIBE consumption_items;
DESCRIBE channels;
DESCRIBE auto_messages;

-- 说明：
-- 这些实体已从用户级隔离迁移到门店级隔离
-- store_id字段将由StoreScopedEntityListener自动填充
-- user_id字段已被标记为@Deprecated，将在未来版本中移除
