import {
  S3Client,
  PutObjectCommand,
  GetObjectCommand,
  HeadObjectCommand,
  DeleteObjectCommand,
} from '@aws-sdk/client-s3';

/**
 * Universal S3-compatible Driver
 * Works with Cloudflare R2, AWS S3, MinIO, Wasabi, Backblaze B2, and Google Cloud Storage (HMAC S3 Interop).
 */
export class S3StorageDriver {
  constructor(options = {}) {
    this.bucketName = options.bucketName || process.env.S3_BUCKET_NAME;
    this.endpoint = (options.endpoint || process.env.S3_ENDPOINT || '').trim();
    this.region = (options.region || process.env.S3_REGION || process.env.AWS_REGION || '').trim();
    if (!this.region) {
      this.region = this.endpoint ? 'auto' : 'ap-south-1';
    }
    this.publicBaseUrl = (options.publicBaseUrl || process.env.S3_PUBLIC_BASE_URL || '').trim();

    if (!this.bucketName) {
      throw new Error('S3StorageDriver: S3_BUCKET_NAME is required');
    }

    const clientConfig = {
      region: this.region,
    };

    const accessKeyId = options.accessKeyId || process.env.S3_ACCESS_KEY_ID || process.env.AWS_ACCESS_KEY_ID;
    const secretAccessKey = options.secretAccessKey || process.env.S3_SECRET_ACCESS_KEY || process.env.AWS_SECRET_ACCESS_KEY;
    if (accessKeyId && secretAccessKey) {
      clientConfig.credentials = {
        accessKeyId: accessKeyId.trim(),
        secretAccessKey: secretAccessKey.trim(),
      };
    }

    if (this.endpoint) {
      clientConfig.endpoint = this.endpoint;
    }

    // Needed for Cloudflare R2 / MinIO / GCS
    const forcePathStyle = options.forcePathStyle !== undefined
      ? options.forcePathStyle
      : (process.env.S3_FORCE_PATH_STYLE === 'true');
    if (forcePathStyle) {
      clientConfig.forcePathStyle = true;
    }

    this.client = new S3Client(clientConfig);
  }

  async uploadFile({ key, buffer, contentType = 'application/octet-stream' }) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const command = new PutObjectCommand({
      Bucket: this.bucketName,
      Key: safeKey,
      Body: buffer,
      ContentType: contentType,
      CacheControl: 'public, max-age=31536000',
    });

    await this.client.send(command);

    return {
      key: safeKey,
      url: this.getUrl(safeKey),
      provider: 's3',
      size: buffer.length,
      contentType,
    };
  }

  async getFileBuffer(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const command = new GetObjectCommand({
      Bucket: this.bucketName,
      Key: safeKey,
    });
    const response = await this.client.send(command);
    const byteArray = await response.Body.transformToByteArray();
    return Buffer.from(byteArray);
  }

  async getFileStream(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const command = new GetObjectCommand({
      Bucket: this.bucketName,
      Key: safeKey,
    });
    const response = await this.client.send(command);
    return response.Body;
  }

  async exists(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    try {
      const command = new HeadObjectCommand({
        Bucket: this.bucketName,
        Key: safeKey,
      });
      await this.client.send(command);
      return true;
    } catch (err) {
      if (err.name === 'NotFound' || err.$metadata?.httpStatusCode === 404) {
        return false;
      }
      throw err;
    }
  }

  async deleteFile(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    const command = new DeleteObjectCommand({
      Bucket: this.bucketName,
      Key: safeKey,
    });
    await this.client.send(command);
    return true;
  }

  getUrl(key) {
    const safeKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
    if (this.publicBaseUrl) {
      const base = this.publicBaseUrl.replace(/\/+$/, '');
      return `${base}/${safeKey}`;
    }
    if (this.endpoint) {
      const base = this.endpoint.replace(/\/+$/, '');
      return `${base}/${this.bucketName}/${safeKey}`;
    }
    return `https://${this.bucketName}.s3.${this.region}.amazonaws.com/${safeKey}`;
  }
}
