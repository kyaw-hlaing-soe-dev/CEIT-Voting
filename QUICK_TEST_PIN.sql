-- =====================================================================
-- QUICK SETUP: Create Test PIN for Testing
-- Copy and paste this into your PostgreSQL database
-- =====================================================================

-- Create a test PIN: 12345
-- This creates a dummy voter record so PIN verification works
-- Each device will get its own voter record when they vote
INSERT INTO voters (pin, device_id, has_voted)
SELECT '20267', 'seed-' || EXTRACT(EPOCH FROM NOW())::BIGINT, FALSE
WHERE NOT EXISTS (SELECT 1 FROM voters WHERE pin = '12345' LIMIT 1);

-- Verify it was created
SELECT 'PIN 12345 is ready!' AS status, pin, device_id, has_voted 
FROM voters 
WHERE pin = '20267';

