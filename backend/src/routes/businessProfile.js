import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/business-profile — your own business details (single row)
router.get('/', async (req, res) => {
  const result = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  res.json(result.rows[0] || null);
});

// PUT /api/business-profile — create or update it (upsert, always row id=1)
router.put('/', async (req, res) => {
  const {
    name, address, gstin, pan, mobile, state,
    bank_name, bank_account_no, bank_ifsc, bank_branch, terms_and_conditions,
    signature_image,
  } = req.body;

  const result = await pool.query(
    `INSERT INTO business_profile
       (id, name, address, gstin, pan, mobile, state, bank_name, bank_account_no, bank_ifsc, bank_branch, terms_and_conditions, signature_image, updated_at)
     VALUES (1, $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, now())
     ON CONFLICT (id) DO UPDATE SET
       name = $1, address = $2, gstin = $3, pan = $4, mobile = $5, state = $6,
       bank_name = $7, bank_account_no = $8, bank_ifsc = $9, bank_branch = $10,
       terms_and_conditions = $11, signature_image = $12, updated_at = now()
     RETURNING *`,
    [
      name || null, address || null, gstin || null, pan || null, mobile || null, state || null,
      bank_name || null, bank_account_no || null, bank_ifsc || null, bank_branch || null,
      terms_and_conditions || null, signature_image || null,
    ]
  );
  res.json(result.rows[0]);
});

export default router;
