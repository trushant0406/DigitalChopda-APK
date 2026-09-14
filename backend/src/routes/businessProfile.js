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
  const existingRes = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  const existing = existingRes.rows[0] || {};

  const name = req.body.name !== undefined ? req.body.name : (existing.name || null);
  const address = req.body.address !== undefined ? req.body.address : (existing.address || null);
  const gstin = req.body.gstin !== undefined ? req.body.gstin : (existing.gstin || null);
  const pan = req.body.pan !== undefined ? req.body.pan : (existing.pan || null);
  const mobile = req.body.mobile !== undefined ? req.body.mobile : (existing.mobile || null);
  const state = req.body.state !== undefined ? req.body.state : (existing.state || null);
  const bank_name = req.body.bank_name !== undefined ? req.body.bank_name : (existing.bank_name || null);
  const bank_account_no = req.body.bank_account_no !== undefined ? req.body.bank_account_no : (existing.bank_account_no || null);
  const bank_ifsc = req.body.bank_ifsc !== undefined ? req.body.bank_ifsc : (existing.bank_ifsc || null);
  const bank_branch = req.body.bank_branch !== undefined ? req.body.bank_branch : (existing.bank_branch || null);
  const terms_and_conditions = req.body.terms_and_conditions !== undefined ? req.body.terms_and_conditions : (existing.terms_and_conditions || null);
  const signature_image = req.body.signature_image !== undefined ? req.body.signature_image : (existing.signature_image || null);

  const prefix = req.body.invoice_prefix !== undefined ? (req.body.invoice_prefix || 'INV-').trim() : (existing.invoice_prefix || 'INV-');
  const nextNumber = req.body.invoice_next_number !== undefined ? Math.max(1, parseInt(req.body.invoice_next_number, 10) || 1) : (existing.invoice_next_number || 1);
  const padding = req.body.invoice_padding !== undefined ? Math.max(0, parseInt(req.body.invoice_padding, 10) || 0) : (existing.invoice_padding || 0);

  const result = await pool.query(
    `INSERT INTO business_profile
       (id, name, address, gstin, pan, mobile, state, bank_name, bank_account_no, bank_ifsc, bank_branch, terms_and_conditions, signature_image, invoice_prefix, invoice_next_number, invoice_padding, updated_at)
     VALUES (1, $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, now())
     ON CONFLICT (id) DO UPDATE SET
       name = COALESCE($1, business_profile.name),
       address = COALESCE($2, business_profile.address),
       gstin = COALESCE($3, business_profile.gstin),
       pan = COALESCE($4, business_profile.pan),
       mobile = COALESCE($5, business_profile.mobile),
       state = COALESCE($6, business_profile.state),
       bank_name = COALESCE($7, business_profile.bank_name),
       bank_account_no = COALESCE($8, business_profile.bank_account_no),
       bank_ifsc = COALESCE($9, business_profile.bank_ifsc),
       bank_branch = COALESCE($10, business_profile.bank_branch),
       terms_and_conditions = COALESCE($11, business_profile.terms_and_conditions),
       signature_image = COALESCE($12, business_profile.signature_image),
       invoice_prefix = $13,
       invoice_next_number = $14,
       invoice_padding = $15,
       updated_at = now()
     RETURNING *`,
    [
      name, address, gstin, pan, mobile, state,
      bank_name, bank_account_no, bank_ifsc, bank_branch,
      terms_and_conditions, signature_image, prefix, nextNumber, padding,
    ]
  );
  res.json(result.rows[0]);
});

export default router;
