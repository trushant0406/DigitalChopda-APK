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

export function buildQuotationHtml({ business, quotation, items }) {
  const subtotal = items.reduce((sum, i) => sum + Number(i.quantity) * Number(i.rate), 0);
  const totalQty = items.reduce((sum, i) => sum + Number(i.quantity), 0);
  const taxAmount = items.reduce((sum, i) => sum + Number(i.quantity) * Number(i.rate) * (Number(i.gst_rate) / 100), 0);
  const total = subtotal + taxAmount;
  const isSplit = quotation.tax_mode === 'split';
  const taxRatePct = subtotal > 0 ? (taxAmount / subtotal) * 100 : 0;

  const itemRows = items
    .map((item, index) => {
      const lineBase = Number(item.quantity) * Number(item.rate);
      const lineTax = lineBase * (Number(item.gst_rate) / 100);
      const lineTotal = lineBase + lineTax;
      return `
        <tr>
          <td class="center muted">${index + 1}</td>
          <td>${escapeHtml(item.product_name)}</td>
          <td class="center muted">${escapeHtml(item.hsn_code || '—')}</td>
          <td class="center">${Number(item.quantity)} <span class="muted">${escapeHtml(item.unit || 'PCS')}</span></td>
          <td class="right">${formatINR(item.rate)}</td>
          <td class="right">${formatINR(lineTax)}<div class="tax-pct">(${Number(item.gst_rate)}%)</div></td>
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
  .quo-title { font-size: 22px; font-weight: 800; text-align: right; margin-bottom: 12px; color: ${ACCENT}; letter-spacing: 1px; }
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
      <div class="quo-title">QUOTATION</div>
      <div class="quo-meta">
        <table>
          <tr><td>Quotation No.</td><td>${quotation.id}</td></tr>
          <tr><td>Date</td><td>${new Date(quotation.quotation_date).toLocaleDateString('en-GB')}</td></tr>
        </table>
      </div>
    </div>
  </div>

  <div class="parties">
    <div class="party">
      <span class="tag">BILL TO</span>
      <h2>${escapeHtml(quotation.bill_to_name)}</h2>
      <div>${multiline(quotation.bill_to_address || '')}</div>
      ${quotation.bill_to_mobile ? `<div>Mobile: ${escapeHtml(quotation.bill_to_mobile)}</div>` : ''}
      ${quotation.bill_to_gstin ? `<div>GSTIN: ${escapeHtml(quotation.bill_to_gstin)}</div>` : ''}
      ${quotation.bill_to_pan ? `<div>PAN: ${escapeHtml(quotation.bill_to_pan)}</div>` : ''}
      ${quotation.place_of_supply ? `<div>Place of Supply: ${escapeHtml(quotation.place_of_supply)}</div>` : ''}
    </div>
    <div class="party">
      <span class="tag">SHIP TO</span>
      <h2>${escapeHtml(quotation.ship_to_name || quotation.bill_to_name)}</h2>
      <div>${multiline(quotation.ship_to_address || quotation.bill_to_address || '')}</div>
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
        <td class="right">₹ ${formatINR(subtotal / (totalQty || 1))}</td>
        <td class="right">₹ ${formatINR(taxAmount)}</td>
        <td class="right">₹ ${formatINR(total)}</td>
      </tr>
    </tbody>
  </table>

  <div class="bottom">
    <div class="bottom-left">
      <div class="section-title">Terms &amp; Conditions</div>
      <div>${multiline(quotation.terms_and_conditions || '')}</div>

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
    </div>
    <div class="bottom-right">
      <div class="totals-box">
        <table>
          <tr><td>Taxable Amount</td><td class="right">₹ ${formatINR(subtotal)}</td></tr>
          ${taxRows}
          <tr class="total-row"><td>Total Amount</td><td class="right">₹ ${formatINR(total)}</td></tr>
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

  <div class="footer-note">This is a computer-generated quotation.</div>
</body>
</html>`;
}
