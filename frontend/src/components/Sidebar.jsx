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
];

export default function Sidebar({ active, onNavigate }) {
  const { user, logout, activeBusiness } = useAuth();

  return (
    <nav className="sidebar">
      <div className="sidebar-nav-top">
        <div className="brand">
          <span className="brand-mark">DC</span>
          <span className="brand-name">DigitalChopda.in</span>
        </div>

        {/* Active Business Badge (Static display — no tenant switching) */}
        <div className="company-switcher-wrapper">
          <div className="company-switcher-trigger" style={{ cursor: 'default' }}>
            <div className="company-switcher-left">
              <span className="company-switcher-badge">🏢</span>
              <div className="company-switcher-texts">
                <span className="company-switcher-name" title={activeBusiness?.name || user?.name || ''}>
                  {activeBusiness?.name || (user?.role !== 'admin' && user?.name ? user.name : 'My Business')}
                </span>
                <span className="company-switcher-tag">Active Company</span>
              </div>
            </div>
          </div>
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
