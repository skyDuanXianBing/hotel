-- Add default_price_plan_id to channels table
-- This field binds each channel to a default price plan for OTA price sync

ALTER TABLE channels
ADD COLUMN default_price_plan_id BIGINT AFTER auto_sync_price;

ALTER TABLE channels
ADD CONSTRAINT fk_channel_default_price_plan
    FOREIGN KEY (default_price_plan_id)
    REFERENCES price_plans(id)
    ON DELETE SET NULL;

CREATE INDEX idx_channel_default_price_plan
ON channels(default_price_plan_id);

-- Add default_price_plan_id to ota_integrations table
-- This field binds each OTA integration to a default price plan

ALTER TABLE ota_integrations
ADD COLUMN default_price_plan_id BIGINT AFTER auto_sync_price;

ALTER TABLE ota_integrations
ADD CONSTRAINT fk_ota_integration_default_price_plan
    FOREIGN KEY (default_price_plan_id)
    REFERENCES price_plans(id)
    ON DELETE SET NULL;

CREATE INDEX idx_ota_integration_default_price_plan
ON ota_integrations(default_price_plan_id);
