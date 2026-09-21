import { amountToWords } from '../lib/numberToWords.js';

const ACCENT = '#059669'; // Emerald green for payment vouchers / receipts

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

export function buildPaymentReceiptHtml({ business, payment, contact, order, closingBalance }) {
  const receiptNo = payment.receipt_number || `REC-${String(payment.id).padStart(4, '0')}`;
  const amount = Number(payment.amount || 0);
  const amountWords = amountToWords(amount);
  const mode = (payment.payment_mode || payment.method || 'Cash').toUpperCase();
  const dateObj = new Date(payment.paid_at || Date.now());
  const formattedDate = dateObj.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  const formattedTime = dateObj.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });

  const signatureBlock = business?.signature_image
    ? `<img class="sig-image" src="${business.signature_image}" alt="Signature" />`
    : `<div class="sig-placeholder"></div>`;

  const hasBankDetails = Boolean(business?.bank_name || business?.bank_account_no || business?.upi_id);
  const balanceVal = closingBalance !== undefined && closingBalance !== null ? Number(closingBalance) : null;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Payment Receipt - ${escapeHtml(receiptNo)}</title>
  <style>
    @page {
      size: A4 portrait;
      margin: 14mm 16mm 14mm 16mm;
    }
    * { box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      color: #0f172a;
      background: #ffffff;
      margin: 0;
      padding: 0;
      font-size: 13px;
      line-height: 1.5;
    }
    .receipt-container {
      max-width: 100%;
      margin: 0 auto;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      overflow: hidden;
    }
    .top-banner {
      background: linear-gradient(135deg, #065f46 0%, #059669 100%);
      color: #ffffff;
      padding: 20px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .top-banner .title {
      font-size: 22px;
      font-weight: 800;
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }
    .top-banner .subtitle {
      font-size: 11.5px;
      color: #a7f3d0;
      margin-top: 3px;
    }
    .top-banner .receipt-tag {
      background: rgba(255, 255, 255, 0.18);
      border: 1px solid rgba(255, 255, 255, 0.35);
      border-radius: 6px;
      padding: 8px 14px;
      text-align: right;
    }
    .top-banner .receipt-tag .number {
      font-size: 17px;
      font-weight: 800;
      letter-spacing: 0.5px;
    }
    .top-banner .receipt-tag .date {
      font-size: 11px;
      color: #d1fae5;
      margin-top: 2px;
    }

    .parties-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      border-bottom: 1px solid #e2e8f0;
    }
    .party-card {
      padding: 18px 22px;
    }
    .party-card.left {
      border-right: 1px solid #e2e8f0;
    }
    .party-card .label {
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #059669;
      margin-bottom: 6px;
    }
    .party-card .name {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      margin-bottom: 4px;
    }
    .party-card .detail {
      font-size: 12px;
      color: #475569;
      margin: 2px 0;
    }
    .party-card .badge {
      display: inline-block;
      font-size: 10.5px;
      font-weight: 600;
      background: #f1f5f9;
      color: #334155;
      padding: 2px 6px;
      border-radius: 4px;
      margin-top: 4px;
    }

    /* Highlight Amount Box */
    .amount-highlight-box {
      background: #ecfdf5;
      border-top: 2px solid #059669;
      border-bottom: 1px solid #a7f3d0;
      padding: 20px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .amount-highlight-box .left-info .label {
      font-size: 11.5px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 700;
      color: #065f46;
    }
    .amount-highlight-box .left-info .words {
      font-size: 12.5px;
      font-weight: 600;
      color: #047857;
      margin-top: 4px;
      max-width: 420px;
    }
    .amount-highlight-box .right-figure {
      text-align: right;
    }
    .amount-highlight-box .right-figure .val {
      font-size: 26px;
      font-weight: 800;
      color: #065f46;
    }
    .amount-highlight-box .right-figure .mode-pill {
      display: inline-block;
      font-size: 11px;
      font-weight: 700;
      background: #059669;
      color: #ffffff;
      padding: 2px 8px;
      border-radius: 12px;
      margin-top: 4px;
      text-transform: uppercase;
    }

    /* Details Table */
    .details-table {
      width: 100%;
      border-collapse: collapse;
    }
    .details-table th, .details-table td {
      padding: 11px 16px;
      border-bottom: 1px solid #e2e8f0;
      text-align: left;
      font-size: 12.5px;
    }
    .details-table th {
      background: #f8fafc;
      color: #475569;
      font-weight: 700;
      font-size: 11.5px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .details-table .key {
      width: 220px;
      font-weight: 600;
      color: #334155;
      background: #fdfdfd;
    }
    .details-table .val {
      color: #0f172a;
    }

    /* Footer Notes & Sign */
    .bottom-section {
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      padding: 20px 24px;
      gap: 20px;
      background: #ffffff;
    }
    .bank-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 12px 16px;
    }
    .bank-box .bank-title {
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      color: #059669;
      margin-bottom: 6px;
    }
    .bank-box .bank-item {
      font-size: 11.5px;
      color: #334155;
      margin: 2px 0;
    }
    .sign-box {
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      align-items: flex-end;
      text-align: right;
    }
    .sign-box .sign-company {
      font-size: 12.5px;
      font-weight: 700;
      color: #0f172a;
      margin-bottom: 4px;
    }
    .sig-image {
      max-height: 55px;
      max-width: 170px;
      object-fit: contain;
      display: block;
      margin: 4px 0 6px auto;
    }
    .sig-placeholder {
      height: 45px;
    }
    .sign-box .sign-line {
      width: 170px;
      border-top: 1px solid #94a3b8;
      padding-top: 4px;
      font-size: 11px;
      color: #64748b;
      font-weight: 600;
      text-align: center;
    }
    .closing-note {
      text-align: center;
      padding: 10px 16px;
      font-size: 11px;
      color: #64748b;
      background: #f8fafc;
      border-top: 1px solid #e2e8f0;
    }
  </style>
</head>
<body>

  <div class="receipt-container">
    <!-- Top Header Banner -->
    <div class="top-banner">
      <div>
        <div class="title">Official Payment Receipt</div>
        <div class="subtitle">Valid GST Payment Voucher & Acknowledgment</div>
      </div>
      <div class="receipt-tag">
        <div class="number">${escapeHtml(receiptNo)}</div>
        <div class="date">${formattedDate} at ${formattedTime}</div>
      </div>
    </div>

    <!-- Parties Grid -->
    <div class="parties-grid">
      <!-- Received From (Customer) -->
      <div class="party-card left">
        <div class="label">Received With Thanks From</div>
        <div class="name">${escapeHtml(contact?.name || 'Customer')}</div>
        ${contact?.phone ? `<div class="detail"><strong>Phone:</strong> ${escapeHtml(contact.phone)}</div>` : ''}
        ${contact?.billing_address ? `<div class="detail">${multiline(contact.billing_address)}</div>` : ''}
        ${contact?.city || contact?.state ? `<div class="detail">${escapeHtml([contact.city, contact.state].filter(Boolean).join(', '))}</div>` : ''}
        ${contact?.gstin ? `<div class="badge">GSTIN: ${escapeHtml(contact.gstin)}</div>` : ''}
        ${contact?.pan ? `<div class="badge">PAN: ${escapeHtml(contact.pan)}</div>` : ''}
      </div>

      <!-- Issued By (Business) -->
      <div class="party-card">
        <div class="label">Issued By</div>
        <div class="name">${escapeHtml(business?.name || 'Accounts Dept')}</div>
        ${business?.mobile || business?.phone ? `<div class="detail"><strong>Phone:</strong> ${escapeHtml(business.mobile || business.phone)}</div>` : ''}
        ${business?.email ? `<div class="detail"><strong>Email:</strong> ${escapeHtml(business.email)}</div>` : ''}
        ${business?.address ? `<div class="detail">${multiline(business.address)}</div>` : ''}
        ${business?.city || business?.state ? `<div class="detail">${escapeHtml([business.city, business.state].filter(Boolean).join(', '))}</div>` : ''}
        ${business?.gstin ? `<div class="badge">GSTIN: ${escapeHtml(business.gstin)}</div>` : ''}
        ${business?.pan ? `<div class="badge">PAN: ${escapeHtml(business.pan)}</div>` : ''}
      </div>
    </div>

    <!-- Highlight Amount Box -->
    <div class="amount-highlight-box">
      <div class="left-info">
        <div class="label">Total Received Amount</div>
        <div class="words">${escapeHtml(amountWords)}</div>
      </div>
      <div class="right-figure">
        <div class="val">₹ ${formatINR(amount)}</div>
        <div class="mode-pill">Paid via ${escapeHtml(mode)}</div>
      </div>
    </div>

    <!-- Payment Transaction Details -->
    <table class="details-table">
      <tbody>
        <tr>
          <td class="key">Receipt Number</td>
          <td class="val"><strong>${escapeHtml(receiptNo)}</strong></td>
        </tr>
        <tr>
          <td class="key">Payment Date & Time</td>
          <td class="val">${formattedDate} at ${formattedTime}</td>
        </tr>
        <tr>
          <td class="key">Payment Method / Mode</td>
          <td class="val"><strong>${escapeHtml(mode)}</strong></td>
        </tr>
        ${payment.reference_no ? `
        <tr>
          <td class="key">Reference / Transaction ID</td>
          <td class="val"><strong>${escapeHtml(payment.reference_no)}</strong></td>
        </tr>` : ''}
        ${order?.invoice_number || order?.order_number ? `
        <tr>
          <td class="key">Settled Against Invoice</td>
          <td class="val">
            <strong>${escapeHtml(order.invoice_number || order.order_number)}</strong>
            ${order.invoice_date ? `<span style="color: #64748b; margin-left: 8px;">(Dated ${new Date(order.invoice_date).toLocaleDateString('en-IN')})</span>` : ''}
          </td>
        </tr>` : ''}
        ${payment.notes ? `
        <tr>
          <td class="key">Notes / Purpose</td>
          <td class="val">${escapeHtml(payment.notes)}</td>
        </tr>` : ''}
        ${balanceVal !== null ? `
        <tr>
          <td class="key">Party Closing Balance</td>
          <td class="val">
            <strong style="color: ${balanceVal > 0 ? '#b91c1c' : (balanceVal < 0 ? '#047857' : '#0f172a')}">
              ${balanceVal > 0 ? `₹ ${formatINR(balanceVal)} (Receivable Due)` : (balanceVal < 0 ? `₹ ${formatINR(Math.abs(balanceVal))} (Advance)` : '₹ 0.00 (Fully Settled)')}
            </strong>
          </td>
        </tr>` : ''}
      </tbody>
    </table>

    <!-- Bottom Section: Bank Info & Signature -->
    <div class="bottom-section">
      <div>
        ${hasBankDetails ? `
        <div class="bank-box">
          <div class="bank-title">Bank / UPI Transfer Details</div>
          ${business.bank_name ? `<div class="bank-item"><strong>Bank:</strong> ${escapeHtml(business.bank_name)}</div>` : ''}
          ${business.bank_account_no ? `<div class="bank-item"><strong>Account No:</strong> ${escapeHtml(business.bank_account_no)}</div>` : ''}
          ${business.bank_ifsc ? `<div class="bank-item"><strong>IFSC Code:</strong> ${escapeHtml(business.bank_ifsc)}</div>` : ''}
          ${business.upi_id ? `<div class="bank-item"><strong>UPI ID:</strong> ${escapeHtml(business.upi_id)}</div>` : ''}
        </div>` : ''}
      </div>

      <div class="sign-box">
        <div class="sign-company">For <strong>${escapeHtml(business?.name || 'Company')}</strong></div>
        ${signatureBlock}
        <div class="sign-line">Authorized Signatory</div>
      </div>
    </div>

    <div class="closing-note">
      This is a computer generated payment receipt. No physical signature is required. Thank you for your business!
    </div>
  </div>

</body>
</html>`;
}
