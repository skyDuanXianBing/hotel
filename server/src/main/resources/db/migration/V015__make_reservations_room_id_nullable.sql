SET @table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reservations'
);

SET @is_nullable := (
    SELECT IS_NULLABLE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reservations'
      AND COLUMN_NAME = 'room_id'
    LIMIT 1
);

SET @ddl := (
    SELECT IF(
        @table_exists = 0 OR @is_nullable IS NULL OR @is_nullable = 'YES',
        'SELECT 1',
        'ALTER TABLE reservations MODIFY COLUMN room_id BIGINT NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
