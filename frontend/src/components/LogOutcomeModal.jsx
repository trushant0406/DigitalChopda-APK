import { useState } from 'react';
import { api } from '../api';
import CreateOrderModal from './CreateOrderModal';

export default function LogOutcomeModal({ followup, onClose, onDone }) {
  const [direction, setDirection] = useState('outgoing');
  const [outcome, setOutcome] = useState('answered');
  const [durationSec, setDurationSec] = useState('');
  const [notes, setNotes] = useState('');
  const [nextDate, setNextDate] = useState('');
  const [nextReason, setNextReason] = useState('');
  const [nextChannel, setNextChannel] = useState('push');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [showOrderModal, setShowOrderModal] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.logFollowupCall(followup.id, {
        direction,
        outcome,
        duration_sec: durationSec || undefined,
        notes: notes || undefined,
        next_followup_date: nextDate || undefined,
        next_followup_reason: nextReason || undefined,
        next_followup_channel: nextChannel,
      });
      onDone();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h2>Log outcome</h2>
            <p className="subtitle">
              {followup.name || followup.phone} {followup.reason ? `· was: ${followup.reason}` : ''}
            </p>
          </div>
          <button type="button" onClick={onClose}>Close</button>
        </div>

        <button type="button" className="secondary-action" onClick={() => setShowOrderModal(true)}>
          + Create order for this customer
        </button>

        <form onSubmit={handleSubmit}>
          <div className="grid">
            <label>
              Direction
              <select value={direction} onChange={(e) => setDirection(e.target.value)}>
                <option value="outgoing">Outgoing (I called them)</option>
                <option value="incoming">Incoming (they called me)</option>
              </select>
            </label>
            <label>
              Outcome
              <select value={outcome} onChange={(e) => setOutcome(e.target.value)}>
                <option value="answered">Answered</option>
                <option value="missed">Missed</option>
                <option value="busy">Busy</option>
              </select>
            </label>
            <label>
              Duration (sec)
              <input type="number" min="0" value={durationSec} onChange={(e) => setDurationSec(e.target.value)} />
            </label>
          </div>

          <label>
            Notes
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} />
          </label>

          <fieldset>
            <legend>Next follow-up (optional)</legend>
            <div className="grid">
              <label>
                Due date
                <input type="datetime-local" value={nextDate} onChange={(e) => setNextDate(e.target.value)} />
              </label>
              <label>
                Reason
                <input value={nextReason} onChange={(e) => setNextReason(e.target.value)} />
              </label>
              <label>
                Remind via
                <select value={nextChannel} onChange={(e) => setNextChannel(e.target.value)}>
                  <option value="push">Push (me)</option>
                  <option value="whatsapp">WhatsApp (customer)</option>
                </select>
              </label>
            </div>
          </fieldset>

          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save & close follow-up'}
          </button>
        </form>
      </div>

      {showOrderModal && (
        <CreateOrderModal
          contact={{ phone: followup.phone, name: followup.name, city: followup.city }}
          onClose={() => setShowOrderModal(false)}
          onCreated={() => {}}
        />
      )}
    </div>
  );
}
