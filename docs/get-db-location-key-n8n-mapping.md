# Get-DB Location Key n8n Mapping

Muc tieu: backend `get-db` nhan `location` theo key chuan nhu `TT16_F5`, tu map ra toan bo alias va loc dung du lieu cho sync. Dong thoi backend phai normalize `assetType` o nguon de khong bi tach `Chuot` va `CHUOT` thanh 2 nhom.

Trang thai hien tai da xac nhan tren app:
- App da gui request `location=TT16_F5`
- Backend hien tai tra `HTTP 200` nhung body rong cho `TT16_F5`
- Backend van tra du lieu dung cho `location=Lầu 5 - TT16`

Vi vay backend can them 2 lop chuan hoa:
- `location key -> alias`
- `assetType raw -> assetType canonical`

## Rule backend can dat

1. Neu request `location` la key chuan thi backend phai map sang nhom alias tuong ung.
2. Neu request `location` la text cu nhu `Idoplex - 5` hoac `Lầu 5 - TT16` thi backend van phai resolve ve cung mot key.
3. Filter location nen so theo `location key`, khong so raw text.
4. Filter `assetType` nen so theo key da normalize, khong so raw text.
5. Trong response, co the giu `location` goc cua ban ghi, nhung `asset_type` nen tra ve dang canonical thong nhat.

## Danh sach key de xuat

| Key | Display canonical | Alias toi thieu |
|---|---|---|
| `TT16` | `TT16` | `TT16`, `Idoplex` |
| `TT16_B` | `Tầng B - TT16` | `Tầng B - TT16`, `Tang B - TT16`, `Idoplex - B`, `Idoplex-B` |
| `TT16_G` | `Tầng G - TT16` | `Tầng G - TT16`, `Tang G - TT16`, `Idoplex - G`, `Idoplex-G` |
| `TT16_F1` | `Lầu 1 - TT16` | `Lầu 1 - TT16`, `Lau 1 - TT16`, `Idoplex - 1`, `Idoplex-1` |
| `TT16_F2` | `Lầu 2 - TT16` | `Lầu 2 - TT16`, `Lau 2 - TT16`, `Idoplex - 2`, `Idoplex-2` |
| `TT16_F3` | `Lầu 3 - TT16` | `Lầu 3 - TT16`, `Lau 3 - TT16`, `Idoplex - 3`, `Idoplex-3` |
| `TT16_F4` | `Lầu 4 - TT16` | `Lầu 4 - TT16`, `Lau 4 - TT16`, `Idoplex - 4`, `Idoplex-4` |
| `TT16_F5` | `Lầu 5 - TT16` | `Lầu 5 - TT16`, `Lau 5 - TT16`, `Lầu 5 - TT17`, `Lau 5 - TT17`, `Idoplex - 5`, `Idoplex-5` |
| `TT16_F6` | `Lầu 6 - TT16` | `Lầu 6 - TT16`, `Lau 6 - TT16`, `Idoplex - 6`, `Idoplex-6` |
| `LA_FACTORY` | `LA.Factory` | `LA.Factory`, `LA Factory` |
| `WAREHOUSE` | `Warehouse` | `Warehouse` |

## Luong n8n de xuat

1. Webhook `get-db` nhan query param:
   - `department`
   - `location`
   - `assetType`
2. Node Code dau tien:
   - normalize `location`
   - resolve `locationKey`
   - tao `locationAliases`
   - normalize `assetType`
   - tao `assetTypeKey`
3. Node DB / query:
   - neu co `locationKey` thi loc theo cung key do
   - neu co `assetType` thi loc theo gia tri da normalize
4. Node response:
   - van tra `success`, `message`, `filters`, `count`, `items`
   - `items[*].asset_type` nen duoc canonicalize ve mot dang duy nhat

## Code mau cho n8n Code node

Co the copy gan nhu nguyen khoi vao Code node truoc buoc query/filter:

```js
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

const input = $json;
const location = input.location || "";
const requestAssetType = String(input.assetType || "").trim();
const requestAssetTypeKey = normalizeAssetTypeKey(requestAssetType);
const requestAssetTypeDisplay = canonicalizeAssetType(requestAssetType);
const locationKey = resolveLocationKey(location);
const locationAliases = resolveLocationAliases(location);

return [{
  json: {
    ...input,
    locationKey,
    locationAliases,
    assetType: requestAssetTypeDisplay || requestAssetType,
    assetTypeKey: requestAssetTypeKey,
  }
}];
```

## Neu filter trong Code node sau khi da doc du lieu tu DB

Neu data da doc ra thanh `rows`, co the loc nhu sau:

```js
function matchesLocation(rowLocation, requestLocationKey) {
  if (!requestLocationKey) return true;
  return resolveLocationKey(rowLocation) === requestLocationKey;
}

const requestLocationKey = $json.locationKey || "";
const requestDepartment = ($json.department || "").trim();
const requestAssetType = ($json.assetType || "").trim();
const requestAssetTypeKey = $json.assetTypeKey || normalizeAssetTypeKey(requestAssetType);

const rows = $input.first().json.rows || [];

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
      location: $json.location || "",
      locationKey: requestLocationKey,
      assetType: canonicalizeAssetType(requestAssetType) || requestAssetType,
      assetTypeKey: requestAssetTypeKey,
    },
    count: filtered.length,
    items: filtered,
  }
}];
```

## Neu filter ngay trong SQL

Tot nhat la luu them cot `location_key`, hoac tao virtual expression tuong duong.

Huong de xuat:
- cot request: `:locationKey`
- cot du lieu: `location_key`

Filter:

```sql
SELECT
  ...,
  UPPER(TRIM(asset_type)) AS asset_type
FROM asset_list1
WHERE (:department = '' OR department = :department)
  AND (:assetType = '' OR UPPER(TRIM(asset_type)) = UPPER(TRIM(:assetType)))
  AND (:locationKey = '' OR location_key = :locationKey)
```

Neu chua co `location_key`, co the tam thoi filter theo `IN (...)` tren alias list, nhung day chi la giai phap trung gian.

## Ket qua mong muon de verify

Ca 4 request sau phai tra cung nhom du lieu:

- `get-db?location=TT16_F5`
- `get-db?location=Lầu 5 - TT16`
- `get-db?location=Lau 5 - TT16`
- `get-db?location=Idoplex - 5`

Va voi case co phong ban:

- `get-db?department=IT&location=TT16_F5`
- `get-db?department=IT&location=Lầu 5 - TT16`

hai request nay phai ra cung tap asset.

Va voi `assetType`, cac request sau phai ra cung count va response chi con 1 dang canonical:

- `get-db?assetType=Chuot`
- `get-db?assetType=CHUOT`
- `get-db?assetType=CHUỘT`

Vi du nhom `CHUỘT` trong response khong duoc tra lan `CHUỘT` va `Chuột`.

## Ghi chu

- App da san sang gui `location key`
- Hien tai app van co lop compatibility fallback tam thoi de retry bang display text
- Khi backend map key on dinh, co the bo fallback tam nay o app trong phase cleanup sau
