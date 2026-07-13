-- Keep creator-visible internal task queries symmetric with the assignee indexes from V048.
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'internal_tasks'
      AND index_name = 'idx_internal_tasks_creator_active'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_internal_tasks_creator_active ON internal_tasks (store_id, created_by_user_id, archived_at, status, created_at)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'internal_tasks'
      AND index_name = 'idx_internal_tasks_creator_completed'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_internal_tasks_creator_completed ON internal_tasks (store_id, created_by_user_id, archived_at, status, completed_at)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;
