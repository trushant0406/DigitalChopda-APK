import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { api } from '../api';
import { NEXT_FULFILLMENT, NEXT_FULFILLMENT_LABEL } from '../orderStatus';
import {
  MoreVerticalIcon,
  FileTextIcon,
  WhatsAppIcon,
  EditIcon,
  TagIcon,
  TruckIcon,
  TrashIcon,
  CreditNoteIcon,
  PaymentInIcon,
} from './Icons';

export default function OrderRowActions({
  order,
  onSettle,
  onChanged,
  handleSendWhatsApp,
  sendingWhatsappId,
  advanceFulfillment,
  cancelOrder,
  onIssueCreditNote,
  onEditOrder,
  onDeleteOrder,
}) {
  const [open, setOpen] = useState(false);
  const [style, setStyle] = useState({});
  const triggerRef = useRef(null);
  const panelRef = useRef(null);

  if (order.status === 'cancelled') {
    return <span className="badge neutral">Cancelled</span>;
  }

  const isUnpaid = order.payment_status === 'pending' || order.payment_status === 'partially_paid';

  function openMenu() {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    const spaceBelow = window.innerHeight - rect.bottom;
    const spaceAbove = rect.top;
    const menuWidth = 260;
    const left = Math.max(10, Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 10));

    if (spaceBelow < 320 && spaceAbove > spaceBelow) {
      const maxHeight = Math.max(180, Math.min(spaceAbove - 16, 420));
      setStyle({
        bottom: `${window.innerHeight - rect.top + 6}px`,
        left: `${left}px`,
        maxHeight: `${maxHeight}px`,
        width: `${menuWidth}px`,
      });
    } else {
      const maxHeight = Math.max(180, Math.min(spaceBelow - 16, 420));
      setStyle({
        top: `${rect.bottom + 6}px`,
        left: `${left}px`,
        maxHeight: `${maxHeight}px`,
        width: `${menuWidth}px`,
      });
    }
    setOpen(true);
  }

  useEffect(() => {
    if (!open) return;
    function handlePointerDown(e) {
      if (triggerRef.current?.contains(e.target) || panelRef.current?.contains(e.target)) return;
      setOpen(false);
    }
    function handleKey(e) {
      if (e.key === 'Escape') setOpen(false);
    }
    function handleScroll(e) {
      if (panelRef.current?.contains(e.target)) return;
      setOpen(false);
    }
    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKey);
    window.addEventListener('scroll', handleScroll, true);
    window.addEventListener('resize', handleScroll);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKey);
      window.removeEventListener('scroll', handleScroll, true);
      window.removeEventListener('resize', handleScroll);
    };
  }, [open]);

  async function markPaid(method) {
    setOpen(false);
    await api.updateOrder(order.id, { payment_status: 'paid', payment_method: method });
    onChanged();
  }

  const baseBtnStyle = {
    background: '#ffffff',
    color: '#475569',
    border: '1px solid var(--border, #e2e8f0)',
    width: '30px',
    height: '28px',
    borderRadius: '6px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: 'pointer',
    textDecoration: 'none',
    padding: 0,
    transition: 'all 0.15s ease',
    boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)',
  };

  return (
    <div
      className="order-actions-cell"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        justifyContent: 'flex-end',
        whiteSpace: 'nowrap',
      }}
    >
      {/* 1. EDIT INVOICE (CLEAN ENTERPRISE OUTLINE) */}
      {onEditOrder && (
        <button
          type="button"
          onClick={() => onEditOrder(order)}
          title={`Edit Invoice (${order.invoice_number || order.order_number})`}
          style={{
            ...baseBtnStyle,
            color: '#1d4ed8',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = '#eff6ff';
            e.currentTarget.style.borderColor = '#bfdbfe';
            e.currentTarget.style.color = '#1e40af';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = '#ffffff';
            e.currentTarget.style.borderColor = 'var(--border, #e2e8f0)';
            e.currentTarget.style.color = '#1d4ed8';
          }}
        >
          <EditIcon size={14} color="#1d4ed8" />
        </button>
      )}

      {/* 2. TAX INVOICE PDF DOWNLOAD (UNIFORM ENTERPRISE OUTLINE) */}
      <a
        href={api.orderPdfUrl(order.id)}
        target="_blank"
        rel="noreferrer"
        title="Download Tax Invoice PDF"
        style={{
          ...baseBtnStyle,
          color: '#2563eb',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.background = '#eff6ff';
          e.currentTarget.style.borderColor = '#bfdbfe';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.background = '#ffffff';
          e.currentTarget.style.borderColor = 'var(--border, #e2e8f0)';
        }}
      >
        <FileTextIcon size={14} color="#2563eb" />
      </a>

      {/* 2. SEND TAX INVOICE VIA WHATSAPP (UNIFORM ENTERPRISE OUTLINE) */}
      <button
        type="button"
        onClick={() => handleSendWhatsApp(order)}
        disabled={sendingWhatsappId === order.id}
        title="Send Invoice via WhatsApp"
        style={{
          ...baseBtnStyle,
          color: '#16a34a',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.background = '#f0fdf4';
          e.currentTarget.style.borderColor = '#bbf7d0';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.background = '#ffffff';
          e.currentTarget.style.borderColor = 'var(--border, #e2e8f0)';
        }}
      >
        {sendingWhatsappId === order.id ? (
          <span style={{ fontSize: '11px', color: '#16a34a' }}>…</span>
        ) : (
          <WhatsAppIcon size={14} color="#16a34a" />
        )}
      </button>

      {/* 3. CONTEXTUAL MORE OPTIONS (•••) */}
      <button
        ref={triggerRef}
        type="button"
        onClick={() => (open ? setOpen(false) : openMenu())}
        title="More Actions"
        aria-label="More actions"
        style={{
          ...baseBtnStyle,
          background: open ? 'var(--hover-bg, #f1f5f9)' : '#ffffff',
          borderColor: open ? '#cbd5e1' : 'var(--border, #e2e8f0)',
          color: open ? '#0f172a' : '#64748b',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.background = '#f8fafc';
          e.currentTarget.style.borderColor = '#cbd5e1';
          e.currentTarget.style.color = '#0f172a';
        }}
        onMouseLeave={(e) => {
          if (!open) {
            e.currentTarget.style.background = '#ffffff';
            e.currentTarget.style.borderColor = 'var(--border, #e2e8f0)';
            e.currentTarget.style.color = '#64748b';
          }
        }}
      >
        <MoreVerticalIcon size={15} />
      </button>

      {/* DROPDOWN MENU PORTAL (CLEAN ENTERPRISE LAYOUT) */}
      {open &&
        createPortal(
          <div ref={panelRef} className="payment-menu-panel" style={{ ...style, zIndex: 9999 }}>
            {/* INVOICE & CUSTOMER HEADER */}
            <div style={{ padding: '6px 10px 8px', borderBottom: '1px solid var(--border, #e2e8f0)', marginBottom: '4px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '6px' }}>
                <span style={{ fontSize: '11.5px', fontWeight: 700, color: 'var(--color-primary, #1d4ed8)' }}>
                  {order.invoice_number || order.order_number}
                </span>
                <span
                  style={{
                    fontSize: '10.5px',
                    fontWeight: 600,
                    textTransform: 'capitalize',
                    padding: '1px 5px',
                    borderRadius: '4px',
                    background: order.payment_status === 'paid' ? '#dcfce7' : '#fef3c7',
                    color: order.payment_status === 'paid' ? '#15803d' : '#92400e',
                  }}
                >
                  {order.payment_status === 'partially_paid' ? 'Partially Paid' : order.payment_status}
                </span>
              </div>
              {order.name && (
                <div style={{ fontSize: '12px', fontWeight: 600, color: '#334155', marginTop: '2px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {order.name}
                </div>
              )}
            </div>

            {/* PRIMARY ACTIONS: EDIT & SHIPPING LABEL */}
            {onEditOrder && (
              <button
                type="button"
                className="payment-menu-item"
                onClick={() => {
                  setOpen(false);
                  onEditOrder(order);
                }}
                style={{ color: '#1d4ed8', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
              >
                <EditIcon size={14} color="#1d4ed8" />
                <span>Edit Invoice Details</span>
              </button>
            )}

            <a
              className="payment-menu-item"
              href={api.orderShippingLabelUrl(order.id)}
              target="_blank"
              rel="noreferrer"
              onClick={() => setOpen(false)}
              style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none', color: '#0f766e', fontWeight: 500 }}
            >
              <TagIcon size={14} color="#0f766e" />
              <span>Print Shipping Label (1/3 A4)</span>
            </a>

            {/* DOCUMENTS */}
            <div className="payment-menu-divider" />
            <div className="payment-menu-label">Tax Invoice Documents</div>

            <a
              className="payment-menu-item"
              href={api.orderPdfUrl(order.id)}
              target="_blank"
              rel="noreferrer"
              onClick={() => setOpen(false)}
              style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none' }}
            >
              <FileTextIcon size={14} color="#2563eb" />
              <span>Download Invoice (Standard)</span>
            </a>

            {(order.payment_status === 'paid' || order.payment_status === 'partially_paid') && (
              <a
                className="payment-menu-item"
                href={api.orderPdfUrl(order.id, true)}
                target="_blank"
                rel="noreferrer"
                onClick={() => setOpen(false)}
                style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none' }}
              >
                <span style={{ fontSize: '13px' }}>🧾</span>
                <span>Invoice + Payment History</span>
              </a>
            )}

            {/* PAYMENTS & SETTLEMENT */}
            {onSettle && isUnpaid && (
              <>
                <div className="payment-menu-divider" />
                <div className="payment-menu-label">Payments &amp; Settlement</div>
                <button
                  type="button"
                  className="payment-menu-item"
                  onClick={() => {
                    setOpen(false);
                    onSettle(order);
                  }}
                  style={{ color: '#15803d', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
                >
                  <PaymentInIcon size={14} color="#15803d" />
                  <span>Record Payment In (Settle)</span>
                </button>
              </>
            )}

            {order.payment_status === 'pending' && (
              <>
                <div className="payment-menu-label" style={{ marginTop: '4px' }}>Quick Mark Full Paid</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4px', padding: '0 4px' }}>
                  <button
                    type="button"
                    className="payment-menu-item"
                    style={{ fontSize: '11px', padding: '5px 6px', textAlign: 'center', justifyContent: 'center' }}
                    onClick={() => markPaid('cash')}
                  >
                    💵 Cash
                  </button>
                  <button
                    type="button"
                    className="payment-menu-item"
                    style={{ fontSize: '11px', padding: '5px 6px', textAlign: 'center', justifyContent: 'center' }}
                    onClick={() => markPaid('upi')}
                  >
                    ⚡ UPI
                  </button>
                  <button
                    type="button"
                    className="payment-menu-item"
                    style={{ fontSize: '11px', padding: '5px 6px', textAlign: 'center', justifyContent: 'center' }}
                    onClick={() => markPaid('bank_transfer')}
                  >
                    🏦 Bank
                  </button>
                  <button
                    type="button"
                    className="payment-menu-item"
                    style={{ fontSize: '11px', padding: '5px 6px', textAlign: 'center', justifyContent: 'center' }}
                    onClick={() => markPaid('card')}
                  >
                    💳 Card
                  </button>
                </div>
              </>
            )}

            {/* FULFILLMENT */}
            {NEXT_FULFILLMENT[order.fulfillment_status] && (
              <>
                <div className="payment-menu-divider" />
                <div className="payment-menu-label">Order Fulfillment</div>
                <button
                  type="button"
                  className="payment-menu-item"
                  onClick={() => {
                    setOpen(false);
                    advanceFulfillment(order);
                  }}
                  style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
                >
                  <TruckIcon size={14} color="#475569" />
                  <span>{NEXT_FULFILLMENT_LABEL[order.fulfillment_status]}</span>
                </button>
              </>
            )}

            {/* RETURNS & CREDIT NOTES */}
            {onIssueCreditNote && (
              <>
                <div className="payment-menu-divider" />
                <div className="payment-menu-label">Returns &amp; Credit Note</div>
                <button
                  type="button"
                  className="payment-menu-item"
                  style={{ color: '#7c3aed', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
                  onClick={() => {
                    setOpen(false);
                    onIssueCreditNote(order);
                  }}
                >
                  <CreditNoteIcon size={14} color="#7c3aed" />
                  <span>Issue Credit Note / Return</span>
                </button>
              </>
            )}

            {/* DANGER ZONE: DELETE & CANCEL */}
            <div className="payment-menu-divider" />
            {onDeleteOrder && (
              <button
                type="button"
                className="payment-menu-item"
                style={{ color: '#dc2626', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
                onClick={() => {
                  setOpen(false);
                  onDeleteOrder(order);
                }}
              >
                <TrashIcon size={14} color="#dc2626" />
                <span>Delete Invoice Completely</span>
              </button>
            )}

            <button
              type="button"
              className="payment-menu-item"
              style={{ color: '#64748b', display: 'flex', alignItems: 'center', gap: '8px' }}
              onClick={() => {
                setOpen(false);
                cancelOrder(order);
              }}
            >
              <span style={{ fontSize: '12px' }}>✕</span>
              <span>Cancel Order</span>
            </button>
          </div>,
          document.body
        )}
    </div>
  );
}
