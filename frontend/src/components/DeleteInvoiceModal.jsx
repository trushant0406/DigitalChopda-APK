import { useState } from 'react';
import { api } from '../api';
import { formatDate } from '../dateFormat';

export default function DeleteInvoiceModal({ order, onClose, onDeleted }) {
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState(null);

  if (!order) return null;

  const rawCns = Array.isArray(order.credit_notes_list)
    ? order.credit_notes_list
    : (typeof order.credit_notes_list === 'string'
        ? JSON.parse(order.credit_notes_list || '[]')
        : []);
  const hasCreditNotes = rawCns.length > 0;
  const cnListStr = rawCns.map((c) => c.credit_note_number || `#${c.id}`).join(', ');

  const invoiceNum = order.invoice_number || order.order_number || `#${order.id}`;
  const grandTotal = Number(order.grand_total ?? (Number(order.total_amount || 0) + Number(order.tax_amount || 0)));
  const formattedTotal = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(grandTotal);

  async function handleDelete() {
    setDeleting(true);
    setError(null);
    try {
      await api.deleteOrder(order.id);
      onDeleted?.(order);
    } catch (err) {
      setError(err.message || 'Failed to delete invoice');
      setDeleting(false);
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(3px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 10000,
        padding: '16px',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget && !deleting) onClose();
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '520px',
          background: 'var(--card-bg, #ffffff)',
          borderRadius: '12px',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1)',
          border: '1px solid var(--border, #e2e8f0)',
          overflow: 'hidden',
          animation: 'fadeIn 0.15s ease-out',
        }}
      >
        {/* MODAL HEADER */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--border, #e2e8f0)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: '#fff1f2',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '20px' }}>🗑️</span>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.1rem', color: '#9f1239', fontWeight: 700 }}>
                Delete Tax Invoice
              </h3>
              <div style={{ fontSize: '0.8rem', color: '#be123c' }}>
                Permanent invoice deletion
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={deleting}
            style={{
              background: 'transparent',
              border: 'none',
              fontSize: '18px',
              color: '#9f1239',
              cursor: 'pointer',
              padding: '4px 8px',
              borderRadius: '4px',
            }}
          >
            ✕
          </button>
        </div>

        {/* MODAL BODY */}
        <div style={{ padding: '20px' }}>
          {/* INVOICE DETAILS CARD */}
          <div
            style={{
              background: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: '8px',
              padding: '12px 16px',
              marginBottom: '16px',
              fontSize: '13px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <span style={{ color: '#64748b' }}>Invoice Number:</span>
              <strong style={{ color: '#0f172a' }}>{invoiceNum}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <span style={{ color: '#64748b' }}>Invoice Date:</span>
              <span style={{ color: '#0f172a' }}>{formatDate(order.invoice_date || order.created_at)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <span style={{ color: '#64748b' }}>Party / Customer:</span>
              <span style={{ color: '#0f172a', fontWeight: 600 }}>{order.name || order.phone}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <span style={{ color: '#64748b' }}>Grand Total:</span>
              <strong style={{ color: '#047857', fontSize: '14px' }}>{formattedTotal}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b' }}>Payment Status:</span>
              <span style={{ textTransform: 'capitalize', fontWeight: 600, color: order.payment_status === 'paid' ? '#15803d' : '#b45309' }}>
                {order.payment_status === 'partially_paid' ? 'Partially Paid' : order.payment_status}
              </span>
            </div>
          </div>

          {/* CREDIT NOTE SAFEGUARD (BLOCKS DELETION) */}
          {hasCreditNotes ? (
            <div
              style={{
                background: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: '8px',
                padding: '14px',
                color: '#991b1b',
                fontSize: '13px',
                lineHeight: '1.45',
              }}
            >
              <div style={{ fontWeight: 700, marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span>🛑</span> Cannot Delete Invoice (Active Returns Linked)
              </div>
              <p style={{ margin: '0 0 6px' }}>
                Credit Note(s) <strong>{cnListStr}</strong> are linked to this invoice.
              </p>
              <p style={{ margin: 0, fontSize: '12px', color: '#b91c1c' }}>
                For accounting and audit integrity, you must delete or void the associated Credit Note before deleting this invoice.
              </p>
            </div>
          ) : (
            <div
              style={{
                background: '#fff7ed',
                border: '1px solid #fed7aa',
                borderRadius: '8px',
                padding: '14px',
                color: '#9a3412',
                fontSize: '13px',
                lineHeight: '1.45',
              }}
            >
              <div style={{ fontWeight: 700, marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span>⚠️</span> Warning: This action completely deletes the invoice
              </div>
              <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '12.5px' }}>
                <li>The invoice record and all line items will be permanently erased.</li>
                <li>Any payments tied to this invoice will be deleted, reversing the customer balance.</li>
                {order.quotation_id && (
                  <li>Linked Quotation #{order.quotation_id} will be reverted back to <em>Accepted</em>.</li>
                )}
                <li>Cached PDF files from storage will be removed.</li>
              </ul>
            </div>
          )}

          {error && (
            <div
              style={{
                marginTop: '14px',
                padding: '10px 12px',
                background: '#fef2f2',
                border: '1px solid #fca5a5',
                borderRadius: '6px',
                color: '#b91c1c',
                fontSize: '13px',
              }}
            >
              ⚠️ {error}
            </div>
          )}
        </div>

        {/* MODAL FOOTER */}
        <div
          style={{
            padding: '14px 20px',
            borderTop: '1px solid var(--border, #e2e8f0)',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '10px',
            background: '#f8fafc',
          }}
        >
          <button
            type="button"
            onClick={onClose}
            disabled={deleting}
            style={{
              padding: '8px 16px',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              background: '#ffffff',
              color: '#475569',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Cancel
          </button>
          {!hasCreditNotes && (
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleting}
              style={{
                padding: '8px 18px',
                borderRadius: '6px',
                border: 'none',
                background: '#dc2626',
                color: '#ffffff',
                fontSize: '13px',
                fontWeight: 600,
                cursor: deleting ? 'not-allowed' : 'pointer',
                opacity: deleting ? 0.7 : 1,
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              {deleting ? 'Deleting…' : '🗑️ Delete Invoice Completely'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
