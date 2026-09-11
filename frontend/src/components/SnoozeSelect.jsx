import { api } from '../api';
import { snoozeDate } from '../dueRanges';

const OPTIONS = [
  { days: 1, label: '+1 day' },
  { days: 3, label: '+3 days' },
  { days: 7, label: '+1 week' },
];

export default function SnoozeSelect({ followupId, onDone }) {
  async function handleChange(e) {
    const days = Number(e.target.value);
    e.target.value = '';
    if (!days) return;
    await api.updateFollowup(followupId, { due_at: snoozeDate(days) });
    onDone();
  }

  return (
    <select defaultValue="" onChange={handleChange} className="snooze-select" title="Snooze">
      <option value="" disabled>Snooze…</option>
      {OPTIONS.map((o) => (
        <option key={o.days} value={o.days}>{o.label}</option>
      ))}
    </select>
  );
}
