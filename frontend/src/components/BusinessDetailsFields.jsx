export default function BusinessDetailsFields({ value, onChange }) {
  function set(field) {
    return (e) => onChange({ ...value, [field]: e.target.value });
  }

  return (
    <>
      <div className="grid">
        <label>
          GSTIN
          <input value={value.gstin || ''} onChange={set('gstin')} placeholder="e.g. 24DDPPK6180J1Z3" />
        </label>
        <label>
          PAN
          <input value={value.pan || ''} onChange={set('pan')} placeholder="e.g. DDPPK6180J" />
        </label>
        <label>
          State
          <input value={value.state || ''} onChange={set('state')} placeholder="e.g. Gujarat" />
        </label>
      </div>
      <label>
        Billing address
        <textarea value={value.billing_address || ''} onChange={set('billing_address')} rows={2} />
      </label>
      <label>
        Shipping address (leave blank if same as billing)
        <textarea value={value.shipping_address || ''} onChange={set('shipping_address')} rows={2} />
      </label>
    </>
  );
}
