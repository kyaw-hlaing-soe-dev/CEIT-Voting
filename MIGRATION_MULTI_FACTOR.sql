-- Migration script to add multi-factor device identification columns
-- Run this SQL against your database before deploying the updated application

-- Add hardware_hash column for cross-browser device identification
ALTER TABLE voters ADD COLUMN IF NOT EXISTS hardware_hash VARCHAR(128);

-- Add screen_info column for screen resolution matching
ALTER TABLE voters ADD COLUMN IF NOT EXISTS screen_info VARCHAR(100);

-- Add indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_hardware_hash ON voters(hardware_hash);
CREATE INDEX IF NOT EXISTS idx_screen_info ON voters(screen_info);
CREATE INDEX IF NOT EXISTS idx_fingerprint ON voters(fingerprint);
CREATE INDEX IF NOT EXISTS idx_ip_address ON voters(ip_address);

-- Verify the changes
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'voters'
ORDER BY ordinal_position;

