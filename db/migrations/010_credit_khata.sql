-- Credit limit per customer, for the khata (credit book) system.
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS credit_limit NUMERIC NOT NULL DEFAULT 0;

-- Orders can now be placed "on credit" — treated like pending until collected
-- via the payments ledger below, rather than marked paid in one shot.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_method_check;
ALTER TABLE orders ADD CONSTRAINT orders_payment_method_check
  CHECK (payment_method IS NULL OR payment_method IN ('cash', 'upi', 'card', 'bank_transfer', 'credit', 'other'));

-- Ledger of amounts collected against an order over time — supports partial
-- khata collections instead of the binary pending/paid an order alone gives.
CREATE TABLE IF NOT EXISTS payments (
  id       SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  amount   NUMERIC NOT NULL CHECK (amount > 0),
  method   TEXT NOT NULL DEFAULT 'cash' CHECK (method IN ('cash', 'upi', 'card', 'bank_transfer', 'other')),
  notes    TEXT,
  paid_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
