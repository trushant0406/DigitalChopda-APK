import { useState } from 'react';
import { api } from '../api';

const initial = {
  phone: '',
  name: '',
  city: '',
  direction: 'incoming',
  outcome: 'answered',
  duration_sec: '',
  notes: '',
  next_followup_date: '',
  followup_reason: '',
  followup_channel: 'push',
};

export default function CallForm({ onLogged }) {
  const [form, setForm] = useState(initial);
  const [recording, setRecording] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const data = new FormData();
      Object.entries(form).forEach(([key, value]) => {
        if (value !== '') data.append(key, value);
      });
      if (recording) data.append('recording', recording);

      await api.logCall(data);
      setForm(initial);
      setRecording(null);
      e.target.reset();
      onLogged();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card call-form" onSubmit={handleSubmit}>
      <h2>Log a call</h2>
      <div className="grid">
        <label>
          Phone *
          <input required value={form.phone} onChange={update('phone')} placeholder="+91 98765 43210" />
        </label>
        <label>
          Name
          <input value={form.name} onChange={update('name')} placeholder="Caller name" />
        </label>
        <label>
          City
          <input value={form.city} onChange={update('city')} />
        </label>
        <label>
          Direction
          <select value={form.direction} onChange={update('direction')}>
            <option value="incoming">Incoming</option>
            <option value="outgoing">Outgoing</option>
          </select>
        </label>
        <label>
          Outcome
          <select value={form.outcome} onChange={update('outcome')}>
            <option value="answered">Answered</option>
            <option value="missed">Missed</option>
            <option value="busy">Busy</option>
          </select>
        </label>
        <label>
          Duration (sec)
          <input type="number" min="0" value={form.duration_sec} onChange={update('duration_sec')} />
        </label>
      </div>

      <label>
        Notes
        <textarea value={form.notes} onChange={update('notes')} rows={2} />
      </label>

      <label>
        Recording (optional)
        <input type="file" accept="audio/*" onChange={(e) => setRecording(e.target.files[0] || null)} />
      </label>

      <fieldset>
        <legend>Next follow-up (optional)</legend>
        <div className="grid">
          <label>
            Due date
            <input type="datetime-local" value={form.next_followup_date} onChange={update('next_followup_date')} />
          </label>
          <label>
            Reason
            <input value={form.followup_reason} onChange={update('followup_reason')} placeholder="e.g. send quote" />
          </label>
          <label>
            Remind via
            <select value={form.followup_channel} onChange={update('followup_channel')}>
              <option value="push">Push notification (me)</option>
              <option value="whatsapp">WhatsApp (customer)</option>
            </select>
          </label>
        </div>
      </fieldset>

      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={saving}>
        {saving ? 'Saving…' : 'Log call'}
      </button>
    </form>
  );
}
