import { Router } from 'express';
import { pool } from '../db.js';
import { findOrCreateContact } from '../lib/contacts.js';

const router = Router();

const ORDER_SELECT = `
  SELECT orders.*, contacts.name, contacts.phone, contacts.city,
         COALESCE(item_totals.total_amount, 0) AS total_amount,
         COALESCE(item_totals.tax_amount, 0) AS tax_amount,
         COALESCE(item_totals.items, '[]') AS items
  FROM orders
  JOIN contacts ON contacts.id = orders.contact_id
  LEFT JOIN (
    SELECT order_id,
           SUM(quantity * unit_price) AS total_amount,
           SUM(quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS tax_amount,
           json_agg(json_build_object('product_name', product_name, 'quantity', quantity, 'unit_price', unit_price, 'gst_rate', gst_rate)) AS items
    FROM order_items
    GROUP BY order_id
  ) item_totals ON item_totals.order_id = orders.id
`;

// POST /api/orders — create an order with one or more line items
router.post('/', async (req, res) => {
  const {
    phone, name, city, call_id, notes, items, payment_status, payment_method,
    gstin, pan, billing_address, shipping_address, state,
  } = req.body;

  if (!phone) return res.status(400).json({ error: 'phone is required' });
  if (!Array.isArray(items) || items.length === 0) {
    return res.status(400).json({ error: 'At least one item is required' });
  }
  for (const item of items) {
    if (!item.product_name || item.quantity === undefined || item.quantity === '' || Number(item.quantity) <= 0 || item.unit_price === undefined || item.unit_price === '') {
      return res.status(400).json({ error: 'Each item needs a valid product_name, a quantity greater than 0, and a unit_price' });
    }
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const contact = await findOrCreateContact(client, {
      phone, name, city, gstin, pan, billing_address, shipping_address, state,
    });

    const paidNow = payment_status === 'paid';
    const orderResult = await client.query(
      `INSERT INTO orders (contact_id, call_id, order_number, notes, payment_status, payment_method)
       VALUES ($1, $2, '', $3, $4, $5) RETURNING *`,
      [contact.id, call_id || null, notes || null, paidNow ? 'paid' : 'pending', payment_method || (paidNow ? 'other' : null)]
    );
    const order = orderResult.rows[0];

    const orderNumber = `ORD-${String(order.id).padStart(5, '0')}`;
    await client.query('UPDATE orders SET order_number = $1 WHERE id = $2', [orderNumber, order.id]);

    for (const item of items) {
      await client.query(
        `INSERT INTO order_items (order_id, product_name, quantity, unit_price, gst_rate)
         VALUES ($1, $2, $3, $4, $5)`,
        [order.id, item.product_name, item.quantity, item.unit_price, item.gst_rate || null]
      );
    }

    // Payment pending at creation → auto-remind the owner to collect it, same
    // way an unpaid follow-up would. Paid orders need no such reminder.
    if (!paidNow) {
      const dueAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
      await client.query(
        `INSERT INTO followups (contact_id, order_id, due_at, reason, channel)
         VALUES ($1, $2, $3, $4, 'push')`,
        [contact.id, order.id, dueAt, `Collect payment for order ${orderNumber}`]
      );
    }

    await client.query('COMMIT');

    const full = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [order.id]);
    res.status(201).json(full.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to create order' });
  } finally {
    client.release();
  }
});

// GET /api/orders — filterable by payment_status / fulfillment_status / status
router.get('/', async (req, res) => {
  const { payment_status, fulfillment_status, status } = req.query;
  const conditions = [];
  const values = [];
  let i = 1;

  if (payment_status) {
    conditions.push(`orders.payment_status = $${i++}`);
    values.push(payment_status);
  }
  if (fulfillment_status) {
    conditions.push(`orders.fulfillment_status = $${i++}`);
    values.push(fulfillment_status);
  }
  if (status) {
    conditions.push(`orders.status = $${i++}`);
    values.push(status);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await pool.query(
    `${ORDER_SELECT} ${where} ORDER BY orders.created_at DESC LIMIT 500`,
    values
  );
  res.json(result.rows);
});

// GET /api/orders/summary — counts + pending amount for stat cards
router.get('/summary', async (req, res) => {
  const [counts, pendingAmount] = await Promise.all([
    pool.query(`
      SELECT
        COUNT(*) FILTER (WHERE payment_status = 'pending') AS payment_pending,
        COUNT(*) FILTER (WHERE fulfillment_status = 'ready') AS ready_to_dispatch,
        COUNT(*) FILTER (WHERE fulfillment_status IN ('not_ready', 'ready')) AS not_dispatched,
        COUNT(*) FILTER (WHERE fulfillment_status = 'dispatched') AS dispatched,
        COUNT(*) FILTER (WHERE fulfillment_status = 'delivered') AS delivered,
        COUNT(*) AS total_orders
      FROM orders
      WHERE orders.status != 'cancelled'
    `),
    pool.query(`
      SELECT COALESCE(SUM(order_items.quantity * order_items.unit_price), 0) AS amount
      FROM orders JOIN order_items ON order_items.order_id = orders.id
      WHERE orders.payment_status = 'pending' AND orders.status != 'cancelled'
    `),
  ]);

  const row = counts.rows[0];
  res.json({
    paymentPending: Number(row.payment_pending),
    readyToDispatch: Number(row.ready_to_dispatch),
    notDispatched: Number(row.not_dispatched),
    dispatched: Number(row.dispatched),
    delivered: Number(row.delivered),
    totalOrders: Number(row.total_orders),
    pendingAmount: Number(pendingAmount.rows[0].amount),
  });
});

// GET /api/orders/:id
router.get('/:id', async (req, res) => {
  const result = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [req.params.id]);
  if (result.rows.length === 0) return res.status(404).json({ error: 'Order not found' });
  res.json(result.rows[0]);
});

// PATCH /api/orders/:id — update payment_status and/or fulfillment_status
router.patch('/:id', async (req, res) => {
  const { payment_status, payment_method, fulfillment_status, status, notes } = req.body;
  const fields = [];
  const values = [];
  let i = 1;

  if (payment_status) {
    fields.push(`payment_status = $${i++}`);
    values.push(payment_status);
  }
  if (payment_method !== undefined) {
    fields.push(`payment_method = $${i++}`);
    values.push(payment_method || null);
  }
  if (fulfillment_status) {
    fields.push(`fulfillment_status = $${i++}`);
    values.push(fulfillment_status);
  }
  if (status) {
    fields.push(`status = $${i++}`);
    values.push(status);
  }
  if (notes !== undefined) {
    fields.push(`notes = $${i++}`);
    values.push(notes);
  }
  if (fields.length === 0) return res.status(400).json({ error: 'Nothing to update' });
  fields.push('updated_at = now()');

  values.push(req.params.id);

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const result = await client.query(
      `UPDATE orders SET ${fields.join(', ')} WHERE id = $${i} RETURNING id`,
      values
    );
    if (result.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    // Paid, or cancelled outright → the "collect payment" reminder is no longer needed.
    if (payment_status === 'paid' || status === 'cancelled') {
      await client.query(
        `UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND status = 'pending'`,
        [req.params.id]
      );
    }

    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    return res.status(500).json({ error: 'Failed to update order' });
  } finally {
    client.release();
  }

  const full = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [req.params.id]);
  res.json(full.rows[0]);
});

export default router;
