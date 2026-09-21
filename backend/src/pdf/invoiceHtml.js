import { amountToWords } from '../lib/numberToWords.js';

const ACCENT = '#2952e3';

function formatINR(amount) {
  return new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
    Number(amount)
  );
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

function multiline(text) {
  return escapeHtml(text).replace(/\n/g, '<br/>');
}

function formatPaymentDateTime(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return String(dateStr);
  const date = d.toLocaleDateString('en-GB');
  const time = d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase();
  return `${date}, ${time}`;
}

function formatMode(mode) {
  if (!mode) return '—';
  const m = String(mode).toLowerCase();
  if (m === 'upi') return 'UPI';
  if (m === 'cash') return 'Cash';
  if (m === 'bank_transfer' || m === 'bank') return 'Bank Transfer';
  if (m === 'cheque') return 'Cheque';
  if (m === 'card') return 'Card';
  if (m === 'credit') return 'Credit';
  return mode.charAt(0).toUpperCase() + mode.slice(1);
}

export function buildInvoiceHtml({ business, order, items, showPaymentDetails = false }) {
  const safeItems = Array.isArray(items) ? items : [];
  const subtotal = safeItems.reduce((sum, i) => sum + Number(i.quantity || 0) * Number(i.unit_price ?? i.rate ?? 0), 0);
  const totalQty = safeItems.reduce((sum, i) => sum + Number(i.quantity || 0), 0);
  const taxAmount = safeItems.reduce((sum, i) => sum + Number(i.quantity || 0) * Number(i.unit_price ?? i.rate ?? 0) * (Number(i.gst_rate || 0) / 100), 0);
  const total = subtotal + taxAmount;

  const rawPayments = Array.isArray(order?.payments_list)
    ? order.payments_list
    : (typeof order?.payments_list === 'string'
        ? JSON.parse(order.payments_list || '[]')
        : []);
  const payments = rawPayments.filter((p) => p && Number(p.amount) > 0);
  const totalPaidFromReceipts = payments.reduce((sum, p) => sum + Number(p.amount || 0), 0);

  const rawCreditNotes = Array.isArray(order?.credit_notes_list)
    ? order.credit_notes_list
    : (typeof order?.credit_notes_list === 'string'
        ? JSON.parse(order.credit_notes_list || '[]')
        : []);
  const creditNotes = rawCreditNotes.filter((cn) => cn && Number(cn.total_amount) > 0);
  const totalCreditedFromReturns = creditNotes.reduce((sum, cn) => sum + Number(cn.total_amount || 0), 0);

  const sellerState = (business?.state || '').trim().toLowerCase();
  const custState = (order?.state || '').trim().toLowerCase();
  const isSplit = order.tax_mode === 'split' ||
    (Boolean(sellerState && custState) && sellerState === custState) ||
    (!order?.state && true);
  const taxRatePct = subtotal > 0 ? (taxAmount / subtotal) * 100 : 0;

  const itemRows = safeItems
    .map((item, index) => {
      const rate = Number(item.unit_price ?? item.rate ?? 0);
      const qty = Number(item.quantity ?? 0);
      const gst = Number(item.gst_rate ?? 0);
      const lineBase = qty * rate;
      const lineTax = lineBase * (gst / 100);
      const lineTotal = lineBase + lineTax;
      return `
        <tr>
          <td class="center muted">${index + 1}</td>
          <td><strong>${escapeHtml(item.product_name)}</strong></td>
          <td class="center muted">${escapeHtml(item.hsn_code || '—')}</td>
          <td class="center">${qty} <span class="muted">${escapeHtml(item.unit || 'PCS')}</span></td>
          <td class="right">${formatINR(rate)}</td>
          <td class="right">${formatINR(lineTax)}<div class="tax-pct">(${gst}%)</div></td>
          <td class="right strong">${formatINR(lineTotal)}</td>
        </tr>`;
    })
    .join('');

  const taxRows = isSplit
    ? `
      <tr><td>CGST @${(taxRatePct / 2).toFixed(1)}%</td><td class="right">₹ ${formatINR(taxAmount / 2)}</td></tr>
      <tr><td>SGST @${(taxRatePct / 2).toFixed(1)}%</td><td class="right">₹ ${formatINR(taxAmount / 2)}</td></tr>`
    : `<tr><td>IGST @${taxRatePct.toFixed(1)}%</td><td class="right">₹ ${formatINR(taxAmount)}</td></tr>`;

  const signatureBlock = business?.signature_image
    ? `<img class="sig-image" src="${business.signature_image}" alt="Signature" />`
    : `<div class="sig-placeholder">Sign here</div>`;

  const dateObj = order.created_at ? new Date(order.created_at) : (order.invoice_date ? new Date(order.invoice_date) : new Date());
  const formattedDate = order.invoice_date
    ? new Date(order.invoice_date).toLocaleDateString('en-GB')
    : dateObj.toLocaleDateString('en-GB');
  const formattedTime = dateObj.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase();

  return `<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<style>
  * { box-sizing: border-box; }
  body {
    font-family: -apple-system, "Segoe UI", Arial, Helvetica, sans-serif;
    color: #1f2430; font-size: 12px; margin: 0; padding: 36px 40px 30px;
  }
  .topbar { height: 6px; background: ${ACCENT}; border-radius: 3px; margin-bottom: 22px; }
  .header { display: flex; justify-content: space-between; margin-bottom: 22px; }
  .seller h1 { font-size: 19px; margin: 0 0 6px; letter-spacing: -0.2px; }
  .seller div { margin-bottom: 3px; max-width: 380px; color: #444; }
  .seller strong { color: #1f2430; }
  .quo-title { font-size: 22px; font-weight: 800; text-align: right; margin-bottom: 6px; color: ${ACCENT}; letter-spacing: 1px; }
  .status-badge {
    display: inline-block; padding: 3px 10px; border-radius: 4px; font-weight: 700; font-size: 10px;
    letter-spacing: 0.4px; text-transform: uppercase; margin-bottom: 8px;
  }
  .status-paid { background: #dcfce7; color: #166534; border: 1px solid #86efac; }
  .status-partial { background: #ffedd5; color: #9a3412; border: 1px solid #fdba74; }
  .status-pending { background: #fef3c7; color: #92400e; border: 1px solid #fcd34d; }
  .quo-meta { text-align: right; }
  .quo-meta table { margin-left: auto; }
  .quo-meta td { padding: 3px 0 3px 18px; font-size: 12px; }
  .quo-meta td:first-child { color: #777; }
  .quo-meta td:last-child { font-weight: 700; }
  .parties { display: flex; gap: 24px; margin-bottom: 18px; }
  .party { flex: 1; border-top: 2px solid ${ACCENT}; padding-top: 10px; }
  .tag {
    display: inline-block; background: ${ACCENT}; color: white; padding: 3px 11px;
    border-radius: 3px; font-weight: 700; font-size: 10px; letter-spacing: 0.5px; margin-bottom: 8px;
  }
  .party h2 { font-size: 14px; margin: 0 0 4px; }
  .party div { margin-bottom: 2px; color: #444; }
  table.items { width: 100%; border-collapse: collapse; margin-bottom: 0; }
  table.items th {
    background: #1f2430; color: white; padding: 9px 8px; font-size: 10.5px;
    text-align: left; letter-spacing: 0.4px;
  }
  table.items th:first-child { border-radius: 4px 0 0 0; }
  table.items th:last-child { border-radius: 0 4px 0 0; }
  table.items td { padding: 9px 8px; border-bottom: 1px solid #eee; vertical-align: top; }
  .center { text-align: center; }
  .right { text-align: right; }
  .muted { color: #888; }
  .strong { font-weight: 700; }
  .tax-pct { color: #999; font-size: 9.5px; }
  .subtotal-row td { background: #f3f5fb; font-weight: 700; padding: 9px 8px; border-top: 2px solid ${ACCENT}; }
  .bottom { display: flex; justify-content: space-between; margin-top: 26px; gap: 28px; }
  .bottom-left { flex: 1; }
  .bottom-right { flex: 1; }
  .section-title { font-weight: 700; margin-bottom: 5px; color: ${ACCENT}; font-size: 11px; text-transform: uppercase; letter-spacing: 0.4px; }
  .bank-details { margin-top: 18px; }
  .bank-details table td { padding: 2px 0; vertical-align: top; }
  .bank-details table td:first-child { padding-right: 10px; color: #777; }
  .payments-history { margin-top: 16px; }
  .payments-history-table { width: 100%; border-collapse: collapse; margin-top: 6px; }
  .payments-history-table th {
    background: #f8fafc; color: #475569; font-size: 9px; font-weight: 700;
    padding: 5px 6px; border-bottom: 1.5px solid #cbd5e1; text-align: left;
    text-transform: uppercase; letter-spacing: 0.3px;
  }
  .payments-history-table td {
    padding: 5px 6px; border-bottom: 1px solid #f1f5f9; font-size: 10px; vertical-align: middle;
  }
  .payments-history-table .payments-total-row td {
    background: #f0fdf4; border-top: 1.5px solid #86efac; font-weight: 700; padding: 5px 6px;
  }
  .payments-history-table .right { text-align: right; }
  .returns-history { margin-top: 14px; }
  .returns-history-table { width: 100%; border-collapse: collapse; margin-top: 6px; }
  .returns-history-table th {
    background: #faf5ff; color: #6b21a8; font-size: 9px; font-weight: 700;
    padding: 5px 6px; border-bottom: 1.5px solid #d8b4fe; text-align: left;
    text-transform: uppercase; letter-spacing: 0.3px;
  }
  .returns-history-table td {
    padding: 5px 6px; border-bottom: 1px solid #f3e8ff; font-size: 10px; vertical-align: middle;
  }
  .returns-history-table .returns-total-row td {
    background: #f5f3ff; border-top: 1.5px solid #c084fc; font-weight: 700; padding: 5px 6px;
  }
  .returns-history-table .right { text-align: right; }
  .status-returned {
    background: #f3e8ff; color: #7e22ce; border: 1px solid #d8b4fe;
  }
  .status-partial-return {
    background: #fef3c7; color: #92400e; border: 1px solid #fde68a;
  }
  .totals-box table { width: 100%; }
  .totals-box td { padding: 4px 0; }
  .totals-box .total-row td { border-top: 2px solid #1f2430; font-weight: 800; font-size: 13.5px; padding-top: 8px; color: ${ACCENT}; }
  .words { margin-top: 18px; text-align: right; }
  .words .label { font-weight: 700; font-size: 10.5px; text-transform: uppercase; color: #777; letter-spacing: 0.4px; }
  .words div:last-child { margin-top: 2px; }
  .signature { margin-top: 40px; text-align: right; font-weight: 700; }
  .sig-image { max-height: 60px; max-width: 180px; display: block; margin: 0 0 6px auto; }
  .sig-placeholder {
    height: 60px; width: 180px; margin: 0 0 6px auto; margin-left: auto;
    border: 1px dashed #ccc; border-radius: 4px; display: flex; align-items: center; justify-content: center;
    color: #bbb; font-size: 10px; font-style: italic;
  }
  .footer-note { margin-top: 30px; text-align: center; color: #aaa; font-size: 9.5px; }
</style>
</head>
<body>
  <div class="topbar"></div>
  <div class="header">
    <div class="seller">
      <h1>${escapeHtml(business?.name || 'Your Business')}</h1>
      <div>${multiline(business?.address || '')}</div>
      ${business?.gstin ? `<div><strong>GSTIN:</strong> ${escapeHtml(business.gstin)}</div>` : ''}
      ${business?.mobile ? `<div><strong>Mobile:</strong> ${escapeHtml(business.mobile)}</div>` : ''}
      ${business?.pan ? `<div><strong>PAN:</strong> ${escapeHtml(business.pan)}</div>` : ''}
    </div>
    <div>
      <div class="quo-title">TAX INVOICE</div>
      ${showPaymentDetails ? (() => {
        let badgeClass = 'status-pending';
        let badgeText = 'PAYMENT PENDING';
        const isPaid = order.payment_status === 'paid';
        const isPartial = order.payment_status === 'partially_paid';

        if (totalCreditedFromReturns >= total - 0.01 && total > 0) {
          badgeClass = 'status-returned';
          badgeText = '100% RETURNED (CREDITED)';
        } else if (totalCreditedFromReturns > 0 && isPaid) {
          badgeClass = 'status-paid';
          badgeText = 'PAID (WITH SALES RETURN)';
        } else if (totalCreditedFromReturns > 0) {
          badgeClass = 'status-partial-return';
          badgeText = 'PARTIALLY RETURNED';
        } else if (isPaid) {
          badgeClass = 'status-paid';
          badgeText = 'PAID ✓';
        } else if (isPartial) {
          badgeClass = 'status-partial';
          badgeText = 'PARTIALLY PAID';
        }

        return `
        <div style="text-align: right; margin-bottom: 6px;">
          <span class="status-badge ${badgeClass}">${badgeText}</span>
        </div>`;
      })() : ''}
      <div class="quo-meta">
        <table>
          <tr><td>Invoice No.</td><td>${escapeHtml(order.invoice_number || order.order_number)}</td></tr>
          ${order.order_number && order.order_number !== order.invoice_number ? `<tr><td>Order No.</td><td>${escapeHtml(order.order_number)}</td></tr>` : ''}
          <tr><td>Date</td><td>${formattedDate}</td></tr>
          <tr><td>Time</td><td>${formattedTime}</td></tr>
          ${order.order_ref ? `<tr><td>Order / PO Ref</td><td>${escapeHtml(order.order_ref)}</td></tr>` : ''}
          ${order.quotation_id ? `<tr><td>Quotation Ref</td><td>Quote #${order.quotation_id}</td></tr>` : ''}
        </table>
      </div>
    </div>
  </div>

  <div class="parties">
    <div class="party">
      <span class="tag">BILL TO</span>
      <h2>${escapeHtml(order.name || 'Customer')}</h2>
      <div>${multiline(order.billing_address || order.city || '')}</div>
      ${order.phone ? `<div>Mobile: ${escapeHtml(order.phone)}</div>` : ''}
      ${order.gstin ? `<div>GSTIN: ${escapeHtml(order.gstin)}</div>` : ''}
      ${order.pan ? `<div>PAN: ${escapeHtml(order.pan)}</div>` : ''}
      ${order.state ? `<div>Place of Supply: ${escapeHtml(order.state)}</div>` : ''}
    </div>
    <div class="party">
      <span class="tag">SHIP TO</span>
      <div>${multiline(order.shipping_address || order.billing_address || order.city || 'Same as Billing Address')}</div>
    </div>
  </div>

  <table class="items">
    <thead>
      <tr>
        <th class="center">S.NO.</th>
        <th>ITEM</th>
        <th class="center">HSN</th>
        <th class="center">QTY</th>
        <th class="right">RATE</th>
        <th class="right">TAX</th>
        <th class="right">AMOUNT</th>
      </tr>
    </thead>
    <tbody>
      ${itemRows}
      <tr class="subtotal-row">
        <td colspan="3">SUBTOTAL</td>
        <td class="center">${totalQty}</td>
        <td class="center muted">—</td>
        <td class="right">₹ ${formatINR(taxAmount)}</td>
        <td class="right">₹ ${formatINR(total)}</td>
      </tr>
    </tbody>
  </table>

  <div class="bottom">
    <div class="bottom-left">
      <div class="section-title">Terms &amp; Conditions</div>
      <div>${multiline(order.notes || business?.terms_and_conditions || '')}</div>

      ${business?.bank_name ? `
      <div class="bank-details">
        <div class="section-title">Bank Details</div>
        <table>
          <tr><td>Name:</td><td>${escapeHtml(business.bank_name)}</td></tr>
          <tr><td>IFSC:</td><td>${escapeHtml(business.bank_ifsc || '')}</td></tr>
          <tr><td>Account No:</td><td>${escapeHtml(business.bank_account_no || '')}</td></tr>
          <tr><td>Bank:</td><td>${escapeHtml(business.bank_branch || '')}</td></tr>
        </table>
      </div>` : ''}

      ${showPaymentDetails ? `
      <div class="payments-history">
        <div class="section-title" style="margin-top: 14px;">Payment / Receipt History</div>
        ${payments.length > 0 ? `
        <table class="payments-history-table">
          <thead>
            <tr>
              <th>Receipt #</th>
              <th>Date &amp; Time</th>
              <th>Mode</th>
              <th class="right">Amount</th>
            </tr>
          </thead>
          <tbody>
            ${payments.map((p) => `
              <tr>
                <td><strong>${escapeHtml(p.receipt_number || ('REC-' + String(p.id).padStart(4, '0')))}</strong></td>
                <td>${formatPaymentDateTime(p.paid_at)}</td>
                <td>${formatMode(p.payment_mode || p.method)}${p.reference_no ? `<div class="muted" style="font-size: 8.5px;">Ref: ${escapeHtml(p.reference_no)}</div>` : ''}</td>
                <td class="right strong" style="color: #166534;">₹ ${formatINR(p.amount)}</td>
              </tr>
            `).join('')}
            <tr class="payments-total-row">
              <td colspan="3"><strong>Total Received</strong></td>
              <td class="right strong" style="color: #166534;">₹ ${formatINR(totalPaidFromReceipts)}</td>
            </tr>
          </tbody>
        </table>
        ` : `
        <div style="background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 4px; padding: 6px 10px; color: #64748b; font-size: 10.5px; margin-top: 4px;">
          ${order.payment_status === 'paid' && totalCreditedFromReturns === 0 ? 'Marked as Paid in Full ✓' : (totalCreditedFromReturns > 0 ? 'Settled via Sales Return' : 'No payment receipts recorded yet.')}
        </div>
        `}
      </div>

      ${creditNotes.length > 0 ? `
      <div class="returns-history">
        <div class="section-title" style="margin-top: 14px; color: #7e22ce;">Sales Returns / Credit Notes</div>
        <table class="returns-history-table">
          <thead>
            <tr>
              <th>Credit Note #</th>
              <th>Date</th>
              <th>Reason</th>
              <th class="right">Credited</th>
            </tr>
          </thead>
          <tbody>
            ${creditNotes.map((cn) => `
              <tr>
                <td><strong style="color: #7e22ce;">${escapeHtml(cn.credit_note_number || ('CN-' + String(cn.id)))}</strong></td>
                <td>${cn.credit_note_date ? new Date(cn.credit_note_date).toLocaleDateString('en-GB') : '—'}</td>
                <td>${escapeHtml(cn.reason || 'Sales Return')}</td>
                <td class="right strong" style="color: #7e22ce;">₹ ${formatINR(cn.total_amount)}</td>
              </tr>
            `).join('')}
            <tr class="returns-total-row">
              <td colspan="3"><strong style="color: #7e22ce;">Total Credited (Returns)</strong></td>
              <td class="right strong" style="color: #7e22ce;">₹ ${formatINR(totalCreditedFromReturns)}</td>
            </tr>
          </tbody>
        </table>
      </div>
      ` : ''}
      ` : ''}
    </div>
    <div class="bottom-right">
      <div class="totals-box">
        <table>
          <tr><td>Taxable Amount</td><td class="right">₹ ${formatINR(subtotal)}</td></tr>
          ${taxRows}
          <tr class="total-row"><td>Total Amount</td><td class="right">₹ ${formatINR(total)}</td></tr>
          ${showPaymentDetails ? (() => {
            const isPaid = order.payment_status === 'paid';
            const totalPaid = payments.length > 0
              ? totalPaidFromReceipts
              : (isPaid && totalCreditedFromReturns === 0 ? total : Number(order.received_amount || 0));
            const totalCredited = totalCreditedFromReturns;
            const balDue = (isPaid && totalPaid + totalCredited >= total - 0.01)
              ? 0
              : Math.max(0, total - totalPaid - totalCredited);

            return `
              ${totalPaid > 0 ? `<tr><td style="color: #16a34a; font-weight: 600;">Received Amount (Paid)</td><td class="right" style="color: #16a34a; font-weight: 600;">₹ ${formatINR(totalPaid)}</td></tr>` : ''}
              ${totalCredited > 0 ? `<tr><td style="color: #7e22ce; font-weight: 600;">Sales Return (Credited)</td><td class="right" style="color: #7e22ce; font-weight: 600;">₹ ${formatINR(totalCredited)}</td></tr>` : ''}
              <tr style="border-top: 1.5px solid #cbd5e1; font-weight: 700;">
                <td>Balance Due</td>
                <td class="right" style="color: ${balDue > 0 ? '#dc2626' : '#16a34a'};">₹ ${formatINR(balDue)}</td>
              </tr>
            `;
          })() : ''}
        </table>
      </div>
      <div class="words">
        <div class="label">Total Amount (in words)</div>
        <div>${amountToWords(total)}</div>
      </div>
      <div class="signature">
        ${signatureBlock}
        Authorised Signature for<br/>${escapeHtml(business?.name || '')}
      </div>
    </div>
  </div>

  <div class="footer-note">This is a computer-generated tax invoice.</div>
</body>
</html>`;
}
