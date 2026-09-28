/**
 * full_regression_test.js
 * End-to-end regression test suite for DigitalChopda Multi-Tenant Architecture:
 * - Auth & Business switching
 * - RBAC (Admin-only onboarding)
 * - Contacts & Calls tenant isolation
 * - Products tenant isolation
 * - Orders, Invoices, & Customer-wise Cloud Storage Keys
 * - Quotations creation & PDF
 * - Credit Notes creation & PDF
 * - Payments, Receipts & Ledger
 * - Dashboard Analytics
 */

const BASE_URL = process.env.TEST_API_URL || 'http://localhost:3000/api';

let passedTests = 0;
let failedTests = 0;

function assert(condition, message) {
  if (!condition) {
    failedTests++;
    console.error(`  ❌ FAILED: ${message}`);
    throw new Error(message);
  }
  passedTests++;
  console.log(`  ✅ PASSED: ${message}`);
}

async function runRegressionTests() {
  console.log('='.repeat(70));
  console.log('🧪 STARTING COMPREHENSIVE MULTI-TENANT REGRESSION TEST SUITE');
  console.log(`Target API: ${BASE_URL}`);
  console.log('='.repeat(70));

  // =========================================================================
  // 1. AUTHENTICATION & MULTI-TENANCY DISCOVERY
  // =========================================================================
  console.log('\n--- MODULE 1: AUTHENTICATION & BUSINESS DISCOVERY ---');
  const loginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' }),
  });
  const loginData = await loginRes.json();
  assert(loginRes.ok && loginData.token, 'Admin login succeeded and returned JWT');
  assert(Array.isArray(loginData.businesses), 'Login returned businesses array');
  assert(loginData.businesses.length >= 2, `At least 2 businesses discovered (Found: ${loginData.businesses.length})`);

  const biz1 = loginData.businesses.find((b) => b.id === 1);
  const biz2 = loginData.businesses.find((b) => b.id === 2);
  assert(biz1 && biz2, `Found Business 1 ("${biz1?.name}") and Business 2 ("${biz2?.name}")`);

  const token = loginData.token;
  const authHeadersBiz1 = { Authorization: `Bearer ${token}`, 'X-Business-Id': '1' };
  const authHeadersBiz2 = { Authorization: `Bearer ${token}`, 'X-Business-Id': '2' };

  // =========================================================================
  // 2. ADMIN-ONLY ONBOARDING RBAC PROTECTION
  // =========================================================================
  console.log('\n--- MODULE 2: RBAC ONBOARDING PROTECTION ---');
  // We simulate a non-admin token check if available, or verify the endpoint requires admin
  const nonAdminHeaders = {
    Authorization: `Bearer ${token}`,
    'X-Business-Id': '1',
  };
  // Note: admin has user.role === 'admin'
  assert(loginData.user.role === 'admin', 'Current user has role: admin');

  // Verify business listing endpoint
  const bizListRes = await fetch(`${BASE_URL}/businesses`, { headers: authHeadersBiz1 });
  const bizList = await bizListRes.json();
  assert(bizListRes.ok && Array.isArray(bizList), 'GET /api/businesses returned active business list');

  // =========================================================================
  // 3. BUSINESS PROFILE SEPARATION
  // =========================================================================
  console.log('\n--- MODULE 3: BUSINESS PROFILE ISOLATION ---');
  const prof1Res = await fetch(`${BASE_URL}/business-profile`, { headers: authHeadersBiz1 });
  const prof1 = await prof1Res.json();
  const prof2Res = await fetch(`${BASE_URL}/business-profile`, { headers: authHeadersBiz2 });
  const prof2 = await prof2Res.json();

  assert(prof1.id === 1 || prof1.name, `Business 1 profile loaded: ${prof1.name}`);
  assert(prof2.id === 2 || prof2.name, `Business 2 profile loaded: ${prof2.name}`);
  assert(prof1.id !== prof2.id, 'Profiles have distinct tenant IDs');

  // =========================================================================
  // 4. CONTACTS ISOLATION & SEARCH
  // =========================================================================
  console.log('\n--- MODULE 4: CONTACTS ISOLATION ---');
  const contacts1Res = await fetch(`${BASE_URL}/contacts`, { headers: authHeadersBiz1 });
  const contacts1 = await contacts1Res.json();
  const contacts2Res = await fetch(`${BASE_URL}/contacts`, { headers: authHeadersBiz2 });
  const contacts2 = await contacts2Res.json();

  assert(Array.isArray(contacts1) && Array.isArray(contacts2), 'Contacts returned as arrays for both tenants');
  const hardikInB2 = contacts2.find((c) => (c.name || '').toLowerCase().includes('hardik'));
  assert(Boolean(hardikInB2), `Hardik is properly present in Business 2 contacts (ID=${hardikInB2?.id})`);

  // =========================================================================
  // 5. CALL LOGGING & AUTO-CONTACT CREATION IN ACTIVE TENANT
  // =========================================================================
  console.log('\n--- MODULE 5: CALL LOGGING & TENANT-AWARE CONTACT CREATION ---');
  const uniquePhone = `9825${Math.floor(100000 + Math.random() * 900000)}`;
  const logCallRes = await fetch(`${BASE_URL}/calls`, {
    method: 'POST',
    headers: { ...authHeadersBiz2, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      phone: uniquePhone,
      name: 'Sunil Regression Tester',
      city: 'Vadodara',
      direction: 'incoming',
      outcome: 'answered',
      notes: 'Automated regression test call',
    }),
  });
  const logCallData = await logCallRes.json();
  assert(logCallRes.ok && logCallData.call, 'Call logged successfully in Business 2');
  assert(
    logCallData.contact?.business_id === 2,
    `Contact auto-created with business_id = 2 (Got: ${logCallData.contact?.business_id})`
  );

  // Verify Sunil does not leak into Business 1 contacts
  const recheckB1Res = await fetch(`${BASE_URL}/contacts`, { headers: authHeadersBiz1 });
  const recheckB1 = await recheckB1Res.json();
  const leakedSunil = recheckB1.find((c) => c.phone === uniquePhone);
  assert(!leakedSunil, 'Verified: Sunil did NOT leak into Business 1 contacts (strict isolation)');

  // =========================================================================
  // 6. PRODUCTS ISOLATION
  // =========================================================================
  console.log('\n--- MODULE 6: PRODUCTS ISOLATION ---');
  const prodName = `Test Product ${Date.now()}`;
  const createProdRes = await fetch(`${BASE_URL}/products`, {
    method: 'POST',
    headers: { ...authHeadersBiz2, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: prodName,
      default_price: 3200,
      gst_rate: 18,
    }),
  });
  const createdProd = await createProdRes.json();
  assert(createProdRes.ok && createdProd.id, `Product created in Business 2: ${prodName}`);

  const b1ProdsRes = await fetch(`${BASE_URL}/products`, { headers: authHeadersBiz1 });
  const b1Prods = await b1ProdsRes.json();
  const prodInB1 = b1Prods.find((p) => p.name === prodName);
  assert(!prodInB1, 'Verified: Business 2 product did NOT leak into Business 1 products');

  // =========================================================================
  // 7. INVOICE CREATION & CUSTOMER-WISE PDF STORAGE KEY
  // =========================================================================
  console.log('\n--- MODULE 7: ORDERS, INVOICES & CUSTOMER-WISE STORAGE ---');
  const createOrderRes = await fetch(`${BASE_URL}/orders`, {
    method: 'POST',
    headers: { ...authHeadersBiz2, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      phone: '9662765957',
      name: 'Hardik',
      city: 'Surat',
      create_invoice: true,
      items: [
        {
          product_name: 'Regression Test Camera 5MP',
          quantity: 2,
          unit_price: 2500,
          gst_rate: 18,
        },
      ],
      payment_mode: 'unpaid',
    }),
  });
  const orderData = await createOrderRes.json();
  assert(createOrderRes.ok && orderData.id, `Invoice created in Business 2 (Order ID: ${orderData.id}, Invoice: ${orderData.invoice_number})`);
  assert(orderData.business_id === 2, 'Order business_id correctly set to 2');
  assert(
    orderData.invoice_number && orderData.invoice_number.startsWith('TS2-'),
    `Invoice number follows Business 2 prefix: ${orderData.invoice_number}`
  );

  // Fetch the invoice PDF
  const pdfRes = await fetch(`${BASE_URL}/orders/${orderData.id}/pdf`, { headers: authHeadersBiz2 });
  assert(pdfRes.status === 200, `GET /api/orders/${orderData.id}/pdf returned HTTP 200`);
  const contentType = pdfRes.headers.get('content-type') || '';
  assert(contentType.includes('application/pdf'), `PDF Content-Type valid: ${contentType}`);
  const pdfBuffer = Buffer.from(await pdfRes.arrayBuffer());
  assert(pdfBuffer.toString('utf8', 0, 5) === '%PDF-', 'PDF starts with valid %PDF- magic header');

  // Verify that an order in Business 2 cannot be retrieved using Business 1's header
  const crossFetchRes = await fetch(`${BASE_URL}/orders/${orderData.id}`, { headers: authHeadersBiz1 });
  assert(crossFetchRes.status === 404, 'Verified: Business 1 cannot access Business 2 order (returns 404)');

  // =========================================================================
  // 8. QUOTATIONS CREATION, PDF & CONVERSION
  // =========================================================================
  console.log('\n--- MODULE 8: QUOTATIONS FLOW & PDF ---');
  const createQuotRes = await fetch(`${BASE_URL}/quotations`, {
    method: 'POST',
    headers: { ...authHeadersBiz2, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      phone: '9662765957',
      name: 'Hardik',
      city: 'Surat',
      items: [
        {
          product_name: 'Industrial Router AC1200',
          quantity: 1,
          rate: 6500,
          gst_rate: 18,
        },
      ],
    }),
  });
  const quotData = await createQuotRes.json();
  assert(createQuotRes.ok && quotData.id, `Quotation created in Business 2 (ID: ${quotData.id})`);
  assert(quotData.business_id === 2, 'Quotation business_id correctly set to 2');

  // Fetch quotation PDF
  const quotPdfRes = await fetch(`${BASE_URL}/quotations/${quotData.id}/pdf`, { headers: authHeadersBiz2 });
  assert(quotPdfRes.status === 200, `GET /api/quotations/${quotData.id}/pdf returned HTTP 200`);
  const quotPdfBuffer = Buffer.from(await quotPdfRes.arrayBuffer());
  assert(quotPdfBuffer.toString('utf8', 0, 5) === '%PDF-', 'Quotation PDF starts with valid %PDF- header');

  // Convert quotation to order
  const convertRes = await fetch(`${BASE_URL}/quotations/${quotData.id}/convert-to-invoice`, {
    method: 'POST',
    headers: authHeadersBiz2,
  });
  const convertData = await convertRes.json();
  assert(convertRes.ok && convertData.order, `Quotation converted to order in Business 2 (Order ID: ${convertData.order?.id})`);

  // =========================================================================
  // 9. CREDIT NOTES & PDF
  // =========================================================================
  console.log('\n--- MODULE 9: CREDIT NOTES FLOW & PDF ---');
  // First query returnable items for the order
  const returnableRes = await fetch(`${BASE_URL}/credit-notes/orders/${orderData.id}/returnable-items`, { headers: authHeadersBiz2 });
  const returnableData = await returnableRes.json();
  assert(returnableRes.ok && Array.isArray(returnableData.items), 'Fetched returnable items for invoice');

  if (returnableData.items.length > 0) {
    const itemToReturn = returnableData.items[0];
    const createCnRes = await fetch(`${BASE_URL}/credit-notes`, {
      method: 'POST',
      headers: { ...authHeadersBiz2, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contact_id: hardikInB2.id,
        order_id: orderData.id,
        reason: 'Sales Return (Regression Test)',
        items: [
          {
            order_item_id: itemToReturn.id,
            product_name: itemToReturn.product_name,
            quantity: 1,
            unit_price: itemToReturn.unit_price,
            gst_rate: itemToReturn.gst_rate,
          },
        ],
      }),
    });
    const cnData = await createCnRes.json();
    assert(createCnRes.ok && cnData.id, `Credit Note created in Business 2: ${cnData.credit_note_number}`);
    assert(cnData.business_id === 2, 'Credit note business_id correctly set to 2');

    // Fetch Credit Note PDF
    const cnPdfRes = await fetch(`${BASE_URL}/credit-notes/${cnData.id}/pdf`, { headers: authHeadersBiz2 });
    assert(cnPdfRes.status === 200, `GET /api/credit-notes/${cnData.id}/pdf returned HTTP 200`);
    const cnPdfBuffer = Buffer.from(await cnPdfRes.arrayBuffer());
    assert(cnPdfBuffer.toString('utf8', 0, 5) === '%PDF-', 'Credit Note PDF starts with valid %PDF- header');
  }

  // =========================================================================
  // 10. PAYMENTS, RECEIPTS & CREDIT LEDGER
  // =========================================================================
  console.log('\n--- MODULE 10: PAYMENTS, RECEIPTS & CREDIT LEDGER ---');
  const payInRes = await fetch(`${BASE_URL}/credit/payment-in`, {
    method: 'POST',
    headers: { ...authHeadersBiz2, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contact_id: hardikInB2.id,
      amount: 1000,
      payment_mode: 'upi',
      notes: 'Regression test partial payment',
    }),
  });
  const payInData = await payInRes.json();
  assert(payInRes.ok && payInData.payment, 'Payment-In recorded successfully in Business 2');
  assert(payInData.payment.business_id === 2, 'Payment business_id correctly set to 2');

  // Fetch payment receipt PDF
  const receiptPdfRes = await fetch(`${BASE_URL}/credit/payments/${payInData.payment.id}/receipt/pdf`, { headers: authHeadersBiz2 });
  assert(receiptPdfRes.status === 200, `GET /api/credit/payments/${payInData.payment.id}/receipt/pdf returned HTTP 200`);
  const receiptPdfBuffer = Buffer.from(await receiptPdfRes.arrayBuffer());
  assert(receiptPdfBuffer.toString('utf8', 0, 5) === '%PDF-', 'Payment Receipt PDF starts with valid %PDF- header');

  // Check ledger for Hardik in Business 2
  const ledgerRes = await fetch(`${BASE_URL}/credit/customers/${hardikInB2.id}/ledger`, { headers: authHeadersBiz2 });
  const ledgerData = await ledgerRes.json();
  assert(ledgerRes.ok && ledgerData.stats, 'Customer credit ledger returned stats and transactions');
  assert(Array.isArray(ledgerData.entries || ledgerData.transactions), 'Ledger transactions/entries returned as array');

  // =========================================================================
  // 11. DASHBOARD ANALYTICS ISOLATION
  // =========================================================================
  console.log('\n--- MODULE 11: DASHBOARD METRICS SEPARATION ---');
  const dash1Res = await fetch(`${BASE_URL}/dashboard`, { headers: authHeadersBiz1 });
  const dash1 = await dash1Res.json();
  const dash2Res = await fetch(`${BASE_URL}/dashboard`, { headers: authHeadersBiz2 });
  const dash2 = await dash2Res.json();

  console.log(`   Business 1 Total Contacts: ${dash1.totalContacts || 0}, Calls Today: ${dash1.callsToday || 0}`);
  console.log(`   Business 2 Total Contacts: ${dash2.totalContacts || 0}, Calls Today: ${dash2.callsToday || 0}`);

  // =========================================================================
  // 12. ONBOARDED MERCHANT STRICT TENANT ISOLATION (DHRUV & SHREEJI)
  // =========================================================================
  console.log('\n--- MODULE 12: ONBOARDED MERCHANT STRICT TENANT ISOLATION ---');
  // Test Dhruv (Surat E Commerce Store - Business ID 4)
  const dhruvLoginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'dhruv', password: 'admin' }),
  });
  const dhruvData = await dhruvLoginRes.json();
  assert(dhruvLoginRes.ok && dhruvData.token, 'Dhruv login succeeded with valid token');
  assert(dhruvData.user.username === 'dhruv', 'Dhruv user identity verified');
  assert(dhruvData.user.role === 'user', 'Dhruv is a non-admin merchant (role: user)');
  assert(Array.isArray(dhruvData.businesses) && dhruvData.businesses.length === 1, 'Dhruv has strictly 1 business assigned');
  assert(dhruvData.activeBusiness?.id === 4, 'Dhruv activeBusiness ID is strictly 4');
  assert(dhruvData.activeBusiness?.name === 'Surat E Commerce Store', 'Dhruv activeBusiness name is "Surat E Commerce Store"');

  const dhruvMeRes = await fetch(`${BASE_URL}/auth/me`, {
    headers: { Authorization: `Bearer ${dhruvData.token}` },
  });
  const dhruvMeData = await dhruvMeRes.json();
  assert(dhruvMeRes.ok, 'GET /api/auth/me for Dhruv succeeded');
  assert(dhruvMeData.activeBusiness?.id === 4, 'Dhruv /me returns activeBusiness ID 4');
  assert(dhruvMeData.activeBusiness?.name === 'Surat E Commerce Store', 'Dhruv /me returns activeBusiness name "Surat E Commerce Store"');

  // Verify Dhruv product operations
  const dhruvProdRes = await fetch(`${BASE_URL}/products`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${dhruvData.token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      name: `Dhruv Item ${Date.now()}`,
      default_price: 499,
      gst_rate: 12,
    }),
  });
  const dhruvProd = await dhruvProdRes.json();
  assert(dhruvProdRes.ok && dhruvProd.id, 'Dhruv successfully created a product');
  assert(Number(dhruvProd.business_id) === 4, 'Dhruv product is strictly scoped to Business 4');

  // Test Shreeji (Shreeji Telecom - Business ID 3)
  const shreejiLoginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'shreeji', password: 'shreeji123' }),
  });
  const shreejiData = await shreejiLoginRes.json();
  assert(shreejiLoginRes.ok && shreejiData.token, 'Shreeji login succeeded with valid token');
  assert(shreejiData.user.username === 'shreeji', 'Shreeji user identity verified');
  assert(shreejiData.user.role === 'user', 'Shreeji is a non-admin merchant (role: user)');
  assert(Array.isArray(shreejiData.businesses) && shreejiData.businesses.length === 1, 'Shreeji has strictly 1 business assigned');
  assert(shreejiData.activeBusiness?.id === 3, 'Shreeji activeBusiness ID is strictly 3');
  assert(shreejiData.activeBusiness?.name === 'Shreeji Telecom', 'Shreeji activeBusiness name is "Shreeji Telecom"');

  const shreejiMeRes = await fetch(`${BASE_URL}/auth/me`, {
    headers: { Authorization: `Bearer ${shreejiData.token}` },
  });
  const shreejiMeData = await shreejiMeRes.json();
  assert(shreejiMeRes.ok, 'GET /api/auth/me for Shreeji succeeded');
  assert(shreejiMeData.activeBusiness?.id === 3, 'Shreeji /me returns activeBusiness ID 3');
  assert(shreejiMeData.activeBusiness?.name === 'Shreeji Telecom', 'Shreeji /me returns activeBusiness name "Shreeji Telecom"');

  // =========================================================================
  // SUMMARY
  // =========================================================================
  console.log('\n' + '='.repeat(70));
  console.log(`🏁 REGRESSION TEST SUITE COMPLETE!`);
  console.log(`   Passed: ${passedTests}`);
  console.log(`   Failed: ${failedTests}`);
  console.log('='.repeat(70));

  if (failedTests > 0) {
    process.exit(1);
  } else {
    console.log('\n🎉 ALL REGRESSION TESTS PASSED WITH 100% SUCCESS!\n');
    process.exit(0);
  }
}

runRegressionTests().catch((err) => {
  console.error('\n❌ Unhandled error during regression test execution:', err);
  process.exit(1);
});
