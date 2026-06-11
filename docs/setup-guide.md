# Cai Dat Va Van Hanh

Cap nhat: 2026-04-21

Tai lieu nay danh cho IT/admin cai dat app Asset Manager tren thiet bi C72/UHF va ho tro van hanh.

## 1. Yeu cau thiet bi

- Android minSdk 26 tro len.
- Uu tien Chainway C72.
- Co module UHF RFID va barcode scanner.
- Co camera neu dung QR.
- Co ket noi mang noi bo hoac internet de goi backend n8n.
- Co quyen doc/ghi storage de import/export CSV.

## 2. Thong tin ung dung

| Hang muc | Gia tri |
|---|---|
| Ten app | Asset Manager |
| Package | `com.idocean.asset` |
| Version | `1.4.6` |
| Launcher Activity | `com.idocean.asset.feature.dashboard.DashboardActivity` |
| APK output | `UHF-serial_v1.4.6.apk` |
| Export folder | `Documents/IDO Asset` |

## 3. Cai APK len thiet bi

Neu da co file APK:

1. Copy APK vao thiet bi.
2. Mo file APK tren thiet bi.
3. Cho phep cai dat tu nguon tin cay neu Android yeu cau.
4. Cai app.
5. Mo app `Asset Manager`.
6. Cap quyen khi app hoi.

Neu cai bang ADB:

```powershell
adb devices
adb install -r UHF-serial_v1.4.6.apk
```

Neu dung Gradle tu source:

```powershell
.\gradlew.bat installDebug
```

## 4. Cap quyen runtime

App khai bao cac quyen:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `CAMERA`
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `MANAGE_EXTERNAL_STORAGE`

Sau khi cai dat, nen kiem tra trong Settings cua Android:

- Camera: allow.
- Files and media: allow.
- Manage all files neu thiet bi yeu cau cho Android 11+.

Neu khong cap quyen storage, export CSV co the that bai hoac khong thay file.

## 5. Cau hinh API

Base URL hien tai duoc cau hinh trong `app/build.gradle`:

```text
https://n8n.idocean.info:8443/webhook/
```

App dung cac endpoint chinh:

| Endpoint | Phuong thuc | Muc dich |
|---|---|---|
| `get-db` | GET | Dong bo danh sach tai san |
| `update-asset` | POST | Cap nhat thong tin tai san |
| `checkout-asset` | POST | Ghi lich su ban giao/check out |
| `checkin-assets` | POST | Gui batch ket qua kiem ke/checkin len backend |

API key duoc lay tu:

- `local.properties` voi key `IDO_API_KEY`.
- Hoac bien moi truong `IDO_API_KEY`.

Vi du `local.properties`:

```properties
IDO_API_KEY=your-api-key
```

Khong commit API key that vao repo neu repo duoc chia se ra ngoai.

## 6. Build APK tu source

Lenh build:

```powershell
.\gradlew.bat assembleDebug
```

Lenh test va build debug:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest assembleDebug
```

APK output duoc dat ten theo `versionName`:

```text
UHF-serial_v1.4.6.apk
```

## 7. Kiem tra sau khi cai

Checklist smoke test:

1. Mo app thanh cong vao Dashboard.
2. Dashboard hien ten/brand AssetTrack.
3. Bam dong bo `Tat ca` hoac `Theo bo loc`.
4. Kiem tra tong so tai san sau dong bo.
5. Vao `Settings`, luu nguoi thao tac va phong ban.
6. Vao `Lookup`, tim thu mot code/TID.
7. Vao `Inventory`, quet thu RFID hoac QR.
8. Vao `Check Out`, quet thu mot item va export CSV.
9. Kiem tra file trong `Documents/IDO Asset`.
10. Vao `Logs`, dam bao log duoc ghi.

## 8. Cau hinh thiet bi C72

Nen kiem tra:

- Service scanner cua Chainway dang chay.
- Module UHF co the init thanh cong.
- Nut trigger vat ly khong bi app khac chiem.
- Vung tan so RFID phu hop voi thiet bi va moi truong.
- Pin du de quet lien tuc.

Neu RFID khong doc:

- Dong app mo lai.
- Khoi dong lai thiet bi.
- Kiem tra app scanner goc neu thiet bi co.
- Thu QR/camera de tach loi scanner UHF voi loi app.
- Xem log Android neu co may dev.

## 9. Thu muc va file du lieu

File export duoc tao trong:

```text
Documents/IDO Asset
```

Ten file co the gom:

- `IDO_INVENTORY_...csv`
- `IDO_CHECKOUT_...csv`
- `checkin_...csv`
- File log CSV neu nguoi dung export log.

App cung co cache du lieu noi bo. Neu vua import/dong bo xong nhung man hinh chua cap nhat, hay refresh man hinh hoac mo lai app.

## 10. Cap nhat phien ban

Khi cap nhat APK:

1. Backup file CSV quan trong trong `Documents/IDO Asset`.
2. Ghi lai version cu.
3. Cai APK moi bang `adb install -r` hoac cai thu cong.
4. Mo app va chay smoke test.
5. Dong bo lai du lieu neu can.

Khong nen xoa data app tru khi can lam sach cache, vi viec xoa data co the lam mat session/cau hinh noi bo.
