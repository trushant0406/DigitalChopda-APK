import { PAYMENT_METHODS } from '../paymentMethods';

export default function PaymentField({ status, setStatus, method, setMethod }) {
  return (
    <fieldset>
      <legend>Payment</legend>
      <div className="grid">
        <label>
          Status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="pending">Pending</option>
            <option value="paid">Paid now</option>
            <option value="credit">On credit (Khata)</option>
          </select>
        </label>
        {status === 'paid' && (
          <label>
            Method
            <select value={method} onChange={(e) => setMethod(e.target.value)}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </label>
        )}
      </div>
      {status === 'credit' && (
        <p className="hint">Added to this customer's khata balance. Collect payment later from the Credit page.</p>
      )}
    </fieldset>
  );
}
