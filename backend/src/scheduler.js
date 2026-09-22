import cron from 'node-cron';
import { pool } from './db.js';
import { sendOwnerPush } from './notify/push.js';
import { sendWhatsAppTemplate } from './notify/whatsapp.js';

// Scans for follow-ups whose due_at has passed and that haven't been reminded yet
// (sent_at IS NULL). Owner reminders (channel='push') and customer messages
// (channel='whatsapp') are dispatched down separate paths per the cost split in
// the handoff doc — only whatsapp sends cost money.
export async function processDueFollowups() {
  const { rows: due } = await pool.query(`
    SELECT followups.*, contacts.name, contacts.phone
    FROM followups JOIN contacts ON contacts.id = followups.contact_id
    WHERE followups.status = 'pending' AND followups.sent_at IS NULL AND followups.due_at <= now()
    ORDER BY followups.due_at ASC
  `);

  for (const followup of due) {
    try {
      if (followup.channel === 'whatsapp') {
        await sendWhatsAppTemplate(followup.phone, {
          reason: followup.reason,
          contactName: followup.name,
        });
      } else {
        await sendOwnerPush({
          title: 'Follow-up due',
          body: `${followup.name || followup.phone}: ${followup.reason || 'follow up now'}`,
          followupId: followup.id,
        });
      }
      await pool.query('UPDATE followups SET sent_at = now() WHERE id = $1', [followup.id]);
    } catch (err) {
      console.error(`reminder dispatch failed for followup ${followup.id}`, err.message);
    }
  }

  return due.length;
}

let scheduledTask = null;

export function startScheduler() {
  const seconds = Number(process.env.REMINDER_POLL_SECONDS) || 60;
  // node-cron has no plain "every N seconds" primitive under a minute; build it explicitly.
  const pattern = seconds < 60 ? `*/${seconds} * * * * *` : '* * * * *';

  scheduledTask = cron.schedule(pattern, async () => {
    const count = await processDueFollowups();
    if (count > 0) console.log(`[scheduler] dispatched ${count} due follow-up(s)`);
  });

  console.log(`[scheduler] polling for due follow-ups every ${seconds}s`);
}

export function stopScheduler() {
  if (scheduledTask) {
    scheduledTask.stop();
    scheduledTask = null;
    console.log('[scheduler] Stopped reminder poll scheduler.');
  }
}
