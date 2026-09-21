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

let qrCodeDataUrl = null;
let clientStatus = 'DISCONNECTED'; // 'DISCONNECTED' | 'INITIALIZING' | 'QR_READY' | 'CONNECTED'
let client = null; // WhatsApp Web Client with MobX media model patch
let isReconnecting = false;

export function getWhatsAppWebStatus() {
  return {
    status: clientStatus,
    qrCodeDataUrl,
  };
}

export async function killStaleBrowserProcesses() {
  if (client?.pupBrowser?.process) {
    try {
      const proc = client.pupBrowser.process();
      if (proc?.pid) {
        if (process.platform === 'win32') {
          spawn('taskkill', ['/F', '/T', '/PID', String(proc.pid)], { stdio: 'ignore', windowsHide: true });
        } else {
          try { process.kill(proc.pid, 'SIGKILL'); } catch (e) {}
        }
      }
    } catch (e) {}
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

export async function getWhatsAppWebDiagnostics() {
  let state = null;
  if (client) {
    try {
      state = await client.getState();
    } catch (e) {
      state = `getState error: ${e.message}`;
    }
  }
  return {
    clientStatus,
    hasClient: Boolean(client),
    hasQr: Boolean(qrCodeDataUrl),
    state,
    info: client?.info ? {
      wid: client.info.wid,
      pushname: client.info.pushname,
      platform: client.info.platform,
    } : null,
  };
}

export async function initWhatsAppWebClient() {
  if (client && (clientStatus === 'CONNECTED' || clientStatus === 'QR_READY')) {
    return;
  }
  if (client) {
    try { await client.destroy().catch(() => {}); } catch (e) {}
    client = null;
  }

  await killStaleBrowserProcesses();

  const executablePath = process.env.CHROME_PATH || CHROME_PATHS.find((p) => existsSync(p));
  clientStatus = 'INITIALIZING';
  qrCodeDataUrl = null;
  console.log('[whatsapp-web] Initializing client (executable:', executablePath || 'bundled', ')...');

  try {
    client = new Client({
      authStrategy: new LocalAuth({ dataPath: './.wwebjs_auth' }),
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
        qrCodeDataUrl = await QRCode.toDataURL(qr);
        clientStatus = 'QR_READY';
        console.log('📱 [whatsapp-web] QR code generated successfully and ready to scan!');
      } catch (err) {
        console.error('[whatsapp-web] Failed to generate QR data URL:', err.message);
      }
    });

    client.on('ready', () => {
      console.log('✅ WhatsApp Web Client is ready and connected!');
      clientStatus = 'CONNECTED';
      qrCodeDataUrl = null;
    });

    client.on('authenticated', () => {
      console.log('[whatsapp-web] Authenticated successfully.');
      clientStatus = 'CONNECTED';
      qrCodeDataUrl = null;
    });

    client.on('auth_failure', async (msg) => {
      console.error('[whatsapp-web] Authentication failure:', msg);
      clientStatus = 'DISCONNECTED';
      qrCodeDataUrl = null;
      try { await client.destroy().catch(() => {}); } catch (e) {}
      client = null;
      await killStaleBrowserProcesses();
      setTimeout(() => {
        reconnectWhatsAppWeb(true).catch((e) => console.warn('[whatsapp-web] Auto-restart on auth_failure failed:', e.message));
      }, 1000);
    });

    client.on('disconnected', async (reason) => {
      console.log('[whatsapp-web] Disconnected from device/web:', reason);
      clientStatus = 'DISCONNECTED';
      qrCodeDataUrl = null;
      try { await client.destroy().catch(() => {}); } catch (e) {}
      client = null;
      await killStaleBrowserProcesses();
      setTimeout(() => {
        reconnectWhatsAppWeb(true).catch((e) => console.warn('[whatsapp-web] Auto-restart on disconnect failed:', e.message));
      }, 1000);
    });

    client.initialize().catch((err) => {
      console.error('[whatsapp-web] Failed to initialize:', err.message);
      clientStatus = 'DISCONNECTED';
      client = null;
    });
  } catch (err) {
    console.error('[whatsapp-web] Setup error:', err.message);
    clientStatus = 'DISCONNECTED';
    client = null;
  }
}

export async function reconnectWhatsAppWeb(forceReset = false) {
  if (isReconnecting) {
    console.log('[whatsapp-web] Reconnect already in progress, returning status.');
    return getWhatsAppWebStatus();
  }
  isReconnecting = true;
  console.log('[whatsapp-web] reconnectWhatsAppWeb called:', JSON.stringify({ forceReset, currentStatus: clientStatus }));
  
  try {
    if (client) {
      try { await client.destroy().catch(() => {}); } catch (e) {}
      client = null;
    }

    await killStaleBrowserProcesses();
    await new Promise((resolve) => setTimeout(resolve, 600));

    clientStatus = 'INITIALIZING';
    qrCodeDataUrl = null;

    if (forceReset) {
      const authPath = path.resolve(process.cwd(), './.wwebjs_auth');
      for (let attempt = 0; attempt < 3; attempt++) {
        try {
          if (existsSync(authPath)) {
            fs.rmSync(authPath, { recursive: true, force: true });
            console.log('[whatsapp-web] Cleared .wwebjs_auth session cache.');
          }
          break;
        } catch (e) {
          console.warn(`[whatsapp-web] Clean attempt ${attempt + 1} failed: ${e.message}`);
          await killStaleBrowserProcesses();
          await new Promise((r) => setTimeout(r, 800));
        }
      }
    }

    await initWhatsAppWebClient();
  } finally {
    isReconnecting = false;
  }

  return getWhatsAppWebStatus();
}

export async function sendWhatsAppWebDocument(phone, docOptions, filenameArg, captionArg) {
  if (clientStatus !== 'CONNECTED' || !client) {
    initWhatsAppWebClient();
    throw new Error(`WhatsApp Web client is not connected yet (status=${clientStatus}). Please scan the QR code in Settings.`);
  }

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
  }

  if (!pdfBuffer) {
    throw new Error('Missing pdfBuffer in sendWhatsAppWebDocument');
  }

  const cleanPhone = (phone || '').replace(/\D/g, '');
  const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;
  const chatId = `${formattedPhone}@c.us`;

  const media = new MessageMedia('application/pdf', pdfBuffer.toString('base64'), filename);

  try {
    const sendResult = await client.sendMessage(chatId, media, { sendMediaAsDocument: true, caption });
    return { sent: true, method: 'whatsapp-web', messageId: sendResult?.id?._serialized };
  } catch (sendErr) {
    console.error(`[whatsapp-web] Document send failed to ${chatId}:`, sendErr.message);
    throw sendErr;
  }
}

export async function sendWhatsAppWebText(phone, text) {
  if (clientStatus !== 'CONNECTED' || !client) {
    initWhatsAppWebClient();
    throw new Error(`WhatsApp Web client is not connected yet (status=${clientStatus}). Please scan the QR code in Settings.`);
  }

  const cleanPhone = (phone || '').replace(/\D/g, '');
  const formattedPhone = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;
  const chatId = `${formattedPhone}@c.us`;

  try {
    const sendResult = await client.sendMessage(chatId, text);
    return { sent: true, method: 'whatsapp-web', messageId: sendResult?.id?._serialized };
  } catch (sendErr) {
    console.error(`[whatsapp-web] Text send failed to ${chatId}:`, sendErr.message);
    throw sendErr;
  }
}
