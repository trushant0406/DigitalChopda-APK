import http from 'http';
import crypto from 'node:crypto';
import { pool } from '../src/db.js';

const SECRET = process.env.JWT_SECRET || 'callintel_secret_key_2026_super_secure';

function makeToken(id, username) {
  const payload = `${id}:${username}:${Date.now() + 7 * 24 * 60 * 60 * 1000}`;
  const sig = crypto.createHmac('sha256', SECRET).update(payload).digest('hex');
  return `${payload}:${sig}`;
}

function req(method, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const payload = body ? JSON.stringify(body) : null;
    const opts = {
      hostname: 'localhost',
      port: 4000,
      path: '/api' + path,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers,
        ...(payload ? { 'Content-Length': Buffer.byteLength(payload) } : {}),
      },
    };
    const r = http.request(opts, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch {
          resolve({ status: res.statusCode, raw: data });
        }
      });
    });
    r.on('error', reject);
    if (payload) r.write(payload);
    r.end();
  });
}

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ ASSERTION FAILED: ${message}`);
    throw new Error(message);
  }
  console.log(`  ✓ ${message}`);
}

async function run() {
  console.log('\n' + '='.repeat(70));
  console.log('🧪 MULTI-COMPANY END-TO-END VERIFICATION SUITE');
  console.log('   Testing Multi-Tenant Isolation, Scenarios & Security for Each Company');
  console.log('='.repeat(70) + '\n');

  const dhruvToken = makeToken(52, 'dhruv');
  const adminToken = makeToken(1, 'admin');

  let co3InvoiceId = null;
  let co5InvoiceId = null;
  let co3ProductId = null;
  let co5ProductId = null;

  try {
    // -------------------------------------------------------------------------
    // TEST 1: Auth & Company Discovery for User Dhruv
    // -------------------------------------------------------------------------
    console.log('📌 TEST 1: User Discovery & Assigned Companies (User @dhruv)');
    const meRes = await req('GET', '/auth/me', null, { Authorization: `Bearer ${dhruvToken}` });
    assert(meRes.status === 200, 'GET /api/auth/me returns status 200');
    assert(meRes.body.user.username === 'dhruv', 'Logged in user is @dhruv');
    assert(Array.isArray(meRes.body.businesses), 'Businesses list is an array');
    
    const bizIds = meRes.body.businesses.map((b) => b.id);
    console.log(`   Assigned Company IDs: [${bizIds.join(', ')}]`);
    assert(bizIds.includes(3), 'Company 3 (Shreeji Telecom) is assigned');
    assert(bizIds.includes(4), 'Company 4 (Surat E Commerce Store) is assigned');
    assert(bizIds.includes(5), 'Company 5 (Dhruv Logistics) is assigned');
    assert(!bizIds.includes(1), 'Company 1 (My Business) is NOT assigned to @dhruv');
    assert(!bizIds.includes(2), 'Company 2 (Test Shop 2) is NOT assigned to @dhruv');

    // -------------------------------------------------------------------------
    // TEST 2: Company 3 ("Shreeji Telecom") Operations
    // -------------------------------------------------------------------------
    console.log('\n📌 TEST 2: Company 3 ("Shreeji Telecom") Isolated Operations');
    const co3Profile = await req('GET', '/business-profile', null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '3',
    });
    assert(co3Profile.status === 200, 'Company 3 profile fetched successfully');
    assert(co3Profile.body.name === 'Shreeji Telecom', 'Company 3 name matches "Shreeji Telecom"');
    assert(co3Profile.body.invoice_prefix === 'ST-', 'Company 3 prefix is "ST-"');

    // Create unique product in Company 3
    const prod3Res = await req('POST', '/products', {
      name: `5G Telecom Handset ${Date.now()}`,
      default_price: 18500,
      hsn_code: '8517',
      gst_rate: 18,
    }, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '3',
    });
    assert(prod3Res.status === 200 || prod3Res.status === 201, 'Company 3 product created');
    co3ProductId = prod3Res.body.id;

    // Create Sales Invoice in Company 3
    const inv3Res = await req('POST', '/orders', {
      phone: '9825011111',
      name: 'Patel Electronics Co3',
      city: 'Ahmedabad',
      create_invoice: true,
      items: [
        { product_name: '5G Telecom Handset', quantity: 2, unit_price: 18500, gst_rate: 18 },
      ],
      payment_status: 'pending',
    }, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '3',
    });
    assert(inv3Res.status === 200 || inv3Res.status === 201, 'Company 3 sales invoice created');
    co3InvoiceId = inv3Res.body.id;
    assert(inv3Res.body.invoice_number.startsWith('ST-'), `Company 3 invoice number "${inv3Res.body.invoice_number}" uses ST- prefix`);
    assert(inv3Res.body.business_id === 3, 'Company 3 invoice has business_id = 3');

    // -------------------------------------------------------------------------
    // TEST 3: Company 5 ("Dhruv Logistics") Strict Isolation
    // -------------------------------------------------------------------------
    console.log('\n📌 TEST 3: Company 5 ("Dhruv Logistics") Strict Data Isolation');
    const co5Profile = await req('GET', '/business-profile', null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    assert(co5Profile.status === 200, 'Company 5 profile fetched successfully');
    assert(co5Profile.body.name === 'Dhruv Logistics', 'Company 5 name matches "Dhruv Logistics"');
    assert(co5Profile.body.invoice_prefix === 'DL-', 'Company 5 prefix is "DL-"');

    // Verify Company 5 invoice list does NOT contain Company 3 invoice
    const co5OrdersRes = await req('GET', '/orders', null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    assert(co5OrdersRes.status === 200, 'Company 5 orders listed successfully');
    const co5Orders = Array.isArray(co5OrdersRes.body) ? co5OrdersRes.body : co5OrdersRes.body.orders || [];
    assert(!co5Orders.some((o) => o.id === co3InvoiceId), 'Company 5 orders list DOES NOT leak Company 3 invoice');

    // Verify Company 5 products list does NOT contain Company 3 product
    const co5ProdsRes = await req('GET', '/products', null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    const co5Prods = Array.isArray(co5ProdsRes.body) ? co5ProdsRes.body : co5ProdsRes.body.products || [];
    assert(!co5Prods.some((p) => p.id === co3ProductId), 'Company 5 products list DOES NOT leak Company 3 product');

    // Create unique product in Company 5
    const prod5Res = await req('POST', '/products', {
      name: `Heavy Cargo Crate ${Date.now()}`,
      default_price: 3200,
      hsn_code: '4415',
      gst_rate: 12,
    }, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    assert(prod5Res.status === 200 || prod5Res.status === 201, 'Company 5 product created');
    co5ProductId = prod5Res.body.id;

    // Create Sales Invoice in Company 5
    const inv5Res = await req('POST', '/orders', {
      phone: '9825055555',
      name: 'Gujarat Logistics Partner Co5',
      city: 'Surat',
      create_invoice: true,
      items: [
        { product_name: 'Heavy Cargo Crate', quantity: 5, unit_price: 3200, gst_rate: 12 },
      ],
      payment_status: 'paid',
    }, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    assert(inv5Res.status === 200 || inv5Res.status === 201, 'Company 5 sales invoice created');
    co5InvoiceId = inv5Res.body.id;
    assert(inv5Res.body.invoice_number.startsWith('DL-'), `Company 5 invoice number "${inv5Res.body.invoice_number}" uses DL- prefix`);
    assert(inv5Res.body.business_id === 5, 'Company 5 invoice has business_id = 5');

    // -------------------------------------------------------------------------
    // TEST 4: Full Invoice Lifecycle in Company 5 (Edit & Delete)
    // -------------------------------------------------------------------------
    console.log('\n📌 TEST 4: Company 5 Invoice Lifecycle: Edit & Delete (MyBillBook style)');
    
    // Edit invoice in Company 5
    const editRes = await req('PUT', `/orders/${co5InvoiceId}`, {
      phone: '9825055555',
      name: 'Gujarat Logistics Partner Co5 - Updated',
      city: 'Surat Port',
      invoice_number: inv5Res.body.invoice_number,
      invoice_date: '2026-10-10',
      items: [
        { product_name: 'Heavy Cargo Crate', quantity: 10, unit_price: 3200, gst_rate: 12 },
      ],
      payment_status: 'paid',
      notes: 'Expedited transport delivery',
    }, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    assert(editRes.status === 200, 'Company 5 invoice edited successfully');
    assert(editRes.body.notes === 'Expedited transport delivery', 'Updated notes saved');
    assert(editRes.body.name === 'Gujarat Logistics Partner Co5 - Updated', 'Updated contact name saved');

    // Verify Company 3 invoice is untouched
    const co3CheckRes = await req('GET', `/orders/${co3InvoiceId}`, null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '3',
    });
    assert(co3CheckRes.status === 200, 'Company 3 invoice is still intact');
    assert(co3CheckRes.body.name === 'Patel Electronics Co3', 'Company 3 invoice details remain uncorrupted');

    // Delete invoice in Company 5
    const delRes = await req('DELETE', `/orders/${co5InvoiceId}`, null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    assert(delRes.status === 200, 'Company 5 invoice deleted successfully');
    assert(delRes.body.success === true, 'Delete response confirms success: true');

    // Verify deletion in Company 5
    const verifyDelRes = await req('GET', `/orders/${co5InvoiceId}`, null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '5',
    });
    assert(verifyDelRes.status === 404, 'Deleted invoice returns 404 in Company 5');

    // -------------------------------------------------------------------------
    // TEST 5: Company 4 ("Surat E Commerce Store") Verification
    // -------------------------------------------------------------------------
    console.log('\n📌 TEST 5: Company 4 ("Surat E Commerce Store") Isolation');
    const co4Profile = await req('GET', '/business-profile', null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '4',
    });
    assert(co4Profile.status === 200, 'Company 4 profile fetched successfully');
    assert(co4Profile.body.name === 'Surat E Commerce Store', 'Company 4 name matches');
    assert(co4Profile.body.invoice_prefix === 'SES-', 'Company 4 prefix is "SES-"');

    // -------------------------------------------------------------------------
    // TEST 6: Security: Rogue Tenant Access Fallback & Protection
    // -------------------------------------------------------------------------
    console.log('\n📌 TEST 6: Security & Cross-Tenant Access Protection');
    // User @dhruv tries to request Company 1 (which he is NOT assigned to)
    const rogueRes = await req('GET', '/business-profile', null, {
      Authorization: `Bearer ${dhruvToken}`,
      'X-Business-Id': '1',
    });
    assert(rogueRes.status === 200, 'Request handled safely');
    assert(rogueRes.body.id !== 1, 'Access to unauthorized Company 1 was PREVENTED');
    assert([3, 4, 5].includes(rogueRes.body.id), 'Fallback correctly defaulted to user assigned business');

    // -------------------------------------------------------------------------
    // TEST 7: Super Admin Multi-Company Switching
    // -------------------------------------------------------------------------
    console.log('\n📌 TEST 7: Super Admin Universal Access');
    const adminCo1 = await req('GET', '/business-profile', null, {
      Authorization: `Bearer ${adminToken}`,
      'X-Business-Id': '1',
    });
    assert(adminCo1.body.id === 1, 'Super Admin can access Company 1');
    const adminCo3 = await req('GET', '/business-profile', null, {
      Authorization: `Bearer ${adminToken}`,
      'X-Business-Id': '3',
    });
    assert(adminCo3.body.id === 3, 'Super Admin can switch to Company 3');

    console.log('\n' + '='.repeat(70));
    console.log('🎉 ALL MULTI-COMPANY TEST SCENARIOS PASSED WITH 100% SUCCESS!');
    console.log('='.repeat(70) + '\n');
    process.exit(0);
  } catch (err) {
    console.error('\n❌ TEST SUITE FAILED:', err.message);
    process.exit(1);
  } finally {
    // Cleanup created test records
    try {
      if (co3InvoiceId) await pool.query('DELETE FROM orders WHERE id = $1', [co3InvoiceId]);
      if (co3ProductId) await pool.query('DELETE FROM products WHERE id = $1', [co3ProductId]);
      if (co5ProductId) await pool.query('DELETE FROM products WHERE id = $1', [co5ProductId]);
    } catch {}
    await pool.end();
  }
}

run();
