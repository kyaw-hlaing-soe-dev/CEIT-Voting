-- Migration script to remove multi-factor device identification columns
-- Run this SQL against your database to clean up after updating the application

-- Drop hardware_hash column as it's no longer needed
ALTER TABLE voters DROP COLUMN IF EXISTS hardware_hash;

-- Drop screen_info column as it's no longer needed
ALTER TABLE voters DROP COLUMN IF EXISTS screen_info;

-- Drop fingerprint column as it's no longer needed
ALTER TABLE voters DROP COLUMN IF EXISTS fingerprint;

-- Drop indexes related to the removed columns
DROP INDEX IF EXISTS idx_hardware_hash;
DROP INDEX IF EXISTS idx_screen_info;
DROP INDEX IF EXISTS idx_fingerprint;
-- Keep idx_ip_address as it is still used

-- Verify the changes
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'voters'
ORDER BY ordinal_position;
