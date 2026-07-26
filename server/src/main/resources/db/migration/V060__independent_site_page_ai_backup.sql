-- 独立站重构第 2 期：AI 局部修改的单步备份列。
-- 注意：Flyway 默认关闭（spring.flyway.enabled=false），本迁移需在本地库手动执行；
-- 语句用 information_schema 守卫保证幂等。

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_site_pages'
      AND column_name = 'draft_backup_schema_json'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE independent_site_pages
        ADD COLUMN draft_backup_schema_json LONGTEXT NULL AFTER draft_schema_json',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;
