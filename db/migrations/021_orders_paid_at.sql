-- Migration 021: Add paid_at timestamp to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

-- Backfill paid_at for already paid orders
UPDATE orders o
SET paid_at = COALESCE(
  (SELECT MAX(p.paid_at) FROM payments p WHERE p.order_id = o.id),
  o.updated_at,
  o.created_at
)
WHERE o.payment_status = 'paid' AND o.paid_at IS NULL;
