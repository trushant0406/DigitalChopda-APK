import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import './DownloadApkPage.css';

export default function DownloadApkPage({ isPublic = false }) {
  const navigate = useNavigate();
  const [appInfo, setAppInfo] = useState({
    appName: 'DigitalChopda Android Client',
    version: '1.0.0',
    sizeMb: '20.0 MB',
    directApkUrl: '/apk/DigitalChopda.apk',
    apiApkUrl: '/api/app/apk',
    tailscaleServerUrl: 'http://100.99.250.0:4000/api/',
    qrDataUrl: '',
    updatedAt: new Date().toISOString(),
  });
  const [_loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let mounted = true;
    api
      .getAppInfo()
      .then((info) => {
        if (mounted && info) {
          setAppInfo((prev) => ({ ...prev, ...info }));
        }
      })
      .catch((err) => {
        console.warn('Failed to fetch dynamic app info, using fallback static config:', err);
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, []);

  const downloadUrl = window.location.origin + (appInfo.directApkUrl || '/apk/DigitalChopda.apk');

  function handleCopyLink() {
    navigator.clipboard?.writeText(downloadUrl).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    });
  }

  return (
    <div className={`apk-page-container ${isPublic ? 'public-view' : ''}`}>
      {isPublic && (
        <header className="public-top-nav">
          <div className="public-brand" onClick={() => navigate('/')} role="button" tabIndex={0}>
            <span className="brand-mark">DC</span>
            <span className="brand-name">DigitalChopda.in</span>
          </div>
          <button type="button" className="btn-secondary" onClick={() => navigate('/login')}>
            Web Login →
          </button>
        </header>
      )}

      <div className="apk-content-wrapper">
        {!isPublic && (
          <div className="apk-back-nav">
            <button type="button" className="btn-back" onClick={() => navigate('/')}>
              ← Back to Dashboard
            </button>
          </div>
        )}

        {/* Hero Section */}
        <section className="apk-hero-card">
          <div className="apk-hero-badge">
            <span className="pulse-dot"></span>
            Android Production Build • v{appInfo.version}
          </div>

          <div className="apk-hero-header">
            <div className="apk-icon-box">
              <span className="apk-robot-icon">📱</span>
            </div>
            <div className="apk-hero-text">
              <h1 className="apk-title">DigitalChopda for Android</h1>
              <p className="apk-subtitle">
                High-speed offline-first point of sale, GST tax invoicing, customer khata ledger &amp; wireless Bluetooth thermal printing.
              </p>
            </div>
          </div>

          {/* Action Row */}
          <div className="apk-cta-group">
            <a
              href={appInfo.directApkUrl || '/apk/DigitalChopda.apk'}
              download="DigitalChopda.apk"
              className="btn-download-primary"
            >
              <span className="btn-icon">⬇️</span>
              <div className="btn-labels">
                <span className="btn-main-label">Download Android APK</span>
                <span className="btn-sub-label">Direct High-Speed Download • {appInfo.sizeMb || '~20 MB'}</span>
              </div>
            </a>

            <button type="button" className="btn-copy-link" onClick={handleCopyLink}>
              {copied ? '✓ Link Copied!' : '🔗 Copy Download Link'}
            </button>
          </div>

          <div className="apk-meta-bar">
            <div className="meta-item">
              <span className="meta-label">Package Name</span>
              <span className="meta-value">com.digitalchopda.app</span>
            </div>
            <div className="meta-item">
              <span className="meta-label">Compatibility</span>
              <span className="meta-value">Android 8.0 to Android 15+</span>
            </div>
            <div className="meta-item">
              <span className="meta-label">Central Server</span>
              <span className="meta-value font-mono">100.99.250.0:4000 (Tailscale)</span>
            </div>
            <div className="meta-item">
              <span className="meta-label">Security</span>
              <span className="meta-value text-success">✓ Verified &amp; Signed</span>
            </div>
          </div>
        </section>

        {/* QR Code & Scan Section */}
        <section className="apk-qr-section">
          <div className="apk-qr-card">
            <div className="qr-wrapper">
              {appInfo.qrDataUrl ? (
                <img src={appInfo.qrDataUrl} alt="Scan QR Code to download APK on phone" className="qr-image" />
              ) : (
                <div className="qr-placeholder">
                  <span>📱</span>
                  <small>Loading QR Code…</small>
                </div>
              )}
            </div>
            <div className="qr-instructions">
              <h3>Scan from your Android Phone</h3>
              <p>
                Open your smartphone camera or Google Lens and scan this QR code to download and install the app directly on your device.
              </p>
              <div className="qr-link-box font-mono">
                {downloadUrl}
              </div>
            </div>
          </div>
        </section>

        {/* 3 Quick Installation Steps */}
        <section className="apk-steps-section">
          <h2 className="section-heading">How to Install in 3 Easy Steps</h2>
          <div className="steps-grid">
            <div className="step-card">
              <div className="step-number">1</div>
              <h4>Download the APK</h4>
              <p>
                Tap the <strong>Download Android APK</strong> button above or scan the QR code using your phone. The file will save to your device downloads.
              </p>
            </div>
            <div className="step-card">
              <div className="step-number">2</div>
              <h4>Allow Unknown Source</h4>
              <p>
                When prompted by Android security, tap <strong>Settings</strong> and toggle on <strong>Allow from this source</strong> (Chrome / Files).
              </p>
            </div>
            <div className="step-card">
              <div className="step-number">3</div>
              <h4>Launch &amp; Start Billing</h4>
              <p>
                Open DigitalChopda, log in with your credentials, and enjoy real-time sync with your central laptop server!
              </p>
            </div>
          </div>
        </section>

        {/* Feature Highlights Grid */}
        <section className="apk-features-section">
          <h2 className="section-heading">Built for Indian Retail &amp; Wholesale Counters</h2>
          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">⚡</div>
              <h4>Offline-First SQLite</h4>
              <p>Create bills, record payments, and manage contacts even during network downtime. Auto-syncs to the central depot when reconnected.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🖨️</div>
              <h4>Bluetooth Thermal Printing</h4>
              <p>Supports all 58mm &amp; 80mm ESC/POS portable Bluetooth printers. Print tax invoices and payment vouchers instantly at the counter.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🔒</div>
              <h4>Biometric Security</h4>
              <p>Secure cash counters and customer khata records with one-touch Fingerprint &amp; Face Unlock authentication.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🌐</div>
              <h4>Tri-Lingual Interface</h4>
              <p>Seamlessly switch the entire UI and receipts between <strong>English</strong>, <strong>ગુજરાતી (Gujarati)</strong>, and <strong>हिन्दी (Hindi)</strong>.</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
