-- 独立站重构第 1 期：一店多站 + 一站多页面。
-- 注意：Flyway 默认关闭（spring.flyway.enabled=false），本迁移需在本地库手动执行；
-- 全部语句用 information_schema 守卫保证幂等。

-- 1. 删除一店一站唯一约束
--    store_id 上有外键 fk_independent_sites_store（V057），InnoDB 要求引用列必须有索引，
--    唯一键 uk_independent_sites_store 正充当该角色，直接删会报 ERROR 1553；
--    先建普通索引 idx_independent_sites_store 承接外键，再删唯一键。
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND index_name = 'idx_independent_sites_store'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_independent_sites_store ON independent_sites (store_id)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND index_name = 'uk_independent_sites_store'
);
SET @ddl = IF(
    @index_exists > 0,
    'ALTER TABLE independent_sites DROP INDEX uk_independent_sites_store',
    'SELECT 1'
);
PREPARE drop_statement FROM @ddl;
EXECUTE drop_statement;
DEALLOCATE PREPARE drop_statement;

-- 2. 新增 name 列（先可空，回填后改 NOT NULL）
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'name'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE independent_sites ADD COLUMN name VARCHAR(120) NULL AFTER slug',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

UPDATE independent_sites SET name = slug WHERE name IS NULL;

SET @column_nullable = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'name'
      AND is_nullable = 'YES'
);
SET @ddl = IF(
    @column_nullable > 0,
    'ALTER TABLE independent_sites MODIFY COLUMN name VARCHAR(120) NOT NULL',
    'SELECT 1'
);
PREPARE modify_statement FROM @ddl;
EXECUTE modify_statement;
DEALLOCATE PREPARE modify_statement;

-- 3. 新增 theme_key 列
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'theme_key'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE independent_sites ADD COLUMN theme_key VARCHAR(30) NOT NULL DEFAULT ''classic'' AFTER name',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

-- 4. 页面表
CREATE TABLE IF NOT EXISTS independent_site_pages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    path VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(120) NOT NULL,
    seo_description VARCHAR(300) NULL,
    room_type_id BIGINT NULL,
    draft_schema_json LONGTEXT NULL,
    published_schema_json LONGTEXT NULL,
    draft_version BIGINT NOT NULL DEFAULT 0,
    draft_updated_at DATETIME NULL,
    published_at DATETIME NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_independent_site_pages_path UNIQUE (site_id, path),
    CONSTRAINT fk_independent_site_pages_site
        FOREIGN KEY (site_id) REFERENCES independent_sites(id) ON DELETE CASCADE,
    INDEX idx_independent_site_pages_store_site (store_id, site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 数据迁移：每个现存站点生成 HOME 页（仅当旧页面列仍存在时执行）
SET @legacy_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'page_schema_json'
);
SET @migrate = IF(
    @legacy_column_exists > 0,
    'INSERT INTO independent_site_pages (
        store_id, site_id, path, type, title, seo_description, room_type_id,
        draft_schema_json, published_schema_json, draft_version, draft_updated_at,
        published_at, sort_order, enabled, created_at, updated_at
    )
    SELECT
        s.store_id, s.id, ''/'', ''HOME'', COALESCE(s.name, s.slug), NULL, NULL,
        COALESCE(s.draft_page_schema_json, s.page_schema_json), s.page_schema_json,
        COALESCE(s.draft_version, 0), s.draft_updated_at,
        s.published_at, 0, 1, NOW(), NOW()
    FROM independent_sites s
    WHERE NOT EXISTS (
        SELECT 1 FROM independent_site_pages p WHERE p.site_id = s.id AND p.path = ''/''
    )',
    'SELECT 1'
);
PREPARE migrate_statement FROM @migrate;
EXECUTE migrate_statement;
DEALLOCATE PREPARE migrate_statement;

-- 6. 删除站点表上的 4 个旧页面列
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'page_schema_json'
);
SET @ddl = IF(
    @column_exists > 0,
    'ALTER TABLE independent_sites DROP COLUMN page_schema_json',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'draft_page_schema_json'
);
SET @ddl = IF(
    @column_exists > 0,
    'ALTER TABLE independent_sites DROP COLUMN draft_page_schema_json',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'draft_updated_at'
);
SET @ddl = IF(
    @column_exists > 0,
    'ALTER TABLE independent_sites DROP COLUMN draft_updated_at',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'independent_sites'
      AND column_name = 'draft_version'
);
SET @ddl = IF(
    @column_exists > 0,
    'ALTER TABLE independent_sites DROP COLUMN draft_version',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;
