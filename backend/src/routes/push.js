import { Router } from 'express';
import { isPushConfigured, saveSubscription } from '../notify/push.js';

const router = Router();

router.get('/vapid-public-key', (req, res) => {
  res.json({ configured: isPushConfigured(), publicKey: process.env.VAPID_PUBLIC_KEY || null });
});

router.post('/subscribe', async (req, res) => {
  const sub = req.body;
  if (!sub?.endpoint || !sub?.keys?.p256dh || !sub?.keys?.auth) {
    return res.status(400).json({ error: 'Invalid push subscription' });
  }
  await saveSubscription(sub);
  res.status(201).json({ ok: true });
});

export default router;
