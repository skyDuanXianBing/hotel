ALTER TABLE stores
    ADD COLUMN facilities_json LONGTEXT NULL;

ALTER TABLE room_types
    ADD COLUMN facilities_json LONGTEXT NULL;
