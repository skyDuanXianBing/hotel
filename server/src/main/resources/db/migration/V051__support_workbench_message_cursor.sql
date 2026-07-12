-- Support the workbench keyset slice after the awaiting-reply window has selected open threads.
-- Guarded because local databases may already contain an equivalent Hibernate-created index.
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'su_message_threads'
      AND index_name = 'idx_su_msg_thread_workbench_cursor'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_su_msg_thread_workbench_cursor ON su_message_threads (store_id, closed, last_activity, id)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'registration_forms'
      AND index_name = 'idx_registration_home_submitted_cursor');
SET @ddl = IF(@index_exists = 0,
    'CREATE INDEX idx_registration_home_submitted_cursor ON registration_forms (store_id, status, submitted_at, id)',
    'SELECT 1');
PREPARE index_statement FROM @ddl; EXECUTE index_statement; DEALLOCATE PREPARE index_statement;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'registration_forms'
      AND index_name = 'idx_registration_home_approved_cursor');
SET @ddl = IF(@index_exists = 0,
    'CREATE INDEX idx_registration_home_approved_cursor ON registration_forms (store_id, status, approved_at, id)',
    'SELECT 1');
PREPARE index_statement FROM @ddl; EXECUTE index_statement; DEALLOCATE PREPARE index_statement;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'internal_tasks'
      AND index_name = 'idx_internal_home_active_cursor');
SET @ddl = IF(@index_exists = 0,
    'CREATE INDEX idx_internal_home_active_cursor ON internal_tasks (store_id, archived_at, status, updated_at, id)',
    'SELECT 1');
PREPARE index_statement FROM @ddl; EXECUTE index_statement; DEALLOCATE PREPARE index_statement;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'cleaning_tasks'
      AND index_name = 'idx_cleaning_home_cursor');
SET @ddl = IF(@index_exists = 0,
    'CREATE INDEX idx_cleaning_home_cursor ON cleaning_tasks (task_date, status, estimated_time, id, room_id, cleaner_id)',
    'SELECT 1');
PREPARE index_statement FROM @ddl; EXECUTE index_statement; DEALLOCATE PREPARE index_statement;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'reservations'
      AND index_name = 'idx_reservation_home_cursor');
SET @ddl = IF(@index_exists = 0,
    'CREATE INDEX idx_reservation_home_cursor ON reservations (store_id, check_out_date, check_in_date, id, status, room_id)',
    'SELECT 1');
PREPARE index_statement FROM @ddl; EXECUTE index_statement; DEALLOCATE PREPARE index_statement;
