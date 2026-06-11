# Asset Manager C72

Asset Manager la ung dung Android native dung cho quan ly tai san tren thiet bi cam tay Chainway C72/UHF. App ho tro dong bo du lieu tai san tu backend n8n, tra cuu bang code/TID, kiem ke bang RFID/QR, check out/check in, import/export CSV va xem nhat ky thao tac.

## Thong tin nhanh

| Hang muc | Gia tri |
|---|---|
| Ten hien thi | Asset Manager |
| Package | `com.idocean.asset` |
| APK | `UHF-serial_v1.4.6.apk` |
| Nen tang | Android native Java |
| Thiet bi uu tien | Chainway C72 hoac thiet bi co UHF/barcode scanner tuong duong |
| Thu muc export | `Documents/IDO Asset` |

## Tai lieu

| Tai lieu | Dung cho | Noi dung chinh |
|---|---|---|
| [Huong dan su dung](docs/user-guide.md) | Nhan vien van hanh, kho, IT hien truong | Cac buoc su dung Dashboard, Settings, Lookup, Inventory, Check Out, Assets va Logs |
| [Build tu source](docs/build-from-source.md) | Dev/IT nhan repo qua Git/zip | Yeu cau moi truong, tao `local.properties`, build APK, test va install debug |
| [Quy trinh nghiep vu](docs/workflows.md) | Nhan vien kiem ke va ban giao tai san | Checklist cho dong bo, kiem ke, check out, check in, ban giao va import CSV |
| [Cai dat va van hanh](docs/setup-guide.md) | IT/admin | Cai APK, cap quyen, cau hinh API, build/install va smoke test |
| [Xu ly loi thuong gap](docs/troubleshooting.md) | Van hanh va IT ho tro | Trieu chung, nguyen nhan kha di va cach xu ly |
| [Tham chieu CSV](docs/csv-reference.md) | Van hanh, IT, nguoi chuan bi du lieu | Cot import tai san, file export inventory, checkout va checkin |
| [Tai lieu ky thuat](docs/technical-guide.md) | Dev/IT | Kien truc source, man hinh, API, scanner, cache, build va test |
| [Thong tin app hien co](docs/app-information.md) | Dev/IT | Ghi chu da doi chieu voi source va smoke test |

## Anh minh hoa

Mot so anh chup man hinh hien co trong repo:

- `codex_dashboard.png`
- `codex_dashboard_filter.png`
- `codex_dashboard_live.png`
- `codex_c72_screen.png`

Co the chen cac anh nay vao tai lieu khi can xuat ban PDF hoac ban giao cho nguoi dung.

## Build nhanh

```powershell
.\gradlew.bat assembleDebug
```

Chi tiet moi truong va cac buoc clone/build xem them tai [docs/build-from-source.md](docs/build-from-source.md).

APK build ra theo cau hinh hien tai co ten dang:

```text
UHF-serial_v1.4.6.apk
```

## Luu y van hanh

- App can quyen `INTERNET`, `CAMERA` va quyen doc/ghi storage de dong bo, quet QR/RFID va xuat file CSV.
- App khong co man hinh login trong source hien tai.
- Scanner RFID/barcode phu thuoc SDK/DeviceAPI va service tren thiet bi Chainway.
- Du lieu export nam trong `Documents/IDO Asset`.
- Backend mac dinh duoc cau hinh trong `app/build.gradle`, voi API key lay tu `local.properties` hoac bien moi truong `IDO_API_KEY`.
