import { pool } from '../src/db.js';
import { storageService } from '../src/storage/storageService.js';
import {
  syncInvoicePdf,
  syncQuotationPdf,
  syncCreditNotePdf,
  syncPaymentReceiptPdf,
} from '../src/storage/documentSync.js';

async function backfill() {
  console.log(`\n🚀 [Backfill] Starting Cloud Storage Backfill (Driver: ${storageService.getDriverName()})...\n`);

  // 1. Invoices
  const ordersRes = await pool.query(
    `SELECT id, order_number, invoice_number FROM orders ORDER BY id ASC`
  );
  console.log(`📦 Found ${ordersRes.rows.length} orders/invoices to verify.`);
  let invoiceSynced = 0;
  for (const ord of ordersRes.rows) {
    const invNum = ord.invoice_number || ord.order_number || ord.id;
    const cacheKey = `invoices/Tax-Invoice-${invNum}.pdf`;
    const exists = await storageService.exists(cacheKey);
    if (!exists) {
      console.log(`  -> Generating and syncing missing invoice PDF: ${cacheKey}...`);
      await syncInvoicePdf(ord.id);
      invoiceSynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cacheKey}`);
    }
  }

  // 2. Quotations
  const quotRes = await pool.query(`SELECT id FROM quotations ORDER BY id ASC`);
  console.log(`\n📋 Found ${quotRes.rows.length} quotations to verify.`);
  let quotSynced = 0;
  for (const q of quotRes.rows) {
    const cacheKey = `quotations/Quotation-${q.id}.pdf`;
    const exists = await storageService.exists(cacheKey);
    if (!exists) {
      console.log(`  -> Generating and syncing missing quotation PDF: ${cacheKey}...`);
      await syncQuotationPdf(q.id);
      quotSynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cacheKey}`);
    }
  }

  // 3. Credit Notes
  const cnRes = await pool.query(`SELECT id, credit_note_number FROM credit_notes ORDER BY id ASC`);
  console.log(`\n💳 Found ${cnRes.rows.length} credit notes to verify.`);
  let cnSynced = 0;
  for (const cn of cnRes.rows) {
    const safeNumber = (cn.credit_note_number || `credit_note_${cn.id}`).replace(/[^a-zA-Z0-9_-]/g, '_');
    const cacheKey = `credit-notes/Credit-Note-${safeNumber}.pdf`;
    const exists = await storageService.exists(cacheKey);
    if (!exists) {
      console.log(`  -> Generating and syncing missing credit note PDF: ${cacheKey}...`);
      await syncCreditNotePdf(cn.id);
      cnSynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cacheKey}`);
    }
  }

  // 4. Payment Receipts
  const payRes = await pool.query(`SELECT id, receipt_number FROM payments WHERE receipt_number IS NOT NULL ORDER BY id ASC`);
  console.log(`\n🧾 Found ${payRes.rows.length} payment receipts to verify.`);
  let paySynced = 0;
  for (const p of payRes.rows) {
    const receiptNum = p.receipt_number.replace(/[^a-zA-Z0-9_-]/g, '_');
    const cacheKey = `receipts/Payment-Receipt-${receiptNum}.pdf`;
    const exists = await storageService.exists(cacheKey);
    if (!exists) {
      console.log(`  -> Generating and syncing missing payment receipt PDF: ${cacheKey}...`);
      await syncPaymentReceiptPdf(p.id);
      paySynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cacheKey}`);
    }
  }

  console.log('\n' + '='.repeat(60));
  console.log(`🎉 [Backfill Complete!]`);
  console.log(`   Invoices synced:       ${invoiceSynced}`);
  console.log(`   Quotations synced:     ${quotSynced}`);
  console.log(`   Credit Notes synced:   ${cnSynced}`);
  console.log(`   Payment Receipts:      ${paySynced}`);
  console.log('='.repeat(60) + '\n');
  process.exit(0);
}

backfill().catch((err) => {
  console.error('❌ Backfill failed:', err);
  process.exit(1);
});
