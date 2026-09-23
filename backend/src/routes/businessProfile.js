import { Router } from 'express';
import { pool } from '../db.js';
import { storageService } from '../storage/storageService.js';
import { sanitizeStorageSlug } from '../storage/storageKeyHelper.js';

const router = Router();

// GET /api/business-profile — current business details for active tenant
router.get('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const result = await pool.query('SELECT * FROM businesses WHERE id = $1', [businessId]);
  if (result.rows.length > 0) {
    return res.json(result.rows[0]);
  }
  // Fallback to legacy single-row business_profile if businesses row not found
  const legacy = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  res.json(legacy.rows[0] || null);
});

// PUT /api/business-profile — create or update active business profile
router.put('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const existingRes = await pool.query('SELECT * FROM businesses WHERE id = $1', [businessId]);
  const existing = existingRes.rows[0] || {};

  const name = req.body.name !== undefined ? req.body.name : (existing.name || 'My Business');
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
  let signature_image = req.body.signature_image !== undefined ? req.body.signature_image : (existing.signature_image || null);

  if (signature_image && typeof signature_image === 'string' && signature_image.startsWith('data:image/')) {
    try {
      const matches = signature_image.match(/^data:([A-Za-z-+\/]+);base64,(.+)$/);
      if (matches && matches.length === 3) {
        const mimeType = matches[1];
        const buffer = Buffer.from(matches[2], 'base64');
        const ext = mimeType.split('/')[1]?.replace('+xml', '') || 'png';
        const bizSlug = sanitizeStorageSlug(existing.name || 'Business', `Business_${businessId}`);
        const key = `tenants/${bizSlug}/branding/signature_${Date.now()}.${ext}`;
        const uploadResult = await storageService.uploadFile({
          key,
          buffer,
          contentType: mimeType,
          isPublic: true,
        });
        signature_image = uploadResult.url;
      }
    } catch (uploadErr) {
      console.warn('[BusinessProfile] Signature upload to storage failed, preserving base64:', uploadErr.message);
    }
  }

  const prefix = req.body.invoice_prefix !== undefined ? (req.body.invoice_prefix || 'INV-').trim() : (existing.invoice_prefix || 'INV-');
  const nextNumber = req.body.invoice_next_number !== undefined ? Math.max(1, parseInt(req.body.invoice_next_number, 10) || 1) : (existing.invoice_next_number || 1);
  const padding = req.body.invoice_padding !== undefined ? Math.max(0, parseInt(req.body.invoice_padding, 10) || 0) : (existing.invoice_padding || 0);

  const result = await pool.query(
    `INSERT INTO businesses
       (id, name, address, gstin, pan, mobile, state, bank_name, bank_account_no, bank_ifsc, bank_branch, terms_and_conditions, signature_image, invoice_prefix, invoice_next_number, invoice_padding, updated_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, now())
     ON CONFLICT (id) DO UPDATE SET
       name = COALESCE($2, businesses.name),
       address = COALESCE($3, businesses.address),
       gstin = COALESCE($4, businesses.gstin),
       pan = COALESCE($5, businesses.pan),
       mobile = COALESCE($6, businesses.mobile),
       state = COALESCE($7, businesses.state),
       bank_name = COALESCE($8, businesses.bank_name),
       bank_account_no = COALESCE($9, businesses.bank_account_no),
       bank_ifsc = COALESCE($10, businesses.bank_ifsc),
       bank_branch = COALESCE($11, businesses.bank_branch),
       terms_and_conditions = COALESCE($12, businesses.terms_and_conditions),
       signature_image = COALESCE($13, businesses.signature_image),
       invoice_prefix = $14,
       invoice_next_number = $15,
       invoice_padding = $16,
       updated_at = now()
     RETURNING *`,
    [
      businessId, name, address, gstin, pan, mobile, state,
      bank_name, bank_account_no, bank_ifsc, bank_branch,
      terms_and_conditions, signature_image, prefix, nextNumber, padding,
    ]
  );

  // Sync to legacy business_profile table if id=1 for backward compatibility
  if (businessId === 1) {
    try {
      await pool.query(
        `INSERT INTO business_profile (id, name, address, gstin, pan, mobile, state, bank_name, bank_account_no, bank_ifsc, bank_branch, terms_and_conditions, signature_image, invoice_prefix, invoice_next_number, invoice_padding, updated_at)
         VALUES (1, $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, now())
         ON CONFLICT (id) DO UPDATE SET
           name = EXCLUDED.name, address = EXCLUDED.address, gstin = EXCLUDED.gstin, pan = EXCLUDED.pan,
           mobile = EXCLUDED.mobile, state = EXCLUDED.state, bank_name = EXCLUDED.bank_name,
           bank_account_no = EXCLUDED.bank_account_no, bank_ifsc = EXCLUDED.bank_ifsc,
           bank_branch = EXCLUDED.bank_branch, terms_and_conditions = EXCLUDED.terms_and_conditions,
           signature_image = EXCLUDED.signature_image, invoice_prefix = EXCLUDED.invoice_prefix,
           invoice_next_number = EXCLUDED.invoice_next_number, invoice_padding = EXCLUDED.invoice_padding,
           updated_at = now()`,
        [
          name, address, gstin, pan, mobile, state,
          bank_name, bank_account_no, bank_ifsc, bank_branch,
          terms_and_conditions, signature_image, prefix, nextNumber, padding
        ]
      );
    } catch (e) {
      // Ignore legacy sync failures
    }
  }

  res.json(result.rows[0]);
});

export default router;
