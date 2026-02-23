-- Add optional check-in guide link to room_types.
-- Use dynamic SQL for MySQL compatibility in environments without IF NOT EXISTS on ADD COLUMN.

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'room_types'
              AND column_name = 'check_in_guide_link'
        ),
        'SELECT 1',
        'ALTER TABLE room_types ADD COLUMN check_in_guide_link VARCHAR(500) NULL'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

