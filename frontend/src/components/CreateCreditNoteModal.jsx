import { useState, useEffect, useRef } from 'react';
import { api } from '../api';

function formatINR(amount) {
  return new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
    Number(amount || 0)
  );
}

const REASON_OPTIONS = [
  'Sales Return',
  'Damaged / Defective Goods',
  'Incorrect Item Delivered',
  'Post-Sale Discount / Rate Difference',
  'Order Cancellation / Partial Refund',
  'Quality Not Satisfactory',
  'Other',
];

export default function CreateCreditNoteModal({
  customer = null,
  initialCustomer = null,
  initialContactId = null,
  initialOrder = null,
  onClose,
  onSuccess,
}) {
  const initialCust = customer || initialCustomer || null;
  const [selectedCustomer, setSelectedCustomer] = useState(initialCust);
  const [allCustomers, setAllCustomers] = useState([]);
  const [customerSearch, setCustomerSearch] = useState('');
  const [loadingCustomers, setLoadingCustomers] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  const [creditNoteNumber, setCreditNoteNumber] = useState('');
  const [creditNoteDate, setCreditNoteDate] = useState(() => {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  });
  const [reason, setReason] = useState('Sales Return');
  const [notes, setNotes] = useState('');
  const [sendWhatsApp, setSendWhatsApp] = useState(Boolean((customer || initialCustomer)?.phone));

  // Orders available for this customer
  const [customerOrders, setCustomerOrders] = useState([]);
  const [selectedOrderId, setSelectedOrderId] = useState(initialOrder?.id || '');
  const [selectedOrder, setSelectedOrder] = useState(initialOrder);
  const [orderReturnDetails, setOrderReturnDetails] = useState(null);

  // Return items list: { product_name, quantity, maxQuantity, billed_quantity, already_returned_quantity, unit_price, gst_rate, hsn_code, selected, disabled }
  const [items, setItems] = useState([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [loadingItems, setLoadingItems] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Close customer dropdown on outside click
  useEffect(() => {
    function handleClickOutside(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Load all customers for selection or ID matching
  useEffect(() => {
    if (!customer && !initialCustomer) {
      setLoadingCustomers(true);
      api.listContacts()
        .then((list) => {
          setAllCustomers(list || []);
        })
        .catch((err) => console.error('Failed to load contacts for credit note:', err))
        .finally(() => setLoadingCustomers(false));
    }
  }, [customer, initialCustomer]);

  useEffect(() => {
    const c = customer || initialCustomer;
    if (c) {
      setSelectedCustomer(c);
      setSendWhatsApp(Boolean(c?.phone));
    }
  }, [customer, initialCustomer]);

  useEffect(() => {
    if (!selectedCustomer && initialContactId && allCustomers.length > 0) {
      const match = allCustomers.find((cust) => String(cust.id) === String(initialContactId));
      if (match) {
        setSelectedCustomer(match);
        setSendWhatsApp(Boolean(match?.phone));
      }
    }
  }, [initialContactId, allCustomers, selectedCustomer]);

  useEffect(() => {
    api.getNextCreditNoteNumber()
      .then((res) => {
        if (res?.suggestedNumber) setCreditNoteNumber(res.suggestedNumber);
      })
      .catch((e) => console.error('Failed to get next credit note number:', e));
  }, []);

  // Fetch orders for the selected customer
  useEffect(() => {
    if (selectedCustomer?.id) {
      setLoadingOrders(true);
      api.listCreditOrders(selectedCustomer.id)
        .then((ords) => {
          setCustomerOrders(ords || []);
          if (!selectedOrderId && ords && ords.length > 0) {
            // Pick first non-fully-returned order if available
            const nonReturned = ords.find((o) => !o.is_fully_returned) || ords[0];
            setSelectedOrderId(nonReturned.id);
            setSelectedOrder(nonReturned);
          }
        })
        .catch((e) => console.error('Failed to load customer orders:', e))
        .finally(() => setLoadingOrders(false));
    } else {
      setCustomerOrders([]);
      setSelectedOrderId('');
      setSelectedOrder(null);
      setItems([]);
    }
  }, [selectedCustomer?.id]);

  // Load returnable items for the selected order (tracking billed vs returned quantities)
  useEffect(() => {
    if (selectedOrderId) {
      loadReturnableItems(selectedOrderId);
    } else {
      setOrderReturnDetails(null);
      setItems([
        {
          product_name: '',
          quantity: 1,
          maxQuantity: 9999,
          billed_quantity: 0,
          already_returned_quantity: 0,
          unit_price: 0,
          gst_rate: 0,
          hsn_code: '',
          selected: true,
          disabled: false,
        },
      ]);
    }
  }, [selectedOrderId]);

  async function loadReturnableItems(ordId) {
    setLoadingItems(true);
    setError(null);
    try {
      const data = await api.getReturnableOrderItems(ordId);
      setOrderReturnDetails(data);

      if (data?.items && data.items.length > 0) {
        setItems(
          data.items.map((it) => {
            const rem = Number(it.remaining_returnable_quantity ?? it.quantity);
            const isFinished = rem <= 0;
            return {
              product_name: it.product_name,
              quantity: isFinished ? 0 : rem,
              maxQuantity: rem > 0 ? rem : 0,
              billed_quantity: Number(it.billed_quantity || it.quantity || 0),
              already_returned_quantity: Number(it.already_returned_quantity || 0),
              unit_price: Number(it.unit_price || 0),
              gst_rate: Number(it.gst_rate || 0),
              hsn_code: it.hsn_code || '',
              selected: !isFinished,
              disabled: isFinished,
            };
          })
        );
      } else {
        setItems([
          {
            product_name: '',
            quantity: 1,
            maxQuantity: 9999,
            billed_quantity: 0,
            already_returned_quantity: 0,
            unit_price: 0,
            gst_rate: 0,
            hsn_code: '',
            selected: true,
            disabled: false,
          },
        ]);
      }
    } catch (err) {
      console.error('Failed to load returnable items for order:', err);
      // Fallback to basic order items
      const ord = customerOrders.find((o) => String(o.id) === String(ordId));
      if (ord) {
        const rawItems = Array.isArray(ord.items)
          ? ord.items
          : (typeof ord.items === 'string' ? JSON.parse(ord.items || '[]') : []);
        setItems(
          rawItems.map((it) => ({
            product_name: it.product_name,
            quantity: Number(it.quantity || 1),
            maxQuantity: Number(it.quantity || 1),
            billed_quantity: Number(it.quantity || 1),
            already_returned_quantity: 0,
            unit_price: Number(it.unit_price || 0),
            gst_rate: Number(it.gst_rate || 0),
            hsn_code: it.hsn_code || '',
            selected: true,
            disabled: false,
          }))
        );
      }
    } finally {
      setLoadingItems(false);
    }
  }

  function handleOrderChange(ordId) {
    setSelectedOrderId(ordId);
    const ord = customerOrders.find((o) => String(o.id) === String(ordId));
    setSelectedOrder(ord || null);
  }

  function handleItemToggle(index) {
    setItems((prev) =>
      prev.map((it, idx) => (idx === index && !it.disabled ? { ...it, selected: !it.selected } : it))
    );
  }

  function handleItemChange(index, field, value) {
    setItems((prev) =>
      prev.map((it, idx) => {
        if (idx !== index) return it;
        if (field === 'quantity') {
          const num = Number(value);
          if (it.maxQuantity && num > it.maxQuantity) {
            return { ...it, quantity: it.maxQuantity };
          }
        }
        return { ...it, [field]: value };
      })
    );
  }

  function handleAddItem() {
    setItems((prev) => [
      ...prev,
      {
        product_name: '',
        quantity: 1,
        maxQuantity: 9999,
        billed_quantity: 0,
        already_returned_quantity: 0,
        unit_price: 0,
        gst_rate: 0,
        hsn_code: '',
        selected: true,
        disabled: false,
      },
    ]);
  }

  function handleRemoveItem(index) {
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  }

  // Filtered customer list for instant search
  const filteredCustomers = allCustomers.filter((c) => {
    if (!customerSearch) return true;
    const q = customerSearch.toLowerCase();
    return (
      (c.name && c.name.toLowerCase().includes(q)) ||
      (c.phone && c.phone.toLowerCase().includes(q)) ||
      (c.city && c.city.toLowerCase().includes(q)) ||
      (c.gstin && c.gstin.toLowerCase().includes(q))
    );
  });

  // Calculate totals for selected active items
  const activeItems = items.filter((it) => it.selected && it.product_name.trim() && Number(it.quantity) > 0);
  const returnSubtotal = activeItems.reduce(
    (sum, it) => sum + Number(it.quantity || 0) * Number(it.unit_price || 0),
    0
  );
  const returnTax = activeItems.reduce(
    (sum, it) => sum + Number(it.quantity || 0) * Number(it.unit_price || 0) * (Number(it.gst_rate || 0) / 100),
    0
  );
  const returnTotal = returnSubtotal + returnTax;

  const isOrderFullyReturned = orderReturnDetails?.is_fully_returned || false;

  async function handleSubmit(e) {
    e.preventDefault();
    if (!selectedCustomer?.id) {
      setError('Please select a customer for this sales return');
      return;
    }

    if (isOrderFullyReturned) {
      setError('This invoice has already been fully returned. You cannot issue another credit note against it.');
      return;
    }

    if (activeItems.length === 0) {
      setError('Please select at least one item with a quantity > 0 to return');
      return;
    }

    for (const it of activeItems) {
      if (!it.product_name.trim()) {
        setError('All selected items must have a product name');
        return;
      }
      if (Number(it.quantity) <= 0) {
        setError(`Quantity for "${it.product_name}" must be greater than 0`);
        return;
      }
      if (it.maxQuantity && Number(it.quantity) > it.maxQuantity + 0.001) {
        setError(`Cannot return ${it.quantity} of "${it.product_name}". Only ${it.maxQuantity} remaining returnable.`);
        return;
      }
    }

    setSaving(true);
    setError(null);

    try {
      const payload = {
        contact_id: selectedCustomer.id,
        order_id: selectedOrderId ? Number(selectedOrderId) : null,
        credit_note_number: creditNoteNumber.trim() || undefined,
        credit_note_date: creditNoteDate,
        reason,
        notes: notes.trim() || undefined,
        items: activeItems.map((it) => ({
          product_name: it.product_name,
          quantity: Number(it.quantity),
          unit_price: Number(it.unit_price),
          gst_rate: Number(it.gst_rate || 0),
          hsn_code: it.hsn_code || undefined,
        })),
      };

      const res = await api.createCreditNote(payload);

      if (sendWhatsApp && res?.id) {
        try {
          const waRes = await api.sendCreditNoteWhatsApp(res.id);
          if (!waRes?.sentViaWeb && waRes?.whatsappUrl) {
            window.open(waRes.whatsappUrl, '_blank');
          }
        } catch (waErr) {
          console.warn('WhatsApp dispatch warning:', waErr.message);
        }
      }

      onSuccess(res);
    } catch (err) {
      setError(err.message || 'Failed to create credit note');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-dialog-enterprise"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Sticky Enterprise Header */}
        <div className="modal-dialog-header">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <span style={{ fontSize: '1.25rem' }}>↩</span>
              <h2 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700 }}>
                Issue Credit Note / Sales Return
              </h2>
            </div>
            <p className="subtitle" style={{ margin: '4px 0 0', fontSize: '0.84rem' }}>
              Credits customer account passbook and adjusts open invoice receivables.
            </p>
          </div>
          <button
            type="button"
            className="modal-dialog-close-btn"
            onClick={onClose}
            title="Close"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
          {/* Scrollable Form Body */}
          <div className="modal-dialog-body">
            {error && (
              <div style={{
                background: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: '8px',
                padding: '10px 14px',
                color: '#991b1b',
                fontSize: '0.88rem',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}>
                <span>⚠️</span>
                <span>{error}</span>
              </div>
            )}

            {/* CUSTOMER SELECTION (Exact Parity with New Sales Invoice) */}
            <div>
              <label style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600, fontSize: '0.88rem' }}>
                Customer *
              </label>

              {selectedCustomer ? (
                <div className="customer-selected-card" style={{ margin: 0 }}>
                  <div className="customer-selected-info">
                    <div className="customer-selected-avatar">
                      {(selectedCustomer.name || selectedCustomer.phone || 'C')[0].toUpperCase()}
                    </div>
                    <div className="customer-selected-details">
                      <div className="customer-selected-header">
                        <span className="customer-selected-name">{selectedCustomer.name || selectedCustomer.phone}</span>
                        {selectedCustomer.gstin && <span className="badge ok">GST: {selectedCustomer.gstin}</span>}
                        {selectedCustomer.state && <span className="badge neutral">{selectedCustomer.state}</span>}
                        {Number(selectedCustomer.outstanding || 0) > 0.01 && (
                          <span className="badge warning">Due: ₹{formatINR(selectedCustomer.outstanding)}</span>
                        )}
                      </div>
                      <div className="customer-selected-meta">
                        <span>📞 {selectedCustomer.phone || 'No phone'}</span>
                        {selectedCustomer.city && <span>📍 {selectedCustomer.city}</span>}
                        {selectedCustomer.billing_address && (
                          <span title={selectedCustomer.billing_address}>
                            🏠 {selectedCustomer.billing_address.length > 45 ? `${selectedCustomer.billing_address.slice(0, 45)}…` : selectedCustomer.billing_address}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  {!customer && (
                    <button
                      type="button"
                      className="remove-item"
                      onClick={() => {
                        setSelectedCustomer(null);
                        setCustomerSearch('');
                        setDropdownOpen(true);
                        setSelectedOrderId('');
                        setCustomerOrders([]);
                        setItems([]);
                      }}
                    >
                      Change customer
                    </button>
                  )}
                </div>
              ) : (
                <div className="customer-combobox" ref={dropdownRef} style={{ margin: 0 }}>
                  <div className="customer-combobox-input-wrap">
                    <span className="customer-combobox-search-icon">🔍</span>
                    <input
                      type="text"
                      className="customer-combobox-input"
                      placeholder={loadingCustomers ? 'Loading customers…' : 'Select customer or type to search name, phone, city, GST…'}
                      value={customerSearch}
                      onChange={(e) => {
                        setCustomerSearch(e.target.value);
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
                      <div className="customer-dropdown-section-title">
                        Existing Customers ({filteredCustomers.length})
                      </div>
                      {filteredCustomers.length === 0 ? (
                        <div className="customer-dropdown-empty">
                          No matching customer found.
                        </div>
                      ) : (
                        filteredCustomers.map((c) => (
                          <button
                            key={c.id}
                            type="button"
                            className="customer-option-item"
                            onClick={() => {
                              setSelectedCustomer(c);
                              setSendWhatsApp(Boolean(c.phone));
                              setDropdownOpen(false);
                              setCustomerSearch('');
                            }}
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
            </div>

            {/* Symmetrical 4-Field Metadata Grid */}
            <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', margin: 0, gap: '1rem' }}>
              <label style={{ margin: 0 }}>
                Original Invoice Reference
                <select
                  value={selectedOrderId}
                  onChange={(e) => handleOrderChange(e.target.value)}
                  disabled={loadingOrders || !selectedCustomer}
                >
                  <option value="">— Standalone Credit Note (No Invoice) —</option>
                  {customerOrders.map((ord) => {
                    const invNum = ord.invoice_number || ord.order_number;
                    const invDate = ord.invoice_date ? String(ord.invoice_date).slice(0, 10) : '';
                    const isFullyRet = ord.is_fully_returned;
                    const label = `${invNum} (${invDate ? `${invDate} · ` : ''}₹${formatINR(ord.grand_total)}${isFullyRet ? ' — FULLY RETURNED ⛔' : ''})`;
                    return (
                      <option key={ord.id} value={ord.id} disabled={isFullyRet} style={{ color: isFullyRet ? '#94a3b8' : 'inherit' }}>
                        {label}
                      </option>
                    );
                  })}
                </select>
              </label>

              <label style={{ margin: 0 }}>
                Credit Note #
                <input
                  type="text"
                  value={creditNoteNumber}
                  onChange={(e) => setCreditNoteNumber(e.target.value)}
                  placeholder="CN-0001"
                  style={{ fontWeight: 600, color: 'var(--accent)' }}
                />
              </label>

              <label style={{ margin: 0 }}>
                Credit Note Date *
                <input
                  type="date"
                  required
                  value={creditNoteDate}
                  onChange={(e) => setCreditNoteDate(e.target.value)}
                />
              </label>

              <label style={{ margin: 0 }}>
                Reason for Return *
                <select
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                >
                  {REASON_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
              </label>
            </div>

            {/* FULLY RETURNED NOTICE BANNER */}
            {isOrderFullyReturned && (
              <div style={{
                background: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: '8px',
                padding: '12px 16px',
                color: '#991b1b',
                fontSize: '0.88rem',
                display: 'flex',
                alignItems: 'center',
                gap: '12px'
              }}>
                <span style={{ fontSize: '1.3rem' }}>⛔</span>
                <div>
                  <strong style={{ display: 'block', fontSize: '0.92rem' }}>Notice: This invoice has already been 100% returned.</strong>
                  <span style={{ fontSize: '0.82rem', color: '#b91c1c' }}>
                    All line items were previously credited in full. To prevent duplicate ledger credits, no further credit notes can be issued against this invoice.
                  </span>
                </div>
              </div>
            )}

            {/* Editable Return Items Table Card */}
            <div className="credit-note-items-card">
              <div className="credit-note-items-card-header">
                <div>
                  <strong style={{ fontSize: '0.95rem' }}>Items to Return / Credit</strong>
                  <span className="hint" style={{ marginLeft: '8px', fontSize: '0.82rem' }}>
                    ({activeItems.length} selected for credit)
                  </span>
                </div>
                <button
                  type="button"
                  onClick={handleAddItem}
                  className="secondary-action"
                  style={{ fontSize: '0.82rem', padding: '0.35rem 0.8rem', margin: 0 }}
                >
                  + Add Item
                </button>
              </div>

              {loadingItems ? (
                <div style={{ padding: '24px', textAlign: 'center', color: 'var(--muted)', fontSize: '0.88rem' }}>
                  Loading invoice item return status…
                </div>
              ) : (
                <div className="credit-note-table-wrap">
                  <table className="credit-note-table">
                    <thead>
                      <tr>
                        <th style={{ width: '36px', textAlign: 'center' }}>✓</th>
                        <th>Item Description</th>
                        <th style={{ width: '70px', textAlign: 'center' }}>Billed</th>
                        <th style={{ width: '75px', textAlign: 'center' }}>Returned</th>
                        <th style={{ width: '95px', textAlign: 'right' }}>Return Qty</th>
                        <th style={{ width: '105px', textAlign: 'right' }}>Price (₹)</th>
                        <th style={{ width: '80px', textAlign: 'right' }}>GST %</th>
                        <th style={{ width: '110px', textAlign: 'right' }}>Total (₹)</th>
                        <th style={{ width: '36px', textAlign: 'center' }}></th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.map((it, idx) => {
                        const lineSub = (Number(it.quantity) || 0) * (Number(it.unit_price) || 0);
                        const lineTax = lineSub * ((Number(it.gst_rate) || 0) / 100);
                        const lineTotal = lineSub + lineTax;
                        const isFinished = it.disabled || (it.maxQuantity !== undefined && it.maxQuantity <= 0 && it.billed_quantity > 0);

                        return (
                          <tr
                            key={idx}
                            style={{
                              background: isFinished ? 'var(--hover-bg, #f8fafc)' : (it.selected ? 'transparent' : 'rgba(0,0,0,0.02)'),
                              opacity: isFinished ? 0.65 : (it.selected ? 1 : 0.65),
                            }}
                          >
                            <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>
                              <input
                                type="checkbox"
                                checked={it.selected && !isFinished}
                                disabled={isFinished}
                                onChange={() => handleItemToggle(idx)}
                                style={{ width: '16px', height: '16px', cursor: isFinished ? 'not-allowed' : 'pointer', margin: 0 }}
                              />
                            </td>
                            <td style={{ verticalAlign: 'middle' }}>
                              <input
                                type="text"
                                value={it.product_name}
                                onChange={(e) => handleItemChange(idx, 'product_name', e.target.value)}
                                placeholder="Product / Service name"
                                disabled={!it.selected || isFinished}
                                style={{ width: '100%', fontSize: '0.85rem' }}
                              />
                              {isFinished && (
                                <span style={{ fontSize: '0.72rem', color: '#15803d', fontWeight: 600, display: 'block', marginTop: '2px' }}>
                                  ✓ 100% Already Returned ({it.already_returned_quantity} / {it.billed_quantity})
                                </span>
                              )}
                            </td>
                            <td style={{ textAlign: 'center', verticalAlign: 'middle', color: 'var(--muted)', fontWeight: 500 }}>
                              {it.billed_quantity > 0 ? it.billed_quantity : '—'}
                            </td>
                            <td style={{ textAlign: 'center', verticalAlign: 'middle', color: it.already_returned_quantity > 0 ? 'var(--accent)' : 'var(--muted)', fontWeight: 600 }}>
                              {it.already_returned_quantity > 0 ? it.already_returned_quantity : '0'}
                            </td>
                            <td style={{ textAlign: 'right', verticalAlign: 'middle' }}>
                              <input
                                type="number"
                                min="0"
                                step="any"
                                max={it.maxQuantity !== undefined ? it.maxQuantity : undefined}
                                value={it.quantity}
                                onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                                disabled={!it.selected || isFinished}
                                style={{ width: '80px', textAlign: 'right' }}
                              />
                              {it.maxQuantity !== undefined && it.maxQuantity < 9999 && !isFinished && (
                                <div style={{ fontSize: '0.68rem', color: 'var(--muted)', marginTop: '1px' }}>
                                  max {it.maxQuantity}
                                </div>
                              )}
                            </td>
                            <td style={{ textAlign: 'right', verticalAlign: 'middle' }}>
                              <input
                                type="number"
                                min="0"
                                step="any"
                                value={it.unit_price}
                                onChange={(e) => handleItemChange(idx, 'unit_price', e.target.value)}
                                disabled={!it.selected || isFinished}
                                style={{ width: '90px', textAlign: 'right' }}
                              />
                            </td>
                            <td style={{ textAlign: 'right', verticalAlign: 'middle' }}>
                              <select
                                value={it.gst_rate}
                                onChange={(e) => handleItemChange(idx, 'gst_rate', Number(e.target.value))}
                                disabled={!it.selected || isFinished}
                                style={{ width: '70px', padding: '0.35rem 0.2rem' }}
                              >
                                <option value={0}>0%</option>
                                <option value={5}>5%</option>
                                <option value={12}>12%</option>
                                <option value={18}>18%</option>
                                <option value={28}>28%</option>
                              </select>
                            </td>
                            <td style={{ textAlign: 'right', verticalAlign: 'middle', fontWeight: 700, color: (it.selected && !isFinished) ? 'var(--accent)' : 'var(--muted)' }}>
                              ₹{formatINR(lineTotal)}
                            </td>
                            <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>
                              <button
                                type="button"
                                className="remove-item"
                                onClick={() => handleRemoveItem(idx)}
                                title="Remove item"
                                style={{ padding: '2px 6px', fontSize: '0.8rem' }}
                              >
                                ✕
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Remarks / Return Notes */}
            <label style={{ display: 'block', margin: 0 }}>
              Remarks / Return Notes (Optional)
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="e.g. Customer returned goods due to damage, credited against invoice"
                style={{ width: '100%', marginTop: '0.35rem' }}
              />
            </label>

            {/* Totals & Passbook Summary Card */}
            <div className="credit-note-summary-card">
              <div className="credit-note-ledger-info">
                <div className="credit-note-ledger-badge">
                  <span>🏦</span>
                  <span>Account Passbook Credit</span>
                </div>
                <div className="credit-note-ledger-text">
                  {returnTotal > 0 ? (
                    <>
                      ✓ <strong>₹{formatINR(returnTotal)}</strong> will be credited to <strong>{selectedCustomer?.name || 'Customer'}</strong>'s passbook, reducing balance.
                    </>
                  ) : (
                    <span style={{ color: 'var(--muted)' }}>
                      Select items to return and enter quantity to credit customer passbook.
                    </span>
                  )}
                </div>
              </div>

              <div className="credit-note-amounts">
                <div className="credit-note-amount-breakdown">
                  <span>Subtotal: ₹{formatINR(returnSubtotal)}</span>
                  <span>+</span>
                  <span>GST: ₹{formatINR(returnTax)}</span>
                </div>
                <div className="credit-note-grand-total">
                  <span className="total-label">Total Credit:</span>
                  <span className="total-val">₹{formatINR(returnTotal)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* STICKY ENTERPRISE FOOTER (Always 100% visible at the bottom) */}
          <div className="modal-dialog-footer">
            <label className="credit-note-whatsapp-toggle">
              <input
                type="checkbox"
                checked={sendWhatsApp}
                onChange={(e) => setSendWhatsApp(e.target.checked)}
              />
              <span>Send Credit Note PDF via WhatsApp 💬</span>
            </label>

            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
              <button
                type="button"
                className="secondary-action"
                onClick={onClose}
                disabled={saving}
                style={{ margin: 0 }}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={saving || returnTotal <= 0 || isOrderFullyReturned}
                style={{ margin: 0 }}
              >
                {saving ? 'Issuing Credit Note…' : `✓ Issue Credit Note (₹${formatINR(returnTotal)})`}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
