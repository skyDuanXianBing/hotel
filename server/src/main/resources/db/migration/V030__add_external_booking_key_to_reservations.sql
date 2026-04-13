ALTER TABLE reservations
    ADD COLUMN external_booking_key VARCHAR(120) NULL AFTER channel_order_number;

CREATE INDEX idx_reservations_store_channel_external_booking_key
    ON reservations (store_id, channel_id, external_booking_key);

UPDATE reservations
SET external_booking_key = TRIM(channel_order_number)
WHERE external_booking_key IS NULL
  AND channel_order_number IS NOT NULL
  AND TRIM(channel_order_number) <> '';

UPDATE reservations
SET external_booking_key = TRIM(SUBSTRING_INDEX(su_reservation_id, '_', 1))
WHERE external_booking_key IS NULL
  AND su_reservation_id IS NOT NULL
  AND TRIM(su_reservation_id) <> ''
  AND LOCATE('_', su_reservation_id) > 0;

UPDATE reservations
SET external_booking_key = TRIM(su_reservation_id)
WHERE external_booking_key IS NULL
  AND su_reservation_id IS NOT NULL
  AND TRIM(su_reservation_id) <> '';

UPDATE reservations
SET external_booking_key = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(order_number, '_', 1), '-', -1))
WHERE external_booking_key IS NULL
  AND order_number IS NOT NULL
  AND TRIM(order_number) <> ''
  AND order_number REGEXP '^SU[0-9]+-.+_.+$';

UPDATE reservations
SET external_booking_key = TRIM(order_number)
WHERE external_booking_key IS NULL
  AND order_number IS NOT NULL
  AND TRIM(order_number) <> '';
