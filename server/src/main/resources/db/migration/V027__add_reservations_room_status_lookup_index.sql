SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND index_name = 'idx_reservations_store_room_status_dates'
);

SET @sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_reservations_store_room_status_dates ON reservations (store_id, room_id, status, check_in_date, check_out_date)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
