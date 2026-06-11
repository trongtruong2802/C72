const LOCATION_ALIAS_MAP = {
  TT16: ["TT16", "Idoplex"],
  TT16_B: ["Tầng B - TT16", "Tang B - TT16", "Idoplex - B", "Idoplex-B"],
  TT16_G: ["Tầng G - TT16", "Tang G - TT16", "Idoplex - G", "Idoplex-G"],
  TT16_F1: ["Lầu 1 - TT16", "Lau 1 - TT16", "Idoplex - 1", "Idoplex-1"],
  TT16_F2: ["Lầu 2 - TT16", "Lau 2 - TT16", "Idoplex - 2", "Idoplex-2"],
  TT16_F3: ["Lầu 3 - TT16", "Lau 3 - TT16", "Idoplex - 3", "Idoplex-3"],
  TT16_F4: ["Lầu 4 - TT16", "Lau 4 - TT16", "Idoplex - 4", "Idoplex-4"],
  TT16_F5: ["Lầu 5 - TT16", "Lau 5 - TT16", "Lầu 5 - TT17", "Lau 5 - TT17", "Idoplex - 5", "Idoplex-5"],
  TT16_F6: ["Lầu 6 - TT16", "Lau 6 - TT16", "Idoplex - 6", "Idoplex-6"],
  LA_FACTORY: ["LA.Factory", "LA Factory"],
  WAREHOUSE: ["Warehouse"],
};

function normalizeText(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_|_$/g, "");
}

function normalizeAssetTypeKey(value) {
  return normalizeText(value);
}

function canonicalizeAssetType(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .trim()
    .replace(/\s+/g, " ")
    .toUpperCase();
}

function buildAliasIndex() {
  const index = {};
  for (const [key, aliases] of Object.entries(LOCATION_ALIAS_MAP)) {
    index[normalizeText(key)] = key;
    for (const alias of aliases) {
      index[normalizeText(alias)] = key;
    }
  }
  return index;
}

const LOCATION_ALIAS_INDEX = buildAliasIndex();

function resolveLocationKey(value) {
  const normalized = normalizeText(value);
  if (!normalized) return "";
  return LOCATION_ALIAS_INDEX[normalized] || normalized;
}

function resolveLocationAliases(value) {
  const key = resolveLocationKey(value);
  if (!key) return [];
  return LOCATION_ALIAS_MAP[key] || [value].filter(Boolean);
}

function matchesLocation(rowLocation, requestLocationKey) {
  if (!requestLocationKey) return true;
  return resolveLocationKey(rowLocation) === requestLocationKey;
}

const input = $json;
const requestDepartment = String(input.department || "").trim();
const requestAssetType = String(input.assetType || "").trim();
const requestAssetTypeKey = normalizeAssetTypeKey(requestAssetType);
const requestAssetTypeDisplay = canonicalizeAssetType(requestAssetType);
const requestLocation = String(input.location || "").trim();
const requestLocationKey = resolveLocationKey(requestLocation);
const requestLocationAliases = resolveLocationAliases(requestLocation);

const rows = input.rows || [];

const filtered = rows
  .filter((row) => {
    const rowDepartment = String(row.department || "").trim();
    const rowAssetType = String(row.asset_type || row.assetType || "").trim();
    const rowAssetTypeKey = normalizeAssetTypeKey(rowAssetType);
    const rowLocation = row.location || row.location_name || row.vi_tri || "";

    if (requestDepartment && rowDepartment !== requestDepartment) return false;
    if (requestAssetTypeKey && rowAssetTypeKey !== requestAssetTypeKey) return false;
    if (!matchesLocation(rowLocation, requestLocationKey)) return false;
    return true;
  })
  .map((row) => {
    const canonicalAssetType = canonicalizeAssetType(row.asset_type || row.assetType || "");
    if (!canonicalAssetType) {
      return row;
    }
    return {
      ...row,
      asset_type: canonicalAssetType,
      assetType: canonicalAssetType,
    };
  });

return [{
  json: {
    success: true,
    message: "Lấy dữ liệu tài sản thành công",
    filters: {
      department: requestDepartment,
      location: requestLocation,
      locationKey: requestLocationKey,
      locationAliases: requestLocationAliases,
      assetType: requestAssetTypeDisplay || requestAssetType,
      assetTypeKey: requestAssetTypeKey,
    },
    count: filtered.length,
    items: filtered,
  }
}];
