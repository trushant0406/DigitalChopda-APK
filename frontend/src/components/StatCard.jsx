export default function StatCard({ label, value, tone, onClick }) {
  const className = `stat-card${tone ? ` tone-${tone}` : ''}${onClick ? ' clickable' : ''}`;
  return (
    <div
      className={className}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={onClick ? (e) => (e.key === 'Enter' || e.key === ' ') && onClick() : undefined}
    >
      <div className="stat-value">{value ?? '—'}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}
