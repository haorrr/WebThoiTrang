-- Fix: shipping_address was incorrectly typed as JSONB, change to TEXT
-- to match the Order entity mapping (plain string address)
ALTER TABLE orders
    ALTER COLUMN shipping_address TYPE TEXT USING shipping_address::TEXT;
