-- 客人消息语言判定：渠道订单解析 customer.countrycode / customer.guest_lang 落库，
-- 自动消息（AutoMessage）据此给日本客人发日文模板，其他客人发默认（英文）模板。
ALTER TABLE reservations
    ADD COLUMN guest_country VARCHAR(100) NULL,
    ADD COLUMN guest_language VARCHAR(20) NULL;

ALTER TABLE auto_messages
    ADD COLUMN message_ja TEXT NULL;
