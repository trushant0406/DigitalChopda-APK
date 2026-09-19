-- Migration 014: Add distinct invoice_number column to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS invoice_number TEXT UNIQUE;

-- Populate invoice_number from any orders that had INV- prefix in order_number
UPDATE orders
SET invoice_number = order_number
WHERE order_number LIKE 'INV-%' AND invoice_number IS NULL;

-- Normalize all order_numbers to Call Intelligence standard ORD-XXXXX
UPDATE orders
SET order_number = 'ORD-' || LPAD(id::text, 5, '0')
WHERE order_number LIKE 'INV-%';

CREATE INDEX IF NOT EXISTS idx_orders_invoice_number ON orders(invoice_number);
