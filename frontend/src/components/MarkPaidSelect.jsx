import { api } from '../api';
import { PAYMENT_METHODS } from '../paymentMethods';

export default function MarkPaidSelect({ orderId, onDone }) {
  async function handleChange(e) {
    const method = e.target.value;
    e.target.value = '';
    if (!method) return;
    await api.updateOrder(orderId, { payment_status: 'paid', payment_method: method });
    onDone();
  }

  return (
    <select defaultValue="" onChange={handleChange} className="snooze-select" title="Mark paid">
      <option value="" disabled>Mark paid via…</option>
      {PAYMENT_METHODS.map((m) => (
        <option key={m.value} value={m.value}>{m.label}</option>
      ))}
    </select>
  );
}
