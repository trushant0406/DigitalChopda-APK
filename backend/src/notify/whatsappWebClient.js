import pkg from 'whatsapp-web.js';
const { Client, LocalAuth, MessageMedia } = pkg;
import qrcodeTerm from 'qrcode-terminal';
import QRCode from 'qrcode';
import fs, { existsSync } from 'node:fs';
import path from 'node:path';

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

export function getWhatsAppWebStatus() {
  return {
    status: clientStatus,
    qrCodeDataUrl,
  };
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

export function initWhatsAppWebClient() {
  if (client && (clientStatus === 'CONNECTED' || clientStatus === 'QR_READY')) {
    return;
  }
  if (client) {
    try { client.destroy().catch(() => {}); } catch (e) {}
    client = null;
  }

  const executablePath = process.env.CHROME_PATH || CHROME_PATHS.find((p) => existsSync(p));
  clientStatus = 'INITIALIZING';

  try {
    client = new Client({
      authStrategy: new LocalAuth({ dataPath: './.wwebjs_auth' }),
      puppeteer: {
        executablePath,
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
      },
    });

    client.on('qr', async (qr) => {
      try {
        qrcodeTerm.generate(qr, { small: true });
      } catch (e) {}

      try {
        qrCodeDataUrl = await QRCode.toDataURL(qr);
        clientStatus = 'QR_READY';
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

    client.on('auth_failure', (msg) => {
      console.error('[whatsapp-web] Authentication failure:', msg);
      clientStatus = 'DISCONNECTED';
      qrCodeDataUrl = null;
      try { client.destroy().catch(() => {}); } catch (e) {}
      client = null;
    });

    client.on('disconnected', (reason) => {
      console.log('[whatsapp-web] Disconnected:', reason);
      clientStatus = 'DISCONNECTED';
      qrCodeDataUrl = null;
      try { client.destroy().catch(() => {}); } catch (e) {}
      client = null;
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
  console.log('[whatsapp-web] reconnectWhatsAppWeb called:', JSON.stringify({ forceReset, currentStatus: clientStatus }));
  if (client) {
    try { await client.destroy().catch(() => {}); } catch (e) {}
    client = null;
  }
  clientStatus = 'DISCONNECTED';
  qrCodeDataUrl = null;

  if (forceReset) {
    try {
      const authPath = path.resolve(process.cwd(), './.wwebjs_auth');
      if (existsSync(authPath)) {
        fs.rmSync(authPath, { recursive: true, force: true });
        console.log('[whatsapp-web] Cleared .wwebjs_auth session cache.');
      }
    } catch (e) {
      console.warn('[whatsapp-web] Could not clean .wwebjs_auth directory:', e.message);
    }
  }

  initWhatsAppWebClient();
  return getWhatsAppWebStatus();
}

export async function sendWhatsAppWebDocument(phone, { pdfBuffer, filename, caption }) {
  if (clientStatus !== 'CONNECTED' || !client) {
    initWhatsAppWebClient();
    throw new Error(`WhatsApp Web client is not connected yet (status=${clientStatus}). Please scan the QR code in Settings.`);
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
