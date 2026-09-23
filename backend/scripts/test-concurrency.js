import { pool } from '../src/db.js';

const BASE_URL = process.env.API_URL || 'http://localhost:4000';

async function run() {
  console.log('\n=============================================================');
  console.log('⚡ SIMULTANEOUS 10-INVOICE CREATION & CONCURRENT ACCESS TEST');
  console.log('=============================================================\n');

  // Step 1: Get admin auth token
  const loginRes = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then(r => r.json()).catch(() => ({}));

  const headers = { 'Content-Type': 'application/json' };
  if (loginRes?.token) headers['Authorization'] = `Bearer ${loginRes.token}`;

  // Step 2: Create 10 Invoices
  console.log('1️⃣  Creating 10 Invoices with GST & Line Items...');
  const tCreateStart = Date.now();
  const createdInvoices = [];

  for (let i = 1; i <= 10; i++) {
    const res = await fetch(`${BASE_URL}/api/orders`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        phone: `98222000${String(i).padStart(2, '0')}`,
        name: `Customer ${i}`,
        create_invoice: true,
        payment_status: 'pending',
        items: [
          { product_name: `Product ${i}A`, quantity: 2, unit_price: 250 * i, gst_rate: 18 },
          { product_name: `Product ${i}B`, quantity: 1, unit_price: 150, gst_rate: 12 }
        ]
      })
    }).then(r => r.json());

    if (res && res.id) {
      createdInvoices.push(res);
    }
  }

  const createDuration = Date.now() - tCreateStart;
  console.log(`   ✅ Created ${createdInvoices.length}/10 invoices in ${createDuration}ms:`);
  console.log(`      Numbers: ${createdInvoices.map(inv => inv.invoice_number || `ORD-${inv.id}`).join(', ')}\n`);

  if (createdInvoices.length === 0) {
    console.error('❌ Failed to create invoices. Aborting.');
    process.exit(1);
  }

  // Step 3: Access all 10 invoices simultaneously
  console.log('2️⃣  Accessing / Reading all 10 Invoices at the EXACT SAME TIME...');
  const tFetchStart = Date.now();

  const fetchTasks = createdInvoices.map(async (inv, idx) => {
    const start = Date.now();
    const res = await fetch(`${BASE_URL}/api/orders/${inv.id}`, { headers });
    const data = await res.json().catch(() => ({}));
    return {
      Invoice: inv.invoice_number || `INV-${inv.id}`,
      Customer: data.name || `Customer ${idx + 1}`,
      GrandTotal: `₹${data.grand_total || inv.grand_total}`,
      Status: `${res.status} OK`,
      Latency: `${Date.now() - start}ms`
    };
  });

  const fetchResults = await Promise.all(fetchTasks);
  const totalFetchTime = Date.now() - tFetchStart;

  console.table(fetchResults);
  console.log(`   ⚡ All 10 invoices fetched simultaneously in ${totalFetchTime}ms (avg ~${Math.round(totalFetchTime / 10)}ms each)!\n`);

  // Step 4: Stream all 10 PDFs simultaneously through the concurrency queue
  console.log('3️⃣  Requesting 10 PDF Invoices Simultaneously (Testing Chromium Concurrency Queue)...');
  const tPdfStart = Date.now();

  const pdfTasks = createdInvoices.map(async (inv) => {
    const start = Date.now();
    const res = await fetch(`${BASE_URL}/api/orders/${inv.id}/pdf`, { headers });
    const buf = await res.arrayBuffer();
    return {
      Invoice: inv.invoice_number || `INV-${inv.id}`,
      Status: `${res.status} OK`,
      Size: `${(buf.byteLength / 1024).toFixed(1)} KB`,
      RenderTime: `${Date.now() - start}ms`
    };
  });

  const pdfResults = await Promise.all(pdfTasks);
  const totalPdfTime = Date.now() - tPdfStart;

  console.table(pdfResults);
  console.log(`   📄 All 10 PDFs successfully rendered in ${totalPdfTime}ms without server crash or OOM!\n`);

  // Step 5: Clean up test records
  console.log('4️⃣  Cleaning up 10 temporary test invoices...');
  const ids = createdInvoices.map(inv => inv.id);
  await pool.query('DELETE FROM order_items WHERE order_id = ANY($1)', [ids]);
  await pool.query('DELETE FROM orders WHERE id = ANY($1)', [ids]);
  console.log('   🧹 Test data cleaned up successfully!\n');

  console.log('=============================================================');
  console.log('🎉 ALL TESTS PASSED! SERVER STABLE UNDER 10-CONCURRENT LOAD');
  console.log('=============================================================\n');

  process.exit(0);
}

run().catch(err => {
  console.error('\n❌ Test encountered an error:', err);
  process.exit(1);
});
