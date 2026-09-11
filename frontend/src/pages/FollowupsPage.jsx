import { useEffect, useState } from 'react';
import { api } from '../api';
import DataTable from '../components/DataTable';
import LogOutcomeModal from '../components/LogOutcomeModal';
import CreateOrderModal from '../components/CreateOrderModal';
import PhoneLink from '../components/PhoneLink';
// import SnoozeSelect from '../components/SnoozeSelect'; // snooze disabled for now
import { DUE_RANGES, dueRangeParams, dueTone } from '../dueRanges';
import { useSelection } from '../useSelection';

const STATUS_OPTIONS = ['pending', 'done', 'cancelled'];
const CHANNEL_OPTIONS = ['push', 'whatsapp'];

export default function FollowupsPage({ refreshKey }) {
  const [status, setStatus] = useState('pending');
  const [channel, setChannel] = useState('');
  const [dueRange, setDueRange] = useState('all');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loggingFollowup, setLoggingFollowup] = useState(null);
  const [orderingFor, setOrderingFor] = useState(null);
  const selection = useSelection();

  async function load() {
    setLoading(true);
    try {
      const params = { ...dueRangeParams(dueRange) };
      if (status) params.status = status;
      if (channel) params.channel = channel;
      setRows(await api.listFollowups(params));
      selection.clear();
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, channel, dueRange, refreshKey]);

  async function updateStatus(id, newStatus) {
    await api.updateFollowup(id, { status: newStatus });
    load();
  }

  async function bulkMarkDone() {
    await Promise.all([...selection.selectedIds].map((id) => api.updateFollowup(id, { status: 'done' })));
    load();
  }

  const columns = [
    { key: 'name', label: 'Name', render: (r) => r.name || '—' },
    { key: 'phone', label: 'Phone', render: (r) => <PhoneLink phone={r.phone} /> },
    { key: 'due_at', label: 'Due', render: (r) => new Date(r.due_at).toLocaleString() },
    { key: 'reason', label: 'Reason', render: (r) => r.reason || '—' },
    { key: 'channel', label: 'Channel' },
    {
      key: 'reminded',
      label: 'Reminded',
      render: (r) => (r.sent_at ? new Date(r.sent_at).toLocaleString() : '—'),
    },
    { key: 'status', label: 'Status' },
    {
      key: 'action',
      label: '',
      render: (r) => (
        <div className="row-actions">
          <button type="button" onClick={() => setOrderingFor(r)}>Create order</button>
          {r.status === 'pending' && (
            <>
              <button type="button" onClick={() => setLoggingFollowup(r)}>Log outcome</button>
              {/* <SnoozeSelect followupId={r.id} onDone={load} /> snooze disabled for now */}
              <button type="button" onClick={() => updateStatus(r.id, 'done')}>Done</button>
              <button type="button" onClick={() => updateStatus(r.id, 'cancelled')}>Cancel</button>
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      <div className="card">
        <div className="card-header">
          <h2>Follow-ups</h2>
          <div className="filters">
            <select value={dueRange} onChange={(e) => setDueRange(e.target.value)}>
              {DUE_RANGES.map((r) => (
                <option key={r.key} value={r.key}>{r.label}</option>
              ))}
            </select>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">All statuses</option>
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            <select value={channel} onChange={(e) => setChannel(e.target.value)}>
              <option value="">All channels</option>
              {CHANNEL_OPTIONS.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>

        {selection.selectedIds.size > 0 && (
          <div className="bulk-bar">
            <span>{selection.selectedIds.size} selected</span>
            <button type="button" onClick={bulkMarkDone}>Mark done</button>
          </div>
        )}

        <DataTable
          columns={columns}
          rows={rows}
          loading={loading}
          rowClassName={(r) => (r.status === 'pending' ? `row-${dueTone(r.due_at) || 'none'}` : undefined)}
          selection={selection}
        />
      </div>

      {loggingFollowup && (
        <LogOutcomeModal
          followup={loggingFollowup}
          onClose={() => setLoggingFollowup(null)}
          onDone={load}
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
