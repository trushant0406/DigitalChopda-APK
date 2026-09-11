import { useEffect, useState } from 'react';
import { api } from '../api';
import DataTable from '../components/DataTable';
import QuotationForm from '../components/QuotationForm';

export default function QuotationsPage({ presetContact }) {
  const [quotations, setQuotations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sendingId, setSendingId] = useState(null);
  const [sendError, setSendError] = useState(null);

  async function load() {
    setLoading(true);
    try {
      setQuotations(await api.listQuotations());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function sendWhatsApp(id) {
    setSendingId(id);
    setSendError(null);
    try {
      const res = await api.sendQuotationWhatsApp(id);
      if (!res?.sentViaWeb && res?.whatsappUrl) {
        window.open(res.whatsappUrl, '_blank');
      }
      await load();
    } catch (err) {
      setSendError(err.message);
    } finally {
      setSendingId(null);
    }
  }

  const columns = [
    { key: 'id', label: 'Quotation #' },
    { key: 'bill_to_name', label: 'Customer' },
    { key: 'quotation_date', label: 'Date', render: (q) => new Date(q.quotation_date).toLocaleDateString() },
    { key: 'total_amount', label: 'Total', align: 'right', render: (q) => `₹${Number(q.total_amount).toFixed(2)}` },
    { key: 'tax_mode', label: 'Tax', render: (q) => (q.tax_mode === 'split' ? 'CGST+SGST' : 'IGST') },
    {
      key: 'status',
      label: 'Status',
      render: (q) => <span className={q.status === 'sent' ? 'badge ok' : 'badge neutral'}>{q.status}</span>,
    },
    {
      key: 'action',
      label: '',
      render: (q) => (
        <div className="row-actions">
          <a className="button-link" href={api.quotationPdfUrl(q.id)} target="_blank" rel="noreferrer">
            Download PDF
          </a>
          <button type="button" onClick={() => sendWhatsApp(q.id)} disabled={sendingId === q.id}>
            {sendingId === q.id ? 'Sending…' : 'Send via WhatsApp'}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      {presetContact && (
        <div className="bulk-bar">
          Creating a quotation for <strong>{presetContact.name || presetContact.phone}</strong> — details pre-filled below.
        </div>
      )}
      <QuotationForm onCreated={load} presetContact={presetContact} />

      <div className="card">
        <div className="card-header">
          <h2>Quotations</h2>
        </div>
        {sendError && <p className="error">{sendError}</p>}
        <DataTable columns={columns} rows={quotations} loading={loading} emptyText="No quotations yet." />
      </div>
    </div>
  );
}
