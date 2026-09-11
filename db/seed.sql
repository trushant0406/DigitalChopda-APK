-- Wipes all transactional data (schema stays) and reseeds a small, realistic
-- demo dataset that exercises every feature built so far: calls of every
-- outcome, follow-ups in every bucket, orders across the full lifecycle
-- (including one cancelled), a multi-item order, and a small furniture
-- product catalog. Safe to re-run anytime — ids reset to 1 each time.

TRUNCATE TABLE push_subscriptions, followups, order_items, orders, calls, contacts, products
  RESTART IDENTITY CASCADE;

-- ── Products ────────────────────────────────────────────────────────────────
INSERT INTO products (name, default_price) VALUES
  ('Dining Table', 8500),
  ('High Table', 4200),
  ('Study Table', 3200),
  ('Office Chair', 2500),
  ('Bookshelf', 3800),
  ('Bed Frame (Queen)', 12000);

-- ── Contacts (ids 1-6, in this order) ───────────────────────────────────────
INSERT INTO contacts (phone, name, city, first_seen, last_contacted) VALUES
  ('+919876543210', 'Rajesh Kumar',    'Pune',      now() - interval '6 days', now() - interval '1 day'),
  ('+919876543211', 'Sunita Traders',  'Mumbai',    now() - interval '8 days', now() - interval '2 days'),
  ('+919876543212', 'Vivek Patel',     'Ahmedabad', now() - interval '2 days', now() - interval '2 days'),
  ('+919876543213', 'Meera Shah',      'Surat',     now() - interval '4 days', now() - interval '1 day'),
  ('+919876543214', 'Arjun Rao',       'Bangalore', now() - interval '3 days', now() - interval '2 hours'),
  ('+919876543215', 'Priya Nair',      'Kochi',     now() - interval '6 days', now() - interval '6 days');

-- ── Calls (ids 1-10, in this order) ─────────────────────────────────────────
INSERT INTO calls (contact_id, direction, outcome, duration_sec, started_at, notes, source) VALUES
  (1, 'incoming', 'answered', 180, now() - interval '6 days', 'Interested in a dining table set', 'manual'),
  (1, 'outgoing', 'answered', 90,  now() - interval '1 day',  'Confirmed order details',           'manual'),
  (2, 'incoming', 'answered', 240, now() - interval '8 days', 'Bulk order inquiry for office furniture', 'manual'),
  (2, 'incoming', 'answered', 60,  now() - interval '2 days', 'Placed a second order',              'manual'),
  (3, 'incoming', 'answered', 120, now() - interval '2 days', 'Wants a study table, will pay later', 'manual'),
  (4, 'incoming', 'missed',   NULL, now() - interval '4 days', NULL, 'manual'),
  (4, 'incoming', 'missed',   NULL, now() - interval '3 days', NULL, 'manual'),
  (4, 'incoming', 'busy',     NULL, now() - interval '1 day',  NULL, 'manual'),
  (5, 'incoming', 'answered', 150, now() - interval '2 hours', 'Interested in a bookshelf, call back today', 'manual'),
  (6, 'incoming', 'answered', 200, now() - interval '6 days', 'Asked about bed frame pricing', 'manual');

-- ── Orders (ids 1-6, in this order) ─────────────────────────────────────────
-- order_number gets a unique placeholder here (the column is UNIQUE), then the
-- UPDATE right after sets the real ORD-00001-style value from each row's id.
INSERT INTO orders (contact_id, call_id, order_number, payment_status, payment_method, fulfillment_status, status, notes) VALUES
  (1, 2,    'seed-1', 'paid',    'upi',           'dispatched', 'active',    NULL),
  (1, NULL, 'seed-2', 'paid',    'cash',          'delivered',  'active',    NULL),
  (1, NULL, 'seed-3', 'paid',    'card',          'ready',      'active',    NULL),
  (2, 4,    'seed-4', 'paid',    'bank_transfer', 'not_ready',  'active',    'Bulk office order'),
  (2, NULL, 'seed-5', 'pending', NULL,            'not_ready',  'cancelled', 'Customer changed their mind'),
  (3, 5,    'seed-6', 'pending', NULL,            'not_ready',  'active',    NULL);

UPDATE orders SET order_number = 'ORD-' || lpad(id::text, 5, '0');

-- ── Order items ──────────────────────────────────────────────────────────────
INSERT INTO order_items (order_id, product_name, quantity, unit_price) VALUES
  (1, 'Dining Table',  1, 8500),
  (2, 'High Table',    2, 4200),
  (3, 'Study Table',   1, 3200),
  (4, 'Office Chair',  10, 2500),
  (4, 'Bookshelf',     4, 3800),
  (5, 'Bed Frame (Queen)', 1, 12000),
  (6, 'Study Table',   1, 3200);

-- ── Follow-ups ───────────────────────────────────────────────────────────────
-- Rajesh: pending, a few days out
INSERT INTO followups (contact_id, call_id, order_id, due_at, reason, status, channel, sent_at) VALUES
  (1, 2,    NULL, now() + interval '3 days', 'Check satisfaction after delivery',        'pending', 'push',     NULL),
  (3, 5,    6,    now() + interval '1 day',  'Collect payment for order ORD-00006',      'pending', 'push',     NULL),
  (5, 9,    NULL, date_trunc('day', now()) + interval '18 hours', 'Call back about bookshelf', 'pending', 'push', NULL),
  (2, 4,    NULL, now() - interval '2 days', 'Follow up on bulk order satisfaction',     'pending', 'whatsapp', NULL),
  (1, 1,    NULL, now() - interval '5 days', 'Confirm dining table interest',            'done',    'push',     now() - interval '5 days'),
  (4, 8,    NULL, now() + interval '2 days', 'Try calling again — missed 3 times',       'pending', 'push',     NULL);
-- Priya (contact 6) deliberately gets no follow-up — she's the "never followed up" example.
