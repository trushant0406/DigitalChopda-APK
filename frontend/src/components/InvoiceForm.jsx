import { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import OrderItemsEditor from './OrderItemsEditor';
import PaymentField from './PaymentField';
import BusinessDetailsFields from './BusinessDetailsFields';
import { emptyItem, itemsTotal, itemsTax, itemsToPayload } from '../orderItems';

export default function InvoiceForm({ onCreated, onCancel }) {
  const [contacts, setContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [selectedContact, setSelectedContact] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [isCreatingNew, setIsCreatingNew] = useState(false);

  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [orderRef, setOrderRef] = useState('');
  const [invoiceDate, setInvoiceDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [phone, setPhone] = useState('');
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState([emptyItem()]);
  const [status, setStatus] = useState('pending');
  const [method, setMethod] = useState('cash');
  const [business, setBusiness] = useState({});
  const [shipSame, setShipSame] = useState(true);
  const [sendWhatsApp, setSendWhatsApp] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const dropdownRef = useRef(null);

  async function loadContacts() {
    setLoadingContacts(true);
    try {
      const list = await api.listContacts();
      setContacts(list || []);
    } catch (e) {
      console.error('Failed to load contacts for invoice:', e);
    } finally {
      setLoadingContacts(false);
    }
  }

  function loadNextNumber() {
    api.getNextInvoiceNumber()
      .then((res) => {
        if (res?.suggestedNumber) setInvoiceNumber(res.suggestedNumber);
      })
      .catch(() => {});
  }

  useEffect(() => {
    loadContacts();
    loadNextNumber();
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleSelectContact(contact) {
    setSelectedContact(contact);
    setName(contact.name || '');
    setPhone(contact.phone || '');
    setCity(contact.city || '');
    setBusiness({
      gstin: contact.gstin || '',
      pan: contact.pan || '',
      billing_address: contact.billing_address || '',
      shipping_address: contact.shipping_address || contact.billing_address || '',
      state: contact.state || '',
    });
    setShipSame(!contact.shipping_address || contact.shipping_address === contact.billing_address);
    setSearchQuery('');
    setDropdownOpen(false);
    setIsCreatingNew(false);
  }

  function handleClearContact() {
    setSelectedContact(null);
    setName('');
    setPhone('');
    setCity('');
    setBusiness({});
    setShipSame(true);
    setIsCreatingNew(false);
    setSearchQuery('');
    setDropdownOpen(true);
  }

  function handleStartNewCustomer() {
    setSelectedContact(null);
    setName(searchQuery ? searchQuery.trim() : '');
    setPhone('');
    setCity('');
    setBusiness({});
    setShipSame(true);
    setIsCreatingNew(true);
    setDropdownOpen(false);
  }

  const filteredContacts = contacts.filter((c) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      (c.name && c.name.toLowerCase().includes(q)) ||
      (c.phone && c.phone.toLowerCase().includes(q)) ||
      (c.city && c.city.toLowerCase().includes(q)) ||
      (c.gstin && c.gstin.toLowerCase().includes(q))
    );
  });

  const hasGstOnFile = Boolean(business.gstin || business.billing_address);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const effectiveShipping = shipSame ? business.billing_address : business.shipping_address;
      const created = await api.createOrder({
        create_invoice: true,
        invoice_number: invoiceNumber.trim() || undefined,
        invoice_date: invoiceDate,
        order_ref: orderRef.trim() || undefined,
        phone: selectedContact ? selectedContact.phone : phone,
        name: name || undefined,
        city: city || undefined,
        notes: notes || undefined,
        items: itemsToPayload(items),
        payment_status: status === 'paid' ? 'paid' : 'pending',
        payment_method: status === 'paid' ? method : status === 'credit' ? 'credit' : undefined,
        ...business,
        shipping_address: effectiveShipping,
      });

      if (sendWhatsApp && created?.id) {
        try {
          const waRes = await api.sendOrderWhatsApp(created.id);
          if (!waRes?.sentViaWeb && waRes?.whatsappUrl) {
            window.open(waRes.whatsappUrl, '_blank');
          }
        } catch (waErr) {
          console.warn('WhatsApp dispatch notice:', waErr.message);
        }
      }

      // Reset form
      setPhone('');
      setName('');
      setCity('');
      setNotes('');
      setOrderRef('');
      setItems([emptyItem()]);
      setStatus('pending');
      setMethod('cash');
      setBusiness({});
      setSelectedContact(null);
      setIsCreatingNew(false);
      loadNextNumber();
      loadContacts();
      onCreated?.(created);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card call-form" onSubmit={handleSubmit}>
      <div className="card-header" style={{ marginBottom: '1rem' }}>
        <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span>🧾</span> New Sales Invoice
        </h2>
        {onCancel && (
          <button type="button" className="button-link" onClick={onCancel}>
            ✕ Close Form
          </button>
        )}
      </div>

      {/* INVOICE NUMBER, DATE & ORDER REF HEADER */}
      <div className="grid">
        <label>
          Invoice #
          <input
            value={invoiceNumber}
            onChange={(e) => setInvoiceNumber(e.target.value)}
            placeholder="Auto-generated (edit to override)"
            style={{ fontWeight: 600, color: 'var(--accent)' }}
          />
        </label>
        <label>
          Invoice Date
          <input
            type="date"
            required
            value={invoiceDate}
            onChange={(e) => setInvoiceDate(e.target.value)}
          />
        </label>
        <label>
          Order / PO Ref (optional)
          <input
            value={orderRef}
            onChange={(e) => setOrderRef(e.target.value)}
            placeholder="e.g. PO-8921 or Quote #10"
          />
        </label>
      </div>

      {/* CUSTOMER SEARCH SECTION (EXACT PARITY WITH QUOTATION SCREEN) */}
      {selectedContact ? (
        <>
          <div className="customer-selected-card">
            <div className="customer-selected-info">
              <div className="customer-selected-avatar">
                {(name || phone || 'C')[0].toUpperCase()}
              </div>
              <div className="customer-selected-details">
                <div className="customer-selected-header">
                  <span className="customer-selected-name">{name || phone}</span>
                  {business.gstin && <span className="badge ok">GST: {business.gstin}</span>}
                  {business.state && <span className="badge neutral">{business.state}</span>}
                </div>
                <div className="customer-selected-meta">
                  <span>📞 {phone}</span>
                  {city && <span>📍 {city}</span>}
                  {business.billing_address && (
                    <span title={business.billing_address}>
                      🏠 {business.billing_address.length > 45 ? `${business.billing_address.slice(0, 45)}…` : business.billing_address}
                    </span>
                  )}
                </div>
              </div>
            </div>
            <button type="button" className="remove-item" onClick={handleClearContact}>
              Change customer
            </button>
          </div>

          <div className={`customer-prefill-status ${hasGstOnFile ? 'has-gst' : 'no-gst'}`}>
            {hasGstOnFile
              ? '✓ GST and address details pre-filled from customer profile. Any updates you make below will be saved automatically for next time.'
              : 'ℹ No GST details on file yet. Enter GST/Address below and they will be automatically saved to this customer for future invoices.'}
          </div>
        </>
      ) : isCreatingNew ? (
        <div style={{ marginBottom: '1rem' }}>
          <div className="card-header" style={{ marginBottom: '0.6rem' }}>
            <span style={{ fontWeight: 600, color: 'var(--accent)' }}>➕ Entering New Customer Details</span>
            <button type="button" className="button-link" onClick={() => setIsCreatingNew(false)}>
              Select existing customer instead
            </button>
          </div>
          <div className="grid">
            <label>
              Phone *
              <input
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+91 98765 43210"
              />
            </label>
            <label>
              Customer / Company name
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Acme Enterprises"
              />
            </label>
            <label>
              City
              <input
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="e.g. Surat"
              />
            </label>
          </div>
        </div>
      ) : (
        <div className="customer-combobox" ref={dropdownRef}>
          <label style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600, fontSize: '0.88rem' }}>
            Customer *
          </label>
          <div className="customer-combobox-input-wrap">
            <span className="customer-combobox-search-icon">🔍</span>
            <input
              type="text"
              className="customer-combobox-input"
              placeholder={loadingContacts ? 'Loading customers…' : 'Select customer or type to search name, phone, city, GST…'}
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setDropdownOpen(true);
              }}
              onFocus={() => setDropdownOpen(true)}
            />
            <button
              type="button"
              className="customer-combobox-toggle-btn"
              onClick={() => setDropdownOpen((prev) => !prev)}
              tabIndex={-1}
            >
              {dropdownOpen ? '▲' : '▼'}
            </button>
          </div>

          {dropdownOpen && (
            <div className="customer-combobox-dropdown">
              <button
                type="button"
                className="customer-option-new"
                onClick={handleStartNewCustomer}
              >
                <span>➕</span>
                <span>
                  Add new customer {searchQuery ? `"${searchQuery.trim()}"` : ''}…
                </span>
              </button>

              <div className="customer-dropdown-section-title">
                Existing Customers ({filteredContacts.length})
              </div>

              {filteredContacts.length === 0 ? (
                <div className="customer-dropdown-empty">
                  No matching customer found.
                  <div style={{ marginTop: '0.4rem' }}>
                    <button type="button" className="secondary-action" onClick={handleStartNewCustomer}>
                      + Create "{searchQuery}" as new
                    </button>
                  </div>
                </div>
              ) : (
                filteredContacts.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    className="customer-option-item"
                    onClick={() => handleSelectContact(c)}
                  >
                    <div className="customer-option-primary">
                      <span className="customer-option-name">
                        {c.name || 'Unnamed'}
                        {c.city ? ` · ${c.city}` : ''}
                      </span>
                      <span className="customer-option-phone">📞 {c.phone}</span>
                    </div>
                    <div className="customer-option-badges">
                      {c.gstin ? (
                        <span className="badge ok" title={`GSTIN: ${c.gstin}`}>
                          GST: {c.gstin.slice(0, 5)}…
                        </span>
                      ) : null}
                      {c.state ? <span className="badge neutral">{c.state}</span> : null}
                    </div>
                  </button>
                ))
              )}
            </div>
          )}
        </div>
      )}

      {/* ITEMS EDITOR */}
      <fieldset>
        <legend>Items</legend>
        <OrderItemsEditor items={items} onChange={setItems} />
        <p className="order-total">
          Total: ₹{itemsTotal(items).toFixed(2)}
          {itemsTax(items) > 0 && (
            <span className="hint">
              {' '}(incl. ₹{itemsTax(items).toFixed(2)} GST = ₹{(itemsTotal(items) + itemsTax(items)).toFixed(2)})
            </span>
          )}
        </p>
      </fieldset>

      {/* PAYMENT DETAILS */}
      <PaymentField status={status} setStatus={setStatus} method={method} setMethod={setMethod} />

      {/* BILLING & GST DETAILS */}
      <fieldset>
        <legend>GST / billing details (optional){hasGstOnFile && ' — on file, edit if needed'}</legend>
        <BusinessDetailsFields value={business} onChange={setBusiness} />
      </fieldset>

      {/* NOTES & WHATSAPP TOGGLE */}
      <div className="grid" style={{ alignItems: 'center' }}>
        <label>
          Notes
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} placeholder="Optional notes on invoice…" />
        </label>
        <div style={{ padding: '0.75rem', background: 'var(--bg-secondary)', borderRadius: '8px', border: '1px solid var(--border)' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', cursor: 'pointer', fontWeight: 600 }}>
            <input
              type="checkbox"
              checked={sendWhatsApp}
              onChange={(e) => setSendWhatsApp(e.target.checked)}
              style={{ width: '18px', height: '18px', cursor: 'pointer' }}
            />
            <span>Send Tax Invoice via WhatsApp 💬</span>
          </label>
          <div style={{ fontSize: '0.78rem', color: 'var(--muted)', marginTop: '0.3rem' }}>
            Sends official PDF attachment directly to customer on WhatsApp.
          </div>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
        {onCancel && (
          <button type="button" className="secondary-action" onClick={onCancel} disabled={saving}>
            Cancel
          </button>
        )}
        <button type="submit" disabled={saving}>
          {saving ? 'Creating invoice…' : '✓ Create Tax Invoice'}
        </button>
      </div>
    </form>
  );
}
