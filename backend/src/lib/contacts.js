export async function findOrCreateContact(client, data = {}, explicitBusinessId = null) {
  const {
    phone, name, city, gstin, pan, billing_address, shipping_address, state, business_id,
  } = data;
  const bizId = explicitBusinessId || business_id || data.businessId || 1;
  const existing = await client.query('SELECT * FROM contacts WHERE phone = $1 AND business_id = $2', [phone, bizId]);
  if (existing.rows.length > 0) {
    const contact = existing.rows[0];
    const updated = await client.query(
      `UPDATE contacts SET
         name = COALESCE($1, name),
         city = COALESCE($2, city),
         gstin = COALESCE($3, gstin),
         pan = COALESCE($4, pan),
         billing_address = COALESCE($5, billing_address),
         shipping_address = COALESCE($6, shipping_address),
         state = COALESCE($7, state)
       WHERE id = $8 AND business_id = $9 RETURNING *`,
      [name || null, city || null, gstin || null, pan || null, billing_address || null, shipping_address || null, state || null, contact.id, bizId]
    );
    return updated.rows[0];
  }
  const created = await client.query(
    `INSERT INTO contacts (phone, name, city, gstin, pan, billing_address, shipping_address, state, business_id)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING *`,
    [phone, name || null, city || null, gstin || null, pan || null, billing_address || null, shipping_address || null, state || null, bizId]
  );
  return created.rows[0];
}
