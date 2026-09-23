import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../context/AuthContext';

const INDIAN_STATES = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh',
  'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka',
  'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram',
  'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu',
  'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
  'Delhi', 'Jammu and Kashmir', 'Ladakh'
];

export default function OnboardBusinessModal({ isOpen, onClose }) {
  const { refreshBusinesses, switchBusiness } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [form, setForm] = useState({
    name: '',
    trade_name: '',
    gstin: '',
    state: 'Gujarat',
    mobile: '',
    address: '',
    invoice_prefix: 'INV-',
  });

  if (!isOpen) return null;

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
    setError(null);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.name.trim()) {
      setError('Business / Company name is required.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const newBiz = await api.createBusiness({
        name: form.name.trim(),
        trade_name: form.trade_name.trim() || null,
        gstin: form.gstin.trim() || null,
        state: form.state || null,
        mobile: form.mobile.trim() || null,
        address: form.address.trim() || null,
        invoice_prefix: form.invoice_prefix.trim() || 'INV-',
      });

      await refreshBusinesses();
      switchBusiness(newBiz.id);
      onClose();
    } catch (err) {
      console.error('Failed to create business:', err);
      setError(err.message || 'Failed to onboard business');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="onboard-modal-overlay" onClick={onClose}>
      <div className="onboard-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="onboard-modal-header">
          <div className="onboard-modal-title-group">
            <div className="onboard-modal-icon">🏢</div>
            <div>
              <h2 className="onboard-modal-title">Onboard New Business</h2>
              <p className="onboard-modal-subtitle">
                Set up a new company profile. Invoices, customers, and inventory remain 100% isolated.
              </p>
            </div>
          </div>
          <button type="button" className="onboard-modal-close" onClick={onClose} aria-label="Close">
            &times;
          </button>
        </div>

        {error && <div className="onboard-modal-error">{error}</div>}

        <form onSubmit={handleSubmit} className="onboard-modal-form">
          <div className="onboard-form-group">
            <label className="onboard-label">
              Business / Company Name <span className="required-star">*</span>
            </label>
            <input
              type="text"
              className="onboard-input"
              placeholder="e.g. Shree Ganesh Traders"
              value={form.name}
              onChange={(e) => handleChange('name', e.target.value)}
              required
              autoFocus
            />
          </div>

          <div className="onboard-form-row">
            <div className="onboard-form-group">
              <label className="onboard-label">Trade Name / Brand (Optional)</label>
              <input
                type="text"
                className="onboard-input"
                placeholder="e.g. Ganesh Electronics"
                value={form.trade_name}
                onChange={(e) => handleChange('trade_name', e.target.value)}
              />
            </div>
            <div className="onboard-form-group">
              <label className="onboard-label">GSTIN (Optional)</label>
              <input
                type="text"
                className="onboard-input"
                placeholder="e.g. 24AAAAA0000A1Z5"
                maxLength={15}
                value={form.gstin}
                onChange={(e) => handleChange('gstin', e.target.value.toUpperCase())}
              />
            </div>
          </div>

          <div className="onboard-form-row">
            <div className="onboard-form-group">
              <label className="onboard-label">State</label>
              <select
                className="onboard-input"
                value={form.state}
                onChange={(e) => handleChange('state', e.target.value)}
              >
                {INDIAN_STATES.map((st) => (
                  <option key={st} value={st}>
                    {st}
                  </option>
                ))}
              </select>
            </div>
            <div className="onboard-form-group">
              <label className="onboard-label">Mobile Number</label>
              <input
                type="tel"
                className="onboard-input"
                placeholder="e.g. 9876543210"
                value={form.mobile}
                onChange={(e) => handleChange('mobile', e.target.value)}
              />
            </div>
          </div>

          <div className="onboard-form-group">
            <label className="onboard-label">Business Address</label>
            <textarea
              className="onboard-input"
              rows={2}
              placeholder="Shop/Office address..."
              value={form.address}
              onChange={(e) => handleChange('address', e.target.value)}
            />
          </div>

          <div className="onboard-form-group">
            <label className="onboard-label">Invoice Prefix</label>
            <input
              type="text"
              className="onboard-input"
              placeholder="e.g. INV- or SGT-"
              value={form.invoice_prefix}
              onChange={(e) => handleChange('invoice_prefix', e.target.value)}
            />
            <span className="onboard-help-text">First invoice will start at {form.invoice_prefix || 'INV-'}1</span>
          </div>

          <div className="onboard-modal-footer">
            <button
              type="button"
              className="onboard-btn-secondary"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="onboard-btn-primary"
              disabled={loading}
            >
              {loading ? 'Creating...' : 'Create & Switch Business'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
