import http from 'http';
import { pool } from '../src/db.js';

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
        'X-Business-Id': '1',
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
        } catch (e) {
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

async function runAllTests() {
  console.log('===============================================================');
  console.log('🧪 COMPREHENSIVE SUITE: EDIT & COMPLETE DELETE INVOICE (MYBILLBOOK)');
  console.log('===============================================================\n');

  let orderId = null;
  let siblingOrderId = null;
  let creditNoteId = null;

  try {
    // -------------------------------------------------------------
    // CASE 1: Standard Invoice Creation (Pending Payment)
    // -------------------------------------------------------------
    console.log('📌 CASE 1: Create Sales Invoice (Pending Payment)');
    const createRes = await req('POST', '/orders', {
      phone: '9988776655',
      name: 'Original Test Merchant',
      city: 'Surat',
      create_invoice: true,
      invoice_number: 'TEST-MBB-101',
      invoice_date: '2026-10-02',
      items: [
        { product_name: 'Raw Cotton', quantity: 10, unit_price: 150, gst_rate: 5 },
        { product_name: 'Packaging Box', quantity: 5, unit_price: 40, gst_rate: 18 },
      ],
      payment_status: 'pending',
    });

    assert(createRes.status === 201, `Invoice created with HTTP 201 (got ${createRes.status})`);
    orderId = createRes.body?.id;
    assert(orderId > 0, `Valid order ID generated: ${orderId}`);
    assert(createRes.body.invoice_number === 'TEST-MBB-101', `Invoice number is TEST-MBB-101`);
    // Calculation: (10*150=1500, tax 75) + (5*40=200, tax 36) => Subtotal: 1700, Tax: 111, Grand Total: 1811
    assert(Number(createRes.body.total_amount) === 1700, `Taxable subtotal is ₹1700.00`);
    assert(Number(createRes.body.tax_amount) === 111, `GST tax amount is ₹111.00`);
    assert(Number(createRes.body.grand_total) === 1811, `Grand total is ₹1811.00`);
    assert(createRes.body.payment_status === 'pending', `Initial payment status is 'pending'`);
    assert(Number(createRes.body.balance_due) === 1811, `Balance due equals grand total (₹1811.00)`);

    // -------------------------------------------------------------
    // CASE 2: Edit Customer / Party Details & Inter-State Place of Supply
    // -------------------------------------------------------------
    console.log('\n📌 CASE 2: Edit Customer Details & Inter-State Place of Supply');
    const editPartyRes = await req('PUT', `/orders/${orderId}`, {
      phone: '9123456780',
      name: 'Interstate Buyer Pvt Ltd',
      city: 'Mumbai',
      state: 'Maharashtra',
      gstin: '27AABCB1234F1Z5',
      place_of_supply: '27 - Maharashtra',
      tax_mode: 'igst',
      items: [
        { product_name: 'Raw Cotton', quantity: 10, unit_price: 150, gst_rate: 5 },
        { product_name: 'Packaging Box', quantity: 5, unit_price: 40, gst_rate: 18 },
      ],
      payment_status: 'pending',
    });

    assert(editPartyRes.status === 200, `Party edit returned HTTP 200`);
    assert(editPartyRes.body.name === 'Interstate Buyer Pvt Ltd', `Customer name updated`);
    assert(editPartyRes.body.city === 'Mumbai', `City updated to Mumbai`);
    assert(editPartyRes.body.state === 'Maharashtra', `State updated to Maharashtra`);
    assert(editPartyRes.body.gstin === '27AABCB1234F1Z5', `GSTIN updated`);
    assert(editPartyRes.body.tax_mode === 'igst', `Tax mode switched to IGST for interstate supply`);

    // -------------------------------------------------------------
    // CASE 3: Edit Line Items, Quantities, Unit Prices, GST Rates
    // -------------------------------------------------------------
    console.log('\n📌 CASE 3: Edit Line Items (Add item, change quantities, rates)');
    const editItemsRes = await req('PUT', `/orders/${orderId}`, {
      phone: '9123456780',
      name: 'Interstate Buyer Pvt Ltd',
      city: 'Mumbai',
      invoice_number: 'TEST-MBB-101',
      items: [
        { product_name: 'Premium Fabric', quantity: 20, unit_price: 200, gst_rate: 12 }, // 4000 + 480 = 4480
        { product_name: 'Fancy Buttons', quantity: 50, unit_price: 10, gst_rate: 18 },   // 500 + 90 = 590
      ],
      payment_status: 'pending',
    });

    assert(editItemsRes.status === 200, `Item modifications returned HTTP 200`);
    assert(editItemsRes.body.items?.length === 2, `2 line items present after edit`);
    // 4000 + 500 = 4500 subtotal; 480 + 90 = 570 tax; Grand Total = 5070
    assert(Number(editItemsRes.body.total_amount) === 4500, `New taxable subtotal is ₹4500.00`);
    assert(Number(editItemsRes.body.tax_amount) === 570, `New tax amount is ₹570.00`);
    assert(Number(editItemsRes.body.grand_total) === 5070, `New grand total is ₹5070.00`);
    assert(Number(editItemsRes.body.balance_due) === 5070, `Updated balance due is ₹5070.00`);

    // -------------------------------------------------------------
    // CASE 4: Edit Invoice Number Uniqueness Guard (MyBillBook style)
    // -------------------------------------------------------------
    console.log('\n📌 CASE 4: Invoice Number Uniqueness Guard');
    // Create a sibling order to test conflict
    const sibRes = await req('POST', '/orders', {
      phone: '9888888888',
      name: 'Sibling Customer',
      invoice_number: 'TEST-MBB-SIBLING',
      items: [{ product_name: 'Thread', quantity: 1, unit_price: 100, gst_rate: 5 }],
    });
    siblingOrderId = sibRes.body?.id;
    assert(sibRes.status === 201, `Sibling invoice created with TEST-MBB-SIBLING`);

    // Try to update current invoice to sibling's number -> Should fail with 400
    const dupRes = await req('PUT', `/orders/${orderId}`, {
      phone: '9123456780',
      invoice_number: 'TEST-MBB-SIBLING',
      items: [{ product_name: 'Premium Fabric', quantity: 20, unit_price: 200, gst_rate: 12 }],
    });
    assert(dupRes.status === 400, `Duplicate invoice number rejected with HTTP 400`);
    assert(dupRes.body.error?.includes('already exists'), `Error message correctly reports duplicate invoice number`);

    // Update current invoice to a new unique number -> Should succeed
    const newNumRes = await req('PUT', `/orders/${orderId}`, {
      phone: '9123456780',
      invoice_number: 'TEST-MBB-101-RENAMED',
      items: [{ product_name: 'Premium Fabric', quantity: 20, unit_price: 200, gst_rate: 12 }],
    });
    assert(newNumRes.status === 200, `Rename to new unique number returned HTTP 200`);
    assert(newNumRes.body.invoice_number === 'TEST-MBB-101-RENAMED', `Invoice number successfully updated to TEST-MBB-101-RENAMED`);

    // -------------------------------------------------------------
    // CASE 5: Payment Settlement Adjustment on Edit (MyBillBook style)
    // -------------------------------------------------------------
    console.log('\n📌 CASE 5: Payment Settlement Adjustment on Edit');
    // Mark as paid in full via UPI
    const payRes = await req('PUT', `/orders/${orderId}`, {
      phone: '9123456780',
      invoice_number: 'TEST-MBB-101-RENAMED',
      items: [{ product_name: 'Premium Fabric', quantity: 20, unit_price: 200, gst_rate: 12 }], // Total = 4480
      payment_status: 'paid',
      payment_method: 'upi',
    });
    assert(payRes.status === 200, `Mark paid returned HTTP 200`);
    assert(payRes.body.payment_status === 'paid', `Payment status is 'paid'`);
    assert(Number(payRes.body.received_amount) === 4480, `Received amount matches invoice total (₹4480.00)`);
    assert(Number(payRes.body.balance_due) === 0, `Balance due is ₹0.00`);

    // Now edit line items to increase total to 30 qty (total = 6720) while remaining 'paid':
    // Payment record should automatically adjust to ₹6720
    const payAdjustRes = await req('PUT', `/orders/${orderId}`, {
      phone: '9123456780',
      invoice_number: 'TEST-MBB-101-RENAMED',
      items: [{ product_name: 'Premium Fabric', quantity: 30, unit_price: 200, gst_rate: 12 }], // Total = 6720
      payment_status: 'paid',
    });
    assert(payAdjustRes.status === 200, `Adjustment with paid status returned HTTP 200`);
    assert(Number(payAdjustRes.body.grand_total) === 6720, `Grand total increased to ₹6720.00`);
    assert(Number(payAdjustRes.body.received_amount) === 6720, `Linked payment amount auto-adjusted to ₹6720.00`);
    assert(Number(payAdjustRes.body.balance_due) === 0, `Balance due remains ₹0.00`);

    // -------------------------------------------------------------
    // CASE 6: Credit Note (Sales Return) Safeguards
    // -------------------------------------------------------------
    console.log('\n📌 CASE 6: Credit Note (Sales Return) Safeguards');
    // Issue a Credit Note of ₹1000 against this invoice
    const cnRes = await req('POST', '/credit-notes', {
      order_id: orderId,
      contact_id: payAdjustRes.body.contact_id,
      credit_note_date: '2026-10-03',
      credit_note_number: 'CN-TEST-001',
      reason: 'Damaged Goods Return',
      items: [{ product_name: 'Premium Fabric', quantity: 4, unit_price: 200, gst_rate: 12 }], // 800 + 96 = 896
    });
    assert(cnRes.status === 201, `Credit Note created with HTTP 201`);
    creditNoteId = cnRes.body?.id;
    const creditedAmt = Number(cnRes.body?.total_amount || 896);

    // Safeguard 6A: Try to edit invoice grand total to ₹500 (which is less than credited ₹896) -> Must fail with 400
    const editBelowReturnRes = await req('PUT', `/orders/${orderId}`, {
      phone: '9123456780',
      invoice_number: 'TEST-MBB-101-RENAMED',
      items: [{ product_name: 'Sample', quantity: 2, unit_price: 200, gst_rate: 12 }], // 448 < 896
    });
    assert(editBelowReturnRes.status === 400, `Edit total below credited return blocked with HTTP 400`);
    assert(editBelowReturnRes.body.error?.includes('cannot be less than already credited'), `Safe error message on reducing total below credit notes`);

    // Safeguard 6B: Try to DELETE invoice while Credit Note is active -> Must fail with 400
    const deleteBlockedRes = await req('DELETE', `/orders/${orderId}`);
    assert(deleteBlockedRes.status === 400, `Deletion of invoice with active Credit Note blocked with HTTP 400`);
    assert(deleteBlockedRes.body.error?.includes('Credit Note'), `Delete error references linked Credit Note`);

    // -------------------------------------------------------------
    // CASE 7: Complete Invoice Deletion (Clean Ledger & Purge)
    // -------------------------------------------------------------
    console.log('\n📌 CASE 7: Complete Invoice Deletion');
    // First remove the Credit Note so the invoice can be deleted
    const delCnRes = await req('DELETE', `/credit-notes/${creditNoteId}`);
    assert(delCnRes.status === 200, `Credit Note deleted successfully`);

    // Now delete the invoice completely
    const delOrderRes = await req('DELETE', `/orders/${orderId}`);
    assert(delOrderRes.status === 200, `Invoice deletion returned HTTP 200`);
    assert(delOrderRes.body.success === true, `Delete response indicated success`);

    // Verify database state: order record deleted
    const verifyGetRes = await req('GET', `/orders/${orderId}`);
    assert(verifyGetRes.status === 404, `GET /orders/:id returns 404 Not Found after deletion`);

    // Verify database state directly: order_items deleted
    const itemsCheck = await pool.query('SELECT count(*) FROM order_items WHERE order_id = $1', [orderId]);
    assert(Number(itemsCheck.rows[0].count) === 0, `All order_items cleanly removed from database`);

    // Verify database state directly: linked payments deleted
    const paymentsCheck = await pool.query('SELECT count(*) FROM payments WHERE order_id = $1', [orderId]);
    assert(Number(paymentsCheck.rows[0].count) === 0, `All linked payments cleanly removed from database`);

    // Clean up sibling order
    if (siblingOrderId) {
      await req('DELETE', `/orders/${siblingOrderId}`);
    }

    console.log('\n===============================================================');
    console.log('🎉 ALL TEST CASES PASSED WITH 100% SUCCESS!');
    console.log('===============================================================');
    process.exit(0);
  } catch (err) {
    console.error('\n❌ TEST RUN FAILED:', err);
    // Cleanup if possible
    if (creditNoteId) await req('DELETE', `/credit-notes/${creditNoteId}`).catch(() => {});
    if (orderId) await req('DELETE', `/orders/${orderId}`).catch(() => {});
    if (siblingOrderId) await req('DELETE', `/orders/${siblingOrderId}`).catch(() => {});
    process.exit(1);
  }
}

runAllTests();
