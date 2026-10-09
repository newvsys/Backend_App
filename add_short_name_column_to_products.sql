-- Add short_name column to products table
-- Purpose: Store a short/display name for the product (e.g., for cards, menus, labels)
-- Type: VARCHAR(255), not unique, nullable

ALTER TABLE products
ADD COLUMN IF NOT EXISTS short_name VARCHAR(255);

-- Log the migration
-- Migration timestamp: 2026-10-09
-- Change: Added short_name column to products table

