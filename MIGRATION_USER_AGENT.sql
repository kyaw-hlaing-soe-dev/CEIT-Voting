-- =====================================================================
-- Migration: Add User-Agent and IP Address tracking to voters table
-- Run this script if you already have an existing database with voters table
-- =====================================================================

-- Add user_agent column for storing device User-Agent string
ALTER TABLE voters
ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);

-- Add ip_address column for storing client IP address
ALTER TABLE voters
ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);

-- Comment explaining the purpose
COMMENT ON COLUMN voters.user_agent IS 'Browser User-Agent string for device fingerprinting';
COMMENT ON COLUMN voters.ip_address IS 'Client IP address for auditing and tracking';

-- Note: The device_id is now generated using SHA-256 hash of IP + User-Agent
-- This provides better device fingerprinting than IP-only approach

