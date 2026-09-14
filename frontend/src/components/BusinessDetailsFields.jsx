const GST_STATE_CODES = {
  '01': 'Jammu & Kashmir', '02': 'Himachal Pradesh', '03': 'Punjab', '04': 'Chandigarh',
  '05': 'Uttarakhand', '06': 'Haryana', '07': 'Delhi', '08': 'Rajasthan',
  '09': 'Uttar Pradesh', '10': 'Bihar', '11': 'Sikkim', '12': 'Arunachal Pradesh',
  '13': 'Nagaland', '14': 'Manipur', '15': 'Mizoram', '16': 'Tripura',
  '17': 'Meghalaya', '18': 'Assam', '19': 'West Bengal', '20': 'Jharkhand',
  '21': 'Odisha', '22': 'Chhattisgarh', '23': 'Madhya Pradesh', '24': 'Gujarat',
  '26': 'Dadra & Nagar Haveli and Daman & Diu', '27': 'Maharashtra', '29': 'Karnataka',
  '30': 'Goa', '31': 'Lakshadweep', '32': 'Kerala', '33': 'Tamil Nadu',
  '34': 'Puducherry', '36': 'Telangana', '37': 'Andhra Pradesh', '38': 'Ladakh'
};

export default function BusinessDetailsFields({ value, onChange }) {
  function set(field) {
    return (e) => onChange({ ...value, [field]: e.target.value });
  }

  function handleGstChange(e) {
    const val = e.target.value.toUpperCase();
    const next = { ...value, gstin: val };
    if (val.length >= 12 && !value.pan) {
      next.pan = val.slice(2, 12);
    }
    if (val.length >= 2 && !value.state) {
      const code = val.slice(0, 2);
      if (GST_STATE_CODES[code]) {
        next.state = GST_STATE_CODES[code];
      }
    }
    onChange(next);
  }

  return (
    <>
      <div className="grid">
        <label>
          GSTIN
          <input value={value.gstin || ''} onChange={handleGstChange} placeholder="e.g. 24DDPPK6180J1Z3" />
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
