import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const targetFile = path.resolve(__dirname, '../node_modules/whatsapp-web.js/src/util/Injected/Utils.js');

if (!fs.existsSync(targetFile)) {
  console.log('[patch-wwebjs] Utils.js not found (node_modules might not be installed yet). Skipping.');
  process.exit(0);
}

let content = fs.readFileSync(targetFile, 'utf8');

// Target the broken line that spreads the MobX model instance ...mediaOptions directly into message
const brokenPattern = `            type: 'chat',
            ...ephemeralFields,
            ...mediaOptions,
            ...(mediaOptions.toJSON ? mediaOptions.toJSON() : {}),`;

const patchedPattern = `            isNewMsg: true,
            type: 'chat',
            ...ephemeralFields,
            ...(mediaOptions.toJSON ? mediaOptions.toJSON() : mediaOptions),
            caption: mediaOptions.caption,
            isViewOnce: mediaOptions.isViewOnce,`;

if (content.includes('...mediaOptions,')) {
  content = content.replace(
    /type:\s*'chat',\s*\.\.\.ephemeralFields,\s*\.\.\.mediaOptions,\s*\.\.\.\(mediaOptions\.toJSON\s*\?\s*mediaOptions\.toJSON\(\)\s*:\s*\{\}\),/,
    `type: 'chat',
            ...ephemeralFields,
            ...(mediaOptions.toJSON ? mediaOptions.toJSON() : mediaOptions),
            caption: mediaOptions.caption,
            isViewOnce: mediaOptions.isViewOnce,`
  );
  fs.writeFileSync(targetFile, content, 'utf8');
  console.log('[patch-wwebjs] Successfully patched whatsapp-web.js Utils.js to fix MobX media model memoize bug.');
} else {
  console.log('[patch-wwebjs] whatsapp-web.js Utils.js is already patched.');
}
