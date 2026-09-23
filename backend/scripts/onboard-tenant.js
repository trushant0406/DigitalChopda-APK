/**
 * onboard-tenant.js
 * CLI Utility for Super Admins to onboard new stores/businesses and provision merchant users.
 *
 * Usage:
 *   node backend/scripts/onboard-tenant.js \
 *     --name "Rajesh Electronics" \
 *     --prefix "RE-" \
 *     --phone "9825123456" \
 *     --gstin "24AAACR1234A1Z5" \
 *     --username "rajesh" \
 *     --password "rajesh123"
 */

import { pool } from '../src/db.js';
import crypto from 'node:crypto';
import { sanitizeStorageSlug } from '../src/storage/storageKeyHelper.js';
import { storageService } from '../src/storage/storageService.js';

const SALT = 'callintel_salt_2026';

function hashPassword(password) {
  return crypto.pbkdf2Sync(password, SALT, 1000, 64, 'sha512').toString('hex');
}

function parseArgs() {
  const args = process.argv.slice(2);
  const params = {};
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg.startsWith('--')) {
      const key = arg.slice(2);
      const val = args[i + 1] && !args[i + 1].startsWith('--') ? args[i + 1].trim() : true;
      params[key] = val;
      if (val !== true) i++;
    } else if (!params.name && !params.business) {
      params.name = arg.trim();
    }
  }
  return params;
}

async function onboard() {
  const params = parseArgs();

  console.log('\n' + '='.repeat(65));
  console.log('🏢 DIGITALCHOPDA.IN — SUPER ADMIN TENANT ONBOARDING CLI');
  console.log('='.repeat(65));

  const name = params.name || params.business;
  if (!name || typeof name !== 'string' || !name.trim()) {
    console.error('\n❌ ERROR: Business name is required.');
    console.log('Usage: node backend/scripts/onboard-tenant.js --name "Store Name" [--prefix "STR-"] [--username "user"] [--password "pass"]\n');
    process.exit(1);
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Generate default prefix from initials if not supplied
    let prefix = (params.prefix || '').trim().toUpperCase();
    if (!prefix) {
      const words = name.trim().split(/\s+/);
      const initials = words.map((w) => w[0]).join('').slice(0, 3).toUpperCase();
      prefix = `${initials}-`;
    }
    if (!prefix.endsWith('-')) prefix += '-';

    // 2. Insert new business
    const insertBiz = await client.query(
      `INSERT INTO businesses (
         name, trade_name, gstin, pan, mobile, email, address, state,
         bank_name, bank_account_no, bank_ifsc, bank_branch,
         terms_and_conditions, invoice_prefix, invoice_next_number, invoice_padding,
         status, created_at, updated_at
       ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, 1, 0, 'active', now(), now())
       RETURNING *`,
      [
        name.trim(),
        params['trade-name'] || params.tradeName || null,
        (params.gstin || '').trim().toUpperCase() || null,
        (params.pan || '').trim().toUpperCase() || null,
        params.phone || params.mobile || null,
        (params.email || '').trim().toLowerCase() || null,
        params.address || null,
        params.state || null,
        params['bank-name'] || params.bankName || null,
        params['bank-account-no'] || params.bankAccountNo || null,
        (params['bank-ifsc'] || params.bankIfsc || '').trim().toUpperCase() || null,
        params['bank-branch'] || params.bankBranch || null,
        params.terms || 'Thanks for your business!',
        prefix,
      ]
    );

    const business = insertBiz.rows[0];
    console.log(`\n✅ Store Created Successfully!`);
    console.log(`   ID:             ${business.id}`);
    console.log(`   Business Name:  ${business.name}`);
    console.log(`   Invoice Prefix: ${business.invoice_prefix}`);
    console.log(`   Status:         ${business.status}`);

    // 3. Link Super Admin (all users with role 'admin') to this business
    const adminUsers = await client.query(`SELECT id, username FROM users WHERE role = 'admin'`);
    for (const admin of adminUsers.rows) {
      await client.query(
        `INSERT INTO business_users (business_id, user_id, role, is_active, created_at)
         VALUES ($1, $2, 'owner', true, now())
         ON CONFLICT (business_id, user_id) DO NOTHING`,
        [business.id, admin.id]
      );
      console.log(`   Super Admin Linked: @${admin.username}`);
    }

    // 4. Optionally create or link a merchant user
    let merchantUser = null;
    const username = (params.username || '').trim().toLowerCase();
    const password = params.password;

    if (username && password) {
      const existingUser = await client.query(`SELECT * FROM users WHERE username = $1`, [username]);
      if (existingUser.rows.length > 0) {
        merchantUser = existingUser.rows[0];
        console.log(`   Existing User Linked: @${username}`);
      } else {
        const hash = hashPassword(password);
        const insUser = await client.query(
          `INSERT INTO users (username, password_hash, name, role, created_at)
           VALUES ($1, $2, $3, 'user', now())
           RETURNING id, username, name, role`,
          [username, hash, params['user-name'] || business.name]
        );
        merchantUser = insUser.rows[0];
        console.log(`   New Merchant User Created: @${username} (Role: user)`);
      }

      await client.query(
        `INSERT INTO business_users (business_id, user_id, role, is_active, created_at)
         VALUES ($1, $2, 'owner', true, now())
         ON CONFLICT (business_id, user_id) DO NOTHING`,
        [business.id, merchantUser.id]
      );
    }

    await client.query('COMMIT');

    // 5. Test/pre-provision storage folder structure
    const bizSlug = sanitizeStorageSlug(business.name, `Business_${business.id}`);
    const placeholderKey = `tenants/${bizSlug}/.initialized`;
    try {
      await storageService.uploadFile({
        key: placeholderKey,
        buffer: Buffer.from(`Tenant initialized at ${new Date().toISOString()}`),
        contentType: 'text/plain',
        isPublic: false,
      });
      console.log(`   Storage Initialized: tenants/${bizSlug}/`);
    } catch (storErr) {
      console.log(`   Storage notice: ${storErr.message}`);
    }

    console.log('\n' + '='.repeat(65));
    console.log('🎉 ONBOARDING COMPLETE!');
    console.log(`   Store Name:   ${business.name}`);
    console.log(`   Store ID:     ${business.id}`);
    if (merchantUser && password) {
      console.log(`   Merchant URL: http://localhost:3000`);
      console.log(`   Username:     ${merchantUser.username}`);
      console.log(`   Password:     ${password}`);
      console.log(`   Scope:        Only accesses "${business.name}"`);
    }
    console.log(`   Super Admin:  Can access this and all stores via Company Switcher`);
    console.log('='.repeat(65) + '\n');

    process.exit(0);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('\n❌ Onboarding failed:', err);
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
  }
}

onboard();
