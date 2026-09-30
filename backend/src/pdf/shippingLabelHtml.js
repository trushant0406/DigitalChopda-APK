function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

export function buildShippingLabelHtml({ business, order, items = [] }) {
  const partyName = (order.name || '').trim();
  const partyPhone = (order.phone || '').trim();
  const partyGstin = (order.gstin || '').trim();
  const partyAddress = (order.shipping_address || order.billing_address || '').trim();
  const partyCity = (order.city || '').trim();
  const partyState = (order.state || order.place_of_supply || '').trim().toUpperCase();

  // If order items were not passed directly in items, try order.items
  const resolvedItems = (items && items.length > 0) ? items : (order.items && order.items.length > 0 ? order.items : []);
  const displayItems = resolvedItems.slice(0, 3);

  const itemsHtml = displayItems.length > 0
    ? displayItems.map(item => `
        <div class="item-line">
          <span class="col-item-val">${escapeHtml(item.product_name)}</span>
          <span class="col-pcs-val">${Number(item.quantity) || 1} ${escapeHtml(item.unit || 'PCS')}</span>
        </div>
      `).join('')
    : `
        <div class="item-line">
          <span class="col-item-val fill-dots" style="width: 50mm;"></span>
          <span class="col-pcs-val fill-dots" style="width: 25mm;"></span>
        </div>
      `;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>Shipping Label</title>
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

    /* Single Shipping Label Container: Sized to 1/4 A4 (~64mm high), only 1 label on page */
    .shipping-box {
      width: 198mm;
      margin: 6mm auto 0 auto;
      border: 1.5px solid #0f172a;
      background: #ffffff;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      page-break-inside: avoid;
      page-break-after: avoid;
    }

    /* 1. TOP HEADER: Party Details (Left) | State (Middle) | Transport (Right) */
    .header-grid {
      display: grid;
      grid-template-columns: 1.35fr 0.85fr 1fr;
      border-bottom: 1.5px solid #0f172a;
      background: #ffffff;
      min-height: 25mm;
    }

    .header-col {
      padding: 1.8mm 2.5mm;
      position: relative;
    }
    .header-col.party-col {
      border-right: 1.5px solid #0f172a;
    }
    .header-col.state-col {
      border-right: 1.5px solid #0f172a;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      text-align: center;
      padding: 1.5mm 2mm;
    }
    .header-col.transport-col {
      padding: 1.5mm 2mm;
    }

    .field-tag {
      font-size: 6.5pt;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #64748b;
      margin-bottom: 0.8mm;
    }
    .party-name-val {
      font-size: 10.5pt;
      font-weight: 900;
      color: #0f172a;
      line-height: 1.15;
      margin-bottom: 0.8mm;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .party-info-row {
      font-size: 7.5pt;
      color: #334155;
      line-height: 1.25;
      margin-bottom: 0.6mm;
    }
    .party-addr-val {
      font-size: 7.2pt;
      color: #334155;
      line-height: 1.25;
      max-height: 2.5em;
      overflow: hidden;
    }

    /* State Column */
    .state-title {
      font-size: 6.8pt;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.6px;
      color: #64748b;
      margin-bottom: 1mm;
    }
    .state-name-val {
      font-size: 12pt;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: 0.6px;
      margin-bottom: 0.8mm;
    }
    .city-name-val {
      font-size: 8pt;
      font-weight: 700;
      color: #475569;
    }

    /* Transport Inner Box */
    .transport-box {
      border: 1px solid #0f172a;
      padding: 1mm 2mm;
      height: 100%;
      background: #ffffff;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .transport-title {
      font-size: 6.8pt;
      font-weight: 900;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #0f172a;
      border-bottom: 1px solid #cbd5e1;
      padding-bottom: 0.8mm;
      margin-bottom: 1mm;
      text-align: center;
    }

    .fill-line {
      display: flex;
      align-items: flex-end;
      font-size: 6.8pt;
      color: #334155;
      margin-bottom: 1mm;
    }
    .fill-line:last-child {
      margin-bottom: 0;
    }
    .fill-lbl {
      font-weight: 700;
      min-width: 48px;
      color: #0f172a;
    }
    .fill-dots {
      flex: 1;
      border-bottom: 1px dotted #475569;
      height: 8px;
      margin-left: 2px;
    }

    /* 2. MIDDLE SECTION: Item & Pcs close together on far left, NO horizontal line, right side empty */
    .items-section {
      width: 100%;
      border-bottom: 1.5px solid #0f172a;
      padding: 2mm 3mm;
      background: #ffffff;
      min-height: 18mm;
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
    }
    .items-header-row {
      display: flex;
      gap: 6mm;
      margin-bottom: 1.2mm;
      /* Notice: NO border-bottom! Horizontal line removed as requested */
    }
    .col-item-header {
      width: 50mm;
      font-size: 7.8pt;
      font-weight: 900;
      text-transform: uppercase;
      letter-spacing: 0.6px;
      color: #0f172a;
      text-align: left;
    }
    .col-pcs-header {
      width: 25mm;
      font-size: 7.8pt;
      font-weight: 900;
      text-transform: uppercase;
      letter-spacing: 0.6px;
      color: #0f172a;
      text-align: left;
    }

    .items-list {
      display: flex;
      flex-direction: column;
      gap: 1mm;
    }
    .item-line {
      display: flex;
      gap: 6mm;
      align-items: center;
      font-size: 7.5pt;
      line-height: 1.25;
    }
    .col-item-val {
      width: 50mm;
      font-weight: 700;
      color: #0f172a;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: left;
    }
    .col-pcs-val {
      width: 25mm;
      font-weight: 800;
      color: #0f172a;
      text-align: left;
    }

    /* 3. BOTTOM SECTION: Remark: (Left) | Payment (Right - no amount/mode) */
    .bottom-grid {
      display: grid;
      grid-template-columns: 1.35fr 1fr;
      background: #ffffff;
      min-height: 18mm;
    }
    .remark-section {
      border-right: 1.5px solid #0f172a;
      padding: 1.8mm 2.5mm;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .remark-header {
      font-size: 7.5pt;
      font-weight: 900;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #0f172a;
      margin-bottom: 1mm;
    }
    .remark-line {
      border-bottom: 1px dotted #94a3b8;
      height: 10px;
      width: 100%;
    }

    /* Payment Section: only Paid & Credit checkboxes, NO amount, NO mode */
    .payment-section {
      padding: 1.8mm 2.5mm;
      background: #ffffff;
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
    }
    .payment-header {
      font-size: 7.5pt;
      font-weight: 900;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #0f172a;
      margin-bottom: 2mm;
    }
    .payment-options {
      display: flex;
      align-items: center;
      gap: 6mm;
      margin-top: 1mm;
    }
    .pay-chk {
      display: inline-flex;
      align-items: center;
      gap: 5px;
    }
    .chk-box {
      width: 13px;
      height: 13px;
      border: 1.5px solid #0f172a;
      border-radius: 2px;
      background: #ffffff;
    }
    .chk-text {
      font-size: 8pt;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: 0.4px;
    }
  </style>
</head>
<body>

  <div class="shipping-box">
    <!-- TOP HEADER: Party Details (Left) | State (Middle) | Transport (Right) -->
    <div class="header-grid">
      <!-- Top Left: Party Name, Number, GST & Address -->
      <div class="header-col party-col">
        <div class="field-tag">PARTY DETAILS</div>
        <div class="party-name-val">${escapeHtml(partyName || '__________________________')}</div>
        ${partyPhone ? `
          <div class="party-info-row">Number: <strong>${escapeHtml(partyPhone)}</strong></div>
        ` : `
          <div class="fill-line" style="margin-bottom: 1mm;">
            <span class="fill-lbl" style="min-width: 45px;">Number:</span>
            <span class="fill-dots"></span>
          </div>
        `}
        ${partyGstin ? `
          <div class="party-info-row">GSTIN: <strong>${escapeHtml(partyGstin)}</strong></div>
        ` : ''}
        ${partyAddress ? `
          <div class="party-addr-val">
            ${escapeHtml(partyAddress)}<br/>
            <strong>${escapeHtml(partyCity)}${partyState ? ', ' + escapeHtml(partyState) : ''}</strong>
          </div>
        ` : `
          <div class="fill-line">
            <span class="fill-lbl" style="min-width: 28px;">Add:</span>
            <span class="fill-dots"></span>
          </div>
        `}
      </div>

      <!-- Middle: State -->
      <div class="header-col state-col">
        <div class="state-title">STATE.</div>
        ${partyState ? `
          <div class="state-name-val">${escapeHtml(partyState)}</div>
          <div class="city-name-val">${escapeHtml(partyCity || '')}</div>
        ` : `
          <div style="width: 100%; padding: 0 1mm;">
            <div class="fill-line">
              <span class="fill-lbl" style="min-width: 36px; font-weight: 800;">State:</span>
              <span class="fill-dots"></span>
            </div>
            <div class="fill-line" style="margin-top: 1.5mm;">
              <span class="fill-lbl" style="min-width: 36px; font-weight: 800;">City:</span>
              <span class="fill-dots"></span>
            </div>
          </div>
        `}
      </div>

      <!-- Right: Transport (In a neat box) -->
      <div class="header-col transport-col">
        <div class="transport-box">
          <div class="transport-title">TRANSPORT</div>
          <div class="fill-line">
            <span class="fill-lbl">Transport:</span>
            <span class="fill-dots"></span>
          </div>
          <div class="fill-line">
            <span class="fill-lbl">LR / Bilti:</span>
            <span class="fill-dots"></span>
          </div>
          <div class="fill-line">
            <span class="fill-lbl">Parcels:</span>
            <span class="fill-dots"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- MIDDLE SECTION: Item & Pcs close together on far left, NO horizontal line, right side empty -->
    <div class="items-section">
      <div class="items-header-row">
        <span class="col-item-header">ITEM</span>
        <span class="col-pcs-header">PCS</span>
      </div>
      <div class="items-list">
        ${itemsHtml}
      </div>
    </div>

    <!-- BOTTOM SECTION: Remark: (Left) | Payment (Right - no amount/mode) -->
    <div class="bottom-grid">
      <!-- Remark Box -->
      <div class="remark-section">
        <div class="remark-header">Remark:</div>
        <div class="remark-line"></div>
        <div class="remark-line"></div>
      </div>

      <!-- Payment Section: only Paid & Pending checkboxes, no amount, no mode -->
      <div class="payment-section">
        <div class="payment-header">Payment</div>
        <div class="payment-options">
          <span class="pay-chk">
            <span class="chk-box"></span>
            <span class="chk-text">Paid</span>
          </span>
          <span class="pay-chk">
            <span class="chk-box"></span>
            <span class="chk-text">Pending</span>
          </span>
        </div>
      </div>
    </div>
  </div>

</body>
</html>
`;
}
