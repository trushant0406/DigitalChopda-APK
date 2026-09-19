import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import DataTable from '../components/DataTable';
import QuotationForm from '../components/QuotationForm';
import ConvertQuotationModal from '../components/ConvertQuotationModal';

export default function QuotationsPage({ presetContact }) {
  const navigate = useNavigate();
  const [quotations, setQuotations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sendingId, setSendingId] = useState(null);
  const [sendError, setSendError] = useState(null);
  const [convertingQuotation, setConvertingQuotation] = useState(null);
  const [toast, setToast] = useState(null);
  const [showForm, setShowForm] = useState(Boolean(presetContact));
  const [search, setSearch] = useState('');

  async function load(queryText = search) {
    setLoading(true);
    try {
      const params = {};
      if (queryText && queryText.trim()) params.q = queryText.trim();
      const list = await api.listQuotations(params);
      setQuotations(list || []);
      if (!list || list.length === 0) {
        setShowForm(true);
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (presetContact) {
      setShowForm(true);
    }
  }, [presetContact]);

  useEffect(() => {
    const timer = setTimeout(() => {
      load(search);
    }, 250);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

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

  // Instant local filtering so typing feels 0ms instantaneous
  const filteredQuotations = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return quotations;
    return quotations.filter((item) => {
      const idStr = String(item.id).toLowerCase();
      const name = (item.bill_to_name || '').toLowerCase();
      const phone = (item.bill_to_mobile || '').toLowerCase();
      const address = (item.bill_to_address || '').toLowerCase();
      const gstin = (item.bill_to_gstin || '').toLowerCase();
      const invNum = (item.converted_order_invoice_number || item.converted_order_number || '').toLowerCase();
      const itemsMatch = (item.items || []).some((prod) =>
        (prod.product_name || '').toLowerCase().includes(q)
      );
      return (
        idStr.includes(q) ||
        name.includes(q) ||
        phone.includes(q) ||
        address.includes(q) ||
        gstin.includes(q) ||
        invNum.includes(q) ||
        itemsMatch
      );
    });
  }, [quotations, search]);

  const columns = [
    { key: 'id', label: 'Quotation #' },
    { key: 'bill_to_name', label: 'Customer' },
    { key: 'quotation_date', label: 'Date', render: (q) => new Date(q.quotation_date).toLocaleDateString('en-IN') },
    { key: 'total_amount', label: 'Total', align: 'right', render: (q) => `₹${Number(q.total_amount).toFixed(2)}` },
    { key: 'tax_mode', label: 'Tax', render: (q) => (q.tax_mode === 'split' ? 'CGST+SGST' : 'IGST') },
    {
      key: 'status',
      label: 'Status',
      render: (q) => {
        if (q.status === 'converted') {
          return (
            <span className="badge ok" title={`Converted to Invoice ${q.converted_order_invoice_number || q.converted_order_number || ('#' + q.converted_order_id)}`}>
              Converted ✓
            </span>
          );
        }
        return <span className={q.status === 'sent' ? 'badge ok' : 'badge neutral'}>{q.status}</span>;
      },
    },
    {
      key: 'action',
      label: '',
      render: (q) => (
        <div className="row-actions">
          {q.status === 'converted' ? (
            <a
              className="button-link"
              href={q.converted_order_id ? api.orderPdfUrl(q.converted_order_id) : '#'}
              target="_blank"
              rel="noreferrer"
              title="Open Tax Invoice PDF directly in a new tab"
              style={{ fontWeight: 600, color: 'var(--accent)' }}
            >
              📄 Invoice: {q.converted_order_invoice_number || q.converted_order_number || `#${q.converted_order_id}`} ↗
            </a>
          ) : (
            <button
              type="button"
              className="advance-action"
              style={{ fontWeight: 600 }}
              onClick={() => setConvertingQuotation(q)}
            >
              ⚡ Convert to Invoice
            </button>
          )}
          <a className="button-link" href={api.quotationPdfUrl(q.id)} target="_blank" rel="noreferrer" title="Download Quotation PDF">
            Quote PDF
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
      {/* HEADER BAR */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem', flexWrap: 'wrap', gap: '0.8rem' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '1.45rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span>▦</span> Quotations &amp; Estimates
          </h1>
          <p style={{ margin: '0.2rem 0 0 0', color: 'var(--muted)', fontSize: '0.86rem' }}>
            Create GST quotations, send estimates on WhatsApp, and convert them directly to official tax invoices.
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
          {showForm ? '✕ Hide Form' : '➕ Create Quotation'}
        </button>
      </div>

      {presetContact && (
        <div className="bulk-bar" style={{ marginBottom: '1rem' }}>
          Creating a quotation for <strong>{presetContact.name || presetContact.phone}</strong> — details pre-filled below.
        </div>
      )}

      {toast && (
        <div className="bulk-bar" style={{ background: '#e6f7ec', color: '#166534', border: '1px solid #bbf7d0', marginBottom: '1rem' }}>
          ✓ {toast}
        </div>
      )}

      {/* COLLAPSIBLE QUOTATION FORM */}
      {showForm && (
        <div style={{ marginBottom: '1.5rem', animation: 'fadeIn 0.2s ease-out' }}>
          <QuotationForm
            onCreated={() => {
              load();
              setShowForm(false);
              setToast('Quotation created successfully!');
              setTimeout(() => setToast(null), 5000);
            }}
            presetContact={presetContact}
          />
        </div>
      )}

      {/* QUOTATIONS DATA TABLE */}
      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '0.8rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <h2 style={{ margin: 0 }}>Quotations</h2>
            <span className="badge info" style={{ fontSize: '0.75rem' }}>
              {filteredQuotations.length}
            </span>
          </div>

          <div className="filters">
            <div style={{ position: 'relative', display: 'inline-flex', alignItems: 'center' }}>
              <input
                type="search"
                placeholder="Search quote #, customer, phone, item…"
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
          </div>
        </div>

        {sendError && <p className="error">{sendError}</p>}
        <DataTable
          columns={columns}
          rows={filteredQuotations}
          loading={loading}
          emptyText={search.trim() ? `No quotations matching "${search.trim()}".` : 'No quotations yet.'}
        />
      </div>

      {convertingQuotation && (
        <ConvertQuotationModal
          quotation={convertingQuotation}
          onClose={() => setConvertingQuotation(null)}
          onConverted={(res) => {
            load();
            setToast(res.message);
            setTimeout(() => setToast(null), 6000);
          }}
        />
      )}
    </div>
  );
}
