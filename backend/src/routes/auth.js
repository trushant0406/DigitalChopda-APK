import { Router } from 'express';
import crypto from 'node:crypto';
import { pool } from '../db.js';

const router = Router();
const SALT = 'callintel_salt_2026';
const SECRET = process.env.JWT_SECRET || 'callintel_secret_key_2026_super_secure';

function hashPassword(password) {
  return crypto.pbkdf2Sync(password, SALT, 1000, 64, 'sha512').toString('hex');
}

function generateToken(user) {
  const payload = `${user.id}:${user.username}:${Date.now() + 7 * 24 * 60 * 60 * 1000}`;
  const sig = crypto.createHmac('sha256', SECRET).update(payload).digest('hex');
  return `${payload}:${sig}`;
}

export function verifyToken(token) {
  if (!token) return null;
  const parts = token.split(':');
  if (parts.length !== 4) return null;
  const [id, username, expStr, sig] = parts;
  const exp = Number(expStr);
  if (Date.now() > exp) return null;
  const payload = `${id}:${username}:${expStr}`;
  const expectedSig = crypto.createHmac('sha256', SECRET).update(payload).digest('hex');
  if (sig !== expectedSig) return null;
  return { id: Number(id), username };
}

// POST /api/auth/login
router.post('/login', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required' });
  }

  const result = await pool.query('SELECT * FROM users WHERE username = $1', [username.trim().toLowerCase()]);
  if (result.rows.length === 0) {
    return res.status(401).json({ error: 'Invalid username or password' });
  }

  const user = result.rows[0];
  const hash = hashPassword(password);
  if (hash !== user.password_hash) {
    return res.status(401).json({ error: 'Invalid username or password' });
  }

  const token = generateToken(user);
  res.json({
    token,
    user: { id: user.id, username: user.username, name: user.name, role: user.role },
  });
});

// GET /api/auth/me
router.get('/me', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader?.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);
  if (!decoded) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  const result = await pool.query('SELECT id, username, name, role, created_at FROM users WHERE id = $1', [decoded.id]);
  if (result.rows.length === 0) {
    return res.status(401).json({ error: 'User not found' });
  }

  res.json({ user: result.rows[0] });
});

// POST /api/auth/change-password
router.post('/change-password', async (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader?.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const decoded = verifyToken(token);
  if (!decoded) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  const { currentPassword, newPassword } = req.body;
  if (!currentPassword || !newPassword || newPassword.length < 6) {
    return res.status(400).json({ error: 'New password must be at least 6 characters long' });
  }

  const result = await pool.query('SELECT * FROM users WHERE id = $1', [decoded.id]);
  if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });

  const user = result.rows[0];
  if (hashPassword(currentPassword) !== user.password_hash) {
    return res.status(400).json({ error: 'Current password is incorrect' });
  }

  const newHash = hashPassword(newPassword);
  await pool.query('UPDATE users SET password_hash = $1 WHERE id = $2', [newHash, decoded.id]);
  res.json({ ok: true, message: 'Password updated successfully' });
});

export default router;
