-- Seller's own details, entered once in Settings.
CREATE TABLE IF NOT EXISTS business_profile (
  id                SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  name              TEXT,
  address           TEXT,
  gstin             TEXT,
  pan               TEXT,
  mobile            TEXT,
  state             TEXT,
  bank_name         TEXT,
  bank_account_no   TEXT,
  bank_ifsc         TEXT,
  bank_branch       TEXT,
  terms_and_conditions TEXT DEFAULT 'Thanks',
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- GST/tax + address details per customer — optional, filled in once and reused
-- on every quotation for that contact.
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS gstin TEXT;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS pan TEXT;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS billing_address TEXT;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS shipping_address TEXT;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS state TEXT;

-- HSN + tax rate per catalog product, so quotation line items don't need
-- retyping them every time.
ALTER TABLE products ADD COLUMN IF NOT EXISTS hsn_code TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS gst_rate NUMERIC NOT NULL DEFAULT 18;

CREATE TABLE IF NOT EXISTS quotations (
  id                SERIAL PRIMARY KEY,
  contact_id        INTEGER NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
  quotation_date    DATE NOT NULL DEFAULT CURRENT_DATE,
  bill_to_name      TEXT NOT NULL,
  bill_to_address   TEXT,
  bill_to_mobile    TEXT,
  bill_to_gstin     TEXT,
  bill_to_pan       TEXT,
  ship_to_name      TEXT,
  ship_to_address   TEXT,
  place_of_supply   TEXT,
  tax_mode          TEXT NOT NULL DEFAULT 'igst' CHECK (tax_mode IN ('igst', 'split')),
  terms_and_conditions TEXT,
  status            TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'sent')),
  sent_at           TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS quotation_items (
  id            SERIAL PRIMARY KEY,
  quotation_id  INTEGER NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
  product_name  TEXT NOT NULL,
  hsn_code      TEXT,
  unit          TEXT NOT NULL DEFAULT 'PCS',
  quantity      NUMERIC NOT NULL CHECK (quantity > 0),
  rate          NUMERIC NOT NULL CHECK (rate >= 0),
  gst_rate      NUMERIC NOT NULL DEFAULT 18
);

CREATE INDEX IF NOT EXISTS idx_quotations_contact_id ON quotations(contact_id);
CREATE INDEX IF NOT EXISTS idx_quotation_items_quotation_id ON quotation_items(quotation_id);
