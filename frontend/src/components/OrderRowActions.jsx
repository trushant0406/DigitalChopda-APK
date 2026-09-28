import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { api } from '../api';
import { NEXT_FULFILLMENT, NEXT_FULFILLMENT_LABEL } from '../orderStatus';
import { MoreVerticalIcon, FileTextIcon, WhatsAppIcon } from './Icons';

export default function OrderRowActions({
  order,
  onSettle,
  onChanged,
  handleSendWhatsApp,
  sendingWhatsappId,
  advanceFulfillment,
  cancelOrder,
  onIssueCreditNote,
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
    const menuWidth = 240;
    const left = Math.max(10, Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 10));

    if (spaceBelow < 300 && spaceAbove > spaceBelow) {
      const maxHeight = Math.max(160, Math.min(spaceAbove - 16, 380));
      setStyle({
        bottom: `${window.innerHeight - rect.top + 6}px`,
        left: `${left}px`,
        maxHeight: `${maxHeight}px`,
        width: `${menuWidth}px`,
      });
    } else {
      const maxHeight = Math.max(160, Math.min(spaceBelow - 16, 380));
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

  return (
    <div className="order-actions-cell" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', justifyContent: 'flex-end', whiteSpace: 'nowrap' }}>
      {/* 1. TAX INVOICE PDF ICON */}
      <a
        href={api.orderPdfUrl(order.id)}
        target="_blank"
        rel="noreferrer"
        title="Download Tax Invoice PDF"
        style={{
          background: 'transparent',
          color: 'var(--accent, #2563eb)',
          border: '1px solid var(--border, #cbd5e1)',
          width: '28px',
          height: '26px',
          borderRadius: '5px',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          textDecoration: 'none',
          cursor: 'pointer',
        }}
      >
        <FileTextIcon size={14} color="#2563eb" />
      </a>

      {/* 1b. PRINT TRANSPORT PARCEL LABEL (1/3 A4) */}
      <a
        href={api.orderShippingLabelUrl(order.id)}
        target="_blank"
        rel="noreferrer"
        title="Print Shipping Label"
        style={{
          background: '#f0fdfa',
          color: '#0f766e',
          border: '1px solid #99f6e4',
          width: '28px',
          height: '26px',
          borderRadius: '5px',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: '12px',
          textDecoration: 'none',
          cursor: 'pointer',
        }}
      >
        🏷️
      </a>

      {/* 2. SEND TAX INVOICE VIA WHATSAPP ICON */}
      <button
        type="button"
        onClick={() => handleSendWhatsApp(order)}
        disabled={sendingWhatsappId === order.id}
        title="Send Tax Invoice via WhatsApp"
        style={{
          background: '#f0fdf4',
          border: '1px solid #bbf7d0',
          width: '28px',
          height: '26px',
          borderRadius: '5px',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          padding: 0,
        }}
      >
        {sendingWhatsappId === order.id ? (
          <span style={{ fontSize: '11px', color: '#16a34a' }}>…</span>
        ) : (
          <WhatsAppIcon size={14} color="#16a34a" />
        )}
      </button>

      {/* 3. QUICK SETTLE BUTTON (IF UNPAID / PARTIAL) */}
      {isUnpaid && onSettle && (
        <button
          type="button"
          onClick={() => onSettle(order)}
          title={`Record Payment / Settle (${order.invoice_number || order.order_number})`}
          style={{
            background: '#ecfdf5',
            color: '#047857',
            border: '1px solid #6ee7b7',
            fontWeight: 800,
            fontSize: '13px',
            width: '28px',
            height: '26px',
            borderRadius: '5px',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            padding: 0,
          }}
        >
          ₹
        </button>
      )}

      {/* 4. MORE ACTIONS TRIGGER (•••) */}
      <button
        ref={triggerRef}
        type="button"
        onClick={() => (open ? setOpen(false) : openMenu())}
        title="More Actions"
        aria-label="More actions"
        style={{
          background: open ? 'var(--hover-bg, #f1f5f9)' : 'transparent',
          border: '1px solid var(--border, #cbd5e1)',
          borderRadius: '5px',
          width: '28px',
          height: '26px',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          color: 'var(--muted, #64748b)',
          padding: 0,
        }}
      >
        <MoreVerticalIcon size={14} />
      </button>

      {/* DROPDOWN MENU PORTAL */}
      {open &&
        createPortal(
          <div ref={panelRef} className="payment-menu-panel" style={{ ...style, zIndex: 9999 }}>
            <div style={{ padding: '4px 8px 6px', borderBottom: '1px solid var(--border)', marginBottom: '4px' }}>
              <div style={{ fontSize: '11px', fontWeight: 700, color: 'var(--muted)' }}>
                Tax Invoice: {order.invoice_number || order.order_number}
              </div>
              {order.name && (
                <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {order.name}
                </div>
              )}
            </div>

            <div className="payment-menu-label">Tax Invoice Documents</div>
            <a
              className="payment-menu-item"
              href={api.orderPdfUrl(order.id)}
              target="_blank"
              rel="noreferrer"
              onClick={() => setOpen(false)}
              style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none' }}
            >
              <span>📄</span> Download Tax Invoice (Standard)
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
                <span>🧾</span> Download Tax Invoice + Payment History
              </a>
            )}

            <a
              className="payment-menu-item"
              href={api.orderShippingLabelUrl(order.id)}
              target="_blank"
              rel="noreferrer"
              onClick={() => setOpen(false)}
              style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none', color: '#0f766e', fontWeight: 600 }}
            >
              <span>🏷️</span> Print Shipping Label
            </a>

            {/* PAYMENT OPTIONS */}
            {onSettle && isUnpaid && (
              <>
                <div className="payment-menu-divider" />
                <div className="payment-menu-label">Payments & Settle</div>
                <button
                  type="button"
                  className="payment-menu-item"
                  onClick={() => {
                    setOpen(false);
                    onSettle(order);
                  }}
                  style={{ color: '#15803d', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}
                >
                  <span>₹</span> Record Payment In (Settle)
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
                  <span>🚚</span> {NEXT_FULFILLMENT_LABEL[order.fulfillment_status]}
                </button>
              </>
            )}

            {/* SALES RETURN & CREDIT NOTE */}
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
                  <span>↩</span> Issue Credit Note / Return
                </button>
              </>
            )}

            {/* DANGER: CANCEL */}
            <div className="payment-menu-divider" />
            <button
              type="button"
              className="payment-menu-item"
              style={{ color: '#dc2626', display: 'flex', alignItems: 'center', gap: '8px' }}
              onClick={() => {
                setOpen(false);
                cancelOrder(order);
              }}
            >
              <span>✕</span> Cancel Order
            </button>
          </div>,
          document.body
        )}
    </div>
  );
}
