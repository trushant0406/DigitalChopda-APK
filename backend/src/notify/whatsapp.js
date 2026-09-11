const GRAPH_URL = 'https://graph.facebook.com/v20.0';

function isConfigured() {
  return Boolean(process.env.WHATSAPP_TOKEN && process.env.WHATSAPP_PHONE_NUMBER_ID);
}

export function isWhatsAppConfigured() {
  return isConfigured();
}

// Sends a pre-approved template message (business-initiated messages must use one).
// Falls back to a console dry-run when no Cloud API credentials are set, so phase 2
// stays demoable with zero signup.
export async function sendWhatsAppTemplate(phone, { reason, contactName } = {}) {
  const templateName = process.env.WHATSAPP_TEMPLATE_NAME || 'followup_reminder';

  if (!isConfigured()) {
    console.log(`[whatsapp DRY RUN] to=${phone} template=${templateName} reason="${reason || ''}"`);
    return { sent: false, dryRun: true };
  }

  const res = await fetch(`${GRAPH_URL}/${process.env.WHATSAPP_PHONE_NUMBER_ID}/messages`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${process.env.WHATSAPP_TOKEN}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      messaging_product: 'whatsapp',
      to: phone,
      type: 'template',
      template: {
        name: templateName,
        language: { code: 'en' },
        components: [
          {
            type: 'body',
            parameters: [
              { type: 'text', text: contactName || 'there' },
              { type: 'text', text: reason || 'your follow-up' },
            ],
          },
        ],
      },
    }),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new Error(`WhatsApp send failed (${res.status}): ${body}`);
  }
  return { sent: true, dryRun: false };
}

// Sends a document (the quotation PDF) — WhatsApp requires uploading the file
// to its Media API first to get a media id, then referencing that id in a
// "document" message. Falls back to a console dry-run with no credentials,
// same as the template-message path above.
export async function sendWhatsAppDocument(phone, { pdfBuffer, filename, caption }) {
  if (!isConfigured()) {
    console.log(`[whatsapp DRY RUN] document to=${phone} filename=${filename} bytes=${pdfBuffer.length}`);
    return { sent: false, dryRun: true };
  }

  const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;
  const form = new FormData();
  form.append('messaging_product', 'whatsapp');
  form.append('file', new Blob([pdfBuffer], { type: 'application/pdf' }), filename);

  const uploadRes = await fetch(`${GRAPH_URL}/${phoneNumberId}/media`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${process.env.WHATSAPP_TOKEN}` },
    body: form,
  });
  if (!uploadRes.ok) {
    throw new Error(`WhatsApp media upload failed (${uploadRes.status}): ${await uploadRes.text()}`);
  }
  const { id: mediaId } = await uploadRes.json();

  const sendRes = await fetch(`${GRAPH_URL}/${phoneNumberId}/messages`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${process.env.WHATSAPP_TOKEN}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      messaging_product: 'whatsapp',
      to: phone,
      type: 'document',
      document: { id: mediaId, filename, caption },
    }),
  });
  if (!sendRes.ok) {
    throw new Error(`WhatsApp document send failed (${sendRes.status}): ${await sendRes.text()}`);
  }
  return { sent: true, dryRun: false };
}
