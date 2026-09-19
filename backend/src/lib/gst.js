// Indian GST State Code Registry & Tax Resolver

export const GST_STATE_CODES = {
  '01': 'Jammu and Kashmir',
  '02': 'Himachal Pradesh',
  '03': 'Punjab',
  '04': 'Chandigarh',
  '05': 'Uttarakhand',
  '06': 'Haryana',
  '07': 'Delhi',
  '08': 'Rajasthan',
  '09': 'Uttar Pradesh',
  '10': 'Bihar',
  '11': 'Sikkim',
  '12': 'Arunachal Pradesh',
  '13': 'Nagaland',
  '14': 'Manipur',
  '15': 'Mizoram',
  '16': 'Tripura',
  '17': 'Meghalaya',
  '18': 'Assam',
  '19': 'West Bengal',
  '20': 'Jharkhand',
  '21': 'Odisha',
  '22': 'Chhattisgarh',
  '23': 'Madhya Pradesh',
  '24': 'Gujarat',
  '25': 'Daman and Diu',
  '26': 'Dadra and Nagar Haveli',
  '27': 'Maharashtra',
  '28': 'Andhra Pradesh (Old)',
  '29': 'Karnataka',
  '30': 'Goa',
  '31': 'Lakshadweep',
  '32': 'Kerala',
  '33': 'Tamil Nadu',
  '34': 'Puducherry',
  '35': 'Andaman and Nicobar Islands',
  '36': 'Telangana',
  '37': 'Andhra Pradesh',
  '38': 'Ladakh',
  '97': 'Other Territory',
};

// Aliases mapping common abbreviations and lowercase strings to canonical state code
const STATE_ALIASES = {
  gj: '24', gujarat: '24', guj: '24',
  mh: '27', maharashtra: '27', maha: '27',
  dl: '07', delhi: '07', nct: '07', 'new delhi': '07',
  rj: '08', rajasthan: '08', raj: '08',
  mp: '23', 'madhya pradesh': '23',
  up: '09', 'uttar pradesh': '09',
  ka: '29', karnataka: '29',
  tn: '33', 'tamil nadu': '33', tamilnadu: '33',
  ts: '36', tg: '36', telangana: '36',
  ap: '37', 'andhra pradesh': '37', andhra: '37',
  kl: '32', kerala: '32',
  wb: '19', 'west bengal': '19', bengal: '19',
  pb: '03', punjab: '03',
  hr: '06', haryana: '06',
  br: '10', bihar: '10',
  ch: '04', chandigarh: '04',
  ga: '30', goa: '30',
  jh: '20', jharkhand: '20',
  or: '21', od: '21', odisha: '21', orissa: '21',
  cg: '22', chhattisgarh: '22',
  uk: '05', uttarakhand: '05',
  hp: '02', 'himachal pradesh': '02',
  jk: '01', 'jammu and kashmir': '01', 'jammu & kashmir': '01',
  as: '18', assam: '18',
};

/**
 * Extracts or resolves the 2-digit Indian GST state code from a GSTIN or state name
 */
export function resolveStateCode(gstin, stateName) {
  // 1. If valid GSTIN (at least 2 leading digits)
  if (gstin) {
    const cleaned = String(gstin).trim().toUpperCase();
    const prefix = cleaned.slice(0, 2);
    if (/^\d{2}$/.test(prefix) && GST_STATE_CODES[prefix]) {
      return prefix;
    }
  }

  // 2. If stateName string provided
  if (stateName) {
    const cleanedName = String(stateName).trim().toLowerCase();
    if (STATE_ALIASES[cleanedName]) {
      return STATE_ALIASES[cleanedName];
    }
    // Check if directly passed 2-digit code
    if (/^\d{2}$/.test(cleanedName) && GST_STATE_CODES[cleanedName]) {
      return cleanedName;
    }
    // Search within GST_STATE_CODES values
    for (const [code, name] of Object.entries(GST_STATE_CODES)) {
      if (name.toLowerCase() === cleanedName) {
        return code;
      }
    }
  }

  return null;
}

/**
 * Determines tax mode ('split' for CGST+SGST or 'igst' for IGST)
 * based on business profile and customer GSTIN/state.
 */
export function determineTaxMode({ businessGstin, businessState, customerGstin, customerState }) {
  const sellerCode = resolveStateCode(businessGstin, businessState);
  const buyerCode = resolveStateCode(customerGstin, customerState);

  // If both state codes are resolved:
  if (sellerCode && buyerCode) {
    return sellerCode === buyerCode ? 'split' : 'igst';
  }

  // Fallback: If only buyer state is available and matches seller state name
  if (businessState && customerState) {
    const s = String(businessState).trim().toLowerCase();
    const c = String(customerState).trim().toLowerCase();
    if (s === c) return 'split';
  }

  // Default within domestic trade is intra-state (split)
  return 'split';
}
