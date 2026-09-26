import path from 'node:path';
import { fileURLToPath } from 'node:url';
import dotenv from 'dotenv';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.resolve(__dirname, '../.env') });
dotenv.config({ path: path.resolve(__dirname, '../../.env') });
import { S3StorageDriver } from '../src/storage/drivers/s3Driver.js';

async function testS3Connection() {
  console.log('\n' + '='.repeat(60));
  console.log('🧪 DIGITALCHOPDA.IN — AWS S3 CONNECTIVITY TEST');
  console.log('='.repeat(60));

  const bucket = process.env.S3_BUCKET_NAME;
  const region = process.env.S3_REGION || 'ap-south-1';
  const accessKey = process.env.S3_ACCESS_KEY_ID || process.env.AWS_ACCESS_KEY_ID;
  const endpoint = process.env.S3_ENDPOINT;

  console.log(`\n📋 Configuration:`);
  console.log(`   Bucket:     ${bucket || '(Not Set)'}`);
  console.log(`   Region:     ${region}`);
  console.log(`   Endpoint:   ${endpoint || '(Standard AWS S3)'}`);
  console.log(`   Access Key: ${accessKey ? accessKey.slice(0, 4) + '...' + accessKey.slice(-4) : '(IAM Role / Default Provider)'}`);

  if (!bucket || bucket === 'digitalchopda-documents-prod' && accessKey === 'AKIAIOSFODNN7EXAMPLE') {
    console.warn('\n⚠️  WARNING: Placeholder AWS credentials detected in .env!');
    console.warn('   Please replace S3_BUCKET_NAME, S3_ACCESS_KEY_ID, and S3_SECRET_ACCESS_KEY');
    console.warn('   with your actual AWS credentials before running this test against real AWS.\n');
  }

  let driver;
  try {
    driver = new S3StorageDriver();
  } catch (err) {
    console.error(`\n❌ Failed to instantiate S3 driver: ${err.message}\n`);
    process.exit(1);
  }

  const testKey = `test-connectivity/ping-${Date.now()}.txt`;
  const testPayload = `DigitalChopda S3 Healthcheck at ${new Date().toISOString()}`;

  try {
    console.log(`\n1️⃣  Testing S3 Upload (PutObject)...`);
    const uploadRes = await driver.uploadFile({
      key: testKey,
      buffer: Buffer.from(testPayload, 'utf-8'),
      contentType: 'text/plain',
    });
    console.log(`   ✅ Upload successful!`);
    console.log(`      Key:  ${uploadRes.key}`);
    console.log(`      URL:  ${uploadRes.url}`);

    console.log(`\n2️⃣  Testing S3 Existence Check (HeadObject)...`);
    const exists = await driver.exists(testKey);
    if (!exists) throw new Error('File was uploaded but driver.exists returned false');
    console.log(`   ✅ Existence confirmed.`);

    console.log(`\n3️⃣  Testing S3 Download (GetObject)...`);
    const downloadedBuf = await driver.getFileBuffer(testKey);
    const downloadedText = downloadedBuf.toString('utf-8');
    if (downloadedText !== testPayload) {
      throw new Error(`Data mismatch: Expected "${testPayload}", got "${downloadedText}"`);
    }
    console.log(`   ✅ Content verified successfully (${downloadedBuf.length} bytes).`);

    console.log(`\n4️⃣  Testing S3 Cleanup (DeleteObject)...`);
    await driver.deleteFile(testKey);
    const stillExists = await driver.exists(testKey);
    if (stillExists) throw new Error('File delete succeeded but file still exists');
    console.log(`   ✅ Test file cleaned up successfully.`);

    console.log('\n' + '='.repeat(60));
    console.log('🎉 ALL AWS S3 CONNECTIVITY TESTS PASSED!');
    console.log('='.repeat(60) + '\n');
    process.exit(0);
  } catch (err) {
    console.error('\n' + '='.repeat(60));
    console.error(`❌ AWS S3 TEST FAILED: ${err.name || 'Error'}`);
    console.error(`   Message: ${err.message}`);
    if (err.$metadata) {
      console.error(`   HTTP Status: ${err.$metadata.httpStatusCode}`);
      console.error(`   Request ID:  ${err.$metadata.requestId}`);
    }
    console.error('='.repeat(60));

    if (err.name === 'InvalidAccessKeyId' || err.name === 'SignatureDoesNotMatch') {
      console.log('\n💡 Hint: Check your S3_ACCESS_KEY_ID and S3_SECRET_ACCESS_KEY in .env.');
    } else if (err.name === 'NoSuchBucket') {
      console.log(`\n💡 Hint: The bucket "${bucket}" does not exist in region "${region}".`);
    } else if (err.name === 'AccessDenied') {
      console.log('\n💡 Hint: IAM user lacks permissions. Ensure s3:PutObject, s3:GetObject, s3:DeleteObject are allowed.');
    }
    console.log('');
    process.exit(1);
  }
}

testS3Connection();
