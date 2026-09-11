import puppeteer from 'puppeteer-core';

const CHROME_PATHS = [
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium-browser',
  '/usr/bin/chromium',
];

export async function renderHtmlToPdf(html) {
  const { existsSync } = await import('node:fs');
  const executablePath = process.env.CHROME_PATH || CHROME_PATHS.find((p) => existsSync(p));
  if (!executablePath) {
    throw new Error('No Chrome/Chromium install found. Set CHROME_PATH to point at one.');
  }

  const browser = await puppeteer.launch({ executablePath, headless: true });
  try {
    const page = await browser.newPage();
    await page.setContent(html, { waitUntil: 'networkidle0' });
    const pdf = await page.pdf({ format: 'A4', printBackground: true, margin: { top: '0', bottom: '0', left: '0', right: '0' } });
    return Buffer.from(pdf);
  } finally {
    await browser.close();
  }
}
