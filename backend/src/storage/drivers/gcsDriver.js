import { Storage } from '@google-cloud/storage';
import fs from 'fs';
import path from 'path';

/**
 * Google Cloud Storage Driver
 * Connects to GCS using Service Account credentials, Key file, or default ambient credentials.
 */
export class GCSStorageDriver {
  constructor(options = {}) {
    this.bucketName = options.bucketName || process.env.GCS_BUCKET_NAME;
    this.projectId = options.projectId || process.env.GCS_PROJECT_ID;
    this.customDomain = options.customDomain || process.env.GCS_CUSTOM_DOMAIN;

    if (!this.bucketName) {
      throw new Error('GCSStorageDriver: GCS_BUCKET_NAME is required');
    }

    const storageOptions = {};
    if (this.projectId) storageOptions.projectId = this.projectId;

    let keyFile = options.keyFilename || process.env.GOOGLE_APPLICATION_CREDENTIALS || process.env.GCS_KEY_FILE;
    if (keyFile) {
      // Normalize slashes and trim
      keyFile = keyFile.trim();
      let candidates = [
        keyFile,
        keyFile.replace(/^[/\\]backend[/\\]/, ''),
        keyFile.replace(/^[/\\]/, ''),
        path.resolve(process.cwd(), keyFile),
        path.resolve(process.cwd(), keyFile.replace(/^[/\\]backend[/\\]/, '')),
        path.resolve(process.cwd(), 'backend', path.basename(keyFile)),
        path.resolve(path.dirname(process.cwd()), 'backend', path.basename(keyFile)),
      ];

      let foundPath = candidates.find((p) => p && fs.existsSync(p));
      if (foundPath) {
        storageOptions.keyFilename = foundPath;
      } else {
        throw new Error(`Service account key file not found at: ${keyFile}`);
      }
    } else if (process.env.GCS_CLIENT_EMAIL && process.env.GCS_PRIVATE_KEY && !process.env.GCS_CLIENT_EMAIL.includes('my-service-account')) {
      storageOptions.credentials = {
        client_email: process.env.GCS_CLIENT_EMAIL,
        private_key: process.env.GCS_PRIVATE_KEY.replace(/\\n/g, '\n'),
      };
    }

    this.storage = new Storage(storageOptions);
    this.bucket = this.storage.bucket(this.bucketName);
  }

  async uploadFile({ key, buffer, contentType = 'application/octet-stream', isPublic = true }) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const file = this.bucket.file(safeKey);

    await file.save(buffer, {
      contentType,
      resumable: false,
      metadata: {
        contentType,
        cacheControl: 'public, max-age=31536000',
      },
    });

    return {
      key: safeKey,
      url: this.getUrl(safeKey),
      provider: 'gcs',
      size: buffer.length,
      contentType,
    };
  }

  async getFileBuffer(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const file = this.bucket.file(safeKey);
    const [buffer] = await file.download();
    return buffer;
  }

  getFileStream(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const file = this.bucket.file(safeKey);
    return file.createReadStream();
  }

  async exists(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const file = this.bucket.file(safeKey);
    const [exists] = await file.exists();
    return exists;
  }

  async deleteFile(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const file = this.bucket.file(safeKey);
    try {
      await file.delete({ ignoreNotFound: true });
      return true;
    } catch (err) {
      if (err.code === 404) return false;
      throw err;
    }
  }

  getUrl(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    if (this.customDomain) {
      const domain = this.customDomain.replace(/\/+$/, '');
      return `${domain}/${safeKey}`;
    }
    return `https://storage.googleapis.com/${this.bucketName}/${safeKey}`;
  }
}
