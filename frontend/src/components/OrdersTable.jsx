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

export default function OrdersTable({ orders, loading, onChanged, showCustomer = true }) {
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
    { key: 'order_number', label: 'Order #' },
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

  return <DataTable columns={columns} rows={orders} loading={loading} emptyText="No orders yet." />;
}
