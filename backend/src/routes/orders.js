import { Router } from 'express';
import { pool } from '../db.js';
import { findOrCreateContact } from '../lib/contacts.js';
import { buildInvoiceHtml } from '../pdf/invoiceHtml.js';
import { renderHtmlToPdf } from '../pdf/renderPdf.js';
import { getNextReceiptNumber } from './credit.js';
import { storageService } from '../storage/storageService.js';
import { syncInvoicePdf } from '../storage/documentSync.js';
import { getDocumentStorageKey, getDocumentFallbackKeys, findCachedDocumentBuffer } from '../storage/storageKeyHelper.js';

const router = Router();

const ORDER_SELECT = `
  SELECT orders.*, contacts.name, contacts.phone, contacts.city,
         contacts.gstin, contacts.pan, contacts.billing_address, contacts.shipping_address, contacts.state,
         orders.place_of_supply, orders.tax_mode, orders.round_off,
         ROUND(COALESCE(item_totals.total_amount, 0)::numeric, 2) AS total_amount,
         ROUND(COALESCE(item_totals.tax_amount, 0)::numeric, 2) AS tax_amount,
         ROUND((COALESCE(item_totals.total_amount, 0) + COALESCE(item_totals.tax_amount, 0) + COALESCE(orders.round_off, 0))::numeric, 2) AS grand_total,
          COALESCE(
            CASE 
              WHEN orders.payment_status = 'paid' THEN ROUND((COALESCE(item_totals.total_amount, 0) + COALESCE(item_totals.tax_amount, 0) + COALESCE(orders.round_off, 0))::numeric, 2)
              ELSE COALESCE(pay_totals.received_amount, 0)
            END, 0
          ) AS received_amount,
          COALESCE(cn_totals.credited_amount, 0) AS credited_amount,
          CASE
            WHEN orders.payment_status = 'paid' THEN 0
            ELSE GREATEST(0, ROUND((COALESCE(item_totals.total_amount, 0) + COALESCE(item_totals.tax_amount, 0) + COALESCE(orders.round_off, 0))::numeric, 2) - COALESCE(pay_totals.received_amount, 0) - COALESCE(cn_totals.credited_amount, 0))
          END AS balance_due,
          COALESCE(pay_totals.payments_list, '[]'::json) AS payments_list,
          COALESCE(cn_totals.credit_notes_list, '[]'::json) AS credit_notes_list,
          COALESCE(item_totals.items, '[]') AS items
  FROM orders
  JOIN contacts ON contacts.id = orders.contact_id
  LEFT JOIN (
    SELECT oi.order_id,
           SUM(ROUND((oi.quantity * oi.unit_price)::numeric, 2)) AS total_amount,
           SUM(ROUND((oi.quantity * oi.unit_price * COALESCE(oi.gst_rate, 0) / 100.0)::numeric, 2)) AS tax_amount,
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
  LEFT JOIN (
    SELECT order_id, SUM(amount) AS received_amount,
           json_agg(json_build_object(
             'id', id,
             'amount', amount,
             'payment_mode', payment_mode,
             'method', method,
             'receipt_number', receipt_number,
             'reference_no', reference_no,
             'paid_at', paid_at
           ) ORDER BY paid_at ASC, id ASC) AS payments_list
    FROM payments
    WHERE order_id IS NOT NULL
    GROUP BY order_id
  ) pay_totals ON pay_totals.order_id = orders.id
  LEFT JOIN (
    SELECT order_id, SUM(total_amount) AS credited_amount,
           json_agg(json_build_object(
             'id', id,
             'credit_note_number', credit_note_number,
             'total_amount', total_amount,
             'credit_note_date', credit_note_date,
             'reason', reason
           ) ORDER BY credit_note_date ASC, id ASC) AS credit_notes_list
    FROM credit_notes
    WHERE order_id IS NOT NULL
    GROUP BY order_id
  ) cn_totals ON cn_totals.order_id = orders.id
`;

// Helper to calculate the next invoice number strictly from (latest existing invoice number + 1)
// combined with business settings
export async function calculateNextInvoiceNumber(clientOrPool, businessId = 1) {
  const targetId = businessId || 1;
  const profileRes = await clientOrPool.query(
    'SELECT invoice_prefix, invoice_next_number, invoice_padding FROM businesses WHERE id = $1',
    [targetId]
  );
  let profile = profileRes.rows[0];
  if (!profile) {
    const legacy = await clientOrPool.query('SELECT invoice_prefix, invoice_next_number, invoice_padding FROM business_profile WHERE id = 1');
    profile = legacy.rows[0] || {};
  }
  const prefix = (profile.invoice_prefix !== undefined && profile.invoice_prefix !== null) ? profile.invoice_prefix : 'INV-';
  const profileNext = Math.max(1, Number(profile.invoice_next_number) || 1);
  const padding = Number(profile.invoice_padding) || 0;

  // Query existing orders matching current prefix on invoice_number FOR THIS BUSINESS
  let maxInvoiceNum = 0;
  if (prefix) {
    const ordersRes = await clientOrPool.query(
      'SELECT invoice_number FROM orders WHERE business_id = $1 AND invoice_number LIKE $2',
      [targetId, `${prefix}%`]
    );
    for (const row of ordersRes.rows) {
      const val = (row.invoice_number || '').trim();
      const suffix = val.slice(prefix.length);
      const m = suffix.match(/^(\d+)$/);
      if (m) {
        const n = parseInt(m[1], 10);
        if (n > maxInvoiceNum) maxInvoiceNum = n;
      }
    }
  }

  const nextNum = Math.max(profileNext, maxInvoiceNum + 1);
  const numStr = padding > 1 ? String(nextNum).padStart(padding, '0') : String(nextNum);
  const suggestedNumber = `${prefix}${numStr}`;

  return { prefix, nextNumber: nextNum, padding, suggestedNumber, maxOrderNum: maxInvoiceNum };
}

// GET /api/orders/next-number — returns next suggested invoice number based on business profile settings + existing invoices
router.get('/next-number', async (req, res) => {
  try {
    const businessId = req.businessId || 1;
    const nextData = await calculateNextInvoiceNumber(pool, businessId);
    // Sync businesses table if it was lagging behind actual invoices
    if (nextData.nextNumber > (Number(nextData.nextNumber) || 1)) {
      await pool.query(
        'UPDATE businesses SET invoice_next_number = $1, updated_at = now() WHERE id = $2 AND invoice_next_number < $1',
        [nextData.nextNumber, businessId]
      );
    }
    res.json({
      prefix: nextData.prefix,
      nextNumber: nextData.nextNumber,
      padding: nextData.padding,
      suggestedNumber: nextData.suggestedNumber,
    });
  } catch (err) {
    console.error('Error in GET /next-number:', err);
    res.status(500).json({ error: 'Failed to get next invoice number' });
  }
});

// POST /api/orders — create an order or an invoice
router.post('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const {
    phone, name, city, call_id, notes, items, payment_status, payment_method,
    gstin, pan, billing_address, shipping_address, state,
    create_invoice, invoice_number, invoice_date, order_ref,
    place_of_supply, tax_mode,
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
      phone, name, city, gstin, pan, billing_address, shipping_address, state, business_id: businessId,
    });

    const bpRes = await client.query('SELECT state FROM businesses WHERE id = $1', [businessId]);
    const sellerState = (bpRes.rows[0]?.state || 'Gujarat').trim();
    const effectivePlaceOfSupply = (place_of_supply || state || contact.state || sellerState).trim();
    const effectiveTaxMode = tax_mode || (
      effectivePlaceOfSupply.toLowerCase() === sellerState.toLowerCase() ? 'split' : 'igst'
    );

    // Calculate subtotal, tax amount, and automatic cash round-off
    let calculatedSubtotal = 0;
    let calculatedTax = 0;
    for (const item of items) {
      const q = Number(item.quantity || 0);
      const p = Number(item.unit_price || 0);
      const g = Number(item.gst_rate || 0);
      const lineBase = Math.round(q * p * 100) / 100;
      const lineTax = Math.round(lineBase * (g / 100) * 100) / 100;
      calculatedSubtotal += lineBase;
      calculatedTax += lineTax;
    }
    const rawTotal = calculatedSubtotal + calculatedTax;
    const roundedTotal = Math.round(rawTotal);
    const calculatedRoundOff = Number((roundedTotal - rawTotal).toFixed(2));
    const effectiveRoundOff = (req.body.round_off !== undefined && req.body.round_off !== null)
      ? Number(req.body.round_off)
      : calculatedRoundOff;

    const paidNow = payment_status === 'paid';
    const effectiveInvoiceDate = invoice_date || new Date().toISOString().split('T')[0];

    // Determine final invoice number if creating an invoice
    let finalInvoiceNumber = null;
    if (create_invoice || invoice_number) {
      finalInvoiceNumber = (invoice_number || '').trim();
      if (finalInvoiceNumber) {
        const existing = await client.query('SELECT id FROM orders WHERE invoice_number = $1 AND business_id = $2', [finalInvoiceNumber, businessId]);
        if (existing.rows.length > 0) {
          await client.query('ROLLBACK');
          return res.status(400).json({ error: `Invoice number "${finalInvoiceNumber}" already exists. Please choose a different number.` });
        }

        const match = finalInvoiceNumber.match(/^(.*?)(\d+)$/);
        if (match) {
          const detectedPrefix = match[1];
          const digitStr = match[2];
          const num = parseInt(digitStr, 10);
          const padding = digitStr.length > 1 ? digitStr.length : 0;
          await client.query(
            `UPDATE businesses
             SET invoice_next_number = GREATEST(invoice_next_number, $1),
                 invoice_padding = $2,
                 invoice_prefix = COALESCE(NULLIF($3, ''), invoice_prefix),
                 updated_at = now()
             WHERE id = $4`,
            [num + 1, padding, detectedPrefix, businessId]
          );
        }
      } else {
        const nextData = await calculateNextInvoiceNumber(client, businessId);
        finalInvoiceNumber = nextData.suggestedNumber;
        await client.query(
          'UPDATE businesses SET invoice_next_number = $1, updated_at = now() WHERE id = $2',
          [nextData.nextNumber + 1, businessId]
        );
      }
    }

    // Insert order record with frozen place_of_supply, tax_mode, and round_off
    const orderResult = await client.query(
      `INSERT INTO orders (
         contact_id, call_id, order_number, invoice_number, invoice_date, order_ref, notes,
         payment_status, payment_method, place_of_supply, tax_mode, round_off, business_id
       )
       VALUES ($1, $2, '', $3, $4, $5, $6, $7, $8, $9, $10, $11, $12) RETURNING *`,
      [
        contact.id,
        call_id || null,
        finalInvoiceNumber,
        effectiveInvoiceDate,
        order_ref || null,
        notes || null,
        paidNow ? 'paid' : 'pending',
        payment_method || (paidNow ? 'other' : null),
        effectivePlaceOfSupply,
        effectiveTaxMode,
        effectiveRoundOff,
        businessId,
      ]
    );
    const order = orderResult.rows[0];

    // Standard Call Intelligence order number ORD-XXXXX
    const generatedOrderNumber = `ORD-${String(order.id).padStart(5, '0')}`;
    await client.query('UPDATE orders SET order_number = $1 WHERE id = $2', [generatedOrderNumber, order.id]);

    for (const item of items) {
      await client.query(
        `INSERT INTO order_items (order_id, product_name, quantity, unit_price, gst_rate)
         VALUES ($1, $2, $3, $4, $5)`,
        [order.id, item.product_name, item.quantity, item.unit_price, item.gst_rate || null]
      );
    }

    // If paid now, record the payment in payments table so the party ledger accurately reflects the credit
    if (paidNow) {
      const grandTotal = roundedTotal;
      if (grandTotal > 0) {
        const seqRes = await client.query(
          `SELECT COALESCE(MAX(NULLIF(regexp_replace(receipt_number, '\\D', '', 'g'), '')::INT), 0) + 1 AS next_seq
           FROM payments WHERE business_id = $1 AND receipt_number IS NOT NULL`,
          [businessId]
        );
        const receiptNum = `REC-${String(seqRes.rows[0]?.next_seq || 1).padStart(4, '0')}`;
        const pMode = (payment_method && ['cash', 'upi', 'bank_transfer', 'card', 'cheque'].includes(payment_method))
          ? payment_method
          : 'cash';

        let initialPayAt = new Date();
        if (effectiveInvoiceDate) {
          const parts = String(effectiveInvoiceDate).split('-').map(Number);
          if (parts.length === 3 && !parts.some(isNaN)) {
            const [y, m, d] = parts;
            const now = new Date();
            initialPayAt = new Date(y, m - 1, d, now.getHours(), now.getMinutes(), now.getSeconds(), now.getMilliseconds());
          }
        }

        await client.query(
          `INSERT INTO payments (order_id, contact_id, amount, payment_mode, method, paid_at, notes, receipt_number, business_id)
           VALUES ($1, $2, $3, $4, $4, $5, $6, $7, $8)`,
          [
            order.id,
            contact.id,
            grandTotal,
            pMode,
            initialPayAt.toISOString(),
            finalInvoiceNumber ? `Paid at invoice ${finalInvoiceNumber}` : `Paid at order ${generatedOrderNumber}`,
            receiptNum,
            businessId,
          ]
        );
      }
    } else {
      // Payment pending at creation → auto-remind the owner to collect it
      const dueAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
      await client.query(
        `INSERT INTO followups (contact_id, order_id, due_at, reason, channel, business_id)
         VALUES ($1, $2, $3, $4, 'push', $5)`,
        [contact.id, order.id, dueAt, `Collect payment for order ${generatedOrderNumber}${finalInvoiceNumber ? ` (Invoice ${finalInvoiceNumber})` : ''}`, businessId]
      );
    }

    await client.query('COMMIT');

    const full = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [order.id]);
    const createdOrder = full.rows[0];

    // Eager background sync to Cloud Storage (myBillBook / Zoho style)
    setImmediate(() => {
      syncInvoicePdf(order.id).catch((e) => console.warn('[Storage] Background invoice PDF upload failed:', e.message));
    });

    res.status(201).json(createdOrder);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to create order' });
  } finally {
    client.release();
  }
});

// GET /api/orders — filterable by payment_status / fulfillment_status / status / is_invoice / q (search)
router.get('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const { payment_status, fulfillment_status, status, is_invoice, q } = req.query;
  const conditions = [`orders.business_id = $1`];
  const values = [businessId];
  let i = 2;

  if (is_invoice === 'true') {
    conditions.push('orders.invoice_number IS NOT NULL');
  } else if (is_invoice === 'false') {
    conditions.push('orders.invoice_number IS NULL');
  }

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
  if (q && q.trim()) {
    const term = `%${q.trim()}%`;
    conditions.push(`(
      orders.order_number ILIKE $${i} OR
      orders.invoice_number ILIKE $${i} OR
      orders.order_ref ILIKE $${i} OR
      contacts.name ILIKE $${i} OR
      contacts.phone ILIKE $${i} OR
      contacts.city ILIKE $${i} OR
      COALESCE(orders.notes, '') ILIKE $${i} OR
      EXISTS (
        SELECT 1 FROM order_items
        WHERE order_items.order_id = orders.id
          AND order_items.product_name ILIKE $${i}
      )
    )`);
    values.push(term);
    i++;
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
  const businessId = req.businessId || 1;
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
      WHERE orders.status != 'cancelled' AND orders.business_id = $1
    `, [businessId]),
    pool.query(`
      SELECT COALESCE(SUM(order_items.quantity * order_items.unit_price), 0) AS amount
      FROM orders JOIN order_items ON order_items.order_id = orders.id
      WHERE orders.payment_status = 'pending' AND orders.status != 'cancelled' AND orders.business_id = $1
    `, [businessId]),
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
  const businessId = req.businessId || 1;
  const result = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1 AND orders.business_id = $2`, [req.params.id, businessId]);
  if (result.rows.length === 0) return res.status(404).json({ error: 'Order not found' });
  res.json(result.rows[0]);
});

// PATCH /api/orders/:id — update payment_status and/or fulfillment_status
router.patch('/:id', async (req, res) => {
  const businessId = req.businessId || 1;
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
  values.push(businessId);

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const result = await client.query(
      `UPDATE orders SET ${fields.join(', ')} WHERE id = $${i++} AND business_id = $${i} RETURNING id`,
      values
    );
    if (result.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    // Paid, or cancelled outright → the "collect payment" reminder is no longer needed.
    if (payment_status === 'paid') {
      const orderData = await client.query(
        `SELECT o.id, o.contact_id, o.payment_method,
                COALESCE(item_totals.grand_total, 0) AS grand_total,
                COALESCE(pay_totals.total_paid, 0) AS total_paid
         FROM orders o
         JOIN (
           SELECT order_id,
                  SUM(quantity * unit_price) + SUM(quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS grand_total
           FROM order_items GROUP BY order_id
         ) item_totals ON item_totals.order_id = o.id
         LEFT JOIN (
           SELECT order_id, SUM(amount) AS total_paid FROM payments WHERE order_id IS NOT NULL AND business_id = $2 GROUP BY order_id
         ) pay_totals ON pay_totals.order_id = o.id
         WHERE o.id = $1 AND o.business_id = $2`,
        [req.params.id, businessId]
      );

      if (orderData.rows.length > 0) {
        const row = orderData.rows[0];
        const unpaid = Math.max(0, Number(row.grand_total) - Number(row.total_paid));
        if (unpaid > 0.01) {
          const recNum = await getNextReceiptNumber(client, businessId);
          await client.query(
            `INSERT INTO payments (order_id, contact_id, amount, method, payment_mode, receipt_number, paid_at, notes, business_id)
             VALUES ($1, $2, $3, $4, $4, $5, now(), 'Settled on mark paid', $6)`,
            [
              req.params.id,
              row.contact_id,
              unpaid,
              payment_method || row.payment_method || 'cash',
              recNum,
              businessId,
            ]
          );
        }
      }

      await client.query(
        `UPDATE orders SET paid_at = COALESCE(paid_at, now()) WHERE id = $1 AND business_id = $2`,
        [req.params.id, businessId]
      );
      await client.query(
        `UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND business_id = $2 AND status = 'pending'`,
        [req.params.id, businessId]
      );
    } else if (status === 'cancelled') {
      await client.query(
        `UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND business_id = $2 AND status = 'pending'`,
        [req.params.id, businessId]
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

  const full = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1 AND orders.business_id = $2`, [req.params.id, businessId]);
  const updatedOrder = full.rows[0];

  // Invalidate and eagerly re-sync cached PDF in Google Cloud Storage when order is modified
  if (updatedOrder) {
    const invNum = updatedOrder.invoice_number || updatedOrder.order_number || updatedOrder.id;
    const business = await loadBusinessProfile(businessId);
    for (const paidFlag of ['', '-paid']) {
      const filename = `Tax-Invoice-${invNum}${paidFlag}.pdf`;
      const primaryKey = getDocumentStorageKey({ business, contactName: updatedOrder.name, docType: 'invoices', filename });
      const fallbackKeys = getDocumentFallbackKeys({ business, contactName: updatedOrder.name, docType: 'invoices', filename });
      for (const k of [primaryKey, ...fallbackKeys]) {
        storageService.deleteFile(k).catch(() => {});
      }
    }
    setImmediate(() => {
      syncInvoicePdf(updatedOrder.id).catch((e) => console.warn('[Storage] Background updated invoice PDF upload failed:', e.message));
    });
  }

  res.json(updatedOrder);
});

export async function loadFullOrder(id, businessId = null) {
  const where = businessId ? 'WHERE orders.id = $1 AND orders.business_id = $2' : 'WHERE orders.id = $1';
  const params = businessId ? [id, businessId] : [id];
  const result = await pool.query(`${ORDER_SELECT} ${where}`, params);
  return result.rows[0] || null;
}

async function loadBusinessProfile(businessId = 1) {
  const targetId = businessId || 1;
  const result = await pool.query('SELECT * FROM businesses WHERE id = $1', [targetId]);
  if (result.rows.length > 0) return result.rows[0];
  const legacy = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  return legacy.rows[0] || null;
}

// GET /api/orders/:id/pdf — stream the generated Tax Invoice PDF
router.get('/:id/pdf', async (req, res) => {
  try {
    const order = await loadFullOrder(req.params.id);
    if (!order) return res.status(404).json({ error: 'Invoice not found' });
    const businessId = order.business_id || req.businessId || 1;
    const business = await loadBusinessProfile(businessId);
    const showPaymentDetails = req.query.payment_details === 'true' || req.query.show_payments === 'true';
    const invoiceNum = order.invoice_number || order.order_number || order.id;
    const filename = `Tax-Invoice-${invoiceNum}${showPaymentDetails ? '-paid' : ''}.pdf`;
    const primaryKey = getDocumentStorageKey({
      business,
      contactName: order.name,
      docType: 'invoices',
      filename,
    });
    const fallbackKeys = getDocumentFallbackKeys({
      business,
      contactName: order.name,
      docType: 'invoices',
      filename,
    });

    // 1. Return cached PDF from object storage if present (checks primary, then fallback keys)
    const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
    if (cached) {
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader(
        'Content-Disposition',
        `inline; filename="Tax-Invoice-${invoiceNum}.pdf"`
      );
      return res.send(cached.buffer);
    }

    const html = buildInvoiceHtml({ business, order, items: order.items, showPaymentDetails });
    const pdf = await renderHtmlToPdf(html);

    // Asynchronously save to primary storage cache
    storageService.uploadFile({
      key: primaryKey,
      buffer: pdf,
      contentType: 'application/pdf',
      isPublic: true,
    }).catch((e) => console.warn('[Storage] Failed to cache invoice PDF in storage:', e.message));

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader(
      'Content-Disposition',
      `inline; filename="Tax-Invoice-${invoiceNum}.pdf"`
    );
    res.send(pdf);
  } catch (err) {
    console.error('Invoice PDF generation failed:', err);
    res.status(500).json({ error: 'Failed to generate PDF' });
  }
});

// POST /api/orders/:id/send-whatsapp — send invoice PDF via WhatsApp
router.post('/:id/send-whatsapp', async (req, res) => {
  try {
    const order = await loadFullOrder(req.params.id);
    if (!order) return res.status(404).json({ error: 'Invoice not found' });
    const businessId = order.business_id || req.businessId || 1;
    const business = await loadBusinessProfile(businessId);

    const host = req.get('host');
    const protocol = req.protocol;
    const pdfUrl = `${protocol}://${host}/api/orders/${order.id}/pdf`;

    const cleanPhone = (order.phone || '').replace(/\D/g, '');
    const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;

    const dateObj = order.created_at ? new Date(order.created_at) : (order.invoice_date ? new Date(order.invoice_date) : new Date());
    const invDate = order.invoice_date ? new Date(order.invoice_date).toLocaleDateString('en-IN') : dateObj.toLocaleDateString('en-IN');
    const invTime = dateObj.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase();
    const grandTotal = Number(order.total_amount) + Number(order.tax_amount || 0);
    const formattedAmount = grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    const pdfCaption = `📄 *Tax Invoice ${order.invoice_number || order.order_number}*\n\nDear *${order.name || 'Customer'}*,\n\nThank you for doing business with *${business?.name || 'us'}*! Please find your official Tax Invoice PDF attached above.\n\n💰 *Total Amount:* ₹${formattedAmount}\n📅 *Invoice Date & Time:* ${invDate} at ${invTime}\n📊 *Status:* ${order.payment_status === 'paid' ? 'Paid ✓' : 'Payment Pending'}\n\nBest regards,\n*${business?.name || 'Customer Support Team'}*`;

    const message = `Hello *${order.name || 'Customer'}*,\n\nPlease find attached your Tax Invoice *${order.order_number}* for *₹${formattedAmount}* from *${business?.name || 'us'}*.\n\nView or download PDF:\n${pdfUrl}\n\nThank you for your business!`;
    const whatsappUrl = `https://wa.me/${formattedPhone}?text=${encodeURIComponent(message)}`;

    // 1. Try sending actual PDF file attachment via WhatsApp Web Client (tokenless)
    let webResult = null;
    let webError = null;
    try {
      const { sendWhatsAppWebDocument } = await import('../notify/whatsappWebClient.js');
      const invoiceNum = order.invoice_number || order.order_number || order.id;
      const filename = `Tax-Invoice-${invoiceNum}.pdf`;
      const primaryKey = getDocumentStorageKey({
        business,
        contactName: order.name,
        docType: 'invoices',
        filename,
      });
      const fallbackKeys = getDocumentFallbackKeys({
        business,
        contactName: order.name,
        docType: 'invoices',
        filename,
      });
      const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
      let pdf = cached?.buffer || null;
      if (!pdf) {
        const html = buildInvoiceHtml({ business, order, items: order.items });
        pdf = await renderHtmlToPdf(html);
        storageService.uploadFile({ key: primaryKey, buffer: pdf, contentType: 'application/pdf', isPublic: true }).catch(() => {});
      }
      webResult = await sendWhatsAppWebDocument(order.phone, {
        pdfBuffer: pdf,
        filename: `Invoice-${order.order_number || order.id}.pdf`,
        caption: pdfCaption,
        businessId: order.business_id || business?.id || 1,
      });
    } catch (e) {
      webError = e.message;
      console.warn('WhatsApp Web send fallback:', e.message);
    }

    // 2. Try sending via Cloud API if configured
    let apiResult = { dryRun: true };
    try {
      const { isWhatsAppConfigured, sendWhatsAppDocument } = await import('../notify/whatsapp.js');
      if (isWhatsAppConfigured()) {
        const html = buildInvoiceHtml({ business, order, items: order.items });
        const pdf = await renderHtmlToPdf(html);
        apiResult = await sendWhatsAppDocument(order.phone, {
          pdfBuffer: pdf,
          filename: `Invoice-${order.order_number || order.id}.pdf`,
          caption: `Invoice ${order.order_number} from ${business?.name || 'us'}`,
        });
      }
    } catch (e) {
      console.warn('WhatsApp Cloud API notice:', e.message);
    }

    res.json({
      ok: true,
      sentViaWeb: Boolean(webResult?.sent),
      whatsappUrl,
      webError,
      ...apiResult,
      order: await loadFullOrder(order.id),
    });
  } catch (err) {
    console.error('Failed to send invoice via WhatsApp:', err.message);
    res.status(500).json({ error: err.message || 'Failed to send invoice via WhatsApp' });
  }
});

export default router;
