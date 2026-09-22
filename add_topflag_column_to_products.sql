-- Add topflag column to products table
-- Purpose: Mark products as "top featured" for special display/filtering
-- Type: Character field accepting 'Y' or 'N' values
-- Default: 'N' (not a top product)

ALTER TABLE products
ADD COLUMN IF NOT EXISTS top_flag VARCHAR(1) DEFAULT 'N';

-- Add index on top_flag column for efficient filtering of top products
CREATE INDEX IF NOT EXISTS idx_products_top_flag ON products(top_flag);

-- Log the migration
-- Migration timestamp: 2026-09-22
-- Change: Added top_flag column to products table for marking top featured products

