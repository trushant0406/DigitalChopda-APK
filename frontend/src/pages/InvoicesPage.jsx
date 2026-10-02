import { useEffect, useState, useMemo } from 'react';
import { api } from '../api';
import InvoiceForm from '../components/InvoiceForm';
import OrdersTable from '../components/OrdersTable';
import RecordPaymentInModal from '../components/RecordPaymentInModal';
import CreateCreditNoteModal from '../components/CreateCreditNoteModal';
import EditInvoiceModal from '../components/EditInvoiceModal';
import DeleteInvoiceModal from '../components/DeleteInvoiceModal';
import StatCard from '../components/StatCard';
import { FULFILLMENT_LABEL } from '../orderStatus';

const PAYMENT_OPTIONS = [
  { value: 'pending', label: 'Unpaid / Pending' },
  { value: 'partially_paid', label: 'Partially Paid' },
  { value: 'paid', label: 'Paid' },
];
const FULFILLMENT_OPTIONS = ['not_ready', 'ready', 'dispatched', 'delivered'];

export default function InvoicesPage({ refreshKey }) {
  const [search, setSearch] = useState('');
  const [paymentStatus, setPaymentStatus] = useState('');
  const [fulfillmentStatus, setFulfillmentStatus] = useState('');
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [settlingOrder, setSettlingOrder] = useState(null);
  const [creditNoteOrder, setCreditNoteOrder] = useState(null);
  const [editingOrder, setEditingOrder] = useState(null);
  const [deletingOrder, setDeletingOrder] = useState(null);
  const [toast, setToast] = useState(null);

  async function load(queryText = search) {
    setLoading(true);
    try {
      const params = { is_invoice: 'true' };
      if (paymentStatus) params.payment_status = paymentStatus;
      if (fulfillmentStatus) params.fulfillment_status = fulfillmentStatus;
      if (queryText && queryText.trim()) params.q = queryText.trim();
      const list = await api.listOrders(params);
      setInvoices(list || []);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paymentStatus, fulfillmentStatus, refreshKey]);

  useEffect(() => {
    const timer = setTimeout(() => {
      load(search);
    }, 250);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  // Calculations for Invoices stats
  const stats = useMemo(() => {
    const totalCount = invoices.length;
    let pendingCount = 0;
    let pendingAmount = 0;
    let paidAmount = 0;
    let totalRevenue = 0;

    for (const inv of invoices) {
      const total = Number(inv.total_amount || 0) + Number(inv.tax_amount || 0);
      totalRevenue += total;
      const rec = inv.payment_status === 'paid' ? total : Number(inv.received_amount || 0);
      const due = inv.payment_status === 'paid' ? 0 : (inv.balance_due !== undefined ? Number(inv.balance_due) : Math.max(0, total - rec));
      paidAmount += rec;
      if (due > 0.01) {
        pendingCount++;
        pendingAmount += due;
      }
    }

    return { totalCount, pendingCount, pendingAmount, paidAmount, totalRevenue };
  }, [invoices]);

  // Instant local filtering so typing feels 0ms instantaneous
  const filteredInvoices = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return invoices;
    return invoices.filter((o) => {
      const inv = (o.invoice_number || '').toLowerCase();
      const ord = (o.order_number || '').toLowerCase();
      const ref = (o.order_ref || '').toLowerCase();
      const name = (o.name || '').toLowerCase();
      const phone = (o.phone || '').toLowerCase();
      const city = (o.city || '').toLowerCase();
      const notes = (o.notes || '').toLowerCase();
      const itemsMatch = (o.items || []).some((item) =>
        (item.product_name || '').toLowerCase().includes(q)
      );
      return (
        inv.includes(q) ||
        ord.includes(q) ||
        ref.includes(q) ||
        name.includes(q) ||
        phone.includes(q) ||
        city.includes(q) ||
        notes.includes(q) ||
        itemsMatch
      );
    });
  }, [invoices, search]);

  return (
    <div className="page">
      {/* HEADER BAR */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem', flexWrap: 'wrap', gap: '0.8rem' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.45rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span>🧾</span> Sales Invoices
          </h1>
          <p style={{ margin: '0.2rem 0 0 0', color: 'var(--muted)', fontSize: '0.86rem' }}>
            GST tax billing, sequential invoice numbers, PDF downloads, and WhatsApp delivery.
          </p>
        </div>
        <button
          type="button"
          className={showForm ? 'secondary-action' : 'primary-btn'}
          onClick={() => setShowForm((prev) => !prev)}
          style={{
            padding: '0.6rem 1.25rem',
            fontSize: '0.92rem',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.45rem',
            cursor: 'pointer',
          }}
        >
          {showForm ? '✕ Hide Form' : '➕ Create Sales Invoice'}
        </button>
      </div>

      {toast && (
        <div className="bulk-bar" style={{ background: '#e6f7ec', color: '#166534', border: '1px solid #bbf7d0', marginBottom: '1rem' }}>
          ✓ {toast}
        </div>
      )}

      {/* STATS */}
      <div className="stat-grid">
        <StatCard label="Total invoices" value={stats.totalCount} />
        <StatCard label="Total billed" value={`₹${stats.totalRevenue.toFixed(0)}`} tone="info" />
        <StatCard label="Payment pending" value={stats.pendingCount} tone="danger" />
        <StatCard label="₹ pending" value={`₹${stats.pendingAmount.toFixed(0)}`} tone="danger" />
        <StatCard label="₹ received" value={`₹${stats.paidAmount.toFixed(0)}`} tone="ok" />
      </div>

      {/* COLLAPSIBLE INVOICE CREATION FORM */}
      {showForm && (
        <div style={{ marginBottom: '1.5rem', animation: 'fadeIn 0.2s ease-out' }}>
          <InvoiceForm
            onCreated={(created) => {
              load();
              setShowForm(false);
              setToast(`Invoice ${created?.invoice_number || created?.order_number || ''} created successfully!`);
              setTimeout(() => setToast(null), 5000);
            }}
            onCancel={() => setShowForm(false)}
          />
        </div>
      )}

      {/* INVOICES DATA TABLE */}
      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '0.8rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <h2 style={{ margin: 0 }}>All Invoices</h2>
            <span className="badge info" style={{ fontSize: '0.75rem' }}>
              {filteredInvoices.length}
            </span>
          </div>

          <div className="filters" style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
            <div style={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}>
              <input
                type="search"
                placeholder="Search invoice #, customer, phone, item…"
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
                <option key={s.value} value={s.value}>{s.label}</option>
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
          </div>
        </div>

        <OrdersTable
          orders={filteredInvoices}
          loading={loading}
          onChanged={load}
          onSettle={(order) => setSettlingOrder(order)}
          onIssueCreditNote={(order) => setCreditNoteOrder(order)}
          onEditOrder={(order) => setEditingOrder(order)}
          onDeleteOrder={(order) => setDeletingOrder(order)}
          emptyText={search.trim() ? `No invoices matching "${search.trim()}".` : 'No invoices created yet.'}
        />
      </div>

      {/* QUICK SETTLE PAYMENT IN MODAL */}
      {settlingOrder && (
        <RecordPaymentInModal
          customer={{
            id: settlingOrder.contact_id,
            name: settlingOrder.name,
            phone: settlingOrder.phone,
            city: settlingOrder.city,
          }}
          currentBalance={Number(settlingOrder.balance_due || (Number(settlingOrder.total_amount || 0) + Number(settlingOrder.tax_amount || 0)))}
          initialOrderId={settlingOrder.id}
          onClose={() => setSettlingOrder(null)}
          onSuccess={() => {
            setSettlingOrder(null);
            setToast(`Payment recorded successfully against ${settlingOrder.invoice_number || settlingOrder.order_number}!`);
            load();
            setTimeout(() => setToast(null), 4000);
          }}
        />
      )}

      {/* ISSUE CREDIT NOTE (SALES RETURN) MODAL */}
      {creditNoteOrder && (
        <CreateCreditNoteModal
          customer={{
            id: creditNoteOrder.contact_id,
            name: creditNoteOrder.name,
            phone: creditNoteOrder.phone,
            city: creditNoteOrder.city,
            gstin: creditNoteOrder.gstin,
          }}
          initialOrder={creditNoteOrder}
          onClose={() => setCreditNoteOrder(null)}
          onSuccess={(cn) => {
            setCreditNoteOrder(null);
            setToast(`Credit Note ${cn.credit_note_number} issued successfully against ${creditNoteOrder.invoice_number || creditNoteOrder.order_number}!`);
            load();
            setTimeout(() => setToast(null), 4000);
          }}
        />
      )}

      {/* EDIT INVOICE MODAL */}
      {editingOrder && (
        <EditInvoiceModal
          order={editingOrder}
          onClose={() => setEditingOrder(null)}
          onSaved={(updated) => {
            setEditingOrder(null);
            setToast(`Invoice ${updated.invoice_number || updated.order_number} updated successfully!`);
            load();
            setTimeout(() => setToast(null), 4000);
          }}
        />
      )}

      {/* DELETE INVOICE MODAL */}
      {deletingOrder && (
        <DeleteInvoiceModal
          order={deletingOrder}
          onClose={() => setDeletingOrder(null)}
          onDeleted={(deleted) => {
            setDeletingOrder(null);
            setToast(`Invoice ${deleted.invoice_number || deleted.order_number} deleted completely.`);
            load();
            setTimeout(() => setToast(null), 4000);
          }}
        />
      )}
    </div>
  );
}
