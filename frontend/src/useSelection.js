import { useState } from 'react';

export function useSelection() {
  const [selectedIds, setSelectedIds] = useState(new Set());

  const onToggle = (id) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const onToggleAll = (ids) =>
    setSelectedIds((prev) => (ids.every((id) => prev.has(id)) ? new Set() : new Set(ids)));

  const clear = () => setSelectedIds(new Set());

  return { selectedIds, onToggle, onToggleAll, clear };
}
