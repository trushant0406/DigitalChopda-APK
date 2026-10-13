import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!username || !password) return;
    setLoading(true);
    setError(null);
    try {
      await login(username, password);
    } catch (err) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">DC</div>
          <h1>DigitalChopda.in</h1>
          <p className="login-subtitle">Smart Billing, Invoicing &amp; Khata</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {error && <div className="login-error-banner">{error}</div>}

          <label>
            Username
            <input
              type="text"
              required
              autoFocus
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>

          <label>
            Password
            <input
              type="password"
              required
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>

          <button type="submit" disabled={loading} className="login-submit-btn">
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="login-footer-apk" style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid #e2e8f0', textAlign: 'center' }}>
          <a
            href="/download"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              color: '#3f5efb',
              fontWeight: 600,
              fontSize: '0.88rem',
              textDecoration: 'none',
              padding: '6px 12px',
              borderRadius: '6px',
              transition: 'background 0.15s ease'
            }}
          >
            <span>📱</span> Download Android APK
          </a>
        </div>
      </div>
    </div>
  );
}
