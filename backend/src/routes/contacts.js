import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/contacts — list all contacts, optional ?q= search on name/phone
router.get('/', async (req, res) => {
  const { q } = req.query;
  const params = [];
  let where = '';
  if (q) {
    params.push(`%${q}%`);
    where = `WHERE contacts.name ILIKE $1 OR contacts.phone ILIKE $1`;
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
     LEFT JOIN calls ON calls.contact_id = contacts.id
     LEFT JOIN orders ON orders.contact_id = contacts.id
     LEFT JOIN (
       SELECT orders.contact_id, SUM(order_items.quantity * order_items.unit_price) AS total_spent
       FROM orders JOIN order_items ON order_items.order_id = orders.id
       WHERE orders.status != 'cancelled'
       GROUP BY orders.contact_id
     ) order_totals ON order_totals.contact_id = contacts.id
     ${where}
     GROUP BY contacts.id, order_totals.total_spent
     ORDER BY last_call_at DESC NULLS LAST`,
    params
  );
  res.json(result.rows);
});

// GET /api/contacts/:id — contact detail with full call + follow-up + order timeline
router.get('/:id', async (req, res) => {
  const { id } = req.params;
  const contact = await pool.query('SELECT * FROM contacts WHERE id = $1', [id]);
  if (contact.rows.length === 0) return res.status(404).json({ error: 'Contact not found' });

  const [calls, followups, orders, topProducts, outstanding] = await Promise.all([
    pool.query('SELECT * FROM calls WHERE contact_id = $1 ORDER BY started_at DESC', [id]),
    pool.query('SELECT * FROM followups WHERE contact_id = $1 ORDER BY due_at DESC', [id]),
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
       WHERE orders.contact_id = $1
       GROUP BY orders.id
       ORDER BY orders.created_at DESC`,
      [id]
    ),
    pool.query(
      `SELECT order_items.product_name,
              SUM(order_items.quantity) AS total_quantity,
              SUM(order_items.quantity * order_items.unit_price) AS total_amount
       FROM order_items
       JOIN orders ON orders.id = order_items.order_id
       WHERE orders.contact_id = $1 AND orders.status != 'cancelled'
       GROUP BY order_items.product_name
       ORDER BY total_amount DESC
       LIMIT 5`,
      [id]
    ),
    pool.query(
      `SELECT COALESCE(SUM(order_items.quantity * order_items.unit_price), 0) AS amount
       FROM orders JOIN order_items ON order_items.order_id = orders.id
       WHERE orders.contact_id = $1 AND orders.payment_status = 'pending' AND orders.status != 'cancelled'`,
      [id]
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

// PATCH /api/contacts/:id — edit GST/PAN/address details (or name/city)
router.patch('/:id', async (req, res) => {
  const { name, city, gstin, pan, billing_address, shipping_address, state } = req.body;
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

  if (fields.length === 0) return res.status(400).json({ error: 'Nothing to update' });

  values.push(req.params.id);
  const result = await pool.query(
    `UPDATE contacts SET ${fields.join(', ')} WHERE id = $${i} RETURNING *`,
    values
  );
  if (result.rows.length === 0) return res.status(404).json({ error: 'Contact not found' });
  res.json(result.rows[0]);
});

export default router;
