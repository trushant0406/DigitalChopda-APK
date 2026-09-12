import { useEffect, useState } from 'react';
import { api } from '../api';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import PhoneLink from '../components/PhoneLink';
import CreditLedger from '../components/CreditLedger';

const STATUS_OPTIONS = [
  { value: '', label: 'All balances' },
  { value: 'outstanding', label: 'Outstanding' },
  { value: 'over_limit', label: 'Over limit' },
  { value: 'settled', label: 'Settled' },
];

export default function CreditPage({ refreshKey }) {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [customers, setCustomers] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);

  async function load() {
    setLoading(true);
    try {
      const params = {};
      if (query) params.q = query;
      if (status) params.status = status;
      setCustomers(await api.listCreditCustomers(params));
    } finally {
      setLoading(false);
    }
  }

  function refreshSummary() {
    api.creditSummary().then(setSummary).catch(() => {});
  }

  useEffect(() => {
    refreshSummary();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, refreshKey]);

  function handleChanged() {
    load();
    refreshSummary();
  }

  const columns = [
    {
      key: 'name',
      label: 'Customer',
      render: (r) => (
        <>
          {r.name || '—'}
          <span className="cell-secondary"><PhoneLink phone={r.phone} /></span>
        </>
      ),
    },
    { key: 'credit_limit', label: 'Credit limit', align: 'right', render: (r) => `₹${Number(r.credit_limit).toFixed(0)}` },
    { key: 'total_credit_amount', label: 'Total on credit', align: 'right', render: (r) => `₹${Number(r.total_credit_amount).toFixed(0)}` },
    { key: 'collected', label: 'Collected', align: 'right', render: (r) => `₹${Number(r.collected).toFixed(0)}` },
    {
      key: 'outstanding',
      label: 'Outstanding',
      align: 'right',
      render: (r) => {
        const outstanding = Number(r.outstanding);
        const overLimit = Number(r.credit_limit) > 0 && outstanding > Number(r.credit_limit);
        if (outstanding <= 0) return <span className="badge ok">Settled</span>;
        return <span className={`badge ${overLimit ? 'warn' : 'info'}`}>₹{outstanding.toFixed(0)}{overLimit ? ' · over limit' : ''}</span>;
      },
    },
    {
      key: 'action',
      label: '',
      render: (r) => (
        <button type="button" onClick={() => setSelected(r)}>View ledger</button>
      ),
    },
  ];

  return (
    <div className="page">
      <div className="stat-grid">
        <StatCard label="Total given on credit" value={summary ? `₹${summary.totalCreditAmount.toFixed(0)}` : undefined} />
        <StatCard label="Collected" value={summary ? `₹${summary.totalCollected.toFixed(0)}` : undefined} tone="info" />
        <StatCard label="Outstanding" value={summary ? `₹${summary.totalOutstanding.toFixed(0)}` : undefined} tone="danger" />
        <StatCard label="Khata customers" value={customers.length} />
      </div>

      <div className="card">
        <div className="card-header">
          <h2>Credit (Khata) customers</h2>
          <div className="filters">
            <form
              className="search-form"
              onSubmit={(e) => {
                e.preventDefault();
                load();
              }}
            >
              <input
                placeholder="Search name or phone…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <button type="submit">Search</button>
            </form>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              {STATUS_OPTIONS.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </div>
        </div>
        <DataTable
          columns={columns}
          rows={customers}
          loading={loading}
          emptyText="No credit orders or credit limits set yet. Create an order with payment status 'On credit (Khata)' to get started."
        />
      </div>

      {selected && (
        <CreditLedger
          customer={selected}
          onClose={() => setSelected(null)}
          onChanged={handleChanged}
        />
      )}
    </div>
  );
}
