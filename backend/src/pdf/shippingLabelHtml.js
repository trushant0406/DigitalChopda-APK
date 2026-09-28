function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

function formatDate(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  if (isNaN(d.getTime())) return String(isoStr);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function buildShippingLabelHtml({ business, order, items = [] }) {
  const invNumber = order.invoice_number || order.order_number || `ORD-${order.id}`;
  const invDate = formatDate(order.invoice_date || order.created_at);
  const totalQty = items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
  const destCity = (order.city || order.state || '').trim().toUpperCase();

  const itemRows = items
    .slice(0, 8)
    .map((item, idx) => {
      const unit = item.unit || 'PCS';
      const qty = Number(item.quantity) || 1;
      return `
        <tr>
          <td style="width: 28px; text-align: center; color: #64748b; font-weight: 600;">${idx + 1}</td>
          <td style="font-weight: 600; color: #0f172a;">${escapeHtml(item.product_name)}</td>
          <td style="width: 85px; text-align: right; font-weight: 700; color: #0f172a;">${qty} ${escapeHtml(unit)}</td>
        </tr>`;
    })
    .join('');

  const remainingItemsCount = items.length > 8 ? items.length - 8 : 0;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>Shipping Label - ${escapeHtml(invNumber)}</title>
  <style>
    @page {
      size: A4 portrait;
      margin: 0;
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }
    html, body {
      width: 210mm;
      height: 100%;
      margin: 0;
      padding: 0;
      overflow: hidden;
      page-break-after: avoid;
      page-break-before: avoid;
      page-break-inside: avoid;
      background: #ffffff;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      color: #0f172a;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }

    /* Standard Parcel Shipping Label Box (strictly 1 page) */
    .shipping-label-card {
      width: 198mm;
      height: 95mm;
      margin: 6mm auto 0 auto;
      border: 2px solid #0f172a;
      border-radius: 6px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: 4mm 5mm;
      background: #ffffff;
      overflow: hidden;
      page-break-inside: avoid;
      page-break-after: avoid;
    }

    /* Top Brand & Identification Bar */
    .label-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 2px solid #0f172a;
      padding-bottom: 2mm;
      margin-bottom: 2.5mm;
    }
    .header-left {
      display: flex;
      align-items: center;
      gap: 3mm;
    }
    .shipping-badge {
      font-size: 11pt;
      font-weight: 900;
      letter-spacing: 0.8px;
      text-transform: uppercase;
      background: #0f172a;
      color: #ffffff;
      padding: 3px 8px;
      border-radius: 4px;
    }
    .city-pill {
      font-size: 9pt;
      font-weight: 800;
      letter-spacing: 0.5px;
      color: #0f172a;
      background: #f1f5f9;
      border: 1px solid #cbd5e1;
      padding: 2px 7px;
      border-radius: 4px;
    }
    .header-right {
      text-align: right;
    }
    .invoice-tag {
      font-size: 11pt;
      font-weight: 800;
      color: #0f172a;
    }
    .date-tag {
      font-size: 7.5pt;
      font-weight: 600;
      color: #64748b;
    }

    /* 2 Column Address Layout */
    .address-grid {
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      gap: 3.5mm;
      margin-bottom: 2.5mm;
    }
    .address-box {
      border: 1px solid #cbd5e1;
      border-radius: 4px;
      padding: 2.5mm 3mm;
      background: #ffffff;
    }
    .address-box.ship-to {
      background: #f8fafc;
      border-color: #94a3b8;
      border-left: 4px solid #0f172a;
    }
    .section-label {
      font-size: 7pt;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #475569;
      margin-bottom: 1mm;
      display: flex;
      justify-content: space-between;
    }
    .party-title {
      font-size: 11.5pt;
      font-weight: 800;
      color: #0f172a;
      line-height: 1.2;
      margin-bottom: 1mm;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .party-contact {
      font-size: 9.5pt;
      font-weight: 700;
      color: #0f172a;
      margin-bottom: 1mm;
    }
    .party-location {
      font-size: 8pt;
      color: #334155;
      line-height: 1.3;
      max-height: 2.6em;
      overflow: hidden;
    }

    /* Items Section */
    .items-container {
      flex: 1;
      min-height: 0;
      margin-bottom: 2mm;
    }
    .items-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 7.5pt;
    }
    .items-table th {
      background: #f1f5f9;
      color: #334155;
      font-weight: 700;
      text-transform: uppercase;
      font-size: 7pt;
      padding: 1.5mm 2mm;
      border: 1px solid #cbd5e1;
      letter-spacing: 0.3px;
    }
    .items-table td {
      padding: 1mm 2mm;
      border: 1px solid #e2e8f0;
      line-height: 1.2;
    }

    /* Bottom Status Bar */
    .label-footer-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-top: 1px solid #cbd5e1;
      padding-top: 1.5mm;
      font-size: 7.5pt;
      color: #475569;
    }
    .footer-meta {
      font-weight: 600;
    }
    .footer-qty {
      font-weight: 800;
      font-size: 9pt;
      color: #0f172a;
    }
  </style>
</head>
<body>

  <!-- SHIPPING LABEL (TOP OF PAGE) -->
  <div class="shipping-label-card">
    <!-- Header -->
    <div class="label-header">
      <div class="header-left">
        <div class="shipping-badge">📦 SHIPPING LABEL</div>
        ${destCity ? `<div class="city-pill">DESTINATION: ${escapeHtml(destCity)}</div>` : ''}
      </div>
      <div class="header-right">
        <div class="invoice-tag">${escapeHtml(invNumber)}</div>
        <div class="date-tag">DATE: ${escapeHtml(invDate)}</div>
      </div>
    </div>

    <!-- 2 Column Addresses -->
    <div class="address-grid">
      <!-- SHIP TO (RECIPIENT) -->
      <div class="address-box ship-to">
        <div class="section-label">
          <span>SHIP TO / DELIVER TO</span>
          <span>STANDARD DELIVERY</span>
        </div>
        <div class="party-title" title="${escapeHtml(order.name || '')}">${escapeHtml(order.name || 'Customer')}</div>
        <div class="party-contact">📞 ${escapeHtml(order.phone || '—')}</div>
        <div class="party-location">
          ${escapeHtml(order.shipping_address || order.billing_address || 'Address not specified')}<br/>
          <strong>${escapeHtml(order.city || '')}${order.state ? ', ' + escapeHtml(order.state) : ''}</strong>
        </div>
      </div>

      <!-- RETURN ADDRESS (SENDER) -->
      <div class="address-box">
        <div class="section-label">SHIPPED BY / RETURN ADDRESS</div>
        <div class="party-title" style="font-size: 10pt;" title="${escapeHtml(business?.name || '')}">${escapeHtml(business?.name || 'DigitalChopda Store')}</div>
        <div class="party-contact" style="font-size: 8.5pt;">📞 ${escapeHtml(business?.mobile || business?.phone || '—')}</div>
        <div class="party-location">
          ${escapeHtml(business?.address || '')}<br/>
          <strong>${escapeHtml(business?.state || '')}</strong>
        </div>
      </div>
    </div>

    <!-- Items summary (NO GST, NO RATES, ONLY QUANTITIES) -->
    <div class="items-container">
      <table class="items-table">
        <thead>
          <tr>
            <th style="width: 28px;">#</th>
            <th style="text-align: left;">PACKAGE CONTENTS / ITEMS</th>
            <th style="text-align: right;">QUANTITY</th>
          </tr>
        </thead>
        <tbody>
          ${itemRows}
          ${remainingItemsCount > 0 ? `<tr><td colspan="3" style="text-align: center; color: #64748b; font-style: italic;">+ ${remainingItemsCount} more item(s)...</td></tr>` : ''}
        </tbody>
      </table>
    </div>

    <!-- Footer Summary -->
    <div class="label-footer-bar">
      <div class="footer-meta">
        ${order.order_ref ? `<span>Ref / PO: <strong>${escapeHtml(order.order_ref)}</strong> · </span>` : ''}
        <span>Total Items: <strong>${items.length}</strong></span>
      </div>
      <div class="footer-qty">
        TOTAL PIECES: ${totalQty}
      </div>
    </div>
  </div>

</body>
</html>
`;
}
