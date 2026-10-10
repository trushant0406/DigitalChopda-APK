import { useEffect, useState } from 'react';
import { api } from '../api';
import DataTable from '../components/DataTable';

export default function ProductsPage({ refreshKey }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [hsnCode, setHsnCode] = useState('');
  const [gstRate, setGstRate] = useState('18');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState('');
  const [editPrice, setEditPrice] = useState('');
  const [editHsn, setEditHsn] = useState('');
  const [editGstRate, setEditGstRate] = useState('');
  const [editSaving, setEditSaving] = useState(false);
  const [editError, setEditError] = useState(null);

  async function load() {
    setLoading(true);
    try {
      setProducts(await api.listProducts());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [refreshKey]);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.createProduct({ name, default_price: price, hsn_code: hsnCode, gst_rate: gstRate });
      setName('');
      setPrice('');
      setHsnCode('');
      setGstRate('18');
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function startEdit(product) {
    setEditingId(product.id);
    setEditName(product.name);
    setEditPrice(String(product.default_price));
    setEditHsn(product.hsn_code || '');
    setEditGstRate(String(product.gst_rate));
    setEditError(null);
  }

  async function saveEdit(id) {
    setEditSaving(true);
    setEditError(null);
    try {
      await api.updateProduct(id, { name: editName, default_price: editPrice, hsn_code: editHsn, gst_rate: editGstRate });
      setEditingId(null);
      await load();
    } catch (err) {
      setEditError(err.message);
    } finally {
      setEditSaving(false);
    }
  }

  async function deleteProduct(product) {
    if (!window.confirm(`Delete "${product.name}" from the catalog? Past orders keep their own record either way.`)) return;
    await api.deleteProduct(product.id);
    load();
  }

  const columns = [
    {
      key: 'name',
      label: 'Product',
      render: (p) =>
        editingId === p.id ? (
          <input value={editName} onChange={(e) => setEditName(e.target.value)} />
        ) : (
          p.name
        ),
    },
    {
      key: 'hsn_code',
      label: 'HSN',
      render: (p) =>
        editingId === p.id ? (
          <input value={editHsn} onChange={(e) => setEditHsn(e.target.value)} placeholder="e.g. 9403" />
        ) : (
          p.hsn_code || <span className="cell-muted">—</span>
        ),
    },
    {
      key: 'gst_rate',
      label: 'GST %',
      align: 'right',
      render: (p) =>
        editingId === p.id ? (
          <input type="number" min="0" step="any" value={editGstRate} onChange={(e) => setEditGstRate(e.target.value)} />
        ) : (
          `${Number(p.gst_rate)}%`
        ),
    },
    {
      key: 'default_price',
      label: 'Default price',
      align: 'right',
      render: (p) =>
        editingId === p.id ? (
          <input type="number" min="0" step="any" value={editPrice} onChange={(e) => setEditPrice(e.target.value)} />
        ) : (
          `₹${Number(p.default_price).toFixed(2)}`
        ),
    },
    {
      key: 'created_at',
      label: 'Added',
      render: (p) => new Date(p.created_at).toLocaleDateString(),
    },
    {
      key: 'action',
      label: '',
      render: (p) =>
        editingId === p.id ? (
          <div className="row-actions">
            <button type="button" onClick={() => saveEdit(p.id)} disabled={editSaving}>
              {editSaving ? 'Saving…' : 'Save'}
            </button>
            <button type="button" className="remove-item" onClick={() => setEditingId(null)}>Cancel</button>
          </div>
        ) : (
          <div className="row-actions">
            <button type="button" onClick={() => startEdit(p)}>Edit</button>
            <button type="button" className="danger-action" onClick={() => deleteProduct(p)}>Delete</button>
          </div>
        ),
    },
  ];

  return (
    <div className="page">
      <form className="card call-form" onSubmit={handleSubmit}>
        <h2>Add product</h2>
        <div className="grid">
          <label>
            Name *
            <input required value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Steel bolt (M8)" />
          </label>
          <label>
            HSN code
            <input value={hsnCode} onChange={(e) => setHsnCode(e.target.value)} placeholder="e.g. 9403" />
          </label>
          <label>
            GST %
            <input type="number" min="0" step="any" value={gstRate} onChange={(e) => setGstRate(e.target.value)} />
          </label>
          <label>
            Default price *
            <input
              required
              type="number"
              min="0"
              step="any"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </label>
        </div>
        <p className="hint">HSN classifies the product for GST — needed on quotations. Leave blank if you don't sell under GST.</p>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Add product'}
        </button>
      </form>

      <div className="card">
        <div className="card-header">
          <h2>Catalog</h2>
        </div>
        {editError && <p className="error">{editError}</p>}
        <DataTable columns={columns} rows={products} loading={loading} emptyText="No products yet — add your first one above." />
      </div>
    </div>
  );
}
