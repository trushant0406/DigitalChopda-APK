import fs from 'fs';
import path from 'path';

/**
 * Local filesystem driver
 * Stores files under the local uploads directory or custom root directory.
 */
export class LocalStorageDriver {
  constructor(options = {}) {
    this.baseDir = options.baseDir || path.join(process.cwd(), 'uploads');
    fs.mkdirSync(this.baseDir, { recursive: true });
    this.publicBaseUrl = options.publicBaseUrl || '/uploads';
  }

  _resolvePath(key) {
    // Prevent directory traversal
    const safeKey = key.replace(/\.\./g, '').replace(/^[/\\]+/, '');
    return path.join(this.baseDir, safeKey);
  }

  async uploadFile({ key, buffer, contentType }) {
    const filePath = this._resolvePath(key);
    const dir = path.dirname(filePath);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(filePath, buffer);
    return {
      key,
      url: this.getUrl(key),
      provider: 'local',
      size: buffer.length,
      contentType,
    };
  }

  async getFileBuffer(key) {
    const filePath = this._resolvePath(key);
    if (!fs.existsSync(filePath)) {
      throw new Error(`File not found: ${key}`);
    }
    return fs.readFileSync(filePath);
  }

  getFileStream(key) {
    const filePath = this._resolvePath(key);
    if (!fs.existsSync(filePath)) {
      throw new Error(`File not found: ${key}`);
    }
    return fs.createReadStream(filePath);
  }

  async exists(key) {
    const filePath = this._resolvePath(key);
    return fs.existsSync(filePath);
  }

  async deleteFile(key) {
    const filePath = this._resolvePath(key);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
      return true;
    }
    return false;
  }

  getUrl(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    return `${this.publicBaseUrl}/${safeKey}`;
  }
}
