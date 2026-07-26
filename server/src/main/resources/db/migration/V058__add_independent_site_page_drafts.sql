ALTER TABLE independent_sites
    ADD COLUMN draft_page_schema_json LONGTEXT NULL AFTER page_schema_json,
    ADD COLUMN draft_updated_at DATETIME NULL AFTER draft_page_schema_json,
    ADD COLUMN draft_version BIGINT NOT NULL DEFAULT 0 AFTER draft_updated_at;
