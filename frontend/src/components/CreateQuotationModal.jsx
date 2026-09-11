import QuotationForm from './QuotationForm';

export default function CreateQuotationModal({ contact, onClose, onCreated }) {
  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <h2>Create quotation</h2>
            <p className="subtitle">{contact.name || contact.phone}</p>
          </div>
          <button type="button" onClick={onClose}>Close</button>
        </div>
        <QuotationForm
          presetContact={contact}
          onCreated={(quotation) => {
            onCreated(quotation);
            onClose();
          }}
        />
      </div>
    </div>
  );
}
