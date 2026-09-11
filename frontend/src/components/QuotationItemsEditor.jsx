import { useEffect, useState } from 'react';
import { api } from '../api';
import { emptyQuotationItem } from '../quotationItems';

const NEW_PRODUCT_VALUE = '__add_new__';

export default function QuotationItemsEditor({ items, onChange }) {
  const [products, setProducts] = useState([]);
  const [creatingIndex, setCreatingIndex] = useState(null);
  const [draftName, setDraftName] = useState('');
  const [draftPrice, setDraftPrice] = useState('');
  const [draftHsn, setDraftHsn] = useState('');
  const [draftGstRate, setDraftGstRate] = useState('18');
  const [draftError, setDraftError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.listProducts().then(setProducts).catch(() => {});
  }, []);

  function updateItem(index, field, value) {
    onChange(items.map((item, i) => (i === index ? { ...item, [field]: value } : item)));
  }

  function addItem() {
    onChange([...items, emptyQuotationItem()]);
  }

  function removeItem(index) {
    onChange(items.length > 1 ? items.filter((_, i) => i !== index) : items);
  }

  function handleProductSelect(index, value) {
    if (value === NEW_PRODUCT_VALUE) {
      setCreatingIndex(index);
      setDraftName('');
      setDraftPrice('');
      setDraftHsn('');
      setDraftGstRate('18');
      setDraftError(null);
      return;
    }
    const product = products.find((p) => p.name === value);
    onChange(
      items.map((item, i) =>
        i === index
          ? {
              ...item,
              product_name: value,
              hsn_code: product?.hsn_code || item.hsn_code,
              rate: product ? String(product.default_price) : item.rate,
              gst_rate: product ? Number(product.gst_rate) : item.gst_rate,
            }
          : item
      )
    );
  }

  async function saveNewProduct(index) {
    if (!draftName || draftPrice === '') return;
    setSaving(true);
    setDraftError(null);
    try {
      const product = await api.createProduct({
        name: draftName, default_price: draftPrice, hsn_code: draftHsn, gst_rate: draftGstRate,
      });
      setProducts((prev) => [...prev, product].sort((a, b) => a.name.localeCompare(b.name)));
      onChange(
        items.map((item, i) =>
          i === index
            ? { ...item, product_name: product.name, hsn_code: product.hsn_code || '', rate: String(product.default_price), gst_rate: Number(product.gst_rate) }
            : item
        )
      );
      setCreatingIndex(null);
    } catch (err) {
      setDraftError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      {items.map((item, index) => (
        <div key={index}>
          <div className="order-item-row quotation-item-row">
            {creatingIndex === index ? (
              <>
                <input autoFocus placeholder="New product name" value={draftName} onChange={(e) => setDraftName(e.target.value)} />
                <input placeholder="HSN code" value={draftHsn} onChange={(e) => setDraftHsn(e.target.value)} />
                <input type="number" min="0" step="any" placeholder="Price" value={draftPrice} onChange={(e) => setDraftPrice(e.target.value)} />
                <input type="number" min="0" step="any" placeholder="GST %" value={draftGstRate} onChange={(e) => setDraftGstRate(e.target.value)} />
                <button type="button" onClick={() => saveNewProduct(index)} disabled={saving}>
                  {saving ? 'Saving…' : 'Save'}
                </button>
                <button type="button" className="remove-item" onClick={() => setCreatingIndex(null)} title="Cancel">✕</button>
              </>
            ) : (
              <>
                <select required value={item.product_name} onChange={(e) => handleProductSelect(index, e.target.value)}>
                  <option value="" disabled>Select product…</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.name}>{p.name} (₹{Number(p.default_price).toFixed(0)})</option>
                  ))}
                  <option value={NEW_PRODUCT_VALUE}>+ Add new product…</option>
                </select>
                <input placeholder="HSN" value={item.hsn_code} onChange={(e) => updateItem(index, 'hsn_code', e.target.value)} />
                <input required type="number" min="0" step="any" placeholder="Qty" value={item.quantity} onChange={(e) => updateItem(index, 'quantity', e.target.value)} />
                <input required type="number" min="0" step="any" placeholder="Rate" value={item.rate} onChange={(e) => updateItem(index, 'rate', e.target.value)} />
                <input required type="number" min="0" step="any" placeholder="GST %" value={item.gst_rate} onChange={(e) => updateItem(index, 'gst_rate', e.target.value)} />
                <button type="button" className="remove-item" onClick={() => removeItem(index)} disabled={items.length === 1} title="Remove item">✕</button>
              </>
            )}
          </div>
          {creatingIndex === index && draftError && <p className="error">{draftError}</p>}
        </div>
      ))}
      <button type="button" onClick={addItem}>+ Add product</button>
    </>
  );
}
