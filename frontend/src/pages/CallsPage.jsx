import { useEffect, useState } from 'react';
import { api } from '../api';
import CallForm from '../components/CallForm';
import DataTable from '../components/DataTable';
import PhoneLink from '../components/PhoneLink';
import CreateOrderModal from '../components/CreateOrderModal';

function columns(onCreateOrder) {
  return [
    { key: 'name', label: 'Name' },
    { key: 'phone', label: 'Phone', render: (r) => <PhoneLink phone={r.phone} /> },
    { key: 'direction', label: 'Direction' },
    { key: 'outcome', label: 'Outcome' },
    { key: 'duration_sec', label: 'Duration', render: (r) => (r.duration_sec ? `${r.duration_sec}s` : '—') },
    { key: 'started_at', label: 'When', render: (r) => new Date(r.started_at).toLocaleString() },
    { key: 'notes', label: 'Notes' },
    {
      key: 'action',
      label: '',
      render: (r) => (
        <button type="button" onClick={() => onCreateOrder(r)}>Create order</button>
      ),
    },
  ];
}

export default function CallsPage({ refreshKey, onLogged }) {
  const [calls, setCalls] = useState([]);
  const [loading, setLoading] = useState(true);
  const [orderingFor, setOrderingFor] = useState(null);

  async function load() {
    setLoading(true);
    try {
      setCalls(await api.listCalls());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [refreshKey]);

  return (
    <div className="page">
      <CallForm onLogged={onLogged} />

      <div className="card">
        <div className="card-header">
          <h2>All calls</h2>
          <a className="button-link" href={api.callsExportUrl()}>
            Export CSV
          </a>
        </div>
        <DataTable columns={columns(setOrderingFor)} rows={calls} loading={loading} />
      </div>

      {orderingFor && (
        <CreateOrderModal
          contact={{ phone: orderingFor.phone, name: orderingFor.name, city: orderingFor.city }}
          callId={orderingFor.id}
          onClose={() => setOrderingFor(null)}
          onCreated={() => {}}
        />
      )}
    </div>
  );
}
