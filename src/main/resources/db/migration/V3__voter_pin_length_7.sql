-- Flyway migration: revert voter_pins.pin to varchar(7)
-- Use with caution: ensure data fits 7 chars before running.

ALTER TABLE IF EXISTS voter_pins ALTER COLUMN pin TYPE varchar(7);

-- Optionally trim longer pins:
-- UPDATE voter_pins SET pin = RIGHT(pin, 7) WHERE CHAR_LENGTH(pin) > 7;

