export default function PhoneLink({ phone }) {
  if (!phone) return '—';
  return (
    <a className="phone-link" href={`tel:${phone}`} onClick={(e) => e.stopPropagation()}>
      {phone}
    </a>
  );
}
