CREATE TABLE IF NOT EXISTS message_knowledge_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    scope_id BIGINT NULL,
    thread_id BIGINT NOT NULL,
    reservation_id BIGINT NULL,
    room_id BIGINT NULL,
    room_number VARCHAR(80) NULL,
    room_type_id BIGINT NULL,
    room_type_name VARCHAR(120) NULL,
    channel_id INT NULL,
    booking_key VARCHAR(255) NULL,
    source_guest_message_id BIGINT NOT NULL,
    source_staff_message_id BIGINT NOT NULL,
    source_timestamp DATETIME NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    normalized_text TEXT NOT NULL,
    normalized_hash CHAR(64) NOT NULL,
    language VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pii_redaction_status VARCHAR(20) NOT NULL DEFAULT 'REDACTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    indexed_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_kb_store_source_hash (
        store_id,
        source_guest_message_id,
        source_staff_message_id,
        normalized_hash
    ),
    INDEX idx_msg_kb_store_scope_status_time (
        store_id,
        scope_type,
        scope_id,
        status,
        source_timestamp
    ),
    INDEX idx_msg_kb_store_thread_source (
        store_id,
        thread_id,
        source_guest_message_id,
        source_staff_message_id
    ),
    INDEX idx_msg_kb_store_room_time (
        store_id,
        room_id,
        source_timestamp
    ),
    INDEX idx_msg_kb_store_room_type_time (
        store_id,
        room_type_id,
        source_timestamp
    ),
    INDEX idx_msg_kb_store_booking_time (
        store_id,
        channel_id,
        booking_key,
        source_timestamp
    ),
    INDEX idx_msg_kb_store_status_time (
        store_id,
        status,
        source_timestamp
    )
);
