-- Make migration idempotent (some environments may already have these columns)
-- MySQL does not universally support "ADD COLUMN IF NOT EXISTS", so we use dynamic SQL.

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'su_ari_sync_events'
              AND column_name = 'date_ranges'
        ),
        'SELECT 1',
        'ALTER TABLE su_ari_sync_events ADD COLUMN date_ranges TEXT NULL'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'su_ari_sync_events'
              AND column_name = 'room_type_ids'
        ),
        'SELECT 1',
        'ALTER TABLE su_ari_sync_events ADD COLUMN room_type_ids TEXT NULL'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'su_ari_sync_events'
              AND column_name = 'rate_plan_ids'
        ),
        'SELECT 1',
        'ALTER TABLE su_ari_sync_events ADD COLUMN rate_plan_ids TEXT NULL'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'su_ari_sync_events'
              AND column_name = 'push_availability'
        ),
        'SELECT 1',
        'ALTER TABLE su_ari_sync_events ADD COLUMN push_availability TINYINT(1) NOT NULL DEFAULT 1'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'su_ari_sync_events'
              AND column_name = 'push_rates'
        ),
        'SELECT 1',
        'ALTER TABLE su_ari_sync_events ADD COLUMN push_rates TINYINT(1) NOT NULL DEFAULT 1'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'su_ari_sync_events'
              AND column_name = 'push_restrictions'
        ),
        'SELECT 1',
        'ALTER TABLE su_ari_sync_events ADD COLUMN push_restrictions TINYINT(1) NOT NULL DEFAULT 1'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @stmt := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'su_ari_sync_events'
              AND column_name = 'derive_closed_from_blockouts'
        ),
        'SELECT 1',
        'ALTER TABLE su_ari_sync_events ADD COLUMN derive_closed_from_blockouts TINYINT(1) NOT NULL DEFAULT 0'
    )
);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
