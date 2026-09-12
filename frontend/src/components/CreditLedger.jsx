import { useEffect, useState } from 'react';
import { api } from '../api';
import PhoneLink from './PhoneLink';
import { PAYMENT_METHODS } from '../paymentMethods';

function CollectForm({ order, onCollected }) {
  const [amount, setAmount] = useState(String(Number(order.outstanding).toFixed(2)));
  const [method, setMethod] = useState('cash');
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!amount || Number(amount) <= 0) return;
    setSaving(true);
    setError(null);
    try {
      await api.collectCreditPayment({ order_id: order.id, amount: Number(amount), method, notes: notes || undefined });
      onCollected();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <input
        type="number" min="0" step="any" placeholder="Amount"
        value={amount} onChange={(e) => setAmount(e.target.value)}
      />
      <select value={method} onChange={(e) => setMethod(e.target.value)}>
        {PAYMENT_METHODS.map((m) => (
          <option key={m.value} value={m.value}>{m.label}</option>
        ))}
      </select>
      <input placeholder="Notes (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} />
      <button type="submit" disabled={saving}>{saving ? 'Collecting…' : 'Collect'}</button>
      {error && <p className="error">{error}</p>}
    </form>
  );
}

export default function CreditLedger({ customer, onClose, onChanged }) {
  const [orders, setOrders] = useState(null);
  const [limitDraft, setLimitDraft] = useState(String(customer.credit_limit));
  const [savingLimit, setSavingLimit] = useState(false);

  async function load() {
    setOrders(await api.listCreditOrders(customer.id));
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [customer.id]);

  async function saveLimit() {
    setSavingLimit(true);
    try {
      await api.updateContact(customer.id, { credit_limit: limitDraft === '' ? 0 : Number(limitDraft) });
      onChanged();
    } finally {
      setSavingLimit(false);
    }
  }

  function handleCollected() {
    load();
    onChanged();
  }

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h2>{customer.name || 'Unnamed contact'}</h2>
            <p className="subtitle"><PhoneLink phone={customer.phone} /></p>
          </div>
          <button type="button" onClick={onClose}>Close</button>
        </div>

        <div className="inline-form">
          <label>
            Credit limit
            <input
              type="number" min="0" step="any"
              value={limitDraft} onChange={(e) => setLimitDraft(e.target.value)}
            />
          </label>
          <button type="button" onClick={saveLimit} disabled={savingLimit}>
            {savingLimit ? 'Saving…' : 'Save limit'}
          </button>
        </div>

        <h3>Credit orders</h3>
        {orders === null && <p>Loading…</p>}
        {orders?.length === 0 && <p className="empty">No credit orders yet.</p>}
        {orders?.map((order) => (
          <div key={order.id} className="card credit-order-card">
            <div className="card-header">
              <strong>{order.order_number}</strong>
              <span className={Number(order.outstanding) > 0 ? 'badge warn' : 'badge ok'}>
                {Number(order.outstanding) > 0 ? `₹${Number(order.outstanding).toFixed(0)} due` : 'Settled'}
              </span>
            </div>
            <p className="cell-muted">
              {order.items.map((i) => `${i.product_name} ×${i.quantity}`).join(', ')}
            </p>
            <p className="cell-muted">
              Total ₹{Number(order.total_amount).toFixed(2)} · Collected ₹{Number(order.collected).toFixed(2)}
            </p>
            {Number(order.outstanding) > 0 && <CollectForm order={order} onCollected={handleCollected} />}
          </div>
        ))}
      </div>
    </div>
  );
}
