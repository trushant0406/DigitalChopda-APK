import { Router } from 'express';
import { pool } from '../db.js';
import { findOrCreateContact } from '../lib/contacts.js';
import { buildInvoiceHtml } from '../pdf/invoiceHtml.js';
import { renderHtmlToPdf } from '../pdf/renderPdf.js';

const router = Router();

const ORDER_SELECT = `
  SELECT orders.*, contacts.name, contacts.phone, contacts.city,
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
`;

// Helper to calculate the next invoice number strictly from (latest existing invoice number + 1)
// combined with business_profile settings
export async function calculateNextInvoiceNumber(clientOrPool) {
  const profileRes = await clientOrPool.query(
    'SELECT invoice_prefix, invoice_next_number, invoice_padding FROM business_profile WHERE id = 1'
  );
  const profile = profileRes.rows[0] || {};
  const prefix = (profile.invoice_prefix !== undefined && profile.invoice_prefix !== null) ? profile.invoice_prefix : 'INV-';
  const profileNext = Math.max(1, Number(profile.invoice_next_number) || 1);
  const padding = Number(profile.invoice_padding) || 0;

  // Query existing orders matching current prefix on invoice_number
  let maxInvoiceNum = 0;
  if (prefix) {
    const ordersRes = await clientOrPool.query(
      'SELECT invoice_number FROM orders WHERE invoice_number LIKE $1',
      [`${prefix}%`]
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
    const nextData = await calculateNextInvoiceNumber(pool);
    // Sync business_profile if it was lagging behind actual invoices
    if (nextData.nextNumber > (Number(nextData.nextNumber) || 1)) {
      await pool.query(
        'UPDATE business_profile SET invoice_next_number = $1, updated_at = now() WHERE id = 1 AND invoice_next_number < $1',
        [nextData.nextNumber]
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
  const {
    phone, name, city, call_id, notes, items, payment_status, payment_method,
    gstin, pan, billing_address, shipping_address, state,
    create_invoice, invoice_number, invoice_date, order_ref,
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
      phone, name, city, gstin, pan, billing_address, shipping_address, state,
    });

    const paidNow = payment_status === 'paid';
    const effectiveInvoiceDate = invoice_date || new Date().toISOString().split('T')[0];

    // Determine final invoice number if creating an invoice
    let finalInvoiceNumber = null;
    if (create_invoice || invoice_number) {
      finalInvoiceNumber = (invoice_number || '').trim();
      if (finalInvoiceNumber) {
        const existing = await client.query('SELECT id FROM orders WHERE invoice_number = $1', [finalInvoiceNumber]);
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
    }

    // Insert order record (order_number initialized blank, then set to ORD-XXXXX)
    const orderResult = await client.query(
      `INSERT INTO orders (contact_id, call_id, order_number, invoice_number, invoice_date, order_ref, notes, payment_status, payment_method)
       VALUES ($1, $2, '', $3, $4, $5, $6, $7, $8) RETURNING *`,
      [
        contact.id,
        call_id || null,
        finalInvoiceNumber,
        effectiveInvoiceDate,
        order_ref || null,
        notes || null,
        paidNow ? 'paid' : 'pending',
        payment_method || (paidNow ? 'other' : null),
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

    // Payment pending at creation → auto-remind the owner to collect it
    if (!paidNow) {
      const dueAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
      await client.query(
        `INSERT INTO followups (contact_id, order_id, due_at, reason, channel)
         VALUES ($1, $2, $3, $4, 'push')`,
        [contact.id, order.id, dueAt, `Collect payment for order ${generatedOrderNumber}${finalInvoiceNumber ? ` (Invoice ${finalInvoiceNumber})` : ''}`]
      );
    }

    await client.query('COMMIT');

    const full = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [order.id]);
    res.status(201).json(full.rows[0]);
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
  const { payment_status, fulfillment_status, status, is_invoice, q } = req.query;
  const conditions = [];
  const values = [];
  let i = 1;

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
      WHERE orders.status != 'cancelled'
    `),
    pool.query(`
      SELECT COALESCE(SUM(order_items.quantity * order_items.unit_price), 0) AS amount
      FROM orders JOIN order_items ON order_items.order_id = orders.id
      WHERE orders.payment_status = 'pending' AND orders.status != 'cancelled'
    `),
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
  const result = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [req.params.id]);
  if (result.rows.length === 0) return res.status(404).json({ error: 'Order not found' });
  res.json(result.rows[0]);
});

// PATCH /api/orders/:id — update payment_status and/or fulfillment_status
router.patch('/:id', async (req, res) => {
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

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const result = await client.query(
      `UPDATE orders SET ${fields.join(', ')} WHERE id = $${i} RETURNING id`,
      values
    );
    if (result.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }

    // Paid, or cancelled outright → the "collect payment" reminder is no longer needed.
    if (payment_status === 'paid' || status === 'cancelled') {
      await client.query(
        `UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND status = 'pending'`,
        [req.params.id]
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

  const full = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [req.params.id]);
  res.json(full.rows[0]);
});

async function loadFullOrder(id) {
  const result = await pool.query(`${ORDER_SELECT} WHERE orders.id = $1`, [id]);
  return result.rows[0] || null;
}

async function loadBusinessProfile() {
  const result = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  return result.rows[0] || null;
}

// GET /api/orders/:id/pdf — stream the generated Tax Invoice PDF
router.get('/:id/pdf', async (req, res) => {
  try {
    const order = await loadFullOrder(req.params.id);
    if (!order) return res.status(404).json({ error: 'Invoice not found' });
    const business = await loadBusinessProfile();

    const html = buildInvoiceHtml({ business, order, items: order.items });
    const pdf = await renderHtmlToPdf(html);

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader(
      'Content-Disposition',
      `inline; filename="Tax-Invoice-${order.invoice_number || order.order_number || order.id}.pdf"`
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
    const business = await loadBusinessProfile();

    const host = req.get('host');
    const protocol = req.protocol;
    const pdfUrl = `${protocol}://${host}/api/orders/${order.id}/pdf`;

    const cleanPhone = (order.phone || '').replace(/\D/g, '');
    const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;

    const invDate = order.invoice_date ? new Date(order.invoice_date).toLocaleDateString('en-IN') : '';
    const grandTotal = Number(order.total_amount) + Number(order.tax_amount || 0);
    const formattedAmount = grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    const pdfCaption = `📄 *Tax Invoice ${order.order_number}*\n\nDear *${order.name || 'Customer'}*,\n\nThank you for doing business with *${business?.name || 'us'}*! Please find your official Tax Invoice PDF attached above.\n\n💰 *Total Amount:* ₹${formattedAmount}\n📅 *Invoice Date:* ${invDate}\n📊 *Status:* ${order.payment_status === 'paid' ? 'Paid ✓' : 'Payment Pending'}\n\nBest regards,\n*${business?.name || 'Customer Support Team'}*`;

    const message = `Hello *${order.name || 'Customer'}*,\n\nPlease find attached your Tax Invoice *${order.order_number}* for *₹${formattedAmount}* from *${business?.name || 'us'}*.\n\nView or download PDF:\n${pdfUrl}\n\nThank you for your business!`;
    const whatsappUrl = `https://wa.me/${formattedPhone}?text=${encodeURIComponent(message)}`;

    // 1. Try sending actual PDF file attachment via WhatsApp Web Client (tokenless)
    let webResult = null;
    let webError = null;
    try {
      const { sendWhatsAppWebDocument } = await import('../notify/whatsappWebClient.js');
      const html = buildInvoiceHtml({ business, order, items: order.items });
      const pdf = await renderHtmlToPdf(html);
      webResult = await sendWhatsAppWebDocument(order.phone, {
        pdfBuffer: pdf,
        filename: `Invoice-${order.order_number || order.id}.pdf`,
        caption: pdfCaption,
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
