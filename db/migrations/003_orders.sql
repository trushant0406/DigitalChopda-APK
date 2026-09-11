CREATE TABLE IF NOT EXISTS orders (
  id                 SERIAL PRIMARY KEY,
  contact_id         INTEGER NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
  call_id            INTEGER REFERENCES calls(id) ON DELETE SET NULL,
  order_number       TEXT NOT NULL UNIQUE,
  payment_status     TEXT NOT NULL DEFAULT 'pending' CHECK (payment_status IN ('pending', 'paid')),
  fulfillment_status TEXT NOT NULL DEFAULT 'not_ready' CHECK (fulfillment_status IN ('not_ready', 'ready', 'dispatched', 'delivered')),
  notes              TEXT,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_items (
  id           SERIAL PRIMARY KEY,
  order_id     INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_name TEXT NOT NULL,
  quantity     NUMERIC NOT NULL CHECK (quantity > 0),
  unit_price   NUMERIC NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_orders_contact_id ON orders(contact_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
