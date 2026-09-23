-- Migration 017: Link payments and support partially_paid status

-- 1. Ensure existing payments with order_id NULL that were recorded for a specific contact get linked to open orders if possible, or link REC-0024 to order 35
UPDATE payments
SET order_id = 35
WHERE id = 29 AND contact_id = 22 AND order_id IS NULL;

-- 2. Backfill any paid orders that don't have a payment record so their received_amount and balance_due match
DO $$
DECLARE
  ord RECORD;
  tot NUMERIC;
  paid NUMERIC;
  rec_seq INT;
  rec_num TEXT;
BEGIN
  -- Find max receipt number sequence
  SELECT COALESCE(MAX(NULLIF(regexp_replace(receipt_number, '\D', '', 'g'), '')::INT), 0)
  INTO rec_seq
  FROM payments;

  FOR ord IN
    SELECT o.id, o.contact_id, o.payment_method, o.created_at, o.invoice_number,
           COALESCE(SUM(oi.quantity * oi.unit_price) + SUM(oi.quantity * oi.unit_price * COALESCE(oi.gst_rate, 0) / 100), 0) AS grand_total,
           COALESCE((SELECT SUM(amount) FROM payments WHERE order_id = o.id), 0) AS total_paid
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.id
    WHERE o.payment_status = 'paid' AND o.status != 'cancelled'
    GROUP BY o.id, o.contact_id, o.payment_method, o.created_at, o.invoice_number
  LOOP
    IF ord.total_paid < ord.grand_total - 0.01 THEN
      rec_seq := rec_seq + 1;
      rec_num := 'REC-' || LPAD(rec_seq::TEXT, 4, '0');
      IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payments' AND column_name = 'business_id') THEN
        EXECUTE 'INSERT INTO payments (order_id, contact_id, amount, method, payment_mode, receipt_number, paid_at, notes, business_id) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 1)'
        USING ord.id, ord.contact_id, ord.grand_total - ord.total_paid, COALESCE(ord.payment_method, 'cash'), COALESCE(ord.payment_method, 'cash'), rec_num, ord.created_at, 'Settled on order creation / mark paid';
      ELSE
        INSERT INTO payments (order_id, contact_id, amount, method, payment_mode, receipt_number, paid_at, notes)
        VALUES (
          ord.id,
          ord.contact_id,
          ord.grand_total - ord.total_paid,
          COALESCE(ord.payment_method, 'cash'),
          COALESCE(ord.payment_method, 'cash'),
          rec_num,
          ord.created_at,
          'Settled on order creation / mark paid'
        );
      END IF;
    END IF;
  END LOOP;
END $$;
