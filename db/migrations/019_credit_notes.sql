-- Migration 019: Credit Notes (Sales Return) — myBillBook Parity
-- Supports customer goods returns, partial quantity returns, and credit adjustments
-- Automatically enters a Credit entry into the party ledger, reducing receivable balance.

CREATE TABLE IF NOT EXISTS credit_notes (
  id                    SERIAL PRIMARY KEY,
  credit_note_number    TEXT UNIQUE NOT NULL,
  order_id              INTEGER REFERENCES orders(id) ON DELETE SET NULL,
  contact_id            INTEGER NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
  credit_note_date      TIMESTAMPTZ NOT NULL DEFAULT now(),
  original_invoice_no   TEXT,
  original_invoice_date DATE,
  reason                TEXT NOT NULL DEFAULT 'Sales Return',
  notes                 TEXT,
  subtotal              NUMERIC NOT NULL DEFAULT 0,
  tax_amount            NUMERIC NOT NULL DEFAULT 0,
  total_amount          NUMERIC NOT NULL DEFAULT 0,
  tax_mode              TEXT DEFAULT 'split',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS credit_note_items (
  id                  SERIAL PRIMARY KEY,
  credit_note_id      INTEGER NOT NULL REFERENCES credit_notes(id) ON DELETE CASCADE,
  product_name        TEXT NOT NULL,
  quantity            NUMERIC NOT NULL CHECK (quantity > 0),
  unit_price          NUMERIC NOT NULL CHECK (unit_price >= 0),
  gst_rate            NUMERIC NOT NULL DEFAULT 0,
  hsn_code            TEXT
);

CREATE INDEX IF NOT EXISTS idx_credit_notes_contact ON credit_notes(contact_id);
CREATE INDEX IF NOT EXISTS idx_credit_notes_order ON credit_notes(order_id);
CREATE INDEX IF NOT EXISTS idx_credit_notes_date ON credit_notes(credit_note_date);
CREATE INDEX IF NOT EXISTS idx_credit_note_items_cn ON credit_note_items(credit_note_id);
