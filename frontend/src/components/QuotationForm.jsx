import { useEffect, useState } from 'react';
import { api } from '../api';
import QuotationItemsEditor from './QuotationItemsEditor';
import BusinessDetailsFields from './BusinessDetailsFields';
import { emptyQuotationItem, quotationTotals } from '../quotationItems';

export default function QuotationForm({ onCreated, presetContact }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [selectedContact, setSelectedContact] = useState(presetContact || null);

  const [phone, setPhone] = useState(presetContact?.phone || '');
  const [name, setName] = useState(presetContact?.name || '');
  const [business, setBusiness] = useState(
    presetContact
      ? {
          gstin: presetContact.gstin || '',
          pan: presetContact.pan || '',
          billing_address: presetContact.billing_address || '',
          shipping_address: presetContact.shipping_address || '',
          state: presetContact.state || '',
        }
      : {}
  );
  const [shipSame, setShipSame] = useState(true);
  const [shipToAddress, setShipToAddress] = useState('');

  const [items, setItems] = useState([emptyQuotationItem()]);
  const [terms, setTerms] = useState('Thanks');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.getBusinessProfile().then((p) => p?.terms_and_conditions && setTerms(p.terms_and_conditions)).catch(() => {});
  }, []);

  async function runSearch() {
    if (!query) return;
    setResults(await api.listContacts(query));
  }

  function selectContact(contact) {
    setSelectedContact(contact);
    setPhone(contact.phone);
    setName(contact.name || '');
    setBusiness({
      gstin: contact.gstin || '',
      pan: contact.pan || '',
      billing_address: contact.billing_address || '',
      shipping_address: contact.shipping_address || '',
      state: contact.state || '',
    });
    setResults([]);
    setQuery('');
  }

  function clearContact() {
    setSelectedContact(null);
    setPhone('');
    setName('');
    setBusiness({});
  }

  const totals = quotationTotals(items);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await api.createQuotation({
        contact_id: selectedContact?.id,
        phone: selectedContact ? undefined : phone,
        name: name || undefined,
        ...business,
        ship_to_name: name || undefined,
        ship_to_address: shipSame ? undefined : shipToAddress,
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
      setItems([emptyQuotationItem()]);
      clearContact();
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

      {!selectedContact ? (
        <>
          <div className="inline-form">
            <input
              placeholder="Search existing customer by name or phone…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), runSearch())}
            />
            <button type="button" onClick={runSearch}>Search</button>
          </div>
          {results.length > 0 && (
            <ul className="top-products">
              {results.map((r) => (
                <li key={r.id}>
                  <button type="button" className="secondary-action" onClick={() => selectContact(r)}>
                    {r.name || r.phone} — {r.phone}
                  </button>
                </li>
              ))}
            </ul>
          )}
          <p className="hint">Or enter a new customer below:</p>
          <div className="grid">
            <label>
              Phone *
              <input required value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91 98765 43210" />
            </label>
            <label>
              Name
              <input value={name} onChange={(e) => setName(e.target.value)} />
            </label>
          </div>
        </>
      ) : (
        <div className="customer-summary">
          <strong>{name || phone}</strong>
          <span>{phone}</span>
          <button type="button" className="remove-item" onClick={clearContact}>Change customer</button>
        </div>
      )}

      <fieldset>
        <legend>Bill to / GST details</legend>
        <BusinessDetailsFields value={business} onChange={setBusiness} />
      </fieldset>

      <label className="hint">
        <input type="checkbox" checked={shipSame} onChange={(e) => setShipSame(e.target.checked)} style={{ width: 'auto', marginRight: '0.4rem' }} />
        Ship to same address as billing
      </label>
      {!shipSame && (
        <label>
          Shipping address
          <textarea value={shipToAddress} onChange={(e) => setShipToAddress(e.target.value)} rows={2} />
        </label>
      )}

      <fieldset>
        <legend>Items</legend>
        <QuotationItemsEditor items={items} onChange={setItems} />
        <p className="order-total">
          Subtotal ₹{totals.subtotal.toFixed(2)} + Tax ₹{totals.taxAmount.toFixed(2)} = Total ₹{totals.total.toFixed(2)}
        </p>
      </fieldset>

      <label>
        Terms &amp; conditions
        <textarea value={terms} onChange={(e) => setTerms(e.target.value)} rows={2} />
      </label>

      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={saving}>
        {saving ? 'Creating…' : 'Create quotation'}
      </button>
    </form>
  );
}
