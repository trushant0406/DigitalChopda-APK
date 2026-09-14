export const emptyItem = () => ({ product_name: '', quantity: '1', unit_price: '', gst_rate: null });

export function itemsTotal(items) {
  return items.reduce((sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.unit_price) || 0), 0);
}

export function itemsTax(items) {
  return items.reduce(
    (sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.unit_price) || 0) * ((Number(i.gst_rate) || 0) / 100),
    0
  );
}

export function itemsToPayload(items) {
  return items.map((i) => ({
    product_name: i.product_name,
    quantity: Number(i.quantity),
    unit_price: Number(i.unit_price),
    gst_rate: i.gst_rate || undefined,
  }));
}
