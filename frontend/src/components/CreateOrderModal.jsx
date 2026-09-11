import { useEffect, useState } from 'react';
import { api } from '../api';
import OrderItemsEditor from './OrderItemsEditor';
import PaymentField from './PaymentField';
import BusinessDetailsFields from './BusinessDetailsFields';
import { emptyItem, itemsTotal, itemsTax, itemsToPayload } from '../orderItems';

export default function CreateOrderModal({ contact, callId, onClose, onCreated }) {
  const [items, setItems] = useState([emptyItem()]);
  const [notes, setNotes] = useState('');
  const [paidNow, setPaidNow] = useState(false);
  const [method, setMethod] = useState('cash');
  const [business, setBusiness] = useState({});
  const [businessOnFile, setBusinessOnFile] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.listContacts(contact.phone).then((matches) => {
      const exact = matches.find((c) => c.phone === contact.phone);
      if (exact) {
        setBusiness({
          gstin: exact.gstin || '',
          pan: exact.pan || '',
          billing_address: exact.billing_address || '',
          shipping_address: exact.shipping_address || '',
          state: exact.state || '',
        });
        setBusinessOnFile(Boolean(exact.gstin || exact.pan || exact.billing_address));
      }
    }).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contact.phone]);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.createOrder({
        phone: contact.phone,
        name: contact.name || undefined,
        city: contact.city || undefined,
        call_id: callId || undefined,
        notes: notes || undefined,
        items: itemsToPayload(items),
        payment_status: paidNow ? 'paid' : 'pending',
        payment_method: paidNow ? method : undefined,
        ...business,
      });
      onCreated();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h2>Create order</h2>
            <p className="subtitle">
              {contact.name || contact.phone} {contact.city ? `· ${contact.city}` : ''}
            </p>
          </div>
          <button type="button" onClick={onClose}>Close</button>
        </div>

        <form onSubmit={handleSubmit}>
          <fieldset>
            <legend>Items</legend>
            <OrderItemsEditor items={items} onChange={setItems} />
            <p className="order-total">
              Total: ₹{itemsTotal(items).toFixed(2)}
              {itemsTax(items) > 0 && <span className="hint"> (incl. ₹{itemsTax(items).toFixed(2)} GST = ₹{(itemsTotal(items) + itemsTax(items)).toFixed(2)})</span>}
            </p>
          </fieldset>

          <PaymentField paidNow={paidNow} setPaidNow={setPaidNow} method={method} setMethod={setMethod} />

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
      </div>
    </div>
  );
}
