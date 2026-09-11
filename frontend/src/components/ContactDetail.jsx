import { useEffect, useState } from 'react';
import { api } from '../api';
import PhoneLink from './PhoneLink';
import OrdersTable from './OrdersTable';
import BusinessDetailsFields from './BusinessDetailsFields';
import CreateQuotationModal from './CreateQuotationModal';
import { leadStage, LEAD_STAGE_LABEL, LEAD_STAGE_TONE } from '../leadStage';

export default function ContactDetail({ contactId, onClose, onChanged }) {
  const [data, setData] = useState(null);
  const [dueAt, setDueAt] = useState('');
  const [reason, setReason] = useState('');
  const [channel, setChannel] = useState('push');
  const [saving, setSaving] = useState(false);

  const [editingBusiness, setEditingBusiness] = useState(false);
  const [businessDraft, setBusinessDraft] = useState({});
  const [businessSaving, setBusinessSaving] = useState(false);
  const [showQuotationModal, setShowQuotationModal] = useState(false);

  async function load() {
    setData(await api.getContact(contactId));
  }

  function startEditBusiness(contact) {
    setBusinessDraft({
      gstin: contact.gstin || '',
      pan: contact.pan || '',
      billing_address: contact.billing_address || '',
      shipping_address: contact.shipping_address || '',
      state: contact.state || '',
    });
    setEditingBusiness(true);
  }

  async function saveBusinessDetails() {
    setBusinessSaving(true);
    try {
      await api.updateContact(contactId, businessDraft);
      setEditingBusiness(false);
      await load();
      onChanged?.();
    } finally {
      setBusinessSaving(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contactId]);

  async function addFollowup(e) {
    e.preventDefault();
    if (!dueAt) return;
    setSaving(true);
    try {
      await api.createFollowup({ contact_id: contactId, due_at: dueAt, reason, channel });
      setDueAt('');
      setReason('');
      await load();
      onChanged?.();
    } finally {
      setSaving(false);
    }
  }

  function handleOrdersChanged() {
    load();
    onChanged?.();
  }

  if (!data) return null;
  const { contact, calls, followups, orders, topProducts, outstandingAmount } = data;
  const activeOrders = orders.filter((o) => o.status !== 'cancelled');
  const totalSpent = activeOrders.reduce((sum, o) => sum + Number(o.total_amount), 0);
  const stage = leadStage({
    callCount: calls.length,
    orderCount: activeOrders.length,
    hasPaidOrder: activeOrders.some((o) => o.payment_status === 'paid'),
    hasDispatchedOrder: activeOrders.some((o) => o.fulfillment_status === 'dispatched'),
    hasDeliveredOrder: activeOrders.some((o) => o.fulfillment_status === 'delivered'),
  });

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h2>{contact.name || 'Unnamed contact'}</h2>
            <p className="subtitle">
              <PhoneLink phone={contact.phone} /> {contact.city ? `· ${contact.city}` : ''}
            </p>
          </div>
          <button type="button" onClick={onClose}>Close</button>
        </div>

        <div className="customer-summary">
          <span className={`badge ${LEAD_STAGE_TONE[stage]}`}>{LEAD_STAGE_LABEL[stage]}</span>
          <span>{activeOrders.length} order{activeOrders.length === 1 ? '' : 's'}</span>
          <span>₹{totalSpent.toFixed(0)} total</span>
          {outstandingAmount > 0 && <span className="badge warn">₹{outstandingAmount.toFixed(0)} outstanding</span>}
        </div>

        <div className="top-products">
          <div className="card-header">
            <h3>Business details</h3>
            {!editingBusiness && (
              <button type="button" onClick={() => startEditBusiness(contact)}>
                {contact.gstin || contact.pan || contact.billing_address ? 'Edit' : '+ Add GST / address'}
              </button>
            )}
          </div>
          {editingBusiness ? (
            <>
              <BusinessDetailsFields value={businessDraft} onChange={setBusinessDraft} />
              <div className="row-actions">
                <button type="button" onClick={saveBusinessDetails} disabled={businessSaving}>
                  {businessSaving ? 'Saving…' : 'Save'}
                </button>
                <button type="button" className="remove-item" onClick={() => setEditingBusiness(false)}>Cancel</button>
              </div>
            </>
          ) : (
            <ul>
              {contact.gstin && <li>GSTIN: {contact.gstin}</li>}
              {contact.pan && <li>PAN: {contact.pan}</li>}
              {contact.state && <li>State: {contact.state}</li>}
              {contact.billing_address && <li>Billing: {contact.billing_address}</li>}
              {contact.shipping_address && <li>Shipping: {contact.shipping_address}</li>}
              {!contact.gstin && !contact.pan && !contact.billing_address && <li className="cell-muted">Nothing on file yet.</li>}
            </ul>
          )}
        </div>

        {topProducts.length > 0 && (
          <div className="top-products">
            <h3>Buys most</h3>
            <ul>
              {topProducts.map((p) => (
                <li key={p.product_name}>
                  {p.product_name} — ×{p.total_quantity} (₹{Number(p.total_amount).toFixed(0)})
                </li>
              ))}
            </ul>
          </div>
        )}

        <form className="inline-form" onSubmit={addFollowup}>
          <input
            type="datetime-local"
            value={dueAt}
            onChange={(e) => setDueAt(e.target.value)}
            required
          />
          <input
            placeholder="Reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          <select value={channel} onChange={(e) => setChannel(e.target.value)}>
            <option value="push">Push (me)</option>
            <option value="whatsapp">WhatsApp (customer)</option>
          </select>
          <button type="submit" disabled={saving}>Add follow-up</button>
        </form>

        <div className="card-header">
          <h3>Orders</h3>
          <button type="button" onClick={() => setShowQuotationModal(true)}>Create quotation</button>
        </div>
        <OrdersTable orders={orders} loading={false} onChanged={handleOrdersChanged} showCustomer={false} />

        <h3>Timeline</h3>
        <ul className="timeline">
          {[...calls.map((c) => ({ ...c, kind: 'call', at: c.started_at })),
            ...followups.map((f) => ({ ...f, kind: 'followup', at: f.due_at }))]
            .sort((a, b) => new Date(b.at) - new Date(a.at))
            .map((item) => (
              <li key={`${item.kind}-${item.id}`} className={`timeline-item ${item.kind}`}>
                {item.kind === 'call' ? (
                  <>
                    <strong>{item.direction} · {item.outcome}</strong>
                    <span>{new Date(item.started_at).toLocaleString()}</span>
                    {item.notes && <p>{item.notes}</p>}
                  </>
                ) : (
                  <>
                    <strong>Follow-up ({item.channel}) — {item.status}</strong>
                    <span>{new Date(item.due_at).toLocaleString()}</span>
                    {item.reason && <p>{item.reason}</p>}
                  </>
                )}
              </li>
            ))}
        </ul>
      </div>

      {showQuotationModal && (
        <CreateQuotationModal
          contact={contact}
          onClose={() => setShowQuotationModal(false)}
          onCreated={() => {}}
        />
      )}
    </div>
  );
}
