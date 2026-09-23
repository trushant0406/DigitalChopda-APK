import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/products — the catalog for the active business
router.get('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const result = await pool.query('SELECT * FROM products WHERE business_id = $1 ORDER BY name ASC', [businessId]);
  res.json(result.rows);
});

// POST /api/products — add a new product to the catalog
router.post('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const { name, default_price, hsn_code, gst_rate } = req.body;
  if (!name || default_price === undefined || default_price === '') {
    return res.status(400).json({ error: 'name and default_price are required' });
  }
  try {
    const result = await pool.query(
      'INSERT INTO products (name, default_price, hsn_code, gst_rate, business_id) VALUES ($1, $2, $3, $4, $5) RETURNING *',
      [name, default_price, hsn_code || null, gst_rate || 18, businessId]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    if (err.code === '23505') return res.status(409).json({ error: 'That product already exists' });
    console.error(err);
    res.status(500).json({ error: 'Failed to create product' });
  }
});

// PATCH /api/products/:id — fix a typo or update the default price
router.patch('/:id', async (req, res) => {
  const businessId = req.businessId || 1;
  const { name, default_price, hsn_code, gst_rate } = req.body;
  const fields = [];
  const values = [];
  let i = 1;

  if (name) {
    fields.push(`name = $${i++}`);
    values.push(name);
  }
  if (default_price !== undefined && default_price !== '') {
    fields.push(`default_price = $${i++}`);
    values.push(default_price);
  }
  if (hsn_code !== undefined) {
    fields.push(`hsn_code = $${i++}`);
    values.push(hsn_code || null);
  }
  if (gst_rate !== undefined && gst_rate !== '') {
    fields.push(`gst_rate = $${i++}`);
    values.push(gst_rate);
  }
  if (fields.length === 0) return res.status(400).json({ error: 'Nothing to update' });

  values.push(req.params.id);
  values.push(businessId);
  try {
    const result = await pool.query(
      `UPDATE products SET ${fields.join(', ')} WHERE id = $${i++} AND business_id = $${i} RETURNING *`,
      values
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'Product not found' });
    res.json(result.rows[0]);
  } catch (err) {
    if (err.code === '23505') return res.status(409).json({ error: 'That product already exists' });
    console.error(err);
    res.status(500).json({ error: 'Failed to update product' });
  }
});

// DELETE /api/products/:id
router.delete('/:id', async (req, res) => {
  const businessId = req.businessId || 1;
  const result = await pool.query('DELETE FROM products WHERE id = $1 AND business_id = $2 RETURNING id', [req.params.id, businessId]);
  if (result.rows.length === 0) return res.status(404).json({ error: 'Product not found' });
  res.json({ ok: true });
});

export default router;
