import fs from 'node:fs';
import path from 'node:path';
import pg from 'pg';
import dotenv from 'dotenv';

dotenv.config();

const connectionString = process.env.DATABASE_URL || 'postgres://callintel:callintel@localhost:5432/callintel';
const pool = new pg.Pool({ connectionString });

async function runMigrations() {
  const client = await pool.connect();
  try {
    const defaultDir = fs.existsSync(path.resolve(process.cwd(), 'db/migrations'))
      ? path.resolve(process.cwd(), 'db/migrations')
      : path.resolve(process.cwd(), '../db/migrations');
    const migrationsDir = process.env.MIGRATIONS_DIR || defaultDir;
    
    if (!fs.existsSync(migrationsDir)) {
      console.error(`Migrations directory not found at: ${migrationsDir}`);
      process.exit(1);
    }

    const files = fs.readdirSync(migrationsDir).filter(f => f.endsWith('.sql')).sort();
    
    for (const file of files) {
      console.log(`Executing migration ${file}...`);
      const sql = fs.readFileSync(path.join(migrationsDir, file), 'utf8');
      await client.query(sql);
    }
    console.log('✅ All migrations applied successfully!');
  } catch (err) {
    console.error('❌ Migration failed:', err.message);
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
  }
}

runMigrations();
