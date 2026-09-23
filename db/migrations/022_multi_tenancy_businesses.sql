-- Migration 022: Multi-Tenancy Architecture (Businesses & Tenant Scoping)

-- 1. Create businesses table (multi-tenant business entities)
CREATE TABLE IF NOT EXISTS businesses (
  id                   SERIAL PRIMARY KEY,
  name                 TEXT NOT NULL,
  trade_name           TEXT,
  address              TEXT,
  gstin                TEXT,
  pan                  TEXT,
  mobile               TEXT,
  email                TEXT,
  state                TEXT,
  bank_name            TEXT,
  bank_account_no      TEXT,
  bank_ifsc            TEXT,
  bank_branch          TEXT,
  terms_and_conditions TEXT DEFAULT 'Thanks',
  signature_image      TEXT,
  logo_image           TEXT,
  invoice_prefix       TEXT NOT NULL DEFAULT 'INV-',
  invoice_next_number  INTEGER NOT NULL DEFAULT 1,
  invoice_padding      INTEGER NOT NULL DEFAULT 0,
  currency             TEXT NOT NULL DEFAULT 'INR',
  status               TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'trial')),
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Ensure default business with id=1 always exists
INSERT INTO businesses (id, name, invoice_prefix, invoice_next_number, invoice_padding)
VALUES (1, 'My Business', 'INV-', 1, 0)
ON CONFLICT (id) DO NOTHING;

-- If business_profile has data, copy it over
UPDATE businesses b
SET
  name = COALESCE(bp.name, b.name),
  address = bp.address,
  gstin = bp.gstin,
  pan = bp.pan,
  mobile = bp.mobile,
  state = bp.state,
  bank_name = bp.bank_name,
  bank_account_no = bp.bank_account_no,
  bank_ifsc = bp.bank_ifsc,
  bank_branch = bp.bank_branch,
  terms_and_conditions = COALESCE(bp.terms_and_conditions, b.terms_and_conditions),
  signature_image = bp.signature_image,
  invoice_prefix = COALESCE(bp.invoice_prefix, b.invoice_prefix),
  invoice_next_number = COALESCE(bp.invoice_next_number, b.invoice_next_number),
  invoice_padding = COALESCE(bp.invoice_padding, b.invoice_padding)
FROM business_profile bp
WHERE bp.id = b.id;

-- Reset sequence
SELECT setval('businesses_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM businesses));

-- 2. Create business_users (many-to-many relationship between users and businesses)
CREATE TABLE IF NOT EXISTS business_users (
  id          SERIAL PRIMARY KEY,
  business_id INTEGER NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role        TEXT NOT NULL DEFAULT 'owner' CHECK (role IN ('owner', 'admin', 'accountant', 'staff')),
  is_active   BOOLEAN NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(business_id, user_id)
);

-- Associate all existing users with business 1 as owners
INSERT INTO business_users (business_id, user_id, role)
SELECT 1, id, 'owner'
FROM users
ON CONFLICT (business_id, user_id) DO NOTHING;

-- 3. Add business_id to core entity tables
-- contacts
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE contacts SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE contacts ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE contacts ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE contacts DROP CONSTRAINT IF EXISTS contacts_phone_key;
ALTER TABLE contacts DROP CONSTRAINT IF EXISTS contacts_business_phone_key;
ALTER TABLE contacts ADD CONSTRAINT contacts_business_phone_key UNIQUE (business_id, phone);
CREATE INDEX IF NOT EXISTS idx_contacts_business_id ON contacts(business_id);

-- products
ALTER TABLE products ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE products SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE products ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE products ALTER COLUMN business_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_business_id ON products(business_id);

-- orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE orders SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE orders ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE orders ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_number_key;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_invoice_number_key;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_business_order_number_key;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_business_invoice_number_key;
ALTER TABLE orders ADD CONSTRAINT orders_business_order_number_key UNIQUE (business_id, order_number);
ALTER TABLE orders ADD CONSTRAINT orders_business_invoice_number_key UNIQUE (business_id, invoice_number);
CREATE INDEX IF NOT EXISTS idx_orders_business_id ON orders(business_id);

-- quotations
ALTER TABLE quotations ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE quotations SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE quotations ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE quotations ALTER COLUMN business_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_quotations_business_id ON quotations(business_id);

-- credit_notes
ALTER TABLE credit_notes ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE credit_notes SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE credit_notes ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE credit_notes ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE credit_notes DROP CONSTRAINT IF EXISTS credit_notes_credit_note_number_key;
ALTER TABLE credit_notes DROP CONSTRAINT IF EXISTS credit_notes_business_number_key;
ALTER TABLE credit_notes ADD CONSTRAINT credit_notes_business_number_key UNIQUE (business_id, credit_note_number);
CREATE INDEX IF NOT EXISTS idx_credit_notes_business_id ON credit_notes(business_id);

-- payments
ALTER TABLE payments ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE payments SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE payments ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE payments ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_receipt_number_key;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_business_receipt_number_key;
ALTER TABLE payments ADD CONSTRAINT payments_business_receipt_number_key UNIQUE (business_id, receipt_number);
CREATE INDEX IF NOT EXISTS idx_payments_business_id ON payments(business_id);

-- calls
ALTER TABLE calls ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE calls SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE calls ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE calls ALTER COLUMN business_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_calls_business_id ON calls(business_id);

-- followups
ALTER TABLE followups ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE followups SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE followups ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE followups ALTER COLUMN business_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_followups_business_id ON followups(business_id);

-- push_subscriptions
ALTER TABLE push_subscriptions ADD COLUMN IF NOT EXISTS business_id INTEGER DEFAULT 1 REFERENCES businesses(id) ON DELETE CASCADE;
UPDATE push_subscriptions SET business_id = 1 WHERE business_id IS NULL;
ALTER TABLE push_subscriptions ALTER COLUMN business_id SET DEFAULT 1;
ALTER TABLE push_subscriptions ALTER COLUMN business_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_push_subs_business_id ON push_subscriptions(business_id);
