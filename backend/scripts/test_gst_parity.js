// Automated verification script for Indian GST statutory calculation parity
const API_BASE = 'http://localhost:4000/api';

async function runTests() {
  console.log('🚀 Starting GST Parity Verification Tests...\n');

  // Test 1: Intra-State Invoice (Gujarat seller to Gujarat buyer) with multi-rate items
  console.log('--- TEST 1: Intra-State Invoice (CGST + SGST) with Multi-rate items ---');
  const intraOrderPayload = {
    create_invoice: true,
    name: 'Gujarat Steel Traders',
    phone: '9898012345',
    city: 'Ahmedabad',
    state: 'Gujarat',
    gstin: '24AAAAA0000A1Z5',
    billing_address: '101, Steel Market, Ahmedabad, Gujarat',
    items: [
      { product_name: 'Industrial Valve 18%', quantity: 1, unit_price: 100.50, gst_rate: 18, hsn_code: '8481' },
      { product_name: 'Steel Pipe 12%', quantity: 2, unit_price: 50.25, gst_rate: 12, hsn_code: '7306' },
    ],
    payment_status: 'pending',
  };

  const res1 = await fetch(`${API_BASE}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(intraOrderPayload),
  });
  if (!res1.ok) {
    throw new Error(`Test 1 Order creation failed: ${res1.status} ${await res1.text()}`);
  }
  const order1 = await res1.json();
  console.log(`✓ Order created: ID ${order1.id}, Number: ${order1.order_number}`);
  console.log(`  Tax Mode: ${order1.tax_mode} (Expected: split)`);
  console.log(`  Place of Supply: ${order1.place_of_supply}`);
  console.log(`  Raw Subtotal: 201.00`);
  console.log(`  Expected Tax: 18.09 (from 18%) + 12.06 (from 12%) = 30.15`);
  console.log(`  Raw Total: 231.15`);
  console.log(`  Round Off: ${order1.round_off} (Expected: -0.15)`);
  console.log(`  Grand Total: ${order1.grand_total} (Expected: 231)`);

  if (order1.tax_mode !== 'split') throw new Error(`Expected tax_mode 'split' but got ${order1.tax_mode}`);
  if (Math.abs(Number(order1.grand_total) - 231) > 0.01) throw new Error(`Grand total mismatch: ${order1.grand_total}`);

  // Test 2: Inter-State Invoice (Gujarat seller to Maharashtra buyer)
  console.log('\n--- TEST 2: Inter-State Invoice (IGST) with Odd Decimal Round-off ---');
  const interOrderPayload = {
    create_invoice: true,
    name: 'Mumbai Marine Works',
    phone: '9820012345',
    city: 'Mumbai',
    state: 'Maharashtra',
    gstin: '27BBBBB1111B1Z2',
    billing_address: '42, Dock Area, Mumbai, Maharashtra',
    items: [
      { product_name: 'Marine Engine Part 18%', quantity: 1, unit_price: 333.33, gst_rate: 18, hsn_code: '8409' },
    ],
    payment_status: 'pending',
  };

  const res2 = await fetch(`${API_BASE}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(interOrderPayload),
  });
  if (!res2.ok) {
    throw new Error(`Test 2 Order creation failed: ${res2.status} ${await res2.text()}`);
  }
  const order2 = await res2.json();
  console.log(`✓ Order created: ID ${order2.id}, Number: ${order2.order_number}`);
  console.log(`  Tax Mode: ${order2.tax_mode} (Expected: igst)`);
  console.log(`  Place of Supply: ${order2.place_of_supply}`);
  console.log(`  Raw Subtotal: 333.33`);
  console.log(`  Tax (18% IGST): 60.00`);
  console.log(`  Raw Total: 393.33`);
  console.log(`  Round Off: ${order2.round_off} (Expected: -0.33)`);
  console.log(`  Grand Total: ${order2.grand_total} (Expected: 393)`);

  if (order2.tax_mode !== 'igst') throw new Error(`Expected tax_mode 'igst' but got ${order2.tax_mode}`);
  if (Math.abs(Number(order2.grand_total) - 393) > 0.01) throw new Error(`Grand total mismatch: ${order2.grand_total}`);

  // Test 3: PDF Generation & Statutory Elements Verification
  console.log('\n--- TEST 3: PDF HTML Statutory Rendering Verification ---');
  const { buildInvoiceHtml } = await import('../src/pdf/invoiceHtml.js');
  const { pool } = await import('../src/db.js');

  const fullOrder1Res = await pool.query('SELECT * FROM orders WHERE id = $1', [order1.id]);
  const fullOrder1 = fullOrder1Res.rows[0];
  const items1Res = await pool.query('SELECT * FROM order_items WHERE order_id = $1', [order1.id]);
  const items1 = items1Res.rows;
  const businessRes = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  const business = businessRes.rows[0];

  const html1 = buildInvoiceHtml({ business, order: fullOrder1, items: items1, showPaymentDetails: true });

  // Check HTML for:
  // 1. CGST @9% and SGST @9%
  // 2. CGST @6% and SGST @6%
  // 3. No blended average (no 7.5%)
  // 4. Place of Supply
  // 5. Round Off
  // 6. HSN / GST Tax Summary
  const hasCGST9 = html1.includes('CGST @9%');
  const hasSGST9 = html1.includes('SGST @9%');
  const hasCGST6 = html1.includes('CGST @6%');
  const hasSGST6 = html1.includes('SGST @6%');
  const hasBlendedAverage = html1.includes('7.5%');
  const hasRoundOff = html1.includes('Round Off');
  const hasHsnSummary = html1.includes('HSN / GST Tax Summary');
  const hasPlaceOfSupply = html1.includes('Place of Supply');

  console.log('Intra-state PDF HTML checks:');
  console.log('  Includes CGST @9%: ' + hasCGST9);
  console.log('  Includes SGST @9%: ' + hasSGST9);
  console.log('  Includes CGST @6%: ' + hasCGST6);
  console.log('  Includes SGST @6%: ' + hasSGST6);
  console.log('  NO blended average 7.5%: ' + (!hasBlendedAverage));
  console.log('  Includes Round Off row: ' + hasRoundOff);
  console.log('  Includes HSN Tax Summary table: ' + hasHsnSummary);
  console.log('  Includes Place of Supply: ' + hasPlaceOfSupply);

  if (!hasCGST9 || !hasSGST9 || !hasCGST6 || !hasSGST6) {
    throw new Error('Multi-rate tax grouping failed in Intra-state invoice HTML');
  }
  if (hasBlendedAverage) {
    throw new Error('Detected obsolete averaged tax rate in invoice HTML');
  }
  if (!hasRoundOff) {
    throw new Error('Round off row missing in invoice HTML');
  }
  if (!hasHsnSummary) {
    throw new Error('HSN / GST Tax Summary table missing in invoice HTML');
  }

  // Inter-state HTML check
  const fullOrder2Res = await pool.query('SELECT * FROM orders WHERE id = $1', [order2.id]);
  const fullOrder2 = fullOrder2Res.rows[0];
  const items2Res = await pool.query('SELECT * FROM order_items WHERE order_id = $1', [order2.id]);
  const items2 = items2Res.rows;
  const html2 = buildInvoiceHtml({ business, order: fullOrder2, items: items2, showPaymentDetails: true });

  const hasIGST18 = html2.includes('IGST @18%');
  const hasNoSplit = !html2.includes('CGST') && !html2.includes('SGST');
  console.log('\nInter-state PDF HTML checks:');
  console.log('  Includes IGST @18%: ' + hasIGST18);
  console.log('  Contains NO CGST / SGST: ' + hasNoSplit);

  if (!hasIGST18 || !hasNoSplit) {
    throw new Error('Inter-state IGST calculation or rendering failed');
  }

  console.log('\n🎉 ALL 3 GST PARITY STATUTORY VERIFICATION TESTS PASSED SUCCESSFULLY!');
  process.exit(0);
}

runTests().catch((err) => {
  console.error('\n❌ GST Parity Test Error:', err);
  process.exit(1);
});
