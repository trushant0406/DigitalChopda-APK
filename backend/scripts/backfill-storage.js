import { pool } from '../src/db.js';
import { storageService } from '../src/storage/storageService.js';
import {
  syncInvoicePdf,
  syncQuotationPdf,
  syncCreditNotePdf,
  syncPaymentReceiptPdf,
  loadBusinessProfile,
} from '../src/storage/documentSync.js';
import {
  getDocumentStorageKey,
  getDocumentFallbackKeys,
  findCachedDocumentBuffer,
} from '../src/storage/storageKeyHelper.js';

async function backfill() {
  console.log(`\n🚀 [Backfill] Starting Cloud Storage Backfill (Driver: ${storageService.getDriverName()})...\n`);

  // 1. Invoices
  const ordersRes = await pool.query(
    `SELECT orders.id, orders.order_number, orders.invoice_number, orders.business_id, contacts.name AS contact_name
     FROM orders
     LEFT JOIN contacts ON contacts.id = orders.contact_id
     ORDER BY orders.id ASC`
  );
  console.log(`📦 Found ${ordersRes.rows.length} orders/invoices to verify.`);
  let invoiceSynced = 0;
  for (const ord of ordersRes.rows) {
    const invNum = ord.invoice_number || ord.order_number || ord.id;
    const business = await loadBusinessProfile(ord.business_id || 1);
    const filename = `Tax-Invoice-${invNum}.pdf`;
    const primaryKey = getDocumentStorageKey({ business, contactName: ord.contact_name, docType: 'invoices', filename });
    const fallbackKeys = getDocumentFallbackKeys({ business, contactName: ord.contact_name, docType: 'invoices', filename });
    const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
    if (!cached) {
      console.log(`  -> Generating and syncing missing invoice PDF: ${primaryKey}...`);
      await syncInvoicePdf(ord.id);
      invoiceSynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cached.key}`);
    }
  }

  // 2. Quotations
  const quotRes = await pool.query(
    `SELECT id, bill_to_name, business_id FROM quotations ORDER BY id ASC`
  );
  console.log(`\n📋 Found ${quotRes.rows.length} quotations to verify.`);
  let quotSynced = 0;
  for (const q of quotRes.rows) {
    const business = await loadBusinessProfile(q.business_id || 1);
    const filename = `Quotation-${q.id}.pdf`;
    const primaryKey = getDocumentStorageKey({ business, contactName: q.bill_to_name, docType: 'quotations', filename });
    const fallbackKeys = getDocumentFallbackKeys({ business, contactName: q.bill_to_name, docType: 'quotations', filename });
    const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
    if (!cached) {
      console.log(`  -> Generating and syncing missing quotation PDF: ${primaryKey}...`);
      await syncQuotationPdf(q.id);
      quotSynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cached.key}`);
    }
  }

  // 3. Credit Notes
  const cnRes = await pool.query(
    `SELECT credit_notes.id, credit_notes.credit_note_number, credit_notes.business_id, contacts.name AS contact_name
     FROM credit_notes
     LEFT JOIN contacts ON contacts.id = credit_notes.contact_id
     ORDER BY credit_notes.id ASC`
  );
  console.log(`\n💳 Found ${cnRes.rows.length} credit notes to verify.`);
  let cnSynced = 0;
  for (const cn of cnRes.rows) {
    const safeNumber = (cn.credit_note_number || `credit_note_${cn.id}`).replace(/[^a-zA-Z0-9_-]/g, '_');
    const business = await loadBusinessProfile(cn.business_id || 1);
    const filename = `Credit-Note-${safeNumber}.pdf`;
    const primaryKey = getDocumentStorageKey({ business, contactName: cn.contact_name, docType: 'credit-notes', filename });
    const fallbackKeys = getDocumentFallbackKeys({ business, contactName: cn.contact_name, docType: 'credit-notes', filename });
    const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
    if (!cached) {
      console.log(`  -> Generating and syncing missing credit note PDF: ${primaryKey}...`);
      await syncCreditNotePdf(cn.id);
      cnSynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cached.key}`);
    }
  }

  // 4. Payment Receipts
  const payRes = await pool.query(
    `SELECT payments.id, payments.receipt_number, payments.business_id, contacts.name AS contact_name
     FROM payments
     LEFT JOIN contacts ON contacts.id = payments.contact_id
     WHERE payments.receipt_number IS NOT NULL
     ORDER BY payments.id ASC`
  );
  console.log(`\n🧾 Found ${payRes.rows.length} payment receipts to verify.`);
  let paySynced = 0;
  for (const p of payRes.rows) {
    const receiptNum = p.receipt_number.replace(/[^a-zA-Z0-9_-]/g, '_');
    const business = await loadBusinessProfile(p.business_id || 1);
    const filename = `Payment-Receipt-${receiptNum}.pdf`;
    const primaryKey = getDocumentStorageKey({ business, contactName: p.contact_name, docType: 'receipts', filename });
    const fallbackKeys = getDocumentFallbackKeys({ business, contactName: p.contact_name, docType: 'receipts', filename });
    const cached = await findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys);
    if (!cached) {
      console.log(`  -> Generating and syncing missing payment receipt PDF: ${primaryKey}...`);
      await syncPaymentReceiptPdf(p.id);
      paySynced++;
    } else {
      console.log(`  ✓ Already in storage: ${cached.key}`);
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
