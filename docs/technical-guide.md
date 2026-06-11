# Tai Lieu Ky Thuat

Cap nhat: 2026-04-21

Tai lieu nay danh cho dev/IT can hieu source Asset Manager.

## 1. Tong quan source

| Hang muc | Gia tri |
|---|---|
| Nen tang | Android native Java |
| Build system | Gradle Android plugin |
| Namespace | `com.idocean.asset` |
| Application ID | `com.idocean.asset` |
| minSdk | 26 |
| targetSdk | 33 |
| compileSdk | 34 |
| Version | `1.4.6` |
| Launcher | `com.idocean.asset.feature.dashboard.DashboardActivity` |

Thu vien chinh:

- AndroidX AppCompat, Activity, ConstraintLayout, RecyclerView.
- Material Components.
- Retrofit 2 + Gson converter.
- OkHttp logging interceptor.
- Chainway `DeviceAPI_ver20251103_release.aar`.
- JUnit cho unit test.

## 2. Man hinh

| Man hinh | Class | Layout |
|---|---|---|
| Dashboard | `feature.dashboard.DashboardActivity` | `activity_dashboard.xml` |
| Inventory | `ui.inventory.InventoryActivity` | `activity_ido_inventory.xml`, `fragment_inventory.xml` |
| Check Out / Check In | `ui.checkout.CheckoutActivity` | `activity_ido_checkout.xml` |
| Lookup / Ban giao | `ui.lookup.LookupActivity` | `activity_ido_lookup.xml`, `dialog_lookup_handover.xml` |
| Assets | `ui.assets.AssetsActivity` | `activity_ido_assets.xml` |
| Logs | `ui.logs.LogsActivity` | `activity_ido_logs.xml` |
| Settings | `ui.settings.SettingsActivity` | `activity_ido_settings.xml` |

Launcher activity duoc khai bao trong `AndroidManifest.xml` la Dashboard.

## 3. Package chinh

| Package | Vai tro |
|---|---|
| `config` | Cau hinh chung nhu `BASE_URL`, `API_KEY`, export folder |
| `data.api` | Retrofit service |
| `data.dto` | DTO request/response API |
| `data.mapper` | Map JSON/CSV/API ve model noi bo |
| `data.repository` | Repository, sync, cache, CSV checkout/checkin, log |
| `export` | Export inventory CSV |
| `feature.dashboard` | Dashboard state/controller/activity |
| `importer` | Import CSV tai san |
| `model` | Model nghiep vu |
| `scanner.barcode` | Barcode scanner service/listener |
| `scanner.rfid` | UHF RFID service/listener/normalizer |
| `storage` | CSV reader/writer, permission, export file |
| `ui.*` | Cac man hinh Android |

## 4. Cau hinh API

File: `app/build.gradle`

```groovy
buildConfigField "String", "BASE_URL", "\"https://n8n.idocean.info:8443/webhook/\""
buildConfigField "String", "API_KEY", "\"${apiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
```

API key duoc doc tu:

- `local.properties`: `IDO_API_KEY`
- Bien moi truong: `IDO_API_KEY`

`AppConfig` expose:

- `BASE_URL`
- `API_KEY`
- `EXPORT_FOLDER_NAME = "IDO Asset"`
- `getExportDirectory()`
- `hasApiKey()`

## 5. Retrofit service

Interface: `AssetApiService`

| Method | Endpoint | Muc dich |
|---|---|---|
| `getAssets()` | `GET get-db` | Dong bo tat ca |
| `getAssets(queryMap)` | `GET get-db` | Dong bo theo filter |
| `getAssets(url)` | `GET <dynamic>` | Dong bo URL dong/fallback |
| `updateAsset(requestDto)` | `POST update-asset` | Cap nhat asset |
| `checkoutAsset(requestDto)` | `POST checkout-asset` | Ghi lich su ban giao/check out |
| `checkinAssets(url, requestDto)` | `POST <dynamic>` | Gui batch checkin/inventory |

## 6. Dong bo tai san

Luong chinh:

1. UI goi repository sync.
2. Repository build request `get-db`.
3. Backend tra JSON.
4. Parser/mapper normalize response.
5. Asset duoc luu vao cache/runtime.
6. Dashboard/Assets doc lai cache de hien thi.

`AssetMapper` chap nhan nhieu alias field de chong sai khac format giua API va CSV.

## 7. Import CSV tai san

Class: `AssetImportManager`

Nguyen tac:

- Doc file qua `ContentResolver`.
- Bo qua dong rong.
- Tu nhan delimiter `,` hoac `;`.
- Normalize header qua `AssetMapper.normalizeHeader`.
- Map tung row thanh `Asset`.
- Bo qua dong khong co code, TID hoac ten tai san.

Chi tiet cot xem [csv-reference.md](csv-reference.md).

## 8. Export file

Export folder:

```text
Documents/IDO Asset
```

Class lien quan:

- `ExportFileManager`: resolve folder/file, sanitize file name, notify media scanner.
- `CsvWriter`: ghi CSV co BOM va separator hint.
- `InventoryCsvExportManager`: export inventory.
- `CheckoutCsvRepository`: export/import checkout va export checkin.

## 9. Checkout/checkin CSV

`CheckoutCsvRepository` quan ly:

- `IDO_CHECKOUT` export.
- `IDO_CHECKIN` export.
- `ticket_id` dang `CO_<timestamp>`.
- `identity_key` uu tien TID, fallback sang code.
- Import checkout de tao danh sach doi chieu checkin.
- Bo qua item trung `identity_key`.

## 10. Checkin-assets backend

Class: `InventoryCheckinService`

Trang thai trong code:

- Service doc lap de gui batch kiem ke len webhook.
- Comment trong source ghi chua noi vao UI o phase hien tai.
- Endpoint dynamic la `checkin-assets`.

Contract backend nen tra object phang:

```json
{
  "success": true,
  "message": "Gui batch kiem ke thanh cong.",
  "session_id": "session-abc",
  "total_received": 12,
  "total_scanned_valid": 10,
  "total_skipped": 2,
  "total_inserted": 10,
  "inserted_rows": []
}
```

Chi tiet response xem [checkin-assets-n8n-response.md](checkin-assets-n8n-response.md).

## 11. Scanner

Package lien quan:

- `scanner.rfid.ChainwayUhfService`
- `scanner.rfid.RfidTagNormalizer`
- `scanner.rfid.ScannerTriggerHandler`
- `scanner.barcode.ChainwayBarcodeService`

Rui ro can luu y:

- Thiet bi khong co module UHF se khong quet RFID dung.
- DeviceAPI/service scanner tren C72 phai san sang.
- Nut trigger vat ly co the phu thuoc cau hinh he thong.

## 12. Permissions

`AndroidManifest.xml` khai bao:

- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `MANAGE_EXTERNAL_STORAGE`
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `CAMERA`

Storage can cho import/export CSV. Camera can cho QR. Internet can cho sync API.

## 13. Build va test

Build APK:

```powershell
.\gradlew.bat assembleDebug
```

Unit test va build:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest assembleDebug
```

Cai len thiet bi:

```powershell
.\gradlew.bat installDebug
```

Kiem tra thiet bi:

```powershell
adb devices
```

## 14. Tai lieu backend lien quan

- [Checkout Asset n8n Mapping](checkout-asset-n8n-mapping.md)
- [Get-DB Location Key n8n Mapping](get-db-location-key-n8n-mapping.md)
- [Checkin Assets n8n Response](checkin-assets-n8n-response.md)

## 15. Ghi chu bao tri

- Khong xoa cache/data app khi chua can, vi co the mat session noi bo.
- Khong sua format CSV checkout neu van can import checkin.
- Khi them field asset moi, cap nhat dong thoi mapper API, mapper CSV, UI va tai lieu CSV.
- Khi backend thay contract response, cap nhat parser va checklist troubleshooting.
