-- Migration: Add unique constraint to tracking_number column in shipping table
-- Purpose: Ensure tracking numbers are unique across all shipments (including cancelled ones)
-- Date: 2026-09-10

-- Add unique constraint to tracking_number column
-- This constraint considers all shipments, including cancelled ones, to ensure no duplicate tracking numbers exist
ALTER TABLE shipping ADD CONSTRAINT uk_shipping_tracking_number UNIQUE (tracking_number);

-- Note: If the table already has duplicate tracking numbers, you'll need to clean them up first
-- To find duplicates, run:
-- SELECT tracking_number, COUNT(*) FROM shipping WHERE tracking_number IS NOT NULL GROUP BY tracking_number HAVING COUNT(*) > 1;

