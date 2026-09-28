-- Migration 024: Cleanup merchant business links
-- Disassociate non-admin merchant users from Business 1 if they have a dedicated store assigned.
-- Also ensure that merchants only belong to their dedicated store(s).

DELETE FROM business_users
WHERE business_id = 1
  AND user_id IN (
    SELECT bu.user_id
    FROM business_users bu
    JOIN users u ON u.id = bu.user_id
    WHERE bu.business_id != 1 AND u.role != 'admin'
  );
