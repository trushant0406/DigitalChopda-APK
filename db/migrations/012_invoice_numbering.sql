-- Invoice numbering configuration on business_profile
ALTER TABLE business_profile ADD COLUMN IF NOT EXISTS invoice_prefix TEXT NOT NULL DEFAULT 'INV-';
ALTER TABLE business_profile ADD COLUMN IF NOT EXISTS invoice_next_number INTEGER NOT NULL DEFAULT 1;
ALTER TABLE business_profile ADD COLUMN IF NOT EXISTS invoice_padding INTEGER NOT NULL DEFAULT 0;

-- Invoice date on orders (for backdating/future billing)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS invoice_date DATE NOT NULL DEFAULT CURRENT_DATE;

