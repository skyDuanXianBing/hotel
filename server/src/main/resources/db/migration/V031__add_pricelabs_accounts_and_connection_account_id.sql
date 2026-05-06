CREATE TABLE IF NOT EXISTS pricelabs_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    account_name VARCHAR(120) NOT NULL,
    pricelabs_email VARCHAR(255) NOT NULL,
    is_enabled BIT NOT NULL DEFAULT b'1',
    created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pricelabs_accounts_store_name (store_id, account_name),
    UNIQUE KEY uk_pricelabs_accounts_store_email (store_id, pricelabs_email)
);

SET @account_id_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pricelabs_connections'
      AND COLUMN_NAME = 'account_id'
);

SET @add_account_id_sql = IF(
    @account_id_column_exists = 0,
    'ALTER TABLE pricelabs_connections ADD COLUMN account_id BIGINT NULL AFTER store_id',
    'SELECT 1'
);
PREPARE add_account_id_stmt FROM @add_account_id_sql;
EXECUTE add_account_id_stmt;
DEALLOCATE PREPARE add_account_id_stmt;

INSERT INTO pricelabs_accounts (store_id, account_name, pricelabs_email, is_enabled, created_at, updated_at)
SELECT
    pi.store_id,
    COALESCE(NULLIF(TRIM(pi.pricelabs_email), ''), CONCAT('PriceLabs-', pi.store_id)),
    TRIM(pi.pricelabs_email),
    COALESCE(pi.is_enabled, b'1'),
    COALESCE(pi.created_at, NOW()),
    COALESCE(pi.updated_at, NOW())
FROM pricelabs_integrations pi
WHERE pi.pricelabs_email IS NOT NULL
  AND TRIM(pi.pricelabs_email) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM pricelabs_accounts pa
      WHERE pa.store_id = pi.store_id
        AND pa.pricelabs_email = TRIM(pi.pricelabs_email)
  );

UPDATE pricelabs_connections pc
JOIN pricelabs_integrations pi
  ON pi.store_id = pc.store_id
JOIN pricelabs_accounts pa
  ON pa.store_id = pc.store_id
 AND pa.pricelabs_email = TRIM(pi.pricelabs_email)
SET pc.account_id = pa.id
WHERE pc.account_id IS NULL
  AND pi.pricelabs_email IS NOT NULL
  AND TRIM(pi.pricelabs_email) <> '';
