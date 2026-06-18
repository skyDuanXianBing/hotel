SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND COLUMN_NAME = 'fact_hash'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN fact_hash CHAR(64) NULL',
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
      AND COLUMN_NAME = 'extractor_version'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_items ADD COLUMN extractor_version VARCHAR(40) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_items'
      AND INDEX_NAME = 'uk_msg_knowledge_item_scope_topic'
);
-- Replace the legacy topic-only uniqueness before creating the fact-level unique key.
SET @idx_sql := IF(
    @idx_exists > 0,
    'ALTER TABLE message_knowledge_items DROP INDEX uk_msg_knowledge_item_scope_topic',
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
      AND INDEX_NAME = 'uk_msg_knowledge_item_scope_fact'
);
-- New uniqueness allows multiple reusable facts under the same store/scope/topic.
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE UNIQUE INDEX uk_msg_knowledge_item_scope_fact ON message_knowledge_items (store_id, scope_key, topic_hash, fact_hash)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'source_kind'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_evidence ADD COLUMN source_kind VARCHAR(30) NOT NULL DEFAULT ''MESSAGE_PAIR''',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'source_message_ids_json'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_evidence ADD COLUMN source_message_ids_json MEDIUMTEXT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'source_message_start_id'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_evidence ADD COLUMN source_message_start_id BIGINT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'source_message_end_id'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_evidence ADD COLUMN source_message_end_id BIGINT NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'extractor_version'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_evidence ADD COLUMN extractor_version VARCHAR(40) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'source_fingerprint'
);
SET @column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE message_knowledge_evidence ADD COLUMN source_fingerprint CHAR(64) NULL',
    'DO 0'
);
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND INDEX_NAME = 'uk_msg_knowledge_evidence_source'
);
SET @idx_sql := IF(
    @idx_exists > 0,
    'ALTER TABLE message_knowledge_evidence DROP INDEX uk_msg_knowledge_evidence_source',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE message_knowledge_evidence MODIFY COLUMN source_guest_message_id BIGINT NULL;
ALTER TABLE message_knowledge_evidence MODIFY COLUMN source_staff_message_id BIGINT NULL;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND INDEX_NAME = 'uk_msg_knowledge_evidence_fingerprint'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE UNIQUE INDEX uk_msg_knowledge_evidence_fingerprint ON message_knowledge_evidence (store_id, source_fingerprint)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND INDEX_NAME = 'idx_msg_knowledge_evidence_source_thread'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_msg_knowledge_evidence_source_thread ON message_knowledge_evidence (store_id, thread_id, source_kind, source_message_start_id, source_message_end_id)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
