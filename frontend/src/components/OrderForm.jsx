import { useState, useEffect } from 'react';
import { api } from '../api';
import OrderItemsEditor from './OrderItemsEditor';
import PaymentField from './PaymentField';
import BusinessDetailsFields from './BusinessDetailsFields';
import { emptyItem, itemsTotal, itemsTax, itemsToPayload } from '../orderItems';

export default function OrderForm({ onCreated }) {
  const [orderNumber, setOrderNumber] = useState('');
  const [invoiceDate, setInvoiceDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [phone, setPhone] = useState('');
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState([emptyItem()]);
  const [status, setStatus] = useState('pending');
  const [method, setMethod] = useState('cash');
  const [business, setBusiness] = useState({});
  const [businessOnFile, setBusinessOnFile] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  function loadNextNumber() {
    api.getNextInvoiceNumber()
      .then((res) => {
        if (res?.suggestedNumber) setOrderNumber(res.suggestedNumber);
      })
      .catch(() => {});
  }

  useEffect(() => {
    loadNextNumber();
  }, []);

  async function lookupByPhone() {
    if (!phone) return;
    const matches = await api.listContacts(phone).catch(() => []);
    const exact = matches.find((c) => c.phone === phone);
    if (exact) {
      if (!name) setName(exact.name || '');
      setBusiness({
        gstin: exact.gstin || '',
        pan: exact.pan || '',
        billing_address: exact.billing_address || '',
        shipping_address: exact.shipping_address || '',
        state: exact.state || '',
      });
      setBusinessOnFile(Boolean(exact.gstin || exact.pan || exact.billing_address));
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.createOrder({
        order_number: orderNumber.trim() || undefined,
        invoice_date: invoiceDate,
        phone,
        name: name || undefined,
        city: city || undefined,
        notes: notes || undefined,
        items: itemsToPayload(items),
        payment_status: status === 'paid' ? 'paid' : 'pending',
        payment_method: status === 'paid' ? method : status === 'credit' ? 'credit' : undefined,
        ...business,
      });
      setPhone('');
      setName('');
      setCity('');
      setNotes('');
      setItems([emptyItem()]);
      setStatus('pending');
      setMethod('cash');
      setBusiness({});
      setBusinessOnFile(false);
      loadNextNumber();
      onCreated();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card call-form" onSubmit={handleSubmit}>
      <h2>New sales invoice / order</h2>
      <div className="grid">
        <label>
          Invoice / Bill #
          <input
            value={orderNumber}
            onChange={(e) => setOrderNumber(e.target.value)}
            placeholder="Auto-generated (edit to override)"
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
      <div className="grid">
        <label>
          Phone *
          <input
            required
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            onBlur={lookupByPhone}
            placeholder="+91 98765 43210"
          />
        </label>
        <label>
          Name
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label>
          City
          <input value={city} onChange={(e) => setCity(e.target.value)} />
        </label>
      </div>

      <fieldset>
        <legend>Items</legend>
        <OrderItemsEditor items={items} onChange={setItems} />
        <p className="order-total">
          Total: ₹{itemsTotal(items).toFixed(2)}
          {itemsTax(items) > 0 && <span className="hint"> (incl. ₹{itemsTax(items).toFixed(2)} GST = ₹{(itemsTotal(items) + itemsTax(items)).toFixed(2)})</span>}
        </p>
      </fieldset>

      <PaymentField status={status} setStatus={setStatus} method={method} setMethod={setMethod} />

      <fieldset>
        <legend>GST / billing details (optional){businessOnFile && ' — on file, edit if needed'}</legend>
        <BusinessDetailsFields value={business} onChange={setBusiness} />
      </fieldset>

      <label>
        Notes
        <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} />
      </label>

      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={saving}>
        {saving ? 'Saving…' : 'Create order'}
      </button>
    </form>
  );
}
