import { createContext, useContext, useEffect, useState } from 'react';
import { api } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function checkAuth() {
      const token = localStorage.getItem('ci_token');
      if (token) {
        try {
          const res = await api.getMe();
          setUser(res.user);
        } catch (err) {
          console.error('Session expired or invalid:', err.message);
          localStorage.removeItem('ci_token');
          setUser(null);
        }
      }
      setLoading(false);
    }
    checkAuth();
  }, []);

  async function login(username, password) {
    const res = await api.login(username, password);
    localStorage.setItem('ci_token', res.token);
    setUser(res.user);
    return res.user;
  }

  function logout() {
    localStorage.removeItem('ci_token');
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
