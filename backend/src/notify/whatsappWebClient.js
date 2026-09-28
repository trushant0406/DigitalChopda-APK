import pkg from 'whatsapp-web.js';
const { Client, LocalAuth, MessageMedia } = pkg;
import qrcodeTerm from 'qrcode-terminal';
import QRCode from 'qrcode';
import fs, { existsSync } from 'node:fs';
import path from 'node:path';
import { spawn } from 'node:child_process';

const CHROME_PATHS = [
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium-browser',
  '/usr/bin/chromium',
];

// Multi-Tenant Session Registry: businessId -> Session Object
const tenantSessions = new Map();

function getTenantSession(bizId = 1) {
  const id = Number(bizId) || 1;
  if (!tenantSessions.has(id)) {
    tenantSessions.set(id, {
      businessId: id,
      client: null,
      clientStatus: 'DISCONNECTED', // 'DISCONNECTED' | 'INITIALIZING' | 'QR_READY' | 'CONNECTED'
      qrCodeDataUrl: null,
      info: null,
      isReconnecting: false,
    });
  }
  return tenantSessions.get(id);
}

export function getWhatsAppWebStatus(bizId = 1) {
  const session = getTenantSession(bizId);
  return {
    businessId: session.businessId,
    status: session.clientStatus,
    qrCodeDataUrl: session.qrCodeDataUrl,
  };
}

export async function killStaleBrowserProcesses(bizId = null) {
  if (bizId) {
    const session = tenantSessions.get(Number(bizId));
    if (session?.client?.pupBrowser?.process) {
      try {
        const proc = session.client.pupBrowser.process();
        if (proc?.pid) {
          if (process.platform === 'win32') {
            spawn('taskkill', ['/F', '/T', '/PID', String(proc.pid)], { stdio: 'ignore', windowsHide: true });
          } else {
            try { process.kill(proc.pid, 'SIGKILL'); } catch (e) {}
          }
        }
      } catch (e) {}
    }
    return;
  }

  // If no bizId given, iterate all known sessions
  for (const session of tenantSessions.values()) {
    if (session?.client?.pupBrowser?.process) {
      try {
        const proc = session.client.pupBrowser.process();
        if (proc?.pid) {
          if (process.platform === 'win32') {
            spawn('taskkill', ['/F', '/T', '/PID', String(proc.pid)], { stdio: 'ignore', windowsHide: true });
          } else {
            try { process.kill(proc.pid, 'SIGKILL'); } catch (e) {}
          }
        }
      } catch (e) {}
    }
  }

  if (process.platform === 'win32') {
    await new Promise((resolve) => {
      const ps = spawn('powershell.exe', [
        '-NoProfile',
        '-NonInteractive',
        '-Command',
        `Get-CimInstance Win32_Process -Filter "name = 'chrome.exe' or name = 'msedge.exe'" | Where-Object { $_.CommandLine -like '*wwebjs_auth*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }`
      ], { stdio: 'ignore', windowsHide: true });
      ps.on('close', () => resolve());
      ps.on('error', () => resolve());
      setTimeout(resolve, 2500);
    });
  }
}

export function cleanChromiumLocks(dir = './.wwebjs_auth') {
  if (!fs.existsSync(dir)) return;
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        cleanChromiumLocks(fullPath);
      } else if (entry.name.startsWith('Singleton')) {
        try {
          fs.unlinkSync(fullPath);
        } catch (e) {}
      }
    }
  } catch (e) {}
}

export async function getWhatsAppWebDiagnostics(bizId = 1) {
  const session = getTenantSession(bizId);
  let state = null;
  if (session.client) {
    try {
      state = await session.client.getState();
    } catch (e) {
      state = `getState error: ${e.message}`;
    }
  }
  return {
    businessId: session.businessId,
    clientStatus: session.clientStatus,
    hasClient: Boolean(session.client),
    hasQr: Boolean(session.qrCodeDataUrl),
    state,
    info: session.info || (session.client?.info ? {
      wid: session.client.info.wid,
      pushname: session.client.info.pushname,
      platform: session.client.info.platform,
    } : null),
  };
}

export async function initWhatsAppWebClient(bizId = 1) {
  const session = getTenantSession(bizId);
  if (session.client && (session.clientStatus === 'CONNECTED' || session.clientStatus === 'QR_READY')) {
    return;
  }
  if (session.client) {
    try { await session.client.destroy().catch(() => {}); } catch (e) {}
    session.client = null;
  }

  const clientId = `tenant_${session.businessId}`;
  const sessionAuthPath = path.resolve(process.cwd(), `./.wwebjs_auth/session-${clientId}`);
  cleanChromiumLocks(sessionAuthPath);

  const executablePath = process.env.CHROME_PATH || CHROME_PATHS.find((p) => existsSync(p));
  session.clientStatus = 'INITIALIZING';
  session.qrCodeDataUrl = null;
  console.log(`[whatsapp-web][Tenant ${session.businessId}] Initializing client (executable: ${executablePath || 'bundled'})...`);

  try {
    const client = new Client({
      authStrategy: new LocalAuth({
        clientId,
        dataPath: './.wwebjs_auth',
      }),
      puppeteer: {
        executablePath,
        headless: true,
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-gpu',
          '--no-first-run',
          '--no-default-browser-check',
        ],
      },
    });

    client.on('qr', async (qr) => {
      try {
        qrcodeTerm.generate(qr, { small: true });
      } catch (e) {}

      try {
        session.qrCodeDataUrl = await QRCode.toDataURL(qr);
        session.clientStatus = 'QR_READY';
        console.log(`📱 [whatsapp-web][Tenant ${session.businessId}] QR code generated successfully and ready to scan!`);
      } catch (err) {
        console.error(`[whatsapp-web][Tenant ${session.businessId}] Failed to generate QR data URL:`, err.message);
      }
    });

    client.on('ready', () => {
      console.log(`✅ [whatsapp-web][Tenant ${session.businessId}] Client is ready and connected!`);
      session.clientStatus = 'CONNECTED';
      session.qrCodeDataUrl = null;
      if (client.info) {
        session.info = {
          wid: client.info.wid,
          pushname: client.info.pushname,
          platform: client.info.platform,
        };
      }
    });

    client.on('authenticated', () => {
      console.log(`[whatsapp-web][Tenant ${session.businessId}] Authenticated successfully.`);
      session.clientStatus = 'CONNECTED';
      session.qrCodeDataUrl = null;
    });

    client.on('auth_failure', async (msg) => {
      console.error(`[whatsapp-web][Tenant ${session.businessId}] Authentication failure:`, msg);
      session.clientStatus = 'DISCONNECTED';
      session.qrCodeDataUrl = null;
      session.info = null;
      try { await client.destroy().catch(() => {}); } catch (e) {}
      session.client = null;
      setTimeout(() => {
        reconnectWhatsAppWeb(session.businessId, true).catch((e) =>
          console.warn(`[whatsapp-web][Tenant ${session.businessId}] Auto-restart on auth_failure failed:`, e.message)
        );
      }, 1000);
    });

    client.on('disconnected', async (reason) => {
      console.log(`[whatsapp-web][Tenant ${session.businessId}] Disconnected:`, reason);
      session.clientStatus = 'DISCONNECTED';
      session.qrCodeDataUrl = null;
      session.info = null;
      try { await client.destroy().catch(() => {}); } catch (e) {}
      session.client = null;
      setTimeout(() => {
        reconnectWhatsAppWeb(session.businessId, true).catch((e) =>
          console.warn(`[whatsapp-web][Tenant ${session.businessId}] Auto-restart on disconnect failed:`, e.message)
        );
      }, 1000);
    });

    session.client = client;
    client.initialize().catch((err) => {
      console.error(`[whatsapp-web][Tenant ${session.businessId}] Failed to initialize:`, err.message);
      session.clientStatus = 'DISCONNECTED';
      session.client = null;
    });
  } catch (err) {
    console.error(`[whatsapp-web][Tenant ${session.businessId}] Setup error:`, err.message);
    session.clientStatus = 'DISCONNECTED';
    session.client = null;
  }
}

export async function reconnectWhatsAppWeb(bizId = 1, forceReset = false) {
  const session = getTenantSession(bizId);
  if (session.isReconnecting) {
    console.log(`[whatsapp-web][Tenant ${session.businessId}] Reconnect already in progress.`);
    return getWhatsAppWebStatus(session.businessId);
  }
  session.isReconnecting = true;
  console.log(`[whatsapp-web][Tenant ${session.businessId}] reconnectWhatsAppWeb called: forceReset=${forceReset}`);

  try {
    if (session.client) {
      try { await session.client.logout().catch(() => {}); } catch (e) {}
      try { await session.client.destroy().catch(() => {}); } catch (e) {}
      session.client = null;
    }

    session.clientStatus = 'INITIALIZING';
    session.qrCodeDataUrl = null;
    session.info = null;

    if (forceReset) {
      const clientId = `tenant_${session.businessId}`;
      const sessionPath = path.resolve(process.cwd(), `./.wwebjs_auth/session-${clientId}`);
      if (existsSync(sessionPath)) {
        try {
          fs.rmSync(sessionPath, { recursive: true, force: true });
          console.log(`[whatsapp-web][Tenant ${session.businessId}] Deleted session auth directory: ${sessionPath}`);
        } catch (rmErr) {
          console.warn(`[whatsapp-web][Tenant ${session.businessId}] Could not remove session dir:`, rmErr.message);
        }
      }
    }

    await new Promise((resolve) => setTimeout(resolve, 600));
    await initWhatsAppWebClient(session.businessId);
    return getWhatsAppWebStatus(session.businessId);
  } finally {
    session.isReconnecting = false;
  }
}

export async function destroyWhatsAppWebClient(bizId = null) {
  if (bizId) {
    const session = tenantSessions.get(Number(bizId));
    if (session?.client) {
      try { await session.client.destroy().catch(() => {}); } catch (e) {}
      session.client = null;
    }
    return;
  }

  for (const session of tenantSessions.values()) {
    if (session?.client) {
      try { await session.client.destroy().catch(() => {}); } catch (e) {}
      session.client = null;
    }
  }
  await killStaleBrowserProcesses();
  cleanChromiumLocks('./.wwebjs_auth');
}

export async function sendWhatsAppWebDocument(phone, docOptions, filenameArg, captionArg) {
  let businessId = 1;
  let pdfBuffer;
  let filename = 'document.pdf';
  let caption = '';

  if (Buffer.isBuffer(docOptions)) {
    pdfBuffer = docOptions;
    if (filenameArg) filename = filenameArg;
    if (captionArg) caption = captionArg;
  } else if (docOptions && typeof docOptions === 'object') {
    pdfBuffer = docOptions.pdfBuffer;
    filename = docOptions.filename || docOptions.fileName || filename;
    caption = docOptions.caption || '';
    if (docOptions.businessId) {
      businessId = Number(docOptions.businessId);
    }
  }

  const session = getTenantSession(businessId);
  if (session.clientStatus !== 'CONNECTED' || !session.client) {
    initWhatsAppWebClient(businessId);
    throw new Error(`WhatsApp Web client for Store #${businessId} is not connected yet (status=${session.clientStatus}). Please scan the QR code in Settings.`);
  }

  const cleanPhone = (phone || '').replace(/\D/g, '');
  const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;
  const chatId = `${formattedPhone}@c.us`;

  const media = new MessageMedia('application/pdf', pdfBuffer.toString('base64'), filename);

  try {
    const sendResult = await session.client.sendMessage(chatId, media, { sendMediaAsDocument: true, caption });
    return { sent: true, method: 'whatsapp-web', messageId: sendResult?.id?._serialized };
  } catch (sendErr) {
    console.error(`[whatsapp-web][Tenant ${businessId}] Document send failed to ${chatId}:`, sendErr.message);
    throw sendErr;
  }
}

export async function sendWhatsAppWebText(phone, text, businessId = 1) {
  const session = getTenantSession(businessId);
  if (session.clientStatus !== 'CONNECTED' || !session.client) {
    initWhatsAppWebClient(businessId);
    throw new Error(`WhatsApp Web client for Store #${businessId} is not connected yet (status=${session.clientStatus}). Please scan the QR code in Settings.`);
  }

  const cleanPhone = (phone || '').replace(/\D/g, '');
  const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;
  const chatId = `${formattedPhone}@c.us`;

  try {
    const sendResult = await session.client.sendMessage(chatId, text);
    return { sent: true, method: 'whatsapp-web', messageId: sendResult?.id?._serialized };
  } catch (sendErr) {
    console.error(`[whatsapp-web][Tenant ${businessId}] Text send failed to ${chatId}:`, sendErr.message);
    throw sendErr;
  }
}
