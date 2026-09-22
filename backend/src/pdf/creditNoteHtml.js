import { amountToWords } from '../lib/numberToWords.js';

const ACCENT = '#7c3aed'; // Royal purple for Credit Note / Return vouchers

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

export function buildCreditNoteHtml({ business, creditNote, items, contact }) {
  const safeItems = Array.isArray(items) ? items : [];
  const subtotal = safeItems.reduce((sum, i) => sum + Number(i.quantity || 0) * Number(i.unit_price || 0), 0);
  const totalQty = safeItems.reduce((sum, i) => sum + Number(i.quantity || 0), 0);
  const taxAmount = safeItems.reduce((sum, i) => sum + Number(i.quantity || 0) * Number(i.unit_price || 0) * (Number(i.gst_rate || 0) / 100), 0);
  const total = subtotal + taxAmount;

  const sellerState = (business?.state || '').trim().toLowerCase();
  const custState = (contact?.state || '').trim().toLowerCase();
  const isSplit = creditNote.tax_mode === 'split' ||
    (Boolean(sellerState && custState) && sellerState === custState) ||
    (!contact?.state && true);
  const taxRatePct = subtotal > 0 ? (taxAmount / subtotal) * 100 : 0;

  const itemRows = safeItems
    .map((item, index) => {
      const rate = Number(item.unit_price || 0);
      const qty = Number(item.quantity || 0);
      const gst = Number(item.gst_rate || 0);
      const lineBase = qty * rate;
      const lineTax = lineBase * (gst / 100);
      const lineTotal = lineBase + lineTax;
      return `
        <tr>
          <td class="center muted">${index + 1}</td>
          <td><strong>${escapeHtml(item.product_name)}</strong></td>
          <td class="center muted">${escapeHtml(item.hsn_code || '—')}</td>
          <td class="center">${qty} <span class="muted">PCS</span></td>
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
    : `<div class="sig-placeholder">Authorized Signatory</div>`;

  const formattedCnDate = creditNote.credit_note_date
    ? new Date(creditNote.credit_note_date).toLocaleDateString('en-GB')
    : new Date().toLocaleDateString('en-GB');

  const formattedInvDate = creditNote.original_invoice_date
    ? new Date(creditNote.original_invoice_date).toLocaleDateString('en-GB')
    : null;

  return `<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<style>
  * { box-sizing: border-box; }
  body {
    font-family: "Noto Sans", "DejaVu Sans", -apple-system, "Segoe UI", Arial, Helvetica, sans-serif;
    color: #1f2430; font-size: 12px; margin: 0; padding: 36px 40px 30px;
  }
  .topbar { height: 6px; background: ${ACCENT}; border-radius: 3px; margin-bottom: 22px; }
  .header { display: flex; justify-content: space-between; margin-bottom: 22px; }
  .seller h1 { font-size: 19px; margin: 0 0 6px; letter-spacing: -0.2px; }
  .seller div { margin-bottom: 3px; max-width: 380px; color: #444; }
  .seller strong { color: #1f2430; }
  .cn-title { font-size: 22px; font-weight: 800; text-align: right; margin-bottom: 4px; color: ${ACCENT}; letter-spacing: 1px; }
  .cn-subtitle { font-size: 11px; font-weight: 700; color: #64748b; text-align: right; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
  .cn-badge {
    display: inline-block; padding: 4px 12px; border-radius: 4px; font-weight: 700; font-size: 10px;
    letter-spacing: 0.5px; text-transform: uppercase; margin-bottom: 8px;
    background: #f5f3ff; color: #6d28d9; border: 1px solid #ddd6fe;
  }
  .meta-table { margin-left: auto; border-collapse: collapse; }
  .meta-table td { padding: 3px 0 3px 16px; font-size: 12px; text-align: right; }
  .meta-table td:first-child { color: #64748b; font-weight: 500; }
  .meta-table td:last-child { font-weight: 700; color: #0f172a; }
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
  .subtotal-row td { background: #faf5ff; font-weight: 700; padding: 9px 8px; border-top: 2px solid ${ACCENT}; }
  .bottom { display: flex; justify-content: space-between; margin-top: 26px; gap: 28px; }
  .bottom-left { flex: 1; }
  .bottom-right { flex: 1; }
  .section-title { font-weight: 700; margin-bottom: 5px; color: ${ACCENT}; font-size: 11px; text-transform: uppercase; letter-spacing: 0.4px; }
  .totals { width: 100%; border-collapse: collapse; }
  .totals td { padding: 5px 0; }
  .totals tr.grand td {
    font-size: 14px; font-weight: 800; border-top: 2px solid #1f2430;
    border-bottom: 2px solid #1f2430; padding: 9px 0; color: ${ACCENT};
  }
  .words-block {
    background: #faf5ff; border: 1px solid #e9d5ff; border-radius: 4px;
    padding: 8px 12px; margin-top: 14px; font-size: 11px; color: #4c1d95;
  }
  .reason-box {
    background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px;
    padding: 10px 14px; margin-bottom: 16px; font-size: 11.5px;
  }
  .signature-area { margin-top: 24px; text-align: right; }
  .sig-image { max-height: 55px; max-width: 170px; object-fit: contain; }
  .sig-placeholder {
    height: 48px; border-bottom: 1px dashed #bbb; display: inline-block;
    width: 170px; margin-bottom: 4px; line-height: 48px; color: #999; text-align: center;
  }
  .footer {
    margin-top: 28px; padding-top: 10px; border-top: 1px solid #eee;
    font-size: 9.5px; color: #888; text-align: center;
  }
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
    <div style="text-align: right;">
      <div class="cn-title">CREDIT NOTE</div>
      <div class="cn-subtitle">Sales Return / Price Adjustment</div>
      <div class="cn-badge">CREDITED TO PARTY ACCOUNT</div>
      <table class="meta-table">
        <tr><td>Credit Note No.</td><td>${escapeHtml(creditNote.credit_note_number)}</td></tr>
        <tr><td>Credit Note Date</td><td>${formattedCnDate}</td></tr>
        ${creditNote.original_invoice_no ? `<tr><td>Original Invoice No.</td><td>${escapeHtml(creditNote.original_invoice_no)}</td></tr>` : ''}
        ${formattedInvDate ? `<tr><td>Original Invoice Date</td><td>${formattedInvDate}</td></tr>` : ''}
        <tr><td>Reason</td><td><strong>${escapeHtml(creditNote.reason || 'Sales Return')}</strong></td></tr>
      </table>
    </div>
  </div>

  <div class="parties">
    <div class="party">
      <span class="tag">CREDITED TO (PARTY)</span>
      <h2>${escapeHtml(contact.name || 'Customer')}</h2>
      <div>${multiline(contact.billing_address || contact.city || '')}</div>
      ${contact.phone ? `<div>Mobile: ${escapeHtml(contact.phone)}</div>` : ''}
      ${contact.gstin ? `<div>GSTIN: ${escapeHtml(contact.gstin)}</div>` : ''}
      ${contact.pan ? `<div>PAN: ${escapeHtml(contact.pan)}</div>` : ''}
      ${contact.state ? `<div>Place of Supply: ${escapeHtml(contact.state)}</div>` : ''}
    </div>
    <div class="party">
      <span class="tag">RETURN REFERENCE &amp; REASON</span>
      <div class="reason-box">
        <div><strong>Reason for Issue:</strong> ${escapeHtml(creditNote.reason || 'Sales Return')}</div>
        ${creditNote.original_invoice_no ? `<div style="margin-top: 4px;"><strong>Original Invoice:</strong> ${escapeHtml(creditNote.original_invoice_no)}</div>` : ''}
        ${creditNote.notes ? `<div style="margin-top: 4px; color: #475569;">${escapeHtml(creditNote.notes)}</div>` : ''}
        <div style="margin-top: 6px; font-size: 11px; color: #7c3aed; font-weight: 600;">
          ✓ This amount has been credited to customer's ledger, reducing outstanding balance.
        </div>
      </div>
    </div>
  </div>

  <table class="items">
    <thead>
      <tr>
        <th class="center">S.NO.</th>
        <th>RETURNED ITEM</th>
        <th class="center">HSN</th>
        <th class="center">QTY RETURNED</th>
        <th class="right">RATE</th>
        <th class="right">TAX REVERSED</th>
        <th class="right">CREDIT AMOUNT</th>
      </tr>
    </thead>
    <tbody>
      ${itemRows}
      <tr class="subtotal-row">
        <td colspan="3">TOTAL RETURNED</td>
        <td class="center">${totalQty}</td>
        <td class="center muted">—</td>
        <td class="right">₹ ${formatINR(taxAmount)}</td>
        <td class="right">₹ ${formatINR(total)}</td>
      </tr>
    </tbody>
  </table>

  <div class="bottom">
    <div class="bottom-left">
      <div class="words-block">
        <strong>Total Credit Amount in Words:</strong><br/>
        ${escapeHtml(amountToWords(Math.round(total)))}
      </div>

      ${business?.terms_and_conditions ? `
      <div style="margin-top: 16px;">
        <div class="section-title">Terms &amp; Conditions</div>
        <div style="color: #666; font-size: 11px;">${multiline(business.terms_and_conditions)}</div>
      </div>` : ''}
    </div>

    <div class="bottom-right">
      <table class="totals">
        <tr><td>Return Taxable Subtotal</td><td class="right">₹ ${formatINR(subtotal)}</td></tr>
        ${taxRows}
        <tr class="grand"><td>TOTAL CREDIT AMOUNT</td><td class="right">₹ ${formatINR(total)}</td></tr>
      </table>

      <div class="signature-area">
        <div>For <strong>${escapeHtml(business?.name || 'Your Business')}</strong></div>
        ${signatureBlock}
        <div style="font-size: 10px; color: #777;">Authorized Signatory</div>
      </div>
    </div>
  </div>

  <div class="footer">
    This is a Computer Generated GST Credit Note issued in accordance with Section 34 of the CGST Act.
  </div>
</body>
</html>`;
}
