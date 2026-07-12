-- A STAFF message clears the shared-inbox awaiting-reply state only after SENT is committed.
-- SuAutoReplyService was the only known post-V017 producer of high-confidence STAFF/null rows.
UPDATE su_messages
SET delivery_status = 'SENT'
WHERE sender_type = 'STAFF'
  AND (delivery_status IS NULL OR TRIM(delivery_status) = '')
  AND sender_name = '系统自动回复';

CREATE INDEX idx_su_msg_store_thread_sent_id
    ON su_messages (store_id, thread_id, sent_at, id);
