-- Cover the window-function awaiting-reply query and its batched DTO hydration.
-- Each statement is guarded because local environments may have received the same index from
-- Hibernate ddl-auto before Flyway history is repaired.

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'su_messages'
      AND index_name = 'idx_su_msg_awaiting_reply_cover'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_su_msg_awaiting_reply_cover ON su_messages (store_id, thread_id, sent_at DESC, id DESC, sender_type, delivery_status)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'su_message_threads'
      AND index_name = 'idx_su_msg_thread_open'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_su_msg_thread_open ON su_message_threads (store_id, closed, id)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'su_messages'
      AND index_name = 'idx_su_msg_unread_by_thread'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_su_msg_unread_by_thread ON su_messages (store_id, thread_id, sender_type, is_read)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;
