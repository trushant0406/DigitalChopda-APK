import { useEffect, useState, useMemo } from 'react';
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
  const [search, setSearch] = useState('');
  const [paymentStatus, setPaymentStatus] = useState(initialFilter?.payment_status || '');
  const [fulfillmentStatus, setFulfillmentStatus] = useState(initialFilter?.fulfillment_status || '');
  const [orderStatus, setOrderStatus] = useState('');
  const [orders, setOrders] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  async function load(queryText = search) {
    setLoading(true);
    try {
      const params = {};
      if (paymentStatus) params.payment_status = paymentStatus;
      if (fulfillmentStatus) params.fulfillment_status = fulfillmentStatus;
      if (orderStatus) params.status = orderStatus;
      if (queryText && queryText.trim()) params.q = queryText.trim();
      const list = await api.listOrders(params);
      setOrders(list || []);
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

  // Debounced server query when typing search
  useEffect(() => {
    const timer = setTimeout(() => {
      load(search);
    }, 250);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  function handleChanged() {
    load();
    refreshSummary();
  }

  // Instant local filtering so typing feels 0ms instantaneous
  const filteredOrders = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return orders;
    return orders.filter((o) => {
      const num = (o.order_number || '').toLowerCase();
      const inv = (o.invoice_number || '').toLowerCase();
      const ref = (o.order_ref || '').toLowerCase();
      const name = (o.name || '').toLowerCase();
      const phone = (o.phone || '').toLowerCase();
      const city = (o.city || '').toLowerCase();
      const notes = (o.notes || '').toLowerCase();
      const itemsMatch = (o.items || []).some((item) =>
        (item.product_name || '').toLowerCase().includes(q)
      );
      return (
        num.includes(q) ||
        inv.includes(q) ||
        ref.includes(q) ||
        name.includes(q) ||
        phone.includes(q) ||
        city.includes(q) ||
        notes.includes(q) ||
        itemsMatch
      );
    });
  }, [orders, search]);

  return (
    <div className="page">
      {/* SUMMARY STATS */}
      <div className="stat-grid">
        <StatCard label="Payment pending" value={summary?.paymentPending} tone="danger" />
        <StatCard label="₹ pending" value={summary ? `₹${summary.pendingAmount.toFixed(0)}` : undefined} tone="danger" />
        <StatCard label="Ready to dispatch" value={summary?.readyToDispatch} tone="info" />
        <StatCard label="Not dispatched" value={summary?.notDispatched} tone="warn" />
        <StatCard label="Delivered" value={summary?.delivered} />
        <StatCard label="Total orders" value={summary?.totalOrders} />
      </div>

      {/* ORIGINAL ORDER CREATION FORM */}
      <OrderForm
        onCreated={() => {
          load();
          refreshSummary();
          onLogged?.();
        }}
      />

      {/* ORDERS DATA TABLE */}
      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '0.8rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <h2 style={{ margin: 0 }}>Orders</h2>
            <span className="badge info" style={{ fontSize: '0.75rem' }}>
              {filteredOrders.length}
            </span>
          </div>

          <div className="filters" style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
            <div style={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}>
              <input
                type="search"
                placeholder="Search order #, invoice #, customer, phone, item…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{
                  minWidth: '260px',
                  paddingRight: search ? '2rem' : '0.6rem',
                }}
              />
              {search && (
                <button
                  type="button"
                  onClick={() => setSearch('')}
                  style={{
                    position: 'absolute',
                    right: '6px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'var(--muted)',
                    cursor: 'pointer',
                    padding: '2px 4px',
                    fontSize: '0.85rem',
                  }}
                  title="Clear search"
                >
                  ✕
                </button>
              )}
            </div>

            <select
              value={paymentStatus}
              onChange={(e) => setPaymentStatus(e.target.value)}
              aria-label="Filter by payment status"
            >
              <option value="">All Payment</option>
              {PAYMENT_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>

            <select
              value={fulfillmentStatus}
              onChange={(e) => setFulfillmentStatus(e.target.value)}
              aria-label="Filter by fulfillment status"
            >
              <option value="">All Fulfillment</option>
              {FULFILLMENT_OPTIONS.map((s) => (
                <option key={s} value={s}>{FULFILLMENT_LABEL[s]}</option>
              ))}
            </select>

            <select
              value={orderStatus}
              onChange={(e) => setOrderStatus(e.target.value)}
              aria-label="Filter by active or cancelled status"
            >
              <option value="">Active + Cancelled</option>
              {STATUS_OPTIONS.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </div>
        </div>

        <OrdersTable
          orders={filteredOrders}
          loading={loading}
          onChanged={handleChanged}
          emptyText={search.trim() ? `No orders matching "${search.trim()}".` : 'No orders yet.'}
        />
      </div>
    </div>
  );
}
