-- AI message knowledge thread rebuild cleanup.
-- Manual execution only:
-- 1. Take a verified database backup first.
-- 2. Execute only after the thread-level extractor schema is deployed and the team accepts dropping legacy data.
-- 3. Do not execute from application startup, tests, or CI.
--
-- This script intentionally does not touch:
-- - message_knowledge_items
-- - thread knowledge_* columns on su_message_threads
-- - thread evidence fields on message_knowledge_evidence:
--   source_kind, source_message_ids_json, source_message_start_id,
--   source_message_end_id, extractor_version, source_fingerprint
-- - fact hash / embedding fields used by the new retrieval path

-- Drop the old store-cursor scanner table. The new scheduler uses su_message_threads knowledge_* state.
DROP TABLE IF EXISTS message_knowledge_scan_states;

-- Drop the old pair-level legacy knowledge table. Runtime Java entity/repository have been removed.
DROP TABLE IF EXISTS message_knowledge_entries;

-- Drop obsolete evidence indexes that depend on legacy guest/staff pair ids.
SET @table_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
);

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND INDEX_NAME = 'uk_msg_knowledge_evidence_source'
);
SET @ddl := IF(
    @table_exists > 0 AND @idx_exists > 0,
    'ALTER TABLE message_knowledge_evidence DROP INDEX uk_msg_knowledge_evidence_source',
    'DO 0'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND INDEX_NAME = 'idx_msg_knowledge_evidence_store_thread'
);
SET @ddl := IF(
    @table_exists > 0 AND @idx_exists > 0,
    'ALTER TABLE message_knowledge_evidence DROP INDEX idx_msg_knowledge_evidence_store_thread',
    'DO 0'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop legacy pair id columns after dependent indexes are gone.
SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'source_guest_message_id'
);
SET @ddl := IF(
    @table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE message_knowledge_evidence DROP COLUMN source_guest_message_id',
    'DO 0'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message_knowledge_evidence'
      AND COLUMN_NAME = 'source_staff_message_id'
);
SET @ddl := IF(
    @table_exists > 0 AND @column_exists > 0,
    'ALTER TABLE message_knowledge_evidence DROP COLUMN source_staff_message_id',
    'DO 0'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Optional legacy cursor index cleanup:
-- Leave this commented for now. The index is on su_messages, not on the removed legacy tables,
-- and it can still be useful for message diagnostics or other store-scoped read paths.
-- Only drop after a separate query-plan review confirms no remaining production reads benefit from it.
--
-- SET @idx_exists := (
--     SELECT COUNT(1)
--     FROM INFORMATION_SCHEMA.STATISTICS
--     WHERE TABLE_SCHEMA = DATABASE()
--       AND TABLE_NAME = 'su_messages'
--       AND INDEX_NAME = 'idx_su_msg_store_id_cursor'
-- );
-- SET @ddl := IF(
--     @idx_exists > 0,
--     'ALTER TABLE su_messages DROP INDEX idx_su_msg_store_id_cursor',
--     'DO 0'
-- );
-- PREPARE stmt FROM @ddl;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt;

-- Post-checks for the operator.
SELECT 'message_knowledge_scan_states' AS object_name, COUNT(1) AS remaining
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'message_knowledge_scan_states'
UNION ALL
SELECT 'message_knowledge_entries' AS object_name, COUNT(1) AS remaining
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'message_knowledge_entries'
UNION ALL
SELECT 'message_knowledge_evidence.source_guest_message_id' AS object_name, COUNT(1) AS remaining
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'message_knowledge_evidence'
  AND COLUMN_NAME = 'source_guest_message_id'
UNION ALL
SELECT 'message_knowledge_evidence.source_staff_message_id' AS object_name, COUNT(1) AS remaining
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'message_knowledge_evidence'
  AND COLUMN_NAME = 'source_staff_message_id';
