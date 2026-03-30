-- SQL script to create the short_links table in PostgreSQL
-- Run this in the 'central_commerce' database

CREATE TABLE IF NOT EXISTS short_links (
    id SERIAL PRIMARY KEY,
    prefix VARCHAR(10) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    business_id VARCHAR(100),
    channel VARCHAR(50),
    target_template TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP
);

-- Index for faster lookups by prefix and code
CREATE INDEX IF NOT EXISTS idx_short_links_lookup ON short_links (prefix, code);

-- Index for idempotency checks
CREATE INDEX IF NOT EXISTS idx_short_links_idempotency ON short_links (type, business_id, channel);
