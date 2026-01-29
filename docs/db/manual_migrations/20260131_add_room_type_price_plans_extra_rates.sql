-- 手动数据库变更：room_type_price_plans 增加额外成人/儿童加价字段
-- 背景：即使 spring.jpa.hibernate.ddl-auto=update，在某些环境也可能因缺列导致启动时直接报错。
--
-- 请在执行前先确认表名与当前数据库一致。
-- 如列已存在，请删除对应语句或按需调整。

ALTER TABLE room_type_price_plans
  ADD COLUMN extra_adult_rate DECIMAL(10, 2) NULL;

ALTER TABLE room_type_price_plans
  ADD COLUMN extra_child_rate DECIMAL(10, 2) NULL;

