-- Migration 015: Payment-In System, Receipt Numbers, and Party Ledger Enhancements

-- 1. Make order_id nullable on payments so on-account/lump-sum customer payments are supported
ALTER TABLE payments ALTER COLUMN order_id DROP NOT NULL;

-- 2. Add contact_id to link payments directly to a customer/party
ALTER TABLE payments ADD COLUMN IF NOT EXISTS contact_id INTEGER REFERENCES contacts(id) ON DELETE CASCADE;

-- 3. Add receipt_number for sequential voucher tracking (REC-0001, REC-0002, etc.)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_number TEXT UNIQUE;

-- 4. Add payment_mode (cash, upi, bank_transfer, cheque, other)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_mode TEXT NOT NULL DEFAULT 'cash';

-- 5. Add reference_no (UTR, Cheque number, IMPS/NEFT ref)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS reference_no TEXT;

-- 6. Add receipt_url and whatsapp_sent tracking
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_url TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS whatsapp_sent BOOLEAN NOT NULL DEFAULT false;

-- 7. Backfill contact_id for existing payments from orders table
UPDATE payments p
SET contact_id = o.contact_id
FROM orders o
WHERE p.order_id = o.id AND p.contact_id IS NULL;

-- 8. Backfill receipt_number for existing payments if missing
DO $$
DECLARE
  r RECORD;
  seq INT := 1;
BEGIN
  FOR r IN SELECT id FROM payments WHERE receipt_number IS NULL ORDER BY paid_at ASC, id ASC LOOP
    UPDATE payments
    SET receipt_number = 'REC-' || LPAD(seq::TEXT, 4, '0')
    WHERE id = r.id;
    seq := seq + 1;
  END LOOP;
END $$;

-- 9. Create indexes for high performance ledger queries
CREATE INDEX IF NOT EXISTS idx_payments_contact_id ON payments(contact_id);
CREATE INDEX IF NOT EXISTS idx_payments_paid_at ON payments(paid_at);
CREATE INDEX IF NOT EXISTS idx_payments_receipt_number ON payments(receipt_number);
