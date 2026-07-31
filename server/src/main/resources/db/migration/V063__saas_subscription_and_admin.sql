-- ============================================================================
-- SaaS 订阅系统 V1（P1-A 后端核心）
-- 说明：application.properties 中 ddl-auto=update 会自动建表；本脚本用于生产/审计交付，
--       与 JPA 实体（server/demo/entity/saas/）保持一致，全部使用 IF NOT EXISTS / 幂等写法。
-- 数据库：MySQL 8（utf8mb4）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 功能字典
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saas_feature (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    feature_code        VARCHAR(64)  NOT NULL,
    name                VARCHAR(128) NOT NULL,
    type                VARCHAR(20)  NOT NULL,           -- BOOLEAN / QUOTA / CAPACITY
    unit                VARCHAR(32)  NULL,
    default_reset_cycle VARCHAR(20)  NULL,               -- MONTHLY / NONE（仅 QUOTA 使用）
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saas_feature_code (feature_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ----------------------------------------------------------------------------
-- 2. 套餐模板（版本化：改价 = 上架新行，旧行停售保留）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saas_package (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(128)  NOT NULL,
    version     INT           NOT NULL DEFAULT 1,
    price       DECIMAL(12,2) NOT NULL,
    period      VARCHAR(10)   NOT NULL,                  -- MONTH / YEAR
    status      VARCHAR(20)   NOT NULL,                  -- ON_SHELF / OFF_SHELF
    description VARCHAR(500)  NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ----------------------------------------------------------------------------
-- 3. 套餐权益模板行（quota_limit NULL = 不限）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saas_package_feature (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    package_id   BIGINT      NOT NULL,
    feature_code VARCHAR(64) NOT NULL,
    quota_limit  BIGINT      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saas_package_feature (package_id, feature_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ----------------------------------------------------------------------------
-- 4. 门店订阅（成交时冻结权益快照，改模板不影响存量）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saas_subscription (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    store_id                  BIGINT        NOT NULL,
    package_id                BIGINT        NOT NULL,
    package_name              VARCHAR(128)  NOT NULL,
    entitlement_snapshot_json LONGTEXT      NOT NULL,
    price_paid                DECIMAL(12,2) NOT NULL,
    start_time                DATETIME(6)   NOT NULL,
    end_time                  DATETIME(6)   NOT NULL,
    status                    VARCHAR(20)   NOT NULL,    -- ACTIVE / EXPIRED / CANCELLED
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_saas_subscription_store_status (store_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ----------------------------------------------------------------------------
-- 5. 配额账（total_quota NULL = 不限；period 窗口 + reset_cycle 支持惰性滚动）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saas_quota_account (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    store_id     BIGINT      NOT NULL,
    feature_code VARCHAR(64) NOT NULL,
    total_quota  BIGINT      NULL,
    used_quota   BIGINT      NOT NULL DEFAULT 0,
    period_start DATETIME(6) NOT NULL,
    period_end   DATETIME(6) NOT NULL,
    reset_cycle  VARCHAR(20) NOT NULL,                   -- MONTHLY / NONE
    version      BIGINT      NOT NULL DEFAULT 0,         -- 乐观锁兜底（扣减走原子条件 UPDATE）
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saas_quota_account_store_feature (store_id, feature_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ----------------------------------------------------------------------------
-- 6. 配额流水（DEDUCT / REFUND / GRANT / ADJUST / RESET）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saas_quota_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    store_id     BIGINT       NOT NULL,
    feature_code VARCHAR(64)  NOT NULL,
    delta        BIGINT       NOT NULL,                  -- used_quota 变动量
    action       VARCHAR(20)  NOT NULL,
    biz_id       VARCHAR(200) NULL,
    operator     VARCHAR(64)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_saas_quota_log_store_feature (store_id, feature_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ----------------------------------------------------------------------------
-- 7. 套餐购买订单（DIRECT 直连；STRIPE 预留）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saas_billing_order (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    store_id   BIGINT        NOT NULL,
    package_id BIGINT        NOT NULL,
    amount     DECIMAL(12,2) NOT NULL,
    provider   VARCHAR(30)   NOT NULL,                   -- DIRECT / STRIPE（预留）
    status     VARCHAR(20)   NOT NULL,                   -- PAID
    created_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_saas_billing_order_store (store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- 种子数据
-- ============================================================================

-- 8. 功能字典（幂等）
INSERT INTO saas_feature (feature_code, name, type, unit, default_reset_cycle, created_at, updated_at)
SELECT seed.*, NOW(6), NOW(6) FROM (
    SELECT 'independent_website' AS feature_code, '独立站模块'   AS name, 'BOOLEAN'  AS type, NULL   AS unit, NULL       AS default_reset_cycle
    UNION ALL SELECT 'ai_website_gen',      'AI 建站生成次数', 'QUOTA',           '次', 'MONTHLY'
    UNION ALL SELECT 'room_count',          '可存在房间数量',  'CAPACITY',        '间', NULL
) seed
WHERE NOT EXISTS (SELECT 1 FROM saas_feature f WHERE f.feature_code = seed.feature_code);

-- 9. 默认套餐（幂等：按 name 判断）
--    标准版 ¥99/月 = 10 间房，无独立站（AI 建站依附独立站模块，不单独售卖）
--    豪华版 ¥999/月 = 50 间 + AI 50 次/月 + 独立站
--    旗舰版 ¥2999/月 = 不限房间 + AI 200 次/月 + 独立站
--    默认版 ¥0（OFF_SHELF，不展示）= 不限 + 独立站（存量门店兼容保险）
INSERT INTO saas_package (name, version, price, period, status, description, created_at, updated_at)
SELECT seed.*, NOW(6), NOW(6) FROM (
    SELECT '标准版' AS name, 1 AS version, 99.00   AS price, 'MONTH' AS period, 'ON_SHELF'  AS status, '10 间房'                                                     AS description
    UNION ALL SELECT '豪华版', 1, 999.00,  'MONTH', 'ON_SHELF',  '50 间房，AI 建站 50 次/月，含独立站'
    UNION ALL SELECT '旗舰版', 1, 2999.00, 'MONTH', 'ON_SHELF',  '不限房间，AI 建站 200 次/月，含独立站'
    UNION ALL SELECT '默认版', 1, 0.00,    'MONTH', 'OFF_SHELF', '存量门店默认权益：不限房间与 AI 次数，含独立站'
) seed
WHERE NOT EXISTS (SELECT 1 FROM saas_package p WHERE p.name = seed.name);

-- 10. 套餐权益模板行（幂等）
INSERT INTO saas_package_feature (package_id, feature_code, quota_limit)
SELECT p.id, seed.feature_code, seed.quota_limit FROM (
    SELECT '标准版' AS package_name, 'room_count'          AS feature_code, 10   AS quota_limit
    UNION ALL SELECT '豪华版', 'room_count',           50
    UNION ALL SELECT '豪华版', 'ai_website_gen',       50
    UNION ALL SELECT '豪华版', 'independent_website',  NULL
    UNION ALL SELECT '旗舰版', 'room_count',           NULL
    UNION ALL SELECT '旗舰版', 'ai_website_gen',       200
    UNION ALL SELECT '旗舰版', 'independent_website',  NULL
    UNION ALL SELECT '默认版', 'room_count',           NULL
    UNION ALL SELECT '默认版', 'ai_website_gen',       NULL
    UNION ALL SELECT '默认版', 'independent_website',  NULL
) seed
JOIN saas_package p ON p.name = seed.package_name
WHERE NOT EXISTS (
    SELECT 1 FROM saas_package_feature pf
    WHERE pf.package_id = p.id AND pf.feature_code = seed.feature_code
);

-- 11. 存量门店迁移：为所有尚无 ACTIVE 订阅的门店发放“默认版”订阅（不限额度 + 独立站，
--     iOS/Web 行为完全不变的兼容保险）。end_time 远期，实际套餐切换后此行被 CANCELLED。
INSERT INTO saas_subscription (
    store_id, package_id, package_name, entitlement_snapshot_json,
    price_paid, start_time, end_time, status, created_at, updated_at
)
SELECT
    s.id,
    p.id,
    p.name,
    '{"features":[{"featureCode":"independent_website","type":"BOOLEAN","limit":null},{"featureCode":"ai_website_gen","type":"QUOTA","limit":null},{"featureCode":"room_count","type":"CAPACITY","limit":null}]}',
    0.00,
    NOW(6),
    '2099-12-31 23:59:59',
    'ACTIVE',
    NOW(6),
    NOW(6)
FROM stores s
JOIN saas_package p ON p.name = '默认版'
WHERE NOT EXISTS (
    SELECT 1 FROM saas_subscription sub
    WHERE sub.store_id = s.id AND sub.status = 'ACTIVE'
);

-- ============================================================================
-- P1-B：平台管理端（/api/admin/**）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 12. saas_feature 增加 description 列（管理端可维护的功能描述）。
--     MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema 保持幂等。
-- ----------------------------------------------------------------------------
SET @saas_feature_has_description := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'saas_feature' AND COLUMN_NAME = 'description'
);
SET @saas_feature_add_description := IF(
    @saas_feature_has_description = 0,
    'ALTER TABLE saas_feature ADD COLUMN description VARCHAR(500) NULL AFTER unit',
    'SELECT 1'
);
PREPARE stmt_saas_feature_description FROM @saas_feature_add_description;
EXECUTE stmt_saas_feature_description;
DEALLOCATE PREPARE stmt_saas_feature_description;

-- ----------------------------------------------------------------------------
-- 13. 平台管理员（独立认证：/api/admin/** 由 AdminAuthInterceptor 保护）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(64)  NOT NULL,
    password   VARCHAR(100) NOT NULL,                 -- BCrypt 哈希
    role       VARCHAR(20)  NOT NULL,                 -- SUPER / OPS
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_users_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 首个超管种子：admin / Admin@123456（BCrypt）。
-- ⚠️ 交付后请立即登录管理端修改初始密码。
INSERT INTO admin_users (username, password, role, is_active, created_at, updated_at)
SELECT 'admin', '$2a$10$/0NKjbuECcmY0fbCHeeK0Ouce9ylyci/01ai/dVHrfmhymwMm1ZTq', 'SUPER', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM admin_users u WHERE u.username = 'admin');
