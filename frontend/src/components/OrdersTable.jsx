import { useState } from 'react';
import { api } from '../api';
import DataTable from './DataTable';
import PhoneLink from './PhoneLink';
import OrderRowActions from './OrderRowActions';
import { PAYMENT_METHOD_LABEL } from '../paymentMethods';
import { FULFILLMENT_LABEL, NEXT_FULFILLMENT, NEXT_FULFILLMENT_LABEL } from '../orderStatus';
import { formatDate, formatTime } from '../dateFormat';

function PaymentCell({ order }) {
  if (order.status === 'cancelled') {
    return <span className="cell-muted">{order.payment_status}</span>;
  }

  const rawCns = Array.isArray(order.credit_notes_list)
    ? order.credit_notes_list
    : (typeof order.credit_notes_list === 'string'
        ? JSON.parse(order.credit_notes_list || '[]')
        : []);
  const creditedAmt = Number(order.credited_amount || rawCns.reduce((sum, c) => sum + Number(c.total_amount || 0), 0) || 0);
  const hasReturns = creditedAmt > 0.01;
  const receivedAmt = Number(order.received_amount || 0);
  const balDue = Number(order.balance_due ?? 0);
  const is100PercentReturned = hasReturns && balDue <= 0.01 && receivedAmt <= 0.01;
  const cnTooltip = rawCns.map((c) => `${c.credit_note_number || 'CN'}: ₹${Number(c.total_amount || 0).toFixed(2)} (${c.reason || 'Return'})`).join('\n');

  if (is100PercentReturned) {
    return (
      <span
        className="badge"
        style={{ background: '#f3e8ff', color: '#7e22ce', border: '1px solid #d8b4fe', fontWeight: 600 }}
        title={cnTooltip || `Credited ₹${creditedAmt.toFixed(2)} via Sales Return`}
      >
        ↩ Fully Returned
      </span>
    );
  }

  const isPaid = order.payment_status === 'paid';
  const isPartial = order.payment_status === 'partially_paid';
  const badgeClass = isPaid ? 'badge ok' : (isPartial ? 'badge info' : 'badge warn');
  const label = isPaid ? 'Paid' : (isPartial ? 'Partially Paid' : 'Pending');

  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', flexWrap: 'wrap' }}>
      <span className={badgeClass}>
        {label}
        {order.payment_method ? ` · ${PAYMENT_METHOD_LABEL[order.payment_method] || order.payment_method}` : ''}
      </span>
      {hasReturns && (
        <span
          style={{
            fontSize: '0.72rem',
            padding: '1px 5px',
            borderRadius: '4px',
            background: '#f5f3ff',
            color: '#6d28d9',
            border: '1px solid #ddd6fe',
            fontWeight: 600,
            whiteSpace: 'nowrap',
          }}
          title={cnTooltip || `Credited ₹${creditedAmt.toFixed(2)} via Sales Return`}
        >
          ↩ Ret. ₹{Math.round(creditedAmt)}
        </span>
      )}
    </div>
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

export default function OrdersTable({
  orders,
  loading,
  onChanged,
  onSettle,
  onIssueCreditNote,
  onEditOrder,
  onDeleteOrder,
  showCustomer = true,
  emptyText = 'No orders yet.',
}) {
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
      render: (r) => {
        const primaryDoc = r.invoice_number || r.order_number;
        const secondaryDoc = r.invoice_number ? r.order_number : null;
        const quoteRef = r.quotation_id ? `Q#${r.quotation_id}` : null;
        const customRef = r.order_ref ? `Ref: ${r.order_ref}` : null;
        const timeStr = formatTime(r.created_at || r.invoice_date);

        return (
          <div style={{ lineHeight: 1.35 }}>
            {/* Primary doc # with compact secondary tags */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '5px', flexWrap: 'nowrap' }}>
              <strong style={{ fontSize: '0.9rem', color: r.invoice_number ? 'var(--color-primary, #1d4ed8)' : 'inherit', whiteSpace: 'nowrap' }}>
                {primaryDoc}
              </strong>
              {secondaryDoc && (
                <span
                  style={{
                    fontSize: '0.72rem',
                    color: 'var(--muted)',
                    background: 'var(--hover-bg, #f1f5f9)',
                    border: '1px solid var(--border, #e2e8f0)',
                    padding: '0 4px',
                    borderRadius: '4px',
                    whiteSpace: 'nowrap',
                  }}
                  title={`Internal Order: ${secondaryDoc}`}
                >
                  {secondaryDoc}
                </span>
              )}
              {quoteRef && (
                <span
                  style={{
                    fontSize: '0.7rem',
                    color: '#4f46e5',
                    background: '#eef2ff',
                    border: '1px solid #e0e7ff',
                    padding: '0 4px',
                    borderRadius: '4px',
                    fontWeight: 600,
                    whiteSpace: 'nowrap',
                  }}
                  title={`Converted from Quotation #${r.quotation_id}`}
                >
                  {quoteRef}
                </span>
              )}
              {customRef && (
                <span
                  style={{
                    fontSize: '0.7rem',
                    color: 'var(--muted)',
                    background: 'var(--hover-bg, #f1f5f9)',
                    padding: '0 4px',
                    borderRadius: '4px',
                    whiteSpace: 'nowrap',
                  }}
                  title={`Order Reference / Buyer PO: ${r.order_ref}`}
                >
                  {customRef}
                </span>
              )}
            </div>

            {/* Single-line compact Date & Time */}
            <div style={{ fontSize: '0.75rem', color: 'var(--muted)', marginTop: '2px', whiteSpace: 'nowrap' }}>
              {formatDate(r.invoice_date || r.created_at)}
              {timeStr && <span> · {timeStr}</span>}
            </div>
          </div>
        );
      },
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
      label: 'Actions',
      align: 'right',
      sticky: 'right',
      render: (r) => (
        <OrderRowActions
          order={r}
          onSettle={onSettle}
          onChanged={onChanged}
          onIssueCreditNote={onIssueCreditNote}
          onEditOrder={onEditOrder}
          onDeleteOrder={onDeleteOrder}
          handleSendWhatsApp={handleSendWhatsApp}
          sendingWhatsappId={sendingWhatsappId}
          advanceFulfillment={advanceFulfillment}
          cancelOrder={cancelOrder}
        />
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
