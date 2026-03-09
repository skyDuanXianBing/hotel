SET @ddl := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'su_messages'
        AND COLUMN_NAME = 'delivery_status'
    ),
    'SELECT 1',
    'ALTER TABLE su_messages ADD COLUMN delivery_status VARCHAR(20) NULL AFTER is_read'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE su_messages
SET delivery_status = 'SENT'
WHERE sender_type = 'STAFF'
  AND (delivery_status IS NULL OR delivery_status = '');
