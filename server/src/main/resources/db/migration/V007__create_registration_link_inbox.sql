CREATE TABLE IF NOT EXISTS registration_link_inbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    booking_key VARCHAR(120) NOT NULL,
    link_url VARCHAR(600) NOT NULL,
    guest_name VARCHAR(200) NULL,
    check_in_date DATE NULL,
    check_out_date DATE NULL,
    room_count INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_registration_link_inbox_store_booking (store_id, booking_key),
    INDEX idx_registration_link_inbox_store_created (store_id, created_at)
);

