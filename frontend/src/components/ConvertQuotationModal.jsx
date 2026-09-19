import { useEffect, useState } from 'react';
import { api } from '../api';
import PaymentField from './PaymentField';

const GST_RATES = [0, 5, 12, 18, 28];

export default function ConvertQuotationModal({ quotation, onClose, onConverted }) {
  const [orderNumber, setOrderNumber] = useState('');
  const [orderRef, setOrderRef] = useState(`Quote #${quotation.id}`);
  const [invoiceDate, setInvoiceDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [products, setProducts] = useState([]);
  const [items, setItems] = useState(() =>
    (quotation.items || []).map((i) => ({
      product_name: i.product_name || '',
      quantity: Number(i.quantity) || 1,
      unit_price: Number(i.rate ?? i.unit_price ?? 0),
      gst_rate: Number(i.gst_rate ?? 18),
      hsn_code: i.hsn_code || '',
      unit: i.unit || 'PCS',
      fromQuotation: true, // Existing item from quotation (description/unit read-only)
    }))
  );
  const [paidNow, setPaidNow] = useState(false);
  const [method, setMethod] = useState('cash');
  const [notes, setNotes] = useState(() => `From Quotation #${quotation.id}`);
  const [sendWhatsApp, setSendWhatsApp] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.getNextInvoiceNumber()
      .then((res) => {
        if (res?.suggestedNumber) setOrderNumber(res.suggestedNumber);
      })
      .catch(() => {});

    api.listProducts()
      .then((list) => setProducts(list || []))
      .catch(() => {});
  }, []);

  function updateItem(index, field, value) {
    setItems((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };
      return next;
    });
  }

  function handleSelectProduct(index, productName) {
    const prod = products.find((p) => p.name === productName);
    setItems((prev) => {
      const next = [...prev];
      next[index] = {
        ...next[index],
        product_name: productName,
        unit_price: prod ? Number(prod.default_price) : next[index].unit_price,
        gst_rate: prod ? Number(prod.gst_rate || 18) : next[index].gst_rate,
        unit: prod?.unit || next[index].unit || 'PCS',
        hsn_code: prod?.hsn_code || '',
      };
      return next;
    });
  }

  function removeItem(index) {
    setItems((prev) => (prev.length > 1 ? prev.filter((_, i) => i !== index) : prev));
  }

  function addItem() {
    const firstProd = products[0];
    setItems((prev) => [
      ...prev,
      {
        product_name: firstProd?.name || '',
        quantity: 1,
        unit_price: firstProd ? Number(firstProd.default_price) : 0,
        gst_rate: firstProd ? Number(firstProd.gst_rate || 18) : 18,
        unit: firstProd?.unit || 'PCS',
        hsn_code: firstProd?.hsn_code || '',
        fromQuotation: false, // New item added via product dropdown
      },
    ]);
  }

  // Live calculations
  const subtotal = items.reduce(
    (sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.unit_price) || 0),
    0
  );
  const taxAmount = items.reduce(
    (sum, i) =>
      sum + (Number(i.quantity) || 0) * (Number(i.unit_price) || 0) * ((Number(i.gst_rate) || 0) / 100),
    0
  );
  const totalAmount = subtotal + taxAmount;

  async function handleConvert(withWhatsApp = sendWhatsApp) {
    if (items.some((i) => !i.product_name || Number(i.quantity) <= 0 || Number(i.unit_price) < 0)) {
      setError('Please ensure all items have a valid product selected, quantity > 0, and non-negative price.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const res = await api.convertQuotationToInvoice(quotation.id, {
        invoice_number: orderNumber.trim() || undefined,
        order_number: orderNumber.trim() || undefined,
        order_ref: orderRef.trim() || undefined,
        invoice_date: invoiceDate,
        payment_status: paidNow ? 'paid' : 'pending',
        payment_method: paidNow ? method : undefined,
        notes: notes || undefined,
        items,
        send_whatsapp: withWhatsApp,
      });

      if (withWhatsApp && !res.whatsapp?.sentViaWeb && res.whatsapp?.whatsappUrl) {
        window.open(res.whatsapp.whatsappUrl, '_blank');
      }

      onConverted(res);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to convert quotation');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div
        className="drawer"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '780px', width: '92vw' }}
      >
        <div className="drawer-header">
          <div>
            <h2>Convert to Sales Invoice / Bill</h2>
            <p className="subtitle">
              From Quotation #{quotation.id} &middot; <strong>{quotation.bill_to_name}</strong> ({quotation.bill_to_mobile})
            </p>
          </div>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={(e) => { e.preventDefault(); handleConvert(sendWhatsApp); }}>
          {/* Bill Identifiers & Date */}
          <div className="grid" style={{ marginBottom: '1rem' }}>
            <label>
              Invoice / Bill #
              <input
                value={orderNumber}
                onChange={(e) => setOrderNumber(e.target.value)}
                placeholder="Auto-generated (edit to override)"
                required
              />
            </label>
            <label>
              Order / PO Reference #
              <input
                value={orderRef}
                onChange={(e) => setOrderRef(e.target.value)}
                placeholder="e.g. Quote #10 or PO-1234"
              />
            </label>
            <label>
              Invoice Date
              <input
                type="date"
                required
                value={invoiceDate}
                onChange={(e) => setInvoiceDate(e.target.value)}
              />
            </label>
          </div>

          {/* Editable Items Table */}
          <div className="card" style={{ padding: '1rem', marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <h3 style={{ margin: 0, fontSize: '0.95rem' }}>
                Invoice Line Items <span className="hint">(Edit prices or quantities as needed)</span>
              </h3>
              <button
                type="button"
                onClick={addItem}
                style={{ fontSize: '0.8rem', padding: '0.3rem 0.75rem' }}
              >
                + Add Item
              </button>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left', color: 'var(--muted)' }}>
                    <th style={{ padding: '6px' }}>Item Description</th>
                    <th style={{ padding: '6px', width: '70px', textAlign: 'center' }}>Unit</th>
                    <th style={{ padding: '6px', width: '80px' }}>Qty</th>
                    <th style={{ padding: '6px', width: '110px' }}>Price / Rate (₹)</th>
                    <th style={{ padding: '6px', width: '85px' }}>GST %</th>
                    <th style={{ padding: '6px', width: '95px', textAlign: 'right' }}>Total (₹)</th>
                    <th style={{ padding: '6px', width: '40px' }}></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, idx) => {
                    const lineSub = (Number(item.quantity) || 0) * (Number(item.unit_price) || 0);
                    const lineTax = lineSub * ((Number(item.gst_rate) || 0) / 100);
                    const lineTotal = lineSub + lineTax;
                    return (
                      <tr key={idx} style={{ borderBottom: '1px solid var(--border)' }}>
                        {/* ITEM DESCRIPTION: Non-editable label for quotation items, dropdown for added items */}
                        <td style={{ padding: '8px 6px', verticalAlign: 'middle' }}>
                          {item.fromQuotation ? (
                            <div>
                              <strong style={{ fontSize: '0.92rem', color: 'var(--text)' }}>
                                {item.product_name}
                              </strong>
                              {item.hsn_code && (
                                <div style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>
                                  HSN: {item.hsn_code}
                                </div>
                              )}
                            </div>
                          ) : (
                            <select
                              value={item.product_name}
                              onChange={(e) => handleSelectProduct(idx, e.target.value)}
                              style={{ width: '100%', padding: '0.4rem', borderRadius: '6px' }}
                              required
                            >
                              <option value="" disabled>-- Select Product --</option>
                              {products.map((p) => (
                                <option key={p.id} value={p.name}>
                                  {p.name} (₹{Number(p.default_price).toFixed(2)})
                                </option>
                              ))}
                            </select>
                          )}
                        </td>

                        {/* UNIT: Non-editable badge */}
                        <td style={{ padding: '8px 6px', textAlign: 'center', verticalAlign: 'middle' }}>
                          <span className="badge neutral" style={{ fontWeight: 600, fontSize: '0.78rem' }}>
                            {item.unit || 'PCS'}
                          </span>
                        </td>

                        {/* QUANTITY: Editable */}
                        <td style={{ padding: '6px', verticalAlign: 'middle' }}>
                          <input
                            type="number"
                            min="0.01"
                            step="any"
                            value={item.quantity}
                            onChange={(e) => updateItem(idx, 'quantity', e.target.value)}
                            required
                            style={{ width: '100%', textAlign: 'center' }}
                          />
                        </td>

                        {/* UNIT PRICE: Editable */}
                        <td style={{ padding: '6px', verticalAlign: 'middle' }}>
                          <input
                            type="number"
                            min="0"
                            step="any"
                            value={item.unit_price}
                            onChange={(e) => updateItem(idx, 'unit_price', e.target.value)}
                            required
                            style={{ width: '100%', textAlign: 'right', fontWeight: 600 }}
                          />
                        </td>

                        {/* GST RATE: Selectable */}
                        <td style={{ padding: '6px', verticalAlign: 'middle' }}>
                          <select
                            value={item.gst_rate}
                            onChange={(e) => updateItem(idx, 'gst_rate', e.target.value)}
                            style={{ width: '100%' }}
                          >
                            {GST_RATES.map((rate) => (
                              <option key={rate} value={rate}>{rate}%</option>
                            ))}
                          </select>
                        </td>

                        {/* LINE TOTAL */}
                        <td style={{ padding: '6px', textAlign: 'right', fontWeight: 700, verticalAlign: 'middle' }}>
                          ₹{lineTotal.toFixed(2)}
                        </td>

                        {/* REMOVE ACTION */}
                        <td style={{ padding: '6px', textAlign: 'center', verticalAlign: 'middle' }}>
                          {items.length > 1 && (
                            <button
                              type="button"
                              className="remove-item"
                              onClick={() => removeItem(idx)}
                              title="Remove item"
                              style={{ padding: '2px 6px', fontSize: '0.8rem' }}
                            >
                              ✕
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Totals Summary */}
            <div style={{ marginTop: '1rem', borderTop: '2px solid var(--border)', paddingTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.35rem', alignItems: 'flex-end', fontSize: '0.9rem' }}>
              <div>Taxable Amount: <strong>₹{subtotal.toFixed(2)}</strong></div>
              <div>GST Amount: <strong>₹{taxAmount.toFixed(2)}</strong></div>
              <div style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--accent)', marginTop: '0.25rem' }}>
                Invoice Total: ₹{totalAmount.toFixed(2)}
              </div>
            </div>
          </div>

          {/* Payment Details */}
          <div style={{ marginBottom: '1rem' }}>
            <PaymentField
              status={paidNow ? 'paid' : 'pending'}
              setStatus={(s) => setPaidNow(s === 'paid')}
              method={method}
              setMethod={setMethod}
            />
          </div>

          {/* WhatsApp Direct Notification Option */}
          <div style={{ background: 'var(--bg-secondary)', padding: '0.85rem 1rem', borderRadius: '8px', marginBottom: '1rem', border: '1px solid var(--border)' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', cursor: 'pointer', fontWeight: 600 }}>
              <input
                type="checkbox"
                checked={sendWhatsApp}
                onChange={(e) => setSendWhatsApp(e.target.checked)}
                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
              />
              <span>Send Tax Invoice via WhatsApp to {quotation.bill_to_mobile} 💬</span>
            </label>
            <div style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.25rem', marginLeft: '1.8rem' }}>
              Attaches the official Tax Invoice PDF directly to the customer's chat.
            </div>
          </div>

          {/* Notes */}
          <label style={{ display: 'block', marginBottom: '1.25rem' }}>
            Invoice Notes / Remarks
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              style={{ width: '100%', marginTop: '0.35rem' }}
            />
          </label>

          {error && <p className="error" style={{ marginBottom: '1rem' }}>{error}</p>}

          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
            <button type="button" className="secondary-action" onClick={onClose} disabled={saving} style={{ margin: 0 }}>
              Cancel
            </button>
            <button type="submit" disabled={saving}>
              {saving ? 'Converting…' : '✓ Confirm & Generate Invoice'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
