import { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import OrderItemsEditor from './OrderItemsEditor';
import BusinessDetailsFields, { resolveStateCode, formatPlaceOfSupply } from './BusinessDetailsFields';
import { emptyItem, itemsTotal, itemsTax, itemsToPayload } from '../orderItems';

export default function EditInvoiceModal({ order, onClose, onSaved }) {
  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [invoiceDate, setInvoiceDate] = useState('');
  const [orderRef, setOrderRef] = useState('');
  const [notes, setNotes] = useState('');

  // Customer contact state (exact parity with InvoiceForm.jsx)
  const [contacts, setContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [selectedContact, setSelectedContact] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [isCreatingNew, setIsCreatingNew] = useState(false);

  const [phone, setPhone] = useState('');
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [business, setBusiness] = useState({});
  const [shipSame, setShipSame] = useState(true);

  // Line items (exact parity with InvoiceForm.jsx using OrderItemsEditor)
  const [items, setItems] = useState([emptyItem()]);

  // Payment status
  const [paymentStatus, setPaymentStatus] = useState('pending');
  const [paymentMethod, setPaymentMethod] = useState('cash');

  // Business / seller profile for tax mode calculation
  const [sellerProfile, setSellerProfile] = useState(null);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const dropdownRef = useRef(null);

  // Load contacts and seller profile, and initialize from order
  useEffect(() => {
    if (!order) return;

    setInvoiceNumber(order.invoice_number || order.order_number || '');
    setInvoiceDate(
      order.invoice_date
        ? new Date(order.invoice_date).toISOString().split('T')[0]
        : (order.created_at ? new Date(order.created_at).toISOString().split('T')[0] : new Date().toISOString().split('T')[0])
    );
    setOrderRef(order.order_ref || '');
    setNotes(order.notes || '');

    setPhone(order.phone || '');
    setName(order.name || '');
    setCity(order.city || '');
    const initialBiz = {
      gstin: order.gstin || '',
      pan: order.pan || '',
      billing_address: order.billing_address || '',
      shipping_address: order.shipping_address || order.billing_address || '',
      state: order.state || '',
    };
    setBusiness(initialBiz);
    setShipSame(!order.shipping_address || order.shipping_address === order.billing_address);

    const orderItems = Array.isArray(order.items) && order.items.length > 0
      ? order.items.map((i) => ({
          product_name: i.product_name || '',
          quantity: String(i.quantity ?? 1),
          unit_price: String(i.unit_price ?? 0),
          gst_rate: i.gst_rate !== undefined && i.gst_rate !== null ? Number(i.gst_rate) : 18,
          hsn_code: i.hsn_code || '',
        }))
      : [emptyItem()];
    setItems(orderItems);

    setPaymentStatus(order.payment_status || 'pending');
    setPaymentMethod(order.payment_method || 'cash');

    setLoadingContacts(true);
    Promise.all([
      api.listContacts().catch(() => []),
      api.getBusinessProfile().catch(() => null),
    ]).then(([contactList, profile]) => {
      setContacts(contactList || []);
      setSellerProfile(profile);

      // Find matching contact for this order
      const match = (contactList || []).find((c) => c.id === order.contact_id || (order.phone && c.phone === order.phone));
      if (match) {
        setSelectedContact(match);
      } else if (order.name || order.phone) {
        // Pseudo contact object for display card
        setSelectedContact({
          id: order.contact_id,
          name: order.name,
          phone: order.phone,
          city: order.city,
          gstin: order.gstin,
          billing_address: order.billing_address,
          state: order.state,
        });
      }
      setLoadingContacts(false);
    });
  }, [order]);

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

  if (!order) return null;

  // Contact Selection Handlers (identical to InvoiceForm.jsx)
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

  // Credit notes linked safeguard check
  const rawCns = Array.isArray(order.credit_notes_list)
    ? order.credit_notes_list
    : (typeof order.credit_notes_list === 'string'
        ? JSON.parse(order.credit_notes_list || '[]')
        : []);
  const creditedAmount = Number(order.credited_amount || rawCns.reduce((sum, c) => sum + Number(c.total_amount || 0), 0) || 0);

  // Tax and Total calculations (identical to InvoiceForm.jsx)
  const customerGstin = business.gstin || selectedContact?.gstin || '';
  const customerState = business.state || selectedContact?.state || '';
  const sellerState = (sellerProfile?.state || 'Gujarat').trim();
  const sellerCode = resolveStateCode(sellerProfile?.gstin, sellerState) || '24';
  const buyerCode = resolveStateCode(customerGstin, customerState);

  const effectivePlaceOfSupply = formatPlaceOfSupply(customerGstin, customerState) ||
    (customerState ? customerState : `${sellerCode} - ${sellerState}`);

  const isInterState = buyerCode
    ? buyerCode !== sellerCode
    : (customerState ? customerState.trim().toLowerCase() !== sellerState.toLowerCase() : false);
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

  const hasCreditWarning = creditedAmount > 0 && roundedTotal < creditedAmount;

  async function handleSubmit(e) {
    e.preventDefault();
    if (!invoiceNumber.trim()) {
      setError('Invoice number cannot be empty.');
      return;
    }
    const finalPhone = selectedContact ? selectedContact.phone : phone;
    if (!finalPhone || !finalPhone.trim()) {
      setError('A valid customer phone number is required.');
      return;
    }
    if (items.length === 0) {
      setError('At least one line item is required.');
      return;
    }
    for (const it of items) {
      if (!it.product_name || !it.product_name.trim()) {
        setError('All items must have a product name selected.');
        return;
      }
      if (!it.quantity || Number(it.quantity) <= 0) {
        setError(`Item "${it.product_name}" must have a quantity greater than 0.`);
        return;
      }
      if (it.unit_price === '' || isNaN(Number(it.unit_price))) {
        setError(`Item "${it.product_name}" must have a valid unit price.`);
        return;
      }
    }
    if (hasCreditWarning) {
      setError(`New grand total (₹${roundedTotal}) cannot be less than already credited amount (₹${creditedAmount.toFixed(2)}).`);
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const effectiveShipping = shipSame ? business.billing_address : business.shipping_address;
      const payload = {
        invoice_number: invoiceNumber.trim(),
        invoice_date: invoiceDate,
        order_ref: orderRef.trim() || undefined,
        phone: finalPhone.trim(),
        name: (name || selectedContact?.name || '').trim() || undefined,
        city: (city || selectedContact?.city || '').trim() || undefined,
        ...business,
        shipping_address: effectiveShipping,
        place_of_supply: effectivePlaceOfSupply,
        tax_mode: taxMode,
        round_off: roundOff,
        notes: notes.trim() || undefined,
        payment_status: paymentStatus,
        payment_method: paymentStatus === 'paid' ? paymentMethod : undefined,
        items: itemsToPayload(items),
      };

      const updated = await api.editOrder(order.id, payload);
      onSaved?.(updated);
    } catch (err) {
      setError(err.message || 'Failed to update invoice');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(3px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 10000,
        padding: '16px',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget && !saving) onClose();
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '880px',
          maxHeight: '92vh',
          background: 'var(--card-bg, #ffffff)',
          borderRadius: '12px',
          boxShadow: '0 25px 30px -5px rgba(0, 0, 0, 0.25)',
          border: '1px solid var(--border, #cbd5e1)',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          animation: 'fadeIn 0.15s ease-out',
        }}
      >
        {/* MODAL HEADER */}
        <div
          style={{
            padding: '14px 20px',
            borderBottom: '1px solid var(--border, #e2e8f0)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'var(--hover-bg, #f8fafc)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '20px' }}>✏️</span>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.15rem', color: 'var(--text, #0f172a)', fontWeight: 700 }}>
                Edit Tax Invoice
              </h3>
              <div style={{ fontSize: '0.78rem', color: 'var(--muted, #64748b)' }}>
                Consistent MyBillBook workflow — customer party selection, standard items dropdown, pricing, and taxes.
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            style={{
              background: 'transparent',
              border: 'none',
              fontSize: '18px',
              color: 'var(--muted, #64748b)',
              cursor: 'pointer',
              padding: '4px 8px',
              borderRadius: '4px',
            }}
          >
            ✕
          </button>
        </div>

        {/* MODAL SCROLLABLE BODY */}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: 1, overflowY: 'auto' }}>
          <div style={{ padding: '20px', flex: 1 }}>
            {error && (
              <div
                style={{
                  marginBottom: '16px',
                  padding: '10px 14px',
                  background: '#fef2f2',
                  border: '1px solid #fca5a5',
                  borderRadius: '6px',
                  color: '#b91c1c',
                  fontSize: '13px',
                }}
              >
                ⚠️ {error}
              </div>
            )}

            {/* SECTION 1: INVOICE NUMBER, DATE & PO REF */}
            <div className="grid" style={{ marginBottom: '1.25rem' }}>
              <label>
                Invoice # *
                <input
                  required
                  value={invoiceNumber}
                  onChange={(e) => setInvoiceNumber(e.target.value)}
                  placeholder="e.g. INV-0042"
                  style={{ fontWeight: 600, color: 'var(--accent, #2563eb)' }}
                />
              </label>
              <label>
                Invoice Date *
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

            {/* SECTION 2: CUSTOMER / PARTY SELECTION (EXACT PARITY WITH INVOICE FORM) */}
            <div style={{ marginBottom: '1.25rem' }}>
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

                  <div className={`customer-prefill-status ${hasGstOnFile ? 'has-gst' : 'no-gst'}`} style={{ marginBottom: '12px' }}>
                    {hasGstOnFile
                      ? '✓ GST and address details pre-filled from customer profile.'
                      : 'ℹ No GST details on file yet. Enter GST/Address below to update this party.'}
                  </div>
                </>
              ) : isCreatingNew ? (
                <div style={{ marginBottom: '1rem' }}>
                  <div className="card-header" style={{ marginBottom: '0.6rem' }}>
                    <span style={{ fontWeight: 600, color: 'var(--accent, #2563eb)' }}>➕ Entering New Customer Details</span>
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
                    Customer / Party *
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
              <div className="pos-indicator-badge" style={{ marginTop: '0.8rem', marginBottom: '0.8rem' }}>
                <div className="pos-title">
                  <span>📍</span>
                  <span>Place of Supply:</span>
                  <span className="pos-name">{effectivePlaceOfSupply}</span>
                </div>
                <div className={`tax-mode-tag ${isInterState ? 'tag-inter' : 'tag-intra'}`}>
                  {isInterState ? '⚡ Inter-State (IGST Applicable)' : '🏛️ Intra-State (CGST + SGST Applicable)'}
                </div>
              </div>

              {/* BILLING & GST DETAILS */}
              <fieldset>
                <legend>GST / billing details (optional){hasGstOnFile && ' — on file, edit if needed'}</legend>
                <BusinessDetailsFields value={business} onChange={setBusiness} />
              </fieldset>
            </div>

            {/* SECTION 3: ITEMS EDITOR (STANDARD CONSISTENT OrderItemsEditor) */}
            <fieldset style={{ marginBottom: '1.25rem' }}>
              <legend>Items</legend>
              <OrderItemsEditor items={items} onChange={setItems} />

              {/* LIVE STATUTORY SUMMARY CARD (EXACT PARITY WITH INVOICE FORM) */}
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

            {/* SECTION 4: PAYMENT STATUS & REMARKS */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', marginBottom: '16px' }}>
              <div>
                <label>
                  Notes / Terms
                  <textarea
                    rows={2}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Notes visible on invoice PDF…"
                  />
                </label>
              </div>

              {/* PAYMENT ADJUSTMENT */}
              <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '12px 14px' }}>
                <div style={{ fontSize: '12px', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Payment Settlement Status
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flexWrap: 'wrap' }}>
                  <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', cursor: 'pointer' }}>
                    <input
                      type="radio"
                      name="editPaymentStatus"
                      value="paid"
                      checked={paymentStatus === 'paid'}
                      onChange={() => setPaymentStatus('paid')}
                    />
                    <span style={{ fontWeight: 600, color: '#15803d' }}>Paid in Full (₹{roundedTotal.toFixed(2)})</span>
                  </label>

                  <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', cursor: 'pointer' }}>
                    <input
                      type="radio"
                      name="editPaymentStatus"
                      value="pending"
                      checked={paymentStatus === 'pending' || paymentStatus === 'partially_paid'}
                      onChange={() => setPaymentStatus('pending')}
                    />
                    <span style={{ color: '#b45309' }}>Pending / Credit</span>
                  </label>
                </div>

                {paymentStatus === 'paid' && (
                  <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '12px', color: '#64748b' }}>Payment Mode:</span>
                    <select
                      value={paymentMethod}
                      onChange={(e) => setPaymentMethod(e.target.value)}
                      style={{ padding: '3px 8px', fontSize: '12px', borderRadius: '4px', border: '1px solid #cbd5e1' }}
                    >
                      <option value="cash">💵 Cash</option>
                      <option value="upi">⚡ UPI / QR</option>
                      <option value="bank_transfer">🏦 Bank Transfer / NEFT</option>
                      <option value="card">💳 Card</option>
                    </select>
                  </div>
                )}

                {creditedAmount > 0 && (
                  <div style={{ marginTop: '8px', fontSize: '12px', color: '#7c3aed', background: '#f5f3ff', border: '1px solid #ddd6fe', padding: '6px 10px', borderRadius: '4px' }}>
                    ↩ <strong>₹{creditedAmount.toFixed(2)}</strong> already credited via Sales Return. Total cannot be less than this amount.
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* MODAL FOOTER */}
          <div
            style={{
              padding: '14px 20px',
              borderTop: '1px solid var(--border, #e2e8f0)',
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '10px',
              background: '#f8fafc',
            }}
          >
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              style={{
                padding: '8px 16px',
                borderRadius: '6px',
                border: '1px solid #cbd5e1',
                background: '#ffffff',
                color: '#475569',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving || hasCreditWarning}
              style={{
                padding: '8px 20px',
                borderRadius: '6px',
                border: 'none',
                background: '#2563eb',
                color: '#ffffff',
                fontSize: '13px',
                fontWeight: 600,
                cursor: (saving || hasCreditWarning) ? 'not-allowed' : 'pointer',
                opacity: (saving || hasCreditWarning) ? 0.6 : 1,
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              {saving ? 'Saving Changes…' : '✓ Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
