// One computed "where are they in the funnel" badge — no extra storage,
// derived from signals already on the contact/order rows.
export function leadStage({ callCount, orderCount, hasPaidOrder, hasDispatchedOrder, hasDeliveredOrder }) {
  if (hasDeliveredOrder) return 'delivered';
  if (hasDispatchedOrder) return 'dispatched';
  if (hasPaidOrder) return 'paid';
  if (Number(orderCount) > 0) return 'ordered';
  if (Number(callCount) > 0) return 'contacted';
  return 'lead';
}

export const LEAD_STAGE_LABEL = {
  lead: 'New lead',
  contacted: 'Contacted',
  ordered: 'Ordered',
  paid: 'Paid',
  dispatched: 'Dispatched',
  delivered: 'Delivered',
};

export const LEAD_STAGE_TONE = {
  lead: 'neutral',
  contacted: 'neutral',
  ordered: 'warn',
  paid: 'info',
  dispatched: 'info',
  delivered: 'ok',
};
