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
import businessesRouter from './routes/businesses.js';
import { tenantContext } from './middleware/tenantContext.js';
import { startScheduler, stopScheduler } from './scheduler.js';
import { initWhatsAppWebClient, destroyWhatsAppWebClient, getWhatsAppWebStatus } from './notify/whatsappWebClient.js';
import { pool } from './db.js';
import { getPdfRendererStatus } from './pdf/renderPdf.js';
import QRCode from 'qrcode';

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

// Multi-tenant context middleware (resolves req.businessId & req.business)
app.use(tenantContext);

app.use('/api/businesses', businessesRouter);
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
app.use('/api/payments', creditRouter);
app.use('/api/credit-notes', creditNotesRouter);
app.use('/api/auth', authRouter);

// Android APK Download & Telemetry Endpoint
app.get('/api/app/apk', (req, res) => {
  const possiblePaths = [
    path.join(process.cwd(), 'apk', 'DigitalChopda.apk'),
    path.join(process.cwd(), 'apk', 'DigitalChopda-debug.apk'),
    path.join(process.cwd(), '..', 'apk', 'DigitalChopda.apk'),
    path.join(process.cwd(), '..', 'apk', 'DigitalChopda-debug.apk'),
    path.join('/app', 'apk', 'DigitalChopda.apk'),
    path.join('/app', 'apk', 'DigitalChopda-debug.apk'),
  ];
  for (const apkPath of possiblePaths) {
    if (fs.existsSync(apkPath)) {
      res.setHeader('Content-Type', 'application/vnd.android.package-archive');
      return res.download(apkPath, 'DigitalChopda.apk');
    }
  }
  return res.status(404).json({ error: 'APK package not found on server.' });
});

app.get('/api/app/info', async (req, res) => {
  const possiblePaths = [
    path.join(process.cwd(), 'apk', 'DigitalChopda.apk'),
    path.join(process.cwd(), 'apk', 'DigitalChopda-debug.apk'),
    path.join(process.cwd(), '..', 'apk', 'DigitalChopda.apk'),
    path.join(process.cwd(), '..', 'apk', 'DigitalChopda-debug.apk'),
    path.join('/app', 'apk', 'DigitalChopda.apk'),
    path.join('/app', 'apk', 'DigitalChopda-debug.apk'),
  ];
  let sizeMb = '20.0';
  let updatedAt = new Date();
  let found = false;

  for (const p of possiblePaths) {
    if (fs.existsSync(p)) {
      const stat = fs.statSync(p);
      sizeMb = (stat.size / (1024 * 1024)).toFixed(1);
      updatedAt = stat.mtime;
      found = true;
      break;
    }
  }

  const host = req.get('host') || '100.99.250.0:3000';
  const protocol = req.protocol || 'http';
  const downloadUrl = `${protocol}://${host}/download`;

  let qrDataUrl = '';
  try {
    qrDataUrl = await QRCode.toDataURL(downloadUrl, {
      margin: 2,
      width: 280,
      color: {
        dark: '#1e293b',
        light: '#ffffff',
      },
    });
  } catch (err) {
    console.error('[QRCode]', err.message);
  }

  res.json({
    available: found,
    appName: 'DigitalChopda Android Client',
    version: '1.0.0',
    packageName: 'com.digitalchopda.app',
    sizeMb: `${sizeMb} MB`,
    updatedAt,
    tailscaleServerUrl: 'http://100.99.250.0:4000/api/',
    downloadUrl,
    directApkUrl: '/apk/DigitalChopda.apk',
    apiApkUrl: '/api/app/apk',
    qrDataUrl,
  });
});

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
const server = app.listen(port, async () => {
  console.log(`DigitalChopda.in backend on :${port}`);
  startScheduler();

  // Auto-restore saved WhatsApp Web sessions for each tenant on startup
  try {
    const authDir = path.resolve(process.cwd(), './.wwebjs_auth');
    if (fs.existsSync(authDir)) {
      const entries = fs.readdirSync(authDir);
      for (const entry of entries) {
        const match = entry.match(/^session-tenant_(\d+)$/);
        if (match) {
          const bizId = parseInt(match[1], 10);
          console.log(`[whatsapp-web] Restoring saved session for Business #${bizId}...`);
          initWhatsAppWebClient(bizId).catch(() => {});
        }
      }
    }
  } catch (e) {
    console.warn('[whatsapp-web] Session scan error:', e.message);
  }
  initWhatsAppWebClient(1).catch(() => {});
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
