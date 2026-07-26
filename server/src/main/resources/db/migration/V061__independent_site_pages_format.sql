-- 独立站 Canvas 架构第 1 期：页面格式列。
-- 注意：Flyway 默认关闭（spring.flyway.enabled=false），本迁移需在本地库手动执行；
-- 语句用 information_schema 守卫保证幂等。
-- 存量页面全部为旧区块式 schema，由列默认值回填为 BLOCKS；新建站点/页面由应用层写入 CANVAS。

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_site_pages'
      AND column_name = 'format'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE independent_site_pages
        ADD COLUMN format VARCHAR(20) NOT NULL DEFAULT ''BLOCKS'' AFTER type',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;
