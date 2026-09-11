export default function DataTable({
  columns, rows, loading, emptyText = 'Nothing here.', rowKey = 'id', rowClassName, selection,
}) {
  if (loading) return <p>Loading…</p>;
  if (!rows || rows.length === 0) return <p className="empty">{emptyText}</p>;

  const ids = rows.map((r) => r[rowKey]);
  const allSelected = selection && ids.length > 0 && ids.every((id) => selection.selectedIds.has(id));

  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            {selection && (
              <th>
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={() => selection.onToggleAll(ids)}
                  aria-label="Select all rows"
                />
              </th>
            )}
            {columns.map((col) => (
              <th key={col.key} className={col.align === 'right' ? 'align-right' : undefined}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row[rowKey]} className={rowClassName ? rowClassName(row) : undefined}>
              {selection && (
                <td>
                  <input
                    type="checkbox"
                    checked={selection.selectedIds.has(row[rowKey])}
                    onChange={() => selection.onToggle(row[rowKey])}
                    aria-label="Select row"
                  />
                </td>
              )}
              {columns.map((col) => {
                const classNames = [col.align === 'right' ? 'align-right' : null, col.className]
                  .filter(Boolean)
                  .join(' ');
                return (
                  <td key={col.key} className={classNames || undefined}>
                    {col.render ? col.render(row) : row[col.key] ?? '—'}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
