-- Add priority column to products table
-- Purpose: Store product display priority for sorting (e.g., featured products)
-- Type: Integer (number), not unique, nullable

ALTER TABLE products
ADD COLUMN IF NOT EXISTS priority INTEGER;

-- Optional: Add index on priority column for efficient sorting
CREATE INDEX IF NOT EXISTS idx_products_priority ON products(priority);

-- Log the migration
-- Migration timestamp: 2026-09-22
-- Change: Added priority column to products table for product ordering

