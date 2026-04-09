SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'pricelabs_integrations'
              AND COLUMN_NAME = 'distribution_mode'
        ),
        'SELECT 1',
        'ALTER TABLE pricelabs_integrations ADD COLUMN distribution_mode VARCHAR(32) NULL AFTER hook_url'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE pricelabs_integrations
SET distribution_mode = 'AVAILABILITY_ONLY'
WHERE distribution_mode IS NULL OR distribution_mode = '';

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'channel_prices'
              AND COLUMN_NAME = 'ota_sync_state'
        ),
        'SELECT 1',
        'ALTER TABLE channel_prices ADD COLUMN ota_sync_state VARCHAR(32) NULL AFTER is_synced_to_ota'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE channel_prices
SET ota_sync_state = CASE
    WHEN is_synced_to_ota = b'1' THEN 'SUCCESS'
    WHEN pricelabs_updated_at IS NOT NULL THEN 'NOT_REQUIRED'
    ELSE 'PENDING'
END
WHERE ota_sync_state IS NULL OR ota_sync_state = '';

-- 当前门店切换到 availability-only 分发
UPDATE pricelabs_integrations
SET distribution_mode = 'AVAILABILITY_ONLY'
WHERE store_id = 26;
