import { useEffect, useState } from 'react';
import { api } from '../api';
import StatCard from '../components/StatCard';
import DataTable from '../components/DataTable';
import LogOutcomeModal from '../components/LogOutcomeModal';
import CreateOrderModal from '../components/CreateOrderModal';
import PhoneLink from '../components/PhoneLink';
// import SnoozeSelect from '../components/SnoozeSelect'; // snooze disabled for now
import { DUE_RANGES, dueRangeParams, dueTone } from '../dueRanges';
import { useSelection } from '../useSelection';

const TABS = [
  { key: 'followups', label: 'Follow-ups', kind: 'followup' },
  { key: 'neverFollowedUp', label: 'Never followed up', kind: 'contact', fetcher: api.neverFollowedUp },
  { key: 'repeatNoConnect', label: 'Repeat callers, never connected', kind: 'contact', fetcher: api.repeatNoConnect },
];

const contactColumns = [
  { key: 'name', label: 'Name' },
  { key: 'phone', label: 'Phone', render: (r) => <PhoneLink phone={r.phone} /> },
  { key: 'city', label: 'City' },
  { key: 'call_count', label: 'Calls' },
  { key: 'last_call_at', label: 'Last call', render: (r) => new Date(r.last_call_at).toLocaleString() },
];

function followupColumns(onMarkDone, onLogOutcome, onSnoozed, onCreateOrder) {
  return [
    { key: 'name', label: 'Name' },
    { key: 'phone', label: 'Phone', render: (r) => <PhoneLink phone={r.phone} /> },
    { key: 'city', label: 'City' },
    { key: 'due_at', label: 'Due', render: (r) => new Date(r.due_at).toLocaleString() },
    { key: 'reason', label: 'Reason' },
    {
      key: 'action',
      label: '',
      render: (row) => (
        <div className="row-actions">
          <button type="button" onClick={() => onCreateOrder(row)}>Create order</button>
          <button type="button" onClick={() => onLogOutcome(row)}>Log outcome</button>
          {/* <SnoozeSelect followupId={row.id} onDone={onSnoozed} /> snooze disabled for now */}
          <button type="button" onClick={() => onMarkDone(row.id)}>Mark done</button>
        </div>
      ),
    },
  ];
}

export default function DashboardPage({ refreshKey, onViewOrders }) {
  const [activeTab, setActiveTab] = useState(TABS[0].key);
  const [dueRange, setDueRange] = useState('today');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState(null);
  const [ordersSummary, setOrdersSummary] = useState(null);
  const [loggingFollowup, setLoggingFollowup] = useState(null);
  const [orderingFor, setOrderingFor] = useState(null);
  const selection = useSelection();

  const tab = TABS.find((t) => t.key === activeTab);

  async function load() {
    setLoading(true);
    try {
      const data = tab.kind === 'followup'
        ? await api.listFollowups({ status: 'pending', ...dueRangeParams(dueRange) })
        : await tab.fetcher();
      setRows(data);
      selection.clear();
    } finally {
      setLoading(false);
    }
  }

  function refreshSummary() {
    api.summary().then(setSummary).catch(() => {});
    api.ordersSummary().then(setOrdersSummary).catch(() => {});
  }

  useEffect(() => {
    refreshSummary();
  }, [refreshKey]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, dueRange, refreshKey]);

  async function markDone(id) {
    await api.updateFollowup(id, { status: 'done' });
    load();
    refreshSummary();
  }

  async function bulkMarkDone() {
    await Promise.all([...selection.selectedIds].map((id) => api.updateFollowup(id, { status: 'done' })));
    load();
    refreshSummary();
  }

  return (
    <div className="page">
      <div className="stat-grid">
        <StatCard label="Due today" value={summary?.dueToday} tone="info" />
        <StatCard label="Overdue" value={summary?.overdue} tone="danger" />
        <StatCard label="Never followed up" value={summary?.neverFollowedUp} tone="warn" />
        <StatCard label="Repeat, never connected" value={summary?.repeatNoConnect} tone="warn" />
        <StatCard label="Calls today" value={summary?.callsToday} />
        <StatCard label="Total contacts" value={summary?.totalContacts} />
      </div>

      <p className="section-label">Orders</p>
      <div className="stat-grid">
        <StatCard
          label="Payment pending"
          value={ordersSummary?.paymentPending}
          tone="danger"
          onClick={() => onViewOrders?.({ payment_status: 'pending' })}
        />
        <StatCard
          label="₹ pending"
          value={ordersSummary ? `₹${ordersSummary.pendingAmount.toFixed(0)}` : undefined}
          tone="danger"
          onClick={() => onViewOrders?.({ payment_status: 'pending' })}
        />
        <StatCard
          label="Ready to dispatch"
          value={ordersSummary?.readyToDispatch}
          tone="info"
          onClick={() => onViewOrders?.({ fulfillment_status: 'ready' })}
        />
        <StatCard
          label="Sent to customer"
          value={ordersSummary?.dispatched}
          tone="info"
          onClick={() => onViewOrders?.({ fulfillment_status: 'dispatched' })}
        />
        <StatCard
          label="Delivered"
          value={ordersSummary?.delivered}
          onClick={() => onViewOrders?.({ fulfillment_status: 'delivered' })}
        />
        <StatCard
          label="Total orders"
          value={ordersSummary?.totalOrders}
          onClick={() => onViewOrders?.({})}
        />
      </div>

      <div className="card">
        <div className="card-header">
          <div className="tabs">
            {TABS.map((t) => (
              <button
                key={t.key}
                type="button"
                className={t.key === activeTab ? 'tab active' : 'tab'}
                onClick={() => setActiveTab(t.key)}
              >
                {t.label}
              </button>
            ))}
          </div>

          {tab.kind === 'followup' && (
            <select className="filter-select" value={dueRange} onChange={(e) => setDueRange(e.target.value)}>
              {DUE_RANGES.map((r) => (
                <option key={r.key} value={r.key}>{r.label}</option>
              ))}
            </select>
          )}
        </div>

        {tab.kind === 'followup' && selection.selectedIds.size > 0 && (
          <div className="bulk-bar">
            <span>{selection.selectedIds.size} selected</span>
            <button type="button" onClick={bulkMarkDone}>Mark done</button>
          </div>
        )}

        <DataTable
          loading={loading}
          rows={rows}
          columns={
            tab.kind === 'followup'
              ? followupColumns(markDone, setLoggingFollowup, load, setOrderingFor)
              : contactColumns
          }
          rowClassName={tab.kind === 'followup' ? (r) => `row-${dueTone(r.due_at) || 'none'}` : undefined}
          selection={tab.kind === 'followup' ? selection : undefined}
        />
      </div>

      {loggingFollowup && (
        <LogOutcomeModal
          followup={loggingFollowup}
          onClose={() => setLoggingFollowup(null)}
          onDone={() => {
            load();
            refreshSummary();
          }}
        />
      )}

      {orderingFor && (
        <CreateOrderModal
          contact={{ phone: orderingFor.phone, name: orderingFor.name, city: orderingFor.city }}
          onClose={() => setOrderingFor(null)}
          onCreated={() => {}}
        />
      )}
    </div>
  );
}
