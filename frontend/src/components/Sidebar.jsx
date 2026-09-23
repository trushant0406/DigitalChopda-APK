import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import OnboardBusinessModal from './OnboardBusinessModal';

const NAV = [
  { key: 'dashboard', label: 'Dashboard', icon: '◱' },
  { key: 'calls', label: 'Calls', icon: '☎' },
  { key: 'contacts', label: 'Contacts', icon: '☺' },
  { key: 'followups', label: 'Follow-ups', icon: '◷' },
  { key: 'orders', label: 'Orders', icon: '▣' },
  { key: 'invoices', label: 'Invoices', icon: '🧾' },
  { key: 'sales-returns', label: 'Sales Returns', icon: '↩' },
  { key: 'quotations', label: 'Quotations', icon: '▦' },
  { key: 'products', label: 'Products', icon: '▤' },
  { key: 'credit', label: 'Credit (Khata)', icon: '₹' },
  { key: 'settings', label: 'Settings', icon: '⚙' },
];

export default function Sidebar({ active, onNavigate }) {
  const { user, logout, businesses, activeBusiness, switchBusiness } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [onboardModalOpen, setOnboardModalOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false);
      }
    }
    if (dropdownOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [dropdownOpen]);

  return (
    <>
      <nav className="sidebar">
        <div className="sidebar-nav-top">
          <div className="brand">
            <span className="brand-mark">DC</span>
            <span className="brand-name">DigitalChopda.in</span>
          </div>

          {/* Multi-Tenancy Company Switcher (myBillBook / Zoho style) */}
          <div className="company-switcher-wrapper" ref={dropdownRef}>
            <button
              type="button"
              className="company-switcher-trigger"
              onClick={() => setDropdownOpen((prev) => !prev)}
              title="Click to switch business or company"
            >
              <div className="company-switcher-left">
                <span className="company-switcher-badge">🏢</span>
                <div className="company-switcher-texts">
                  <span className="company-switcher-name">
                    {activeBusiness?.name || 'My Business'}
                  </span>
                  <span className="company-switcher-tag">Active Company</span>
                </div>
              </div>
              <span className="company-switcher-caret">{dropdownOpen ? '▴' : '▾'}</span>
            </button>

            {dropdownOpen && (
              <div className="company-switcher-menu">
                <div className="company-menu-label">SWITCH COMPANY</div>
                <div className="company-menu-list">
                  {(businesses || []).map((biz) => {
                    const isActive = biz.id === activeBusiness?.id;
                    return (
                      <button
                        key={biz.id}
                        type="button"
                        className={`company-menu-item ${isActive ? 'active' : ''}`}
                        onClick={() => {
                          switchBusiness(biz.id);
                          setDropdownOpen(false);
                        }}
                      >
                        <div className="company-menu-item-info">
                          <span className="company-menu-item-name">{biz.name}</span>
                          {biz.trade_name && (
                            <span className="company-menu-item-sub">{biz.trade_name}</span>
                          )}
                        </div>
                        {isActive && <span className="company-menu-item-check">✓</span>}
                      </button>
                    );
                  })}
                </div>
                {user?.role === 'admin' && (
                  <>
                    <div className="company-menu-divider" />
                    <button
                      type="button"
                      className="company-menu-onboard-btn"
                      onClick={() => {
                        setDropdownOpen(false);
                        setOnboardModalOpen(true);
                      }}
                    >
                      <span className="company-onboard-plus">+</span> Onboard New Business
                    </button>
                  </>
                )}
              </div>
            )}
          </div>

          <ul>
            {NAV.map((item) => (
              <li key={item.key}>
                <button
                  type="button"
                  className={item.key === active ? 'nav-item active' : 'nav-item'}
                  onClick={() => onNavigate(item.key)}
                >
                  <span className="nav-icon" aria-hidden="true">{item.icon}</span>
                  {item.label}
                </button>
              </li>
            ))}
          </ul>
        </div>

        {user && (
          <div className="sidebar-footer">
            <div className="sidebar-user">
              <div className="sidebar-avatar">{user.name ? user.name[0].toUpperCase() : 'A'}</div>
              <div className="sidebar-user-details">
                <div className="sidebar-user-name">{user.name || user.username}</div>
                <div className="sidebar-user-role">@{user.username}</div>
              </div>
            </div>
            <button type="button" onClick={logout} className="sidebar-logout-btn">
              🚪 Logout
            </button>
          </div>
        )}
      </nav>

      <OnboardBusinessModal
        isOpen={onboardModalOpen}
        onClose={() => setOnboardModalOpen(false)}
      />
    </>
  );
}
