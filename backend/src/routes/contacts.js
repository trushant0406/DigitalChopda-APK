import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/contacts — list all contacts for active business, optional ?q= search on name/phone
router.get('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const { q } = req.query;
  const params = [businessId];
  let where = 'WHERE contacts.business_id = $1';
  if (q) {
    params.push(`%${q}%`);
    where += ` AND (contacts.name ILIKE $2 OR contacts.phone ILIKE $2)`;
  }

  const result = await pool.query(
    `SELECT contacts.*,
            COUNT(DISTINCT calls.id) AS call_count,
            MAX(calls.started_at) AS last_call_at,
            COUNT(DISTINCT orders.id) FILTER (WHERE orders.status != 'cancelled') AS order_count,
            COALESCE(order_totals.total_spent, 0) AS total_spent,
            COALESCE(bool_or(orders.payment_status = 'paid' AND orders.status != 'cancelled'), false) AS has_paid_order,
            COALESCE(bool_or(orders.fulfillment_status = 'dispatched' AND orders.status != 'cancelled'), false) AS has_dispatched_order,
            COALESCE(bool_or(orders.fulfillment_status = 'delivered' AND orders.status != 'cancelled'), false) AS has_delivered_order
     FROM contacts
     LEFT JOIN calls ON calls.contact_id = contacts.id AND calls.business_id = contacts.business_id
     LEFT JOIN orders ON orders.contact_id = contacts.id AND orders.business_id = contacts.business_id
     LEFT JOIN (
       SELECT orders.contact_id, SUM(order_items.quantity * order_items.unit_price) AS total_spent
       FROM orders JOIN order_items ON order_items.order_id = orders.id
       WHERE orders.status != 'cancelled' AND orders.business_id = $1
       GROUP BY orders.contact_id
     ) order_totals ON order_totals.contact_id = contacts.id
     ${where}
     GROUP BY contacts.id, order_totals.total_spent
     ORDER BY last_call_at DESC NULLS LAST`,
    params
  );
  res.json(result.rows);
});

// POST /api/contacts — create a new contact
router.post('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const {
    phone, name, city, gstin, pan,
    billing_address, shipping_address, state,
    credit_limit, opening_balance, opening_balance_type, opening_balance_date
  } = req.body;

  if (!phone) return res.status(400).json({ error: 'Phone is required' });
  const cleanPhone = phone.replace(/\D/g, '');

  const result = await pool.query(
    `INSERT INTO contacts (
       phone, name, city, gstin, pan, billing_address, shipping_address, state,
       credit_limit, opening_balance, opening_balance_type, opening_balance_date, business_id
     )
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
     ON CONFLICT (business_id, phone) DO UPDATE SET
       name = COALESCE(EXCLUDED.name, contacts.name),
       city = COALESCE(EXCLUDED.city, contacts.city),
       gstin = COALESCE(EXCLUDED.gstin, contacts.gstin),
       pan = COALESCE(EXCLUDED.pan, contacts.pan),
       billing_address = COALESCE(EXCLUDED.billing_address, contacts.billing_address),
       shipping_address = COALESCE(EXCLUDED.shipping_address, contacts.shipping_address),
       state = COALESCE(EXCLUDED.state, contacts.state)
     RETURNING *`,
    [
      cleanPhone, name || null, city || null, gstin || null, pan || null,
      billing_address || null, shipping_address || null, state || null,
      credit_limit || 0, opening_balance || 0, opening_balance_type || 'to_receive',
      opening_balance_date || null, businessId
    ]
  );
  res.status(201).json(result.rows[0]);
});

// GET /api/contacts/:id — contact detail with full call + follow-up + order timeline
router.get('/:id', async (req, res) => {
  const businessId = req.businessId || 1;
  const { id } = req.params;
  const contact = await pool.query('SELECT * FROM contacts WHERE id = $1 AND business_id = $2', [id, businessId]);
  if (contact.rows.length === 0) return res.status(404).json({ error: 'Contact not found' });

  const [calls, followups, orders, topProducts, outstanding] = await Promise.all([
    pool.query('SELECT * FROM calls WHERE contact_id = $1 AND business_id = $2 ORDER BY started_at DESC', [id, businessId]),
    pool.query('SELECT * FROM followups WHERE contact_id = $1 AND business_id = $2 ORDER BY due_at DESC', [id, businessId]),
    pool.query(
      `SELECT orders.*,
              COALESCE(SUM(order_items.quantity * order_items.unit_price), 0) AS total_amount,
              COALESCE(SUM(order_items.quantity * order_items.unit_price * COALESCE(order_items.gst_rate, 0) / 100), 0) AS tax_amount,
              COALESCE(
                json_agg(json_build_object('product_name', order_items.product_name, 'quantity', order_items.quantity, 'unit_price', order_items.unit_price, 'gst_rate', order_items.gst_rate))
                FILTER (WHERE order_items.id IS NOT NULL),
                '[]'
              ) AS items
       FROM orders
       LEFT JOIN order_items ON order_items.order_id = orders.id
       WHERE orders.contact_id = $1 AND orders.business_id = $2
       GROUP BY orders.id
       ORDER BY orders.created_at DESC`,
      [id, businessId]
    ),
    pool.query(
      `SELECT order_items.product_name,
              SUM(order_items.quantity) AS total_quantity,
              SUM(order_items.quantity * order_items.unit_price) AS total_amount
       FROM order_items
       JOIN orders ON orders.id = order_items.order_id
       WHERE orders.contact_id = $1 AND orders.business_id = $2 AND orders.status != 'cancelled'
       GROUP BY order_items.product_name
       ORDER BY total_amount DESC
       LIMIT 5`,
      [id, businessId]
    ),
    pool.query(
      `SELECT COALESCE(SUM(order_items.quantity * order_items.unit_price), 0) AS amount
       FROM orders JOIN order_items ON order_items.order_id = orders.id
       WHERE orders.contact_id = $1 AND orders.business_id = $2 AND orders.payment_status = 'pending' AND orders.status != 'cancelled'`,
      [id, businessId]
    ),
  ]);

  res.json({
    contact: contact.rows[0],
    calls: calls.rows,
    followups: followups.rows,
    orders: orders.rows,
    topProducts: topProducts.rows,
    outstandingAmount: Number(outstanding.rows[0].amount),
  });
});

// PATCH /api/contacts/:id — edit details
router.patch('/:id', async (req, res) => {
  const businessId = req.businessId || 1;
  const { name, city, gstin, pan, billing_address, shipping_address, state, credit_limit } = req.body;
  const fields = [];
  const values = [];
  let i = 1;

  const set = (column, value) => {
    if (value !== undefined) {
      fields.push(`${column} = $${i++}`);
      values.push(value || null);
    }
  };
  set('name', name);
  set('city', city);
  set('gstin', gstin);
  set('pan', pan);
  set('billing_address', billing_address);
  set('shipping_address', shipping_address);
  set('state', state);
  if (credit_limit !== undefined) {
    fields.push(`credit_limit = $${i++}`);
    values.push(credit_limit === '' ? 0 : Number(credit_limit));
  }
  if (req.body.opening_balance !== undefined) {
    fields.push(`opening_balance = $${i++}`);
    values.push(req.body.opening_balance === '' ? 0 : Number(req.body.opening_balance));
  }
  if (req.body.opening_balance_type !== undefined) {
    fields.push(`opening_balance_type = $${i++}`);
    values.push(req.body.opening_balance_type || 'to_receive');
  }
  if (req.body.opening_balance_date !== undefined) {
    fields.push(`opening_balance_date = $${i++}`);
    values.push(req.body.opening_balance_date || null);
  }

  if (fields.length === 0) return res.status(400).json({ error: 'Nothing to update' });

  values.push(req.params.id);
  values.push(businessId);
  const result = await pool.query(
    `UPDATE contacts SET ${fields.join(', ')} WHERE id = $${i++} AND business_id = $${i} RETURNING *`,
    values
  );
  if (result.rows.length === 0) return res.status(404).json({ error: 'Contact not found' });
  res.json(result.rows[0]);
});

export default router;
