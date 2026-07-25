CREATE TABLE IF NOT EXISTS independent_site_stripe_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    publishable_key VARCHAR(255) NULL,
    secret_key_encrypted VARCHAR(1024) NULL,
    webhook_secret_encrypted VARCHAR(1024) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_independent_site_stripe_settings_store UNIQUE (store_id),
    CONSTRAINT fk_independent_site_stripe_settings_store
        FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
