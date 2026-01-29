CREATE TABLE IF NOT EXISTS su_reservation_webhook_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    hotel_id VARCHAR(50) NOT NULL,
    reservation_notif_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    payload_json JSON,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    last_error TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_su_reservation_webhook_events (hotel_id, reservation_notif_id),
    INDEX idx_su_webhook_events_status_retry (status, next_retry_at),
    INDEX idx_su_webhook_events_store (store_id),
    INDEX idx_su_webhook_events_hotel (hotel_id)
);

-- Add columns to reservations table if it exists (and if the column does not exist).
SET @reservations_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
);

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'su_hotel_id'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN su_hotel_id VARCHAR(50)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'su_reservation_id'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN su_reservation_id VARCHAR(100)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'reservation_notif_id'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN reservation_notif_id VARCHAR(100)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'room_reservation_id'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN room_reservation_id VARCHAR(100)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'currency_code'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN currency_code VARCHAR(10)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'modified_at'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN modified_at DATETIME', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
