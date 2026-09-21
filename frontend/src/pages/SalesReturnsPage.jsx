import { useEffect, useState, useMemo } from 'react';
import { api } from '../api';
import CreateCreditNoteModal from '../components/CreateCreditNoteModal';
import StatCard from '../components/StatCard';
import { formatDate } from '../dateFormat';
import { CreditNoteIcon, WhatsAppIcon, PrinterIcon, SearchIcon, TrashIcon } from '../components/Icons';

function formatINR(amount) {
  return new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
    Number(amount || 0)
  );
}

const REASON_OPTIONS = [
  { value: '', label: 'All Return Reasons' },
  { value: 'Sales Return', label: 'Sales Return' },
  { value: 'Damaged / Defective Goods', label: 'Damaged / Defective Goods' },
  { value: 'Incorrect Item Delivered', label: 'Incorrect Item Delivered' },
  { value: 'Post-Sale Discount / Rate Difference', label: 'Rate Difference / Discount' },
  { value: 'Order Cancellation / Partial Refund', label: 'Order Cancellation' },
  { value: 'Quality Not Satisfactory', label: 'Quality Not Satisfactory' },
  { value: 'Other', label: 'Other' },
];

export default function SalesReturnsPage({ refreshKey }) {
  const [creditNotes, setCreditNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [reasonFilter, setReasonFilter] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [sendingId, setSendingId] = useState(null);
  const [toast, setToast] = useState(null);
  const [voidingCn, setVoidingCn] = useState(null);
  const [isVoiding, setIsVoiding] = useState(false);

  async function handleConfirmVoid() {
    if (!voidingCn) return;
    setIsVoiding(true);
    try {
      await api.voidCreditNote(voidingCn.id);
      setToast({
        type: 'success',
        text: `Credit Note ${voidingCn.credit_note_number} voided successfully. Customer ledger credit reversed.`,
      });
      setVoidingCn(null);
      load();
      setTimeout(() => setToast(null), 4500);
    } catch (err) {
      console.error('Failed to void credit note:', err);
      alert(err.message || 'Failed to void credit note');
    } finally {
      setIsVoiding(false);
    }
  }

  async function load(queryText = search) {
    setLoading(true);
    try {
      const params = {};
      if (queryText && queryText.trim()) params.q = queryText.trim();
      const list = await api.listCreditNotes(params);
      setCreditNotes(list || []);
    } catch (err) {
      console.error('Failed to load credit notes:', err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshKey]);

  useEffect(() => {
    const timer = setTimeout(() => {
      load(search);
    }, 250);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  // Quick stats calculation
  const stats = useMemo(() => {
    const totalCount = creditNotes.length;
    let totalAmount = 0;
    const customerIds = new Set();

    for (const cn of creditNotes) {
      totalAmount += Number(cn.total_amount || 0);
      if (cn.contact_id) customerIds.add(cn.contact_id);
    }

    return {
      totalCount,
      totalAmount,
      uniqueParties: customerIds.size,
    };
  }, [creditNotes]);

  // Client-side filtering for 0ms responsiveness
  const filteredCreditNotes = useMemo(() => {
    return creditNotes.filter((cn) => {
      if (reasonFilter && cn.reason !== reasonFilter) return false;

      const q = search.trim().toLowerCase();
      if (!q) return true;

      const cnNum = (cn.credit_note_number || '').toLowerCase();
      const name = (cn.contact_name || '').toLowerCase();
      const phone = (cn.contact_phone || '').toLowerCase();
      const origInv = (cn.original_invoice_no || '').toLowerCase();
      const summary = (cn.items_summary || '').toLowerCase();
      const reason = (cn.reason || '').toLowerCase();

      return (
        cnNum.includes(q) ||
        name.includes(q) ||
        phone.includes(q) ||
        origInv.includes(q) ||
        summary.includes(q) ||
        reason.includes(q)
      );
    });
  }, [creditNotes, search, reasonFilter]);

  async function handleSendWhatsApp(cn) {
    if (!cn?.id) return;
    setSendingId(cn.id);
    try {
      const res = await api.sendCreditNoteWhatsApp(cn.id);
      if (!res?.sentViaWeb && res?.whatsappUrl) {
        window.open(res.whatsappUrl, '_blank');
      }
      setToast({
        type: 'success',
        text: res?.sentViaWeb
          ? `✓ Credit Note PDF document sent directly to ${cn.contact_phone || 'customer'} via WhatsApp Web!`
          : `Credit Note ${cn.credit_note_number} link prepared for WhatsApp!`,
      });
    } catch (err) {
      setToast({ type: 'error', text: err.message || 'Failed to dispatch WhatsApp message' });
    } finally {
      setSendingId(null);
      setTimeout(() => setToast(null), 4000);
    }
  }

  function handlePrintPdf(cn) {
    const url = api.creditNotePdfUrl(cn.id);
    window.open(url, '_blank');
  }

  return (
    <div className="page-container" style={{ padding: '24px 32px', maxWidth: '1400px', margin: '0 auto' }}>
      {toast && (
        <div
          className="bulk-bar"
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            zIndex: 9999,
            background: toast.type === 'error' ? '#fef2f2' : '#f0fdf4',
            color: toast.type === 'error' ? '#991b1b' : '#166534',
            border: `1px solid ${toast.type === 'error' ? '#fecaca' : '#bbf7d0'}`,
            boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
            padding: '12px 20px',
            borderRadius: '8px',
            fontWeight: 600,
            fontSize: '14px',
          }}
        >
          {toast.type === 'error' ? '⚠️ ' : '✓ '} {toast.text}
        </div>
      )}

      {/* Header Section */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              background: '#ede9fe',
              color: '#7c3aed',
            }}>
              <CreditNoteIcon size={20} color="#7c3aed" />
            </span>
            <h1 style={{ margin: 0, fontSize: '1.6rem', fontWeight: 800, color: '#0f172a' }}>
              Sales Returns & Credit Notes
            </h1>
          </div>
          <p style={{ margin: '4px 0 0 46px', fontSize: '0.88rem', color: '#64748b' }}>
            Issue GST credit notes for returned products, rate differences, and ledger credits.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            background: 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)',
            color: '#fff',
            border: 'none',
            padding: '10px 20px',
            borderRadius: '8px',
            fontWeight: 700,
            fontSize: '14px',
            cursor: 'pointer',
            boxShadow: '0 2px 8px rgba(124, 58, 237, 0.3)',
            transition: 'all 0.15s ease',
          }}
        >
          <span>↩</span>
          + Create Sales Return
        </button>
      </div>

      {/* Top Stat Cards */}
      <div className="stats-row" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <StatCard
          label="Total Return Value"
          value={`₹${formatINR(stats.totalAmount)}`}
          subtext="Credited to customer passbooks"
        />
        <StatCard
          label="Sales Returns Issued"
          value={stats.totalCount}
          subtext="Total credit notes generated"
        />
        <StatCard
          label="Parties Credited"
          value={stats.uniqueParties}
          subtext="Customers with sales returns"
        />
      </div>

      {/* Search & Filter Bar */}
      <div style={{
        background: '#fff',
        border: '1px solid #e2e8f0',
        borderRadius: '10px',
        padding: '14px 18px',
        marginBottom: '20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '12px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: '1 1 320px' }}>
          <div style={{ position: 'relative', width: '100%', maxWidth: '380px' }}>
            <span style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', display: 'flex', pointerEvents: 'none' }}>
              <SearchIcon size={16} color="#94a3b8" />
            </span>
            <input
              type="text"
              placeholder="Search by CN #, Customer, Invoice #, Item…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{
                width: '100%',
                padding: '8px 12px 8px 34px',
                borderRadius: '6px',
                border: '1px solid #cbd5e1',
                fontSize: '13px',
                outline: 'none',
              }}
            />
          </div>

          <select
            value={reasonFilter}
            onChange={(e) => setReasonFilter(e.target.value)}
            style={{
              padding: '8px 12px',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              fontSize: '13px',
              color: '#334155',
              background: '#fff',
            }}
          >
            {REASON_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        <div style={{ fontSize: '13px', color: '#64748b', fontWeight: 500 }}>
          Showing <strong>{filteredCreditNotes.length}</strong> of {creditNotes.length} returns
        </div>
      </div>

      {/* Credit Notes Table */}
      <div style={{
        background: '#fff',
        border: '1px solid #e2e8f0',
        borderRadius: '10px',
        overflow: 'hidden',
        boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
      }}>
        {loading ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: '#64748b' }}>
            <div style={{ fontSize: '20px', marginBottom: '8px' }}>⏳</div>
            Loading sales returns…
          </div>
        ) : filteredCreditNotes.length === 0 ? (
          <div style={{ padding: '60px 24px', textAlign: 'center' }}>
            <div style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              background: '#ede9fe',
              color: '#7c3aed',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '24px',
              marginBottom: '14px',
            }}>
              ↩
            </div>
            <h3 style={{ margin: '0 0 6px', fontSize: '1.15rem', color: '#0f172a' }}>
              {search || reasonFilter ? 'No matching sales returns found' : 'No Sales Returns Issued Yet'}
            </h3>
            <p style={{ margin: '0 auto 18px', maxWidth: '420px', fontSize: '0.88rem', color: '#64748b' }}>
              {search || reasonFilter
                ? 'Try adjusting your search criteria or clearing filters.'
                : 'Issue your first GST Credit Note to record product returns, adjust billing mistakes, or credit customer accounts.'}
            </p>
            {!search && !reasonFilter && (
              <button
                type="button"
                onClick={() => setShowCreateModal(true)}
                style={{
                  background: '#7c3aed',
                  color: '#fff',
                  border: 'none',
                  padding: '9px 18px',
                  borderRadius: '6px',
                  fontWeight: 600,
                  fontSize: '13.5px',
                  cursor: 'pointer',
                }}
              >
                + Create Sales Return
              </button>
            )}
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#64748b', fontWeight: 600 }}>
                  <th style={{ padding: '12px 16px' }}>Date</th>
                  <th style={{ padding: '12px 16px' }}>Credit Note #</th>
                  <th style={{ padding: '12px 16px' }}>Customer / Party</th>
                  <th style={{ padding: '12px 16px' }}>Original Invoice</th>
                  <th style={{ padding: '12px 16px' }}>Return Reason & Items</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Credited Amount</th>
                  <th style={{ padding: '12px 16px', textAlign: 'center', width: '140px' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredCreditNotes.map((cn) => {
                  const isSendingThis = sendingId === cn.id;
                  return (
                    <tr
                      key={cn.id}
                      style={{
                        borderBottom: '1px solid #f1f5f9',
                        transition: 'background 0.1s ease',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.background = '#faf5ff')}
                      onMouseLeave={(e) => (e.currentTarget.style.background = '#fff')}
                    >
                      {/* Date */}
                      <td style={{ padding: '14px 16px', color: '#475569', whiteSpace: 'nowrap' }}>
                        {formatDate(cn.credit_note_date || cn.created_at)}
                      </td>

                      {/* CN Number */}
                      <td style={{ padding: '14px 16px', whiteSpace: 'nowrap' }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '5px',
                          background: '#ede9fe',
                          color: '#6d28d9',
                          fontWeight: 700,
                          fontSize: '12.5px',
                          padding: '3px 8px',
                          borderRadius: '5px',
                          border: '1px solid #ddd6fe',
                        }}>
                          <span>↩</span> {cn.credit_note_number}
                        </span>
                      </td>

                      {/* Customer */}
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{ fontWeight: 700, color: '#0f172a' }}>
                          {cn.contact_name || 'Customer'}
                        </div>
                        <div style={{ fontSize: '11.5px', color: '#64748b', marginTop: '2px' }}>
                          {cn.contact_phone ? `📞 ${cn.contact_phone}` : ''} {cn.contact_city ? `· 📍 ${cn.contact_city}` : ''}
                        </div>
                      </td>

                      {/* Original Invoice */}
                      <td style={{ padding: '14px 16px', whiteSpace: 'nowrap' }}>
                        {cn.original_invoice_no ? (
                          <span style={{
                            background: '#f1f5f9',
                            color: '#334155',
                            fontWeight: 600,
                            fontSize: '12px',
                            padding: '2px 7px',
                            borderRadius: '4px',
                            border: '1px solid #e2e8f0',
                          }}>
                            📄 {cn.original_invoice_no}
                          </span>
                        ) : (
                          <span style={{ color: '#94a3b8', fontSize: '12px' }}>Standalone</span>
                        )}
                      </td>

                      {/* Reason & Items */}
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{
                          display: 'inline-block',
                          background: '#f3e8ff',
                          color: '#7e22ce',
                          fontSize: '11px',
                          fontWeight: 600,
                          padding: '2px 7px',
                          borderRadius: '4px',
                          marginBottom: '3px',
                        }}>
                          {cn.reason || 'Sales Return'}
                        </div>
                        {cn.items_summary && (
                          <div style={{ fontSize: '11.5px', color: '#64748b', maxWidth: '300px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={cn.items_summary}>
                            📦 {cn.items_summary}
                          </div>
                        )}
                      </td>

                      {/* Credited Amount */}
                      <td style={{ padding: '14px 16px', textAlign: 'right', whiteSpace: 'nowrap' }}>
                        <div style={{ fontWeight: 800, fontSize: '14.5px', color: '#7c3aed' }}>
                          ₹{formatINR(cn.total_amount)}
                        </div>
                        {Number(cn.tax_amount || 0) > 0 && (
                          <div style={{ fontSize: '11px', color: '#94a3b8' }}>
                            incl. ₹{formatINR(cn.tax_amount)} GST
                          </div>
                        )}
                      </td>

                      {/* Actions */}
                      <td style={{ padding: '14px 16px', textAlign: 'center', whiteSpace: 'nowrap' }}>
                        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                          {/* Print/View PDF */}
                          <button
                            type="button"
                            onClick={() => handlePrintPdf(cn)}
                            title="Download / Print GST Credit Note PDF"
                            style={{
                              background: '#f8fafc',
                              border: '1px solid #cbd5e1',
                              color: '#334155',
                              padding: '5px 9px',
                              borderRadius: '6px',
                              cursor: 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                              fontSize: '12px',
                              fontWeight: 600,
                            }}
                          >
                            <PrinterIcon size={13} color="#2563eb" />
                            PDF
                          </button>

                          {/* WhatsApp Dispatch */}
                          <button
                            type="button"
                            onClick={() => handleSendWhatsApp(cn)}
                            disabled={isSendingThis}
                            title="Send Credit Note PDF via WhatsApp"
                            style={{
                              background: isSendingThis ? '#f1f5f9' : '#f0fdf4',
                              border: '1px solid #bbf7d0',
                              color: '#166534',
                              padding: '5px 9px',
                              borderRadius: '6px',
                              cursor: isSendingThis ? 'not-allowed' : 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                              fontSize: '12px',
                              fontWeight: 600,
                            }}
                          >
                            <WhatsAppIcon size={13} color="#16a34a" />
                            {isSendingThis ? 'Sending…' : 'Share'}
                          </button>

                          {/* Void / Delete Credit Note */}
                          <button
                            type="button"
                            onClick={() => setVoidingCn(cn)}
                            title="Void / Cancel this Credit Note (Reverses ledger credit)"
                            style={{
                              background: '#fff1f2',
                              border: '1px solid #fecdd3',
                              color: '#be123c',
                              padding: '5px 9px',
                              borderRadius: '6px',
                              cursor: 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                              fontSize: '12px',
                              fontWeight: 600,
                            }}
                          >
                            <TrashIcon size={13} color="#e11d48" />
                            Void
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal for Creating Sales Return */}
      {showCreateModal && (
        <CreateCreditNoteModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={(res) => {
            setShowCreateModal(false);
            setToast({
              type: 'success',
              text: `Credit Note ${res.credit_note_number} created successfully! Ledger balance updated.`,
            });
            load();
            setTimeout(() => setToast(null), 4500);
          }}
        />
      )}

      {/* Modal for Confirming Void / Cancellation of Credit Note */}
      {voidingCn && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.65)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            padding: '16px',
          }}
          onClick={(e) => {
            if (e.target === e.currentTarget && !isVoiding) setVoidingCn(null);
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '14px',
              maxWidth: '460px',
              width: '100%',
              boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
              overflow: 'hidden',
              animation: 'modalSlideIn 0.15s ease-out',
            }}
          >
            <div
              style={{
                padding: '18px 24px',
                borderBottom: '1px solid #fee2e2',
                backgroundColor: '#fff1f2',
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
              }}
            >
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '50%',
                  backgroundColor: '#fee2e2',
                  border: '2px solid #fecdd3',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <TrashIcon size={18} color="#e11d48" />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#9f1239' }}>
                  Void Credit Note {voidingCn.credit_note_number}
                </h3>
                <p style={{ margin: '2px 0 0', fontSize: '12px', color: '#be123c' }}>
                  Irreversible ledger reversal and balance restoration
                </p>
              </div>
            </div>

            <div style={{ padding: '20px 24px', fontSize: '13.5px', color: '#334155', lineHeight: 1.5 }}>
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '8px',
                  padding: '12px 16px',
                  marginBottom: '16px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <span style={{ color: '#64748b', fontSize: '12.5px' }}>Customer:</span>
                  <span style={{ fontWeight: 600 }}>{voidingCn.contact_name || 'Walk-in Customer'}</span>
                </div>
                {voidingCn.original_invoice_no && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <span style={{ color: '#64748b', fontSize: '12.5px' }}>Original Invoice:</span>
                    <span style={{ fontWeight: 600, color: '#2563eb' }}>{voidingCn.original_invoice_no}</span>
                  </div>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '6px', borderTop: '1px dashed #e2e8f0' }}>
                  <span style={{ color: '#64748b', fontSize: '12.5px' }}>Credited Amount:</span>
                  <span style={{ fontWeight: 700, color: '#e11d48', fontSize: '14.5px' }}>
                    ₹{formatINR(voidingCn.total_amount)}
                  </span>
                </div>
              </div>

              <p style={{ margin: 0, fontSize: '13px', color: '#475569' }}>
                Voiding will permanently remove this credit note, <strong>reverse ₹{formatINR(voidingCn.total_amount)} credit</strong> from the party's ledger, and restore the returnable line items on the original invoice.
              </p>
            </div>

            <div
              style={{
                padding: '14px 24px',
                backgroundColor: '#f8fafc',
                borderTop: '1px solid #f1f5f9',
                display: 'flex',
                justifyContent: 'flex-end',
                gap: '10px',
              }}
            >
              <button
                type="button"
                onClick={() => setVoidingCn(null)}
                disabled={isVoiding}
                style={{
                  padding: '8px 16px',
                  borderRadius: '6px',
                  border: '1px solid #cbd5e1',
                  background: '#ffffff',
                  color: '#475569',
                  fontWeight: 600,
                  fontSize: '13px',
                  cursor: isVoiding ? 'not-allowed' : 'pointer',
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmVoid}
                disabled={isVoiding}
                style={{
                  padding: '8px 18px',
                  borderRadius: '6px',
                  border: 'none',
                  background: '#e11d48',
                  color: '#ffffff',
                  fontWeight: 600,
                  fontSize: '13px',
                  cursor: isVoiding ? 'not-allowed' : 'pointer',
                  boxShadow: '0 2px 4px rgba(225, 29, 72, 0.25)',
                }}
              >
                {isVoiding ? 'Voiding…' : 'Yes, Void Credit Note'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
