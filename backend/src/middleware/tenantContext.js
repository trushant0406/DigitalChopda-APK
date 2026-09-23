import { pool } from '../db.js';
import { verifyToken } from '../routes/auth.js';

/**
 * Middleware to establish multi-tenant context (req.businessId & req.business)
 * 1. Checks 'X-Business-Id' header
 * 2. Or checks query parameter 'business_id'
 * 3. Falls back to default business ID 1
 */
export async function tenantContext(req, res, next) {
  try {
    // 1. Determine user if authorization header present
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const decoded = verifyToken(authHeader.substring(7));
      if (decoded) {
        req.user = decoded;
      }
    }

    // 2. Extract requested business ID
    let requestedId = null;
    const headerVal = req.headers['x-business-id'];
    if (headerVal && !isNaN(Number(headerVal))) {
      requestedId = parseInt(headerVal, 10);
    } else if (req.query.business_id && !isNaN(Number(req.query.business_id))) {
      requestedId = parseInt(req.query.business_id, 10);
    }

    // 3. Fallback: if user has assigned businesses, pick requested or first assigned
    if (req.user && req.user.id) {
      if (req.user.role === 'admin') {
        const allBizRes = await pool.query(`SELECT *, 'owner' as user_role FROM businesses WHERE status = 'active' ORDER BY id ASC`);
        req.userBusinesses = allBizRes.rows;
        if (requestedId) {
          const match = allBizRes.rows.find((b) => b.id === requestedId);
          if (match) {
            req.businessId = match.id;
            req.business = match;
            return next();
          }
        }
        if (allBizRes.rows.length > 0) {
          req.businessId = allBizRes.rows[0].id;
          req.business = allBizRes.rows[0];
          return next();
        }
      }

      const userBizRes = await pool.query(
        `SELECT b.*, bu.role as user_role
         FROM businesses b
         JOIN business_users bu ON bu.business_id = b.id
         WHERE bu.user_id = $1 AND bu.is_active = true
         ORDER BY b.id ASC`,
        [req.user.id]
      );
      req.userBusinesses = userBizRes.rows;

      if (requestedId) {
        const match = userBizRes.rows.find((b) => b.id === requestedId);
        if (match) {
          req.businessId = match.id;
          req.business = match;
          return next();
        }
      }

      if (userBizRes.rows.length > 0) {
        // If requestedId wasn't found or was invalid, use first accessible business
        req.businessId = userBizRes.rows[0].id;
        req.business = userBizRes.rows[0];
        return next();
      }
    }

    // 4. Default to requestedId or 1
    const targetId = requestedId || 1;
    const bizRes = await pool.query('SELECT * FROM businesses WHERE id = $1', [targetId]);
    if (bizRes.rows.length > 0) {
      req.businessId = bizRes.rows[0].id;
      req.business = bizRes.rows[0];
    } else {
      // If targetId does not exist, fallback to first existing business
      const fallbackRes = await pool.query('SELECT * FROM businesses ORDER BY id ASC LIMIT 1');
      if (fallbackRes.rows.length > 0) {
        req.businessId = fallbackRes.rows[0].id;
        req.business = fallbackRes.rows[0];
      } else {
        req.businessId = 1;
        req.business = null;
      }
    }

    next();
  } catch (err) {
    console.error('[TenantContext] Failed to resolve tenant:', err.message);
    req.businessId = 1;
    next();
  }
}
