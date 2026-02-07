CREATE TABLE IF NOT EXISTS room_blockouts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    block_date DATE NOT NULL,
    block_type VARCHAR(20) NOT NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_room_blockouts_store_room_date (store_id, room_id, block_date),
    INDEX idx_room_blockouts_store_date (store_id, block_date),
    INDEX idx_room_blockouts_room_date (room_id, block_date)
);

CREATE TABLE IF NOT EXISTS su_ari_sync_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    hotel_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source VARCHAR(50) NULL,
    coalesced_count INT NOT NULL DEFAULT 0,
    not_before_at DATETIME NULL,
    next_retry_at DATETIME NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    last_run_at DATETIME NULL,
    last_success_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_su_ari_sync_events_due (status, not_before_at, next_retry_at),
    INDEX idx_su_ari_sync_events_store_hotel (store_id, hotel_id, status)
);

