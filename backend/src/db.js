import pg from 'pg';
import dotenv from 'dotenv';

dotenv.config();

export const pool = new pg.Pool({
  connectionString: process.env.DATABASE_URL || 'postgres://callintel:callintel@localhost:5432/callintel',
});
