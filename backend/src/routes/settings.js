import { Router } from 'express';
import { isPushConfigured } from '../notify/push.js';
import { isWhatsAppConfigured } from '../notify/whatsapp.js';

const router = Router();

router.get('/status', (req, res) => {
  res.json({
    pushConfigured: isPushConfigured(),
    whatsappConfigured: isWhatsAppConfigured(),
    reminderPollSeconds: Number(process.env.REMINDER_POLL_SECONDS) || 60,
  });
});

export default router;
