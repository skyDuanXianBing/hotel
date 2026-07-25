CREATE TABLE IF NOT EXISTS independent_sites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    slug VARCHAR(63) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    channel_id BIGINT NOT NULL,
    page_schema_json LONGTEXT NOT NULL,
    payment_provider VARCHAR(30) NOT NULL DEFAULT 'SIMULATED',
    simulated_payment_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_independent_sites_store UNIQUE (store_id),
    CONSTRAINT uk_independent_sites_slug UNIQUE (slug),
    CONSTRAINT fk_independent_sites_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_independent_sites_channel
        FOREIGN KEY (channel_id) REFERENCES channels(id),
    INDEX idx_independent_sites_enabled_slug (enabled, slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS independent_site_publications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_independent_site_publication_target
        UNIQUE (site_id, target_type, target_id),
    CONSTRAINT fk_independent_site_publications_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_independent_site_publications_site
        FOREIGN KEY (site_id) REFERENCES independent_sites(id) ON DELETE CASCADE,
    INDEX idx_independent_site_publications_store_site (store_id, site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    public_reference VARCHAR(36) NOT NULL,
    group_order_no VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    quote_snapshot_json LONGTEXT NOT NULL,
    provider_transaction_id VARCHAR(100) NULL,
    failure_reason VARCHAR(500) NULL,
    expires_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_attempt_store_idempotency
        UNIQUE (store_id, idempotency_key),
    CONSTRAINT uk_payment_attempt_public_reference
        UNIQUE (public_reference),
    CONSTRAINT fk_payment_attempt_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_payment_attempt_site
        FOREIGN KEY (site_id) REFERENCES independent_sites(id),
    INDEX idx_payment_attempt_pending_expiry (status, expires_at),
    INDEX idx_payment_attempt_store_group (store_id, group_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'guest_email'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE reservations ADD COLUMN guest_email VARCHAR(254) NULL',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND column_name = 'independent_site_id'
);
SET @ddl = IF(
    @column_exists = 0,
    'ALTER TABLE reservations ADD COLUMN independent_site_id BIGINT NULL',
    'SELECT 1'
);
PREPARE column_statement FROM @ddl;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'reservations'
      AND index_name = 'idx_reservations_independent_site'
);
SET @ddl = IF(
    @index_exists = 0,
    'CREATE INDEX idx_reservations_independent_site ON reservations (store_id, independent_site_id, group_order_no)',
    'SELECT 1'
);
PREPARE index_statement FROM @ddl;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;
