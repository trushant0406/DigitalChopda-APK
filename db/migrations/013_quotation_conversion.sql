-- Phase 2: Quotation to Sales Invoice Conversion Linkage

-- 1. Expand quotation status check constraint to include 'converted'
ALTER TABLE quotations DROP CONSTRAINT IF EXISTS quotations_status_check;
ALTER TABLE quotations ADD CONSTRAINT quotations_status_check CHECK (status IN ('draft', 'sent', 'converted'));

-- 2. Link quotations to converted orders
ALTER TABLE quotations ADD COLUMN IF NOT EXISTS converted_order_id INTEGER REFERENCES orders(id) ON DELETE SET NULL;
ALTER TABLE quotations ADD COLUMN IF NOT EXISTS converted_at TIMESTAMPTZ;

-- 3. Link orders back to quotations and optional order reference
ALTER TABLE orders ADD COLUMN IF NOT EXISTS quotation_id INTEGER REFERENCES quotations(id) ON DELETE SET NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_ref TEXT;
CREATE INDEX IF NOT EXISTS idx_orders_quotation_id ON orders(quotation_id);
CREATE INDEX IF NOT EXISTS idx_quotations_converted_order_id ON quotations(converted_order_id);

