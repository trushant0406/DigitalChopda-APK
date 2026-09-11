import { Router } from 'express';
import { pool } from '../db.js';
import { findOrCreateContact } from '../lib/contacts.js';
import { buildQuotationHtml } from '../pdf/quotationHtml.js';
import { renderHtmlToPdf } from '../pdf/renderPdf.js';
import { sendWhatsAppDocument } from '../notify/whatsapp.js';

const router = Router();

const QUOTATION_SELECT = `
  SELECT quotations.*,
         COALESCE(item_totals.subtotal, 0) AS subtotal,
         COALESCE(item_totals.tax_amount, 0) AS tax_amount,
         COALESCE(item_totals.total_amount, 0) AS total_amount,
         COALESCE(item_totals.items, '[]') AS items
  FROM quotations
  LEFT JOIN (
    SELECT quotation_id,
           SUM(quantity * rate) AS subtotal,
           SUM(quantity * rate * gst_rate / 100) AS tax_amount,
           SUM(quantity * rate * (1 + gst_rate / 100)) AS total_amount,
           json_agg(json_build_object(
             'product_name', product_name, 'hsn_code', hsn_code, 'unit', unit,
             'quantity', quantity, 'rate', rate, 'gst_rate', gst_rate
           )) AS items
    FROM quotation_items
    GROUP BY quotation_id
  ) item_totals ON item_totals.quotation_id = quotations.id
`;

async function loadFullQuotation(id) {
  const result = await pool.query(`${QUOTATION_SELECT} WHERE quotations.id = $1`, [id]);
  return result.rows[0] || null;
}

async function loadBusinessProfile() {
  const result = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  return result.rows[0] || null;
}

// POST /api/quotations — create a quotation for a contact (existing or new)
router.post('/', async (req, res) => {
  const {
    contact_id, phone, name, city,
    gstin, pan, billing_address, shipping_address, state,
    ship_to_name, ship_to_address,
    items, terms_and_conditions,
  } = req.body;

  if (!contact_id && !phone) return res.status(400).json({ error: 'contact_id or phone is required' });
  if (!Array.isArray(items) || items.length === 0) {
    return res.status(400).json({ error: 'At least one item is required' });
  }
  for (const item of items) {
    if (!item.product_name || !item.quantity || item.rate === undefined || item.rate === '') {
      return res.status(400).json({ error: 'Each item needs product_name, quantity, and rate' });
    }
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    let contact;
    if (contact_id) {
      const existing = await client.query('SELECT * FROM contacts WHERE id = $1', [contact_id]);
      if (existing.rows.length === 0) {
        await client.query('ROLLBACK');
        return res.status(404).json({ error: 'Contact not found' });
      }
      if (gstin || pan || billing_address || shipping_address || state || name || city) {
        const updated = await client.query(
          `UPDATE contacts SET
             name = COALESCE($1, name), city = COALESCE($2, city),
             gstin = COALESCE($3, gstin), pan = COALESCE($4, pan),
             billing_address = COALESCE($5, billing_address),
             shipping_address = COALESCE($6, shipping_address),
             state = COALESCE($7, state)
           WHERE id = $8 RETURNING *`,
          [name || null, city || null, gstin || null, pan || null, billing_address || null, shipping_address || null, state || null, contact_id]
        );
        contact = updated.rows[0];
      } else {
        contact = existing.rows[0];
      }
    } else {
      contact = await findOrCreateContact(client, { phone, name, city, gstin, pan, billing_address, shipping_address, state });
    }

    const business = await loadBusinessProfile();
    const custState = state || contact.state || '';
    const taxMode = business?.state && custState && business.state.trim().toLowerCase() === custState.trim().toLowerCase()
      ? 'split'
      : 'igst';

    const billToAddress = billing_address || contact.billing_address || null;
    const quotationResult = await client.query(
      `INSERT INTO quotations
         (contact_id, bill_to_name, bill_to_address, bill_to_mobile, bill_to_gstin, bill_to_pan,
          ship_to_name, ship_to_address, place_of_supply, tax_mode, terms_and_conditions)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
       RETURNING *`,
      [
        contact.id,
        name || contact.name || contact.phone,
        billToAddress,
        contact.phone,
        gstin || contact.gstin || null,
        pan || contact.pan || null,
        ship_to_name || name || contact.name || contact.phone,
        ship_to_address || shipping_address || contact.shipping_address || billToAddress,
        custState || null,
        taxMode,
        terms_and_conditions || business?.terms_and_conditions || 'Thanks',
      ]
    );
    const quotation = quotationResult.rows[0];

    for (const item of items) {
      await client.query(
        `INSERT INTO quotation_items (quotation_id, product_name, hsn_code, unit, quantity, rate, gst_rate)
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [quotation.id, item.product_name, item.hsn_code || null, item.unit || 'PCS', item.quantity, item.rate, item.gst_rate ?? 18]
      );
    }

    await client.query('COMMIT');
    res.status(201).json(await loadFullQuotation(quotation.id));
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to create quotation' });
  } finally {
    client.release();
  }
});

// GET /api/quotations — list, most recent first
router.get('/', async (req, res) => {
  const result = await pool.query(`${QUOTATION_SELECT} ORDER BY quotations.created_at DESC LIMIT 500`);
  res.json(result.rows);
});

// GET /api/quotations/:id
router.get('/:id', async (req, res) => {
  const quotation = await loadFullQuotation(req.params.id);
  if (!quotation) return res.status(404).json({ error: 'Quotation not found' });
  res.json(quotation);
});

// GET /api/quotations/:id/pdf — stream the generated PDF
router.get('/:id/pdf', async (req, res) => {
  try {
    const quotation = await loadFullQuotation(req.params.id);
    if (!quotation) return res.status(404).json({ error: 'Quotation not found' });
    const business = await loadBusinessProfile();

    const html = buildQuotationHtml({ business, quotation, items: quotation.items });
    const pdf = await renderHtmlToPdf(html);

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="Quotation-${quotation.id}.pdf"`);
    res.send(pdf);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to generate PDF' });
  }
});

// POST /api/quotations/:id/send-whatsapp — generate the PDF and send it to the customer
router.post('/:id/send-whatsapp', async (req, res) => {
  try {
    const quotation = await loadFullQuotation(req.params.id);
    if (!quotation) return res.status(404).json({ error: 'Quotation not found' });
    const business = await loadBusinessProfile();

    const html = buildQuotationHtml({ business, quotation, items: quotation.items });
    const pdf = await renderHtmlToPdf(html);

    const result = await sendWhatsAppDocument(quotation.bill_to_mobile, {
      pdfBuffer: pdf,
      filename: `Quotation-${quotation.id}.pdf`,
      caption: `Quotation #${quotation.id} from ${business?.name || 'us'}`,
    });

    await pool.query(`UPDATE quotations SET status = 'sent', sent_at = now() WHERE id = $1`, [quotation.id]);
    res.json({ ...result, quotation: await loadFullQuotation(quotation.id) });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message || 'Failed to send quotation' });
  }
});

export default router;
