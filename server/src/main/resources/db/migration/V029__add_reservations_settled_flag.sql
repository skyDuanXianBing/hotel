ALTER TABLE reservations
    ADD COLUMN settled TINYINT(1) NOT NULL DEFAULT 0 AFTER paid_amount;

UPDATE reservations
SET settled = 1
WHERE (su_reservation_id IS NOT NULL AND TRIM(su_reservation_id) <> '')
   OR status IN ('CHECKED_IN', 'CHECKED_OUT')
   OR (COALESCE(total_amount, 0) > 0 AND COALESCE(paid_amount, 0) >= COALESCE(total_amount, 0));
