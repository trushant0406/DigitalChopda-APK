import puppeteer from 'puppeteer-core';
import { existsSync } from 'node:fs';

const CHROME_PATHS = [
  // Windows
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  // macOS
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
  // Linux
  '/usr/bin/google-chrome',
  '/usr/bin/chromium-browser',
  '/usr/bin/chromium',
];

// Bounded FIFO concurrency limiter to protect CPU and memory from OOM crashes
class ConcurrencyLimiter {
  constructor(maxConcurrency) {
    this.maxConcurrency = maxConcurrency;
    this.running = 0;
    this.queue = [];
  }

  async acquire() {
    if (this.running < this.maxConcurrency) {
      this.running++;
      return;
    }
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        const idx = this.queue.findIndex((item) => item.resolve === resolve);
        if (idx !== -1) this.queue.splice(idx, 1);
        reject(new Error('PDF generation queue timeout. Server is under high load, please retry.'));
      }, 30000);
      this.queue.push({ resolve, reject, timeout });
    });
  }

  release() {
    this.running--;
    if (this.queue.length > 0) {
      const next = this.queue.shift();
      clearTimeout(next.timeout);
      this.running++;
      next.resolve();
    }
  }

  getStatus() {
    return { running: this.running, queued: this.queue.length, max: this.maxConcurrency };
  }
}

// Default max 2 concurrent Chromium PDF renders to guarantee memory stability
const limiter = new ConcurrencyLimiter(parseInt(process.env.PDF_MAX_CONCURRENCY || '2', 10));

export function getPdfRendererStatus() {
  return limiter.getStatus();
}

export async function renderHtmlToPdf(html) {
  await limiter.acquire();
  try {
    return await executeRender(html);
  } finally {
    limiter.release();
  }
}

async function executeRender(html) {
  const executablePath = process.env.CHROME_PATH || CHROME_PATHS.find((p) => existsSync(p));
  if (!executablePath) {
    throw new Error('No Chrome/Chromium install found. Set CHROME_PATH to point at one.');
  }

  const browser = await puppeteer.launch({
    executablePath,
    headless: true,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--no-first-run',
      '--no-zygote',
      '--disable-background-networking',
      '--disable-background-timer-throttling',
      '--disable-breakpad',
      '--disable-client-side-phishing-detection',
      '--disable-default-apps',
      '--disable-extensions',
      '--disable-features=Translate',
      '--disable-hang-monitor',
      '--disable-ipc-flooding-protection',
      '--disable-popup-blocking',
      '--disable-prompt-on-repost',
      '--disable-renderer-backgrounding',
      '--disable-sync',
      '--disable-translate',
      '--metrics-recording-only',
      '--mute-audio',
      '--no-default-browser-check',
    ],
  });

  const proc = browser.process();
  let page = null;

  try {
    page = await browser.newPage();
    page.setDefaultTimeout(15000);
    page.setDefaultNavigationTimeout(15000);

    // Render HTML with strict 15s timeout
    await page.setContent(html, { waitUntil: 'networkidle0', timeout: 15000 });
    const pdf = await page.pdf({
      format: 'A4',
      printBackground: true,
      margin: { top: '0', bottom: '0', left: '0', right: '0' },
      timeout: 15000,
    });
    return Buffer.from(pdf);
  } finally {
    if (page) {
      try { await page.close().catch(() => {}); } catch (_) {}
    }
    if (browser) {
      try {
        await Promise.race([
          browser.close(),
          new Promise((_, reject) => setTimeout(() => reject(new Error('browser.close timeout')), 3000)),
        ]).catch(() => {
          if (proc?.pid) {
            try { proc.kill('SIGKILL'); } catch (_) {}
          }
        });
      } catch (_) {
        if (proc?.pid) {
          try { proc.kill('SIGKILL'); } catch (_) {}
        }
      }
    }
  }
}
