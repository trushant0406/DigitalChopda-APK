import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [businesses, setBusinesses] = useState([]);
  const [activeBusiness, setActiveBusiness] = useState(null);
  const [loading, setLoading] = useState(true);

  const resolveActiveBusiness = useCallback((bizList, preferredId) => {
    if (!bizList || bizList.length === 0) return null;
    const targetId = Number(preferredId);
    const found = bizList.find((b) => b.id === targetId);
    return found || bizList[0] || null;
  }, []);

  const refreshBusinesses = useCallback(async () => {
    try {
      const bizList = await api.listBusinesses();
      setBusinesses(bizList);
      const currentStoredId = localStorage.getItem('ci_business_id');
      const active = resolveActiveBusiness(bizList, currentStoredId);
      if (active) {
        localStorage.setItem('ci_business_id', String(active.id));
        setActiveBusiness(active);
      }
      return bizList;
    } catch (err) {
      console.warn('Failed to refresh businesses:', err.message);
      return [];
    }
  }, [resolveActiveBusiness]);

  useEffect(() => {
    async function checkAuth() {
      const token = localStorage.getItem('ci_token');
      if (token) {
        try {
          const res = await api.getMe();
          setUser(res.user);
          const bizList = res.businesses || [];
          setBusinesses(bizList);

          const storedBizId = localStorage.getItem('ci_business_id');
          const currentActive = resolveActiveBusiness(bizList, storedBizId) || res.activeBusiness || bizList[0] || null;
          if (currentActive) {
            localStorage.setItem('ci_business_id', String(currentActive.id));
            setActiveBusiness(currentActive);
          }
        } catch (err) {
          console.error('Session expired or invalid:', err.message);
          localStorage.removeItem('ci_token');
          setUser(null);
          setBusinesses([]);
          setActiveBusiness(null);
        }
      }
      setLoading(false);
    }
    checkAuth();
  }, [resolveActiveBusiness]);

  async function login(username, password) {
    const res = await api.login(username, password);
    localStorage.setItem('ci_token', res.token);
    setUser(res.user);

    const bizList = res.businesses || [];
    setBusinesses(bizList);
    const storedBizId = localStorage.getItem('ci_business_id');
    const active = resolveActiveBusiness(bizList, storedBizId) || res.activeBusiness || bizList[0] || null;
    if (active) {
      localStorage.setItem('ci_business_id', String(active.id));
      setActiveBusiness(active);
    }
    return res;
  }

  function switchBusiness(businessId) {
    const target = businesses.find((b) => b.id === Number(businessId));
    if (target) {
      localStorage.setItem('ci_business_id', String(target.id));
      setActiveBusiness(target);
      // Dispatch custom event so listening components can react immediately without full page reload
      window.dispatchEvent(new CustomEvent('business-switched', { detail: target }));
    }
  }

  function logout() {
    localStorage.removeItem('ci_token');
    localStorage.removeItem('ci_business_id');
    setUser(null);
    setBusinesses([]);
    setActiveBusiness(null);
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        logout,
        businesses,
        activeBusiness,
        switchBusiness,
        refreshBusinesses,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
