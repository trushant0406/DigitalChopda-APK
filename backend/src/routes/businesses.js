import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/businesses — list businesses accessible to current user
router.get('/', async (req, res) => {
  try {
    const userId = req.user?.id;
    if (userId) {
      const result = await pool.query(
        `SELECT b.*, bu.role as user_role
         FROM businesses b
         JOIN business_users bu ON bu.business_id = b.id
         WHERE bu.user_id = $1 AND bu.is_active = true
         ORDER BY b.id ASC`,
        [userId]
      );
      return res.json(result.rows);
    }

    // Unauthenticated or system access: return all active businesses
    const result = await pool.query('SELECT * FROM businesses WHERE status = $1 ORDER BY id ASC', ['active']);
    res.json(result.rows);
  } catch (err) {
    console.error('Failed to list businesses:', err);
    res.status(500).json({ error: 'Failed to list businesses' });
  }
});

// GET /api/businesses/active — get current active business context
router.get('/active', async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const result = await pool.query('SELECT * FROM businesses WHERE id = $1', [businessId]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Active business not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Failed to get active business:', err);
    res.status(500).json({ error: 'Failed to get active business' });
  }
});

// GET /api/businesses/:id — get a specific business
router.get('/:id', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM businesses WHERE id = $1', [req.params.id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Business not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Failed to get business:', err);
    res.status(500).json({ error: 'Failed to get business' });
  }
});

// POST /api/businesses — Onboard a new business (restricted to system administrators)
router.post('/', async (req, res) => {
  try {
    if (req.user && req.user.role !== 'admin') {
      return res.status(403).json({ error: 'Access denied: Only system administrators can onboard new businesses.' });
    }
    const {
      name,
      trade_name,
      gstin,
      pan,
      mobile,
      email,
      address,
      state,
      bank_name,
      bank_account_no,
      bank_ifsc,
      bank_branch,
      terms_and_conditions,
      invoice_prefix,
      invoice_next_number,
      invoice_padding,
    } = req.body;

    if (!name || !name.trim()) {
      return res.status(400).json({ error: 'Business name is required' });
    }

    const prefix = (invoice_prefix || 'INV-').trim();
    const nextNumber = Math.max(1, parseInt(invoice_next_number, 10) || 1);
    const padding = Math.max(0, parseInt(invoice_padding, 10) || 0);

    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      const insertBiz = await client.query(
        `INSERT INTO businesses (
           name, trade_name, gstin, pan, mobile, email, address, state,
           bank_name, bank_account_no, bank_ifsc, bank_branch,
           terms_and_conditions, invoice_prefix, invoice_next_number, invoice_padding,
           created_at, updated_at
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, now(), now())
         RETURNING *`,
        [
          name.trim(),
          trade_name ? trade_name.trim() : null,
          gstin ? gstin.trim().toUpperCase() : null,
          pan ? pan.trim().toUpperCase() : null,
          mobile ? mobile.trim() : null,
          email ? email.trim().toLowerCase() : null,
          address ? address.trim() : null,
          state ? state.trim() : null,
          bank_name ? bank_name.trim() : null,
          bank_account_no ? bank_account_no.trim() : null,
          bank_ifsc ? bank_ifsc.trim().toUpperCase() : null,
          bank_branch ? bank_branch.trim() : null,
          terms_and_conditions || 'Thanks for your business!',
          prefix,
          nextNumber,
          padding,
        ]
      );

      const newBusiness = insertBiz.rows[0];

      // Associate with logged-in user or default admin (user 1)
      const userId = req.user?.id || 1;
      await client.query(
        `INSERT INTO business_users (business_id, user_id, role, is_active, created_at)
         VALUES ($1, $2, 'owner', true, now())
         ON CONFLICT (business_id, user_id) DO NOTHING`,
        [newBusiness.id, userId]
      );

      await client.query('COMMIT');
      res.status(201).json(newBusiness);
    } catch (txErr) {
      await client.query('ROLLBACK');
      throw txErr;
    } finally {
      client.release();
    }
  } catch (err) {
    console.error('Failed to create business:', err);
    res.status(500).json({ error: err.message || 'Failed to create business' });
  }
});

// PUT /api/businesses/:id — update a business
router.put('/:id', async (req, res) => {
  try {
    const businessId = req.params.id;
    const existingRes = await pool.query('SELECT * FROM businesses WHERE id = $1', [businessId]);
    if (existingRes.rows.length === 0) {
      return res.status(404).json({ error: 'Business not found' });
    }
    const existing = existingRes.rows[0];

    const fields = [
      'name', 'trade_name', 'gstin', 'pan', 'mobile', 'email', 'address', 'state',
      'bank_name', 'bank_account_no', 'bank_ifsc', 'bank_branch',
      'terms_and_conditions', 'invoice_prefix', 'invoice_next_number', 'invoice_padding',
      'signature_image', 'logo_image'
    ];

    const updates = {};
    for (const f of fields) {
      if (req.body[f] !== undefined) updates[f] = req.body[f];
      else updates[f] = existing[f];
    }

    const result = await pool.query(
      `UPDATE businesses SET
         name = $1, trade_name = $2, gstin = $3, pan = $4, mobile = $5, email = $6,
         address = $7, state = $8, bank_name = $9, bank_account_no = $10,
         bank_ifsc = $11, bank_branch = $12, terms_and_conditions = $13,
         invoice_prefix = $14, invoice_next_number = $15, invoice_padding = $16,
         signature_image = $17, logo_image = $18, updated_at = now()
       WHERE id = $19
       RETURNING *`,
      [
        updates.name, updates.trade_name, updates.gstin, updates.pan, updates.mobile, updates.email,
        updates.address, updates.state, updates.bank_name, updates.bank_account_no,
        updates.bank_ifsc, updates.bank_branch, updates.terms_and_conditions,
        updates.invoice_prefix, updates.invoice_next_number, updates.invoice_padding,
        updates.signature_image, updates.logo_image,
        businessId
      ]
    );

    res.json(result.rows[0]);
  } catch (err) {
    console.error('Failed to update business:', err);
    res.status(500).json({ error: 'Failed to update business' });
  }
});

export default router;
