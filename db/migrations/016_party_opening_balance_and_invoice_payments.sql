-- Migration 016: Party Opening Balance and Historical Invoice Payments Sync

-- 1. Add opening balance fields to contacts
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS opening_balance NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS opening_balance_type TEXT NOT NULL DEFAULT 'to_receive'; -- 'to_receive' (debit) or 'to_pay' (credit)
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS opening_balance_date DATE DEFAULT CURRENT_DATE;

-- 2. Drop strict legacy method check on payments
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_method_check;

-- 3. Backfill payments for all historical paid orders that don't yet have a payment record
-- This ensures the party ledger double-entry passbook is 100% accurate:
-- Sales Invoice = Debit, Paid at invoice = Credit
DO $$
DECLARE
  ord RECORD;
  tot NUMERIC;
  seq INT;
  pm TEXT;
BEGIN
  -- Determine current highest receipt sequence
  SELECT COALESCE(MAX(NULLIF(regexp_replace(receipt_number, '\D', '', 'g'), '')::INT), 0) + 1 INTO seq
  FROM payments WHERE receipt_number IS NOT NULL;

  FOR ord IN
    SELECT o.id, o.contact_id, o.created_at, o.payment_method, o.invoice_date
    FROM orders o
    WHERE o.payment_status = 'paid'
      AND o.status != 'cancelled'
      AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id)
    ORDER BY o.id ASC
  LOOP
    -- Calculate order grand total (base + tax)
    SELECT COALESCE(SUM(oi.quantity * oi.unit_price) + SUM(oi.quantity * oi.unit_price * COALESCE(oi.gst_rate, 0) / 100), 0)
    INTO tot
    FROM order_items oi WHERE oi.order_id = ord.id;

    IF ord.payment_method IN ('cash', 'upi', 'card', 'bank_transfer') THEN
      pm := ord.payment_method;
    ELSE
      pm := 'cash';
    END IF;

    IF tot > 0 THEN
      INSERT INTO payments (
        order_id, contact_id, amount, payment_mode, method,
        paid_at, notes, receipt_number
      ) VALUES (
        ord.id,
        ord.contact_id,
        tot,
        pm,
        pm,
        COALESCE(ord.invoice_date::timestamptz, ord.created_at),
        'Paid at invoice generation',
        'REC-' || LPAD(seq::TEXT, 4, '0')
      );
      seq := seq + 1;
    END IF;
  END LOOP;
END $$;
