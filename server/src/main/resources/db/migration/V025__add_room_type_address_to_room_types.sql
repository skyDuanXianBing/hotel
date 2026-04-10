ALTER TABLE room_types
    ADD COLUMN room_type_address VARCHAR(500) NULL AFTER check_in_guide_link;
