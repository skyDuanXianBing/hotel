ALTER TABLE stores
    ADD COLUMN desktop_photo_urls_json LONGTEXT NULL,
    ADD COLUMN mobile_photo_urls_json LONGTEXT NULL,
    ADD COLUMN localized_content_json LONGTEXT NULL;

ALTER TABLE room_types
    ADD COLUMN desktop_photo_urls_json LONGTEXT NULL,
    ADD COLUMN mobile_photo_urls_json LONGTEXT NULL,
    ADD COLUMN localized_content_json LONGTEXT NULL;
