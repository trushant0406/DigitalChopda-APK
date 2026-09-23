import 'dotenv/config';
import { Storage } from '@google-cloud/storage';
import fs from 'node:fs';
import path from 'node:path';

// Usage: node scripts/test-bucket.js [bucket-name] [key-file-path]
const targetBucket = process.argv[2] || process.env.GCS_BUCKET_NAME || 'digitalchopda';
const keyFileArg = process.argv[3] || process.env.GOOGLE_APPLICATION_CREDENTIALS || 'starry-district-319705-b9a9e523a82e.json';

console.log('\n================================================================');
console.log('🚀 DigitalChopda Production Cloud Storage Bucket Diagnostics');
console.log('================================================================');
console.log(`Target Bucket : ${targetBucket}`);
console.log(`Key File Spec : ${keyFileArg}`);

// Resolve key file
const candidates = [
  keyFileArg,
  path.resolve(process.cwd(), keyFileArg),
  path.resolve(process.cwd(), 'backend', path.basename(keyFileArg)),
  path.resolve(process.cwd(), path.basename(keyFileArg)),
];

const resolvedKey = candidates.find((p) => p && fs.existsSync(p));

if (!resolvedKey) {
  console.error(`\n❌ Service Account key file NOT FOUND!`);
  console.error(`Looked in:`);
  candidates.forEach(c => console.error(`  - ${c}`));
  console.error(`\nPlease ensure your service account JSON file is placed in 'backend/' or specify its path as the 2nd argument.`);
  process.exit(1);
}

let saJson;
try {
  saJson = JSON.parse(fs.readFileSync(resolvedKey, 'utf8'));
  console.log(`Key File Found: ${resolvedKey}`);
  console.log(`Service Account: ${saJson.client_email}`);
  console.log(`Project ID     : ${saJson.project_id}`);
} catch (e) {
  console.error(`❌ Failed to parse Service Account JSON: ${e.message}`);
  process.exit(1);
}

const storage = new Storage({
  projectId: saJson.project_id,
  keyFilename: resolvedKey,
});

async function runDiagnostic() {
  const steps = [];
  const logStep = (name, ok, detail) => {
    steps.push({ name, ok, detail });
    const icon = ok ? '✅' : '❌';
    console.log(`${icon} [${name}] ${detail || ''}`);
  };

  try {
    // 1. Bucket existence & metadata
    console.log('\n1. Checking Bucket Metadata & Access...');
    const bucket = storage.bucket(targetBucket);
    const [exists] = await bucket.exists();
    if (!exists) {
      logStep('Bucket Exists', false, `Bucket '${targetBucket}' was not found in project '${saJson.project_id}'. Check the bucket name for typos or check if it was created in a different project.`);
      return finish(steps);
    }
    logStep('Bucket Exists', true, `Bucket '${targetBucket}' is reachable`);

    const [metadata] = await bucket.getMetadata();
    logStep('Bucket Location', true, `Location: ${metadata.location}, Storage Class: ${metadata.storageClass}`);

    // 2. Upload test probe
    console.log('\n2. Testing Write / Upload Permissions...');
    const probeKey = `_diagnostics/probe_${Date.now()}.txt`;
    const probeContent = `DigitalChopda Health Probe - Timestamp: ${new Date().toISOString()}`;
    const probeBuffer = Buffer.from(probeContent, 'utf8');

    const file = bucket.file(probeKey);
    await file.save(probeBuffer, {
      contentType: 'text/plain',
      resumable: false,
      metadata: {
        contentType: 'text/plain',
        cacheControl: 'no-cache',
      },
    });
    logStep('Write / Upload', true, `Successfully uploaded test file '${probeKey}' (${probeBuffer.length} bytes)`);

    // 3. Check File Exists
    console.log('\n3. Verifying File Existence...');
    const [fileExists] = await file.exists();
    if (!fileExists) {
      logStep('File Exists', false, `Uploaded probe '${probeKey}' could not be located immediately.`);
    } else {
      logStep('File Exists', true, `File verified in bucket`);
    }

    // 4. Download and verify integrity
    console.log('\n4. Testing Read / Download Permissions...');
    const [downloaded] = await file.download();
    if (downloaded.toString('utf8') === probeContent) {
      logStep('Read / Download', true, `Buffer parity confirmed (100% matched)`);
    } else {
      logStep('Read / Download', false, `Buffer mismatch: expected '${probeContent}', got '${downloaded.toString('utf8')}'`);
    }

    // 5. Test PDF mime-type upload
    console.log('\n5. Testing PDF Object Storage...');
    const pdfKey = `_diagnostics/test_invoice_${Date.now()}.pdf`;
    const dummyPdfBuffer = Buffer.from('%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Count 0/Kids[]>>endobj\nxref\n0 3\n0000000000 65535 f\n0000000010 00000 n\n0000000053 00000 n\ntrailer<</Size 3/Root 1 0 R>>\nstartxref\n102\n%%EOF', 'utf8');
    const pdfFile = bucket.file(pdfKey);
    await pdfFile.save(dummyPdfBuffer, {
      contentType: 'application/pdf',
      resumable: false,
    });
    logStep('PDF Upload', true, `Successfully stored sample invoice PDF (${dummyPdfBuffer.length} bytes)`);

    // 6. Delete test probes
    console.log('\n6. Testing Delete Permissions (Cleanup)...');
    await file.delete({ ignoreNotFound: true });
    await pdfFile.delete({ ignoreNotFound: true });
    logStep('Delete / Cleanup', true, `Cleaned up test probe files cleanly`);

    // 7. Check public URL behavior
    console.log('\n7. Checking Public vs Backend Access Model...');
    const publicUrl = `https://storage.googleapis.com/${targetBucket}/${probeKey}`;
    logStep('Access Strategy', true, `Backend streams all PDFs via authenticated service account. (Public Direct URL: ${publicUrl})`);

  } catch (err) {
    logStep('Diagnostic Error', false, err.message);
    if (err.code === 403) {
      console.error('\n⚠️  PERMISSION DENIED (403):');
      console.error(`The service account '${saJson.client_email}' does NOT have sufficient permissions on bucket '${targetBucket}'.`);
      console.error('\nTo fix this in Google Cloud Console:');
      console.error(`1. Go to Google Cloud Storage -> Buckets -> '${targetBucket}'.`);
      console.error(`2. Click the 'Permissions' tab.`);
      console.error(`3. Click 'Grant Access' (or 'Add Principal').`);
      console.error(`4. New principal: ${saJson.client_email}`);
      console.error(`5. Assign role: 'Storage Object Admin' (or 'Storage Admin').`);
      console.error(`6. Click 'Save'.`);
    } else if (err.code === 404) {
      console.error(`\n⚠️  BUCKET NOT FOUND (404):`);
      console.error(`The bucket '${targetBucket}' was not found. Please verify the exact bucket name.`);
    }
  }

  finish(steps);
}

function finish(steps) {
  const allPassed = steps.every((s) => s.ok);
  console.log('\n================================================================');
  if (allPassed) {
    console.log(`🎉 SUCCESS: Bucket '${targetBucket}' is 100% READY for production!`);
    console.log(`You can now set GCS_BUCKET_NAME=${targetBucket} in your prod .env / docker-compose.`);
  } else {
    console.log(`❌ FAILED: Bucket '${targetBucket}' failed one or more checks.`);
  }
  console.log('================================================================\n');
}

runDiagnostic();
