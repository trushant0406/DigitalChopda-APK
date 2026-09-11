import { useEffect, useState } from 'react';
import { api } from '../api';
import DataTable from '../components/DataTable';
import ContactDetail from '../components/ContactDetail';
import PhoneLink from '../components/PhoneLink';
import { leadStage, LEAD_STAGE_LABEL, LEAD_STAGE_TONE } from '../leadStage';

export default function ContactsPage({ refreshKey, onChanged, onCreateQuotation }) {
  const [query, setQuery] = useState('');
  const [contacts, setContacts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);

  async function load() {
    setLoading(true);
    try {
      setContacts(await api.listContacts(query));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey]);

  const columns = [
    { key: 'name', label: 'Name', render: (r) => r.name || '—' },
    { key: 'phone', label: 'Phone', render: (r) => <PhoneLink phone={r.phone} /> },
    { key: 'city', label: 'City', render: (r) => r.city || '—' },
    { key: 'call_count', label: 'Calls' },
    {
      key: 'last_call_at',
      label: 'Last call',
      render: (r) => (r.last_call_at ? new Date(r.last_call_at).toLocaleString() : '—'),
    },
    { key: 'order_count', label: 'Orders' },
    { key: 'total_spent', label: 'Order value', render: (r) => `₹${Number(r.total_spent).toFixed(0)}` },
    {
      key: 'stage',
      label: 'Stage',
      render: (r) => {
        const stage = leadStage({
          callCount: r.call_count,
          orderCount: r.order_count,
          hasPaidOrder: r.has_paid_order,
          hasDispatchedOrder: r.has_dispatched_order,
          hasDeliveredOrder: r.has_delivered_order,
        });
        return <span className={`badge ${LEAD_STAGE_TONE[stage]}`}>{LEAD_STAGE_LABEL[stage]}</span>;
      },
    },
    {
      key: 'view',
      label: '',
      render: (r) => (
        <div className="row-actions">
          <button type="button" onClick={() => setSelectedId(r.id)}>View</button>
          <button type="button" className="secondary-action" onClick={() => onCreateQuotation?.(r)}>
            Create quotation
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      <div className="card">
        <div className="card-header">
          <h2>Contacts</h2>
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
        </div>
        <DataTable columns={columns} rows={contacts} loading={loading} />
      </div>

      {selectedId && (
        <ContactDetail
          contactId={selectedId}
          onClose={() => setSelectedId(null)}
          onChanged={() => {
            load();
            onChanged?.();
          }}
        />
      )}
    </div>
  );
}
