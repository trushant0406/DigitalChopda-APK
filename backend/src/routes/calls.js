import { Router } from 'express';
import multer from 'multer';
import path from 'node:path';
import { pool } from '../db.js';
import { findOrCreateContact } from '../lib/contacts.js';

const router = Router();

const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => cb(null, path.join(process.cwd(), 'uploads')),
    filename: (req, file, cb) => cb(null, `${Date.now()}-${file.originalname}`),
  }),
});

// POST /api/calls — log a call, optionally schedule a follow-up
router.post('/', upload.single('recording'), async (req, res) => {
  const {
    phone, name, city, direction, outcome, duration_sec, notes,
    next_followup_date, followup_reason, followup_channel,
  } = req.body;

  if (!phone || !direction || !outcome) {
    return res.status(400).json({ error: 'phone, direction, and outcome are required' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const contact = await findOrCreateContact(client, { phone, name, city });

    const recording_url = req.file ? `/uploads/${req.file.filename}` : null;
    const callResult = await client.query(
      `INSERT INTO calls (contact_id, direction, outcome, duration_sec, notes, recording_url, source)
       VALUES ($1, $2, $3, $4, $5, $6, 'manual') RETURNING *`,
      [contact.id, direction, outcome, duration_sec || null, notes || null, recording_url]
    );
    const call = callResult.rows[0];

    await client.query('UPDATE contacts SET last_contacted = now() WHERE id = $1', [contact.id]);

    let followup = null;
    if (next_followup_date) {
      const followupResult = await client.query(
        `INSERT INTO followups (contact_id, call_id, due_at, reason, channel)
         VALUES ($1, $2, $3, $4, $5) RETURNING *`,
        [
          contact.id,
          call.id,
          next_followup_date,
          followup_reason || null,
          followup_channel === 'whatsapp' ? 'whatsapp' : 'push',
        ]
      );
      followup = followupResult.rows[0];
    }

    await client.query('COMMIT');
    res.status(201).json({ contact, call, followup });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to log call' });
  } finally {
    client.release();
  }
});

// GET /api/calls — recent calls, most recent first
router.get('/', async (req, res) => {
  const result = await pool.query(
    `SELECT calls.*, contacts.name, contacts.phone, contacts.city
     FROM calls JOIN contacts ON contacts.id = calls.contact_id
     ORDER BY calls.started_at DESC LIMIT 200`
  );
  res.json(result.rows);
});

// GET /api/calls/export.csv — full call log for offline use / spreadsheets
router.get('/export.csv', async (req, res) => {
  const result = await pool.query(
    `SELECT calls.started_at, contacts.name, contacts.phone, contacts.city,
            calls.direction, calls.outcome, calls.duration_sec, calls.notes, calls.source
     FROM calls JOIN contacts ON contacts.id = calls.contact_id
     ORDER BY calls.started_at DESC`
  );

  const escape = (value) => {
    if (value === null || value === undefined) return '';
    const str = value instanceof Date ? value.toISOString() : String(value);
    return /[",\n]/.test(str) ? `"${str.replaceAll('"', '""')}"` : str;
  };

  const header = ['started_at', 'name', 'phone', 'city', 'direction', 'outcome', 'duration_sec', 'notes', 'source'];
  const lines = [header.join(',')];
  for (const row of result.rows) {
    lines.push(header.map((key) => escape(row[key])).join(','));
  }

  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', 'attachment; filename="calls.csv"');
  res.send(lines.join('\n'));
});

export default router;
