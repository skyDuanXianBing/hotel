CREATE TABLE IF NOT EXISTS message_knowledge_topics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    topic_code VARCHAR(120) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    aliases_json TEXT NULL,
    example_questions_json TEXT NULL,
    example_answers_json TEXT NULL,
    scope_preference VARCHAR(30) NOT NULL DEFAULT 'STORE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(30) NOT NULL DEFAULT 'DEFAULT',
    confidence DECIMAL(5,4) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_knowledge_topic_store_code (
        store_id,
        topic_code
    ),
    INDEX idx_msg_knowledge_topic_store_status (
        store_id,
        status,
        topic_code
    )
);
