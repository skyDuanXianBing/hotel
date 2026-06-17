CREATE TABLE IF NOT EXISTS message_knowledge_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    scope_id BIGINT NULL,
    scope_key VARCHAR(80) NOT NULL,
    thread_id BIGINT NULL,
    room_id BIGINT NULL,
    room_number VARCHAR(80) NULL,
    room_type_id BIGINT NULL,
    room_type_name VARCHAR(120) NULL,
    channel_id INT NULL,
    topic VARCHAR(120) NOT NULL,
    topic_hash CHAR(64) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    normalized_question TEXT NOT NULL,
    normalized_answer TEXT NOT NULL,
    normalized_answer_hash CHAR(64) NOT NULL,
    language VARCHAR(20) NULL,
    confidence DECIMAL(5,4) NOT NULL DEFAULT 0.7000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pii_redaction_status VARCHAR(20) NOT NULL DEFAULT 'REDACTED',
    evidence_count INT NOT NULL DEFAULT 0,
    first_seen_at DATETIME NULL,
    last_seen_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    refined_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_knowledge_item_scope_topic (
        store_id,
        scope_key,
        topic_hash
    ),
    INDEX idx_msg_knowledge_item_store_status (
        store_id,
        status,
        updated_at
    ),
    INDEX idx_msg_knowledge_item_store_scope (
        store_id,
        scope_type,
        scope_id,
        status
    ),
    INDEX idx_msg_knowledge_item_room (
        store_id,
        room_id,
        status
    ),
    INDEX idx_msg_knowledge_item_room_type (
        store_id,
        room_type_id,
        status
    )
);

CREATE TABLE IF NOT EXISTS message_knowledge_evidence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    scope_id BIGINT NULL,
    scope_key VARCHAR(80) NOT NULL,
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
    confidence DECIMAL(5,4) NOT NULL DEFAULT 0.7000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    pii_redaction_status VARCHAR(20) NOT NULL DEFAULT 'REDACTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_knowledge_evidence_source (
        store_id,
        source_guest_message_id,
        source_staff_message_id
    ),
    INDEX idx_msg_knowledge_evidence_item (
        item_id,
        status,
        source_timestamp
    ),
    INDEX idx_msg_knowledge_evidence_store_thread (
        store_id,
        thread_id,
        source_staff_message_id
    ),
    INDEX idx_msg_knowledge_evidence_store_hash (
        store_id,
        normalized_hash
    )
);

CREATE TABLE IF NOT EXISTS message_knowledge_scan_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    last_scanned_message_id BIGINT NOT NULL DEFAULT 0,
    lease_owner VARCHAR(120) NULL,
    lease_until DATETIME NULL,
    next_scan_after DATETIME NULL,
    backoff_until DATETIME NULL,
    failure_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    last_started_at DATETIME NULL,
    last_finished_at DATETIME NULL,
    processed_message_count BIGINT NOT NULL DEFAULT 0,
    extracted_pair_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_knowledge_scan_state_store (store_id),
    INDEX idx_msg_knowledge_scan_due (
        next_scan_after,
        backoff_until,
        lease_until
    ),
    INDEX idx_msg_knowledge_scan_status (
        status,
        lease_until
    )
);

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'su_messages'
      AND INDEX_NAME = 'idx_su_msg_store_id_cursor'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_su_msg_store_id_cursor ON su_messages (store_id, id)',
    'DO 0'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
