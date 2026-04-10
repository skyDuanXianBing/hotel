ALTER TABLE room_types
    ADD COLUMN nearby_station VARCHAR(255) NULL AFTER room_type_address;
