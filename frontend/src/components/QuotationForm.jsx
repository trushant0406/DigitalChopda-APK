import { useEffect, useState, useRef } from 'react';
import { api } from '../api';
import QuotationItemsEditor from './QuotationItemsEditor';
import BusinessDetailsFields, { resolveStateCode, formatPlaceOfSupply } from './BusinessDetailsFields';
import { emptyQuotationItem, quotationTotals } from '../quotationItems';

export default function QuotationForm({ onCreated, presetContact }) {
  const [contacts, setContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [selectedContact, setSelectedContact] = useState(presetContact || null);
  const [searchQuery, setSearchQuery] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [isCreatingNew, setIsCreatingNew] = useState(false);
  const [sellerProfile, setSellerProfile] = useState(null);

  const [phone, setPhone] = useState(presetContact?.phone || '');
  const [name, setName] = useState(presetContact?.name || '');
  const [city, setCity] = useState(presetContact?.city || '');
  const [business, setBusiness] = useState(
    presetContact
      ? {
          gstin: presetContact.gstin || '',
          pan: presetContact.pan || '',
          billing_address: presetContact.billing_address || '',
          shipping_address: presetContact.shipping_address || presetContact.billing_address || '',
          state: presetContact.state || '',
        }
      : {}
  );
  const [shipSame, setShipSame] = useState(true);
  const [items, setItems] = useState([emptyQuotationItem()]);
  const [terms, setTerms] = useState('Thanks');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const dropdownRef = useRef(null);

  async function loadContacts() {
    setLoadingContacts(true);
    try {
      const list = await api.listContacts();
      setContacts(list || []);
    } catch (e) {
      console.error('Failed to load contacts for dropdown:', e);
    } finally {
      setLoadingContacts(false);
    }
  }

  useEffect(() => {
    loadContacts();
    api.getBusinessProfile().then((p) => {
      setSellerProfile(p);
      if (p?.terms_and_conditions) setTerms(p.terms_and_conditions);
    }).catch(() => {});
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

  const totals = quotationTotals(items);

  // Filter contacts based on search query
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

  const rawSubtotal = totals.subtotal;
  const rawTax = totals.taxAmount;
  const rawTotal = totals.total;
  const roundedTotal = Math.round(rawTotal);
  const roundOff = Number((roundedTotal - rawTotal).toFixed(2));

  const taxByRate = items.reduce((acc, item) => {
    const qty = Number(item.quantity || 0);
    const rate = Number(item.rate || 0);
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
      const created = await api.createQuotation({
        contact_id: selectedContact?.id,
        phone: selectedContact ? selectedContact.phone : phone,
        name: name || undefined,
        city: city || undefined,
        gstin: business.gstin || undefined,
        pan: business.pan || undefined,
        billing_address: business.billing_address || undefined,
        shipping_address: effectiveShipping || undefined,
        state: business.state || undefined,
        place_of_supply: effectivePlaceOfSupply,
        tax_mode: taxMode,
        ship_to_name: name || undefined,
        ship_to_address: effectiveShipping || undefined,
        items: items.map((i) => ({
          product_name: i.product_name,
          hsn_code: i.hsn_code || undefined,
          unit: i.unit,
          quantity: Number(i.quantity),
          rate: Number(i.rate),
          gst_rate: Number(i.gst_rate),
        })),
        terms_and_conditions: terms,
      });

      // Refresh contacts cache so any updated GST/address details are immediately available next time
      loadContacts();

      setItems([emptyQuotationItem()]);
      handleClearContact();
      onCreated(created);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card call-form" onSubmit={handleSubmit}>
      <h2>New quotation</h2>

      {/* CUSTOMER SELECTION SECTION */}
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
              : 'ℹ No GST details on file yet. Enter GST/Address below and they will be automatically saved to this customer for future quotes.'}
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

      {/* BILL TO & GST DETAILS */}
      <fieldset>
        <legend>Bill to / GST details (Auto-saved on quotation)</legend>
        <BusinessDetailsFields value={business} onChange={setBusiness} />
      </fieldset>

      {/* SHIPPING ADDRESS */}
      <label className="hint" style={{ display: 'flex', alignItems: 'center', marginBottom: '0.8rem', cursor: 'pointer' }}>
        <input
          type="checkbox"
          checked={shipSame}
          onChange={(e) => setShipSame(e.target.checked)}
          style={{ width: 'auto', marginRight: '0.5rem' }}
        />
        Ship to same address as billing
      </label>
      {!shipSame && (
        <label style={{ marginBottom: '1rem', display: 'block' }}>
          Shipping address
          <textarea
            value={business.shipping_address || ''}
            onChange={(e) => setBusiness((prev) => ({ ...prev, shipping_address: e.target.value }))}
            rows={2}
            placeholder="Enter custom shipping address if different from billing"
          />
        </label>
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
        <QuotationItemsEditor items={items} onChange={setItems} />
        
        <div className="invoice-live-summary-card">
          <div className="invoice-live-summary-title">
            Statutory GST Quotation Summary ({taxMode === 'split' ? 'Intra-State: CGST + SGST' : 'Inter-State: IGST'})
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

      {/* TERMS & CONDITIONS */}
      <label>
        Terms &amp; conditions
        <textarea value={terms} onChange={(e) => setTerms(e.target.value)} rows={2} />
      </label>

      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={saving || (!selectedContact && !phone)}>
        {saving ? 'Creating…' : 'Create quotation'}
      </button>
    </form>
  );
}
