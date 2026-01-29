CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT,
    operation_type VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    operator VARCHAR(100) NOT NULL,
    timestamp DATETIME NOT NULL,
    content TEXT,
    details JSON,
    store_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_operation_logs_reservation_id (reservation_id),
    INDEX idx_operation_logs_store_id (store_id),
    INDEX idx_operation_logs_store_time (store_id, timestamp)
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
      AND column_name = 'payment_method'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN payment_method VARCHAR(50)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'commission'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN commission DECIMAL(10, 2)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'other_fees'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN other_fees DECIMAL(10, 2)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'price_plan'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN price_plan VARCHAR(100)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'special_requests'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN special_requests TEXT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'booking_date'
);
SET @sql := IF(@reservations_table_exists > 0 AND @col_exists = 0, 'ALTER TABLE reservations ADD COLUMN booking_date DATETIME', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
