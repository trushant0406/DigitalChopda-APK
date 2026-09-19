import { useEffect, useState } from 'react';
import { api } from '../api';
import PhoneLink from '../components/PhoneLink';
import RecordPaymentInModal from '../components/RecordPaymentInModal';
import InvoiceForm from '../components/InvoiceForm';
import { formatDate, formatTime, formatDateTime } from '../dateFormat';
import {
  CalendarIcon,
  ExcelIcon,
  PrinterIcon,
  WhatsAppIcon,
  ChevronDownIcon,
  ArrowLeftIcon,
  CloseIcon,
  ReminderBellIcon,
  FileInvoiceIcon,
  PaymentInIcon,
  WalletIcon,
  ClockAlertIcon,
  SalesTrendingIcon,
  ReceivedCheckIcon,
  BookOpenIcon,
  UserCircleIcon,
  ReceiptIcon,
  SearchIcon,
  PhoneIcon,
  MapPinIcon,
} from '../components/Icons';

function formatINR(val) {
  return new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
    Number(val || 0)
  );
}

export default function CreditPage({ refreshKey }) {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);

  // Selected customer for the right master-detail pane
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [ledger, setLedger] = useState(null);
  const [loadingLedger, setLoadingLedger] = useState(false);

  // Tab & Filters
  const [activeTab, setActiveTab] = useState('ledger'); // 'ledger' | 'profile' | 'transactions'
  const [dateFilter, setDateFilter] = useState('all');
  const [sortOrder, setSortOrder] = useState('desc'); // 'desc' (newest first) | 'asc' (oldest first)

  // Modals & Actions
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [showInvoiceModal, setShowInvoiceModal] = useState(false);
  const [sendingReminder, setSendingReminder] = useState(false);
  const [sharingWa, setSharingWa] = useState(false);
  const [toast, setToast] = useState(null);

  // Profile Edit Form State
  const [profileForm, setProfileForm] = useState({
    opening_balance: '0',
    opening_balance_type: 'to_receive',
    opening_balance_date: new Date().toISOString().split('T')[0],
    credit_limit: '0',
    gstin: '',
    pan: '',
    billing_address: '',
    city: '',
    state: '',
  });
  const [savingProfile, setSavingProfile] = useState(false);

  async function loadCustomers() {
    setLoading(true);
    try {
      const params = {};
      if (query) params.q = query;
      if (status) params.status = status;
      const list = await api.listCreditCustomers(params);
      setCustomers(list || []);
      // If none selected or selected customer was deleted, select first
      if (list?.length > 0) {
        if (!selectedCustomer || !list.some((c) => c.id === selectedCustomer.id)) {
          setSelectedCustomer(list[0]);
        }
      }
    } finally {
      setLoading(false);
    }
  }

  async function loadCustomerLedger(customerId) {
    if (!customerId) return;
    setLoadingLedger(true);
    try {
      const data = await api.getCustomerLedger(customerId);
      setLedger(data);
      const c = data?.customer;
      if (c) {
        setProfileForm({
          opening_balance: String(c.opening_balance || 0),
          opening_balance_type: c.opening_balance_type || 'to_receive',
          opening_balance_date: c.opening_balance_date ? String(c.opening_balance_date).slice(0, 10) : new Date().toISOString().split('T')[0],
          credit_limit: String(c.credit_limit || 0),
          gstin: c.gstin || '',
          pan: c.pan || '',
          billing_address: c.billing_address || '',
          city: c.city || '',
          state: c.state || '',
        });
      }
    } catch (err) {
      console.error('Failed to load ledger:', err);
    } finally {
      setLoadingLedger(false);
    }
  }

  useEffect(() => {
    loadCustomers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, status, refreshKey]);

  useEffect(() => {
    if (selectedCustomer?.id) {
      loadCustomerLedger(selectedCustomer.id);
    }
  }, [selectedCustomer?.id]);

  function handleSelectCustomer(c) {
    setSelectedCustomer(c);
  }

  async function handleSaveProfile(e) {
    e.preventDefault();
    if (!selectedCustomer) return;
    setSavingProfile(true);
    try {
      await api.updateContact(selectedCustomer.id, {
        opening_balance: Number(profileForm.opening_balance || 0),
        opening_balance_type: profileForm.opening_balance_type,
        opening_balance_date: profileForm.opening_balance_date || null,
        credit_limit: Number(profileForm.credit_limit || 0),
        gstin: profileForm.gstin || null,
        pan: profileForm.pan || null,
        billing_address: profileForm.billing_address || null,
        city: profileForm.city || null,
        state: profileForm.state || null,
      });
      setToast({ type: 'success', msg: '✓ Party profile & opening balance updated!' });
      await loadCustomers();
      await loadCustomerLedger(selectedCustomer.id);
      setTimeout(() => setToast(null), 4000);
    } catch (err) {
      setToast({ type: 'error', msg: 'Failed to update: ' + err.message });
    } finally {
      setSavingProfile(false);
    }
  }

  async function handleSendReminder() {
    if (!selectedCustomer?.phone) {
      alert('This customer does not have a phone number.');
      return;
    }
    setSendingReminder(true);
    setToast(null);
    try {
      await api.sendCustomerReminderWhatsApp(selectedCustomer.id);
      setToast({ type: 'success', msg: `✓ WhatsApp Payment Reminder sent to ${selectedCustomer.phone}!` });
      setTimeout(() => setToast(null), 5000);
    } catch (err) {
      setToast({ type: 'error', msg: `Failed to send reminder: ${err.message}` });
    } finally {
      setSendingReminder(false);
    }
  }

  async function handleShareWhatsApp() {
    if (!selectedCustomer?.phone) {
      alert('This customer does not have a phone number.');
      return;
    }
    setSharingWa(true);
    setToast(null);
    try {
      await api.sendCustomerStatementWhatsApp(selectedCustomer.id);
      setToast({ type: 'success', msg: `✓ Official Statement PDF sent to ${selectedCustomer.phone} via WhatsApp!` });
      setTimeout(() => setToast(null), 5000);
    } catch (err) {
      setToast({ type: 'error', msg: `Failed to share statement: ${err.message}` });
    } finally {
      setSharingWa(false);
    }
  }

  function exportCSV() {
    if (!ledger?.entries?.length) return;
    const headers = ['Date', 'Voucher', 'Sr No', 'Payment Mode', 'Credit', 'Debit', 'Balance'];
    const csvRows = [headers.join(',')];
    for (const e of ledger.entries) {
      csvRows.push([
        `"${e.date ? (e.voucher === 'Opening Balance' ? '—' : String(e.date).slice(0, 10)) : '—'}"`,
        `"${e.voucher || ''}"`,
        `"${e.sr_no || ''}"`,
        `"${e.payment_mode || '—'}"`,
        e.credit || 0,
        e.debit || 0,
        e.balance || 0,
      ].join(','));
    }
    const blob = new Blob([csvRows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Ledger-${(selectedCustomer?.name || 'Party').replace(/[^a-zA-Z0-9]/g, '_')}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const stats = ledger?.stats || {
    total_receivable_amount: 0,
    overdue_amount: 0,
    total_sales_amount: 0,
    total_received_amount: 0,
    current_balance: 0,
  };

  const nowTs = Date.now();
  const filterDays = dateFilter === '365' ? 365 : dateFilter === '90' ? 90 : dateFilter === '30' ? 30 : null;
  const filteredEntries = (ledger?.entries || []).filter((e) => {
    if (!filterDays) return true;
    if (e.voucher === 'Opening Balance') return true;
    const t = new Date(e.date).getTime();
    return nowTs - t <= filterDays * 86400000;
  });

  const sortedEntries = [...filteredEntries].sort((a, b) => {
    if (a.voucher === 'Opening Balance') return sortOrder === 'desc' ? 1 : -1;
    if (b.voucher === 'Opening Balance') return sortOrder === 'desc' ? -1 : 1;
    const tA = a.timestamp || new Date(a.date).getTime();
    const tB = b.timestamp || new Date(b.date).getTime();
    if (tA !== tB) {
      return sortOrder === 'desc' ? tB - tA : tA - tB;
    }
    // Secondary tie-breaker when timestamps are identical:
    // When Newest First ('desc'), higher ref_id (newer transaction) comes FIRST!
    const idA = Number(a.ref_id) || 0;
    const idB = Number(b.ref_id) || 0;
    if (idA !== idB) {
      return sortOrder === 'desc' ? idB - idA : idA - idB;
    }
    return sortOrder === 'desc'
      ? (b.sr_no || '').localeCompare(a.sr_no || '', undefined, { numeric: true })
      : (a.sr_no || '').localeCompare(b.sr_no || '', undefined, { numeric: true });
  });

  return (
    <div style={{ display: 'flex', gap: '16px', height: 'calc(100vh - 80px)', minHeight: '650px' }}>
      {/* LEFT SIDEBAR: PARTIES LIST (Exact myBillBook Layout) */}
      <div style={{
        width: '320px',
        minWidth: '280px',
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '8px',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}>
        {/* Search header */}
        <div style={{ padding: '12px', borderBottom: '1px solid #e2e8f0' }}>
          <div style={{ position: 'relative' }}>
            <span style={{ position: 'absolute', left: '10px', top: '10px', display: 'flex', alignItems: 'center' }}>
              <SearchIcon size={14} color="#94a3b8" />
            </span>
            <input
              type="text"
              placeholder="Search party…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '7px 28px 7px 30px',
                borderRadius: '6px',
                border: '1px solid #cbd5e1',
                fontSize: '13px',
                boxSizing: 'border-box'
              }}
            />
            {query && (
              <button
                type="button"
                onClick={() => setQuery('')}
                style={{ position: 'absolute', right: '8px', top: '8px', background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', display: 'flex', alignItems: 'center' }}
              >
                <CloseIcon size={13} color="#94a3b8" />
              </button>
            )}
          </div>
        </div>

        {/* Customer Items List */}
        <div style={{ flex: 1, overflowY: 'auto' }}>
          {loading && <div style={{ padding: '20px', textAlign: 'center', color: '#94a3b8', fontSize: '13px' }}>Loading parties…</div>}
          {!loading && customers.length === 0 && (
            <div style={{ padding: '24px', textAlign: 'center', color: '#94a3b8', fontSize: '13px' }}>
              No parties found.
            </div>
          )}
          {!loading && customers.map((c) => {
            const isSelected = selectedCustomer?.id === c.id;
            const outstanding = Number(c.outstanding || 0);
            return (
              <div
                key={c.id}
                onClick={() => handleSelectCustomer(c)}
                style={{
                  padding: '12px 14px',
                  borderBottom: '1px solid #f1f5f9',
                  cursor: 'pointer',
                  background: isSelected ? '#ede9fe' : 'transparent',
                  borderLeft: isSelected ? '4px solid #7c3aed' : '4px solid transparent',
                  transition: 'background 0.15s ease',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px', color: '#0f172a', textTransform: 'uppercase' }}>
                    {c.name || 'Unnamed Contact'}
                  </div>
                  <div style={{ fontSize: '11.5px', color: '#64748b', marginTop: '2px' }}>
                    Customer {c.phone && `· ${c.phone}`}
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{
                    fontWeight: 700,
                    fontSize: '13px',
                    color: outstanding > 0 ? '#16a34a' : (outstanding < 0 ? '#dc2626' : '#64748b')
                  }}>
                    ₹ {formatINR(Math.abs(outstanding))} {outstanding > 0 ? '↓' : ''}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* RIGHT MAIN PANE: SELECTED PARTY DASHBOARD & LEDGER */}
      <div style={{
        flex: 1,
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '8px',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}>
        {!selectedCustomer ? (
          <div style={{ padding: '60px', textAlign: 'center', color: '#94a3b8' }}>
            Select a party from the left to view their complete passbook ledger.
          </div>
        ) : (
          <div style={{ flex: 1, overflowY: 'auto', padding: '20px 24px' }}>
            {/* Top Bar Header with Actions */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              paddingBottom: '14px',
              borderBottom: '1px solid #e2e8f0',
              marginBottom: '14px'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 800, textTransform: 'uppercase', color: '#0f172a' }}>
                  {selectedCustomer.name || 'Party'}
                </h1>
                {selectedCustomer.gstin && (
                  <span style={{ background: '#f1f5f9', color: '#475569', fontSize: '11px', padding: '2px 8px', borderRadius: '4px', fontWeight: 600 }}>
                    GSTIN: {selectedCustomer.gstin}
                  </span>
                )}
              </div>

              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                {selectedCustomer.phone && (
                  <button
                    type="button"
                    onClick={handleSendReminder}
                    disabled={sendingReminder || stats.total_receivable_amount <= 0}
                    style={{
                      background: '#f0fdf4',
                      color: '#166534',
                      border: '1px solid #86efac',
                      padding: '8px 14px',
                      borderRadius: '7px',
                      fontWeight: 600,
                      fontSize: '13px',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '7px',
                      cursor: stats.total_receivable_amount > 0 ? 'pointer' : 'not-allowed',
                      opacity: stats.total_receivable_amount > 0 ? 1 : 0.6,
                      boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
                    }}
                    title="Send WhatsApp Payment Reminder"
                  >
                    <ReminderBellIcon size={15} color="#166534" />
                    <span>{sendingReminder ? 'Sending…' : 'Send Reminder'}</span>
                  </button>
                )}

                <button
                  type="button"
                  onClick={() => setShowInvoiceModal(true)}
                  style={{
                    background: '#2563eb',
                    color: '#fff',
                    border: 'none',
                    padding: '8px 16px',
                    borderRadius: '7px',
                    fontWeight: 600,
                    fontSize: '13px',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '7px',
                    cursor: 'pointer',
                    boxShadow: '0 1px 3px rgba(37,99,235,0.25)'
                  }}
                >
                  <FileInvoiceIcon size={15} color="#fff" />
                  <span>Create Sales Invoice</span>
                </button>

                <button
                  type="button"
                  onClick={() => setShowPaymentModal(true)}
                  style={{
                    background: '#16a34a',
                    color: '#fff',
                    border: 'none',
                    padding: '8px 16px',
                    borderRadius: '7px',
                    fontWeight: 600,
                    fontSize: '13px',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '7px',
                    cursor: 'pointer',
                    boxShadow: '0 1px 3px rgba(22,163,74,0.25)'
                  }}
                >
                  <PaymentInIcon size={15} color="#fff" />
                  <span>Payment In</span>
                </button>
              </div>
            </div>

            {/* Tabs: Transactions, Profile, Ledger, Item Wise */}
            <div style={{ display: 'flex', gap: '24px', borderBottom: '2px solid #e2e8f0', marginBottom: '16px' }}>
              <button
                type="button"
                style={{
                  background: 'none', border: 'none', padding: '10px 4px', fontSize: '13px', fontWeight: 700, cursor: 'pointer',
                  color: activeTab === 'ledger' ? '#2563eb' : '#64748b',
                  borderBottom: activeTab === 'ledger' ? '3px solid #2563eb' : '3px solid transparent',
                  display: 'inline-flex', alignItems: 'center', gap: '7px'
                }}
                onClick={() => setActiveTab('ledger')}
              >
                <BookOpenIcon size={15} color={activeTab === 'ledger' ? '#2563eb' : '#64748b'} />
                <span>Ledger (Statement)</span>
              </button>
              <button
                type="button"
                style={{
                  background: 'none', border: 'none', padding: '10px 4px', fontSize: '13px', fontWeight: 700, cursor: 'pointer',
                  color: activeTab === 'profile' ? '#2563eb' : '#64748b',
                  borderBottom: activeTab === 'profile' ? '3px solid #2563eb' : '3px solid transparent',
                  display: 'inline-flex', alignItems: 'center', gap: '7px'
                }}
                onClick={() => setActiveTab('profile')}
              >
                <UserCircleIcon size={15} color={activeTab === 'profile' ? '#2563eb' : '#64748b'} />
                <span>Profile &amp; Opening Balance</span>
              </button>
              <button
                type="button"
                style={{
                  background: 'none', border: 'none', padding: '10px 4px', fontSize: '13px', fontWeight: 700, cursor: 'pointer',
                  color: activeTab === 'transactions' ? '#2563eb' : '#64748b',
                  borderBottom: activeTab === 'transactions' ? '3px solid #2563eb' : '3px solid transparent',
                  display: 'inline-flex', alignItems: 'center', gap: '7px'
                }}
                onClick={() => setActiveTab('transactions')}
              >
                <ReceiptIcon size={15} color={activeTab === 'transactions' ? '#2563eb' : '#64748b'} />
                <span>Transactions</span>
              </button>
            </div>

            {toast && (
              <div style={{
                padding: '10px 14px',
                borderRadius: '6px',
                marginBottom: '14px',
                fontSize: '13px',
                background: toast.type === 'success' ? '#f0fdf4' : '#fef2f2',
                color: toast.type === 'success' ? '#166534' : '#991b1b',
                border: `1px solid ${toast.type === 'success' ? '#bbf7d0' : '#fecaca'}`
              }}>
                {toast.msg}
              </div>
            )}

            {/* 4 STAT CARDS (Exact match with myBillBook) */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '14px', marginBottom: '16px' }}>
              <div className="card" style={{ padding: '14px 16px', background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', boxShadow: '0 1px 2px rgba(0,0,0,0.03)' }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{ width: 24, height: 24, borderRadius: '50%', background: '#eff6ff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <WalletIcon size={14} color="#2563eb" />
                  </div>
                  <span>Total Receivable Amount</span>
                </div>
                <div style={{ fontSize: '22px', fontWeight: 800, marginTop: '8px', color: stats.total_receivable_amount > 0 ? '#0f172a' : '#16a34a' }}>
                  ₹ {formatINR(stats.total_receivable_amount)}
                </div>
              </div>

              <div className="card" style={{ padding: '14px 16px', background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', boxShadow: '0 1px 2px rgba(0,0,0,0.03)' }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{ width: 24, height: 24, borderRadius: '50%', background: '#fff7ed', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <ClockAlertIcon size={14} color="#ea580c" />
                  </div>
                  <span>Overdue Amount</span>
                </div>
                <div style={{ fontSize: '22px', fontWeight: 800, marginTop: '8px', color: stats.overdue_amount > 0 ? '#ea580c' : '#0f172a' }}>
                  ₹ {formatINR(stats.overdue_amount)}
                </div>
              </div>

              <div className="card" style={{ padding: '14px 16px', background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', boxShadow: '0 1px 2px rgba(0,0,0,0.03)' }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{ width: 24, height: 24, borderRadius: '50%', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <SalesTrendingIcon size={14} color="#475569" />
                  </div>
                  <span>Total Sales Amount</span>
                </div>
                <div style={{ fontSize: '22px', fontWeight: 800, marginTop: '8px', color: '#0f172a' }}>
                  ₹ {formatINR(stats.total_sales_amount)}
                </div>
              </div>

              <div className="card" style={{ padding: '14px 16px', background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', boxShadow: '0 1px 2px rgba(0,0,0,0.03)' }}>
                <div style={{ fontSize: '12px', color: '#64748b', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{ width: 24, height: 24, borderRadius: '50%', background: '#f0fdf4', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <ReceivedCheckIcon size={14} color="#16a34a" />
                  </div>
                  <span>Total Received Amount</span>
                </div>
                <div style={{ fontSize: '22px', fontWeight: 800, marginTop: '8px', color: '#16a34a' }}>
                  ₹ {formatINR(stats.total_received_amount)}
                </div>
              </div>
            </div>

            {/* TAB CONTENT: LEDGER TABLE */}
            {activeTab === 'ledger' && (
              <div className="card" style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '0', overflow: 'hidden' }}>
                {/* Modern High-Clarity Action Toolbar */}
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '12px 18px',
                  borderBottom: '1px solid #e2e8f0',
                  background: '#ffffff',
                  flexWrap: 'wrap',
                  gap: '12px'
                }}>
                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
                    {/* Period Selector */}
                    <div style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '8px',
                      background: '#f8fafc',
                      border: '1px solid #cbd5e1',
                      borderRadius: '8px',
                      padding: '6px 12px',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
                    }}>
                      <CalendarIcon size={16} color="#475569" />
                      <span style={{ fontSize: '11.5px', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.4px' }}>
                        Period:
                      </span>
                      <select
                        value={dateFilter}
                        onChange={(e) => setDateFilter(e.target.value)}
                        style={{
                          border: 'none',
                          background: 'transparent',
                          fontSize: '13px',
                          fontWeight: 600,
                          color: '#0f172a',
                          cursor: 'pointer',
                          outline: 'none',
                          paddingRight: '4px'
                        }}
                      >
                        <option value="all">All Time (Full Ledger)</option>
                        <option value="365">Last 365 Days</option>
                        <option value="90">Last 90 Days</option>
                        <option value="30">Last 30 Days</option>
                      </select>
                      <ChevronDownIcon size={12} color="#64748b" />
                    </div>

                    {/* Sort Order Selector */}
                    <div style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '8px',
                      background: '#f8fafc',
                      border: '1px solid #cbd5e1',
                      borderRadius: '8px',
                      padding: '6px 12px',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
                    }}>
                      <span style={{ fontSize: '11.5px', fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.4px' }}>
                        Sort:
                      </span>
                      <select
                        value={sortOrder}
                        onChange={(e) => setSortOrder(e.target.value)}
                        style={{
                          border: 'none',
                          background: 'transparent',
                          fontSize: '13px',
                          fontWeight: 600,
                          color: '#0f172a',
                          cursor: 'pointer',
                          outline: 'none',
                          paddingRight: '4px'
                        }}
                      >
                        <option value="desc">⬇️ Newest First (Recent)</option>
                        <option value="asc">⬆️ Oldest First (Chronological)</option>
                      </select>
                      <ChevronDownIcon size={12} color="#64748b" />
                    </div>
                  </div>

                  {/* Right: Distinct, High-Affordance Action Buttons */}
                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    {/* Download Excel */}
                    <button
                      type="button"
                      onClick={exportCSV}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '8px',
                        padding: '7px 14px',
                        borderRadius: '8px',
                        border: '1px solid #cbd5e1',
                        background: '#ffffff',
                        color: '#0f172a',
                        fontSize: '13px',
                        fontWeight: 600,
                        cursor: 'pointer',
                        boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
                        transition: 'all 0.15s ease'
                      }}
                      onMouseOver={(e) => { e.currentTarget.style.background = '#f0fdf4'; e.currentTarget.style.borderColor = '#86efac'; }}
                      onMouseOut={(e) => { e.currentTarget.style.background = '#ffffff'; e.currentTarget.style.borderColor = '#cbd5e1'; }}
                      title="Download Party Ledger as Excel CSV file"
                    >
                      <ExcelIcon size={16} color="#16a34a" />
                      <span>Download Excel</span>
                      <span style={{
                        fontSize: '10px',
                        padding: '1px 5px',
                        borderRadius: '4px',
                        background: '#dcfce7',
                        color: '#15803d',
                        fontWeight: 700,
                        letterSpacing: '0.3px'
                      }}>
                        CSV
                      </span>
                    </button>

                    {/* Print PDF */}
                    <button
                      type="button"
                      onClick={() => window.open(api.customerStatementPdfUrl(selectedCustomer.id), '_blank')}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '8px',
                        padding: '7px 14px',
                        borderRadius: '8px',
                        border: '1px solid #cbd5e1',
                        background: '#ffffff',
                        color: '#0f172a',
                        fontSize: '13px',
                        fontWeight: 600,
                        cursor: 'pointer',
                        boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
                        transition: 'all 0.15s ease'
                      }}
                      onMouseOver={(e) => { e.currentTarget.style.background = '#eff6ff'; e.currentTarget.style.borderColor = '#93c5fd'; }}
                      onMouseOut={(e) => { e.currentTarget.style.background = '#ffffff'; e.currentTarget.style.borderColor = '#cbd5e1'; }}
                      title="Open printable official statement PDF"
                    >
                      <PrinterIcon size={16} color="#2563eb" />
                      <span>Print PDF</span>
                      <span style={{
                        fontSize: '10px',
                        padding: '1px 5px',
                        borderRadius: '4px',
                        background: '#dbeafe',
                        color: '#1d4ed8',
                        fontWeight: 700,
                        letterSpacing: '0.3px'
                      }}>
                        PDF
                      </span>
                    </button>

                    {/* Share PDF on WhatsApp */}
                    {selectedCustomer.phone && (
                      <button
                        type="button"
                        onClick={handleShareWhatsApp}
                        disabled={sharingWa}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '8px',
                          padding: '7px 15px',
                          borderRadius: '8px',
                          border: '1px solid #86efac',
                          background: '#f0fdf4',
                          color: '#15803d',
                          fontSize: '13px',
                          fontWeight: 600,
                          cursor: sharingWa ? 'wait' : 'pointer',
                          boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
                          transition: 'all 0.15s ease',
                          opacity: sharingWa ? 0.7 : 1
                        }}
                        onMouseOver={(e) => { if (!sharingWa) { e.currentTarget.style.background = '#dcfce7'; e.currentTarget.style.borderColor = '#4ade80'; } }}
                        onMouseOut={(e) => { if (!sharingWa) { e.currentTarget.style.background = '#f0fdf4'; e.currentTarget.style.borderColor = '#86efac'; } }}
                        title={`Send statement PDF directly to ${selectedCustomer.phone} on WhatsApp`}
                      >
                        <WhatsAppIcon size={16} color="#16a34a" />
                        <span>{sharingWa ? 'Sending WhatsApp…' : 'Share on WhatsApp'}</span>
                      </button>
                    )}
                  </div>
                </div>

                {loadingLedger && <div style={{ padding: '30px', textAlign: 'center', color: '#94a3b8' }}>Loading ledger…</div>}

                {!loadingLedger && sortedEntries.length === 0 && (
                  <div style={{ padding: '30px', textAlign: 'center', color: '#94a3b8' }}>No entries found.</div>
                )}

                {!loadingLedger && sortedEntries.length > 0 && (
                  <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                      <thead>
                        <tr style={{ background: '#f8fafc', color: '#475569', textAlign: 'left', borderBottom: '2px solid #e2e8f0' }}>
                          <th style={{ padding: '10px 14px', width: '120px' }}>Date</th>
                          <th style={{ padding: '10px 14px', width: '150px' }}>Voucher</th>
                          <th style={{ padding: '10px 14px', width: '130px' }}>Sr No</th>
                          <th style={{ padding: '10px 14px', width: '120px' }}>Payment Mode</th>
                          <th style={{ padding: '10px 14px', textAlign: 'right', width: '130px' }}>Credit</th>
                          <th style={{ padding: '10px 14px', textAlign: 'right', width: '130px' }}>Debit</th>
                          <th style={{ padding: '10px 14px', textAlign: 'right', width: '140px' }}>Balance</th>
                        </tr>
                      </thead>
                      <tbody>
                        {sortedEntries.map((row, idx) => {
                          const isDebit = Number(row.debit || 0) > 0;
                          const isCredit = Number(row.credit || 0) > 0;
                          const bal = Number(row.balance ?? row.running_balance ?? 0);
                          const isOpening = row.voucher === 'Opening Balance';

                          return (
                            <tr
                              key={row.id || idx}
                              style={{
                                borderBottom: '1px solid #f1f5f9',
                                background: isOpening ? '#f8fafc' : (idx % 2 === 0 ? '#fff' : '#fafafa')
                              }}
                            >
                              <td style={{ padding: '10px 14px', color: '#64748b', whiteSpace: 'nowrap' }}>
                                {isOpening ? '—' : (
                                  <div>
                                    <div style={{ color: '#0f172a', fontWeight: 500 }}>{formatDate(row.date)}</div>
                                    {formatTime(row.date) && (
                                      <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '1px' }}>
                                        {formatTime(row.date)}
                                      </div>
                                    )}
                                  </div>
                                )}
                              </td>
                              <td style={{ padding: '10px 14px', fontWeight: 600 }}>
                                {row.voucher}
                              </td>
                              <td style={{ padding: '10px 14px', color: '#0f172a' }}>
                                {row.voucher === 'Sales Invoices' && row.ref_id ? (
                                  <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                    <a
                                      href={api.orderPdfUrl(row.ref_id)}
                                      target="_blank"
                                      rel="noreferrer"
                                      style={{ color: '#2563eb', textDecoration: 'none', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '3px' }}
                                      title="View Clean Tax Invoice (Original Bill)"
                                    >
                                      <span>{row.sr_no}</span>
                                      <span style={{ fontSize: '11px' }}>↗</span>
                                    </a>
                                    {(row.status === 'paid' || row.status === 'partially_paid') && (
                                      <a
                                        href={api.orderPdfUrl(row.ref_id, true)}
                                        target="_blank"
                                        rel="noreferrer"
                                        style={{
                                          fontSize: '10.5px',
                                          padding: '1px 5px',
                                          borderRadius: '4px',
                                          background: row.status === 'paid' ? '#dcfce7' : '#fef3c7',
                                          color: row.status === 'paid' ? '#166534' : '#92400e',
                                          border: `1px solid ${row.status === 'paid' ? '#bbf7d0' : '#fde68a'}`,
                                          textDecoration: 'none',
                                          fontWeight: 600
                                        }}
                                        title="View Tax Invoice with Payment & Due Status"
                                      >
                                        +Status
                                      </a>
                                    )}
                                  </div>
                                ) : (
                                  row.sr_no || '—'
                                )}
                              </td>
                              <td style={{ padding: '10px 14px', color: '#64748b' }}>
                                {row.payment_mode || '—'}
                              </td>
                              <td style={{ padding: '10px 14px', textAlign: 'right', color: isCredit ? '#16a34a' : '#94a3b8', fontWeight: isCredit ? 600 : 400 }}>
                                {isCredit ? `₹ ${formatINR(row.credit)}` : '—'}
                              </td>
                              <td style={{ padding: '10px 14px', textAlign: 'right', color: isDebit ? '#0f172a' : '#94a3b8', fontWeight: isDebit ? 600 : 400 }}>
                                {isDebit ? `₹ ${formatINR(row.debit)}` : '—'}
                              </td>
                              <td style={{
                                padding: '10px 14px',
                                textAlign: 'right',
                                fontWeight: 700,
                                color: bal !== 0 ? '#0f172a' : '#64748b'
                              }}>
                                {bal !== 0 ? `₹ ${formatINR(Math.abs(bal))}` : '—'}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {/* TAB CONTENT: PROFILE & OPENING BALANCE */}
            {activeTab === 'profile' && (
              <div className="card" style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '20px' }}>
                <h3 style={{ margin: '0 0 16px', fontSize: '15px' }}>Party Profile &amp; Opening Balance</h3>
                <form onSubmit={handleSaveProfile} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                  <div className="field">
                    <label style={{ fontWeight: 600, fontSize: '12.5px', display: 'block', marginBottom: '4px' }}>
                      Opening Balance (₹)
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="any"
                      value={profileForm.opening_balance}
                      onChange={(e) => setProfileForm({ ...profileForm, opening_balance: e.target.value })}
                    />
                  </div>

                  <div className="field">
                    <label style={{ fontWeight: 600, fontSize: '12.5px', display: 'block', marginBottom: '4px' }}>
                      Opening Balance Type
                    </label>
                    <select
                      value={profileForm.opening_balance_type}
                      onChange={(e) => setProfileForm({ ...profileForm, opening_balance_type: e.target.value })}
                    >
                      <option value="to_receive">To Receive (Customer owes you)</option>
                      <option value="to_pay">To Pay (Advance from customer)</option>
                    </select>
                  </div>

                  <div className="field">
                    <label style={{ fontWeight: 600, fontSize: '12.5px', display: 'block', marginBottom: '4px' }}>
                      As of Date
                    </label>
                    <input
                      type="date"
                      value={profileForm.opening_balance_date}
                      onChange={(e) => setProfileForm({ ...profileForm, opening_balance_date: e.target.value })}
                    />
                  </div>

                  <div className="field">
                    <label style={{ fontWeight: 600, fontSize: '12.5px', display: 'block', marginBottom: '4px' }}>
                      Credit Limit (₹)
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="any"
                      value={profileForm.credit_limit}
                      onChange={(e) => setProfileForm({ ...profileForm, credit_limit: e.target.value })}
                    />
                  </div>

                  <div className="field">
                    <label style={{ fontWeight: 600, fontSize: '12.5px', display: 'block', marginBottom: '4px' }}>
                      GSTIN
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. 24AAAAA0000A1Z5"
                      value={profileForm.gstin}
                      onChange={(e) => setProfileForm({ ...profileForm, gstin: e.target.value.toUpperCase() })}
                    />
                  </div>

                  <div className="field">
                    <label style={{ fontWeight: 600, fontSize: '12.5px', display: 'block', marginBottom: '4px' }}>
                      City
                    </label>
                    <input
                      type="text"
                      value={profileForm.city}
                      onChange={(e) => setProfileForm({ ...profileForm, city: e.target.value })}
                    />
                  </div>

                  <div className="field" style={{ gridColumn: 'span 2' }}>
                    <label style={{ fontWeight: 600, fontSize: '12.5px', display: 'block', marginBottom: '4px' }}>
                      Billing Address
                    </label>
                    <textarea
                      rows="2"
                      value={profileForm.billing_address}
                      onChange={(e) => setProfileForm({ ...profileForm, billing_address: e.target.value })}
                    />
                  </div>

                  <div style={{ gridColumn: 'span 2', display: 'flex', justifyContent: 'flex-end' }}>
                    <button
                      type="submit"
                      disabled={savingProfile}
                      style={{ background: '#2563eb', color: '#fff', border: 'none', padding: '8px 18px', borderRadius: '6px', fontWeight: 600 }}
                    >
                      {savingProfile ? 'Saving…' : 'Save Changes'}
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* TAB CONTENT: TRANSACTIONS */}
            {activeTab === 'transactions' && (
              <div className="card" style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '16px' }}>
                <h3 style={{ margin: '0 0 12px', fontSize: '15px' }}>Transactions Overview</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {filteredEntries.filter(e => e.voucher !== 'Opening Balance').map((e, idx) => (
                    <div key={idx} style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '10px 14px',
                      background: '#f8fafc',
                      border: '1px solid #e2e8f0',
                      borderRadius: '6px'
                    }}>
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '13.5px', color: '#0f172a' }}>
                          {e.voucher}: <strong>{e.sr_no}</strong>
                        </div>
                        <div style={{ color: '#64748b', fontSize: '12px', marginTop: '2px' }}>
                          {e.date ? formatDateTime(e.date) : '—'} · {e.particulars}
                        </div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{
                          fontWeight: 800,
                          fontSize: '14px',
                          color: e.credit > 0 ? '#16a34a' : '#0f172a'
                        }}>
                          {e.credit > 0 ? `+ ₹ ${formatINR(e.credit)}` : `- ₹ ${formatINR(e.debit)}`}
                        </div>
                        <div style={{ fontSize: '11px', color: '#64748b' }}>
                          {e.payment_mode !== '—' ? `Mode: ${e.payment_mode}` : 'Sales'}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* MODAL: Payment In */}
      {showPaymentModal && selectedCustomer && (
        <RecordPaymentInModal
          customer={selectedCustomer}
          currentBalance={stats.total_receivable_amount}
          onClose={() => setShowPaymentModal(false)}
          onSuccess={() => {
            loadCustomers();
            loadCustomerLedger(selectedCustomer.id);
          }}
        />
      )}

      {/* MODAL: Create Sales Invoice for this Customer */}
      {showInvoiceModal && selectedCustomer && (
        <div className="modal-backdrop" onClick={() => setShowInvoiceModal(false)}>
          <div className="modal card" style={{ maxWidth: '950px', width: '95vw', maxHeight: '90vh', overflowY: 'auto' }} onClick={(e) => e.stopPropagation()}>
            <InvoiceForm
              initialContact={selectedCustomer}
              onCreated={() => {
                setShowInvoiceModal(false);
                loadCustomers();
                loadCustomerLedger(selectedCustomer.id);
              }}
              onCancel={() => setShowInvoiceModal(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}
