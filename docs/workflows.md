# Quy Trinh Nghiep Vu

Cap nhat: 2026-04-21

Tai lieu nay gom cac checklist thao tac hang ngay cho app Asset Manager.

## 1. Quy trinh mo ca lam viec

1. Mo app Asset Manager.
2. Cho Dashboard load xong.
3. Vao `Settings`.
4. Chon nguoi thao tac.
5. Chon phong ban.
6. Nhap ghi chu phien neu can.
7. Quay lai Dashboard.
8. Dong bo du lieu tai san.
9. Kiem tra tong so tai san va lan dong bo cuoi.

Ket qua mong muon:

- Dashboard hien co du lieu tai san.
- Phien lam viec co nguoi thao tac va phong ban.
- Co the bat dau Lookup, Inventory hoac Check Out.

## 2. Dong bo tat ca tai san

Dung khi can lam moi toan bo du lieu tu backend.

1. Dam bao may co internet.
2. Mo Dashboard.
3. Bam dong bo `Tat ca`.
4. Cho qua trinh dong bo ket thuc.
5. Kiem tra tong so tai san.
6. Vao `Assets` de xem danh sach neu can.

Neu ket qua bang 0 hoac bao loi:

- Kiem tra mang.
- Kiem tra backend n8n.
- Xem `Logs`.
- Thu lai sau khi backend san sang.

## 3. Dong bo theo bo loc

Dung khi chi can tai san cua mot phong ban, vi tri hoac loai tai san.

1. Mo Dashboard.
2. Chon phong ban neu can.
3. Chon vi tri neu can.
4. Chon loai tai san neu can.
5. Bam dong bo `Theo bo loc`.
6. Kiem tra tong so tai san sau dong bo.

Luu y:

- Neu backend chua ho tro filter dung nhu app gui len, ket qua co the rong.
- Voi location, app co the gui key chuan nhu `TT16_F5`.
- Backend nen map key location sang cac alias cu. Xem [get-db location mapping](get-db-location-key-n8n-mapping.md).

## 4. Dong bo theo phien

Dung khi phien lam viec da co phong ban va muon dong bo theo bo phan hien tai.

1. Vao `Settings`.
2. Chon phong ban cua phien.
3. Quay lai Dashboard.
4. Bam dong bo `Theo phien`.
5. Kiem tra ket qua dong bo.

Neu app bao thieu phong ban, quay lai `Settings` va chon phong ban truoc.

## 5. Kiem ke tai san

Dung khi can doi chieu tai san tai mot khu vuc, phong ban hoac kho.

1. Mo ca lam viec va dong bo du lieu.
2. Vao `Inventory`.
3. Chon che do quet phu hop.
4. Quet tat ca tai san trong khu vuc.
5. Theo doi cac nhom ket qua: da quet/matched, chua thay/missing, ngoai danh sach/outside va trung lap/duplicate.
6. Kiem tra lai cac item ngoai danh sach.
7. Di quet lai khu vuc neu so thieu bat thuong.
8. Xuat file CSV inventory.
9. Luu file export de doi chieu voi quan ly.

Tieu chi hoan tat:

- So item thieu da duoc xac minh.
- Item ngoai danh sach da duoc ghi nhan.
- CSV da duoc xuat thanh cong.

## 6. Tra cuu va ban giao tai san

Dung khi can kiem tra nhanh mot tai san hoac chuyen nguoi/phong ban/vi tri phu trach.

1. Vao `Lookup`.
2. Quet QR/RFID hoac nhap code/TID.
3. Kiem tra thong tin tai san.
4. Neu can sua, dung chuc nang edit.
5. Neu can ban giao, mo form ban giao.
6. Nhap nguoi nhan moi.
7. Chon phong ban moi.
8. Chon vi tri moi.
9. Kiem tra lai thong tin truoc khi luu.
10. Xac nhan ban giao.
11. Xem log hoac thong bao ket qua.

Luu y quan trong:

- Ban giao nen ghi lich su truoc khi update asset master.
- Khong dung thong tin sau update de tao note lich su.
- Xem mapping backend tai [checkout asset n8n mapping](checkout-asset-n8n-mapping.md).

## 7. Check out tai san

Dung khi dua tai san ra ngoai, cho muon, ban giao tam thoi hoac tao phieu theo su kien.

1. Vao `Check Out`.
2. O tab check out, nhap thong tin phieu: nguoi/cau hinh mang tai san, phong ban, muc dich, su kien, ngay check out, ngay du kien tra, nguoi phe duyet va ghi chu.
3. Chon RFID hoac QR.
4. Quet tung tai san.
5. Kiem tra danh sach item.
6. Xoa item sai neu can.
7. Bam export CSV.
8. Luu file CSV check out de dung cho check in ve sau.

Tieu chi hoan tat:

- File CSV check out da duoc tao.
- So luong item trong file dung voi thuc te ban giao.
- File duoc luu an toan va co the import lai.

## 8. Check in tai san

Dung khi tai san duoc tra ve.

1. Vao `Check Out`.
2. Chuyen sang tab `Check In`.
3. Import file CSV check out goc.
4. Kiem tra thong tin phieu.
5. Quet tung tai san tra ve.
6. Theo doi tong so, da tra va con thieu.
7. Kiem tra item ngoai danh sach neu co.
8. Xuat file CSV check in.

Tieu chi hoan tat:

- Tat ca item da tra duoc ghi nhan.
- Cac item thieu duoc ghi ro trong ket qua.
- File CSV check in da duoc luu.

## 9. Import CSV tai san

Dung khi can nap du lieu tai san tu file thay vi API.

1. Chuan bi file CSV UTF-8.
2. Dam bao file co header.
3. Mo app va vao `Assets`.
4. Chon import CSV.
5. Chon file.
6. Cho app doc file.
7. Kiem tra so luong asset import.
8. Loc/tim thu mot vai asset de xac nhan du lieu.

Luu y:

- App tu nhan delimiter `,` hoac `;`.
- Header co dau hay khong dau deu co the duoc normalize.
- Dong khong co code, TID va ten tai san se bi bo qua.

## 10. Xuat log va gui ho tro

Dung khi can dieu tra loi.

1. Vao `Logs`.
2. Tim thoi diem phat sinh loi.
3. Xuat log CSV neu man hinh co nut export.
4. Ghi lai thao tac da lam truoc khi loi xay ra.
5. Gui file log, file CSV lien quan va anh chup man hinh cho IT/dev.

Thong tin nen gui kem:

- Ten thiet bi.
- Thoi diem loi.
- Man hinh dang dung.
- Cach quet RFID hay QR.
- File CSV input/output neu co.
- Thong bao loi tren app.
