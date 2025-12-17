-- =====================================================================
-- Quick Setup: Create a Shared Test PIN
-- Run this SQL to create a test PIN that all users can use
-- =====================================================================

-- Create a dummy voter record with the shared PIN
-- This allows PIN verification to work (checks if PIN exists)
-- Each device will get its own voter record when they vote
INSERT INTO voters (pin, device_id, has_voted)
SELECT '20267', 'shared-pin-seed-' || EXTRACT(EPOCH FROM NOW())::BIGINT, FALSE
WHERE NOT EXISTS (SELECT 1 FROM voters WHERE pin = '12345' LIMIT 1);

-- Verify the PIN was created
SELECT pin, device_id, has_voted, created_at 
FROM voters 
WHERE pin = '20267';

-- =====================================================================
-- TEST PIN: 12345
-- =====================================================================
-- This PIN can be used by all users/devices
-- Each device will get a unique voter record when they vote
-- =====================================================================

