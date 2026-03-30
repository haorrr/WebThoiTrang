ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS payment_status      VARCHAR(30) DEFAULT 'N_A',
  ADD COLUMN IF NOT EXISTS momo_transaction_id VARCHAR(100),
  ADD COLUMN IF NOT EXISTS payment_url         TEXT;
