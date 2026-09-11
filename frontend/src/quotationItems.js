export const emptyQuotationItem = () => ({ product_name: '', hsn_code: '', unit: 'PCS', quantity: '', rate: '', gst_rate: 18 });

export function quotationTotals(items) {
  const subtotal = items.reduce((sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.rate) || 0), 0);
  const taxAmount = items.reduce(
    (sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.rate) || 0) * ((Number(i.gst_rate) || 0) / 100),
    0
  );
  return { subtotal, taxAmount, total: subtotal + taxAmount };
}
