const BASE = 'http://localhost:4000/api';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, options);
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `Request failed: ${res.status}`);
  }
  return res.json();
}

const json = (body) => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});

export const api = {
  logCall: (formData) =>
    fetch(`${BASE}/calls`, { method: 'POST', body: formData }).then(async (res) => {
      if (!res.ok) throw new Error((await res.json().catch(() => ({}))).error || 'Failed to log call');
      return res.json();
    }),
  listCalls: () => request('/calls'),
  callsExportUrl: () => `${BASE}/calls/export.csv`,

  listContacts: (q) => {
    const suffix = q ? `?q=${encodeURIComponent(q)}` : '';
    return request(`/contacts${suffix}`);
  },
  getContact: (id) => request(`/contacts/${id}`),
  updateContact: (id, body) => request(`/contacts/${id}`, { ...json(body), method: 'PATCH' }),

  listFollowups: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    const suffix = qs ? `?${qs}` : '';
    return request(`/followups${suffix}`);
  },
  createFollowup: (body) => request('/followups', json(body)),
  updateFollowup: (id, body) => request(`/followups/${id}`, { ...json(body), method: 'PATCH' }),
  logFollowupCall: (id, body) => request(`/followups/${id}/log-call`, json(body)),

  dueToday: () => request('/dashboard/due-today'),
  overdue: () => request('/dashboard/overdue'),
  neverFollowedUp: () => request('/dashboard/never-followed-up'),
  repeatNoConnect: () => request('/dashboard/repeat-no-connect'),
  summary: () => request('/dashboard/summary'),

  createOrder: (body) => request('/orders', json(body)),
  listOrders: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    const suffix = qs ? `?${qs}` : '';
    return request(`/orders${suffix}`);
  },
  updateOrder: (id, body) => request(`/orders/${id}`, { ...json(body), method: 'PATCH' }),
  ordersSummary: () => request('/orders/summary'),

  listProducts: () => request('/products'),
  createProduct: (body) => request('/products', json(body)),
  updateProduct: (id, body) => request(`/products/${id}`, { ...json(body), method: 'PATCH' }),
  deleteProduct: (id) => request(`/products/${id}`, { method: 'DELETE' }),

  getBusinessProfile: () => request('/business-profile'),
  saveBusinessProfile: (body) => request('/business-profile', { ...json(body), method: 'PUT' }),

  createQuotation: (body) => request('/quotations', json(body)),
  listQuotations: () => request('/quotations'),
  getQuotation: (id) => request(`/quotations/${id}`),
  quotationPdfUrl: (id) => `${BASE}/quotations/${id}/pdf`,
  sendQuotationWhatsApp: (id) => request(`/quotations/${id}/send-whatsapp`, json({})),

  creditSummary: () => request('/credit/summary'),
  listCreditCustomers: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request(`/credit/customers${qs ? `?${qs}` : ''}`);
  },
  listCreditOrders: (contactId) => request(`/credit/customers/${contactId}/orders`),
  collectCreditPayment: (body) => request('/credit/collect', json(body)),

  vapidPublicKey: () => request('/push/vapid-public-key'),
  subscribePush: (subscription) => request('/push/subscribe', json(subscription)),
  settingsStatus: () => request('/settings/status'),
  reconnectWhatsAppWeb: (forceReset) => request('/settings/whatsapp-web/reconnect', json({ forceReset })),
};
