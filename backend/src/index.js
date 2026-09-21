import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import path from 'node:path';
import fs from 'node:fs';

import callsRouter from './routes/calls.js';
import contactsRouter from './routes/contacts.js';
import followupsRouter from './routes/followups.js';
import dashboardRouter from './routes/dashboard.js';
import pushRouter from './routes/push.js';
import settingsRouter from './routes/settings.js';
import ordersRouter from './routes/orders.js';
import productsRouter from './routes/products.js';
import quotationsRouter from './routes/quotations.js';
import businessProfileRouter from './routes/businessProfile.js';
import creditRouter from './routes/credit.js';
import creditNotesRouter from './routes/creditNotes.js';
import authRouter from './routes/auth.js';
import { startScheduler } from './scheduler.js';
import { initWhatsAppWebClient } from './notify/whatsappWebClient.js';

dotenv.config();

process.on('uncaughtException', (err) => {
  console.error('[uncaughtException]', err?.message || err);
});
process.on('unhandledRejection', (reason) => {
  console.error('[unhandledRejection]', reason?.message || reason);
});

const app = express();
app.disable('x-powered-by');
const uploadsDir = path.join(process.cwd(), 'uploads');
fs.mkdirSync(uploadsDir, { recursive: true });

app.use(cors());
app.use(express.json({ limit: '5mb' })); // signature image in business-profile is a base64 data URL
app.use('/uploads', express.static(uploadsDir));

app.use('/api/calls', callsRouter);
app.use('/api/contacts', contactsRouter);
app.use('/api/followups', followupsRouter);
app.use('/api/dashboard', dashboardRouter);
app.use('/api/push', pushRouter);
app.use('/api/settings', settingsRouter);
app.use('/api/orders', ordersRouter);
app.use('/api/products', productsRouter);
app.use('/api/quotations', quotationsRouter);
app.use('/api/business-profile', businessProfileRouter);
app.use('/api/credit', creditRouter);
app.use('/api/credit-notes', creditNotesRouter);
app.use('/api/auth', authRouter);

app.get('/api/health', (req, res) => res.json({ ok: true }));

const port = process.env.PORT || 4000;
app.listen(port, () => {
  console.log(`Call Intelligence backend on :${port}`);
  startScheduler();
  initWhatsAppWebClient();
});
