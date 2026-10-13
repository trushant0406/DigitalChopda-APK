-- ============================================================================
-- DigitalChopda.in — Enterprise Realistic Demo & Mock Dataset (All Modules)
--
-- Covers:
-- 1. Multi-Tenant Businesses (Shreeji Infotech & Apex Industrial) & RBAC Users
-- 2. Products Catalog across 0%, 5%, 12%, 18%, 28% GST with standard HSN codes
-- 3. CRM Contacts / Parties with credit limits, debit/credit opening balances
-- 4. Customer Khata Lifecycle: Over-Limit, Settled, Outstanding balances
-- 5. Call Journal: Answered with duration, Missed, Busy, Incoming/Outgoing, Calls Today
-- 6. Dashboard Action Triggers: Due Today, Overdue, Never Followed Up, Repeat Callers
-- 7. Follow-ups in all lifecycle buckets (Due today, Overdue, Upcoming, Completed)
-- 8. Quotations: Draft, Sent, Converted to Invoice, with multi-item tax breakdown
-- 9. Orders & Sales Invoices: Delivered, Dispatched, Ready, Not Ready, Cancelled
-- 10. Payment In Receipts & Khata Ledger Running Balance
-- 11. Sales Returns / Credit Notes with line items, tax adjustment & ledger credit
-- ============================================================================

TRUNCATE TABLE 
  push_subscriptions,
  credit_note_items,
  credit_notes,
  payments,
  order_items,
  orders,
  quotation_items,
  quotations,
  followups,
  calls,
  contacts,
  products,
  business_users,
  businesses,
  business_profile,
  users
RESTART IDENTITY CASCADE;

-- ── 1. Users (PBKDF2 SHA-512 with 'callintel_salt_2026') ────────────────────
-- admin: admin123
-- dhruv / priya / rajesh: password123
INSERT INTO users (id, username, password_hash, name, role, created_at) VALUES
  (1, 'admin',  '05e9fd2690322eca9166e3550322f385dc162cd99f8a79d6626a56eaa3e58c1ea0a2cd5ceaba31882a3a8522fa2ee298c054746f1432a23a00e9653c333da91d', 'Savan Admin',  'admin', now() - interval '60 days'),
  (2, 'dhruv',  'e11c4794e772eaef4e704620f3a61c6b6d510eefcae393a52140889bf8ef342f02540cb0fa29141beeb7e289bf63d002f2b4fb71317d66fa404ebca3eb0cfeb5', 'Dhruv Patel',  'admin', now() - interval '45 days'),
  (3, 'priya',  'e11c4794e772eaef4e704620f3a61c6b6d510eefcae393a52140889bf8ef342f02540cb0fa29141beeb7e289bf63d002f2b4fb71317d66fa404ebca3eb0cfeb5', 'Priya Sharma', 'staff', now() - interval '30 days'),
  (4, 'rajesh', 'e11c4794e772eaef4e704620f3a61c6b6d510eefcae393a52140889bf8ef342f02540cb0fa29141beeb7e289bf63d002f2b4fb71317d66fa404ebca3eb0cfeb5', 'Rajesh Varma', 'staff', now() - interval '20 days');

-- ── 2. Businesses (Multi-Tenancy) ───────────────────────────────────────────
INSERT INTO businesses (
  id, name, trade_name, address, gstin, pan, mobile, email, state,
  bank_name, bank_account_no, bank_ifsc, bank_branch,
  terms_and_conditions, invoice_prefix, invoice_next_number, invoice_padding, currency, status
) VALUES
  (
    1,
    'Shreeji Infotech & Retail Solutions',
    'DigitalChopda Surat Flagship',
    'Shop 201-204, Millennium Textile Market-2, Ring Road, Surat',
    '24AAACS1429B1ZB',
    'AAACS1429B',
    '9825100100',
    'billing@shreeji-infotech.com',
    'Gujarat',
    'HDFC Bank',
    '50200088991122',
    'HDFC0000123',
    'Ring Road, Surat',
    '1. Payment strictly due within 15 days of invoice date.\n2. Goods once sold will not be taken back without original packaging.\n3. All disputes subject to Surat jurisdiction.',
    'INV-',
    111,
    5,
    'INR',
    'active'
  ),
  (
    2,
    'Apex Industrial Supplies & Hardware',
    'Apex Tools Hub',
    'Plot 45, GIDC Industrial Estate, Sachin, Surat',
    '24AABCA5678C1ZD',
    'AABCA5678C',
    '9825200200',
    'sales@apex-hardware.in',
    'Gujarat',
    'State Bank of India',
    '33445566778',
    'SBIN0001234',
    'GIDC Sachin, Surat',
    '1. Manufacturer warranty applicable on all power tools.\n2. Interest @ 18% p.a. charged after due date.',
    'AIP-',
    53,
    4,
    'INR',
    'active'
  );

-- Sync legacy profile table for tenant 1
INSERT INTO business_profile (
  id, name, address, gstin, pan, mobile, state,
  bank_name, bank_account_no, bank_ifsc, bank_branch,
  terms_and_conditions, invoice_prefix, invoice_next_number, invoice_padding
) VALUES (
  1,
  'Shreeji Infotech & Retail Solutions',
  'Shop 201-204, Millennium Textile Market-2, Ring Road, Surat',
  '24AAACS1429B1ZB',
  'AAACS1429B',
  '9825100100',
  'Gujarat',
  'HDFC Bank',
  '50200088991122',
  'HDFC0000123',
  'Ring Road, Surat',
  '1. Payment strictly due within 15 days of invoice date.\n2. Goods once sold will not be taken back without original packaging.\n3. All disputes subject to Surat jurisdiction.',
  'INV-',
  111,
  5
) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, gstin = EXCLUDED.gstin;

-- Assign users to businesses
INSERT INTO business_users (business_id, user_id, role, is_active) VALUES
  (1, 1, 'owner', true),
  (1, 2, 'admin', true),
  (1, 3, 'accountant', true),
  (1, 4, 'staff', true),
  (2, 1, 'owner', true),
  (2, 2, 'owner', true);

-- ── 3. Products Catalog ─────────────────────────────────────────────────────
INSERT INTO products (name, default_price, hsn_code, gst_rate, business_id) VALUES
  -- Tenant 1
  ('Raw Cotton Bales (100kg)',                     6200,  '5201', 0,  1),
  ('Cotton Fabric Cambric (100 Mtr)',               4500,  '5208', 5,  1),
  ('Polyester Georgette Fabric (50 Mtr)',           3800,  '5407', 5,  1),
  ('Cotton Yarn Spools (Pack of 12)',               1450,  '5205', 5,  1),
  ('Industrial Barcode Scanner USB',                3200,  '8471', 12, 1),
  ('Thermal Receipt Printer 80mm',                  4800,  '8443', 12, 1),
  ('Ergonomic Executive Office Chair',              5500,  '9403', 12, 1),
  ('Solid Teak Wooden Workstation Desk',            14000, '9403', 12, 1),
  ('24" IPS Full HD LED Monitor',                   9800,  '8528', 18, 1),
  ('Commercial Wi-Fi 6 Mesh Router',                7200,  '8517', 18, 1),
  ('Enterprise Billing Software Single-User License', 12500, '9973', 18, 1),
  ('4K Dome Security CCTV Camera',                  4200,  '8525', 18, 1),
  ('Smart Digital Biometric Attendance Device',     8900,  '8471', 18, 1),
  ('Commercial 2.0 Ton Inverter Air Conditioner',   48000, '8415', 28, 1),
  ('Motorized Luxury Recliner Chair',               32000, '9401', 28, 1),

  -- Tenant 2
  ('Heavy Duty Angle Grinder 850W',                 2600,  '8467', 18, 2),
  ('Industrial Safety Helmet Yellow',                350,  '6506', 18, 2),
  ('High-Speed Steel Drill Bit Set (25 Pcs)',       1850,  '8207', 18, 2),
  ('Hydraulic Machine Lubricant Oil (20 Ltr)',      5400,  '2710', 18, 2);

-- ── 4. Contacts / CRM Parties ───────────────────────────────────────────────
INSERT INTO contacts (
  id, phone, name, city, first_seen, last_contacted, gstin, pan,
  billing_address, shipping_address, state, credit_limit,
  opening_balance, opening_balance_type, opening_balance_date, business_id
) VALUES
  (1, '9825111001', 'Surat Garment World', 'Surat', now() - interval '45 days', now() - interval '1 day', '24AADCS8899E1ZG', 'AADCS8899E', '12, Regent Textile Market, Ring Road, Surat', '12, Regent Textile Market, Ring Road, Surat', 'Gujarat', 150000, 25000, 'to_receive', CURRENT_DATE - 30, 1),
  (2, '9825111002', 'Krishna Enterprise & Retailers', 'Ahmedabad', now() - interval '30 days', now() - interval '4 days', '24AAECK7766F1ZK', 'AAECK7766F', '401, Supermall, CG Road, Ahmedabad', '401, Supermall, CG Road, Ahmedabad', 'Gujarat', 60000, 40000, 'to_receive', CURRENT_DATE - 20, 1),
  (3, '9825111003', 'Mahavir Textiles & Furnishings', 'Mumbai', now() - interval '20 days', now() - interval '3 days', '27AABCM3344D1ZU', 'AABCM3344D', '88, Kalbadevi Road, Marine Lines, Mumbai', '88, Kalbadevi Road, Marine Lines, Mumbai', 'Maharashtra', 200000, 0, 'to_receive', CURRENT_DATE - 20, 1),
  (4, '9825111004', 'Shree Ambica Traders', 'Vadodara', now() - interval '25 days', now() - interval '2 days', '24AAYPS5522H1ZN', 'AAYPS5522H', '5, Alkapuri Arcade, RC Dutt Road, Vadodara', '5, Alkapuri Arcade, RC Dutt Road, Vadodara', 'Gujarat', 50000, 10000, 'to_pay', CURRENT_DATE - 25, 1),
  (5, '9825111005', 'Om Sai Electricals & Solutions', 'Pune', now() - interval '15 days', now() - interval '2 days', '27AAPCS9911K1ZS', 'AAPCS9911K', '14, FC Road, Shivajinagar, Pune', '14, FC Road, Shivajinagar, Pune', 'Maharashtra', 100000, 0, 'to_receive', CURRENT_DATE - 15, 1),
  (6, '9825111006', 'Maruti Tech Innovations', 'Rajkot', now() - interval '10 days', now() - interval '1 day', '24AAEFM1234G1ZT', 'AAEFM1234G', '2nd Floor, Crystal Mall, Kalawad Road, Rajkot', '2nd Floor, Crystal Mall, Kalawad Road, Rajkot', 'Gujarat', 75000, 0, 'to_receive', CURRENT_DATE - 10, 1),
  (7, '9825111007', 'Meera Fashion Hub', 'Navsari', now() - interval '6 days', now() - interval '1 day', NULL, NULL, 'Station Road, Navsari', 'Station Road, Navsari', 'Gujarat', 30000, 0, 'to_receive', CURRENT_DATE - 6, 1),
  (8, '9825111008', 'Navkar Stationery & Office Mart', 'Surat', now() - interval '12 days', now() - interval '3 days', '24AAGPN8822J1ZX', 'AAGPN8822J', 'Shop 18, Mini Bazar, Varachha, Surat', 'Shop 18, Mini Bazar, Varachha, Surat', 'Gujarat', 50000, 0, 'to_receive', CURRENT_DATE - 12, 1),
  (9, '9825111009', 'Vibrant Lifestyle Stores', 'Bengaluru', now() - interval '8 days', now() - interval '2 days', '29AAACV4433P1ZR', 'AAACV4433P', '100ft Road, Indiranagar, Bengaluru', '100ft Road, Indiranagar, Bengaluru', 'Karnataka', 150000, 0, 'to_receive', CURRENT_DATE - 8, 1),
  (10, '9825111010', 'Riddhi Siddhi Enterprises', 'Bhavnagar', now() - interval '5 days', now() - interval '2 hours', NULL, NULL, 'High Street Market, Bhavnagar', 'High Street Market, Bhavnagar', 'Gujarat', 40000, 0, 'to_receive', CURRENT_DATE - 5, 1),
  (11, '9825111011', 'Galaxy Digital Systems', 'Gandhinagar', now() - interval '14 days', now() - interval '4 hours', '24AAACG9988L1ZQ', 'AAACG9988L', 'Infocity Tower 2, Gandhinagar', 'Infocity Tower 2, Gandhinagar', 'Gujarat', 80000, 0, 'to_receive', CURRENT_DATE - 14, 1),
  (12, '9825111012', 'Kiran Prints & Dyers', 'Surat', now() - interval '7 days', now() - interval '5 days', NULL, NULL, 'Anjana Farm, Surat', 'Anjana Farm, Surat', 'Gujarat', 25000, 0, 'to_receive', CURRENT_DATE - 7, 1),
  -- Tenant 2
  (13, '9825222001', 'Hazira Heavy Fab Ltd', 'Surat', now() - interval '20 days', now() - interval '2 days', '24AABCH1122D1ZF', 'AABCH1122D', 'Hazira Port Road, Surat', 'Hazira Port Road, Surat', 'Gujarat', 100000, 0, 'to_receive', CURRENT_DATE - 20, 2),
  (14, '9825222002', 'Sachin Engineering Works', 'Surat', now() - interval '15 days', now() - interval '3 days', '24AABCS3344F1ZH', 'AABCS3344F', 'Plot 120, GIDC Sachin, Surat', 'Plot 120, GIDC Sachin, Surat', 'Gujarat', 50000, 0, 'to_receive', CURRENT_DATE - 15, 2);

-- ── 5. Calls ────────────────────────────────────────────────────────────────
INSERT INTO calls (
  id, contact_id, direction, outcome, duration_sec, started_at, notes, source, business_id
) VALUES
  (1, 1, 'incoming', 'answered', 180, now() - interval '12 days', 'Inquired about cotton fabric cambric pricing & lead times', 'manual', 1),
  (2, 1, 'outgoing', 'answered', 95,  now() - interval '8 days',  'Confirmed order specifications and delivery address', 'manual', 1),
  (3, 2, 'incoming', 'answered', 210, now() - interval '14 days', 'Bulk requirement inquiry for LED monitors & biometric devices', 'manual', 1),
  (4, 2, 'outgoing', 'answered', 120, now() - interval '5 days',  'Followed up on invoice payment; client promised transfer soon', 'manual', 1),
  (5, 3, 'incoming', 'answered', 150, now() - interval '10 days', 'Office furniture order requirements for Mumbai branch', 'manual', 1),
  (6, 3, 'outgoing', 'answered', 75,  now() - interval '4 days',  'Payment receipt confirmation; delivery scheduled', 'manual', 1),
  (7, 4, 'incoming', 'answered', 130, now() - interval '6 days',  'Reporting barcode scanner glass crack upon delivery', 'manual', 1),
  (8, 5, 'incoming', 'answered', 140, now() - interval '3 days',  'Discussed quotation for CCTV cameras & Wi-Fi routers', 'manual', 1),
  -- Repeat caller never connected (3 incoming, 0 answered)
  (9,  6, 'incoming', 'missed', NULL, now() - interval '4 days', NULL, 'manual', 1),
  (10, 6, 'incoming', 'busy',   NULL, now() - interval '3 days', NULL, 'manual', 1),
  (11, 6, 'incoming', 'missed', NULL, now() - interval '1 day',  NULL, 'manual', 1),
  -- Never followed up (Calls logged, 0 followups)
  (12, 7, 'incoming', 'answered', 160, now() - interval '2 days', 'Inquired about summer georgette prints. Wants sample catalogue', 'manual', 1),
  (13, 7, 'outgoing', 'answered', 90,  now() - interval '1 day',  'Shared PDF price list on WhatsApp', 'manual', 1),
  (14, 8, 'incoming', 'answered', 110, now() - interval '5 days', 'Discussed thermal receipt printers order', 'manual', 1),
  (15, 9, 'incoming', 'answered', 190, now() - interval '2 days', 'Requested quotation for executive office chairs and desks', 'manual', 1),
  -- Calls Today
  (16, 10, 'incoming', 'answered', 145, now() - interval '2 hours', 'Inquired about dispatch tracking for today', 'manual', 1),
  (17, 11, 'outgoing', 'answered', 80,  now() - interval '4 hours', 'Delivered goods verification call; customer confirmed receipt', 'manual', 1),
  -- Tenant 2 Call
  (18, 13, 'incoming', 'answered', 120, now() - interval '2 days',  'Inquired about bulk discount on drill bits and angle grinders', 'manual', 2);

-- ── 6. Follow-ups ───────────────────────────────────────────────────────────
INSERT INTO followups (
  id, contact_id, call_id, order_id, due_at, reason, status, channel, sent_at, business_id
) VALUES
  (1, 9, 15, NULL, date_trunc('day', now()) + interval '14 hours', 'Follow up on quotation QTN-00004 approval & bulk discount', 'pending', 'whatsapp', NULL, 1),
  (2, 1, 2, NULL,  date_trunc('day', now()) + interval '17 hours', 'Collect remaining balance of ₹21,300 for invoice INV-00102', 'pending', 'push', NULL, 1),
  (3, 8, 14, NULL, now() - interval '3 days', 'Collect payment of overdue khata invoice', 'pending', 'push', NULL, 1),
  (4, 2, 4, NULL,  now() - interval '1 day',  'Critical over-limit credit collection for Krishna Enterprise', 'pending', 'whatsapp', NULL, 1),
  (5, 5, 8, NULL,  now() + interval '2 days', 'Follow up on quotation for Pune office setup', 'pending', 'push', NULL, 1),
  (6, 3, 6, NULL,  now() + interval '5 days', 'Check customer satisfaction after delivery of office furniture', 'pending', 'push', NULL, 1),
  (7, 1, 1, NULL,  now() - interval '7 days', 'Confirm order advance receipt', 'done', 'push', now() - interval '7 days', 1),
  (8, 4, 7, NULL,  now() - interval '4 days', 'Credit note adjustment confirmation', 'done', 'whatsapp', now() - interval '4 days', 1),
  (9, 13, 18, NULL, date_trunc('day', now()) + interval '15 hours', 'Send commercial proposal for industrial tools', 'pending', 'whatsapp', NULL, 2);

-- ── 7. Quotations & Items ───────────────────────────────────────────────────
INSERT INTO quotations (
  id, contact_id, quotation_date, bill_to_name, bill_to_address, bill_to_mobile, bill_to_gstin, bill_to_pan,
  ship_to_name, ship_to_address, place_of_supply, tax_mode, terms_and_conditions, status, sent_at, business_id
) VALUES
  (1, 1, CURRENT_DATE - 10, 'Surat Garment World', '12, Regent Textile Market, Ring Road, Surat', '9825111001', '24AADCS8899E1ZG', 'AADCS8899E', 'Surat Garment World', '12, Regent Textile Market, Ring Road, Surat', 'Gujarat', 'split', 'Valid for 15 days from date of issue.', 'converted', now() - interval '9 days', 1),
  (2, 5, CURRENT_DATE - 3,  'Om Sai Electricals & Solutions', '14, FC Road, Shivajinagar, Pune', '9825111005', '27AAPCS9911K1ZS', 'AAPCS9911K', 'Om Sai Electricals & Solutions', '14, FC Road, Shivajinagar, Pune', 'Maharashtra', 'igst', 'Payment: 50% advance, balance on delivery.', 'sent', now() - interval '2 days', 1),
  (3, 3, CURRENT_DATE - 1,  'Mahavir Textiles & Furnishings', '88, Kalbadevi Road, Marine Lines, Mumbai', '9825111003', '27AABCM3344D1ZU', 'AABCM3344D', 'Mahavir Textiles & Furnishings', '88, Kalbadevi Road, Marine Lines, Mumbai', 'Maharashtra', 'igst', 'Installation and delivery included.', 'draft', NULL, 1),
  (4, 9, CURRENT_DATE - 2,  'Vibrant Lifestyle Stores', '100ft Road, Indiranagar, Bengaluru', '9825111009', '29AAACV4433P1ZR', 'AAACV4433P', 'Vibrant Lifestyle Stores', '100ft Road, Indiranagar, Bengaluru', 'Karnataka', 'igst', 'Special corporate introductory pricing.', 'sent', now() - interval '1 day', 1),
  (5, 11, CURRENT_DATE - 8, 'Galaxy Digital Systems', 'Infocity Tower 2, Gandhinagar', '9825111011', '24AAACG9988L1ZQ', 'AAACG9988L', 'Galaxy Digital Systems', 'Infocity Tower 2, Gandhinagar', 'Gujarat', 'split', 'Prices subject to market fluctuation. Validity: 7 days.', 'draft', NULL, 1),
  (6, 13, CURRENT_DATE - 2, 'Hazira Heavy Fab Ltd', 'Hazira Port Road, Surat', '9825222001', '24AABCH1122D1ZF', 'AABCH1122D', 'Hazira Heavy Fab Ltd', 'Hazira Port Road, Surat', 'Gujarat', 'split', 'Standard industrial supply warranty applies.', 'sent', now() - interval '1 day', 2);

INSERT INTO quotation_items (quotation_id, product_name, hsn_code, unit, quantity, rate, gst_rate) VALUES
  (1, 'Cotton Fabric Cambric (100 Mtr)', '5208', 'MTR', 2, 4500, 5),
  (1, 'Cotton Yarn Spools (Pack of 12)', '5205', 'SET', 4, 1450, 5),
  (2, '4K Dome Security CCTV Camera', '8525', 'PCS', 6, 4200, 18),
  (2, 'Commercial Wi-Fi 6 Mesh Router', '8517', 'PCS', 2, 7200, 18),
  (3, 'Solid Teak Wooden Workstation Desk', '9403', 'PCS', 3, 14000, 12),
  (3, '24" IPS Full HD LED Monitor', '8528', 'PCS', 3, 9800, 18),
  (4, 'Ergonomic Executive Office Chair', '9403', 'PCS', 10, 5500, 12),
  (5, 'Commercial 2.0 Ton Inverter Air Conditioner', '8415', 'PCS', 2, 48000, 28),
  (6, 'Hydraulic Machine Lubricant Oil (20 Ltr)', '2710', 'PCS', 5, 5400, 18);

-- ── 8. Orders & Invoices ────────────────────────────────────────────────────
INSERT INTO orders (
  id, contact_id, call_id, order_number, invoice_number, invoice_date,
  payment_status, fulfillment_status, payment_method, status, notes,
  place_of_supply, tax_mode, round_off, paid_at, quotation_id, created_at, updated_at, business_id
) VALUES
  (1, 1, 2, 'ORD-00101', 'INV-00101', CURRENT_DATE - 8, 'paid', 'delivered', 'upi', 'active', 'Converted from quotation QTN-00001', 'Gujarat', 'split', 0, now() - interval '8 days', 1, now() - interval '8 days', now() - interval '8 days', 1),
  (2, 2, 4, 'ORD-00102', 'INV-00102', CURRENT_DATE - 6, 'partially_paid', 'dispatched', 'bank_transfer', 'active', 'Part payment received; dispatch cleared', 'Gujarat', 'split', 0, NULL, NULL, now() - interval '6 days', now() - interval '6 days', 1),
  (3, 2, NULL, 'ORD-00103', 'INV-00103', CURRENT_DATE - 4, 'pending', 'not_ready', NULL, 'active', 'Urgent software license deployment', 'Gujarat', 'split', 0, NULL, NULL, now() - interval '4 days', now() - interval '4 days', 1),
  (4, 3, 6, 'ORD-00104', 'INV-00104', CURRENT_DATE - 5, 'paid', 'delivered', 'bank_transfer', 'active', 'Office workstation setup for Mumbai HQ', 'Maharashtra', 'igst', 0, now() - interval '5 days', NULL, now() - interval '5 days', now() - interval '5 days', 1),
  (5, 4, NULL, 'ORD-00105', 'INV-00105', CURRENT_DATE - 3, 'pending', 'ready', NULL, 'active', 'Packed and awaiting courier pickup', 'Gujarat', 'split', 0, NULL, NULL, now() - interval '3 days', now() - interval '3 days', 1),
  (6, 10, 16, 'ORD-00106', 'INV-00106', CURRENT_DATE - 1, 'paid', 'ready', 'cash', 'active', 'Over-the-counter retail transaction', 'Gujarat', 'split', 0, now() - interval '1 day', NULL, now() - interval '1 day', now() - interval '1 day', 1),
  (7, 11, 17, 'ORD-00107', 'INV-00107', CURRENT_DATE - 2, 'paid', 'dispatched', 'upi', 'active', 'Dispatched via Professional Courier', 'Gujarat', 'split', 0, now() - interval '2 days', NULL, now() - interval '2 days', now() - interval '2 days', 1),
  (8, 5, 8, 'ORD-00108', NULL, CURRENT_DATE - 2, 'pending', 'not_ready', NULL, 'active', 'Advance booking; invoice to be generated upon stock arrival', 'Maharashtra', 'igst', 0, NULL, NULL, now() - interval '2 days', now() - interval '2 days', 1),
  (9, 12, NULL, 'ORD-00109', NULL, CURRENT_DATE - 7, 'pending', 'not_ready', NULL, 'cancelled', 'Client cancelled order due to factory water-logging', 'Gujarat', 'split', 0, NULL, NULL, now() - interval '7 days', now() - interval '7 days', 1),
  (10, 4, 7, 'ORD-00110', 'INV-00110', CURRENT_DATE - 7, 'paid', 'delivered', 'bank_transfer', 'active', 'Original invoice with subsequent partial return', 'Gujarat', 'split', 0, now() - interval '7 days', NULL, now() - interval '7 days', now() - interval '7 days', 1),
  (11, 13, 18, 'AIP-0051', 'AIP-0051', CURRENT_DATE - 4, 'paid', 'delivered', 'bank_transfer', 'active', 'Industrial heavy fabrication tools order', 'Gujarat', 'split', 0, now() - interval '4 days', NULL, now() - interval '4 days', now() - interval '4 days', 2),
  (12, 14, NULL, 'AIP-0052', 'AIP-0052', CURRENT_DATE - 2, 'pending', 'ready', NULL, 'active', 'Precision drill set supply', 'Gujarat', 'split', 0, NULL, NULL, now() - interval '2 days', now() - interval '2 days', 2);

UPDATE quotations SET converted_order_id = 1, converted_at = now() - interval '8 days' WHERE id = 1;

INSERT INTO order_items (order_id, product_name, quantity, unit_price, gst_rate) VALUES
  (1, 'Cotton Fabric Cambric (100 Mtr)', 2, 4500, 5),
  (1, 'Cotton Yarn Spools (Pack of 12)', 4, 1450, 5),
  (2, '24" IPS Full HD LED Monitor', 3, 9800, 18),
  (2, 'Smart Digital Biometric Attendance Device', 1, 8900, 18),
  (3, 'Enterprise Billing Software Single-User License', 2, 12500, 18),
  (4, 'Ergonomic Executive Office Chair', 6, 5500, 12),
  (4, 'Solid Teak Wooden Workstation Desk', 2, 14000, 12),
  (5, 'Thermal Receipt Printer 80mm', 2, 4800, 12),
  (5, 'Industrial Barcode Scanner USB', 2, 3200, 12),
  (6, 'Cotton Fabric Cambric (100 Mtr)', 4, 4500, 5),
  (7, 'Commercial Wi-Fi 6 Mesh Router', 2, 7200, 18),
  (7, '4K Dome Security CCTV Camera', 4, 4200, 18),
  (8, 'Motorized Luxury Recliner Chair', 1, 32000, 28),
  (9, 'Raw Cotton Bales (100kg)', 5, 6200, 0),
  (10, 'Industrial Barcode Scanner USB', 3, 3200, 12),
  (11, 'Heavy Duty Angle Grinder 850W', 2, 2600, 18),
  (11, 'Industrial Safety Helmet Yellow', 10, 350, 18),
  (12, 'High-Speed Steel Drill Bit Set (25 Pcs)', 4, 1850, 18);

-- ── 9. Payments (Receipts & Ledger Entries) ──────────────────────────────────
INSERT INTO payments (
  id, order_id, contact_id, amount, method, payment_mode, receipt_number, reference_no, notes, paid_at, business_id
) VALUES
  (1, 1, 1, 15540, 'upi', 'upi', 'REC-0001', 'UPI/6209381029', 'Full payment for INV-00101', now() - interval '8 days', 1),
  (2, 2, 2, 20000, 'bank_transfer', 'bank_transfer', 'REC-0002', 'NEFT/HDFC/881920', 'Part payment towards INV-00102', now() - interval '6 days', 1),
  (3, 4, 3, 68320, 'bank_transfer', 'bank_transfer', 'REC-0003', 'RTGS/SBI/441029', 'Full payment for Mumbai branch office setup', now() - interval '5 days', 1),
  (4, 6, 10, 18900, 'cash', 'cash', 'REC-0004', 'CASH-COUNTER', 'Counter cash received for INV-00106', now() - interval '1 day', 1),
  (5, 7, 11, 36816, 'upi', 'upi', 'REC-0005', 'UPI/9918273645', 'Full payment for INV-00107', now() - interval '2 days', 1),
  (6, 10, 4, 10752, 'bank_transfer', 'bank_transfer', 'REC-0006', 'IMPS/772819', 'Full payment for INV-00110', now() - interval '7 days', 1),
  (7, NULL, 1, 10000, 'cheque', 'cheque', 'REC-0007', 'CHQ#665544', 'Payment on account towards opening balance', now() - interval '15 days', 1),
  (8, NULL, 4, 5000, 'upi', 'upi', 'REC-0008', 'UPI/44112233', 'Advance deposit for upcoming fabric orders', now() - interval '10 days', 1),
  (9, 11, 13, 10266, 'bank_transfer', 'bank_transfer', 'REC-AIP-01', 'NEFT/SBI/990011', 'Full payment for AIP-0051', now() - interval '4 days', 2);

-- ── 10. Credit Notes (Sales Returns) ────────────────────────────────────────
INSERT INTO credit_notes (
  id, credit_note_number, order_id, contact_id, credit_note_date,
  original_invoice_no, original_invoice_date, reason, notes,
  subtotal, tax_amount, total_amount, tax_mode, business_id
) VALUES
  (
    1, 'CN-0001', 10, 4, now() - interval '5 days',
    'INV-00110', CURRENT_DATE - 7,
    'Damaged / Defective Goods',
    '1 Barcode Scanner received with cracked lens during transit. Return approved and ledger credited.',
    3200, 384, 3584, 'split', 1
  ),
  (
    2, 'CN-0002', 1, 1, now() - interval '2 days',
    'INV-00101', CURRENT_DATE - 8,
    'Post-Sale Discount / Rate Difference',
    'Special quarterly volume rebate concession of ₹1,000 + GST granted.',
    1000, 50, 1050, 'split', 1
  );

INSERT INTO credit_note_items (
  credit_note_id, product_name, quantity, unit_price, gst_rate, hsn_code
) VALUES
  (1, 'Industrial Barcode Scanner USB', 1, 3200, 12, '8471'),
  (2, 'Cotton Fabric Cambric (100 Mtr)', 1, 1000, 5, '5208');

-- ── 11. Synchronize Sequences ───────────────────────────────────────────────
SELECT setval('users_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM users));
SELECT setval('businesses_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM businesses));
SELECT setval('business_users_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM business_users));
SELECT setval('products_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM products));
SELECT setval('contacts_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM contacts));
SELECT setval('calls_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM calls));
SELECT setval('followups_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM followups));
SELECT setval('quotations_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM quotations));
SELECT setval('quotation_items_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM quotation_items));
SELECT setval('orders_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM orders));
SELECT setval('order_items_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM order_items));
SELECT setval('payments_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM payments));
SELECT setval('credit_notes_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM credit_notes));
SELECT setval('credit_note_items_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM credit_note_items));
SELECT setval('push_subscriptions_id_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM push_subscriptions));
