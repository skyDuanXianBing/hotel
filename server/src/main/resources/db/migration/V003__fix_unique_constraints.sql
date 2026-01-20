-- Fix store-scoped unique constraints (room_types, rooms, reservations)

-- Drop single-column unique index on room_types.name if present
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'room_types'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'name'
  LIMIT 1
);
SET @sql := IF(@idx IS NULL, 'SELECT 1', CONCAT('DROP INDEX `', @idx, '` ON room_types'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop single-column unique index on room_types.code if present
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'room_types'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'code'
  LIMIT 1
);
SET @sql := IF(@idx IS NULL, 'SELECT 1', CONCAT('DROP INDEX `', @idx, '` ON room_types'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop single-column unique index on rooms.room_number if present
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rooms'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'room_number'
  LIMIT 1
);
SET @sql := IF(@idx IS NULL, 'SELECT 1', CONCAT('DROP INDEX `', @idx, '` ON rooms'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop single-column unique index on reservations.order_number if present
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reservations'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'order_number'
  LIMIT 1
);
SET @sql := IF(@idx IS NULL, 'SELECT 1', CONCAT('DROP INDEX `', @idx, '` ON reservations'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure composite unique index room_types(store_id, code)
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'room_types'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) IN ('store_id,code', 'code,store_id')
  LIMIT 1
);
SET @sql := IF(@idx IS NULL,
  'CREATE UNIQUE INDEX uk_room_types_store_code ON room_types (store_id, code)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure composite unique index room_types(store_id, name)
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'room_types'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) IN ('store_id,name', 'name,store_id')
  LIMIT 1
);
SET @sql := IF(@idx IS NULL,
  'CREATE UNIQUE INDEX uk_room_types_store_name ON room_types (store_id, name)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure composite unique index rooms(store_id, room_number)
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'rooms'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) IN ('store_id,room_number', 'room_number,store_id')
  LIMIT 1
);
SET @sql := IF(@idx IS NULL,
  'CREATE UNIQUE INDEX uk_rooms_store_room_number ON rooms (store_id, room_number)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure composite unique index reservations(store_id, order_number)
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reservations'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) IN ('store_id,order_number', 'order_number,store_id')
  LIMIT 1
);
SET @sql := IF(@idx IS NULL,
  'CREATE UNIQUE INDEX uk_reservations_store_order ON reservations (store_id, order_number)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
