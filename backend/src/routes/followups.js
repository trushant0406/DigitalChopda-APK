import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/followups — all follow-ups, optionally filtered by status/channel
router.get('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const { status, channel, due_from, due_to } = req.query;
  const conditions = [`followups.business_id = $1`];
  const values = [businessId];
  let i = 2;

  if (status) {
    conditions.push(`followups.status = $${i++}`);
    values.push(status);
  }
  if (channel) {
    conditions.push(`followups.channel = $${i++}`);
    values.push(channel);
  }
  if (due_from) {
    conditions.push(`followups.due_at >= $${i++}`);
    values.push(due_from);
  }
  if (due_to) {
    conditions.push(`followups.due_at < $${i++}`);
    values.push(due_to);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await pool.query(
    `SELECT followups.*, contacts.name, contacts.phone, contacts.city
     FROM followups JOIN contacts ON contacts.id = followups.contact_id
     ${where}
     ORDER BY followups.due_at ASC
     LIMIT 500`,
    values
  );
  res.json(result.rows);
});

// POST /api/followups — create a standalone follow-up (not tied to a fresh call log)
router.post('/', async (req, res) => {
  const businessId = req.businessId || 1;
  const { contact_id, due_at, reason, channel } = req.body;
  if (!contact_id || !due_at) {
    return res.status(400).json({ error: 'contact_id and due_at are required' });
  }
  const result = await pool.query(
    `INSERT INTO followups (business_id, contact_id, due_at, reason, channel)
     VALUES ($1, $2, $3, $4, $5) RETURNING *`,
    [businessId, contact_id, due_at, reason || null, channel === 'whatsapp' ? 'whatsapp' : 'push']
  );
  res.status(201).json(result.rows[0]);
});

// POST /api/followups/:id/log-call — close the loop: record what happened on the
// callback, mark this follow-up done, and optionally schedule the next one.
router.post('/:id/log-call', async (req, res) => {
  const businessId = req.businessId || 1;
  const { id } = req.params;
  const {
    direction, outcome, duration_sec, notes,
    next_followup_date, next_followup_reason, next_followup_channel,
  } = req.body;

  if (!direction || !outcome) {
    return res.status(400).json({ error: 'direction and outcome are required' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const followupResult = await client.query('SELECT * FROM followups WHERE id = $1 AND business_id = $2', [id, businessId]);
    if (followupResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Follow-up not found' });
    }
    const originalFollowup = followupResult.rows[0];

    const callResult = await client.query(
      `INSERT INTO calls (business_id, contact_id, direction, outcome, duration_sec, notes, source)
       VALUES ($1, $2, $3, $4, $5, $6, 'manual') RETURNING *`,
      [businessId, originalFollowup.contact_id, direction, outcome, duration_sec || null, notes || null]
    );
    const call = callResult.rows[0];

    await client.query('UPDATE contacts SET last_contacted = now() WHERE id = $1 AND business_id = $2', [originalFollowup.contact_id, businessId]);

    const closedResult = await client.query(
      `UPDATE followups SET status = 'done' WHERE id = $1 AND business_id = $2 RETURNING *`,
      [id, businessId]
    );

    let nextFollowup = null;
    if (next_followup_date) {
      const nextResult = await client.query(
        `INSERT INTO followups (business_id, contact_id, call_id, due_at, reason, channel)
         VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
        [
          businessId,
          originalFollowup.contact_id,
          call.id,
          next_followup_date,
          next_followup_reason || null,
          next_followup_channel === 'whatsapp' ? 'whatsapp' : 'push',
        ]
      );
      nextFollowup = nextResult.rows[0];
    }

    await client.query('COMMIT');
    res.status(201).json({ call, closedFollowup: closedResult.rows[0], nextFollowup });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to log call outcome' });
  } finally {
    client.release();
  }
});

// PATCH /api/followups/:id — mark done/cancelled, or reschedule due_at
router.patch('/:id', async (req, res) => {
  const businessId = req.businessId || 1;
  const { id } = req.params;
  const { status, due_at } = req.body;

  const fields = [];
  const values = [];
  let i = 1;

  if (status) {
    fields.push(`status = $${i++}`);
    values.push(status);
  }
  if (due_at) {
    fields.push(`due_at = $${i++}`);
    values.push(due_at);
    // Rescheduling should clear a past reminder so the scheduler fires again for the new date.
    fields.push('sent_at = NULL');
  }
  if (fields.length === 0) return res.status(400).json({ error: 'Nothing to update' });

  values.push(id);
  values.push(businessId);
  const result = await pool.query(
    `UPDATE followups SET ${fields.join(', ')} WHERE id = $${i} AND business_id = $${i + 1} RETURNING *`,
    values
  );
  if (result.rows.length === 0) return res.status(404).json({ error: 'Follow-up not found' });
  res.json(result.rows[0]);
});

export default router;
