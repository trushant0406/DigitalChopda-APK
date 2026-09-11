import webpush from 'web-push';
import { pool } from '../db.js';

const configured = Boolean(process.env.VAPID_PUBLIC_KEY && process.env.VAPID_PRIVATE_KEY);

if (configured) {
  webpush.setVapidDetails(
    process.env.VAPID_SUBJECT || 'mailto:admin@example.com',
    process.env.VAPID_PUBLIC_KEY,
    process.env.VAPID_PRIVATE_KEY
  );
}

export function isPushConfigured() {
  return configured;
}

export async function saveSubscription(sub) {
  await pool.query(
    `INSERT INTO push_subscriptions (endpoint, p256dh, auth)
     VALUES ($1, $2, $3)
     ON CONFLICT (endpoint) DO NOTHING`,
    [sub.endpoint, sub.keys.p256dh, sub.keys.auth]
  );
}

// Broadcasts to every registered owner device. Fine for a single-owner app;
// a stale/expired subscription (410/404) is dropped so it stops erroring forever.
export async function sendOwnerPush(payload) {
  if (!configured) {
    console.log('[push DRY RUN]', payload);
    return { sent: 0, dryRun: true };
  }

  const { rows } = await pool.query('SELECT * FROM push_subscriptions');
  let sent = 0;
  for (const row of rows) {
    const subscription = {
      endpoint: row.endpoint,
      keys: { p256dh: row.p256dh, auth: row.auth },
    };
    try {
      await webpush.sendNotification(subscription, JSON.stringify(payload));
      sent += 1;
    } catch (err) {
      if (err.statusCode === 404 || err.statusCode === 410) {
        await pool.query('DELETE FROM push_subscriptions WHERE endpoint = $1', [row.endpoint]);
      } else {
        console.error('push send failed', err.message);
      }
    }
  }
  return { sent, dryRun: false };
}
