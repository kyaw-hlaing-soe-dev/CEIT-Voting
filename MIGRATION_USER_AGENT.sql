-- =====================================================================
-- Migration cleanup: remove User-Agent fingerprinting (deprecated)
-- This script keeps only IP address tracking for auditing.
-- =====================================================================

-- Remove deprecated user_agent column if it exists
ALTER TABLE voters
DROP COLUMN IF EXISTS user_agent;

-- Ensure ip_address column exists for IP-based checks
ALTER TABLE voters
ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);

COMMENT ON COLUMN voters.ip_address IS 'Client IP address for auditing and one-vote checks';
