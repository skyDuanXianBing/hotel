CREATE TABLE IF NOT EXISTS reservation_daily_prices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    su_hotel_id VARCHAR(50) NULL,
    su_reservation_id VARCHAR(100) NULL,
    room_reservation_id VARCHAR(100) NULL,
    price_date DATE NOT NULL,
    currency_code VARCHAR(10) NULL,
    rate_id VARCHAR(100) NULL,
    mealplan_id VARCHAR(100) NULL,
    mealplan VARCHAR(255) NULL,
    tax_amount DECIMAL(12, 2) NULL,
    price_before_tax DECIMAL(12, 2) NULL,
    price_after_tax DECIMAL(12, 2) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_daily_prices_store_res_date (store_id, reservation_id, price_date),
    KEY idx_reservation_daily_prices_store_date (store_id, price_date),
    KEY idx_reservation_daily_prices_reservation (reservation_id),
    KEY idx_reservation_daily_prices_su_res_date (store_id, su_reservation_id, room_reservation_id, price_date),
    CONSTRAINT fk_reservation_daily_prices_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id)
        ON DELETE CASCADE
);
