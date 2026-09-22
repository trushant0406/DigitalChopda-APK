import { amountToWords } from '../lib/numberToWords.js';

const ACCENT = '#2952e3';

function formatINR(amount) {
  return new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
    Number(amount || 0)
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

export function buildStatementHtml({ business, customer, stats, entries }) {
  const safeEntries = Array.isArray(entries) ? entries : [];

  const rowsHtml = safeEntries.map((e, idx) => {
    const isDebit = Number(e.debit || 0) > 0;
    const isCredit = Number(e.credit || 0) > 0;
    const balanceNum = Number(e.balance ?? e.running_balance ?? 0);
    const balFormatted = `₹ ${formatINR(Math.abs(balanceNum))} ${balanceNum > 0 ? 'Dr' : balanceNum < 0 ? 'Cr' : ''}`;
    let dateFormatted = '—';
    if (e.date && e.voucher !== 'Opening Balance') {
      const ed = new Date(e.date);
      const dStr = ed.toLocaleDateString('en-GB');
      const tStr = ed.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase();
      dateFormatted = `<div>${dStr}</div><div style="font-size: 9.5px; color: #64748b; margin-top: 1px;">${tStr}</div>`;
    }

    return `
      <tr>
        <td>${dateFormatted}</td>
        <td><strong>${escapeHtml(e.voucher || e.type_label || e.type)}</strong></td>
        <td>${escapeHtml(e.sr_no || e.voucher_no || '—')}</td>
        <td>${escapeHtml(e.payment_mode || '—')}</td>
        <td class="right ${isCredit ? 'credit-text' : 'muted'}">${isCredit ? `₹ ${formatINR(e.credit)}` : '—'}</td>
        <td class="right ${isDebit ? 'debit-text' : 'muted'}">${isDebit ? `₹ ${formatINR(e.debit)}` : '—'}</td>
        <td class="right strong ${balanceNum > 0 ? 'balance-due' : 'balance-ok'}">${balFormatted}</td>
      </tr>
    `;
  }).join('');

  const signatureBlock = business?.signature_image
    ? `<img class="sig-image" src="${business.signature_image}" alt="Signature" />`
    : `<div class="sig-placeholder">Authorized Signatory</div>`;

  const closingBalance = Number(stats?.current_balance || 0);
  const closingText = closingBalance > 0
    ? `₹ ${formatINR(closingBalance)} (To Receive)`
    : closingBalance < 0
    ? `₹ ${formatINR(Math.abs(closingBalance))} (Advance)`
    : `₹ 0.00 (Settled)`;

  return `<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<style>
  * { box-sizing: border-box; }
  body {
    font-family: "Noto Sans", "DejaVu Sans", -apple-system, "Segoe UI", Arial, Helvetica, sans-serif;
    color: #1f2430; font-size: 11px; margin: 0; padding: 32px 36px 28px;
  }
  .topbar { height: 6px; background: ${ACCENT}; border-radius: 3px; margin-bottom: 20px; }
  .header { display: flex; justify-content: space-between; margin-bottom: 18px; }
  .seller h1 { font-size: 18px; margin: 0 0 5px; letter-spacing: -0.2px; color: #111; }
  .seller div { margin-bottom: 3px; max-width: 380px; color: #444; }
  .seller strong { color: #1f2430; }
  .statement-title { font-size: 20px; font-weight: 800; text-align: right; margin-bottom: 8px; color: ${ACCENT}; letter-spacing: 0.8px; }
  .statement-meta { text-align: right; }
  .statement-meta table { margin-left: auto; }
  .statement-meta td { padding: 2px 0 2px 14px; font-size: 11px; }
  .statement-meta td:first-child { color: #777; }
  .statement-meta td:last-child { font-weight: 700; }
  
  .party-card {
    border: 1px solid #e2e8f0; border-left: 4px solid ${ACCENT};
    background: #f8fafc; border-radius: 4px; padding: 10px 14px; margin-bottom: 16px;
    display: flex; justify-content: space-between; align-items: center;
  }
  .party-info h2 { font-size: 14px; margin: 0 0 3px; color: #0f172a; }
  .party-info div { color: #475569; font-size: 11px; margin-bottom: 2px; }
  
  .kpi-strip {
    display: flex; gap: 12px; margin-bottom: 18px;
  }
  .kpi-card {
    flex: 1; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 4px;
    padding: 10px 12px; text-align: center;
  }
  .kpi-label { font-size: 9.5px; text-transform: uppercase; color: #64748b; font-weight: 600; letter-spacing: 0.5px; margin-bottom: 4px; }
  .kpi-val { font-size: 14px; font-weight: 800; color: #0f172a; }
  .kpi-val.due { color: #dc2626; }
  .kpi-val.ok { color: #16a34a; }

  table.ledger { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
  table.ledger th {
    background: #1f2430; color: white; padding: 8px 8px; font-size: 10px;
    text-align: left; letter-spacing: 0.3px;
  }
  table.ledger th:first-child { border-radius: 4px 0 0 0; }
  table.ledger th:last-child { border-radius: 0 4px 0 0; text-align: right; }
  table.ledger td { padding: 8px 8px; border-bottom: 1px solid #e2e8f0; vertical-align: middle; font-size: 10.5px; }
  .center { text-align: center; }
  .right { text-align: right; }
  .muted { color: #94a3b8; }
  .strong { font-weight: 700; }
  .credit-text { color: #15803d; font-weight: 600; }
  .debit-text { color: #0f172a; font-weight: 600; }
  .balance-due { color: #b91c1c; }
  .balance-ok { color: #15803d; }
  
  .closing-bar {
    background: #f1f5f9; border-top: 2px solid ${ACCENT}; border-bottom: 2px solid ${ACCENT};
    padding: 10px 14px; display: flex; justify-content: space-between; align-items: center;
    font-size: 12px; font-weight: 700; margin-bottom: 24px; border-radius: 3px;
  }
  
  .bottom { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 20px; }
  .bottom-left { max-width: 360px; color: #64748b; font-size: 10px; line-height: 1.5; }
  .signature { text-align: right; font-weight: 700; font-size: 11px; }
  .sig-image { max-height: 55px; max-width: 160px; display: block; margin: 0 0 4px auto; }
  .sig-placeholder {
    height: 50px; width: 160px; margin: 0 0 4px auto; margin-left: auto;
    border: 1px dashed #cbd5e1; border-radius: 4px; display: flex; align-items: center; justify-content: center;
    color: #94a3b8; font-size: 9.5px; font-style: italic;
  }
  .footer-note { margin-top: 25px; text-align: center; color: #94a3b8; font-size: 9px; }
</style>
</head>
<body>

<div class="topbar"></div>

<div class="header">
  <div class="seller">
    <h1>${escapeHtml(business?.company_name || 'Business Statement')}</h1>
    ${business?.address ? `<div>${multiline(business.address)}</div>` : ''}
    ${business?.phone ? `<div><strong>Phone:</strong> ${escapeHtml(business.phone)}</div>` : ''}
    ${business?.email ? `<div><strong>Email:</strong> ${escapeHtml(business.email)}</div>` : ''}
    ${business?.gstin ? `<div><strong>GSTIN:</strong> ${escapeHtml(business.gstin)}</div>` : ''}
  </div>

  <div>
    <div class="statement-title">PARTY STATEMENT</div>
    <div class="statement-meta">
      <table>
        <tr><td>Statement Generated:</td><td>${new Date().toLocaleDateString('en-GB')} ${new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }).toUpperCase()}</td></tr>
        <tr><td>Total Entries:</td><td>${safeEntries.length}</td></tr>
      </table>
    </div>
  </div>
</div>

<div class="party-card">
  <div class="party-info">
    <h2>${escapeHtml(customer?.name || 'Customer')}</h2>
    ${customer?.phone ? `<div><strong>Phone:</strong> ${escapeHtml(customer.phone)}</div>` : ''}
    ${customer?.gstin ? `<div><strong>GSTIN:</strong> ${escapeHtml(customer.gstin)}</div>` : ''}
    ${customer?.billing_address ? `<div><strong>Address:</strong> ${escapeHtml(customer.billing_address)}</div>` : ''}
  </div>
  <div style="text-align: right;">
    <div style="font-size: 10px; color: #64748b; text-transform: uppercase;">Total Receivable</div>
    <div style="font-size: 16px; font-weight: 800; color: ${closingBalance > 0 ? '#dc2626' : '#16a34a'};">
      ${closingText}
    </div>
  </div>
</div>

<div class="kpi-strip">
  <div class="kpi-card">
    <div class="kpi-label">Total Receivable Amount</div>
    <div class="kpi-val ${closingBalance > 0 ? 'due' : 'ok'}">₹ ${formatINR(stats?.total_receivable_amount || 0)}</div>
  </div>
  <div class="kpi-card">
    <div class="kpi-label">Overdue Amount</div>
    <div class="kpi-val" style="color: #ea580c;">₹ ${formatINR(stats?.overdue_amount || 0)}</div>
  </div>
  <div class="kpi-card">
    <div class="kpi-label">Total Sales Amount</div>
    <div class="kpi-val">₹ ${formatINR(stats?.total_sales_amount || stats?.total_billed || 0)}</div>
  </div>
  <div class="kpi-card">
    <div class="kpi-label">Total Received Amount</div>
    <div class="kpi-val" style="color: #16a34a;">₹ ${formatINR(stats?.total_received_amount || stats?.total_paid || 0)}</div>
  </div>
</div>

<table class="ledger">
  <thead>
    <tr>
      <th style="width: 80px;">Date</th>
      <th style="width: 110px;">Voucher</th>
      <th style="width: 90px;">Sr No</th>
      <th style="width: 90px;">Payment Mode</th>
      <th style="width: 90px;" class="right">Credit</th>
      <th style="width: 90px;" class="right">Debit</th>
      <th style="width: 105px;" class="right">Balance</th>
    </tr>
  </thead>
  <tbody>
    ${rowsHtml || '<tr><td colspan="7" class="center muted" style="padding: 20px;">No transactions recorded in this period.</td></tr>'}
  </tbody>
</table>

<div class="closing-bar">
  <div>CLOSING RECEIVABLE BALANCE</div>
  <div style="color: ${closingBalance > 0 ? '#dc2626' : '#16a34a'};">${closingText}</div>
</div>

<div class="bottom">
  <div class="bottom-left">
    <div><strong>Terms & Conditions:</strong></div>
    <div>Please verify all entries in this party statement. All payments received are accounted for as per transaction date. In case of discrepancy, please contact us immediately.</div>
  </div>
  <div class="signature">
    ${signatureBlock}
    <div>For ${escapeHtml(business?.company_name || 'Business')}</div>
  </div>
</div>

<div class="footer-note">This is a computer-generated statement and requires no physical signature.</div>

</body>
</html>
`;
}
