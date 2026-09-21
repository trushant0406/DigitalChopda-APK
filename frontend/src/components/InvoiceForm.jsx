import { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import OrderItemsEditor from './OrderItemsEditor';
import PaymentField from './PaymentField';
import BusinessDetailsFields, { resolveStateCode, formatPlaceOfSupply } from './BusinessDetailsFields';
import { emptyItem, itemsTotal, itemsTax, itemsToPayload } from '../orderItems';

export default function InvoiceForm({ initialContact, onCreated, onCancel }) {
  const [contacts, setContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [selectedContact, setSelectedContact] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [isCreatingNew, setIsCreatingNew] = useState(false);
  const [sellerProfile, setSellerProfile] = useState(null);

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
      if (initialContact) {
        const match = list.find((c) => c.id === initialContact.id) || initialContact;
        handleSelectContact(match);
      }
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
    api.getBusinessProfile().then((p) => setSellerProfile(p)).catch(() => {});
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

  const customerGstin = business.gstin || selectedContact?.gstin || '';
  const customerState = business.state || selectedContact?.state || '';
  const sellerCode = resolveStateCode(sellerProfile?.gstin, sellerProfile?.state) || '24';
  const buyerCode = resolveStateCode(customerGstin, customerState);

  const effectivePlaceOfSupply = formatPlaceOfSupply(customerGstin, customerState) ||
    (customerState ? customerState : '24 - Gujarat');

  const isInterState = buyerCode
    ? buyerCode !== sellerCode
    : (customerState ? customerState.trim().toLowerCase() !== (sellerProfile?.state || 'gujarat').toLowerCase() : false);
  const taxMode = isInterState ? 'igst' : 'split';

  const rawSubtotal = itemsTotal(items);
  const rawTax = itemsTax(items);
  const rawTotal = rawSubtotal + rawTax;
  const roundedTotal = Math.round(rawTotal);
  const roundOff = Number((roundedTotal - rawTotal).toFixed(2));

  const taxByRate = items.reduce((acc, item) => {
    const qty = Number(item.quantity || 0);
    const rate = Number(item.unit_price || 0);
    const gstRate = Number(item.gst_rate || 0);
    const taxable = qty * rate;
    const tax = taxable * (gstRate / 100);
    if (tax > 0) {
      if (!acc[gstRate]) acc[gstRate] = 0;
      acc[gstRate] += tax;
    }
    return acc;
  }, {});

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
        place_of_supply: effectivePlaceOfSupply,
        tax_mode: taxMode,
        round_off: roundOff,
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

      {/* PLACE OF SUPPLY & STATUTORY TAX MODE INDICATOR */}
      <div className="pos-indicator-badge">
        <div className="pos-title">
          <span>📍</span>
          <span>Place of Supply:</span>
          <span className="pos-name">{effectivePlaceOfSupply}</span>
        </div>
        <div className={`tax-mode-tag ${isInterState ? 'tag-inter' : 'tag-intra'}`}>
          {isInterState ? '⚡ Inter-State (IGST Applicable)' : '🏛️ Intra-State (CGST + SGST Applicable)'}
        </div>
      </div>

      {/* ITEMS EDITOR */}
      <fieldset>
        <legend>Items</legend>
        <OrderItemsEditor items={items} onChange={setItems} />
        
        <div className="invoice-live-summary-card">
          <div className="invoice-live-summary-title">
            Statutory GST Invoice Summary ({taxMode === 'split' ? 'Intra-State: CGST + SGST' : 'Inter-State: IGST'})
          </div>
          <div className="invoice-summary-row">
            <span>Taxable Subtotal</span>
            <strong>₹{rawSubtotal.toFixed(2)}</strong>
          </div>
          {Object.entries(taxByRate).length === 0 ? (
            <div className="invoice-summary-row muted">
              <span>{taxMode === 'split' ? 'CGST + SGST (0%)' : 'IGST (0%)'}</span>
              <span>₹0.00</span>
            </div>
          ) : (
            Object.entries(taxByRate).map(([rateStr, tax]) => {
              const rateNum = Number(rateStr);
              if (taxMode === 'split') {
                const halfRate = (rateNum / 2).toFixed(Number.isInteger(rateNum / 2) ? 0 : 1);
                const halfTax = (tax / 2).toFixed(2);
                return (
                  <div key={rateStr}>
                    <div className="invoice-summary-row">
                      <span>CGST @ {halfRate}%</span>
                      <span>₹{halfTax}</span>
                    </div>
                    <div className="invoice-summary-row">
                      <span>SGST @ {halfRate}%</span>
                      <span>₹{halfTax}</span>
                    </div>
                  </div>
                );
              } else {
                return (
                  <div key={rateStr} className="invoice-summary-row">
                    <span>IGST @ {rateNum}%</span>
                    <span>₹{tax.toFixed(2)}</span>
                  </div>
                );
              }
            })
          )}
          {Math.abs(roundOff) >= 0.005 && (
            <div className="invoice-summary-row round-off">
              <span>Round Off (Auto Cash)</span>
              <span>{roundOff >= 0 ? `+₹${roundOff.toFixed(2)}` : `-₹${Math.abs(roundOff).toFixed(2)}`}</span>
            </div>
          )}
          <div className="invoice-summary-total-row">
            <span>Grand Total (Payable)</span>
            <span className="total-val">₹{roundedTotal.toFixed(2)}</span>
          </div>
        </div>
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
