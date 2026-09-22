import pg from 'pg';
import dotenv from 'dotenv';

dotenv.config();

const connectionString = process.env.DATABASE_URL || 'postgres://callintel:callintel@localhost:5432/callintel';

export const pool = new pg.Pool({
  connectionString,
  // Scale connections for enterprise workloads
  max: parseInt(process.env.DB_POOL_MAX || '20', 10),
  min: parseInt(process.env.DB_POOL_MIN || '2', 10),
  // Close idle clients after 30 seconds to conserve DB RAM
  idleTimeoutMillis: parseInt(process.env.DB_IDLE_TIMEOUT_MS || '30000', 10),
  // Fail fast if database is unreachable (5 seconds instead of infinite hang)
  connectionTimeoutMillis: parseInt(process.env.DB_CONNECT_TIMEOUT_MS || '5000', 10),
  // Cancel queries that run longer than 30s to prevent runaway transactions / lockups
  statement_timeout: parseInt(process.env.DB_STATEMENT_TIMEOUT_MS || '30000', 10),
});

// Prevent unhandled idle connection errors from crashing the Node process
pool.on('error', (err) => {
  console.error('[DB Pool] Unexpected idle client error (auto-recovering):', err.message);
});

