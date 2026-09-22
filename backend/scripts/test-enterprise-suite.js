import http from 'http';

const BASE_URL = 'http://localhost:4000';

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE_URL);
    const reqOpts = {
      method: options.method || 'GET',
      headers: options.headers || {},
    };

    const req = http.request(url, reqOpts, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => {
        const body = Buffer.concat(chunks);
        const contentType = res.headers['content-type'] || '';
        let json = null;
        if (contentType.includes('application/json')) {
          try {
            json = JSON.parse(body.toString());
          } catch (_) {}
        }
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body,
          json,
        });
      });
    });

    req.on('error', reject);

    if (options.body) {
      if (typeof options.body === 'string' || Buffer.isBuffer(options.body)) {
        req.write(options.body);
      } else {
        req.setHeader('Content-Type', 'application/json');
        req.write(JSON.stringify(options.body));
      }
    }
    req.end();
  });
}

async function runTests() {
  console.log('\n========================================================');
  console.log('🧪 ENTERPRISE REGRESSION & STRESS TEST SUITE');
  console.log('========================================================\n');

  let passed = 0;
  let failed = 0;

  async function test(name, fn) {
    process.stdout.write(`• Testing: ${name}... `);
    try {
      await fn();
      console.log('✅ PASSED');
      passed++;
    } catch (err) {
      console.log(`❌ FAILED: ${err.message}`);
      failed++;
    }
  }

  // 1. Health & Diagnostics
  await test('Health check endpoint returns healthy with diagnostics', async () => {
    const res = await request('/api/health');
    if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
    if (res.json.status !== 'healthy') throw new Error(`Status was ${res.json.status}`);
    if (!res.json.database.ok) throw new Error('Database is not ok');
    if (typeof res.json.pdfRenderer.max !== 'number') throw new Error('PDF renderer status missing');
  });

  // 2. Orders & Invoices List
  let testOrderId = null;
  await test('GET /api/orders lists orders and returns 200', async () => {
    const res = await request('/api/orders');
    if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
    if (!Array.isArray(res.json)) throw new Error('Expected array of orders');
    if (res.json.length > 0) testOrderId = res.json[0].id;
  });

  // 3. Invoice PDF Generation & Streaming
  if (testOrderId) {
    await test(`GET /api/orders/${testOrderId}/pdf streams PDF (200)`, async () => {
      const res = await request(`/api/orders/${testOrderId}/pdf`);
      if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
      if (!res.headers['content-type']?.includes('application/pdf')) throw new Error('Not application/pdf');
      if (res.body.length < 5000) throw new Error(`PDF too small: ${res.body.length} bytes`);
    });
  }

  // 4. Create New Invoice (POST /api/orders)
  let createdOrderId = null;
  await test('POST /api/orders creates invoice with eager sync', async () => {
    const res = await request('/api/orders', {
      method: 'POST',
      body: {
        name: 'Enterprise Test Party',
        phone: '9900112233',
        city: 'Surat',
        create_invoice: true,
        items: [
          { product_name: 'Raw Cotton', quantity: 5, unit_price: 200, gst_rate: 5 }
        ]
      }
    });
    if (res.statusCode !== 201) throw new Error(`HTTP ${res.statusCode}: ${res.body.toString()}`);
    if (!res.json.id) throw new Error('No order id returned');
    createdOrderId = res.json.id;
  });

  // 5. Quotations List & PDF
  let testQuotId = null;
  await test('GET /api/quotations lists quotations and returns 200', async () => {
    const res = await request('/api/quotations');
    if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
    if (!Array.isArray(res.json)) throw new Error('Expected array of quotations');
    if (res.json.length > 0) testQuotId = res.json[0].id;
  });

  if (testQuotId) {
    await test(`GET /api/quotations/${testQuotId}/pdf streams PDF (200)`, async () => {
      const res = await request(`/api/quotations/${testQuotId}/pdf`);
      if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
      if (!res.headers['content-type']?.includes('application/pdf')) throw new Error('Not application/pdf');
    });
  }

  // 6. Create Quotation (POST /api/quotations)
  let createdQuotId = null;
  await test('POST /api/quotations creates quotation with eager sync', async () => {
    const res = await request('/api/quotations', {
      method: 'POST',
      body: {
        name: 'Quotation Test Party',
        phone: '9888777666',
        city: 'Ahmedabad',
        items: [
          { product_name: 'Silk Yarn', quantity: 2, rate: 1500, gst_rate: 18 }
        ]
      }
    });
    if (res.statusCode !== 201) throw new Error(`HTTP ${res.statusCode}: ${res.body.toString()}`);
    createdQuotId = res.json.id;
  });

  // 7. Credit Notes List & PDF
  let testCnId = null;
  await test('GET /api/credit-notes lists credit notes and returns 200', async () => {
    const res = await request('/api/credit-notes');
    if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
    if (!Array.isArray(res.json)) throw new Error('Expected array of credit notes');
    if (res.json.length > 0) testCnId = res.json[0].id;
  });

  if (testCnId) {
    await test(`GET /api/credit-notes/${testCnId}/pdf streams PDF (200)`, async () => {
      const res = await request(`/api/credit-notes/${testCnId}/pdf`);
      if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
      if (!res.headers['content-type']?.includes('application/pdf')) throw new Error('Not application/pdf');
    });
  }

  // 8. Khata / Customers List & Statement PDF
  let testCustomerId = null;
  await test('GET /api/credit/customers lists customers and returns 200', async () => {
    const res = await request('/api/credit/customers');
    if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
    if (!Array.isArray(res.json)) throw new Error('Expected array of customers');
    if (res.json.length > 0) testCustomerId = res.json[0].id;
  });

  if (testCustomerId) {
    await test(`GET /api/credit/customers/${testCustomerId}/statement/pdf streams statement PDF`, async () => {
      const res = await request(`/api/credit/customers/${testCustomerId}/statement/pdf`);
      if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
      if (!res.headers['content-type']?.includes('application/pdf')) throw new Error('Not application/pdf');
    });
  }

  // 9. Payment Receipt PDF
  await test('GET /api/credit/payments/:id/receipt/pdf streams receipt PDF', async () => {
    const { pool } = await import('../src/db.js');
    const payRes = await pool.query('SELECT id FROM payments LIMIT 1');
    if (payRes.rows.length === 0) return;
    const payId = payRes.rows[0].id;
    const res = await request(`/api/credit/payments/${payId}/receipt/pdf`);
    if (res.statusCode !== 200) throw new Error(`HTTP ${res.statusCode}`);
    if (!res.headers['content-type']?.includes('application/pdf')) throw new Error('Not application/pdf');
  });

  // 10. Concurrency & Queue Stress Test (simultaneous requests)
  await test('PDF Renderer handles 6 simultaneous PDF renders cleanly through concurrency queue', async () => {
    const tasks = [];
    for (let i = 0; i < 6; i++) {
      tasks.push(request(`/api/orders/${testOrderId || 1}/pdf`));
    }
    const results = await Promise.all(tasks);
    for (const r of results) {
      if (r.statusCode !== 200) throw new Error(`Failed with status ${r.statusCode}`);
    }
  });

  // 11. Cleanup test records
  if (createdOrderId) {
    await test('Clean up temporary test order', async () => {
      const { pool } = await import('../src/db.js');
      await pool.query('DELETE FROM order_items WHERE order_id = $1', [createdOrderId]);
      await pool.query('DELETE FROM orders WHERE id = $1', [createdOrderId]);
      const { storageService } = await import('../src/storage/storageService.js');
      await storageService.deleteFile(`invoices/Tax-Invoice-INV-${createdOrderId}.pdf`);
    });
  }
  if (createdQuotId) {
    await test('Clean up temporary test quotation', async () => {
      const { pool } = await import('../src/db.js');
      await pool.query('DELETE FROM quotation_items WHERE quotation_id = $1', [createdQuotId]);
      await pool.query('DELETE FROM quotations WHERE id = $1', [createdQuotId]);
      const { storageService } = await import('../src/storage/storageService.js');
      await storageService.deleteFile(`quotations/Quotation-${createdQuotId}.pdf`);
    });
  }

  console.log('\n========================================================');
  console.log(`📊 RESULTS: ${passed} passed, ${failed} failed`);
  console.log('========================================================\n');

  if (failed > 0) process.exit(1);
  process.exit(0);
}

runTests().catch(err => {
  console.error('Fatal test error:', err);
  process.exit(1);
});
