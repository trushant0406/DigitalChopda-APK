-- Migration 018: Allow 'partially_paid' in orders_payment_status_check

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;

ALTER TABLE orders ADD CONSTRAINT orders_payment_status_check
  CHECK (payment_status IN ('pending', 'paid', 'credit', 'partially_paid'));
