import { useState, useEffect } from 'react';
import { api } from '../api';
import { formatDateTime } from '../dateFormat';

function getLocalDate() {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export default function RecordPaymentInModal({ customer, currentBalance, initialOrderId = null, onClose, onSuccess }) {
  const [amount, setAmount] = useState('');
  const [paymentMode, setPaymentMode] = useState('upi');
  const [referenceNo, setReferenceNo] = useState('');
  const [paymentDate, setPaymentDate] = useState(getLocalDate);
  const [receiptNumber, setReceiptNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [sendWhatsApp, setSendWhatsApp] = useState(Boolean(customer?.phone));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Settle against bill state (myBillBook style)
  const [unpaidInvoices, setUnpaidInvoices] = useState([]);
  const [selectedOrderId, setSelectedOrderId] = useState(() => (initialOrderId ? String(initialOrderId) : ''));
  const [loadingInvoices, setLoadingInvoices] = useState(true);

  const dueAmount = Math.max(0, Number(currentBalance || 0));

  // Load next sequential receipt number on mount
  useEffect(() => {
    api.getNextReceiptNumber()
      .then((res) => {
        if (res?.receiptNumber) setReceiptNumber(res.receiptNumber);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    let active = true;
    async function fetchUnpaid() {
      setLoadingInvoices(true);
      try {
        const list = await api.getUnpaidInvoices(customer.id);
        if (active) {
          setUnpaidInvoices(list || []);
          if (initialOrderId) {
            const found = (list || []).find((b) => String(b.id) === String(initialOrderId));
            if (found) {
              setSelectedOrderId(String(found.id));
              setAmount(String(Number(found.balance_due).toFixed(2)));
            }
          } else if (list && list.length === 1) {
            setSelectedOrderId(String(list[0].id));
            setAmount(String(list[0].balance_due.toFixed(2)));
          }
        }
      } catch (err) {
        console.error('Failed to load unpaid invoices:', err);
      } finally {
        if (active) setLoadingInvoices(false);
      }
    }
    fetchUnpaid();
    return () => { active = false; };
  }, [customer.id, initialOrderId]);

  function handleBillChange(orderId) {
    setSelectedOrderId(orderId);
    if (!orderId) {
      if (dueAmount > 0) setAmount(String(dueAmount.toFixed(2)));
      return;
    }
    const found = unpaidInvoices.find(b => String(b.id) === String(orderId));
    if (found) {
      setAmount(String(found.balance_due.toFixed(2)));
    }
  }

  function setFullDue() {
    if (selectedOrderId) {
      const found = unpaidInvoices.find(b => String(b.id) === String(selectedOrderId));
      if (found) setAmount(String(found.balance_due.toFixed(2)));
    } else if (dueAmount > 0) {
      setAmount(String(dueAmount.toFixed(2)));
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const numAmount = Number(amount);
    if (!numAmount || numAmount <= 0) {
      setError('Please enter a valid positive payment amount');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      let finalPaidAt = undefined;
      if (paymentDate) {
        const [year, month, day] = paymentDate.split('-').map(Number);
        const now = new Date();
        const d = new Date(year, month - 1, day, now.getHours(), now.getMinutes(), now.getSeconds(), now.getMilliseconds());
        finalPaidAt = d.toISOString();
      }

      const res = await api.recordPaymentIn({
        contact_id: customer.id,
        order_id: selectedOrderId ? Number(selectedOrderId) : undefined,
        amount: numAmount,
        payment_mode: paymentMode,
        reference_no: referenceNo.trim() || undefined,
        receipt_number: receiptNumber.trim() || undefined,
        paid_at: finalPaidAt,
        notes: notes.trim() || undefined,
        send_whatsapp: sendWhatsApp,
      });

      if (onSuccess) onSuccess(res);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to record payment');
    } finally {
      setSaving(false);
    }
  }

  const refLabel =
    paymentMode === 'upi'
      ? 'UPI Reference / UTR No.'
      : paymentMode === 'bank_transfer'
      ? 'Bank Transfer Ref / UTR'
      : paymentMode === 'cheque'
      ? 'Cheque Number'
      : 'Reference (optional)';

  return (
    <div
      className="modal-backdrop"
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        width: '100vw',
        height: '100vh',
        background: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(4px)',
        WebkitBackdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 99999,
        padding: '16px',
        overflowY: 'auto',
        boxSizing: 'border-box',
      }}
    >
      <div
        className="modal card"
        style={{
          maxWidth: '520px',
          width: '100%',
          maxHeight: '90vh',
          overflowY: 'auto',
          margin: 'auto',
          background: '#ffffff',
          borderRadius: '12px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="card-header" style={{ borderBottom: '1px solid var(--color-border)', paddingBottom: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: '18px' }}>📥 Record Payment In</h2>
            <div style={{ color: 'var(--color-muted)', fontSize: '13px', marginTop: '2px' }}>
              Party: <strong>{customer.name || 'Customer'}</strong> {customer.phone && `(${customer.phone})`}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            {receiptNumber && (
              <span style={{
                background: '#eff6ff',
                color: '#2563eb',
                border: '1px solid #bfdbfe',
                borderRadius: '6px',
                padding: '4px 8px',
                fontSize: '12px',
                fontWeight: 700
              }}>
                Receipt: {receiptNumber}
              </span>
            )}
            <button type="button" className="btn-secondary" onClick={onClose} style={{ padding: '4px 10px' }}>✕</button>
          </div>
        </div>

        {dueAmount > 0 && (
          <div style={{
            background: 'var(--color-surface-sunken)',
            padding: '10px 14px',
            borderRadius: '6px',
            margin: '14px 0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            border: '1px solid var(--color-border)'
          }}>
            <div>
              <span style={{ fontSize: '12px', color: 'var(--color-muted)' }}>Current Total Due: </span>
              <strong style={{ color: 'var(--color-danger, #ef4444)', fontSize: '15px' }}>₹{dueAmount.toFixed(2)}</strong>
            </div>
            <button
              type="button"
              className="btn-secondary"
              style={{ fontSize: '12px', padding: '4px 8px' }}
              onClick={setFullDue}
            >
              Pay Full Due
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginTop: '8px' }}>
          {/* Settle Against Bill Dropdown (myBillBook Parity) */}
          <div className="field">
            <label style={{ fontWeight: 600, fontSize: '13px', display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
              <span>Settle Against Bill (Optional)</span>
              {loadingInvoices && <span style={{ fontSize: '11px', color: '#64748b' }}>Loading bills…</span>}
              {!loadingInvoices && unpaidInvoices.length > 0 && (
                <span style={{ fontSize: '11px', color: '#2563eb', fontWeight: 600 }}>
                  {unpaidInvoices.length} unpaid bill{unpaidInvoices.length > 1 ? 's' : ''}
                </span>
              )}
            </label>
            <select
              value={selectedOrderId}
              onChange={(e) => handleBillChange(e.target.value)}
              style={{ padding: '8px 10px', fontSize: '13px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
            >
              <option value="">On-Account (Auto-settle oldest bills FIFO)</option>
              {unpaidInvoices.map((b) => (
                <option key={b.id} value={b.id}>
                  📄 {b.invoice_number || b.order_number} ({formatDateTime(b.created_at || b.invoice_date)}) · Due: ₹{Number(b.balance_due).toFixed(2)}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label style={{ fontWeight: 600, fontSize: '13px', display: 'block', marginBottom: '4px' }}>
              Payment Amount (₹) <span style={{ color: 'red' }}>*</span>
            </label>
            <input
              type="number"
              min="0.01"
              step="any"
              required
              autoFocus
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              style={{ fontSize: '16px', fontWeight: 'bold', padding: '8px 12px' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="field">
              <label style={{ fontWeight: 600, fontSize: '13px', display: 'block', marginBottom: '4px' }}>
                Payment Date
              </label>
              <input
                type="date"
                required
                value={paymentDate}
                onChange={(e) => setPaymentDate(e.target.value)}
              />
            </div>

            <div className="field">
              <label style={{ fontWeight: 600, fontSize: '13px', display: 'block', marginBottom: '4px' }}>
                Receipt #
              </label>
              <input
                type="text"
                placeholder="e.g. REC-0038"
                value={receiptNumber}
                onChange={(e) => setReceiptNumber(e.target.value)}
                style={{ fontWeight: 600, background: '#f8fafc' }}
              />
            </div>
          </div>

          <div className="field">
            <label style={{ fontWeight: 600, fontSize: '13px', display: 'block', marginBottom: '4px' }}>
              Payment Mode
            </label>
            <select value={paymentMode} onChange={(e) => setPaymentMode(e.target.value)}>
              <option value="upi">UPI (GPay / PhonePe / Paytm)</option>
              <option value="cash">Cash</option>
              <option value="bank_transfer">Bank Transfer (IMPS/NEFT)</option>
              <option value="cheque">Cheque</option>
              <option value="other">Other</option>
            </select>
          </div>

          <div className="field">
            <label style={{ fontWeight: 600, fontSize: '13px', display: 'block', marginBottom: '4px' }}>
              {refLabel}
            </label>
            <input
              type="text"
              placeholder={paymentMode === 'upi' ? 'e.g. 629103829102' : 'Optional reference number'}
              value={referenceNo}
              onChange={(e) => setReferenceNo(e.target.value)}
            />
          </div>

          <div className="field">
            <label style={{ fontWeight: 600, fontSize: '13px', display: 'block', marginBottom: '4px' }}>
              Notes / Remarks (optional)
            </label>
            <input
              type="text"
              placeholder="e.g. Partial advance payment received"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          {customer.phone && (
            <label style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 12px',
              background: '#f0fdf4',
              border: '1px solid #bbf7d0',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '13px',
              color: '#166534',
              fontWeight: 500,
            }}>
              <input
                type="checkbox"
                checked={sendWhatsApp}
                onChange={(e) => setSendWhatsApp(e.target.checked)}
                style={{ width: '16px', height: '16px', cursor: 'pointer' }}
              />
              <span>Send Payment Receipt via WhatsApp to <strong>{customer.phone}</strong> 💬</span>
            </label>
          )}

          {error && (
            <div style={{ color: '#dc2626', background: '#fee2e2', padding: '8px 12px', borderRadius: '4px', fontSize: '13px' }}>
              {error}
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '6px' }}>
            <button type="button" className="btn-secondary" onClick={onClose} disabled={saving}>
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving || !amount || Number(amount) <= 0}
              style={{ background: '#2563eb', color: '#fff', border: 'none', padding: '8px 18px', borderRadius: '6px', fontWeight: 600 }}
            >
              {saving ? 'Recording…' : '✓ Save Payment In'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
