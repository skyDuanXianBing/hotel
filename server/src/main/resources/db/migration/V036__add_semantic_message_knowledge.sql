SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'semantic_text'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN semantic_text MEDIUMTEXT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'search_intents_json'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN search_intents_json MEDIUMTEXT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_vector'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_vector MEDIUMTEXT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_status'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_status VARCHAR(20) NOT NULL DEFAULT ''PENDING''',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_provider'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_provider VARCHAR(60) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_model'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_model VARCHAR(120) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_dimensions'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_dimensions INT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_input_hash'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_input_hash CHAR(64) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_error'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_error VARCHAR(500) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_updated_at'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_updated_at DATETIME NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_attempt_count'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_attempt_count INT NOT NULL DEFAULT 0',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_next_attempt_at'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_next_attempt_at DATETIME NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_lease_owner'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_lease_owner VARCHAR(120) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'embedding_lease_until'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN embedding_lease_until DATETIME NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE message_knowledge_items
SET embedding_status = 'PENDING'
WHERE embedding_status IS NULL;

UPDATE message_knowledge_items
SET embedding_attempt_count = 0
WHERE embedding_attempt_count IS NULL;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND INDEX_NAME = 'idx_msg_knowledge_embedding_due'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_msg_knowledge_embedding_due ON message_knowledge_items (embedding_status, embedding_next_attempt_at, embedding_lease_until, store_id)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND INDEX_NAME = 'idx_msg_knowledge_embedding_ready'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_msg_knowledge_embedding_ready ON message_knowledge_items (store_id, status, embedding_status, scope_type, scope_id)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
