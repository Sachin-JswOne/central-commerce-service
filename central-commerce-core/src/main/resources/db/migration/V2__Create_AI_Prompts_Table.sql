-- SQL script to create the ai_prompts table in PostgreSQL
-- Run this in the 'central_commerce' database

CREATE TABLE IF NOT EXISTS ai_prompts (
    id SERIAL PRIMARY KEY,
    prompt_type VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,
    encoded_prompt TEXT NOT NULL,
    encoding VARCHAR(20) NOT NULL DEFAULT 'BASE64',
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100),
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ai_prompts_type_version UNIQUE (prompt_type, version),
    CONSTRAINT chk_ai_prompts_version_positive CHECK (version > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_prompts_active_type
    ON ai_prompts (prompt_type)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_ai_prompts_type_version
    ON ai_prompts (prompt_type, version DESC);
