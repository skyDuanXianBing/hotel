-- Managed operation settlement: multi-property configurations + persisted monthly data
ALTER TABLE managed_operation_settings DROP INDEX uk_managed_operation_settings_store;

ALTER TABLE managed_operation_settings
    ADD COLUMN invoice_issue_day TINYINT NOT NULL DEFAULT 9 AFTER registration_fee_net,
    ADD COLUMN receipt_issue_day TINYINT NOT NULL DEFAULT 10 AFTER invoice_issue_day;

ALTER TABLE managed_operation_settings
    ADD UNIQUE KEY uk_managed_operation_settings_store_name (store_id, property_name);

CREATE TABLE managed_operation_monthly_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    settings_id BIGINT NOT NULL,
    settlement_month CHAR(7) NOT NULL,
    invoice_number VARCHAR(100) NOT NULL DEFAULT '',
    invoice_date DATE NULL,
    payment_due_date DATE NULL,
    receipt_number VARCHAR(100) NOT NULL DEFAULT '',
    receipt_date DATE NULL,
    note VARCHAR(1000) NOT NULL DEFAULT '',
    airbnb_file_key VARCHAR(500) NULL,
    airbnb_file_name VARCHAR(255) NOT NULL DEFAULT '',
    booking_file_key VARCHAR(500) NULL,
    booking_file_name VARCHAR(255) NOT NULL DEFAULT '',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_managed_operation_monthly (store_id, settings_id, settlement_month),
    KEY idx_managed_operation_monthly_settings (settings_id),
    CONSTRAINT fk_mo_monthly_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_mo_monthly_settings FOREIGN KEY (settings_id) REFERENCES managed_operation_settings(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE managed_operation_monthly_fees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    monthly_data_id BIGINT NOT NULL,
    fee_type VARCHAR(10) NOT NULL DEFAULT 'DEDUCTION',
    description VARCHAR(200) NOT NULL DEFAULT '',
    amount_gross DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_mo_fees_monthly (monthly_data_id),
    CONSTRAINT fk_mo_fees_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_mo_fees_monthly FOREIGN KEY (monthly_data_id) REFERENCES managed_operation_monthly_data(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
