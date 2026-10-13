const BASE = '/api';

export function getActiveBusinessId() {
  return localStorage.getItem('ci_business_id') || '1';
}

function getAuthHeaders() {
  const token = localStorage.getItem('ci_token');
  const businessId = localStorage.getItem('ci_business_id');
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (businessId) headers['X-Business-Id'] = businessId;
  return headers;
}

async function request(path, options = {}) {
  const headers = { ...getAuthHeaders(), ...(options.headers || {}) };
  const res = await fetch(`${BASE}${path}`, { ...options, headers });
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
  login: (username, password) => request('/auth/login', json({ username, password })),
  getMe: () => request('/auth/me'),
  changePassword: (currentPassword, newPassword) => request('/auth/change-password', json({ currentPassword, newPassword })),

  logCall: (formData) =>
    fetch(`${BASE}/calls`, { method: 'POST', headers: getAuthHeaders(), body: formData }).then(async (res) => {
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
  getNextInvoiceNumber: () => request('/orders/next-number'),
  listOrders: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    const suffix = qs ? `?${qs}` : '';
    return request(`/orders${suffix}`);
  },
  updateOrder: (id, body) => request(`/orders/${id}`, { ...json(body), method: 'PATCH' }),
  editOrder: (id, body) => request(`/orders/${id}`, { ...json(body), method: 'PUT' }),
  deleteOrder: (id) => request(`/orders/${id}`, { method: 'DELETE' }),
  ordersSummary: () => request('/orders/summary'),
  orderPdfUrl: (id, withPayments = false) => {
    const bizId = getActiveBusinessId();
    return `${BASE}/orders/${id}/pdf?business_id=${bizId}${withPayments ? '&payment_details=true' : ''}`;
  },
  orderShippingLabelUrl: (id) => {
    const bizId = getActiveBusinessId();
    return `${BASE}/orders/${id}/shipping-label/pdf?business_id=${bizId}`;
  },
  sendOrderWhatsApp: (id) => request(`/orders/${id}/send-whatsapp`, json({})),

  listProducts: () => request('/products'),
  createProduct: (body) => request('/products', json(body)),
  updateProduct: (id, body) => request(`/products/${id}`, { ...json(body), method: 'PATCH' }),
  deleteProduct: (id) => request(`/products/${id}`, { method: 'DELETE' }),

  getBusinessProfile: () => request('/business-profile'),
  saveBusinessProfile: (body) => request('/business-profile', { ...json(body), method: 'PUT' }),

  // Multi-Tenancy Businesses
  listBusinesses: () => request('/businesses'),
  getActiveBusiness: () => request('/businesses/active'),
  createBusiness: (body) => request('/businesses', json(body)),
  updateBusiness: (id, body) => request(`/businesses/${id}`, { ...json(body), method: 'PUT' }),

  createQuotation: (body) => request('/quotations', json(body)),
  listQuotations: () => request('/quotations'),
  getQuotation: (id) => request(`/quotations/${id}`),
  quotationPdfUrl: (id) => `${BASE}/quotations/${id}/pdf?business_id=${getActiveBusinessId()}`,
  quotationShippingLabelUrl: (id) => `${BASE}/quotations/${id}/shipping-label/pdf?business_id=${getActiveBusinessId()}`,
  sendQuotationWhatsApp: (id) => request(`/quotations/${id}/send-whatsapp`, json({})),
  convertQuotationToInvoice: (id, body = {}) => request(`/quotations/${id}/convert-to-invoice`, { ...json(body), method: 'POST' }),

  creditSummary: () => request('/credit/summary'),
  listCreditCustomers: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request(`/credit/customers${qs ? `?${qs}` : ''}`);
  },
  listCreditOrders: (contactId) => request(`/credit/customers/${contactId}/orders`),
  getCustomerLedger: (contactId) => request(`/credit/customers/${contactId}/ledger`),
  getUnpaidInvoices: (contactId) => request(`/credit/customers/${contactId}/unpaid-invoices`),
  getNextReceiptNumber: () => request('/credit/next-receipt-number'),
  recordPaymentIn: (body) => request('/credit/payment-in', json(body)),
  customerStatementPdfUrl: (contactId) => `${BASE}/credit/customers/${contactId}/statement/pdf?business_id=${getActiveBusinessId()}`,
  paymentReceiptPdfUrl: (paymentId) => `${BASE}/credit/payments/${paymentId}/receipt/pdf?business_id=${getActiveBusinessId()}`,
  sendCustomerStatementWhatsApp: (contactId) => request(`/credit/customers/${contactId}/statement/whatsapp`, json({})),
  sendCustomerReminderWhatsApp: (contactId) => request(`/credit/customers/${contactId}/reminder`, json({})),
  collectCreditPayment: (body) => request('/credit/collect', json(body)),

  // Credit Notes (Sales Return)
  listCreditNotes: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request(`/credit-notes${qs ? `?${qs}` : ''}`);
  },
  getCreditNote: (id) => request(`/credit-notes/${id}`),
  getNextCreditNoteNumber: () => request('/credit-notes/next-number'),
  createCreditNote: (body) => request('/credit-notes', json(body)),
  getReturnableOrderItems: (orderId) => request(`/credit-notes/orders/${orderId}/returnable-items`),
  creditNotePdfUrl: (id) => `${BASE}/credit-notes/${id}/pdf?business_id=${getActiveBusinessId()}`,
  sendCreditNoteWhatsApp: (id) => request(`/credit-notes/${id}/whatsapp`, json({})),
  voidCreditNote: (id) => request(`/credit-notes/${id}`, { method: 'DELETE' }),

  vapidPublicKey: () => request('/push/vapid-public-key'),
  subscribePush: (subscription) => request('/push/subscribe', json(subscription)),
  settingsStatus: () => request('/settings/status'),
  reconnectWhatsAppWeb: (forceReset) => request('/settings/whatsapp-web/reconnect', json({ forceReset })),
  getAppInfo: () => request('/app/info'),
};
