/**
 * storageKeyHelper.js
 * Centralized helper for building human-readable, customer-wise and tenant-wise
 * storage keys for Google Cloud Storage, AWS S3, and Local Storage.
 *
 * Folder Structure:
 *   tenants/${businessSlug}/${docType}/${customerSlug}/${filename}
 * Example:
 *   tenants/Test_Shop_2/invoices/Hardik/Tax-Invoice-TS2-0001.pdf
 *   tenants/Test_Shop_2/quotations/Amit_Patel/Quotation-1.pdf
 *   tenants/Test_Shop_2/credit-notes/Hardik/Credit-Note-CN-0001.pdf
 *   tenants/Test_Shop_2/receipts/Hardik/Payment-Receipt-REC-0001.pdf
 */

/**
 * Sanitizes any raw string into a safe, clean URL/filesystem/bucket path slug.
 * Example: "Test Shop 2" -> "Test_Shop_2", "M/s ABC & Co." -> "M_s_ABC_Co"
 */
export function sanitizeStorageSlug(str, defaultVal = 'General') {
  if (!str || typeof str !== 'string') return defaultVal;
  const cleaned = str
    .trim()
    .replace(/[^a-zA-Z0-9_-]+/g, '_')
    .replace(/^_+|_+$/g, '');
  return cleaned || defaultVal;
}

/**
 * Generates the primary customer-wise document storage key.
 */
export function getDocumentStorageKey({ business, contactName, docType, filename }) {
  const bizId = business?.id || 1;
  const bizSlug = sanitizeStorageSlug(business?.name, `Business_${bizId}`);
  const custSlug = sanitizeStorageSlug(contactName, 'General_Customer');
  return `tenants/${bizSlug}/${docType}/${custSlug}/${filename}`;
}

/**
 * Generates all possible legacy fallback keys to guarantee backward compatibility
 * for documents that were uploaded previously under legacy or flat keys.
 */
export function getDocumentFallbackKeys({ business, contactName, docType, filename }) {
  const bizId = business?.id || 1;
  const bizSlug = sanitizeStorageSlug(business?.name, `Business_${bizId}`);
  const custSlug = sanitizeStorageSlug(contactName, 'General_Customer');
  const primaryKey = `tenants/${bizSlug}/${docType}/${custSlug}/${filename}`;

  const candidates = [
    // 1. Without customer subfolder (tenant-level document folder)
    `tenants/${bizSlug}/${docType}/${filename}`,
    // 2. Numeric tenant ID with customer subfolder
    `tenants/${bizId}/${docType}/${custSlug}/${filename}`,
    // 3. Numeric tenant ID without customer subfolder (previous format)
    `tenants/${bizId}/${docType}/${filename}`,
    // 4. Flat legacy root format (single-tenant format)
    `${docType}/${filename}`,
  ];

  // Exclude primary key and remove any duplicates
  return Array.from(new Set(candidates.filter((k) => k !== primaryKey)));
}

/**
 * Attempts to locate and read an existing cached PDF buffer from storage.
 * Checks primary key first, then checks each fallback key sequentially.
 *
 * @param {object} storageService - Object storage service instance
 * @param {string} primaryKey - Primary customer-wise key
 * @param {string[]} fallbackKeys - List of fallback keys
 * @returns {Promise<{ buffer: Buffer, key: string } | null>}
 */
export async function findCachedDocumentBuffer(storageService, primaryKey, fallbackKeys = []) {
  const allKeys = [primaryKey, ...fallbackKeys];
  for (const key of allKeys) {
    try {
      if (await storageService.exists(key)) {
        const buffer = await storageService.getFileBuffer(key);
        if (buffer && buffer.length > 0) {
          return { buffer, key };
        }
      }
    } catch {
      // Continue searching next fallback key
    }
  }
  return null;
}
