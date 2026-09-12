import { useState } from 'react';
import Sidebar from './components/Sidebar';
import DashboardPage from './pages/DashboardPage';
import CallsPage from './pages/CallsPage';
import ContactsPage from './pages/ContactsPage';
import FollowupsPage from './pages/FollowupsPage';
import OrdersPage from './pages/OrdersPage';
import ProductsPage from './pages/ProductsPage';
import QuotationsPage from './pages/QuotationsPage';
import CreditPage from './pages/CreditPage';
import SettingsPage from './pages/SettingsPage';
import './App.css';

export default function App() {
  const [page, setPage] = useState('dashboard');
  const [refreshKey, setRefreshKey] = useState(0);
  const [ordersFilter, setOrdersFilter] = useState(null);
  const [quotationPresetContact, setQuotationPresetContact] = useState(null);

  const bump = () => setRefreshKey((k) => k + 1);

  function goToOrders(filter) {
    setOrdersFilter(filter || null);
    setPage('orders');
  }

  function goToQuotationFor(contact) {
    setQuotationPresetContact(contact);
    setPage('quotations');
  }

  function navigate(key) {
    if (key !== 'orders') setOrdersFilter(null);
    if (key !== 'quotations') setQuotationPresetContact(null);
    setPage(key);
  }

  return (
    <div className="shell">
      <Sidebar active={page} onNavigate={navigate} />
      <main className="content">
        {page === 'dashboard' && <DashboardPage refreshKey={refreshKey} onViewOrders={goToOrders} />}
        {page === 'calls' && <CallsPage refreshKey={refreshKey} onLogged={bump} />}
        {page === 'contacts' && <ContactsPage refreshKey={refreshKey} onChanged={bump} onCreateQuotation={goToQuotationFor} />}
        {page === 'followups' && <FollowupsPage refreshKey={refreshKey} />}
        {page === 'orders' && <OrdersPage refreshKey={refreshKey} onLogged={bump} initialFilter={ordersFilter} />}
        {page === 'products' && <ProductsPage />}
        {page === 'quotations' && <QuotationsPage presetContact={quotationPresetContact} />}
        {page === 'credit' && <CreditPage refreshKey={refreshKey} />}
        {page === 'settings' && <SettingsPage />}
      </main>
    </div>
  );
}
