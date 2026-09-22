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
import { startScheduler, stopScheduler } from './scheduler.js';
import { initWhatsAppWebClient, destroyWhatsAppWebClient, getWhatsAppWebStatus } from './notify/whatsappWebClient.js';
import { pool } from './db.js';
import { getPdfRendererStatus } from './pdf/renderPdf.js';

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

// Comprehensive Enterprise Health & Diagnostics
app.get('/api/health', async (req, res) => {
  let dbOk = false;
  let dbLatencyMs = 0;
  try {
    const t0 = Date.now();
    await pool.query('SELECT 1');
    dbLatencyMs = Date.now() - t0;
    dbOk = true;
  } catch (e) {
    dbOk = false;
  }

  const pdfStatus = getPdfRendererStatus();
  const waStatus = getWhatsAppWebStatus();

  const isHealthy = dbOk;
  res.status(isHealthy ? 200 : 503).json({
    status: isHealthy ? 'healthy' : 'degraded',
    timestamp: new Date().toISOString(),
    uptime: Math.floor(process.uptime()),
    database: {
      ok: dbOk,
      latencyMs: dbLatencyMs,
      totalCount: pool.totalCount,
      idleCount: pool.idleCount,
      waitingCount: pool.waitingCount,
    },
    pdfRenderer: pdfStatus,
    whatsapp: {
      status: waStatus.status,
      hasQr: Boolean(waStatus.qrCodeDataUrl),
    },
    memory: {
      heapUsedMb: Math.round(process.memoryUsage().heapUsed / 1024 / 1024),
      rssMb: Math.round(process.memoryUsage().rss / 1024 / 1024),
    },
  });
});

const port = process.env.PORT || 4000;
const server = app.listen(port, () => {
  console.log(`DigitalChopda.in backend on :${port}`);
  startScheduler();
  initWhatsAppWebClient();
});

// Enterprise Keep-Alive & Timeout Protection behind Nginx / ALB
server.keepAliveTimeout = 65000; // Must be higher than Nginx keepalive (60s)
server.headersTimeout = 66000;
server.requestTimeout = 120000;

// Graceful Shutdown Handler
let isShuttingDown = false;

async function gracefulShutdown(signal) {
  if (isShuttingDown) return;
  isShuttingDown = true;
  console.log(`\n[Shutdown] Received ${signal}. Starting graceful shutdown...`);

  // Force-kill watchdog after 10s if tasks hang
  const forceKillTimer = setTimeout(() => {
    console.error('[Shutdown] Forcefully exiting after 10s shutdown timeout.');
    process.exit(1);
  }, 10000);
  forceKillTimer.unref();

  // 1. Stop background scheduler
  try {
    stopScheduler();
  } catch (_) {}

  // 2. Stop accepting new HTTP requests and drain in-flight
  try {
    await new Promise((resolve) => server.close(resolve));
    console.log('[Shutdown] HTTP server closed.');
  } catch (err) {
    console.warn('[Shutdown] Error closing HTTP server:', err.message);
  }

  // 3. Cleanly destroy WhatsApp client and Chromium processes
  try {
    await destroyWhatsAppWebClient();
    console.log('[Shutdown] WhatsApp client destroyed.');
  } catch (err) {
    console.warn('[Shutdown] WhatsApp cleanup note:', err.message);
  }

  // 4. Drain and close database pool
  try {
    await pool.end();
    console.log('[Shutdown] Database connection pool drained.');
  } catch (err) {
    console.warn('[Shutdown] DB pool drain note:', err.message);
  }

  clearTimeout(forceKillTimer);
  console.log('[Shutdown] Graceful shutdown completed cleanly.');
  process.exit(0);
}

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));
