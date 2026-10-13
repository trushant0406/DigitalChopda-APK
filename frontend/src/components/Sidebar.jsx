import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';

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
  { key: 'download', label: 'Android App', icon: '📱' },
];

export default function Sidebar({ active, onNavigate }) {
  const { user, logout, activeBusiness, businesses, switchBusiness } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  const hasMultipleCompanies = Boolean(businesses && businesses.length > 1);

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false);
      }
    }
    if (dropdownOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [dropdownOpen]);

  return (
    <nav className="sidebar">
      <div className="sidebar-nav-top">
        <div className="brand">
          <span className="brand-mark">DC</span>
          <span className="brand-name">DigitalChopda.in</span>
        </div>

        {/* Multi-Company Switcher (Vyapar / MyBillBook style) */}
        <div className="company-switcher-wrapper" ref={dropdownRef}>
          <button
            type="button"
            className={`company-switcher-trigger ${hasMultipleCompanies ? 'clickable' : ''}`}
            onClick={() => hasMultipleCompanies && setDropdownOpen((prev) => !prev)}
            aria-expanded={dropdownOpen}
            aria-haspopup={hasMultipleCompanies ? 'menu' : undefined}
            title={hasMultipleCompanies ? 'Click to switch company' : (activeBusiness?.name || '')}
            style={{ cursor: hasMultipleCompanies ? 'pointer' : 'default' }}
          >
            <div className="company-switcher-left">
              <span className="company-switcher-badge">🏢</span>
              <div className="company-switcher-texts">
                <span className="company-switcher-name" title={activeBusiness?.name || user?.name || ''}>
                  {activeBusiness?.name || (user?.role !== 'admin' && user?.name ? user.name : 'My Business')}
                </span>
                <span className="company-switcher-tag">
                  {hasMultipleCompanies ? 'Switch Company' : 'Active Company'}
                </span>
              </div>
            </div>
            {hasMultipleCompanies && (
              <span
                className="company-switcher-caret"
                style={{
                  transform: dropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                  transition: 'transform 0.18s ease',
                  fontSize: '0.75rem',
                }}
              >
                ▼
              </span>
            )}
          </button>

          {dropdownOpen && hasMultipleCompanies && (
            <div className="company-switcher-menu" role="menu">
              <div className="company-menu-label">COMPANIES ({businesses.length})</div>
              <div className="company-menu-list">
                {businesses.map((biz) => {
                  const isActive = activeBusiness?.id === biz.id;
                  return (
                    <button
                      key={biz.id}
                      type="button"
                      className={`company-menu-item ${isActive ? 'active' : ''}`}
                      onClick={() => {
                        switchBusiness(biz.id);
                        setDropdownOpen(false);
                      }}
                      role="menuitem"
                    >
                      <div className="company-menu-item-info">
                        <span className="company-menu-item-name">{biz.name}</span>
                        <span className="company-menu-item-sub">
                          {biz.trade_name ? biz.trade_name : (biz.invoice_prefix ? `Prefix: ${biz.invoice_prefix}` : 'Active')}
                        </span>
                      </div>
                      {isActive && <span className="company-menu-item-check" aria-label="Active">✓</span>}
                    </button>
                  );
                })}
              </div>
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
  );
}
