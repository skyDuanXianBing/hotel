SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'room_prices'
              AND COLUMN_NAME = 'price_source'
        ),
        'SELECT 1',
        'ALTER TABLE room_prices ADD COLUMN price_source VARCHAR(32) NULL AFTER price'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'room_prices'
              AND COLUMN_NAME = 'manual_override'
        ),
        'SELECT 1',
        'ALTER TABLE room_prices ADD COLUMN manual_override TINYINT(1) NOT NULL DEFAULT 0 AFTER price_source'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'room_prices'
              AND COLUMN_NAME = 'manual_override_until'
        ),
        'SELECT 1',
        'ALTER TABLE room_prices ADD COLUMN manual_override_until DATE NULL AFTER manual_override'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE room_prices
SET price_source = 'SYSTEM'
WHERE price_source IS NULL;

UPDATE room_prices
SET manual_override = 0
WHERE manual_override IS NULL;
