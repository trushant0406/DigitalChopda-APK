import { useAuth } from '../context/AuthContext';

const NAV = [
  { key: 'dashboard', label: 'Dashboard', icon: '◱' },
  { key: 'calls', label: 'Calls', icon: '☎' },
  { key: 'contacts', label: 'Contacts', icon: '☺' },
  { key: 'followups', label: 'Follow-ups', icon: '◷' },
  { key: 'orders', label: 'Orders', icon: '▣' },
  { key: 'products', label: 'Products', icon: '▤' },
  { key: 'quotations', label: 'Quotations', icon: '▦' },
  { key: 'credit', label: 'Credit (Khata)', icon: '₹' },
  { key: 'settings', label: 'Settings', icon: '⚙' },
];

export default function Sidebar({ active, onNavigate }) {
  const { user, logout } = useAuth();

  return (
    <nav className="sidebar">
      <div className="sidebar-nav-top">
        <div className="brand">
          <span className="brand-mark">CI</span>
          <span className="brand-name">Call Intelligence</span>
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
