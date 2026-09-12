export const PAYMENT_METHODS = [
  { value: 'cash', label: 'Cash' },
  { value: 'upi', label: 'UPI' },
  { value: 'card', label: 'Card' },
  { value: 'bank_transfer', label: 'Bank transfer' },
  { value: 'other', label: 'Other' },
];

export const PAYMENT_METHOD_LABEL = {
  ...Object.fromEntries(PAYMENT_METHODS.map((m) => [m.value, m.label])),
  credit: 'Credit (Khata)',
};
