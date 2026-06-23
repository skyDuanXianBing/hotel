-- Compatibility migration for databases that already applied an older V040 smart-lock schema.
-- Current V040 creates active-only generated columns and unique indexes. Old V040 used direct
-- room/provider-lock uniqueness, which blocks soft-deleted historical bindings. This script is
-- intentionally conditional so a fresh database that already ran the current V040 treats V041 as no-op.

SET @bindings_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
);

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'status'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''ACTIVE'' AFTER provider_lock_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'status'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists > 0,
    'UPDATE smart_lock_room_bindings SET status = ''ACTIVE'' WHERE status IS NULL OR status = ''''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'active_room_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN active_room_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = ''ACTIVE'' THEN room_id ELSE NULL END) STORED AFTER status',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'active_provider_lock_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN active_provider_lock_id VARCHAR(120) GENERATED ALWAYS AS (CASE WHEN status = ''ACTIVE'' THEN provider_lock_id ELSE NULL END) STORED AFTER active_room_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'uk_smart_lock_bindings_store_room'
      AND non_unique = 0
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE smart_lock_room_bindings DROP INDEX uk_smart_lock_bindings_store_room',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'uk_smart_lock_bindings_store_provider_lock'
      AND non_unique = 0
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE smart_lock_room_bindings DROP INDEX uk_smart_lock_bindings_store_provider_lock',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'uk_smart_lock_bindings_active_room'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD UNIQUE KEY uk_smart_lock_bindings_active_room (store_id, active_room_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'uk_smart_lock_bindings_active_provider_lock'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD UNIQUE KEY uk_smart_lock_bindings_active_provider_lock (store_id, provider, active_provider_lock_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'idx_smart_lock_bindings_store_room'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD INDEX idx_smart_lock_bindings_store_room (store_id, room_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'idx_smart_lock_bindings_store_provider_lock'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD INDEX idx_smart_lock_bindings_store_provider_lock (store_id, provider, provider_lock_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
