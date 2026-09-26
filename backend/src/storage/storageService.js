import { LocalStorageDriver } from './drivers/localDriver.js';
import { GCSStorageDriver } from './drivers/gcsDriver.js';
import { S3StorageDriver } from './drivers/s3Driver.js';

let activeDriver = null;
let activeDriverName = 'local';

function initializeStorage() {
  const provider = (process.env.STORAGE_PROVIDER || (process.env.GCS_BUCKET_NAME ? 'gcs' : 'local')).toLowerCase();

  if (provider === 'gcs') {
    try {
      activeDriver = new GCSStorageDriver();
      activeDriverName = 'gcs';
      console.log(`[Storage] Initialized Google Cloud Storage (Bucket: ${process.env.GCS_BUCKET_NAME})`);
      return;
    } catch (err) {
      console.warn(`[Storage] GCS initialization failed: ${err.message}. Falling back to local storage.`);
    }
  } else if (['s3', 'r2', 'aws', 'aws-s3', 'aws_s3'].includes(provider)) {
    try {
      activeDriver = new S3StorageDriver();
      activeDriverName = 's3';
      console.log(`[Storage] Initialized S3-compatible storage (Bucket: ${process.env.S3_BUCKET_NAME})`);
      return;
    } catch (err) {
      console.warn(`[Storage] S3 initialization failed: ${err.message}. Falling back to local storage.`);
    }
  }

  activeDriver = new LocalStorageDriver();
  activeDriverName = 'local';
  console.log('[Storage] Initialized Local filesystem storage driver (./uploads)');
}

// Lazy or immediate initialization
function getDriver() {
  if (!activeDriver) {
    initializeStorage();
  }
  return activeDriver;
}

export const storageService = {
  /**
   * Upload a file buffer
   * @param {Object} options
   * @param {string} options.key - Unique file key/path (e.g. 'invoices/INV-1001.pdf')
   * @param {Buffer} options.buffer - File buffer
   * @param {string} [options.contentType] - MIME type
   * @param {boolean} [options.isPublic=true] - Public access flag
   * @returns {Promise<{ key: string, url: string, provider: string, size: number }>}
   */
  async uploadFile(options) {
    return getDriver().uploadFile(options);
  },

  /**
   * Get file as a Buffer
   * @param {string} key
   * @returns {Promise<Buffer>}
   */
  async getFileBuffer(key) {
    return getDriver().getFileBuffer(key);
  },

  /**
   * Get file as a readable stream
   * @param {string} key
   * @returns {ReadableStream|fs.ReadStream}
   */
  getFileStream(key) {
    return getDriver().getFileStream(key);
  },

  /**
   * Check if a file exists
   * @param {string} key
   * @returns {Promise<boolean>}
   */
  async exists(key) {
    return getDriver().exists(key);
  },

  /**
   * Delete a file
   * @param {string} key
   * @returns {Promise<boolean>}
   */
  async deleteFile(key) {
    return getDriver().deleteFile(key);
  },

  /**
   * Get public or standard URL for a file
   * @param {string} key
   * @returns {string}
   */
  getUrl(key) {
    return getDriver().getUrl(key);
  },

  /**
   * Get the active driver name ('gcs', 's3', 'r2', 'local')
   * @returns {string}
   */
  getDriverName() {
    getDriver();
    return activeDriverName;
  },

  /**
   * Returns true if using cloud object storage (GCS/S3)
   * @returns {boolean}
   */
  isCloud() {
    getDriver();
    return activeDriverName !== 'local';
  },

  /**
   * Re-initialize storage (e.g. after testing or changing env)
   */
  reinitialize() {
    activeDriver = null;
    initializeStorage();
  },
};
