-- Migration: backfill cookie_id and enforce NOT NULL/UNIQUE for cookie+IP tracking
-- Run this before deploying the cookie/IP-only version.

BEGIN;

-- Ensure UUID generator is available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Backfill missing or blank cookie_id with a generated UUID
UPDATE voters
   SET cookie_id = 'cookie-' || uuid_generate_v4()
 WHERE (cookie_id IS NULL OR trim(cookie_id) = '');

-- Normalize ip_address length
ALTER TABLE voters ALTER COLUMN ip_address TYPE VARCHAR(45);

-- Enforce NOT NULL and uniqueness on cookie_id (idempotent)
ALTER TABLE voters
    ALTER COLUMN cookie_id SET NOT NULL;

-- Recreate unique constraint/index if needed
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uk_cookie_id' AND table_name = 'voters'
    ) THEN
        ALTER TABLE voters ADD CONSTRAINT uk_cookie_id UNIQUE (cookie_id);
    END IF;
END $$;

-- Helpful indexes (safe if they already exist)
CREATE INDEX IF NOT EXISTS idx_cookie_id ON voters(cookie_id);
CREATE INDEX IF NOT EXISTS idx_ip_address ON voters(ip_address);
CREATE INDEX IF NOT EXISTS idx_has_voted ON voters(has_voted);

COMMIT;

