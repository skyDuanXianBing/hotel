CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scope VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    store_id BIGINT NULL,
    locale VARCHAR(20) NULL,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    starts_at DATETIME NULL,
    ends_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_announcements_home (
        active,
        locale,
        starts_at,
        ends_at,
        sort_order
    ),
    INDEX idx_announcements_store_scope (
        scope,
        store_id,
        active,
        sort_order
    ),
    CONSTRAINT fk_announcements_store FOREIGN KEY (store_id) REFERENCES stores(id)
);
