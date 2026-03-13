ALTER TABLE room_types
    ADD COLUMN max_child_occupancy INT NOT NULL DEFAULT 0,
    ADD COLUMN su_room_type VARCHAR(100) NULL,
    ADD COLUMN size_measurement DECIMAL(10,2) NULL,
    ADD COLUMN size_measurement_unit VARCHAR(10) NULL;
