-- Allow per-room-type permissions on role_permissions by including room_type_id in uniqueness.
-- Also normalize null room_type_id to 0 so uniqueness works consistently in MySQL (unique indexes allow multiple NULLs).

-- Normalize nullable columns before constraints
UPDATE role_permissions SET all_room_types = 0 WHERE all_room_types IS NULL;
UPDATE role_permissions SET room_type_id = 0 WHERE room_type_id IS NULL;

-- Ensure room_type_id is NOT NULL with default 0 (0 means "not scoped to a specific room type")
ALTER TABLE role_permissions
  MODIFY COLUMN room_type_id BIGINT NOT NULL DEFAULT 0;

-- Ensure supporting index on role_id exists so dropping composite unique keys won't break FK requirements
-- (MySQL may use the composite unique key as the only index for FK role_permissions.role_id -> roles.id)
SET @idx_role := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'role_permissions'
    AND INDEX_NAME = 'idx_role_permissions_role_id'
  LIMIT 1
);
SET @sql := IF(@idx_role IS NULL, 'CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure unique index on (role_id, module, action, room_type_id)
SET @idx_new := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'role_permissions'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) IN ('role_id,module,action,room_type_id', 'role_id,module,room_type_id,action', 'role_id,action,module,room_type_id', 'role_id,action,room_type_id,module', 'role_id,room_type_id,module,action', 'role_id,room_type_id,action,module')
  LIMIT 1
);
SET @sql := IF(@idx_new IS NULL,
  'CREATE UNIQUE INDEX uk_role_permissions_role_module_action_room_type ON role_permissions (role_id, module, action, room_type_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop legacy unique index/constraint on (role_id, module, action) if present (do this AFTER creating the new index)
SET @idx_legacy := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'role_permissions'
    AND NON_UNIQUE = 0
  GROUP BY INDEX_NAME
  HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) IN ('role_id,module,action', 'role_id,action,module', 'module,role_id,action', 'module,action,role_id', 'action,role_id,module', 'action,module,role_id')
  LIMIT 1
);
SET @sql := IF(@idx_legacy IS NULL, 'SELECT 1', CONCAT('DROP INDEX `', @idx_legacy, '` ON role_permissions'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
