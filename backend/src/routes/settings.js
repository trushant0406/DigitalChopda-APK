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
  const businessId = req.businessId || 1;
  const webStatus = getWhatsAppWebStatus(businessId);
  res.json({
    pushConfigured: isPushConfigured(),
    whatsappConfigured: isWhatsAppConfigured() || webStatus.status === 'CONNECTED',
    whatsappWebStatus: webStatus.status,
    whatsappWebQr: webStatus.qrCodeDataUrl,
    businessId,
    reminderPollSeconds: Number(process.env.REMINDER_POLL_SECONDS) || 60,
  });
});

router.get('/whatsapp-web/diagnostics', async (req, res) => {
  const businessId = req.businessId || 1;
  const diag = await getWhatsAppWebDiagnostics(businessId);
  res.json(diag);
});

router.post('/whatsapp-web/connect', (req, res) => {
  const businessId = req.businessId || 1;
  initWhatsAppWebClient(businessId);
  res.json(getWhatsAppWebStatus(businessId));
});

router.post('/whatsapp-web/reconnect', async (req, res) => {
  const businessId = req.businessId || 1;
  const { forceReset } = req.body || {};
  const status = await reconnectWhatsAppWeb(businessId, Boolean(forceReset));
  res.json(status);
});

export default router;
