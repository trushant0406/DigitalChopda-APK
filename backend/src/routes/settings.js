import { Router } from 'express';
import { isPushConfigured } from '../notify/push.js';
import { isWhatsAppConfigured } from '../notify/whatsapp.js';
import {
  getWhatsAppWebStatus,
  getWhatsAppWebDiagnostics,
  initWhatsAppWebClient,
  reconnectWhatsAppWeb,
} from '../notify/whatsappWebClient.js';

const router = Router();

router.get('/status', (req, res) => {
  const webStatus = getWhatsAppWebStatus();
  res.json({
    pushConfigured: isPushConfigured(),
    whatsappConfigured: isWhatsAppConfigured() || webStatus.status === 'CONNECTED',
    whatsappWebStatus: webStatus.status,
    whatsappWebQr: webStatus.qrCodeDataUrl,
    reminderPollSeconds: Number(process.env.REMINDER_POLL_SECONDS) || 60,
  });
});

router.get('/whatsapp-web/diagnostics', async (req, res) => {
  const diag = await getWhatsAppWebDiagnostics();
  res.json(diag);
});

router.post('/whatsapp-web/connect', (req, res) => {
  initWhatsAppWebClient();
  res.json(getWhatsAppWebStatus());
});

router.post('/whatsapp-web/reconnect', async (req, res) => {
  const { forceReset } = req.body || {};
  const status = await reconnectWhatsAppWeb(Boolean(forceReset));
  res.json(status);
});

export default router;
