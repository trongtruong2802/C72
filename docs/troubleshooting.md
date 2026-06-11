# Xu Ly Loi Thuong Gap

Cap nhat: 2026-04-21

Tai lieu nay tong hop cac loi thuong gap khi su dung Asset Manager tren C72/UHF.

## 1. Khong dong bo duoc du lieu

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| Dashboard bao loi mang | May khong co internet hoac mang noi bo bi chan | Kiem tra Wi-Fi/4G, mo trinh duyet thu backend neu co |
| Dong bo tra ve 0 tai san | Backend khong co du lieu hoac filter qua hep | Bam dong bo tat ca, xoa bo loc, kiem tra backend |
| Dong bo theo phien loi | Chua chon phong ban trong Settings | Vao Settings chon phong ban roi thu lai |
| Dong bo theo vi tri rong | Backend chua map location key | Xem mapping tai `docs/get-db-location-key-n8n-mapping.md` |
| HTTP 200 nhung body rong | Workflow n8n tra response khong dung contract | Kiem tra node Respond to Webhook va response JSON |

## 2. RFID khong quet duoc

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| Bam scan khong co tag | Dau doc UHF chua init hoac service scanner loi | Dong app mo lai, khoi dong lai thiet bi, kiem tra app scanner goc |
| Chi QR chay, RFID khong chay | Module UHF/DeviceAPI khong kha dung | Kiem tra thiet bi co dung C72/UHF khong |
| RFID doc qua it | Cong suat/vung tan so/khoang cach khong phu hop | Thu gan tag hon, doi goc quet, kiem tra cau hinh dau doc |
| Nut trigger vat ly khong hoat dong | Nut bi service khac hoac setting he thong chiem | Thu nut scan trong UI, kiem tra DeviceManager/Scanner settings |

## 3. QR/camera khong quet duoc

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| Camera khong mo | Chua cap quyen camera | Cap quyen camera trong Android Settings |
| Quet ma nhung khong tim thay asset | QR khong phai code/TID app dang co | Thu tim thu cong, kiem tra du lieu trong Assets |
| Camera mo nhung kho focus | Anh sang yeu hoac ma QR mo | Tang anh sang, lau camera, thu khoang cach khac |

## 4. Khong xuat duoc CSV

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| Export that bai | App chua co quyen storage | Cap quyen Files and media/Manage all files |
| Khong thay file sau export | Media scanner chua cap nhat hoac dang xem sai thu muc | Mo `Documents/IDO Asset`, rut/cam lai USB, mo lai file manager |
| Ten file la `khong-ro` | Session/form thieu thong tin | Dien phong ban, nguoi thao tac, nguoi mang, su kien truoc khi export |
| File mo Excel bi loi font | File dang UTF-8 BOM nhung Excel cau hinh ngon ngu khac | Import CSV trong Excel voi UTF-8 hoac mo bang Google Sheets |

## 5. Import CSV that bai

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| File CSV rong | File khong co du lieu | Kiem tra file truoc khi import |
| Khong doc duoc header | Dong dau khong phai header | Dat header o dong dau tien |
| Import ra it dong | Dong thieu code, TID va ten tai san | Kiem tra cac cot bat buoc/alias trong `csv-reference.md` |
| Sai dau phay/cham phay | File dung delimiter la | App tu nhan `,` hoac `;`, nen luu lai CSV chuan UTF-8 |

## 6. Check out/check in khong dung so luong

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| Check out thieu item | Chua quet het hoac tag bi trung/khong match | Quet lai, kiem tra danh sach truoc khi export |
| Check in import khong duoc | File khong phai CSV check out cua app | Dung file co `export_type=IDO_CHECKOUT` |
| Check in co item ngoai danh sach | Tai san tra ve khong nam trong phieu goc | Kiem tra tai san thuc te, xuat check in de ghi nhan |
| Mat file check out goc | Khong the doi chieu check in day du | Tim backup trong `Documents/IDO Asset` hoac may tinh da luu |

## 7. Ban giao tai san sai lich su

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| Lich su bi dao nguoi gui/nguoi nhan | Backend map sai payload | Kiem tra `docs/checkout-asset-n8n-mapping.md` |
| Phong ban cu bi ghi thanh phong ban moi | Backend tao note sau khi update asset master | Ghi lich su truoc, update asset sau |
| Ban giao update asset nhung khong co history | Workflow `checkout-asset` loi | Xem log backend va log app |

## 8. App bi treo hoac du lieu khong cap nhat

| Trieu chung | Nguyen nhan kha di | Cach xu ly |
|---|---|---|
| Vua sync/import xong nhung list chua doi | Cache/runtime chua refresh | Quay lai Dashboard, mo lai man hinh hoac restart app |
| App cham sau khi quet nhieu | Danh sach scan qua lon | Export ket qua, xoa phien scan neu co, mo lai app |
| Man hinh bi mat trang thai sau khi thoat | Session/cache noi bo bi reset | Vao Settings cau hinh lai phien |

## 9. Thong tin can gui cho IT/dev

Khi bao loi, nen gui kem:

- Anh chup man hinh loi.
- Ten man hinh dang dung.
- Thao tac vua lam truoc khi loi.
- File CSV input/output neu co.
- Thoi diem loi.
- Ten thiet bi hoac serial thiet bi.
- Log export tu man hinh `Logs` neu co.

## 10. Cach kiem tra nhanh tren thiet bi

1. Mo app vao Dashboard.
2. Dong bo tat ca.
3. Vao Assets tim mot asset.
4. Vao Lookup tim dung asset do.
5. Vao Inventory quet thu RFID/QR.
6. Xuat CSV inventory.
7. Vao Check Out quet mot item va export.
8. Import lai file check out trong tab Check In.

Neu ca 8 buoc tren chay duoc, app va thiet bi co ban dang hoat dong binh thuong.
