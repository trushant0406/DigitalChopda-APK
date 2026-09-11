import { PAYMENT_METHODS } from '../paymentMethods';

export default function PaymentField({ paidNow, setPaidNow, method, setMethod }) {
  return (
    <fieldset>
      <legend>Payment</legend>
      <div className="grid">
        <label>
          Status
          <select value={paidNow ? 'paid' : 'pending'} onChange={(e) => setPaidNow(e.target.value === 'paid')}>
            <option value="pending">Pending</option>
            <option value="paid">Paid now</option>
          </select>
        </label>
        {paidNow && (
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
    </fieldset>
  );
}
