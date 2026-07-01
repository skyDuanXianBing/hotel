ALTER TABLE channel_mapping_price_settings
    ADD COLUMN last_su_action VARCHAR(100) NULL,
    ADD COLUMN last_su_http_status INT NULL,
    ADD COLUMN last_su_response_status VARCHAR(100) NULL,
    ADD COLUMN last_su_response_message TEXT NULL,
    ADD COLUMN last_su_response_errors TEXT NULL,
    ADD COLUMN last_su_payload_summary TEXT NULL,
    ADD COLUMN last_su_response_summary TEXT NULL;
