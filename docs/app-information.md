# Thong Tin App

> Da doi chieu voi source va da smoke test tren thiet bi `HC72DE251100181`. Launcher mo dung `DashboardActivity`.

## 1. Ten app
- Ten hien thi tren may: `Asset Manager`
- Ten noi bo / ten file APK: `UHF-serial_v1.4.6`
- Package hien tai: `com.idocean.asset`

## 2. App dung de lam gi
Day la app Android native Java de:
- dong bo danh sach tai san tu backend n8n
- tim kiem, chinh sua, ban giao tai san
- kiem ke tai san bang QR/RFID
- tao va xuat CSV cho checkout/checkin, inventory va logs
- quan ly session lam viec, phong ban, vi tri va bo loc dong bo

## 3. Doi tuong su dung
- nhan vien quan ly tai san
- nhan vien kho / kiem ke
- nhan vien IT hoac van hanh su dung may tay cam Chainway
- quan ly can tra cuu, ban giao, va doi chieu tai san

## 4. Nen tang
- Android native bang Java
- minSdk: 26
- targetSdk: 33
- compileSdk: 34
- ho tro thiet bi handheld co RFID UHF va barcode scanner
- co tap trung vao may Chainway C72 va may cong nhan tuong duong

## 5. Danh sach man hinh
| Man hinh | Class | Mo ta ngan |
|---|---|---|
| Dashboard | `DashboardActivity` | Man hinh launcher, dong bo du lieu, loc dong bo, mo nhanh cac tac vu |
| Tra cuu | `LookupActivity` | Tra asset theo code/TID, sua, ban giao |
| Kiem ke | `InventoryActivity` | Quet QR/RFID, doi chieu asset, xuat ket qua kiem ke |
| Check Out | `CheckoutActivity` | Tao phieu xuat / nhan tra, quet item, xuat CSV |
| Danh sach tai san | `AssetsActivity` | Xem cache/runtime list, loc, import CSV |
| Nhat ky thao tac | `LogsActivity` | Xem log nghiep vu va xuat log |
| Thiet lap phien | `SettingsActivity` | Chon nguoi thao tac, phong ban, note phien |

## 6. Moi man hinh co chuc nang gi
### Dashboard
- hien thi tong quan trang thai du lieu
- dong bo `Tat ca`, `Theo bo loc`, `Theo phien`
- show filter phan bo theo phong ban / vi tri / loai tai san
- mo nhanh Inventory, Check Out, Assets, Logs, Lookup, Settings

### Tra cuu
- quet QR hoac RFID de tim asset
- tim theo code / TID
- xem thong tin asset
- edit asset
- ban giao asset qua luong 2 buoc

### Kiem ke
- quet QR / RFID
- match asset theo TID / EPC / code
- danh dau da quet / trung / ngoai danh sach
- hien thi tong, da quet, thieu, ngoai danh sach
- xuat ket qua kiem ke CSV

### Check Out
- tao danh sach checkout va checkin
- quet QR / RFID
- xu ly trung lap
- xuat CSV checkout / checkin

### Danh sach tai san
- load du lieu tu cache / API / CSV
- loc local
- import CSV
- xem danh sach asset dang co trong cache

### Nhat ky thao tac
- xem cac log nghiep vu gan day
- xuat log CSV

### Thiet lap phien
- chon nguoi thao tac
- chon phong ban
- luu note phien lam viec
- dung cho dashboard va log trong phien hien tai

## 7. Quy trinh su dung tu dau den cuoi
1. Mo app, vao Dashboard.
2. Chon dong bo `Tat ca`, `Theo bo loc`, hoac `Theo phien`.
3. Neu can, vao Settings de set thong tin phien lam viec.
4. Vao Lookup de tim asset theo code hoac TID, sau do edit hoac ban giao.
5. Vao Inventory de quet tai san va doi chieu ket qua.
6. Vao Checkout de quet danh sach checkout/checkin va xuat file.
7. Vao Assets de xem cache runtime, loc va import CSV.
8. Vao Logs de kiem tra nhat ky thao tac.

### Backend / dong bo
- Base URL: `https://n8n.idocean.info:8443/webhook/`
- Dong bo du lieu: `get-db`
- Cap nhat asset: `update-asset`
- Luu lich su ban giao / checkout: `checkout-asset`

## 8. Phan quyen
### Runtime / he thong
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `CAMERA`
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `MANAGE_EXTERNAL_STORAGE`

### Ghi chu
- App khong co login/auth trong code hien tai.
- Quyen storage can thiet cho import/export CSV va luu file ra `Documents/IDO Asset`.
- Scanner QR/RFID phu thuoc SDK thiet bi Chainway / DeviceAPI.

## 9. Cac loi thuong gap / luu y
- Neu backend khong tra du lieu dung format, dong bo co the fallback sang cach load rong hon.
- Dong bo theo bo loc co the co tat khi backend khong ho tro loc server-side dung nhu mong doi.
- Dong bo theo phien can co phong ban trong session; neu chua co, app se can bao loi.
- Export CSV can co quyen storage, dac biet tren Android 11+.
- Neu thiet bi khong co UHF / scanner service, QR/RFID se khong chay dung.
- Doi voi ban giao, luong dang dung la 2 API noi tiep:
  1. `checkout-asset`
  2. `update-asset`
- Cache runtime nam trong internal storage, nen dong bo / import xong phai re-open app hoac refresh man de thay thay doi moi.

## 10. Anh chup man hinh hoac mo ta giao dien
### Dashboard
- Hero header mau sang
- Khu `Bo loc` gom 3 o chon: phong ban, vi tri, loai tai san
- Nut `Xoa bo loc`
- Cac nut dong bo `Tat ca`, `Theo bo loc`, `Theo phien`
- Card thong tin tong asset va lan dong bo cuoi

### Tra cuu
- Thanh scanner QR/RFID o phia tren
- Khu tim kiem theo code/TID
- Form thong tin asset va nut `Edit`, `Ban giao`

### Kiem ke
- Khu quet scan
- Danh sach ket qua theo tung asset da quet
- Counter tong / matched / missing / outside

### Check Out
- Tab / khu checkout va checkin
- Danh sach item da quet
- Nut xuat CSV

### Danh sach tai san
- List asset dang co trong cache
- Filter local o dau man

### Nhat ky thao tac
- Danh sach log theo dong
- Nut xuat log

### Thiet lap phien
- O chon nguoi thao tac
- O chon phong ban
- O ghi chu phien

## Ghi chu xac nhan ky thuat
- Launcher hien tai mo dung `com.idocean.asset/.feature.dashboard.DashboardActivity`
- Da smoke test thanh cong tren `C72 - 13`
