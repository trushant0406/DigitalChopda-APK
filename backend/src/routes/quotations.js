import { Router } from 'express';
import { pool } from '../db.js';
import { findOrCreateContact } from '../lib/contacts.js';
import { buildQuotationHtml } from '../pdf/quotationHtml.js';
import { renderHtmlToPdf } from '../pdf/renderPdf.js';
import { sendWhatsAppDocument } from '../notify/whatsapp.js';
import { calculateNextInvoiceNumber } from './orders.js';
import { storageService } from '../storage/storageService.js';

const router = Router();

const QUOTATION_SELECT = `
  SELECT quotations.*,
         orders.order_number AS converted_order_number,
         orders.invoice_number AS converted_order_invoice_number,
         orders.invoice_date AS converted_order_invoice_date,
         orders.payment_status AS converted_order_payment_status,
         COALESCE(item_totals.subtotal, 0) AS subtotal,
         COALESCE(item_totals.tax_amount, 0) AS tax_amount,
         COALESCE(item_totals.total_amount, 0) AS total_amount,
         COALESCE(item_totals.items, '[]') AS items
  FROM quotations
  LEFT JOIN orders ON orders.id = quotations.converted_order_id
  LEFT JOIN (
    SELECT quotation_id,
           SUM(quantity * rate) AS subtotal,
           SUM(quantity * rate * gst_rate / 100) AS tax_amount,
           SUM(quantity * rate * (1 + gst_rate / 100)) AS total_amount,
           json_agg(json_build_object(
             'id', id,
             'product_name', product_name,
             'hsn_code', hsn_code,
             'unit', unit,
             'quantity', quantity,
             'rate', rate,
             'gst_rate', gst_rate
           )) AS items
    FROM quotation_items
    GROUP BY quotation_id
  ) item_totals ON item_totals.quotation_id = quotations.id
`;

// Helper: load a full quotation with items and business details
async function loadFullQuotation(id) {
  const result = await pool.query(`${QUOTATION_SELECT} WHERE quotations.id = $1`, [id]);
  return result.rows[0] || null;
}

async function loadBusinessProfile() {
  const result = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  return result.rows[0] || null;
}

// POST /api/quotations — create a quotation
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
    if (!item.product_name || item.quantity === undefined || item.quantity === '' || Number(item.quantity) <= 0 || item.rate === undefined || item.rate === '') {
      return res.status(400).json({ error: 'Each item needs a valid product_name, a quantity greater than 0, and a rate' });
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
      contact = await findOrCreateContact(client, {
        phone, name, city, gstin, pan, billing_address, shipping_address, state,
      });
    }

    const business = await loadBusinessProfile();
    const effectivePlaceOfSupply = state || contact.state || (business ? business.state : null);
    const taxMode = (business && business.state && effectivePlaceOfSupply && business.state.trim().toLowerCase() === effectivePlaceOfSupply.trim().toLowerCase())
      ? 'split'
      : 'igst';

    const quotationRes = await client.query(
      `INSERT INTO quotations
       (contact_id, bill_to_name, bill_to_address, bill_to_mobile, bill_to_gstin, bill_to_pan,
        ship_to_name, ship_to_address, place_of_supply, tax_mode, terms_and_conditions)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
       RETURNING *`,
      [
        contact.id,
        name || contact.name || contact.phone,
        billing_address || contact.billing_address || null,
        phone || contact.phone,
        gstin || contact.gstin || null,
        pan || contact.pan || null,
        ship_to_name || name || contact.name || null,
        ship_to_address || shipping_address || contact.shipping_address || billing_address || contact.billing_address || null,
        effectivePlaceOfSupply,
        taxMode,
        terms_and_conditions || business?.terms_and_conditions || null,
      ]
    );
    const quotation = quotationRes.rows[0];

    for (const item of items) {
      await client.query(
        `INSERT INTO quotation_items
         (quotation_id, product_name, hsn_code, unit, quantity, rate, gst_rate)
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [
          quotation.id,
          item.product_name,
          item.hsn_code || null,
          item.unit || 'PCS',
          item.quantity,
          item.rate,
          item.gst_rate !== undefined ? item.gst_rate : 18,
        ]
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

// GET /api/quotations — list with optional search query q
router.get('/', async (req, res) => {
  const { q } = req.query;
  const conditions = [];
  const values = [];
  let i = 1;

  if (q && q.trim()) {
    const term = `%${q.trim()}%`;
    conditions.push(`(
      quotations.id::text ILIKE $${i} OR
      quotations.bill_to_name ILIKE $${i} OR
      quotations.bill_to_mobile ILIKE $${i} OR
      quotations.bill_to_address ILIKE $${i} OR
      quotations.bill_to_gstin ILIKE $${i} OR
      orders.invoice_number ILIKE $${i} OR
      orders.order_number ILIKE $${i}
    )`);
    values.push(term);
    i++;
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await pool.query(`${QUOTATION_SELECT} ${where} ORDER BY quotations.created_at DESC LIMIT 500`, values);
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
    const cacheKey = `quotations/Quotation-${quotation.id}.pdf`;

    // 1. Return cached PDF from object storage if present
    if (await storageService.exists(cacheKey)) {
      try {
        const cachedPdf = await storageService.getFileBuffer(cacheKey);
        res.setHeader('Content-Type', 'application/pdf');
        res.setHeader('Content-Disposition', `inline; filename="Quotation-${quotation.id}.pdf"`);
        return res.send(cachedPdf);
      } catch (cacheErr) {
        console.warn('[Storage] Failed to read cached quotation PDF, regenerating:', cacheErr.message);
      }
    }

    const html = buildQuotationHtml({ business, quotation, items: quotation.items });
    const pdf = await renderHtmlToPdf(html);

    // Asynchronously save to storage cache (GCS, S3, or Local)
    storageService.uploadFile({
      key: cacheKey,
      buffer: pdf,
      contentType: 'application/pdf',
      isPublic: true,
    }).catch((e) => console.warn('[Storage] Failed to cache quotation PDF in storage:', e.message));

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="Quotation-${quotation.id}.pdf"`);
    res.send(pdf);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to generate PDF' });
  }
});

// POST /api/quotations/:id/send-whatsapp — generate the PDF link and WhatsApp wa.me link
router.post('/:id/send-whatsapp', async (req, res) => {
  try {
    const quotation = await loadFullQuotation(req.params.id);
    if (!quotation) return res.status(404).json({ error: 'Quotation not found' });
    const business = await loadBusinessProfile();

    const host = req.get('host');
    const protocol = req.protocol;
    const pdfUrl = `${protocol}://${host}/api/quotations/${quotation.id}/pdf`;

    const cleanPhone = (quotation.bill_to_mobile || '').replace(/\D/g, '');
    const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;

    const qDate = quotation.quotation_date ? new Date(quotation.quotation_date).toLocaleDateString('en-IN') : '';
    const formattedAmount = Number(quotation.total_amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    const pdfCaption = `📄 *Quotation #${quotation.id}*\n\nDear *${quotation.bill_to_name}*,\n\nThank you for reaching out to *${business?.name || 'us'}*! Please find your official quotation PDF attached above.\n\n💰 *Total Amount:* ₹${formattedAmount}${qDate ? `\n📅 *Date:* ${qDate}` : ''}\n\nIf you have any questions or would like to confirm this order, feel free to reply directly to this message.\n\nBest regards,\n*${business?.name || 'Customer Support Team'}*`;

    const message = `Hello *${quotation.bill_to_name}*,\n\nPlease find attached your Quotation *#${quotation.id}* for *₹${formattedAmount}* from *${business?.name || 'us'}*.\n\nView or download PDF:\n${pdfUrl}\n\nThank you for your business!`;
    const whatsappUrl = `https://wa.me/${formattedPhone}?text=${encodeURIComponent(message)}`;

    // Try sending actual PDF file attachment via WhatsApp Web Client (tokenless)
    let webResult = null;
    let webError = null;
    const cacheKey = `quotations/Quotation-${quotation.id}.pdf`;
    let pdf = null;
    if (await storageService.exists(cacheKey)) {
      try {
        pdf = await storageService.getFileBuffer(cacheKey);
      } catch (e) {
        console.warn('[Storage] Failed to read cached quotation PDF, regenerating:', e.message);
      }
    }
    if (!pdf) {
      const html = buildQuotationHtml({ business, quotation, items: quotation.items });
      pdf = await renderHtmlToPdf(html);
      storageService.uploadFile({ key: cacheKey, buffer: pdf, contentType: 'application/pdf', isPublic: true }).catch(() => {});
    }

    try {
      const { sendWhatsAppWebDocument } = await import('../notify/whatsappWebClient.js');
      webResult = await sendWhatsAppWebDocument(quotation.bill_to_mobile, {
        pdfBuffer: pdf,
        filename: `Quotation-${quotation.id}.pdf`,
        caption: pdfCaption,
      });
    } catch (e) {
      webError = e.message;
      console.warn('WhatsApp Web send fallback:', e.message);
    }

    let apiResult = { dryRun: true };
    try {
      const { isWhatsAppConfigured, sendWhatsAppDocument } = await import('../notify/whatsapp.js');
      if (isWhatsAppConfigured()) {
        apiResult = await sendWhatsAppDocument(quotation.bill_to_mobile, {
          pdfBuffer: pdf,
          filename: `Quotation-${quotation.id}.pdf`,
          caption: `Quotation #${quotation.id} from ${business?.name || 'us'}`,
        });
      }
    } catch (e) {
      console.warn('WhatsApp Cloud API notice:', e.message);
    }

    await pool.query(`UPDATE quotations SET status = 'sent', sent_at = now() WHERE id = $1`, [quotation.id]);
    res.json({
      ok: true,
      sentViaWeb: Boolean(webResult?.sent),
      whatsappUrl,
      webError,
      ...apiResult,
      quotation: await loadFullQuotation(quotation.id),
    });
  } catch (err) {
    console.error('Failed to send quotation via WhatsApp:', err.message);
    res.status(500).json({ error: err.message || 'Failed to send quotation' });
  }
});

// POST /api/quotations/:id/convert-to-invoice — converts a quotation into a sales invoice (orders + order_items)
router.post('/:id/convert-to-invoice', async (req, res) => {
  const quotationId = parseInt(req.params.id, 10);
  const {
    order_number, invoice_date, order_ref, notes,
    payment_status, payment_method,
    items: customItems,
    send_whatsapp,
  } = req.body || {};

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Fetch quotation
    const qRes = await client.query('SELECT * FROM quotations WHERE id = $1 FOR UPDATE', [quotationId]);
    if (qRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Quotation not found' });
    }
    const quotation = qRes.rows[0];
    if (quotation.status === 'converted') {
      await client.query('ROLLBACK');
      return res.status(400).json({
        error: `Quotation #${quotationId} has already been converted to an invoice.`,
      });
    }

    // 2. Fetch quotation items if custom items not provided
    const itemsRes = await client.query('SELECT * FROM quotation_items WHERE quotation_id = $1', [quotationId]);
    const itemsFromQuotation = itemsRes.rows.map((i) => ({
      product_name: i.product_name,
      quantity: Number(i.quantity),
      unit_price: Number(i.rate),
      gst_rate: Number(i.gst_rate || 0),
      hsn_code: i.hsn_code,
      unit: i.unit,
    }));

    // If user edited prices/quantities or added/removed items during conversion, use the edited items!
    const finalItems = (Array.isArray(customItems) && customItems.length > 0)
      ? customItems.map((i) => ({
          product_name: i.product_name,
          quantity: Number(i.quantity) || 1,
          unit_price: Number(i.unit_price !== undefined ? i.unit_price : (i.rate ?? 0)),
          gst_rate: i.gst_rate !== undefined ? Number(i.gst_rate) : 18,
          hsn_code: i.hsn_code || null,
          unit: i.unit || 'PCS',
        }))
      : itemsFromQuotation;

    if (finalItems.length === 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Cannot convert a quotation with no items' });
    }

    // 3. Determine final invoice number
    let finalInvoiceNumber = (req.body?.invoice_number || req.body?.order_number || '').trim();
    if (finalInvoiceNumber) {
      const existing = await client.query('SELECT id FROM orders WHERE invoice_number = $1', [finalInvoiceNumber]);
      if (existing.rows.length > 0) {
        await client.query('ROLLBACK');
        return res.status(400).json({ error: `Invoice number "${finalInvoiceNumber}" already exists. Please choose a different number.` });
      }

      // Check trailing digits to advance sequence
      const match = finalInvoiceNumber.match(/^(.*?)(\d+)$/);
      if (match) {
        const detectedPrefix = match[1];
        const digitStr = match[2];
        const num = parseInt(digitStr, 10);
        const padding = digitStr.length > 1 ? digitStr.length : 0;
        await client.query(
          `UPDATE business_profile
           SET invoice_next_number = GREATEST(invoice_next_number, $1),
               invoice_padding = $2,
               invoice_prefix = COALESCE(NULLIF($3, ''), invoice_prefix),
               updated_at = now()
           WHERE id = 1`,
          [num + 1, padding, detectedPrefix]
        );
      }
    } else {
      const nextData = await calculateNextInvoiceNumber(client);
      finalInvoiceNumber = nextData.suggestedNumber;

      await client.query(
        'UPDATE business_profile SET invoice_next_number = $1, updated_at = now() WHERE id = 1',
        [nextData.nextNumber + 1]
      );
    }

    const effectiveInvoiceDate = invoice_date || new Date().toISOString().split('T')[0];
    const isPaid = payment_status === 'paid';

    // Calculate subtotal, tax amount, and automatic cash round-off
    let calculatedSubtotal = 0;
    let calculatedTax = 0;
    for (const item of finalItems) {
      const q = Number(item.quantity || 0);
      const p = Number(item.unit_price || 0);
      const g = Number(item.gst_rate || 0);
      calculatedSubtotal += q * p;
      calculatedTax += q * p * (g / 100);
    }
    const rawTotal = calculatedSubtotal + calculatedTax;
    const roundedTotal = Math.round(rawTotal);
    const calculatedRoundOff = Number((roundedTotal - rawTotal).toFixed(2));

    // 4. Create Order / Invoice record linked to quotation_id and order_ref
    const orderRes = await client.query(
      `INSERT INTO orders (
         contact_id, quotation_id, order_number, invoice_number, invoice_date, order_ref, notes,
         payment_status, payment_method, place_of_supply, tax_mode, round_off
       )
       VALUES ($1, $2, '', $3, $4, $5, $6, $7, $8, $9, $10, $11) RETURNING *`,
      [
        quotation.contact_id,
        quotationId,
        finalInvoiceNumber,
        effectiveInvoiceDate,
        order_ref || `Quote #${quotationId}`,
        notes || (quotation.terms_and_conditions ? `From Quotation #${quotationId}` : null),
        isPaid ? 'paid' : 'pending',
        isPaid ? (payment_method || 'cash') : null,
        quotation.place_of_supply || 'Gujarat',
        quotation.tax_mode || 'split',
        calculatedRoundOff,
      ]
    );
    const newOrder = orderRes.rows[0];

    if (isPaid && roundedTotal > 0) {
      const seqRes = await client.query(
        `SELECT COALESCE(MAX(NULLIF(regexp_replace(receipt_number, '\\D', '', 'g'), '')::INT), 0) + 1 AS next_seq
         FROM payments WHERE receipt_number IS NOT NULL`
      );
      const receiptNum = `REC-${String(seqRes.rows[0]?.next_seq || 1).padStart(4, '0')}`;
      const pMode = (payment_method && ['cash', 'upi', 'bank_transfer', 'card', 'cheque'].includes(payment_method))
        ? payment_method
        : 'cash';
      await client.query(
        `INSERT INTO payments (contact_id, order_id, amount, payment_mode, method, receipt_number, paid_at)
         VALUES ($1, $2, $3, $4, $4, $5, now())`,
        [quotation.contact_id, newOrder.id, roundedTotal, pMode, receiptNum]
      );
    }

    // Standard Call Intelligence order number ORD-XXXXX
    const generatedOrderNumber = `ORD-${String(newOrder.id).padStart(5, '0')}`;
    await client.query('UPDATE orders SET order_number = $1 WHERE id = $2', [generatedOrderNumber, newOrder.id]);

    // 5. Insert order items (using edited prices/quantities)
    for (const item of finalItems) {
      await client.query(
        `INSERT INTO order_items (order_id, product_name, quantity, unit_price, gst_rate)
         VALUES ($1, $2, $3, $4, $5)`,
        [newOrder.id, item.product_name, item.quantity, item.unit_price, item.gst_rate || null]
      );
    }

    // 6. Update quotation status to 'converted' and link converted_order_id
    await client.query(
      `UPDATE quotations
       SET status = 'converted', converted_order_id = $1, converted_at = now()
       WHERE id = $2`,
      [newOrder.id, quotationId]
    );

    // 7. If payment pending, create owner follow-up reminder
    if (!isPaid) {
      const dueAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
      await client.query(
        `INSERT INTO followups (contact_id, order_id, due_at, reason, channel)
         VALUES ($1, $2, $3, $4, 'push')`,
        [quotation.contact_id, newOrder.id, dueAt, `Collect payment for invoice ${finalInvoiceNumber} (Order ${generatedOrderNumber} from Quotation #${quotationId})`]
      );
    }

    await client.query('COMMIT');
    let committed = true;
    client.release();

    const fullOrderRes = await pool.query(
      `SELECT orders.*, contacts.name, contacts.phone, contacts.city,
              contacts.gstin, contacts.pan, contacts.billing_address, contacts.shipping_address, contacts.state,
              COALESCE(item_totals.total_amount, 0) AS total_amount,
              COALESCE(item_totals.tax_amount, 0) AS tax_amount,
              COALESCE(item_totals.items, '[]') AS items
       FROM orders
       JOIN contacts ON contacts.id = orders.contact_id
       LEFT JOIN (
         SELECT oi.order_id,
                SUM(oi.quantity * oi.unit_price) AS total_amount,
                SUM(oi.quantity * oi.unit_price * COALESCE(oi.gst_rate, 0) / 100) AS tax_amount,
                json_agg(json_build_object(
                  'product_name', oi.product_name,
                  'quantity', oi.quantity,
                  'unit_price', oi.unit_price,
                  'gst_rate', oi.gst_rate,
                  'hsn_code', p.hsn_code,
                  'unit', 'PCS'
                )) AS items
         FROM order_items oi
         LEFT JOIN products p ON p.name = oi.product_name
         GROUP BY oi.order_id
       ) item_totals ON item_totals.order_id = orders.id
       WHERE orders.id = $1`,
      [newOrder.id]
    );

    // Optional immediate WhatsApp send
    let whatsappRes = null;
    if (send_whatsapp) {
      try {
        const business = await loadBusinessProfile();
        const host = req.get('host');
        const protocol = req.protocol;
        const pdfUrl = `${protocol}://${host}/api/orders/${newOrder.id}/pdf`;

        const cleanPhone = (quotation.bill_to_mobile || '').replace(/\D/g, '');
        const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;

        const grandTotal = finalItems.reduce((sum, i) => sum + (Number(i.quantity) * Number(i.unit_price) * (1 + (Number(i.gst_rate || 0) / 100))), 0);
        const formattedAmount = grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        const invDate = new Date(effectiveInvoiceDate).toLocaleDateString('en-IN');

        const pdfCaption = `📄 *Tax Invoice ${finalInvoiceNumber}*\n\nDear *${quotation.bill_to_name}*,\n\nThank you for doing business with *${business?.name || 'us'}*! Please find your official Tax Invoice PDF attached above.\n\n💰 *Total Amount:* ₹${formattedAmount}\n📅 *Invoice Date:* ${invDate}\n📊 *Status:* ${isPaid ? 'Paid ✓' : 'Payment Pending'}\n\nBest regards,\n*${business?.name || 'Customer Support Team'}*`;

        const message = `Hello *${quotation.bill_to_name}*,\n\nPlease find attached your Tax Invoice *${finalInvoiceNumber}* for *₹${formattedAmount}* from *${business?.name || 'us'}*.\n\nView or download PDF:\n${pdfUrl}\n\nThank you for your business!`;
        const whatsappUrl = `https://wa.me/${formattedPhone}?text=${encodeURIComponent(message)}`;

        let webSent = false;
        let webError = null;
        try {
          const { sendWhatsAppWebDocument } = await import('../notify/whatsappWebClient.js');
          const { buildInvoiceHtml } = await import('../pdf/invoiceHtml.js');
          const { renderHtmlToPdf } = await import('../pdf/renderPdf.js');
          const fullOrderForPdf = { ...newOrder, name: quotation.bill_to_name, phone: quotation.bill_to_mobile, billing_address: quotation.bill_to_address, state: quotation.place_of_supply };
          const html = buildInvoiceHtml({ business, order: fullOrderForPdf, items: finalItems });
          const pdf = await renderHtmlToPdf(html);
          console.log('[convert-to-invoice] Attempting WhatsApp document dispatch:', JSON.stringify({
            quotationId,
            orderId: newOrder.id,
            invoiceNumber: finalInvoiceNumber,
            phone: quotation.bill_to_mobile,
            pdfLength: pdf?.length,
          }));
          const r = await sendWhatsAppWebDocument(quotation.bill_to_mobile, {
            pdfBuffer: pdf,
            filename: `Invoice-${finalInvoiceNumber}.pdf`,
            caption: pdfCaption,
          });
          webSent = Boolean(r?.sent);
          console.log('[convert-to-invoice] WhatsApp document dispatch success:', JSON.stringify(r));
        } catch (webe) {
          webError = { message: webe.message, stack: webe.stack };
          console.log('[convert-to-invoice] WhatsApp Web send error in convert:', JSON.stringify(webError));
        }

        whatsappRes = { sentViaWeb: webSent, whatsappUrl, webError: webError?.message || null };
        console.log('[convert-to-invoice] Result whatsappRes:', JSON.stringify(whatsappRes));
      } catch (waErr) {
        console.warn('Auto WhatsApp dispatch error:', JSON.stringify({ message: waErr.message }));
      }
    }

    const updatedQuotation = await loadFullQuotation(quotationId);

    return res.status(201).json({
      success: true,
      message: `Quotation #${quotationId} successfully converted to invoice ${finalInvoiceNumber}`,
      order: fullOrderRes.rows[0],
      quotation: updatedQuotation,
      whatsapp: whatsappRes,
    });
  } catch (err) {
    try {
      await client.query('ROLLBACK');
    } catch (_) {}
    console.error('Error during quotation conversion:', err);
    return res.status(500).json({ error: 'Failed to convert quotation: ' + err.message });
  } finally {
    try {
      client.release();
    } catch (_) {}
  }
});

export default router;
