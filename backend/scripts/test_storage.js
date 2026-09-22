import 'dotenv/config';
import { storageService } from '../src/storage/storageService.js';
import { LocalStorageDriver } from '../src/storage/drivers/localDriver.js';
import { GCSStorageDriver } from '../src/storage/drivers/gcsDriver.js';
import { S3StorageDriver } from '../src/storage/drivers/s3Driver.js';

async function runTests() {
  console.log('--- Testing Storage Service Architecture ---');

  // Test 1: Verify classes exist
  console.log('✓ LocalStorageDriver defined:', typeof LocalStorageDriver === 'function');
  console.log('✓ GCSStorageDriver defined:', typeof GCSStorageDriver === 'function');
  console.log('✓ S3StorageDriver defined:', typeof S3StorageDriver === 'function');

  // Test 2: Active Driver defaults to local (or GCS if configured)
  const driverName = storageService.getDriverName();
  console.log(`✓ Active Driver Name: ${driverName}`);

  // Test 3: Upload a test buffer
  const testKey = `test/smoke_test_${Date.now()}.txt`;
  const testContent = Buffer.from('Hello from DigitalChopda Universal Object Storage!');
  const uploadResult = await storageService.uploadFile({
    key: testKey,
    buffer: testContent,
    contentType: 'text/plain',
  });

  console.log('✓ Upload result:', {
    key: uploadResult.key,
    provider: uploadResult.provider,
    url: uploadResult.url,
    size: uploadResult.size,
  });

  // Test 4: Existence check
  const exists = await storageService.exists(testKey);
  if (!exists) throw new Error('File should exist after upload');
  console.log('✓ Exists check passed');

  // Test 5: Buffer retrieval & parity
  const downloadedBuffer = await storageService.getFileBuffer(testKey);
  if (downloadedBuffer.toString() !== testContent.toString()) {
    throw new Error('Downloaded buffer content does not match uploaded content');
  }
  console.log('✓ Content parity verified:', downloadedBuffer.toString());

  // Test 6: URL resolution
  const url = storageService.getUrl(testKey);
  console.log('✓ Resolved URL:', url);

  // Test 7: Delete file
  const deleted = await storageService.deleteFile(testKey);
  if (!deleted) throw new Error('File deletion failed');
  const existsAfterDelete = await storageService.exists(testKey);
  if (existsAfterDelete) throw new Error('File should not exist after deletion');
  console.log('✓ File deleted and verified clean');

  console.log('\n✅ All Storage Service tests passed successfully!\n');
}

runTests().catch((err) => {
  console.error('❌ Storage test failed:', err);
  process.exit(1);
});
