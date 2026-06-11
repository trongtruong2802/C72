# Huong Dan Su Dung Asset Manager

Cap nhat: 2026-04-21

Tai lieu nay danh cho nhan vien van hanh, kho, IT hien truong va nguoi quan ly tai san su dung app `Asset Manager` tren thiet bi C72/UHF.

## 1. App dung de lam gi

Asset Manager ho tro cac tac vu:

- Dong bo danh sach tai san tu backend n8n.
- Tra cuu tai san theo code, TID, QR hoac RFID.
- Sua thong tin va ban giao tai san.
- Kiem ke tai san bang RFID/QR.
- Tao file check out va check in bang CSV.
- Import CSV tai san khi can nap du lieu tu file.
- Xem va xuat nhat ky thao tac.

## 2. Chuan bi truoc khi su dung

Truoc khi thao tac, hay kiem tra:

- Thiet bi la Chainway C72 hoac may co dau doc UHF/barcode tuong duong.
- May co ket noi mang neu can dong bo voi backend.
- App da duoc cap quyen camera va storage.
- Dau doc RFID/barcode tren may dang hoat dong.
- Neu can xuat file, may phai cho phep ghi vao `Documents/IDO Asset`.

## 3. Man hinh Dashboard

Dashboard la man hinh dau tien khi mo app.

Tai Dashboard, nguoi dung co the:

- Xem trang thai du lieu tai san.
- Xem tong so tai san dang co trong cache.
- Xem lan dong bo cuoi.
- Chon bo loc theo phong ban, vi tri va loai tai san.
- Dong bo tat ca tai san.
- Dong bo theo bo loc.
- Dong bo theo phien lam viec.
- Mo nhanh Inventory, Check Out, Assets, Logs, Lookup va Settings.

Anh minh hoa co san:

![Dashboard](../codex_dashboard.png)

## 4. Thiet lap phien lam viec

Vao `Settings` hoac `Phien lam viec` tu Dashboard de khai bao thong tin phien hien tai.

Nen nhap:

- Nguoi thao tac.
- Phong ban.
- Ghi chu phien neu can.

Thong tin phien giup app ghi log, dat ngu canh cho dong bo theo phien va dien thong tin khi xuat file.

## 5. Dong bo du lieu tai san

App co 3 cach dong bo chinh:

| Cach dong bo | Khi nao dung |
|---|---|
| Tat ca | Khi can tai toan bo danh sach tai san tu backend |
| Theo bo loc | Khi chi can tai san theo phong ban, vi tri hoac loai tai san |
| Theo phien | Khi da thiet lap phong ban trong Settings va muon dong bo theo phien lam viec |

Sau khi dong bo, kiem tra:

- Tong so tai san tren Dashboard.
- Thong bao thanh cong hoac loi.
- Lan dong bo cuoi.
- Danh sach tai san trong man hinh `Assets`.

Neu dong bo theo phien bao loi, hay vao `Settings` kiem tra lai phong ban cua phien.

## 6. Tra cuu tai san

Vao `Lookup` de tim tai san.

Cac cach tim:

- Quet QR.
- Quet RFID.
- Nhap code hoac TID thu cong.

Khi tim thay tai san, app hien thi thong tin nhu:

- Ma tai san.
- TID/RFID.
- Ten tai san.
- Loai tai san.
- Serial.
- Nguoi dang su dung.
- Phong ban.
- Vi tri.
- Tinh trang.
- Ghi chu.

Tu man hinh nay co the sua thong tin hoac thuc hien ban giao.

## 7. Ban giao tai san

Ban giao duoc thuc hien tu man hinh `Lookup` sau khi da tim thay tai san.

Thong tin can kiem tra truoc khi ban giao:

- Tai san dung ma/code.
- Nguoi dang su dung hien tai.
- Phong ban hien tai.
- Vi tri hien tai.
- Nguoi nhan moi.
- Phong ban moi.
- Vi tri moi.

Luong ban giao se gui lich su ban giao truoc, sau do cap nhat thong tin asset master. Cach nay giup lich su giu duoc thong tin "tu ai -> den ai" va "tu dau -> den dau".

## 8. Kiem ke tai san

Vao `Inventory` de quet va doi chieu tai san.

Quy trinh goi y:

1. Dong bo du lieu tai san truoc khi kiem ke.
2. Vao `Settings` de chon nguoi thao tac va phong ban.
3. Vao `Inventory`.
4. Chon che do quet RFID hoac QR neu man hinh co tuy chon.
5. Bam scan hoac dung trigger vat ly tren may.
6. Theo doi cac chi so tong, da quet, chua thay va ngoai danh sach.
7. Kiem tra cac dong bi trung, ngoai danh sach hoac khong match.
8. Xuat CSV khi ket thuc.

File export inventory co dang:

```text
IDO_INVENTORY_<phong-ban>_<nguoi-thao-tac>_<ngay>.csv
```

Va duoc luu trong:

```text
Documents/IDO Asset
```

## 9. Check Out

Vao `Check Out` de tao phieu xuat/ban giao tam thoi bang CSV.

Thong tin nen nhap:

- Ten nguoi/cau hinh mang tai san.
- Phong ban.
- Muc dich.
- Su kien.
- Ngay check out.
- Ngay du kien tra.
- Nguoi phe duyet.
- Ghi chu.

Sau do:

1. Chon RFID hoac QR.
2. Quet tai san can dua vao phieu.
3. Kiem tra danh sach item da quet.
4. Xoa dong sai neu can.
5. Xuat file CSV check out.

File check out co dang:

```text
IDO_CHECKOUT_<phong-ban>_<nguoi-mang>_<ngay-xuat>_<ngay-tra>_<su-kien>.csv
```

## 10. Check In

Check in dung de doi chieu tai san tra ve voi file check out truoc do.

Quy trinh:

1. Vao tab `Check In` trong man hinh Check Out.
2. Import file CSV check out da tao truoc do.
3. Quet tai san tra ve bang RFID hoac QR.
4. Theo doi tong so, da tra va con thieu.
5. Xem cac item ngoai danh sach neu co.
6. Xuat file CSV check in.

File check in co dang:

```text
checkin_<ticket-id>_<timestamp>.csv
```

## 11. Danh sach tai san

Vao `Assets` de xem du lieu tai san dang co trong app.

Tai day co the:

- Xem danh sach tai san trong cache/runtime.
- Loc local theo thong tin tai san.
- Import file CSV tai san.
- Kiem tra du lieu sau khi dong bo API hoac import CSV.

Khi import CSV, app chap nhan nhieu ten cot tuong duong. Xem chi tiet o [Tham chieu CSV](csv-reference.md).

## 12. Nhat ky thao tac

Vao `Logs` de xem log nghiep vu gan day.

Nen kiem tra log khi:

- Dong bo that bai.
- Backend tra du lieu khong dung format.
- Check out/check in co ket qua khong nhu mong doi.
- Can doi chieu thao tac cua phien lam viec.

## 13. File xuat ra nam o dau

Tat ca file export duoc luu trong:

```text
Documents/IDO Asset
```

Cac nhom file chinh:

- Inventory CSV.
- Checkout CSV.
- Checkin CSV.
- Log CSV.

Neu khong thay file trong may tinh khi cam cap USB, hay mo lai trinh quan ly file tren thiet bi hoac cap quyen storage cho app.

## 14. Nguyen tac su dung an toan

- Luon dong bo du lieu truoc khi kiem ke hoac check out.
- Luon chon dung phien lam viec truoc khi xuat file.
- Sau khi quet, doc lai so luong tong va cac dong ngoai danh sach.
- Khong xoa file check out goc neu chua hoan tat check in.
- Khi ban giao, kiem tra ky nguoi nhan, phong ban va vi tri moi.
- Khi gap loi backend, chup man hinh thong bao va kiem tra `Logs`.
