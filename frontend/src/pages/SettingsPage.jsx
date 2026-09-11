import { useEffect, useState } from 'react';
import { api } from '../api';
import { subscribeToPush } from '../pushClient';

const emptyProfile = {
  name: '', address: '', gstin: '', pan: '', mobile: '', state: '',
  bank_name: '', bank_account_no: '', bank_ifsc: '', bank_branch: '', terms_and_conditions: 'Thanks',
  signature_image: '',
};

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export default function SettingsPage() {
  const [status, setStatus] = useState(null);
  const [pushState, setPushState] = useState('idle');
  const [error, setError] = useState(null);

  const [profile, setProfile] = useState(emptyProfile);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSaved, setProfileSaved] = useState(false);

  useEffect(() => {
    api.settingsStatus().then(setStatus).catch(() => {});
    api.getBusinessProfile().then((p) => p && setProfile({ ...emptyProfile, ...p })).catch(() => {});
  }, []);

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
        <h2>Customer messages (WhatsApp)</h2>
        <p className="subtitle">
          Business-initiated follow-ups to customers require a pre-approved WhatsApp template
          (~24h approval lead time) and Cloud API credentials.
        </p>
        <p>
          Status:{' '}
          <span className={status?.whatsappConfigured ? 'badge ok' : 'badge warn'}>
            {status ? (status.whatsappConfigured ? 'Configured' : 'Not configured — dry-run mode') : '…'}
          </span>
        </p>
        {!status?.whatsappConfigured && (
          <p className="hint">
            Reminders on the WhatsApp channel are logged to the backend console instead of sent.
            Set WHATSAPP_TOKEN, WHATSAPP_PHONE_NUMBER_ID, and WHATSAPP_TEMPLATE_NAME in backend/.env
            once you have Cloud API access.
          </p>
        )}
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
