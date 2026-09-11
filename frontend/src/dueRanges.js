export const DUE_RANGES = [
  { key: 'all', label: 'Any due date' },
  { key: 'overdue', label: 'Overdue' },
  { key: 'today', label: 'Due today' },
  { key: 'tomorrow', label: 'Due tomorrow' },
  { key: 'next7', label: 'Next 7 days' },
  { key: 'next30', label: 'Next 30 days' },
];

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function addDays(date, days) {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

export function dueRangeParams(key) {
  const todayStart = startOfDay(new Date());
  switch (key) {
    case 'overdue':
      return { due_to: todayStart.toISOString() };
    case 'today':
      return { due_from: todayStart.toISOString(), due_to: addDays(todayStart, 1).toISOString() };
    case 'tomorrow':
      return { due_from: addDays(todayStart, 1).toISOString(), due_to: addDays(todayStart, 2).toISOString() };
    case 'next7':
      return { due_from: todayStart.toISOString(), due_to: addDays(todayStart, 7).toISOString() };
    case 'next30':
      return { due_from: todayStart.toISOString(), due_to: addDays(todayStart, 30).toISOString() };
    default:
      return {};
  }
}

// Row-highlighting: overdue vs due-today vs neither.
export function dueTone(dueAt) {
  const due = new Date(dueAt);
  const todayStart = startOfDay(new Date());
  const tomorrowStart = addDays(todayStart, 1);
  if (due < todayStart) return 'overdue';
  if (due < tomorrowStart) return 'today';
  return null;
}

// Snooze: push the due date to N days from now (not from the original due date —
// the point of snoozing is "remind me again later", not "N days after it was due").
export function snoozeDate(days) {
  return new Date(Date.now() + days * 86400000).toISOString();
}
