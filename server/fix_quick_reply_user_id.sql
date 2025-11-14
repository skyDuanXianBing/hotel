-- 修复QuickReply表的user_id字段
-- 从门店级架构迁移：将user_id从NOT NULL改为NULL

-- 修改quick_replies表的user_id列为可空
ALTER TABLE quick_replies MODIFY COLUMN user_id BIGINT NULL;

-- 验证修改
DESCRIBE quick_replies;
