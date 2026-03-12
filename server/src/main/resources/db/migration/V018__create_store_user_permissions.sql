CREATE TABLE IF NOT EXISTS store_user_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_user_id BIGINT NOT NULL,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    room_type_id BIGINT NOT NULL DEFAULT 0,
    all_room_types BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_user_permissions_store_user
        FOREIGN KEY (store_user_id) REFERENCES store_users(id)
        ON DELETE CASCADE,
    UNIQUE KEY uk_store_user_permissions (store_user_id, module, action, room_type_id),
    INDEX idx_store_user_permissions_store_user_id (store_user_id)
);

