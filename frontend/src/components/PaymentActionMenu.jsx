import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { api } from '../api';
import { PAYMENT_METHODS } from '../paymentMethods';

export default function PaymentActionMenu({ order, onDone }) {
  const [open, setOpen] = useState(false);
  const [style, setStyle] = useState({});
  const triggerRef = useRef(null);
  const panelRef = useRef(null);
  const isCredit = order.payment_method === 'credit';

  function openMenu() {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    const spaceBelow = window.innerHeight - rect.bottom;
    const spaceAbove = rect.top;
    const menuWidth = 220;
    const left = Math.max(10, Math.min(rect.left, window.innerWidth - menuWidth - 10));

    if (spaceBelow < 280 && spaceAbove > spaceBelow) {
      const maxHeight = Math.max(150, Math.min(spaceAbove - 16, 320));
      setStyle({
        bottom: `${window.innerHeight - rect.top + 6}px`,
        left: `${left}px`,
        maxHeight: `${maxHeight}px`,
      });
    } else {
      const maxHeight = Math.max(150, Math.min(spaceBelow - 16, 320));
      setStyle({
        top: `${rect.bottom + 6}px`,
        left: `${left}px`,
        maxHeight: `${maxHeight}px`,
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
    onDone();
  }

  async function toggleCredit() {
    setOpen(false);
    await api.updateOrder(order.id, { payment_method: isCredit ? null : 'credit' });
    onDone();
  }

  return (
    <>
      <button
        type="button"
        ref={triggerRef}
        className="advance-action payment-menu-trigger"
        onClick={() => (open ? setOpen(false) : openMenu())}
      >
        Payment <span className="payment-menu-caret">▾</span>
      </button>
      {open && createPortal(
        <div ref={panelRef} className="payment-menu-panel" style={style}>
          <p className="payment-menu-label">Mark paid via</p>
          {PAYMENT_METHODS.map((m) => (
            <button key={m.value} type="button" className="payment-menu-item" onClick={() => markPaid(m.value)}>
              {m.label}
            </button>
          ))}
          <div className="payment-menu-divider" />
          <button type="button" className="payment-menu-item payment-menu-item-credit" onClick={toggleCredit}>
            {isCredit ? 'Remove from credit (Khata)' : 'Move to credit (Khata)'}
          </button>
        </div>,
        document.body
      )}
    </>
  );
}
