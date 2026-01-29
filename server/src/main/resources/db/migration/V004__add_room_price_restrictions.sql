-- Add restriction flags to room_prices for SU ARI (closeRoom/CTA/CTD)

-- close_room
SET @col := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'room_prices'
    AND COLUMN_NAME = 'close_room'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE room_prices ADD COLUMN close_room TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- cta
SET @col := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'room_prices'
    AND COLUMN_NAME = 'cta'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE room_prices ADD COLUMN cta TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ctd
SET @col := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'room_prices'
    AND COLUMN_NAME = 'ctd'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE room_prices ADD COLUMN ctd TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
