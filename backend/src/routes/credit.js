import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/credit/summary — totals for the khata stat cards
router.get('/summary', async (req, res) => {
  const result = await pool.query(`
    SELECT
      COALESCE(SUM(item_totals.total_amount), 0) AS total_credit_amount,
      COALESCE(SUM(payment_totals.collected), 0) AS total_collected
    FROM orders
    JOIN (
      SELECT order_id, SUM(quantity * unit_price) AS total_amount
      FROM order_items GROUP BY order_id
    ) item_totals ON item_totals.order_id = orders.id
    LEFT JOIN (
      SELECT order_id, SUM(amount) AS collected FROM payments GROUP BY order_id
    ) payment_totals ON payment_totals.order_id = orders.id
    WHERE orders.payment_method = 'credit' AND orders.status != 'cancelled'
  `);
  const row = result.rows[0];
  const totalCreditAmount = Number(row.total_credit_amount);
  const totalCollected = Number(row.total_collected);
  res.json({
    totalCreditAmount,
    totalCollected,
    totalOutstanding: totalCreditAmount - totalCollected,
  });
});

// GET /api/credit/customers — every customer with a credit limit and/or credit orders
// ?q= filters by name/phone; ?status= one of outstanding|over_limit|settled
router.get('/customers', async (req, res) => {
  const { q, status } = req.query;
  const conditions = [];
  const values = [];
  let i = 1;

  if (q) {
    conditions.push(`(khata.name ILIKE $${i} OR khata.phone ILIKE $${i})`);
    values.push(`%${q}%`);
    i += 1;
  }
  if (status === 'outstanding') {
    conditions.push('khata.outstanding > 0');
  } else if (status === 'settled') {
    conditions.push('khata.outstanding <= 0');
  } else if (status === 'over_limit') {
    conditions.push('khata.credit_limit > 0 AND khata.outstanding > khata.credit_limit');
  }
  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';

  const result = await pool.query(
    `SELECT * FROM (
       SELECT contacts.id, contacts.name, contacts.phone, contacts.credit_limit,
              COALESCE(order_totals.total_credit_amount, 0) AS total_credit_amount,
              COALESCE(order_totals.collected, 0) AS collected,
              COALESCE(order_totals.total_credit_amount, 0) - COALESCE(order_totals.collected, 0) AS outstanding
       FROM contacts
       LEFT JOIN (
         SELECT orders.contact_id,
                SUM(item_totals.total_amount) AS total_credit_amount,
                SUM(COALESCE(payment_totals.collected, 0)) AS collected
         FROM orders
         JOIN (
           SELECT order_id, SUM(quantity * unit_price) AS total_amount
           FROM order_items GROUP BY order_id
         ) item_totals ON item_totals.order_id = orders.id
         LEFT JOIN (
           SELECT order_id, SUM(amount) AS collected FROM payments GROUP BY order_id
         ) payment_totals ON payment_totals.order_id = orders.id
         WHERE orders.payment_method = 'credit' AND orders.status != 'cancelled'
         GROUP BY orders.contact_id
       ) order_totals ON order_totals.contact_id = contacts.id
       WHERE contacts.credit_limit > 0 OR order_totals.total_credit_amount > 0
     ) khata
     ${where}
     ORDER BY khata.outstanding DESC NULLS LAST`,
    values
  );
  res.json(result.rows);
});

// GET /api/credit/customers/:id/orders — one customer's credit orders + running collection
router.get('/customers/:id/orders', async (req, res) => {
  const result = await pool.query(
    `SELECT orders.*,
            COALESCE(item_totals.total_amount, 0) AS total_amount,
            COALESCE(payment_totals.collected, 0) AS collected,
            COALESCE(item_totals.total_amount, 0) - COALESCE(payment_totals.collected, 0) AS outstanding,
            COALESCE(item_totals.items, '[]') AS items
     FROM orders
     LEFT JOIN (
       SELECT order_id, SUM(quantity * unit_price) AS total_amount,
              json_agg(json_build_object('product_name', product_name, 'quantity', quantity, 'unit_price', unit_price)) AS items
       FROM order_items GROUP BY order_id
     ) item_totals ON item_totals.order_id = orders.id
     LEFT JOIN (
       SELECT order_id, SUM(amount) AS collected FROM payments GROUP BY order_id
     ) payment_totals ON payment_totals.order_id = orders.id
     WHERE orders.contact_id = $1 AND orders.payment_method = 'credit'
     ORDER BY orders.created_at DESC`,
    [req.params.id]
  );
  res.json(result.rows);
});

// POST /api/credit/collect — record a khata collection against one order
router.post('/collect', async (req, res) => {
  const { order_id, amount, method, notes } = req.body;
  if (!order_id || !amount || Number(amount) <= 0) {
    return res.status(400).json({ error: 'order_id and a positive amount are required' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const orderResult = await client.query('SELECT * FROM orders WHERE id = $1', [order_id]);
    if (orderResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    await client.query(
      `INSERT INTO payments (order_id, amount, method, notes) VALUES ($1, $2, $3, $4)`,
      [order_id, amount, method || 'cash', notes || null]
    );

    const totals = await client.query(
      `SELECT
         COALESCE((SELECT SUM(quantity * unit_price) FROM order_items WHERE order_id = $1), 0) AS total,
         COALESCE((SELECT SUM(amount) FROM payments WHERE order_id = $1), 0) AS collected`,
      [order_id]
    );
    const total = Number(totals.rows[0].total);
    const collected = Number(totals.rows[0].collected);

    if (collected >= total) {
      await client.query(`UPDATE orders SET payment_status = 'paid', updated_at = now() WHERE id = $1`, [order_id]);
      await client.query(
        `UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND status = 'pending'`,
        [order_id]
      );
    }

    await client.query('COMMIT');
    res.status(201).json({ total, collected, outstanding: Math.max(total - collected, 0) });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to record payment' });
  } finally {
    client.release();
  }
});

export default router;
