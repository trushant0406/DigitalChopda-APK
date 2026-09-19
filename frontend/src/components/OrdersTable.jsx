import { useState } from 'react';
import { api } from '../api';
import DataTable from './DataTable';
import PhoneLink from './PhoneLink';
import PaymentActionMenu from './PaymentActionMenu';
import { PAYMENT_METHOD_LABEL } from '../paymentMethods';
import { FULFILLMENT_LABEL, NEXT_FULFILLMENT, NEXT_FULFILLMENT_LABEL } from '../orderStatus';

function PaymentCell({ order }) {
  if (order.status === 'cancelled') {
    return <span className="cell-muted">{order.payment_status}</span>;
  }
  return (
    <span className={order.payment_status === 'paid' ? 'badge ok' : 'badge warn'}>
      {order.payment_status}
      {order.payment_method ? ` · ${PAYMENT_METHOD_LABEL[order.payment_method]}` : ''}
    </span>
  );
}

function FulfillmentCell({ order }) {
  if (order.status === 'cancelled') {
    return <span className="cell-muted">{FULFILLMENT_LABEL[order.fulfillment_status]}</span>;
  }
  if (order.fulfillment_status === 'not_ready') {
    return <span className="cell-muted">Not started</span>;
  }
  const tone = order.fulfillment_status === 'delivered' ? 'ok' : 'info';
  return <span className={`badge ${tone}`}>{FULFILLMENT_LABEL[order.fulfillment_status]}</span>;
}

export default function OrdersTable({ orders, loading, onChanged, showCustomer = true, emptyText = 'No orders yet.' }) {
  const [sendingWhatsappId, setSendingWhatsappId] = useState(null);
  const [whatsappToast, setWhatsappToast] = useState(null);

  async function handleSendWhatsApp(order) {
    setSendingWhatsappId(order.id);
    setWhatsappToast(null);
    try {
      const res = await api.sendOrderWhatsApp(order.id);
      if (!res?.sentViaWeb && res?.whatsappUrl) {
        window.open(res.whatsappUrl, '_blank');
      }
      setWhatsappToast(`Invoice ${order.order_number} sent to ${order.phone}!`);
      setTimeout(() => setWhatsappToast(null), 4000);
    } catch (err) {
      alert('Failed to send invoice via WhatsApp: ' + err.message);
    } finally {
      setSendingWhatsappId(null);
    }
  }

  async function advanceFulfillment(order) {
    const next = NEXT_FULFILLMENT[order.fulfillment_status];
    if (!next) return;
    await api.updateOrder(order.id, { fulfillment_status: next });
    onChanged();
  }

  async function cancelOrder(order) {
    if (!window.confirm(`Cancel order ${order.order_number}? This can't be undone.`)) return;
    await api.updateOrder(order.id, { status: 'cancelled' });
    onChanged();
  }

  const columns = [
    {
      key: 'order_number',
      label: 'Invoice / Order #',
      render: (r) => (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
            <strong>{r.order_number}</strong>
            {r.invoice_number && (
              <span
                className="badge ok"
                style={{ fontSize: '0.72rem', padding: '1px 6px', fontWeight: 600 }}
                title={`Tax Invoice: ${r.invoice_number}`}
              >
                🧾 {r.invoice_number}
              </span>
            )}
            {r.order_ref && (
              <span
                className="badge neutral"
                style={{ fontSize: '0.72rem', padding: '1px 5px', fontWeight: 500 }}
                title={`Order Reference: ${r.order_ref}`}
              >
                {r.order_ref}
              </span>
            )}
            {!r.order_ref && r.quotation_id && (
              <span
                className="badge neutral"
                style={{ fontSize: '0.72rem', padding: '1px 5px', fontWeight: 500 }}
                title={`Converted from Quotation #${r.quotation_id}`}
              >
                Quote #{r.quotation_id}
              </span>
            )}
          </div>
          <div className="cell-secondary">
            {r.invoice_date ? new Date(r.invoice_date).toLocaleDateString('en-IN') : new Date(r.created_at).toLocaleDateString('en-IN')}
          </div>
        </>
      ),
    },
    ...(showCustomer
      ? [
          {
            key: 'customer',
            label: 'Customer',
            render: (r) => (
              <>
                {r.name || '—'}
                <span className="cell-secondary"><PhoneLink phone={r.phone} /></span>
              </>
            ),
          },
        ]
      : []),
    {
      key: 'items',
      label: 'Items',
      className: 'truncate',
      render: (r) => {
        const summary = r.items.map((i) => `${i.product_name} ×${i.quantity}`).join(', ');
        return <span title={summary}>{summary}</span>;
      },
    },
    {
      key: 'total_amount',
      label: 'Total',
      align: 'right',
      render: (r) => (
        <>
          ₹{Number(r.total_amount).toFixed(2)}
          {Number(r.tax_amount) > 0 && (
            <div className="cell-secondary">incl. GST ₹{(Number(r.total_amount) + Number(r.tax_amount)).toFixed(2)}</div>
          )}
        </>
      ),
    },
    { key: 'payment_status', label: 'Payment', render: (r) => <PaymentCell order={r} /> },
    { key: 'fulfillment_status', label: 'Fulfillment', render: (r) => <FulfillmentCell order={r} /> },
    {
      key: 'action',
      label: '',
      render: (r) =>
        r.status === 'cancelled' ? (
          <span className="badge neutral">Cancelled</span>
        ) : (
          <div className="row-actions">
            <a
              className="button-link"
              href={api.orderPdfUrl(r.id)}
              target="_blank"
              rel="noreferrer"
              title="Download Tax Invoice PDF"
            >
              PDF
            </a>
            <button
              type="button"
              className="secondary-action"
              onClick={() => handleSendWhatsApp(r)}
              disabled={sendingWhatsappId === r.id}
              title="Send Tax Invoice PDF via WhatsApp"
            >
              {sendingWhatsappId === r.id ? '…' : 'WhatsApp 💬'}
            </button>
            {r.payment_status === 'pending' && <PaymentActionMenu order={r} onDone={onChanged} />}
            {NEXT_FULFILLMENT[r.fulfillment_status] && (
              <button type="button" className="advance-action" onClick={() => advanceFulfillment(r)}>
                {NEXT_FULFILLMENT_LABEL[r.fulfillment_status]}
              </button>
            )}
            <button type="button" className="danger-action" onClick={() => cancelOrder(r)}>
              Cancel
            </button>
          </div>
        ),
    },
  ];

  return (
    <>
      {whatsappToast && (
        <div className="bulk-bar" style={{ background: '#e6f7ec', color: '#166534', border: '1px solid #bbf7d0', marginBottom: '0.75rem' }}>
          ✓ {whatsappToast}
        </div>
      )}
      <DataTable columns={columns} rows={orders} loading={loading} emptyText={emptyText} />
    </>
  );
}
