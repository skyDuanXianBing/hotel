-- Upgrade smart-lock room bindings from a single device target to explicit control/passcode roles.
-- Legacy device_id/provider_lock_id remain as compatibility aliases and are backfilled from the
-- control role first, then the passcode role.

SET @bindings_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
);

SET @passcodes_table_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_passcode_records'
);

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'control_device_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN control_device_id BIGINT NULL AFTER device_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'control_provider_lock_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN control_provider_lock_id VARCHAR(120) NULL AFTER control_device_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'passcode_device_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN passcode_device_id BIGINT NULL AFTER control_provider_lock_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'passcode_provider_lock_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN passcode_provider_lock_id VARCHAR(120) NULL AFTER passcode_device_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'active_control_provider_lock_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN active_control_provider_lock_id VARCHAR(120) GENERATED ALWAYS AS (CASE WHEN status = ''ACTIVE'' THEN control_provider_lock_id ELSE NULL END) STORED AFTER active_provider_lock_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND column_name = 'active_passcode_provider_lock_id'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD COLUMN active_passcode_provider_lock_id VARCHAR(120) GENERATED ALWAYS AS (CASE WHEN status = ''ACTIVE'' THEN passcode_provider_lock_id ELSE NULL END) STORED AFTER active_control_provider_lock_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE smart_lock_room_bindings binding
JOIN smart_lock_devices device
  ON device.id = binding.device_id
 AND device.store_id = binding.store_id
 AND device.integration_id = binding.integration_id
 AND device.provider = binding.provider
SET binding.control_device_id = device.id,
    binding.control_provider_lock_id = device.provider_lock_id,
    binding.passcode_device_id = device.id,
    binding.passcode_provider_lock_id = device.provider_lock_id
WHERE @bindings_table_exists > 0
  AND binding.provider = 'TTLOCK'
  AND binding.control_device_id IS NULL
  AND binding.passcode_device_id IS NULL;

UPDATE smart_lock_room_bindings binding
JOIN smart_lock_devices device
  ON device.id = binding.device_id
 AND device.store_id = binding.store_id
 AND device.integration_id = binding.integration_id
 AND device.provider = binding.provider
SET binding.control_device_id = device.id,
    binding.control_provider_lock_id = device.provider_lock_id
WHERE @bindings_table_exists > 0
  AND binding.provider = 'SWITCHBOT'
  AND device.device_type IN (
      'Lock',
      'Lock Pro',
      'Lock Lite',
      'Lock Ultra',
      'Smart Lock',
      'Smart Lock Pro',
      'Smart Lock Ultra',
      'Smart Lock Pro Wifi',
      'Lock Vision',
      'Lock Vision Pro'
  )
  AND binding.control_device_id IS NULL;

UPDATE smart_lock_room_bindings binding
JOIN smart_lock_devices device
  ON device.id = binding.device_id
 AND device.store_id = binding.store_id
 AND device.integration_id = binding.integration_id
 AND device.provider = binding.provider
SET binding.passcode_device_id = device.id,
    binding.passcode_provider_lock_id = device.provider_lock_id
WHERE @bindings_table_exists > 0
  AND binding.provider = 'SWITCHBOT'
  AND device.device_type IN (
      'Keypad',
      'Keypad Touch',
      'Keypad Vision',
      'Keypad Vision Pro',
      'Lock Vision',
      'Lock Vision Pro'
  )
  AND binding.passcode_device_id IS NULL;

UPDATE smart_lock_room_bindings binding
JOIN smart_lock_devices keypad
  ON keypad.id = binding.device_id
 AND keypad.store_id = binding.store_id
 AND keypad.integration_id = binding.integration_id
 AND keypad.provider = binding.provider
JOIN smart_lock_devices linked_lock
  ON linked_lock.store_id = binding.store_id
 AND linked_lock.integration_id = binding.integration_id
 AND linked_lock.provider = binding.provider
 AND linked_lock.provider_lock_id = NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(
      IF(JSON_VALID(keypad.raw_data_json), keypad.raw_data_json, '{}'),
      '$.lockDeviceId'
  ))), '')
SET binding.control_device_id = linked_lock.id,
    binding.control_provider_lock_id = linked_lock.provider_lock_id
WHERE @bindings_table_exists > 0
  AND binding.provider = 'SWITCHBOT'
  AND keypad.device_type IN ('Keypad', 'Keypad Touch', 'Keypad Vision', 'Keypad Vision Pro')
  AND linked_lock.device_type IN (
      'Lock',
      'Lock Pro',
      'Lock Lite',
      'Lock Ultra',
      'Smart Lock',
      'Smart Lock Pro',
      'Smart Lock Ultra',
      'Smart Lock Pro Wifi',
      'Lock Vision',
      'Lock Vision Pro'
  )
  AND binding.control_device_id IS NULL;

UPDATE smart_lock_room_bindings binding
JOIN smart_lock_devices keypad
  ON keypad.id = binding.device_id
 AND keypad.store_id = binding.store_id
 AND keypad.integration_id = binding.integration_id
 AND keypad.provider = binding.provider
JOIN smart_lock_devices linked_lock
  ON linked_lock.store_id = binding.store_id
 AND linked_lock.integration_id = binding.integration_id
 AND linked_lock.provider = binding.provider
 AND linked_lock.provider_lock_id = keypad.auxiliary_device_id
SET binding.control_device_id = linked_lock.id,
    binding.control_provider_lock_id = linked_lock.provider_lock_id
WHERE @bindings_table_exists > 0
  AND binding.provider = 'SWITCHBOT'
  AND keypad.device_type IN ('Keypad', 'Keypad Touch', 'Keypad Vision', 'Keypad Vision Pro')
  AND NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(
      IF(JSON_VALID(keypad.raw_data_json), keypad.raw_data_json, '{}'),
      '$.lockDeviceId'
  ))), '') IS NULL
  AND keypad.auxiliary_device_id IS NOT NULL
  AND keypad.auxiliary_device_id <> ''
  AND (
      NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(
          IF(JSON_VALID(keypad.raw_data_json), keypad.raw_data_json, '{}'),
          '$.hubDeviceId'
      ))), '') IS NULL
      OR keypad.auxiliary_device_id <> NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(
          IF(JSON_VALID(keypad.raw_data_json), keypad.raw_data_json, '{}'),
          '$.hubDeviceId'
      ))), '')
  )
  AND linked_lock.device_type IN (
      'Lock',
      'Lock Pro',
      'Lock Lite',
      'Lock Ultra',
      'Smart Lock',
      'Smart Lock Pro',
      'Smart Lock Ultra',
      'Smart Lock Pro Wifi',
      'Lock Vision',
      'Lock Vision Pro'
  )
  AND binding.control_device_id IS NULL;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_passcode_records'
      AND column_name = 'passcode_role'
);
SET @ddl := IF(
    @passcodes_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_passcode_records ADD COLUMN passcode_role VARCHAR(30) NOT NULL DEFAULT ''PASSCODE'' AFTER provider_lock_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_passcode_records'
      AND column_name = 'passcode_device_id'
);
SET @ddl := IF(
    @passcodes_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_passcode_records ADD COLUMN passcode_device_id BIGINT NULL AFTER passcode_role',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_passcode_records'
      AND column_name = 'passcode_provider_lock_id'
);
SET @ddl := IF(
    @passcodes_table_exists > 0 AND @column_exists = 0,
    'ALTER TABLE smart_lock_passcode_records ADD COLUMN passcode_provider_lock_id VARCHAR(120) NULL AFTER passcode_device_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE smart_lock_passcode_records record
SET record.passcode_role = 'PASSCODE',
    record.passcode_provider_lock_id = record.provider_lock_id
WHERE @passcodes_table_exists > 0
  AND record.passcode_provider_lock_id IS NULL;

UPDATE smart_lock_passcode_records record
JOIN smart_lock_room_bindings binding ON binding.id = record.binding_id
SET record.passcode_device_id = binding.passcode_device_id
WHERE @passcodes_table_exists > 0
  AND record.passcode_device_id IS NULL
  AND binding.passcode_device_id IS NOT NULL
  AND binding.passcode_provider_lock_id = record.passcode_provider_lock_id;

UPDATE smart_lock_passcode_records record
JOIN smart_lock_room_bindings binding ON binding.id = record.binding_id
SET record.passcode_device_id = binding.device_id
WHERE @passcodes_table_exists > 0
  AND record.passcode_device_id IS NULL
  AND binding.provider_lock_id = record.passcode_provider_lock_id;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'uk_smart_lock_bindings_active_provider_lock'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists > 0,
    'ALTER TABLE smart_lock_room_bindings DROP INDEX uk_smart_lock_bindings_active_provider_lock',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'uk_smart_lock_bindings_active_control_lock'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD UNIQUE KEY uk_smart_lock_bindings_active_control_lock (store_id, provider, active_control_provider_lock_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'uk_smart_lock_bindings_active_passcode_lock'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD UNIQUE KEY uk_smart_lock_bindings_active_passcode_lock (store_id, provider, active_passcode_provider_lock_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'idx_smart_lock_bindings_store_control_device'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD INDEX idx_smart_lock_bindings_store_control_device (store_id, control_device_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'idx_smart_lock_bindings_store_passcode_device'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD INDEX idx_smart_lock_bindings_store_passcode_device (store_id, passcode_device_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'idx_smart_lock_bindings_store_control_lock'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD INDEX idx_smart_lock_bindings_store_control_lock (store_id, provider, control_provider_lock_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_room_bindings'
      AND index_name = 'idx_smart_lock_bindings_store_passcode_lock'
);
SET @ddl := IF(
    @bindings_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_room_bindings ADD INDEX idx_smart_lock_bindings_store_passcode_lock (store_id, provider, passcode_provider_lock_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'smart_lock_passcode_records'
      AND index_name = 'idx_smart_lock_passcodes_store_passcode_lock'
);
SET @ddl := IF(
    @passcodes_table_exists > 0 AND @index_exists = 0,
    'ALTER TABLE smart_lock_passcode_records ADD INDEX idx_smart_lock_passcodes_store_passcode_lock (store_id, provider, passcode_provider_lock_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
