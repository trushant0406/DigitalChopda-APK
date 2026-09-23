import { Router } from 'express';
import { pool } from '../db.js';
import { renderHtmlToPdf } from '../pdf/renderPdf.js';
import { buildCreditNoteHtml } from '../pdf/creditNoteHtml.js';
import { sendWhatsAppWebDocument } from '../notify/whatsappWebClient.js';
import { storageService } from '../storage/storageService.js';
import { syncCreditNotePdf } from '../storage/documentSync.js';
import { getDocumentStorageKey, getDocumentFallbackKeys, findCachedDocumentBuffer } from '../storage/storageKeyHelper.js';

const router = Router();

async function loadBusinessProfile(businessId = 1) {
  const targetId = businessId || 1;
  const res = await pool.query('SELECT * FROM businesses WHERE id = $1', [targetId]);
  if (res.rows.length > 0) return res.rows[0];
  const legacy = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  return legacy.rows[0] || null;
}

// Calculate the next sequential credit note number (e.g. CN-0001)
export async function calculateNextCreditNoteNumber(clientOrPool = pool, businessId = 1) {
  const targetId = businessId || 1;
  const res = await clientOrPool.query(
    `SELECT credit_note_number FROM credit_notes WHERE business_id = $1 AND credit_note_number LIKE 'CN-%'`,
    [targetId]
  );
  let maxSeq = 0;
  for (const row of res.rows) {
    const val = (row.credit_note_number || '').trim();
    const m = val.match(/^CN-(\d+)$/);
    if (m) {
      const num = parseInt(m[1], 10);
      if (num > maxSeq) maxSeq = num;
    }
  }
  const nextSeq = maxSeq + 1;
  const suggestedNumber = `CN-${String(nextSeq).padStart(4, '0')}`;
  return { nextSeq, suggestedNumber };
}

// GET /api/credit-notes/next-number — get next sequential credit note number
router.get('/next-number', async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const data = await calculateNextCreditNoteNumber(pool, businessId);
    res.json(data);
  } catch (err) {
    console.error('Error in GET /api/credit-notes/next-number:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/credit-notes — list credit notes with filtering
router.get('/', async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const { contact_id, order_id, q } = req.query;
    const conditions = [`cn.business_id = $1`];
    const params = [businessId];
    let i = 2;

    if (contact_id) {
      conditions.push(`cn.contact_id = $${i++}`);
      params.push(contact_id);
    }
    if (order_id) {
      conditions.push(`cn.order_id = $${i++}`);
      params.push(order_id);
    }
    if (q) {
      conditions.push(`(cn.credit_note_number ILIKE $${i} OR c.name ILIKE $${i} OR c.phone ILIKE $${i} OR cn.original_invoice_no ILIKE $${i})`);
      params.push(`%${q}%`);
      i++;
    }

    const whereClause = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';

    const query = `
      SELECT cn.*,
             c.name AS contact_name, c.phone AS contact_phone, c.city AS contact_city,
             c.gstin AS contact_gstin,
             COALESCE(items_data.items_count, 0) AS items_count,
             COALESCE(items_data.items_summary, '') AS items_summary
      FROM credit_notes cn
      JOIN contacts c ON c.id = cn.contact_id
      LEFT JOIN (
        SELECT credit_note_id,
               COUNT(id) AS items_count,
               string_agg(product_name || ' ×' || quantity, ', ') AS items_summary
        FROM credit_note_items
        GROUP BY credit_note_id
      ) items_data ON items_data.credit_note_id = cn.id
      ${whereClause}
      ORDER BY cn.credit_note_date DESC, cn.id DESC
    `;

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    console.error('Error listing credit notes:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/credit-notes/orders/:orderId/returnable-items (and alias /returnable-items/:orderId)
router.get(['/orders/:orderId/returnable-items', '/returnable-items/:orderId'], async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const orderId = req.params.orderId;
    const ordRes = await pool.query(
      `SELECT o.*, c.name AS contact_name, c.phone AS contact_phone, c.city AS contact_city
       FROM orders o
       JOIN contacts c ON c.id = o.contact_id
       WHERE o.id = $1 AND o.business_id = $2`,
      [orderId, businessId]
    );
    if (ordRes.rows.length === 0) {
      return res.status(404).json({ error: 'Order not found' });
    }
    const order = ordRes.rows[0];

    const [itemsRes, prevReturnsRes] = await Promise.all([
      pool.query(`SELECT * FROM order_items WHERE order_id = $1 ORDER BY id ASC`, [orderId]),
      pool.query(
        `SELECT cni.product_name, SUM(cni.quantity) AS total_returned_qty
         FROM credit_note_items cni
         JOIN credit_notes cn ON cn.id = cni.credit_note_id
         WHERE cn.order_id = $1 AND cn.business_id = $2
         GROUP BY cni.product_name`,
        [orderId, businessId]
      ),
    ]);

    const alreadyReturnedMap = {};
    for (const row of prevReturnsRes.rows) {
      alreadyReturnedMap[row.product_name.trim().toLowerCase()] = Number(row.total_returned_qty || 0);
    }

    let totalBilled = 0;
    let totalReturned = 0;

    const returnableItems = itemsRes.rows.map((it) => {
      const key = it.product_name.trim().toLowerCase();
      const billedQty = Number(it.quantity || 0);
      const returnedQty = Number(alreadyReturnedMap[key] || 0);
      const remainingQty = Math.max(0, billedQty - returnedQty);
      totalBilled += billedQty;
      totalReturned += returnedQty;

      return {
        ...it,
        quantity: remainingQty, // default return qty to remaining
        billed_quantity: billedQty,
        already_returned_quantity: returnedQty,
        remaining_returnable_quantity: remainingQty,
        unit_price: Number(it.unit_price || 0),
        gst_rate: Number(it.gst_rate || 0),
        is_fully_returned: remainingQty <= 0,
      };
    });

    const isFullyReturned = totalBilled > 0 && totalReturned >= totalBilled - 0.001;

    res.json({
      order,
      is_fully_returned: isFullyReturned,
      total_billed_qty: totalBilled,
      total_returned_qty: totalReturned,
      items: returnableItems,
    });
  } catch (err) {
    console.error('Error fetching returnable items:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/credit-notes/:id — fetch single credit note with items
router.get('/:id', async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const cnRes = await pool.query(
      `SELECT cn.*,
              c.name AS contact_name, c.phone AS contact_phone, c.city AS contact_city,
              c.gstin AS contact_gstin, c.pan AS contact_pan, c.billing_address, c.shipping_address, c.state
       FROM credit_notes cn
       JOIN contacts c ON c.id = cn.contact_id
       WHERE cn.id = $1 AND cn.business_id = $2`,
      [req.params.id, businessId]
    );
    if (cnRes.rows.length === 0) {
      return res.status(404).json({ error: 'Credit note not found' });
    }

    const itemsRes = await pool.query(
      `SELECT * FROM credit_note_items WHERE credit_note_id = $1 ORDER BY id ASC`,
      [req.params.id]
    );

    res.json({
      ...cnRes.rows[0],
      items: itemsRes.rows,
    });
  } catch (err) {
    console.error('Error fetching credit note:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/credit-notes — create new credit note
router.post('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const {
    contact_id,
    order_id,
    credit_note_number,
    credit_note_date,
    reason,
    notes,
    items,
  } = req.body;

  if (!contact_id) {
    return res.status(400).json({ error: 'contact_id is required' });
  }
  if (!Array.isArray(items) || items.length === 0) {
    return res.status(400).json({ error: 'At least one returned item is required' });
  }

  for (const item of items) {
    if (!item.product_name || Number(item.quantity) <= 0 || Number(item.unit_price) < 0) {
      return res.status(400).json({ error: 'Each item must have a valid product_name, quantity > 0, and non-negative unit_price' });
    }
  }

  const client = await pool.connect();
  let inTransaction = false;
  try {
    await client.query('BEGIN');
    inTransaction = true;

    // Determine credit note number
    let finalCnNumber = (credit_note_number || '').trim();
    if (finalCnNumber) {
      const existing = await client.query('SELECT id FROM credit_notes WHERE credit_note_number = $1 AND business_id = $2', [finalCnNumber, businessId]);
      if (existing.rows.length > 0) {
        await client.query('ROLLBACK');
        inTransaction = false;
        return res.status(400).json({ error: `Credit Note number "${finalCnNumber}" already exists.` });
      }
    } else {
      const nextData = await calculateNextCreditNoteNumber(client, businessId);
      finalCnNumber = nextData.suggestedNumber;
    }

    // Inspect linked order if provided and validate return limits
    let origInvoiceNo = null;
    let origInvoiceDate = null;
    let taxMode = 'split';
    if (order_id) {
      // Use FOR UPDATE for concurrency safety to prevent race conditions during returns
      const ordRes = await client.query('SELECT * FROM orders WHERE id = $1 AND business_id = $2 FOR UPDATE', [order_id, businessId]);
      if (ordRes.rows.length === 0) {
        await client.query('ROLLBACK');
        inTransaction = false;
        return res.status(404).json({ error: 'Linked invoice not found.' });
      }

      const ord = ordRes.rows[0];
      origInvoiceNo = ord.invoice_number || ord.order_number;
      origInvoiceDate = ord.invoice_date || (ord.created_at ? new Date(ord.created_at).toISOString().split('T')[0] : null);
      if (ord.tax_mode) taxMode = ord.tax_mode;

      // Check existing returns for this order to prevent duplicate / over-returning
      const [orderItemsRes, prevReturnsRes] = await Promise.all([
        client.query(`SELECT product_name, quantity FROM order_items WHERE order_id = $1`, [order_id]),
        client.query(
          `SELECT cni.product_name, SUM(cni.quantity) AS total_returned_qty
           FROM credit_note_items cni
           JOIN credit_notes cn ON cn.id = cni.credit_note_id
           WHERE cn.order_id = $1 AND cn.business_id = $2
           GROUP BY cni.product_name`,
          [order_id, businessId]
        ),
      ]);

      const alreadyReturnedMap = {};
      for (const row of prevReturnsRes.rows) {
        alreadyReturnedMap[row.product_name.trim().toLowerCase()] = Number(row.total_returned_qty || 0);
      }

      const orderItemMap = {};
      let orderTotalBilled = 0;
      let orderTotalReturned = 0;

      for (const oItem of orderItemsRes.rows) {
        const key = oItem.product_name.trim().toLowerCase();
        const billedQty = Number(oItem.quantity || 0);
        const returnedQty = Number(alreadyReturnedMap[key] || 0);
        const remaining = Math.max(0, billedQty - returnedQty);

        orderItemMap[key] = {
          product_name: oItem.product_name,
          billedQty,
          returnedQty,
          remaining,
        };
        orderTotalBilled += billedQty;
        orderTotalReturned += returnedQty;
      }

      if (orderTotalBilled > 0 && orderTotalReturned >= orderTotalBilled - 0.001) {
        await client.query('ROLLBACK');
        inTransaction = false;
        return res.status(400).json({
          error: `Invoice "${origInvoiceNo}" has already been completely returned. You cannot issue another credit note on this invoice.`,
        });
      }

      // Check each returned item against remaining returnable quantity
      for (const item of items) {
        const key = (item.product_name || '').trim().toLowerCase();
        const ordItem = orderItemMap[key];
        if (ordItem) {
          const reqQty = Number(item.quantity);
          if (reqQty > ordItem.remaining + 0.001) {
            await client.query('ROLLBACK');
            inTransaction = false;
            return res.status(400).json({
              error: `Cannot return ${reqQty} of "${ordItem.product_name}". Only ${ordItem.remaining} remaining returnable from this invoice (Billed: ${ordItem.billedQty}, Previously returned: ${ordItem.returnedQty}).`,
            });
          }
        }
      }
    }

    // Calculate totals
    let subtotal = 0;
    let taxAmount = 0;
    for (const item of items) {
      const qty = Number(item.quantity);
      const rate = Number(item.unit_price);
      const gst = Number(item.gst_rate || 0);
      const lineSub = qty * rate;
      const lineTax = lineSub * (gst / 100);
      subtotal += lineSub;
      taxAmount += lineTax;
    }
    const totalAmount = subtotal + taxAmount;

    // Generate accurate timestamp with current hours, minutes, seconds (prevents 12:00 AM issue)
    let finalCnDate = new Date();
    if (credit_note_date) {
      const d = new Date(credit_note_date);
      if (!isNaN(d.getTime())) {
        const now = new Date();
        d.setHours(now.getHours(), now.getMinutes(), now.getSeconds(), now.getMilliseconds());
        finalCnDate = d;
      }
    }
    const effectiveDate = finalCnDate.toISOString();

    const cnResult = await client.query(
      `INSERT INTO credit_notes (
        business_id, credit_note_number, order_id, contact_id, credit_note_date,
        original_invoice_no, original_invoice_date, reason, notes,
        subtotal, tax_amount, total_amount, tax_mode
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
      RETURNING *`,
      [
        businessId,
        finalCnNumber,
        order_id || null,
        contact_id,
        effectiveDate,
        origInvoiceNo,
        origInvoiceDate,
        reason || 'Sales Return',
        notes || null,
        subtotal,
        taxAmount,
        totalAmount,
        taxMode,
      ]
    );
    const creditNote = cnResult.rows[0];

    for (const item of items) {
      await client.query(
        `INSERT INTO credit_note_items (credit_note_id, product_name, quantity, unit_price, gst_rate, hsn_code)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [
          creditNote.id,
          item.product_name,
          Number(item.quantity),
          Number(item.unit_price),
          Number(item.gst_rate || 0),
          item.hsn_code || null,
        ]
      );
    }

    // If credit note was linked to an invoice, check if the remaining invoice balance is now 0
    if (order_id) {
      const ordCheck = await client.query(
        `SELECT COALESCE(item_totals.grand_total, 0) AS grand_total,
                COALESCE(pay_totals.total_paid, 0) AS total_paid,
                COALESCE(cn_totals.total_credited, 0) AS total_credited
         FROM orders o
         JOIN (
           SELECT order_id, SUM(quantity * unit_price + quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS grand_total
           FROM order_items GROUP BY order_id
         ) item_totals ON item_totals.order_id = o.id
         LEFT JOIN (
           SELECT order_id, SUM(amount) AS total_paid FROM payments WHERE order_id IS NOT NULL GROUP BY order_id
         ) pay_totals ON pay_totals.order_id = o.id
         LEFT JOIN (
           SELECT order_id, SUM(total_amount) AS total_credited FROM credit_notes WHERE order_id IS NOT NULL GROUP BY order_id
         ) cn_totals ON cn_totals.order_id = o.id
         WHERE o.id = $1 AND o.business_id = $2`,
        [order_id, businessId]
      );
      if (ordCheck.rows.length > 0) {
        const row = ordCheck.rows[0];
        const remaining = Number(row.grand_total) - Number(row.total_paid) - Number(row.total_credited);
        if (remaining <= 0.01) {
          await client.query(`UPDATE orders SET payment_status = 'paid', paid_at = COALESCE(paid_at, now()), updated_at = now() WHERE id = $1`, [order_id]);
          await client.query(`UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND status = 'pending'`, [order_id]);
        }
      }
    }

    await client.query('COMMIT');
    inTransaction = false;

    // Eager background sync to Cloud Storage (myBillBook / Zoho style)
    setImmediate(() => {
      syncCreditNotePdf(creditNote.id).catch((e) => console.warn('[Storage] Background credit note PDF upload failed:', e.message));
    });

    res.status(201).json(creditNote);
  } catch (err) {
    if (inTransaction) {
      try {
        await client.query('ROLLBACK');
      } catch (rbErr) {
        console.error('Error executing ROLLBACK on credit note failure:', rbErr);
      }
    }
    console.error('Error creating credit note:', err);
    if (!res.headersSent) {
      if (err.code === '23505') {
        res.status(400).json({ error: 'A credit note with this number already exists. Transaction safely rolled back.' });
      } else if (err.code === '23503') {
        res.status(400).json({ error: 'Invalid reference: customer or order not found. Transaction safely rolled back.' });
      } else if (err.code === '23514') {
        res.status(400).json({ error: `Check constraint failed: ${err.message}. Transaction safely rolled back.` });
      } else {
        res.status(500).json({ error: err.message || 'Failed to create credit note. Transaction safely rolled back.' });
      }
    }
  } finally {
    client.release();
  }
});

// GET /api/credit-notes/:id/pdf — render and download official GST Credit Note PDF
router.get('/:id/pdf', async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const cnRes = await pool.query('SELECT * FROM credit_notes WHERE id = $1 AND business_id = $2', [req.params.id, businessId]);
    if (cnRes.rows.length === 0) {
      return res.status(404).json({ error: 'Credit note not found' });
    }
    const creditNote = cnRes.rows[0];
    const safeNumber = (creditNote.credit_note_number || `credit_note_${creditNote.id}`).replace(/[^a-zA-Z0-9_-]/g, '_');
    const [itemsRes, contactRes, business] = await Promise.all([
      pool.query('SELECT * FROM credit_note_items WHERE credit_note_id = $1 ORDER BY id ASC', [creditNote.id]),
      pool.query('SELECT * FROM contacts WHERE id = $1', [creditNote.contact_id]),
      loadBusinessProfile(businessId),
    ]);

    const contact = contactRes.rows[0] || {};
    const items = itemsRes.rows;
    const filename = `Credit-Note-${safeNumber}.pdf`;
    const primaryKey = getDocumentStorageKey({
      business,
      contactName: contact.name,
      docType: 'credit-notes',
      filename,
    });
    const fallbackKeys = getDocumentFallbackKeys({
      business,
      contactName: contact.name,
      docType: 'credit-notes',
      filename,
    });

    // 1. Return cached PDF from object storage if present (checks primary, then fallback keys)
    const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
    if (cached) {
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', `inline; filename="${safeNumber}.pdf"`);
      return res.end(cached.buffer);
    }

    const html = buildCreditNoteHtml({ business, creditNote, items, contact });
    const pdfBuffer = await renderHtmlToPdf(html);

    // Asynchronously save to primary storage cache
    storageService.uploadFile({
      key: primaryKey,
      buffer: pdfBuffer,
      contentType: 'application/pdf',
      isPublic: true,
    }).catch((e) => console.warn('[Storage] Failed to cache credit note PDF in storage:', e.message));

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="${safeNumber}.pdf"`);
    res.end(pdfBuffer);
  } catch (err) {
    console.error('Error generating credit note PDF:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/credit-notes/:id/whatsapp (and alias /send-whatsapp) — send Credit Note PDF directly to customer
router.post(['/:id/whatsapp', '/:id/send-whatsapp'], async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const cnRes = await pool.query('SELECT * FROM credit_notes WHERE id = $1 AND business_id = $2', [req.params.id, businessId]);
    if (cnRes.rows.length === 0) {
      return res.status(404).json({ error: 'Credit note not found' });
    }
    const creditNote = cnRes.rows[0];

    const [itemsRes, contactRes, business] = await Promise.all([
      pool.query('SELECT * FROM credit_note_items WHERE credit_note_id = $1 ORDER BY id ASC', [creditNote.id]),
      pool.query('SELECT * FROM contacts WHERE id = $1', [creditNote.contact_id]),
      loadBusinessProfile(businessId),
    ]);

    const contact = contactRes.rows[0] || {};
    const items = itemsRes.rows;
    if (!contact.phone) {
      return res.status(400).json({ error: 'Customer phone number is missing' });
    }

    const safeNumber = (creditNote.credit_note_number || `credit_note_${creditNote.id}`).replace(/[^a-zA-Z0-9_-]/g, '_');
    const filename = `Credit-Note-${safeNumber}.pdf`;
    const primaryKey = getDocumentStorageKey({
      business,
      contactName: contact.name,
      docType: 'credit-notes',
      filename,
    });
    const fallbackKeys = getDocumentFallbackKeys({
      business,
      contactName: contact.name,
      docType: 'credit-notes',
      filename,
    });

    const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
    let pdfBuffer = cached?.buffer || null;
    if (!pdfBuffer) {
      const html = buildCreditNoteHtml({ business, creditNote, items, contact });
      pdfBuffer = await renderHtmlToPdf(html);
      storageService.uploadFile({ key: primaryKey, buffer: pdfBuffer, contentType: 'application/pdf', isPublic: true }).catch(() => {});
    }

    const fileName = `${safeNumber}.pdf`;
    const caption = `Dear ${contact.name || 'Customer'},\n\nPlease find attached Credit Note ${creditNote.credit_note_number} for ₹${Number(creditNote.total_amount).toFixed(2)} (${creditNote.reason || 'Sales Return'}). This amount has been credited to your account ledger.\n\nThank you!`;

    const cleanPhone = (contact.phone || '').replace(/\D/g, '');
    const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;
    const message = `Hello *${contact.name || 'Customer'}*,\n\nPlease find attached your Credit Note *${creditNote.credit_note_number}* for *₹${Number(creditNote.total_amount).toFixed(2)}* (${creditNote.reason || 'Sales Return'}) from *${business?.company_name || business?.name || 'us'}*.\n\nThis amount has been credited to your account ledger.\n\nThank you!`;
    const whatsappUrl = `https://api.whatsapp.com/send?phone=${formattedPhone}&text=${encodeURIComponent(message)}`;

    let sentViaWeb = false;
    let webError = null;
    try {
      const waRes = await sendWhatsAppWebDocument(contact.phone, {
        pdfBuffer,
        filename: fileName,
        caption,
      });
      sentViaWeb = Boolean(waRes?.sent);
    } catch (webErr) {
      webError = webErr.message;
      console.warn('[credit-notes] WhatsApp Web document send fallback:', webErr.message);
    }

    res.json({ ok: true, sentViaWeb, whatsappUrl, webError });
  } catch (err) {
    console.error('Error sending credit note via WhatsApp:', err);
    res.status(500).json({ error: err.message });
  }
});

// DELETE /api/credit-notes/:id — void / cancel credit note with full atomic transaction rollback
router.delete('/:id', async (req, res) => {
  const businessId = req.businessId || 1;
  const client = await pool.connect();
  let inTransaction = false;
  try {
    await client.query('BEGIN');
    inTransaction = true;

    // 1. Fetch and lock credit note row
    const cnRes = await client.query('SELECT * FROM credit_notes WHERE id = $1 AND business_id = $2 FOR UPDATE', [req.params.id, businessId]);
    if (cnRes.rows.length === 0) {
      await client.query('ROLLBACK');
      inTransaction = false;
      return res.status(404).json({ error: 'Credit note not found' });
    }
    const creditNote = cnRes.rows[0];
    const orderId = creditNote.order_id;

    // 2. If linked to an order, lock order row
    if (orderId) {
      await client.query('SELECT id FROM orders WHERE id = $1 AND business_id = $2 FOR UPDATE', [orderId, businessId]);
    }

    // 3. Delete items and credit note header
    await client.query('DELETE FROM credit_note_items WHERE credit_note_id = $1', [creditNote.id]);
    await client.query('DELETE FROM credit_notes WHERE id = $1 AND business_id = $2', [creditNote.id, businessId]);

    // 4. If linked to an order, recalculate the order's remaining balance and update status
    if (orderId) {
      const ordCheck = await client.query(
        `SELECT COALESCE(item_totals.grand_total, 0) AS grand_total,
                COALESCE(pay_totals.total_paid, 0) AS total_paid,
                COALESCE(cn_totals.total_credited, 0) AS total_credited
         FROM orders o
         LEFT JOIN (
           SELECT order_id, SUM(quantity * unit_price + quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS grand_total
           FROM order_items GROUP BY order_id
         ) item_totals ON item_totals.order_id = o.id
         LEFT JOIN (
           SELECT order_id, SUM(amount) AS total_paid FROM payments WHERE order_id IS NOT NULL AND business_id = $2 GROUP BY order_id
         ) pay_totals ON pay_totals.order_id = o.id
         LEFT JOIN (
           SELECT order_id, SUM(total_amount) AS total_credited FROM credit_notes WHERE order_id IS NOT NULL AND business_id = $2 GROUP BY order_id
         ) cn_totals ON cn_totals.order_id = o.id
         WHERE o.id = $1 AND o.business_id = $2`,
        [orderId, businessId]
      );
      if (ordCheck.rows.length > 0) {
        const row = ordCheck.rows[0];
        const grandTotal = Number(row.grand_total);
        const totalPaid = Number(row.total_paid);
        const totalCredited = Number(row.total_credited);
        const remaining = grandTotal - totalPaid - totalCredited;

        if (remaining > 0.01) {
          // Revert payment status back to pending or partially_paid
          const newStatus = totalPaid > 0.01 ? 'partially_paid' : 'pending';
          await client.query(
            `UPDATE orders SET payment_status = $1, paid_at = NULL, updated_at = now() WHERE id = $2 AND business_id = $3`,
            [newStatus, orderId, businessId]
          );
        } else {
          await client.query(
            `UPDATE orders SET payment_status = 'paid', updated_at = now() WHERE id = $1 AND business_id = $2`,
            [orderId, businessId]
          );
        }
      }
    }

    await client.query('COMMIT');
    inTransaction = false;

    // Clean up credit note PDF from Cloud Storage and re-sync linked order invoice
    const safeNumber = (creditNote.credit_note_number || `credit_note_${creditNote.id}`).replace(/[^a-zA-Z0-9_-]/g, '_');
    const [contactRes, business] = await Promise.all([
      client.query('SELECT name FROM contacts WHERE id = $1', [creditNote.contact_id]),
      loadBusinessProfile(businessId),
    ]);
    const contactName = contactRes.rows[0]?.name;
    const filename = `Credit-Note-${safeNumber}.pdf`;
    const primaryKey = getDocumentStorageKey({ business, contactName, docType: 'credit-notes', filename });
    const fallbackKeys = getDocumentFallbackKeys({ business, contactName, docType: 'credit-notes', filename });
    for (const k of [primaryKey, ...fallbackKeys]) {
      storageService.deleteFile(k).catch(() => {});
    }
    if (orderId) {
      import('../storage/documentSync.js').then(({ syncInvoicePdf }) => {
        syncInvoicePdf(orderId).catch(() => {});
      }).catch(() => {});
    }

    res.json({ success: true, message: `Credit note ${creditNote.credit_note_number} has been voided successfully.` });
  } catch (err) {
    if (inTransaction) {
      try {
        await client.query('ROLLBACK');
      } catch (rbErr) {
        console.error('Error executing ROLLBACK on void credit note:', rbErr);
      }
    }
    console.error('Error voiding credit note:', err);
    res.status(500).json({ error: err.message || 'Failed to void credit note' });
  } finally {
    client.release();
  }
});

export default router;
