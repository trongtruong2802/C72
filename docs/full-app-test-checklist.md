# Full App Test Checklist

Tai lieu nay dung de test tay ban app hoan chinh tren thiet bi that, uu tien C72 va cac workflow backend dang su dung thuc te.

## Cach dung

- Dien `PASS`, `FAIL`, hoac `N/A` vao cot `Ket qua`.
- Neu `FAIL`, ghi them chi tiet o cot `Ghi chu`.
- Moi lan ra APK moi, toi thieu chay het nhom `Smoke`, `Sync`, `Inventory`, `Check-in upload`, `Lookup`, `Checkout`.

## Thong Tin Vong Test

| Muc | Gia tri |
|---|---|
| Nguoi test |  |
| Ngay test |  |
| Thiet bi |  |
| Android version |  |
| APK / versionName |  |
| Backend / n8n URL |  |
| Database |  |
| Ghi chu chung |  |

## Dieu kien truoc khi test

- Da cai dung APK can test.
- C72 scan RFID va QR hoat dong binh thuong.
- App da co session hop le: operator, department, note neu can.
- Backend `get-db`, `update-asset`, `checkout-asset`, `checkin-assets` dang online.
- Co it nhat 3 tai san mau:
  - 1 asset lookup duoc bang TID
  - 1 asset lookup duoc bang `So seri`
  - 1 case ngoai danh sach / khong co trong `asset_list1`

## Bang Checklist Tong

| STT | Nhom | Buoc test | Ky vong | Ket qua | Ghi chu |
|---|---|---|---|---|---|
| 1 | Smoke | Mo app tu launcher | App mo duoc, khong crash, vao dung dashboard |  |  |
| 2 | Smoke | Dong app, mo lai app | App mo lai duoc, khong treo, khong vo cache |  |  |
| 3 | Smoke | Chuyen qua lai giua Dashboard, Lookup, Inventory, Checkout, Logs, Settings | Dieu huong binh thuong, khong crash |  |  |
| 4 | Dashboard | Dashboard hien tong so, status sync, bo loc | UI dashboard render dung, khong co text loi bat thuong |  |  |
| 5 | Dashboard | Mo dropdown phong ban, vi tri, loai tai san | Distinct values hien dung, co du lieu |  |  |
| 6 | Dashboard | Bam `Xoa bo loc` | Tat ca filter ve trang thai mac dinh |  |  |
| 7 | Sync | Bam `Tai toan bo` | Sync thanh cong, co progress, khong crash |  |  |
| 8 | Sync | Full sync xong, vao lookup tim 1 asset vua dong bo | Tim ra dung du lieu |  |  |
| 9 | Sync | Bam `Tai theo bo loc` voi 1 phong ban | Chi tai du lieu dung phong ban da chon |  |  |
| 10 | Sync | Bam `Tai theo bo loc` voi nhieu phong ban / nhieu field | So request con va ket qua dung theo filter |  |  |
| 11 | Sync | Bam `Tai theo phien` khi session hop le | Chi tai theo pham vi session |  |  |
| 12 | Sync | Test sync theo vi tri co alias | Du lieu vi tri tra ve dung, khong bi rong sai |  |  |
| 13 | Lookup | Tim asset bang code | Tim ra dung asset |  |  |
| 14 | Lookup | Tim asset bang TID | Tim ra dung asset |  |  |
| 15 | Lookup | Sua 1 vai truong hop le roi luu | Update thanh cong, mo lai asset thay du lieu moi |  |  |
| 16 | Lookup | Thu luu khi khong co thay doi | App bao dung thong diep khong co thay doi |  |  |
| 17 | Checkout | Quet QR o tab Check Out | Them dung item vao danh sach checkout |  |  |
| 18 | Checkout | Quet RFID o tab Check Out | Them dung item vao danh sach checkout |  |  |
| 19 | Checkout | Quet lai cung 1 item | Duplicate bi chan dung |  |  |
| 20 | Checkout | Xuat CSV checkout | Tao file thanh cong, format dung |  |  |
| 21 | Checkin ticket | Import file checkout vao tab Check In | App doc file dung, summary hop le |  |  |
| 22 | Checkin ticket | Quet QR item co trong ticket | Item duoc danh dau da tra ve |  |  |
| 23 | Checkin ticket | Quet RFID item co trong ticket | Item duoc danh dau da tra ve |  |  |
| 24 | Checkin ticket | Quet item khong nam trong ticket | App hien dung trang thai khong nam trong phieu |  |  |
| 25 | Inventory | Bam `Tai tu API` | Dataset inventory nap duoc |  |  |
| 26 | Inventory | Import CSV inventory hop le | Dataset CSV nap duoc, khong crash |  |  |
| 27 | Inventory | Quet QR trung asset trong dataset | Item chuyen `Da kiem ke` |  |  |
| 28 | Inventory | Quet RFID trung asset trong dataset | Item chuyen `Da kiem ke`, TID ghi dung |  |  |
| 29 | Inventory | Quet item ngoai danh sach bang QR | Tao item `Ngoai danh sach` |  |  |
| 30 | Inventory | Quet item ngoai danh sach bang RFID | Tao item `Ngoai danh sach`, co TID/EPC |  |  |
| 31 | Inventory | Quet lai cung 1 item nhieu lan | Khong tao item moi sai, scan count tang hop ly |  |  |
| 32 | Inventory | Tim kiem trong man inventory | Search loc dung item |  |  |
| 33 | Inventory | Bam `Xoa ket qua` | Session inventory reset dung |  |  |
| 34 | Inventory export | Bam `Xuat CSV` khi da co du lieu | Tao file thanh cong, cot dung |  |  |
| 35 | Check-in upload | Bam `Gui kiem ke` khi offline | App bao offline, khong treo UI |  |  |
| 36 | Check-in upload | Bam `Gui kiem ke` khi khong co item hop le | App bao khong co du lieu hop le de gui |  |  |
| 37 | Check-in upload | Gui 1 item `Da kiem ke` lookup duoc bang TID | Backend insert thanh cong, app bao thanh cong |  |  |
| 38 | Check-in upload | Gui 1 item `Da kiem ke` lookup TID khong ra, fallback `So seri` ra | Backend insert thanh cong, map DB dung |  |  |
| 39 | Check-in upload | Gui 1 item `Ngoai danh sach` lookup DB khong ra | Backend van insert bang du lieu tu app |  |  |
| 40 | Check-in upload | Gui batch co nhieu item hop le | Summary `received/valid/skipped/inserted` hop ly |  |  |
| 41 | Check-in upload | Gui batch co item `Chua thay` trong session inventory | App khong gui item `Chua thay` len backend |  |  |
| 42 | Check-in upload | Backend tra `success=true`, `total_inserted=0` | App hien canh bao, giu nguyen session |  |  |
| 43 | Check-in upload | Backend tra `success=false` | App hien thong diep loi nghiep vu ro rang |  |  |
| 44 | Check-in upload | Backend tra body rong / response sai format | App hien loi parse ro rang, co log raw response |  |  |
| 45 | Check-in upload | Gui thanh cong xong kiem tra lai session inventory | Session van duoc giu nguyen, khong tu clear |  |  |
| 46 | Export / Import | Export logs CSV | Tao file log thanh cong |  |  |
| 47 | Export / Import | Export inventory / checkout nhieu lan lien tiep | Khong loi trung file, khong crash |  |  |
| 48 | Settings | Sua session roi luu | Session moi duoc dashboard / inventory / checkout su dung dung |  |  |
| 49 | Persistence | Force close sau khi sync / scan / update | Mo lai app khong mat du lieu bat thuong |  |  |
| 50 | Regression | Sau tat ca test, quay lai mo dashboard va lookup 1 asset vua thao tac | App van hoat dong on dinh, khong state leak |  |  |

## Checklist Rieng Cho Check-in Upload

### Payload app

- App phai gui `POST /webhook/checkin-assets`
- Root body phai co dang:

```json
{
  "items": [
    {
      "code": "...",
      "tid": "...",
      "epc_hex": "...",
      "scan_source": "RFID|QR",
      "scanned_at": "yyyy-MM-dd HH:mm:ss",
      "inventory_status": "Da kiem ke|Ngoai danh sach",
      "asset_name": "...",
      "user": "...",
      "department": "...",
      "location": "...",
      "asset_type": "...",
      "serial": "...",
      "operator": "...",
      "note": "..."
    }
  ]
}
```

### DB insert

- `checkin` trong DB phai luu dung ngay gio tu `scanned_at`
- `TID` phai luu tu app
- `So seri` phai luu tu `code` app
- Neu lookup DB ra:
  - `code <- Old_Serial`
  - `old_code <- Ma cu`
  - `serial <- SerialNumber`
- Neu lookup DB khong ra:
  - workflow phai fallback ve du lieu tu app
- Tuyet doi khong de insert fail chi vi lookup DB rong

### Response backend

- Luon tra JSON object
- Khong co nhanh nao tra body rong
- Contract khuyen nghi:

```json
{
  "success": true,
  "message": "Xu ly batch kiem ke thanh cong",
  "session_id": "",
  "total_received": 1,
  "total_scanned_valid": 1,
  "total_skipped": 0,
  "total_inserted": 1,
  "inserted_rows": []
}
```

## Mau Ghi Nhan Loi

| Muc | Noi dung |
|---|---|
| STT lien quan |  |
| Man hinh / flow |  |
| Buoc tai hien |  |
| Ket qua thuc te |  |
| Ket qua mong doi |  |
| Muc do anh huong |  |
| Anh / log / video dinh kem |  |

## Tong Ket

| Muc | Gia tri |
|---|---|
| Tong so case | 50 |
| PASS |  |
| FAIL |  |
| N/A |  |
| Danh gia chung |  |
