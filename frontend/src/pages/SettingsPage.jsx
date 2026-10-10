import { useEffect, useState } from 'react';
import { api } from '../api';
import { subscribeToPush } from '../pushClient';

const emptyProfile = {
  name: '', address: '', gstin: '', pan: '', mobile: '', state: '',
  bank_name: '', bank_account_no: '', bank_ifsc: '', bank_branch: '', terms_and_conditions: 'Thanks',
  signature_image: '',
  invoice_prefix: 'INV-',
  invoice_next_number: 1,
  invoice_padding: 0,
};

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export default function SettingsPage({ refreshKey }) {
  const [status, setStatus] = useState(null);
  const [pushState, setPushState] = useState('idle');
  const [error, setError] = useState(null);
  const [reconnecting, setReconnecting] = useState(false);

  const [profile, setProfile] = useState(emptyProfile);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSaved, setProfileSaved] = useState(false);

  useEffect(() => {
    api.getBusinessProfile().then((p) => p && setProfile({ ...emptyProfile, ...p })).catch(() => {});
  }, [refreshKey]);

  useEffect(() => {
    function checkStatus() {
      api.settingsStatus().then(setStatus).catch(() => {});
    }
    checkStatus();

    const interval = setInterval(checkStatus, 2000);
    return () => clearInterval(interval);
  }, []);

  async function handleReconnect(forceReset = false) {
    setReconnecting(true);
    setError(null);
    try {
      const res = await api.reconnectWhatsAppWeb(forceReset);
      setStatus((prev) => ({ ...prev, ...res }));
    } catch (err) {
      setError(err.message);
    } finally {
      setReconnecting(false);
    }
  }

  function updateProfile(field) {
    return (e) => {
      setProfile((prev) => ({ ...prev, [field]: e.target.value }));
      setProfileSaved(false);
    };
  }

  async function handleSignatureFile(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    const dataUrl = await readFileAsDataUrl(file);
    setProfile((prev) => ({ ...prev, signature_image: dataUrl }));
    setProfileSaved(false);
  }

  async function saveProfile(e) {
    e.preventDefault();
    setProfileSaving(true);
    try {
      await api.saveBusinessProfile(profile);
      setProfileSaved(true);
    } finally {
      setProfileSaving(false);
    }
  }

  async function enablePush() {
    setError(null);
    setPushState('requesting');
    try {
      const { publicKey } = await api.vapidPublicKey();
      const subscription = await subscribeToPush(publicKey);
      await api.subscribePush(subscription);
      setPushState('enabled');
    } catch (err) {
      setError(err.message);
      setPushState('idle');
    }
  }

  return (
    <div className="page">
      <form className="card call-form" onSubmit={saveProfile}>
        <h2>Business profile</h2>
        <p className="subtitle">Your own details — appear on every quotation you send.</p>
        <div className="grid">
          <label>
            Business name
            <input value={profile.name} onChange={updateProfile('name')} placeholder="e.g. Laduba Industries" />
          </label>
          <label>
            Mobile
            <input value={profile.mobile} onChange={updateProfile('mobile')} />
          </label>
          <label>
            State
            <input value={profile.state} onChange={updateProfile('state')} placeholder="e.g. Gujarat" />
          </label>
          <label>
            GSTIN
            <input value={profile.gstin} onChange={updateProfile('gstin')} />
          </label>
          <label>
            PAN
            <input value={profile.pan} onChange={updateProfile('pan')} />
          </label>
        </div>
        <label>
          Address
          <textarea value={profile.address} onChange={updateProfile('address')} rows={2} />
        </label>

        <fieldset>
          <legend>Bank details (optional)</legend>
          <div className="grid">
            <label>
              Bank name
              <input value={profile.bank_name} onChange={updateProfile('bank_name')} />
            </label>
            <label>
              Account No.
              <input value={profile.bank_account_no} onChange={updateProfile('bank_account_no')} />
            </label>
            <label>
              IFSC
              <input value={profile.bank_ifsc} onChange={updateProfile('bank_ifsc')} />
            </label>
            <label>
              Branch
              <input value={profile.bank_branch} onChange={updateProfile('bank_branch')} />
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>Invoice Numbering &amp; Sequences</legend>
          <div className="grid">
            <label>
              Invoice Prefix
              <input
                value={profile.invoice_prefix || ''}
                onChange={updateProfile('invoice_prefix')}
                placeholder="e.g. INV-, BILL/, CI/"
              />
            </label>
            <label>
              Next Sequence Number
              <input
                type="number"
                min="1"
                value={profile.invoice_next_number ?? 1}
                onChange={(e) => {
                  const val = e.target.value;
                  setProfile((prev) => ({
                    ...prev,
                    invoice_next_number: parseInt(val, 10) || 1,
                    invoice_padding: 0,
                  }));
                  setProfileSaved(false);
                }}
                placeholder="e.g. 1 or 1001"
              />
            </label>
          </div>
          <p className="hint" style={{ marginTop: '0.4rem' }}>
            Preview of next invoice: <strong>{(profile.invoice_prefix || 'INV-') + (profile.invoice_padding > 1 ? String(profile.invoice_next_number ?? 1).padStart(profile.invoice_padding, '0') : String(profile.invoice_next_number ?? 1))}</strong>
          </p>
        </fieldset>

        <label>
          Default terms &amp; conditions
          <textarea value={profile.terms_and_conditions} onChange={updateProfile('terms_and_conditions')} rows={2} />
        </label>

        <label>
          Authorised signature (optional — appears on every quotation PDF)
          <input type="file" accept="image/*" onChange={handleSignatureFile} />
        </label>
        {profile.signature_image && (
          <div className="signature-preview">
            <img src={profile.signature_image} alt="Signature preview" />
            <button type="button" className="remove-item" onClick={() => setProfile((p) => ({ ...p, signature_image: '' }))}>
              Remove
            </button>
          </div>
        )}

        <button type="submit" disabled={profileSaving}>
          {profileSaving ? 'Saving…' : profileSaved ? 'Saved ✓' : 'Save business profile'}
        </button>
      </form>

      <div className="card">
        <h2>Owner reminders</h2>
        <p className="subtitle">
          Push notifications on this device for follow-ups you're waiting on. Free, no signup.
        </p>
        <button type="button" onClick={enablePush} disabled={pushState === 'enabled'}>
          {pushState === 'enabled' ? 'Enabled on this device' : 'Enable push notifications'}
        </button>
        {error && <p className="error">{error}</p>}
      </div>

      <div className="card">
        <h2>WhatsApp Direct PDF Attachment (Tokenless)</h2>
        <p className="subtitle">
          Scan the QR code once with your phone (WhatsApp → Linked Devices) to send actual PDF file attachments directly without Meta tokens or API keys.
        </p>
        <p>
          Status:{' '}
          <span className={status?.whatsappWebStatus === 'CONNECTED' ? 'badge ok' : 'badge warn'}>
            {status?.whatsappWebStatus === 'CONNECTED'
              ? 'Connected ✓ (Ready to send PDF attachments)'
              : status?.whatsappWebStatus === 'QR_READY'
              ? 'Scan QR Code Below'
              : status?.whatsappWebStatus === 'INITIALIZING'
              ? 'Initializing WhatsApp Web Client…'
              : 'Disconnected (Click Reset Session or Refresh below)'}
          </span>
        </p>

        {status?.whatsappWebStatus === 'INITIALIZING' && (
          <div style={{ marginTop: '12px', padding: '10px 14px', background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '6px', color: '#1e40af', fontSize: '0.88rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span>⏳</span>
            <span>Starting clean WhatsApp Web session. Generating new QR code (takes ~4–6 seconds), please wait…</span>
          </div>
        )}

        {status?.whatsappWebStatus === 'DISCONNECTED' && (
          <div style={{ marginTop: '12px', padding: '10px 14px', background: '#fffbeb', border: '1px solid #fde68a', borderRadius: '6px', color: '#92400e', fontSize: '0.88rem' }}>
            ℹ WhatsApp is disconnected. Click <strong>🔑 Reset Session &amp; Get New QR</strong> below to generate a new QR code immediately.
          </div>
        )}

        {status?.whatsappWebQr && (
          <div style={{ marginTop: '16px', textAlign: 'center' }}>
            <p><strong>Scan with WhatsApp (Linked Devices):</strong></p>
            <img src={status.whatsappWebQr} alt="WhatsApp Web QR Code" style={{ width: '220px', borderRadius: '8px', border: '1px solid #cbd5e1', margin: '8px 0', background: '#fff', padding: '8px' }} />
            <p className="hint" style={{ fontSize: '0.82rem', marginTop: '4px' }}>Open WhatsApp on your phone → Settings / ⋮ → Linked Devices → Link a Device</p>
          </div>
        )}
        <div style={{ marginTop: '16px', display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button type="button" onClick={() => handleReconnect(false)} disabled={reconnecting}>
            {reconnecting ? 'Syncing…' : '🔄 Refresh / Sync Connection'}
          </button>
          <button type="button" className="remove-item" onClick={() => handleReconnect(true)} disabled={reconnecting}>
            {reconnecting ? 'Generating QR…' : '🔑 Reset Session & Get New QR'}
          </button>
        </div>
      </div>

      <div className="card">
        <h2>Customer messages (WhatsApp Cloud API)</h2>
        <p className="subtitle">
          Optional Meta Cloud API integration using pre-approved templates.
        </p>
        <p>
          Status:{' '}
          <span className={status?.whatsappConfigured ? 'badge ok' : 'badge warn'}>
            {status ? (status.whatsappConfigured ? 'Configured' : 'Not configured') : '…'}
          </span>
        </p>
      </div>

      <div className="card">
        <h2>Reminder engine</h2>
        <p>
          Polling for due follow-ups every{' '}
          <strong>{status?.reminderPollSeconds ?? '—'}s</strong>.
        </p>
      </div>
    </div>
  );
}
