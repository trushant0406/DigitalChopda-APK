import { useEffect, useState } from 'react';
import { api } from '../api';
import OrderForm from '../components/OrderForm';
import OrdersTable from '../components/OrdersTable';
import StatCard from '../components/StatCard';
import { FULFILLMENT_LABEL } from '../orderStatus';

const PAYMENT_OPTIONS = ['pending', 'paid'];
const FULFILLMENT_OPTIONS = ['not_ready', 'ready', 'dispatched', 'delivered'];
const STATUS_OPTIONS = [
  { value: 'active', label: 'Active' },
  { value: 'cancelled', label: 'Cancelled' },
];

export default function OrdersPage({ refreshKey, onLogged, initialFilter }) {
  const [paymentStatus, setPaymentStatus] = useState(initialFilter?.payment_status || '');
  const [fulfillmentStatus, setFulfillmentStatus] = useState(initialFilter?.fulfillment_status || '');
  const [orderStatus, setOrderStatus] = useState('');
  const [orders, setOrders] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    try {
      const params = {};
      if (paymentStatus) params.payment_status = paymentStatus;
      if (fulfillmentStatus) params.fulfillment_status = fulfillmentStatus;
      if (orderStatus) params.status = orderStatus;
      setOrders(await api.listOrders(params));
    } finally {
      setLoading(false);
    }
  }

  function refreshSummary() {
    api.ordersSummary().then(setSummary).catch(() => {});
  }

  useEffect(() => {
    refreshSummary();
  }, [refreshKey]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paymentStatus, fulfillmentStatus, orderStatus, refreshKey]);

  function handleChanged() {
    load();
    refreshSummary();
  }

  return (
    <div className="page">
      <div className="stat-grid">
        <StatCard label="Payment pending" value={summary?.paymentPending} tone="danger" />
        <StatCard label="₹ pending" value={summary ? `₹${summary.pendingAmount.toFixed(0)}` : undefined} tone="danger" />
        <StatCard label="Ready to dispatch" value={summary?.readyToDispatch} tone="info" />
        <StatCard label="Not dispatched" value={summary?.notDispatched} tone="warn" />
        <StatCard label="Delivered" value={summary?.delivered} />
        <StatCard label="Total orders" value={summary?.totalOrders} />
      </div>

      <OrderForm
        onCreated={() => {
          load();
          refreshSummary();
          onLogged?.();
        }}
      />

      <div className="card">
        <div className="card-header">
          <h2>Orders</h2>
          <div className="filters">
            <select value={paymentStatus} onChange={(e) => setPaymentStatus(e.target.value)}>
              <option value="">All payment</option>
              {PAYMENT_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            <select value={fulfillmentStatus} onChange={(e) => setFulfillmentStatus(e.target.value)}>
              <option value="">All fulfillment</option>
              {FULFILLMENT_OPTIONS.map((s) => (
                <option key={s} value={s}>{FULFILLMENT_LABEL[s]}</option>
              ))}
            </select>
            <select value={orderStatus} onChange={(e) => setOrderStatus(e.target.value)}>
              <option value="">Active + cancelled</option>
              {STATUS_OPTIONS.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </div>
        </div>
        <OrdersTable orders={orders} loading={loading} onChanged={handleChanged} />
      </div>
    </div>
  );
}
