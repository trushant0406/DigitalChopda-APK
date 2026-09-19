import { Router } from 'express';
import { pool } from '../db.js';
import { renderHtmlToPdf } from '../pdf/renderPdf.js';
import { buildStatementHtml } from '../pdf/statementHtml.js';
import { sendWhatsAppWebText, sendWhatsAppWebDocument } from '../notify/whatsappWebClient.js';

const router = Router();

async function loadBusinessProfile() {
  const res = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  return res.rows[0] || null;
}

// GET /api/credit/summary — overall business receivable totals
router.get('/summary', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT
        COALESCE(SUM(item_totals.grand_total), 0) +
        COALESCE((SELECT SUM(opening_balance) FROM contacts WHERE opening_balance_type = 'to_receive'), 0) AS total_sales_amount,
        COALESCE((SELECT SUM(amount) FROM payments), 0) AS total_collected
      FROM orders
      JOIN (
        SELECT order_id,
               SUM(quantity * unit_price) + SUM(quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS grand_total
        FROM order_items GROUP BY order_id
      ) item_totals ON item_totals.order_id = orders.id
      WHERE orders.status != 'cancelled'
    `);

    const row = result.rows[0] || {};
    const totalCreditAmount = Number(row.total_sales_amount || 0);
    const totalCollected = Number(row.total_collected || 0);
    res.json({
      totalCreditAmount,
      totalCollected,
      totalOutstanding: Math.max(0, totalCreditAmount - totalCollected),
    });
  } catch (err) {
    console.error('Error fetching credit summary:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/credit/customers — party list with current balances and search
router.get('/customers', async (req, res) => {
  const { q, status } = req.query;
  const conditions = [];
  const values = [];
  let i = 1;

  if (q) {
    conditions.push(`(khata.name ILIKE $${i} OR khata.phone ILIKE $${i})`);
    values.push(`%${q}%`);
    i += 1;
  }
  if (status === 'outstanding') {
    conditions.push('khata.outstanding > 0');
  } else if (status === 'settled') {
    conditions.push('khata.outstanding <= 0');
  } else if (status === 'over_limit') {
    conditions.push('khata.credit_limit > 0 AND khata.outstanding > khata.credit_limit');
  }
  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';

  try {
    const result = await pool.query(
      `SELECT * FROM (
         SELECT contacts.id, contacts.name, contacts.phone, contacts.credit_limit,
                contacts.gstin, contacts.city, contacts.state,
                contacts.opening_balance, contacts.opening_balance_type, contacts.opening_balance_date,
                COALESCE(order_totals.total_sales, 0) +
                  CASE WHEN contacts.opening_balance_type = 'to_receive' THEN contacts.opening_balance ELSE 0 END AS total_credit_amount,
                COALESCE(payment_totals.collected, 0) +
                  CASE WHEN contacts.opening_balance_type = 'to_pay' THEN contacts.opening_balance ELSE 0 END AS collected,
                (COALESCE(order_totals.total_sales, 0) +
                  CASE WHEN contacts.opening_balance_type = 'to_receive' THEN contacts.opening_balance ELSE 0 END) -
                (COALESCE(payment_totals.collected, 0) +
                  CASE WHEN contacts.opening_balance_type = 'to_pay' THEN contacts.opening_balance ELSE 0 END) AS outstanding
         FROM contacts
         LEFT JOIN (
           SELECT orders.contact_id,
                  SUM(item_totals.grand_total) AS total_sales
           FROM orders
           JOIN (
             SELECT order_id,
                    SUM(quantity * unit_price) + SUM(quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS grand_total
             FROM order_items GROUP BY order_id
           ) item_totals ON item_totals.order_id = orders.id
           WHERE orders.status != 'cancelled'
           GROUP BY orders.contact_id
         ) order_totals ON order_totals.contact_id = contacts.id
         LEFT JOIN (
           SELECT contact_id, SUM(amount) AS collected
           FROM payments
           WHERE contact_id IS NOT NULL
           GROUP BY contact_id
         ) payment_totals ON payment_totals.contact_id = contacts.id
         WHERE contacts.credit_limit > 0
            OR contacts.opening_balance > 0
            OR order_totals.total_sales > 0
            OR payment_totals.collected > 0
       ) khata
       ${where}
       ORDER BY khata.outstanding DESC NULLS LAST, khata.name ASC`,
      values
    );
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching credit customers:', err);
    res.status(500).json({ error: err.message });
  }
});

// Helper to assemble running balance ledger entries for a contact matching myBillBook format
async function getCustomerLedgerData(contactId) {
  const contactRes = await pool.query('SELECT * FROM contacts WHERE id = $1', [contactId]);
  if (contactRes.rows.length === 0) {
    throw new Error('Customer not found');
  }
  const customer = contactRes.rows[0];

  // 1. Fetch ALL sales orders/invoices for this customer (not cancelled)
  const ordersRes = await pool.query(
    `SELECT orders.id, orders.order_number, orders.invoice_number, orders.invoice_date, orders.created_at,
            orders.status, orders.payment_status,
            COALESCE(item_totals.subtotal, 0) AS subtotal,
            COALESCE(item_totals.tax_amount, 0) AS tax_amount,
            COALESCE(item_totals.grand_total, 0) AS grand_total,
            COALESCE(item_totals.items_summary, '') AS items_summary
     FROM orders
     JOIN (
       SELECT oi.order_id,
              SUM(oi.quantity * oi.unit_price) AS subtotal,
              SUM(oi.quantity * oi.unit_price * COALESCE(oi.gst_rate, 0) / 100) AS tax_amount,
              SUM(oi.quantity * oi.unit_price) + SUM(oi.quantity * oi.unit_price * COALESCE(oi.gst_rate, 0) / 100) AS grand_total,
              string_agg(oi.product_name || ' ×' || oi.quantity, ', ') AS items_summary
       FROM order_items oi GROUP BY oi.order_id
     ) item_totals ON item_totals.order_id = orders.id
     WHERE orders.contact_id = $1 AND orders.status != 'cancelled'
     ORDER BY COALESCE(orders.invoice_date, orders.created_at::date) ASC, orders.id ASC`,
    [contactId]
  );

  // 2. Fetch all payments recorded for this contact
  const paymentsRes = await pool.query(
    `SELECT p.*, o.invoice_number AS order_invoice_number, o.order_number
     FROM payments p
     LEFT JOIN orders o ON o.id = p.order_id
     WHERE p.contact_id = $1
     ORDER BY p.paid_at ASC, p.id ASC`,
    [contactId]
  );

  const rawList = [];

  // Row 0: Opening Balance if set
  const openingBal = Number(customer.opening_balance || 0);
  if (openingBal > 0) {
    const isToReceive = (customer.opening_balance_type || 'to_receive') === 'to_receive';
    let earliestTs = Date.now();
    for (const o of ordersRes.rows) {
      const t = new Date(o.invoice_date || o.created_at).getTime();
      if (t < earliestTs) earliestTs = t;
    }
    for (const p of paymentsRes.rows) {
      const t = new Date(p.paid_at).getTime();
      if (t < earliestTs) earliestTs = t;
    }
    const dt = customer.opening_balance_date || customer.created_at;
    const ts = earliestTs - 86400000; // 1 day before earliest transaction

    rawList.push({
      id: `open-${customer.id}`,
      ref_id: customer.id,
      date: dt,
      timestamp: ts,
      voucher: 'Opening Balance',
      sr_no: '—',
      payment_mode: '—',
      particulars: 'Opening Balance',
      debit: isToReceive ? openingBal : 0,
      credit: !isToReceive ? openingBal : 0,
    });
  }

  // Sales Invoices
  for (const o of ordersRes.rows) {
    let dt = o.created_at || o.invoice_date;
    if (o.invoice_date && o.created_at) {
      const invD = new Date(o.invoice_date);
      const crD = new Date(o.created_at);
      invD.setHours(crD.getHours(), crD.getMinutes(), crD.getSeconds(), crD.getMilliseconds());
      dt = invD.toISOString();
    }
    rawList.push({
      id: `inv-${o.id}`,
      ref_id: o.id,
      date: dt,
      timestamp: new Date(dt).getTime(),
      voucher: 'Sales Invoices',
      sr_no: o.invoice_number || o.order_number,
      payment_mode: '—',
      particulars: o.items_summary || 'Tax Invoice',
      debit: Number(o.grand_total || 0),
      credit: 0,
      status: o.payment_status,
    });
  }

  // Payments In
  for (const p of paymentsRes.rows) {
    let dt = p.paid_at;
    let payTs = new Date(dt).getTime();
    if (p.order_id) {
      const linkedOrder = ordersRes.rows.find(o => o.id === p.order_id);
      if (linkedOrder) {
        let orderTs = new Date(linkedOrder.created_at || linkedOrder.invoice_date).getTime();
        if (linkedOrder.invoice_date && linkedOrder.created_at) {
          const invD = new Date(linkedOrder.invoice_date);
          const crD = new Date(linkedOrder.created_at);
          invD.setHours(crD.getHours(), crD.getMinutes(), crD.getSeconds(), crD.getMilliseconds());
          orderTs = invD.getTime();
        }

        const pDate = new Date(dt);
        const isMidnight =
          (pDate.getUTCHours() === 18 && pDate.getUTCMinutes() === 30) ||
          (pDate.getHours() === 0 && pDate.getMinutes() === 0 && pDate.getSeconds() === 0);

        if (isMidnight || payTs <= orderTs) {
          payTs = orderTs + 1000 + (Number(p.id) % 1000);
          dt = new Date(payTs).toISOString();
        }
      }
    }
    const modeLabel = (p.payment_mode || p.method || 'Cash');
    const formattedMode = modeLabel.charAt(0).toUpperCase() + modeLabel.slice(1);
    rawList.push({
      id: `pay-${p.id}`,
      ref_id: p.id,
      date: dt,
      timestamp: payTs,
      voucher: 'Payment In',
      sr_no: p.receipt_number || `REC-${String(p.id).padStart(4, '0')}`,
      payment_mode: formattedMode,
      particulars: p.order_invoice_number ? `Against ${p.order_invoice_number}` : (p.reference_no ? `Ref: ${p.reference_no}` : (p.notes || 'Payment In')),
      debit: 0,
      credit: Number(p.amount || 0),
      reference_no: p.reference_no,
      notes: p.notes,
      whatsapp_sent: p.whatsapp_sent,
    });
  }

  // Sort chronologically ascending (oldest first for running balance)
  rawList.sort((a, b) => {
    if (a.timestamp !== b.timestamp) return a.timestamp - b.timestamp;
    // Tie-breaker 1: Sales Invoices (debit) before Payment In (credit) if exact same millisecond
    if (a.voucher !== b.voucher) {
      if (a.voucher === 'Sales Invoices') return -1;
      if (b.voucher === 'Sales Invoices') return 1;
    }
    // Tie-breaker 2: Numeric ref_id (earlier payment comes first in chronological order)
    const idA = Number(a.ref_id) || 0;
    const idB = Number(b.ref_id) || 0;
    if (idA !== idB) return idA - idB;
    // Tie-breaker 3: sr_no
    return (a.sr_no || '').localeCompare(b.sr_no || '', undefined, { numeric: true });
  });

  // Compute running balance
  let running = 0;
  let totalDebit = 0;
  let totalCredit = 0;

  const entries = rawList.map((entry) => {
    running = running + entry.debit - entry.credit;
    totalDebit += entry.debit;
    totalCredit += entry.credit;
    return {
      ...entry,
      balance: running,
      running_balance: running,
    };
  });

  const creditLimit = Number(customer.credit_limit || 0);
  const currentBalance = running;
  const availableCredit = creditLimit > 0 ? Math.max(0, creditLimit - currentBalance) : 0;

  // Calculate overdue amount (unpaid balance on invoices created > 30 days ago)
  const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).getTime();
  let overdueAmount = 0;
  if (currentBalance > 0) {
    for (const o of ordersRes.rows) {
      const dt = new Date(o.invoice_date || o.created_at).getTime();
      if (dt < thirtyDaysAgo && o.payment_status !== 'paid') {
        overdueAmount += Number(o.grand_total);
      }
    }
    overdueAmount = Math.min(overdueAmount, currentBalance);
  }

  return {
    customer,
    stats: {
      total_receivable_amount: Math.max(0, currentBalance),
      overdue_amount: overdueAmount,
      total_sales_amount: totalDebit,
      total_received_amount: totalCredit,
      current_balance: currentBalance,
      credit_limit: creditLimit,
      available_credit: availableCredit,
      is_over_limit: creditLimit > 0 && currentBalance > creditLimit,
    },
    entries,
  };
}

// GET /api/credit/customers/:id/ledger — detailed running passbook ledger
router.get('/customers/:id/ledger', async (req, res) => {
  try {
    const ledger = await getCustomerLedgerData(req.params.id);
    res.json(ledger);
  } catch (err) {
    console.error('Error fetching customer ledger:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/credit/customers/:id/unpaid-invoices — fetch open/unpaid bills for dropdown
router.get('/customers/:id/unpaid-invoices', async (req, res) => {
  try {
    const contactId = req.params.id;
    const result = await pool.query(
      `SELECT o.id, o.order_number, o.invoice_number, o.invoice_date, o.created_at, o.payment_status,
              COALESCE(item_totals.grand_total, 0) AS grand_total,
              COALESCE(pay_totals.total_paid, 0) AS total_paid,
              GREATEST(0, COALESCE(item_totals.grand_total, 0) - COALESCE(pay_totals.total_paid, 0)) AS balance_due
       FROM orders o
       JOIN (
         SELECT oi.order_id,
                SUM(oi.quantity * oi.unit_price) + SUM(oi.quantity * oi.unit_price * COALESCE(oi.gst_rate, 0) / 100) AS grand_total
         FROM order_items oi GROUP BY oi.order_id
       ) item_totals ON item_totals.order_id = o.id
       LEFT JOIN (
         SELECT order_id, SUM(amount) AS total_paid FROM payments WHERE order_id IS NOT NULL GROUP BY order_id
       ) pay_totals ON pay_totals.order_id = o.id
       WHERE o.contact_id = $1 AND o.status != 'cancelled' AND o.payment_status != 'paid'
       ORDER BY COALESCE(o.invoice_date, o.created_at::date) ASC, o.id ASC`,
      [contactId]
    );

    const unpaid = result.rows
      .map(r => ({
        ...r,
        grand_total: Number(r.grand_total || 0),
        total_paid: Number(r.total_paid || 0),
        balance_due: Number(r.balance_due || 0),
      }))
      .filter(r => r.balance_due > 0.01);

    res.json(unpaid);
  } catch (err) {
    console.error('Error fetching unpaid invoices:', err);
    res.status(500).json({ error: err.message });
  }
});

// Helper to generate next sequential receipt number REC-XXXX
export async function getNextReceiptNumber(client) {
  const result = await client.query(
    `SELECT receipt_number FROM payments WHERE receipt_number IS NOT NULL ORDER BY id DESC LIMIT 50`
  );
  let maxSeq = 0;
  for (const row of result.rows) {
    const match = (row.receipt_number || '').match(/(\d+)$/);
    if (match) {
      const num = parseInt(match[1], 10);
      if (num > maxSeq) maxSeq = num;
    }
  }
  return `REC-${String(maxSeq + 1).padStart(4, '0')}`;
}

// GET /api/credit/next-receipt-number — preview sequential receipt number
router.get('/next-receipt-number', async (req, res) => {
  try {
    const receiptNumber = await getNextReceiptNumber(pool);
    res.json({ receiptNumber });
  } catch (err) {
    console.error('Error getting next receipt number:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/credit/payment-in — record a payment on-account or against orders
router.post('/payment-in', async (req, res) => {
  const { contact_id, order_id, amount, payment_mode, reference_no, receipt_number, paid_at, notes, send_whatsapp } = req.body;

  if (!contact_id) return res.status(400).json({ error: 'contact_id is required' });
  const payAmount = Number(amount);
  if (!payAmount || payAmount <= 0) {
    return res.status(400).json({ error: 'A positive payment amount is required' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const contactRes = await client.query('SELECT * FROM contacts WHERE id = $1', [contact_id]);
    if (contactRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Customer contact not found' });
    }
    const customer = contactRes.rows[0];

    const receiptNumber = receipt_number?.trim() || (await getNextReceiptNumber(client));
    let primaryPayment = null;

    let finalPaidAt = paid_at ? new Date(paid_at) : new Date();
    if (isNaN(finalPaidAt.getTime())) {
      finalPaidAt = new Date();
    } else if (finalPaidAt.getSeconds() === 0 && finalPaidAt.getMilliseconds() === 0) {
      // If user selected date/minute without seconds, inject current real seconds so DB records proper timestamp
      const now = new Date();
      finalPaidAt.setSeconds(now.getSeconds(), now.getMilliseconds());
    }

    // CASE 1: Settle against a specific bill
    if (order_id) {
      const targetOrderRes = await client.query(
        `SELECT o.id, o.invoice_number, o.order_number,
                COALESCE(item_totals.grand_total, 0) AS grand_total,
                COALESCE(pay_totals.total_paid, 0) AS total_paid
         FROM orders o
         JOIN (
           SELECT order_id,
                  SUM(quantity * unit_price) + SUM(quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS grand_total
           FROM order_items GROUP BY order_id
         ) item_totals ON item_totals.order_id = o.id
         LEFT JOIN (
           SELECT order_id, SUM(amount) AS total_paid FROM payments WHERE order_id IS NOT NULL GROUP BY order_id
         ) pay_totals ON pay_totals.order_id = o.id
         WHERE o.id = $1 AND o.contact_id = $2`,
        [order_id, contact_id]
      );

      if (targetOrderRes.rows.length === 0) {
        await client.query('ROLLBACK');
        return res.status(404).json({ error: 'Selected bill not found for this customer' });
      }

      const orderRow = targetOrderRes.rows[0];
      const insertRes = await client.query(
        `INSERT INTO payments (contact_id, order_id, amount, payment_mode, method, reference_no, paid_at, notes, receipt_number)
         VALUES ($1, $2, $3, $4, $4, $5, $6, $7, $8)
         RETURNING *`,
        [
          contact_id,
          order_id,
          payAmount,
          payment_mode || 'cash',
          reference_no || null,
          finalPaidAt.toISOString(),
          notes || (orderRow.invoice_number ? `Settlement for ${orderRow.invoice_number}` : null),
          receiptNumber,
        ]
      );
      primaryPayment = insertRes.rows[0];

      const newTotalPaid = Number(orderRow.total_paid) + payAmount;
      if (newTotalPaid >= Number(orderRow.grand_total) - 0.01) {
        await client.query(`UPDATE orders SET payment_status = 'paid', paid_at = COALESCE(paid_at, now()), updated_at = now() WHERE id = $1`, [order_id]);
        await client.query(`UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND status = 'pending'`, [order_id]);
      } else {
        await client.query(`UPDATE orders SET payment_status = 'partially_paid', updated_at = now() WHERE id = $1`, [order_id]);
      }
    } else {
      // CASE 2: On-Account / Auto-allocate via FIFO to open unpaid orders
      const openOrders = await client.query(
        `SELECT orders.id, orders.invoice_number, orders.order_number,
                COALESCE(item_totals.grand_total, 0) AS grand_total,
                COALESCE(paid_totals.total_paid, 0) AS total_paid
         FROM orders
         JOIN (
           SELECT order_id,
                  SUM(quantity * unit_price) + SUM(quantity * unit_price * COALESCE(gst_rate, 0) / 100) AS grand_total
           FROM order_items GROUP BY order_id
         ) item_totals ON item_totals.order_id = orders.id
         LEFT JOIN (
           SELECT order_id, SUM(amount) AS total_paid FROM payments WHERE order_id IS NOT NULL GROUP BY order_id
         ) paid_totals ON paid_totals.order_id = orders.id
         WHERE orders.contact_id = $1 AND orders.status != 'cancelled' AND orders.payment_status != 'paid'
         ORDER BY COALESCE(orders.invoice_date, orders.created_at::date) ASC, orders.id ASC`,
        [contact_id]
      );

      let remainingToAllocate = payAmount;
      let isFirst = true;

      for (const order of openOrders.rows) {
        if (remainingToAllocate <= 0) break;
        const orderDue = Math.max(0, Number(order.grand_total) - Number(order.total_paid));
        if (orderDue > 0) {
          const canPay = Math.min(orderDue, remainingToAllocate);
          remainingToAllocate -= canPay;

          const recToUse = isFirst ? receiptNumber : await getNextReceiptNumber(client);
          isFirst = false;

          const ins = await client.query(
            `INSERT INTO payments (contact_id, order_id, amount, payment_mode, method, reference_no, paid_at, notes, receipt_number)
             VALUES ($1, $2, $3, $4, $4, $5, $6, $7, $8)
             RETURNING *`,
            [
              contact_id,
              order.id,
              canPay,
              payment_mode || 'cash',
              reference_no || null,
              finalPaidAt.toISOString(),
              notes || (order.invoice_number ? `Against ${order.invoice_number}` : 'Payment In'),
              recToUse,
            ]
          );
          if (!primaryPayment) primaryPayment = ins.rows[0];

          const newPaid = Number(order.total_paid) + canPay;
          if (newPaid >= Number(order.grand_total) - 0.01) {
            await client.query(`UPDATE orders SET payment_status = 'paid', paid_at = COALESCE(paid_at, now()), updated_at = now() WHERE id = $1`, [order.id]);
            await client.query(`UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND status = 'pending'`, [order.id]);
          } else {
            await client.query(`UPDATE orders SET payment_status = 'partially_paid', updated_at = now() WHERE id = $1`, [order.id]);
          }
        }
      }

      // If still money remaining (advance on-account) or no open orders
      if (remainingToAllocate > 0) {
        const recToUse = isFirst ? receiptNumber : await getNextReceiptNumber(client);
        const ins = await client.query(
          `INSERT INTO payments (contact_id, order_id, amount, payment_mode, method, reference_no, paid_at, notes, receipt_number)
           VALUES ($1, NULL, $2, $3, $3, $4, $5, $6, $7)
           RETURNING *`,
          [
            contact_id,
            remainingToAllocate,
            payment_mode || 'cash',
            reference_no || null,
            finalPaidAt.toISOString(),
            notes || 'On-account advance',
            recToUse,
          ]
        );
        if (!primaryPayment) primaryPayment = ins.rows[0];
      }
    }

    await client.query('COMMIT');

    let whatsappResult = { sent: false };
    if (send_whatsapp && customer.phone) {
      try {
        const profile = await loadBusinessProfile();
        const updatedLedger = await getCustomerLedgerData(contact_id);
        const closingBal = updatedLedger.stats.current_balance;
        const balLabel = closingBal > 0
          ? `₹${closingBal.toFixed(2)} (Due)`
          : closingBal < 0
          ? `₹${Math.abs(closingBal).toFixed(2)} (Advance)`
          : '₹0.00 (Settled)';

        const pDate = new Date();
        const formattedReceiptDateTime = `${pDate.toLocaleDateString('en-IN')} at ${pDate.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase()}`;
        const refLine = reference_no ? `\n📌 *Reference:* ${reference_no}` : '';
        const msg =
`🧾 *Payment Receipt: ${receiptNumber}*

Dear *${customer.name || 'Customer'}*,
We have received your payment of *₹${payAmount.toFixed(2)}* via *${(payment_mode || 'cash').toUpperCase()}*.
📅 *Date & Time:* ${formattedReceiptDateTime}${refLine}

💰 *Current Account Balance:* *${balLabel}*

Thank you for your business!
— *${profile?.company_name || 'Accounts Dept'}*`;

        await sendWhatsAppWebText(customer.phone, msg);
        if (primaryPayment?.id) {
          await pool.query(`UPDATE payments SET whatsapp_sent = true WHERE id = $1`, [primaryPayment.id]);
        }
        whatsappResult = { sent: true };
      } catch (waErr) {
        console.warn('Could not send WhatsApp receipt:', waErr.message);
        whatsappResult = { sent: false, error: waErr.message };
      }
    }

    res.status(201).json({
      success: true,
      payment: primaryPayment,
      receipt_number: receiptNumber,
      whatsapp: whatsappResult,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Failed to record payment-in:', err);
    res.status(500).json({ error: err.message });
  } finally {
    client.release();
  }
});

// POST /api/credit/customers/:id/reminder — send payment reminder on WhatsApp (Matches top button in myBillBook)
router.post('/customers/:id/reminder', async (req, res) => {
  try {
    const business = await loadBusinessProfile();
    const ledger = await getCustomerLedgerData(req.params.id);
    const { customer, stats } = ledger;

    if (!customer.phone) {
      return res.status(400).json({ error: 'Customer has no phone number saved' });
    }

    const dueAmount = stats.total_receivable_amount;
    if (dueAmount <= 0) {
      return res.status(400).json({ error: 'Customer has no outstanding balance due' });
    }

    const bankBlock = business?.bank_name
      ? `\n\n*Bank Payment Details:*\nBank: ${business.bank_name}\nA/C: ${business.bank_account_no || '—'}\nIFSC: ${business.bank_ifsc || '—'}`
      : '';

    const msg =
`🔔 *Payment Reminder from ${business?.company_name || 'Laduba Industries'}*

Dear *${customer.name || 'Customer'}*,
This is a gentle reminder that your current outstanding balance is *₹${new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2 }).format(dueAmount)}*.${bankBlock}

Kindly arrange for payment at your earliest convenience. If already paid, please ignore this reminder.

Thank you!
— *${business?.company_name || 'Accounts Team'}*`;

    await sendWhatsAppWebText(customer.phone, msg);
    res.json({ success: true, message: `Reminder sent to ${customer.phone}` });
  } catch (err) {
    console.error('Failed to send payment reminder:', err);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/credit/customers/:id/statement/pdf — generate royal blue Statement PDF
router.get('/customers/:id/statement/pdf', async (req, res) => {
  try {
    const business = await loadBusinessProfile();
    const ledger = await getCustomerLedgerData(req.params.id);

    const html = buildStatementHtml({
      business,
      customer: ledger.customer,
      stats: ledger.stats,
      entries: ledger.entries,
    });

    const pdfBuffer = await renderHtmlToPdf(html);
    const filename = `Statement-${(ledger.customer.name || 'Party').replace(/[^a-zA-Z0-9_-]/g, '_')}.pdf`;

    res.set({
      'Content-Type': 'application/pdf',
      'Content-Disposition': `inline; filename="${filename}"`,
      'Content-Length': pdfBuffer.length,
    });
    res.send(pdfBuffer);
  } catch (err) {
    console.error('Error rendering statement PDF:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/credit/customers/:id/statement/whatsapp — send Statement PDF to customer WhatsApp
router.post('/customers/:id/statement/whatsapp', async (req, res) => {
  try {
    const business = await loadBusinessProfile();
    const ledger = await getCustomerLedgerData(req.params.id);
    const { customer, stats } = ledger;

    if (!customer.phone) {
      return res.status(400).json({ error: 'Customer does not have a phone number' });
    }

    const html = buildStatementHtml({
      business,
      customer,
      stats,
      entries: ledger.entries,
    });

    const pdfBuffer = await renderHtmlToPdf(html);
    const filename = `Statement-${(customer.name || 'Party').replace(/[^a-zA-Z0-9_-]/g, '_')}.pdf`;

    const balText = stats.current_balance > 0
      ? `₹${stats.current_balance.toFixed(2)} (Due)`
      : stats.current_balance < 0
      ? `₹${Math.abs(stats.current_balance).toFixed(2)} (Advance)`
      : '₹0.00 (Settled)';

    const caption =
`📄 *Party Account Statement*
Dear *${customer.name || 'Customer'}*,
Please find attached your updated statement of accounts.

💰 *Net Balance:* *${balText}*
📊 *Total Invoiced:* ₹${stats.total_sales_amount.toFixed(2)}
💳 *Total Received:* ₹${stats.total_received_amount.toFixed(2)}

— *${business?.company_name || 'Accounts Team'}*`;

    await sendWhatsAppWebDocument(customer.phone, {
      pdfBuffer,
      filename,
      caption,
    });

    res.json({ success: true, message: `Statement PDF sent to ${customer.phone}` });
  } catch (err) {
    console.error('Failed to send statement via WhatsApp:', err);
    res.status(500).json({ error: err.message });
  }
});

// Legacy backward-compatibility endpoint for single order collect
router.post('/collect', async (req, res) => {
  const { order_id, amount, method, notes } = req.body;
  if (!order_id || !amount || Number(amount) <= 0) {
    return res.status(400).json({ error: 'order_id and a positive amount are required' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const orderResult = await client.query('SELECT * FROM orders WHERE id = $1', [order_id]);
    if (orderResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Order not found' });
    }
    const order = orderResult.rows[0];

    const receiptNumber = await getNextReceiptNumber(client);

    await client.query(
      `INSERT INTO payments (order_id, contact_id, amount, payment_mode, method, notes, receipt_number)
       VALUES ($1, $2, $3, $4, $4, $5, $6)`,
      [order_id, order.contact_id, amount, method || 'cash', notes || null, receiptNumber]
    );

    const totals = await client.query(
      `SELECT
         COALESCE((SELECT SUM(quantity * unit_price) + SUM(quantity * unit_price * COALESCE(gst_rate, 0) / 100) FROM order_items WHERE order_id = $1), 0) AS total,
         COALESCE((SELECT SUM(amount) FROM payments WHERE order_id = $1), 0) AS collected`,
      [order_id]
    );
    const total = Number(totals.rows[0].total);
    const collected = Number(totals.rows[0].collected);

    if (collected >= total - 0.01) {
      await client.query(`UPDATE orders SET payment_status = 'paid', updated_at = now() WHERE id = $1`, [order_id]);
      await client.query(
        `UPDATE followups SET status = 'cancelled' WHERE order_id = $1 AND status = 'pending'`,
        [order_id]
      );
    }

    await client.query('COMMIT');
    res.status(201).json({ total, collected, outstanding: Math.max(total - collected, 0) });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to record payment' });
  } finally {
    client.release();
  }
});

export default router;
