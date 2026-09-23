import { Router } from 'express';
import { pool } from '../db.js';

const router = Router();

// GET /api/dashboard (and alias /summary) — counts for header stat cards for active business
router.get(['/', '/summary'], async (req, res) => {
  const businessId = req.businessId || 1;
  const [dueToday, overdue, neverFollowedUp, repeatNoConnect, callsToday, totalContacts] = await Promise.all([
    pool.query(`SELECT COUNT(*) FROM followups WHERE status = 'pending' AND due_at::date = CURRENT_DATE AND business_id = $1`, [businessId]),
    pool.query(`SELECT COUNT(*) FROM followups WHERE status = 'pending' AND due_at::date < CURRENT_DATE AND business_id = $1`, [businessId]),
    pool.query(`
      SELECT COUNT(*) FROM (
        SELECT contacts.id FROM contacts JOIN calls ON calls.contact_id = contacts.id AND calls.business_id = contacts.business_id
        WHERE contacts.business_id = $1 AND NOT EXISTS (SELECT 1 FROM followups WHERE followups.contact_id = contacts.id AND followups.business_id = $1)
        GROUP BY contacts.id
      ) x
    `, [businessId]),
    pool.query(`
      SELECT COUNT(*) FROM (
        SELECT contacts.id FROM contacts
        JOIN calls ON calls.contact_id = contacts.id AND calls.direction = 'incoming' AND calls.business_id = contacts.business_id
        WHERE contacts.business_id = $1
        GROUP BY contacts.id
        HAVING COUNT(calls.id) >= 2 AND COUNT(calls.id) FILTER (WHERE calls.outcome = 'answered') = 0
      ) x
    `, [businessId]),
    pool.query(`SELECT COUNT(*) FROM calls WHERE started_at::date = CURRENT_DATE AND business_id = $1`, [businessId]),
    pool.query(`SELECT COUNT(*) FROM contacts WHERE business_id = $1`, [businessId]),
  ]);

  res.json({
    dueToday: Number(dueToday.rows[0].count),
    overdue: Number(overdue.rows[0].count),
    neverFollowedUp: Number(neverFollowedUp.rows[0].count),
    repeatNoConnect: Number(repeatNoConnect.rows[0].count),
    callsToday: Number(callsToday.rows[0].count),
    totalContacts: Number(totalContacts.rows[0].count),
  });
});

// GET /api/dashboard/due-today
router.get('/due-today', async (req, res) => {
  const businessId = req.businessId || 1;
  const result = await pool.query(`
    SELECT followups.*, contacts.name, contacts.phone, contacts.city
    FROM followups JOIN contacts ON contacts.id = followups.contact_id
    WHERE followups.status = 'pending' AND followups.due_at::date = CURRENT_DATE AND followups.business_id = $1
    ORDER BY followups.due_at ASC
  `, [businessId]);
  res.json(result.rows);
});

// GET /api/dashboard/overdue
router.get('/overdue', async (req, res) => {
  const businessId = req.businessId || 1;
  const result = await pool.query(`
    SELECT followups.*, contacts.name, contacts.phone, contacts.city
    FROM followups JOIN contacts ON contacts.id = followups.contact_id
    WHERE followups.status = 'pending' AND followups.due_at::date < CURRENT_DATE AND followups.business_id = $1
    ORDER BY followups.due_at ASC
  `, [businessId]);
  res.json(result.rows);
});

// GET /api/dashboard/never-followed-up — contacts with calls but no follow-up ever scheduled
router.get('/never-followed-up', async (req, res) => {
  const businessId = req.businessId || 1;
  const result = await pool.query(`
    SELECT contacts.*,
           COUNT(calls.id) AS call_count,
           MAX(calls.started_at) AS last_call_at
    FROM contacts
    JOIN calls ON calls.contact_id = contacts.id AND calls.business_id = contacts.business_id
    WHERE contacts.business_id = $1 AND NOT EXISTS (SELECT 1 FROM followups WHERE followups.contact_id = contacts.id AND followups.business_id = $1)
    GROUP BY contacts.id
    ORDER BY last_call_at DESC
  `, [businessId]);
  res.json(result.rows);
});

// GET /api/dashboard/repeat-no-connect — 2+ incoming calls, never answered
router.get('/repeat-no-connect', async (req, res) => {
  const businessId = req.businessId || 1;
  const result = await pool.query(`
    SELECT contacts.*,
           COUNT(calls.id) AS call_count,
           MAX(calls.started_at) AS last_call_at
    FROM contacts
    JOIN calls ON calls.contact_id = contacts.id AND calls.direction = 'incoming' AND calls.business_id = contacts.business_id
    WHERE contacts.business_id = $1
    GROUP BY contacts.id
    HAVING COUNT(calls.id) >= 2
       AND COUNT(calls.id) FILTER (WHERE calls.outcome = 'answered') = 0
    ORDER BY last_call_at DESC
  `, [businessId]);
  res.json(result.rows);
});

export default router;
