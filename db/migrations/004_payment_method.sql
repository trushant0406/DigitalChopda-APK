ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method TEXT
  CHECK (payment_method IS NULL OR payment_method IN ('cash', 'upi', 'card', 'bank_transfer', 'other'));
