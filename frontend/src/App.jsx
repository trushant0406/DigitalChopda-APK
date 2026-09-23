import { useState, useEffect } from 'react';
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import Sidebar from './components/Sidebar';
import DashboardPage from './pages/DashboardPage';
import CallsPage from './pages/CallsPage';
import ContactsPage from './pages/ContactsPage';
import FollowupsPage from './pages/FollowupsPage';
import OrdersPage from './pages/OrdersPage';
import InvoicesPage from './pages/InvoicesPage';
import SalesReturnsPage from './pages/SalesReturnsPage';
import ProductsPage from './pages/ProductsPage';
import QuotationsPage from './pages/QuotationsPage';
import CreditPage from './pages/CreditPage';
import SettingsPage from './pages/SettingsPage';
import './App.css';

function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [refreshKey, setRefreshKey] = useState(0);
  const [ordersFilter, setOrdersFilter] = useState(null);
  const [quotationPresetContact, setQuotationPresetContact] = useState(null);

  const bump = () => setRefreshKey((k) => k + 1);

  useEffect(() => {
    function handleBizSwitch() {
      bump();
    }
    window.addEventListener('business-switched', handleBizSwitch);
    return () => window.removeEventListener('business-switched', handleBizSwitch);
  }, []);

  const path = location.pathname.substring(1);
  const activeTab = path || 'dashboard';

  function goToOrders(filter) {
    setOrdersFilter(filter || null);
    navigate('/orders');
  }

  function goToQuotationFor(contact) {
    setQuotationPresetContact(contact);
    navigate('/quotations');
  }

  function handleNavigate(key) {
    if (key !== 'orders') setOrdersFilter(null);
    if (key !== 'quotations') setQuotationPresetContact(null);
    navigate(key === 'dashboard' ? '/' : `/${key}`);
  }

  return (
    <div className="shell">
      <Sidebar active={activeTab} onNavigate={handleNavigate} />
      <main className="content">
        <Routes>
          <Route path="/" element={<DashboardPage refreshKey={refreshKey} onViewOrders={goToOrders} />} />
          <Route path="/dashboard" element={<DashboardPage refreshKey={refreshKey} onViewOrders={goToOrders} />} />
          <Route path="/calls" element={<CallsPage refreshKey={refreshKey} onLogged={bump} />} />
          <Route path="/contacts" element={<ContactsPage refreshKey={refreshKey} onChanged={bump} onCreateQuotation={goToQuotationFor} />} />
          <Route path="/followups" element={<FollowupsPage refreshKey={refreshKey} />} />
          <Route path="/orders" element={<OrdersPage refreshKey={refreshKey} onLogged={bump} initialFilter={ordersFilter} />} />
          <Route path="/invoices" element={<InvoicesPage refreshKey={refreshKey} />} />
          <Route path="/sales-returns" element={<SalesReturnsPage refreshKey={refreshKey} />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/quotations" element={<QuotationsPage presetContact={quotationPresetContact} />} />
          <Route path="/credit" element={<CreditPage refreshKey={refreshKey} />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Routes>
      </main>
    </div>
  );
}

function AppContent() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="app-loading-screen">
        <div className="brand-mark">DC</div>
        <p>Loading DigitalChopda.in…</p>
      </div>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  return <MainLayout />;
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
