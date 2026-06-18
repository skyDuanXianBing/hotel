SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_pending'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_pending TINYINT(1) NOT NULL DEFAULT 0',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_extract_after'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_extract_after DATETIME NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_extracting_until'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_extracting_until DATETIME NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_extracting_owner'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_extracting_owner VARCHAR(120) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_extracted_at'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_extracted_at DATETIME NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_extracted_message_id'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_extracted_message_id BIGINT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_dirty_message_id'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_dirty_message_id BIGINT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_error'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_error VARCHAR(500) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_attempt_count'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_attempt_count INT NOT NULL DEFAULT 0',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND COLUMN_NAME = 'knowledge_extractor_version'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE su_message_threads ADD COLUMN knowledge_extractor_version VARCHAR(40) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND INDEX_NAME = 'idx_su_msg_thread_kb_due'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_su_msg_thread_kb_due ON su_message_threads (knowledge_pending, knowledge_extract_after, knowledge_extracting_until, store_id, id)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_message_threads'
      AND INDEX_NAME = 'idx_su_msg_thread_store_kb_state'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_su_msg_thread_store_kb_state ON su_message_threads (store_id, knowledge_pending, knowledge_extracted_at)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
