/**
 * assign-company.js
 * CLI Utility to link an existing business/store to an existing user from the backend.
 *
 * Usage:
 *   node backend/scripts/assign-company.js --username dhruv --business 2
 *   node backend/scripts/assign-company.js --username dhruv --business "Surat E Commerce Store"
 */

import { pool } from '../src/db.js';

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
    }
  }
  return params;
}

async function run() {
  const params = parseArgs();
  const username = (params.username || params.user || '').trim().toLowerCase();
  const bizArg = params.business || params['business-id'] || params.biz;

  if (!username || !bizArg) {
    console.log(`
Usage:
  node backend/scripts/assign-company.js --username <username> --business <business_id_or_name>

Examples:
  node backend/scripts/assign-company.js --username dhruv --business 2
  node backend/scripts/assign-company.js --username dhruv --business "Test Shop 2"
`);
    process.exit(1);
  }

  const client = await pool.connect();
  try {
    // 1. Find user
    const userRes = await client.query('SELECT id, username, name, role FROM users WHERE username = $1', [username]);
    if (userRes.rows.length === 0) {
      console.error(`❌ User "@${username}" not found.`);
      process.exit(1);
    }
    const user = userRes.rows[0];

    // 2. Find business
    let bizRes;
    if (!isNaN(Number(bizArg))) {
      bizRes = await client.query('SELECT id, name, invoice_prefix, status FROM businesses WHERE id = $1', [Number(bizArg)]);
    } else {
      bizRes = await client.query('SELECT id, name, invoice_prefix, status FROM businesses WHERE LOWER(name) = LOWER($1)', [bizArg]);
    }

    if (bizRes.rows.length === 0) {
      console.error(`❌ Business "${bizArg}" not found.`);
      process.exit(1);
    }
    const business = bizRes.rows[0];

    // 3. Link user to business
    await client.query(
      `INSERT INTO business_users (business_id, user_id, role, is_active, created_at)
       VALUES ($1, $2, 'owner', true, now())
       ON CONFLICT (business_id, user_id)
       DO UPDATE SET is_active = true, role = 'owner'`,
      [business.id, user.id]
    );

    console.log('\n=============================================================');
    console.log('✅ COMPANY SUCCESSFULLY ASSIGNED!');
    console.log(`   User:       @${user.username} (${user.name || 'User'}) [ID: ${user.id}]`);
    console.log(`   Company:    ${business.name} [ID: ${business.id}, Prefix: ${business.invoice_prefix}]`);
    console.log('=============================================================\n');

    process.exit(0);
  } catch (err) {
    console.error('❌ Failed to assign company:', err.message);
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
  }
}

run();
