import { useEffect, useState } from 'react';
import { api } from '../api';
import { emptyItem } from '../orderItems';

const NEW_PRODUCT_VALUE = '__add_new__';

export default function OrderItemsEditor({ items, onChange }) {
  const [products, setProducts] = useState([]);
  const [creatingIndex, setCreatingIndex] = useState(null);
  const [draftName, setDraftName] = useState('');
  const [draftPrice, setDraftPrice] = useState('');
  const [draftError, setDraftError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.listProducts().then(setProducts).catch(() => {});
  }, []);

  function updateItem(index, field, value) {
    onChange(items.map((item, i) => (i === index ? { ...item, [field]: value } : item)));
  }

  function addItem() {
    onChange([...items, emptyItem()]);
  }

  function removeItem(index) {
    onChange(items.length > 1 ? items.filter((_, i) => i !== index) : items);
  }

  function handleProductSelect(index, value) {
    if (value === NEW_PRODUCT_VALUE) {
      setCreatingIndex(index);
      setDraftName('');
      setDraftPrice('');
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
              quantity: !item.quantity || Number(item.quantity) <= 0 ? '1' : item.quantity,
              unit_price: product ? String(product.default_price) : item.unit_price,
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
      const product = await api.createProduct({ name: draftName, default_price: draftPrice });
      setProducts((prev) => [...prev, product].sort((a, b) => a.name.localeCompare(b.name)));
      onChange(
        items.map((item, i) =>
          i === index
            ? { ...item, product_name: product.name, unit_price: String(product.default_price), gst_rate: Number(product.gst_rate) }
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
          <div className="order-item-row">
            {creatingIndex === index ? (
              <>
                <input
                  autoFocus
                  placeholder="New product name"
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                />
                <input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="Default price"
                  value={draftPrice}
                  onChange={(e) => setDraftPrice(e.target.value)}
                />
                <button type="button" onClick={() => saveNewProduct(index)} disabled={saving}>
                  {saving ? 'Saving…' : 'Save'}
                </button>
                <button type="button" className="remove-item" onClick={() => setCreatingIndex(null)} title="Cancel">
                  ✕
                </button>
              </>
            ) : (
              <>
                <select
                  required
                  value={item.product_name}
                  onChange={(e) => handleProductSelect(index, e.target.value)}
                >
                  <option value="" disabled>Select product…</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.name}>
                      {p.name} (₹{Number(p.default_price).toFixed(0)})
                    </option>
                  ))}
                  <option value={NEW_PRODUCT_VALUE}>+ Add new product…</option>
                </select>
                <input
                  required
                  type="number"
                  min="0"
                  step="any"
                  placeholder="Qty"
                  value={item.quantity}
                  onChange={(e) => updateItem(index, 'quantity', e.target.value)}
                />
                <input
                  required
                  type="number"
                  min="0"
                  step="any"
                  placeholder="Price"
                  value={item.unit_price}
                  onChange={(e) => updateItem(index, 'unit_price', e.target.value)}
                />
                <button
                  type="button"
                  className="remove-item"
                  onClick={() => removeItem(index)}
                  disabled={items.length === 1}
                  title="Remove item"
                >
                  ✕
                </button>
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
