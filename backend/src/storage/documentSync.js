import { pool } from '../db.js';
import { storageService } from './storageService.js';
import { buildInvoiceHtml } from '../pdf/invoiceHtml.js';
import { buildQuotationHtml } from '../pdf/quotationHtml.js';
import { buildCreditNoteHtml } from '../pdf/creditNoteHtml.js';
import { buildPaymentReceiptHtml } from '../pdf/paymentReceiptHtml.js';
import { renderHtmlToPdf } from '../pdf/renderPdf.js';

export async function loadBusinessProfile() {
  const result = await pool.query('SELECT * FROM business_profile WHERE id = 1');
  return result.rows[0] || null;
}

/**
 * Generate and upload invoice PDF to Cloud Storage immediately
 */
export async function syncInvoicePdf(orderId, options = {}) {
  try {
    const { loadFullOrder } = await import('../routes/orders.js');
    const order = await loadFullOrder(orderId);
    if (!order) return null;
    const business = await loadBusinessProfile();
    const showPaymentDetails = Boolean(options.showPaymentDetails);
    const invoiceNum = order.invoice_number || order.order_number || order.id;
    const cacheKey = `invoices/Tax-Invoice-${invoiceNum}${showPaymentDetails ? '-paid' : ''}.pdf`;

    const html = buildInvoiceHtml({ business, order, items: order.items, showPaymentDetails });
    const pdf = await renderHtmlToPdf(html);

    const uploaded = await storageService.uploadFile({
      key: cacheKey,
      buffer: pdf,
      contentType: 'application/pdf',
      isPublic: true,
    });
    console.log(`[DocumentSync] Synced Invoice PDF: ${cacheKey} (${pdf.length} bytes)`);
    return { ...uploaded, buffer: pdf };
  } catch (err) {
    console.warn(`[DocumentSync] Failed to sync invoice PDF for order ${orderId}:`, err.message);
    return null;
  }
}

/**
 * Generate and upload quotation PDF to Cloud Storage immediately
 */
export async function syncQuotationPdf(quotationId) {
  try {
    const { loadFullQuotation } = await import('../routes/quotations.js');
    const quotation = await loadFullQuotation(quotationId);
    if (!quotation) return null;
    const business = await loadBusinessProfile();
    const cacheKey = `quotations/Quotation-${quotation.id}.pdf`;

    const html = buildQuotationHtml({ business, quotation, items: quotation.items });
    const pdf = await renderHtmlToPdf(html);

    const uploaded = await storageService.uploadFile({
      key: cacheKey,
      buffer: pdf,
      contentType: 'application/pdf',
      isPublic: true,
    });
    console.log(`[DocumentSync] Synced Quotation PDF: ${cacheKey} (${pdf.length} bytes)`);
    return { ...uploaded, buffer: pdf };
  } catch (err) {
    console.warn(`[DocumentSync] Failed to sync quotation PDF for ${quotationId}:`, err.message);
    return null;
  }
}

/**
 * Generate and upload credit note PDF to Cloud Storage immediately
 */
export async function syncCreditNotePdf(creditNoteId) {
  try {
    const cnRes = await pool.query('SELECT * FROM credit_notes WHERE id = $1', [creditNoteId]);
    if (cnRes.rows.length === 0) return null;
    const creditNote = cnRes.rows[0];
    const safeNumber = (creditNote.credit_note_number || `credit_note_${creditNote.id}`).replace(/[^a-zA-Z0-9_-]/g, '_');
    const cacheKey = `credit-notes/Credit-Note-${safeNumber}.pdf`;

    const [itemsRes, contactRes, business] = await Promise.all([
      pool.query('SELECT * FROM credit_note_items WHERE credit_note_id = $1 ORDER BY id ASC', [creditNote.id]),
      pool.query('SELECT * FROM contacts WHERE id = $1', [creditNote.contact_id]),
      loadBusinessProfile(),
    ]);

    const contact = contactRes.rows[0] || {};
    const items = itemsRes.rows;

    const html = buildCreditNoteHtml({ business, creditNote, items, contact });
    const pdfBuffer = await renderHtmlToPdf(html);

    const uploaded = await storageService.uploadFile({
      key: cacheKey,
      buffer: pdfBuffer,
      contentType: 'application/pdf',
      isPublic: true,
    });
    console.log(`[DocumentSync] Synced Credit Note PDF: ${cacheKey} (${pdfBuffer.length} bytes)`);
    return { ...uploaded, buffer: pdfBuffer };
  } catch (err) {
    console.warn(`[DocumentSync] Failed to sync credit note PDF for ${creditNoteId}:`, err.message);
    return null;
  }
}

/**
 * Generate and upload payment receipt PDF to Cloud Storage immediately
 */
export async function syncPaymentReceiptPdf(paymentId) {
  try {
    const payRes = await pool.query('SELECT * FROM payments WHERE id = $1', [paymentId]);
    if (payRes.rows.length === 0) return null;
    const payment = payRes.rows[0];

    const [business, contactRes, orderRes] = await Promise.all([
      loadBusinessProfile(),
      pool.query('SELECT * FROM contacts WHERE id = $1', [payment.contact_id]),
      payment.order_id ? pool.query('SELECT * FROM orders WHERE id = $1', [payment.order_id]) : Promise.resolve({ rows: [] }),
    ]);

    const contact = contactRes.rows[0] || {};
    const order = orderRes.rows[0] || null;

    let closingBalance = null;
    try {
      const { getCustomerLedgerData } = await import('../routes/credit.js');
      const ledger = await getCustomerLedgerData(payment.contact_id);
      closingBalance = ledger.stats?.current_balance;
    } catch {
      // ignore
    }

    const receiptNum = (payment.receipt_number || `REC_${payment.id}`).replace(/[^a-zA-Z0-9_-]/g, '_');
    const cacheKey = `receipts/Payment-Receipt-${receiptNum}.pdf`;

    const html = buildPaymentReceiptHtml({
      business,
      payment,
      contact,
      order,
      closingBalance,
    });
    const pdf = await renderHtmlToPdf(html);

    const uploaded = await storageService.uploadFile({
      key: cacheKey,
      buffer: pdf,
      contentType: 'application/pdf',
      isPublic: true,
    });
    console.log(`[DocumentSync] Synced Payment Receipt PDF: ${cacheKey} (${pdf.length} bytes)`);
    return { ...uploaded, buffer: pdf };
  } catch (err) {
    console.warn(`[DocumentSync] Failed to sync payment receipt PDF for ${paymentId}:`, err.message);
    return null;
  }
}
