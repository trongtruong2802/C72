# Refactor Regression Checklist

Tai lieu nay dung de test tay sau moi buoc refactor, theo dung nguyen tac "ruot thay dan, vo giu nguyen".

## Cach dung

- Dien `PASS`, `FAIL`, hoac `N/A` vao cot `Ket qua`.
- Neu `FAIL`, ghi them chi tiet o phan `Ghi chu loi`.
- Moi vong refactor nen test it nhat cac muc trong nhom `Bat buoc`.

## Thong Tin Vong Test

| Muc | Gia tri |
|---|---|
| Nguoi test |  |
| Ngay test |  |
| Thiet bi |  |
| Build / APK |  |
| Nhanh refactor |  |
| Ghi chu chung |  |

## Bang Checklist

| STT | Nhom | Buoc test | Ky vong | Ket qua |
|---|---|---|---|---|
| 1 | Bat buoc | Mo app tu launcher | App mo duoc, khong crash, vao dung man hinh khoi dong |  |
| 2 | Bat buoc | Quan sat dashboard ngay sau khi mo app | Dashboard load binh thuong, khong treo, khong bao loi UI |  |
| 3 | Bat buoc | Kiem tra tong quan du lieu tren dashboard | So luong asset, trang thai cache, sync state hien thi hop ly |  |
| 4 | Bat buoc | Bam `Tai toan bo` tren dashboard | Sync chay duoc, co tien trinh, hoan tat khong crash |  |
| 5 | Bat buoc | Bam `Tai theo bo loc` voi mot tap dieu kien hop le | Sync/filter chay duoc, khong bao rong sai, du lieu tra ve hop ly |  |
| 6 | Bat buoc | Bam `Tai theo phien` khi da co session hop le | Sync theo phien chay duoc, du lieu nam trong pham vi phien |  |
| 7 | Bat buoc | Mo dropdown `Phong ban`, `Vi tri`, `Loai tai san` tren dashboard | Distinct values van hien thi dung, khong mat du lieu, khong rong bat thuong |  |
| 8 | Bat buoc | Chon filter local tren dashboard roi thuc hien luong lien quan | Ket qua van dung theo dieu kien da chon |  |
| 9 | Bat buoc | Vao man tra cuu va tim asset bang code | Lookup ra dung asset theo code |  |
| 10 | Bat buoc | Vao man tra cuu va tim asset bang TID | Lookup ra dung asset theo TID |  |
| 11 | Bat buoc | Trong man tra cuu, bam `Edit`, sua mot vai truong hop le roi luu | Update asset thanh cong, field luu dung, khong sai map |  |
| 12 | Bat buoc | Trong man tra cuu, bam `Ban giao`, nhap day du thong tin roi xac nhan | Handover/checkout chay duoc, khong crash, backend nhan dung payload |  |
| 13 | Bat buoc | Mo lai asset vua ban giao | User / phong ban / vi tri moi phan anh dung trong asset master |  |
| 14 | Bat buoc | Vao inventory va thuc hien scan QR | Scan inventory ghi nhan dung item |  |
| 15 | Bat buoc | Vao inventory va thuc hien scan RFID | Scan inventory ghi nhan dung item |  |
| 16 | Bat buoc | Tai inventory, bam export khi da co du lieu | File export inventory duoc tao thanh cong |  |
| 17 | Bat buoc | Tai checkout/checkin, bam export khi da co du lieu | File export checkout/checkin duoc tao thanh cong |  |
| 18 | Bat buoc | Force close app roi mo lai | Cache van duoc load lai, khong mat du lieu bat thuong |  |
| 19 | Bat buoc | Sau khi mo lai app, kiem tra dashboard va tra cuu mot asset gan day | Dashboard van doc duoc cache, lookup van hoat dong binh thuong |  |
| 20 | Mo rong | Mo man danh sach asset | Man danh sach mo duoc, khong crash |  |
| 21 | Mo rong | Dung search + filter trong man danh sach asset | Filter local van dung, so luong hien thi hop ly |  |
| 22 | Mo rong | Mo dropdown distinct values trong man danh sach asset | Distinct values van dung va dong nhat voi du lieu hien tai |  |
| 23 | Mo rong | Tai inventory tu API | Dataset inventory nap duoc, khong crash |  |
| 24 | Mo rong | Import CSV trong inventory | CSV duoc nap dung, khong vo du lieu hien tai ngoai ky vong |  |
| 25 | Mo rong | Kiem tra summary inventory sau scan | Tong, da kiem, chua thay, ngoai danh sach cap nhat dung |  |
| 26 | Mo rong | Scan QR trong checkout | Asset duoc dua vao danh sach checkout dung |  |
| 27 | Mo rong | Scan RFID trong checkout | Asset duoc dua vao danh sach checkout dung |  |
| 28 | Mo rong | Scan QR trong checkin | Asset duoc danh dau tra ve dung |  |
| 29 | Mo rong | Scan RFID trong checkin | Asset duoc danh dau tra ve dung |  |
| 30 | Mo rong | Vao settings, sua session, luu, mo lai | Session duoc luu va duoc dashboard / inventory / checkout su dung dung |  |
| 31 | Mo rong | Vao logs va export log | File log CSV duoc tao thanh cong |  |

## Ghi Chu Loi

### Loi 1

| Muc | Noi dung |
|---|---|
| STT checklist lien quan |  |
| Buoc test |  |
| Mo ta loi |  |
| Muc do anh huong |  |
| Co the tai hien? |  |
| Ghi chu them |  |

### Loi 2

| Muc | Noi dung |
|---|---|
| STT checklist lien quan |  |
| Buoc test |  |
| Mo ta loi |  |
| Muc do anh huong |  |
| Co the tai hien? |  |
| Ghi chu them |  |

## Tong Ket

| Muc | Gia tri |
|---|---|
| Tong so ca test |  |
| PASS |  |
| FAIL |  |
| N/A |  |
| Danh gia chung |  |

## Goi Y Vong Test Toi Thieu Sau Moi Buoc Refactor

Neu buoc refactor chi dong vao code noi bo va khong cham scanner / export, toi thieu nen chay:

1. Mo app khong crash.
2. Dashboard load binh thuong.
3. `Tai toan bo` hoac luong sync lien quan van chay.
4. Lookup bang code.
5. Update asset hoac handover neu buoc refactor cham vao mutation.
6. Force close va mo lai app de kiem tra cache.

## Checklist Theo Lat Cat Refactor Da Lam

### 1. Tach Sync Asset Sang AssetSyncService

Day la vong refactor chi tach logic sync batched, khong doi UI, khong doi public API cua `AssetRepository`.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| S1 | Mo app tu launcher | App mo binh thuong, khong crash |  |
| S2 | Dashboard load lan dau | So lieu cache va trang thai dashboard hien thi hop ly |  |
| S3 | Bam `Tai toan bo` | Sync chay duoc, co tien trinh, ket thuc khong loi |  |
| S4 | Bam `Tai theo bo loc` voi 1 dieu kien | Luong sync/filter van chay duoc, khong tra rong sai |  |
| S5 | Bam `Tai theo phien` | Du lieu sync theo phien van dung |  |
| S6 | Force close app roi mo lai | Cache van load lai duoc |  |
| S7 | Vao tra cuu tim 1 asset bang code hoac TID | Lookup van dung sau khi sync |  |

### 2. Tach Filter Local Va Distinct Values Sang AssetFilterService

Day la vong refactor chi tach logic filter local, normalize match va distinct values, khong doi flow nguoi dung.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| F1 | Mo dashboard va mo dropdown `Phong ban` | Danh sach phong ban van hien thi dung |  |
| F2 | Mo dropdown `Vi tri` | Danh sach vi tri van hien thi dung, khong rong sai |  |
| F3 | Mo dropdown `Loai tai san` | Danh sach loai tai san van hien thi dung |  |
| F4 | Chon 1 bo loc tren dashboard roi thuc hien luong lien quan | Ket qua van dung theo dieu kien da chon |  |
| F5 | Vao man danh sach asset, dung search + filter | Filter local van dung, so luong hien thi hop ly |  |
| F6 | Vao `Tra cuu`, kiem tra dropdown runtime values | Distinct values van len dung o cac field can thiet |  |
| F7 | Vao `Settings`, kiem tra dropdown phong ban | Distinct values van dung o man cai dat |  |
| F8 | Force close app roi mo lai | Distinct values van duoc dung lai tu cache/runtime state |  |

### 3. Tach Cache Memory Va Cache Disk Sang AssetCacheStore

Day la vong refactor chi tach logic cache runtime + disk cache + snapshot, khong doi format cache file va khong doi flow load cache cua app.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| C1 | Mo app sau khi da co cache tu truoc | Dashboard van hien thi du lieu cache, khong crash |  |
| C2 | Force close app roi mo lai | Cache van duoc khoi phuc lai dung |  |
| C3 | Bam `Tai toan bo` tren dashboard | Cache moi van duoc ghi sau sync thanh cong |  |
| C4 | Sau khi sync xong, force close roi mo lai | Du lieu vua sync van con sau khi mo lai |  |
| C5 | Vao `Tra cuu`, tim 1 asset vua dong bo | Lookup doc duoc asset tu cache moi |  |
| C6 | Sua 1 asset roi luu | Cache runtime duoc cap nhat dung sau mutation |  |
| C7 | Ban giao 1 asset roi mo lai app | Asset master trong cache van phan anh dung thong tin moi |  |
| C8 | Mo dropdown dashboard sau khi mo lai app | Distinct values van dung khi doc tu snapshot/cache |  |

### 4. Tach Mutation Sang AssetMutationService

Day la vong refactor chi tach logic `update asset`, `handover 2 buoc`, parse mutation response va cache update sau mutation, khong doi API contract va khong doi entry point tu Activity.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| M1 | Vao `Tra cuu`, lookup 1 asset bang code | Asset van mo dung de thuc hien mutation |  |
| M2 | Trong `Tra cuu`, sua 1 field hop le roi bam luu | `update-asset` van thanh cong, field luu dung |  |
| M3 | Mo lai asset vua sua | Du lieu master phan anh dung thay doi vua luu |  |
| M4 | Thu 1 ca `Ban giao` day du thong tin | Buoc 1 `checkout-asset` va buoc 2 `update-asset` van chay duoc |  |
| M5 | Sau khi ban giao, kiem tra asset master | `assignedUser`, `department`, `location` moi dung |  |
| M6 | Sau khi ban giao, kiem tra log/history phia backend | Payload old/new va thong diep backend van dung |  |
| M7 | Force close app roi mo lai sau khi sua hoac ban giao | Cache sau mutation van duoc giu dung |  |
| M8 | Thu 1 ca update khong co thay doi | App van bao dung thong diep `Khong co thay doi de cap nhat.` |  |

### 5. Tach LookupActivity Sang LookupController

Day la vong refactor chi tach business logic lookup/edit/handover ra khoi Activity, giu nguyen UI, scanner flow va entry point tu man tra cuu.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| L1 | Mo man tra cuu | Man mo binh thuong, khong crash |  |
| L2 | Kiem tra focus scanner khi vao man tra cuu | QR van duoc focus mac dinh, khong doi flow scan |  |
| L3 | Quet QR de lookup mot asset | Asset van duoc tim dung, status hien dung |  |
| L4 | Quet RFID de lookup mot asset | Asset van duoc tim dung, status hien dung |  |
| L5 | Bam `Edit`, sua mot vai truong hop le roi luu | Update asset thanh cong, khong sai map du lieu |  |
| L6 | Bam `Edit` roi huy | Asset duoc render lai dung, khong mat state |  |
| L7 | Bam `Ban giao`, nhap day du thong tin roi xac nhan | Luong ban giao 2 API van chay duoc, khong crash |  |
| L8 | Sau ban giao, mo lai asset vua cap nhat | User / phong ban / vi tri moi van dung |  |
| L9 | Xoay man hinh / mo lai man tra cuu sau khi da co asset | State voi asset hien tai van khoi phuc dung |  |
| L10 | Ban giao hoac update that bai co chu y | Thong bao loi va trang thai edit/saving van hop ly |  |

### 6. Tach Scanner Lookup Sang Controller Rieng

Day la vong refactor chi tach logic scanner cua `LookupActivity` sang controller rieng, giu nguyen trigger, QR/RFID flow va cach hien thi ket qua hien tai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| SC1 | Mo man tra cuu | Man mo binh thuong, khong crash |  |
| SC2 | Vao man tra cuu va kiem tra scanner mac dinh | QR van duoc focus mac dinh, khong doi flow scan |  |
| SC3 | Quet QR de lookup mot asset | Ket qua QR van tim dung asset, status hien dung |  |
| SC4 | Quet RFID de lookup mot asset | Ket qua RFID van tim dung asset, status hien dung |  |
| SC5 | Bam `Dung scan` khi QR dang quet | Scanner QR dung dung, status cap nhat hop ly |  |
| SC6 | Doi giua QR va RFID roi scan lai | Moi che do van chay theo dung trigger va khong lech state |  |
| SC7 | Vao lookup, de man o trang thai san sang roi thoat man / mo lai | Scanner lifecycle khong crash, man mo lai van binh thuong |  |
| SC8 | Quet xong mot asset roi quay lai man list / dashboard | Khong bi giu state scanner sai va khong crash |  |

### 7. Tach InventoryActivity Sang InventoryController

Day la vong refactor chi tach business logic inventory ra khoi Activity, giu nguyen UI, flow scan, format export va cach render hien tai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| I1 | Mo man kiem ke | Man mo binh thuong, khong crash |  |
| I2 | Kiem tra session info tren man kiem ke | Operator va department hien dung nhu truoc |  |
| I3 | Bam `Tai tu API` | Dataset moi duoc nap, khong doi format hien thi |  |
| I4 | Bam `Nhap CSV` voi file hop le | Dataset nap tu CSV van dung, khong loi UI |  |
| I5 | Quet QR trung asset trong dataset | Item duoc danh dau `Da kiem ke`, counter cap nhat dung |  |
| I6 | Quet RFID trung asset trong dataset | Item duoc match theo TID/code nhu truoc, counter cap nhat dung |  |
| I7 | Quet QR/RFID ngoai danh sach | Item ngoai danh sach duoc tao 1 lan va khong bi trung sai |  |
| I8 | Quet lai cung 1 asset nhieu lan | Duplicate van tang scan count dung, khong tao item moi |  |
| I9 | Bam `Xoa ket qua` | Ket qua session duoc reset, outside items bi xoa, source item ve `Chua thay` |  |
| I10 | Bam `Xuat ket qua` | File CSV van xuat duoc, format cot va noi dung khong doi |  |
| I11 | Nhap chuoi tim kiem tren man kiem ke | Loc local van dung, khong mat item sau scan |  |
| I12 | Dong app va mo lai sau khi da co du lieu kiem ke | Cache load lai duoc, dashboard metrics va danh sach van hop ly |  |

### 8. Tach CheckoutActivity Sang CheckoutController

Day la vong refactor chi tach business logic checkout/checkin ra khoi Activity, giu nguyen UI, flow scan, format CSV va scanner lifecycle hien tai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| K1 | Mo man check out/check in | Man mo binh thuong, khong crash |  |
| K2 | Kiem tra tab mac dinh va scanner status | Tab hien dung, trang thai scanner khong bi doi sai |  |
| K3 | Quet QR o tab Check Out voi asset hop le | Asset duoc them vao danh sach checkout, count cap nhat dung |  |
| K4 | Quet lai cung QR do | Duplicate van bi chan dung, khong tao item moi |  |
| K5 | Quet RFID o tab Check Out voi asset hop le | Asset duoc them vao danh sach checkout, identity key van dung |  |
| K6 | Bam `Xoa` mot item checkout | Item bi xoa dung, list va count cap nhat dung |  |
| K7 | Bam `Xoa danh sach` checkout | Tat ca item checkout bi reset, nut export cap nhat dung |  |
| K8 | Nhap du form checkout va bam `Xuat` | Validation van dung, CSV checkout duoc tao voi format cu |  |
| K9 | Import file checkout hop le sang tab Check In | Import van dung, summary va danh sach expected hien dung |  |
| K10 | Quet QR o tab Check In voi asset co trong ticket | Asset duoc danh dau da tra ve, summary cap nhat dung |  |
| K11 | Quet RFID o tab Check In voi asset co trong ticket | Asset duoc danh dau da tra ve, summary cap nhat dung |  |
| K12 | Quet asset ngoai ticket o tab Check In | Trang thai `khong nam trong phieu` van hien dung |  |
| K13 | Bam `Xoa ket qua` o tab Check In | Session check in duoc reset, state import van giu dung |  |
| K14 | Xuat CSV check in sau khi import | File CSV check in van tao duoc, format cot khong doi |  |
| K15 | Dong app roi mo lai sau khi da co checkout/checkin | State retained van khoi phuc dung, khong mat item |  |

### 9. Tach DashboardActivity Sang DashboardController

Day la vong refactor chi tach logic dieu phoi dashboard ra khoi Activity, giu nguyen UI, nut bam, text hien thi va flow sync hien tai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| D1 | Mo app tu launcher | App mo binh thuong, khong crash, vao dashboard dung nhu cu |  |
| D2 | Quan sat dashboard ngay sau khi mo app | Thong tin cache, last sync, preview va progress hien thi binh thuong |  |
| D3 | Bam `Tai toan bo` | Sync toan bo chay duoc, co progress, khong doi flow |  |
| D4 | Bam `Tai theo bo loc` voi mot tap dieu kien hop le | Sync theo bo loc chay duoc, khong bao rong sai |  |
| D5 | Bam `Tai theo phien` khi da co session hop le | Sync theo phien chay duoc, du lieu nam trong phien |  |
| D6 | Mo dropdown `Phong ban`, `Vi tri`, `Loai tai san` | Distinct values van dung va khong bi mat sau refactor |  |
| D7 | Chon nhieu gia tri trong mot dropdown roi dong y | Summary filter hien dung, flow sync van dung nhu cu |  |
| D8 | Bam `Xoa bo loc` | Tat ca filter duoc reset ve trang thai mac dinh |  |
| D9 | Xem progress khi dang sync | Thanh progress va text preview/progress cap nhat dung |  |
| D10 | Sau khi sync xong, kiem tra status va last sync | Status cap nhat dung, last sync hien thi dung |  |
| D11 | Force close app roi mo lai | Cache va distinct values van load lai duoc, khong crash |  |
| D12 | Vao man tra cuu / inventory / checkout sau khi dashboard sync | Cac man khac van chay binh thuong, khong bi anh huong boi dashboard refactor |  |

### 10. Gom Storage Export Import Sang Module Rieng

Day la vong refactor chi gom logic storage/export/import thanh module ro rang hon, khong doi format file, khong doi duong dan luu file va khong doi flow xin quyen hien tai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| S1 | Vao man inventory va bam `Xuat ket qua` | App van xin quyen nhu truoc va xuat duoc file CSV |  |
| S2 | Kiem tra file inventory vua xuat | File nam trong `Documents/IDO Asset`, ten file va format cot khong doi |  |
| S3 | Vao man checkout va bam `Xuat` o tab Check Out | CSV checkout van tao duoc, delimiter va thu tu cot khong doi |  |
| S4 | Vao man checkout va bam `Xuat` o tab Check In | CSV check in van tao duoc, delimiter va thu tu cot khong doi |  |
| S5 | Mo lai file checkout da xuat va import vao app | App doc duoc file cu, map du lieu van dung |  |
| S6 | Xuat log runtime tu man Logs | File log van tao duoc, format semicolon khong doi |  |
| S7 | Chay xuat file tren Android R+ | Flow xin quyen `All files access` van giu nhu truoc |  |
| S8 | Chay xuat file tren Android duoi R | Flow xin `WRITE_EXTERNAL_STORAGE` van giu nhu truoc |  |
| S9 | Sau khi xuat file, kiem tra file co xuat hien trong bo nho ngoai | Media scanner van quet file, file de mo lai tu ben ngoai |  |
| S10 | Import CSV asset cu tu man inventory | Import van doc duoc file co format cu, khong doi mapping |  |
| S11 | Xuat checkout / inventory / logs nhieu lan lien tiep | Ten file van hop le, khong bi loi trung ten hoac format |  |
| S12 | Dong app roi mo lai, sau do tiep tuc xuat file | Flow xuat van chay binh thuong, khong phu thuoc state cu |  |

### 11. Chuan Hoa Model Va State

Day la vong refactor chi tach draft model va gom normalize du lieu chung, giu `Asset` lam model doc chinh va khong doi behavior hien thi ben ngoai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| M1 | Mo man `Tra cuu` va load 1 asset | Form edit hien thi dung gia tri nhu truoc, khong doi UI |  |
| M2 | Bam `Edit`, sua mot vai truong hop hop le roi luu | `EditableAssetDraft` van di qua luong update nhu cu, khong sai map du lieu |  |
| M3 | Bam `Ban giao`, nhap thong tin roi xac nhan | `HandoverDraft` van di qua luong 2 API nhu cu, khong doi flow |  |
| M4 | Vao man `Assets` va mo dropdown bo loc | Phong ban / vi tri / loai tai san van hien dung, khong mat value cu |  |
| M5 | Loc theo phong ban, vi tri, loai tai san, trang thai | KQ loc local van giong truoc refactor |  |
| M6 | Xoay man hinh o `Tra cuu` sau khi da load asset | State edit/asset hien tai van khoi phuc dung |  |
| M7 | Mo lai app sau khi da doi du lieu asset | Cache va snapshot van dung, field normalize khong lam lech du lieu |  |
| M8 | Kiem tra asset type / status tren man hien thi va export lien quan | Gia tri hien thi van khong doi format, chi duoc chuan hoa dung nhu truoc |  |

### 12. Package Cleanup Dashboard Feature

Day la vong cleanup package nho va an toan dau tien cho namespace, chi di chuyen cac class controller/state/test cua dashboard sang package feature rieng, khong doi UI hay flow.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| P1 | Mo app tu launcher | Dashboard mo binh thuong, khong crash |  |
| P2 | Bam `Tai toan bo` tren dashboard | Sync van chay duoc, progress va toast khong doi |  |
| P3 | Bam `Tai theo bo loc` | Flow sync theo bo loc van nhu cu, khong doi behavior |  |
| P4 | Bam `Tai theo phien` | Flow sync theo phien van khong doi |  |
| P5 | Mo dropdown phong ban / vi tri / loai tai san | Distinct values van hien dung nhu truoc |  |
| P6 | Bam `Xoa bo loc` | Filter ve trang thai mac dinh va UI khong doi |  |
| P7 | Force close app roi mo lai | Dashboard cache va last sync van load dung |  |
| P8 | Di sang lookup / inventory / checkout sau khi dashboard da load | Cac man khac van chay binh thuong, khong bi anh huong boi package move |  |

### 13. Tang Cuong Regression Test Cho Refactor

Day la phan test unit bo sung de khoa chinh cac behavior vua refactor, uu tien luong thuan JVM va cache round-trip de giam rui ro khi cleanup package/naming ve sau.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| T1 | Parse response `get-db` voi `total_count` alias | Parser van hieu total count va doc duoc asset |  |
| T2 | Filter local voi nhieu phong ban / vi tri / loai tai san | Dieu kien OR trong tung nhom va AND giua cac nhom van dung |  |
| T3 | Normalize department / location / asset type / status / condition | Alias legacy va khoang trang van duoc chuan hoa dung |  |
| T4 | Build request update asset | `assignedUser` va cac field thay doi van map dung |  |
| T5 | Build request handover / checkout | `code`, `tid`, `from/to`, `handoverDate`, `Checked Out` van dung contract |  |
| T6 | Parse mutation response cho `update-asset` va `checkout-asset` | Affected rows / success / mismatch van duoc phan loai dung |  |
| T7 | Cache disk round-trip qua `filesDir` gia lap | Serialize, read lai, clear cache van giu format cu va xoa file dung |  |
| T8 | Dashboard clearFilters | Tat ca selection ve rong, khong anh huong UI |  |
| T9 | Checkout RFID scan | Quet RFID them item moi va chan duplicate dung |  |
| T10 | Checkin RFID | Ghi nhan returned dung cho ticket da import |  |
| T11 | Lookup handover normalize fallback | Gia tri phong ban / vi tri khi input rong van fallback dung |  |

### 14. Cleanup Util Helper Thua

Day la vong cleanup helper legacy, uu tien xoa `ToastUtil`, rut gon `FileUtils`, va thay `AssetDepartmentUtils` bang `AssetFieldNormalizer` ma khong doi UI / flow.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| U1 | Mo app tu launcher | App mo binh thuong, khong crash |  |
| U2 | Vao man `Settings` va chon phong ban | Dropdown phong ban van hien va luu dung nhu truoc |  |
| U3 | Vao man `Tra cuu` va load asset | Field phong ban / vi tri / loai tai san van hien dung |  |
| U4 | Vao man `Checkout` va chon phong ban | Dropdown phong ban van doc duoc value runtime va default nhu cu |  |
| U5 | Chay luong crash handler gia lap neu co the | Crash log van duoc ghi vao cung cho, khong doi format file log |  |
| U6 | Chay `testDebugUnitTest` sau cleanup | Test pass, khong con fail do import/helper legacy |  |
| U7 | Chay `assembleDebug` sau cleanup | Project build thanh cong |  |

### 15. Chuan Hoa Launcher DashboardActivity

Day la buoc xac nhan launcher entry point da tro ve DashboardActivity va khong co launcher duplicate.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| L1 | Mo app tu launcher | App mo dung man Dashboard |  |
| L2 | Kiem tra manifest hop nhat | Chi co 1 `MAIN + LAUNCHER`, khong co launcher duplicate |  |
| L3 | Launch app tren thiet bi that | Khong crash khi start, vao Dashboard binh thuong |  |
| L4 | Dong app roi mo lai tu launcher | Vao lai Dashboard, khong doi flow nguoi dung |  |

### 16. Loai Bo Package Legacy com.example.uhf

Day la buoc xac nhan namespace legacy da duoc thay toan bo bang `com.idocean.asset` va app van launch / scan binh thuong.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| N1 | Scan source/test tim `com.example.uhf` | Khong con reference nao trong code chay |  |
| N2 | Build `testDebugUnitTest` sau doi namespace | Test pass, khong con import old package |  |
| N3 | Build `assembleDebug` sau doi namespace | APK build thanh cong |  |
| N4 | Install APK moi len may | Cai dat thanh cong voi package moi `com.idocean.asset` |  |
| N5 | Launch app bang package moi | App mo dung Dashboard, khong crash |  |
| N6 | Quet QR/RFID o Lookup sau doi namespace | Scanner van chay nhu cu, khong bi anh huong |  |
| N7 | Scan `androidTest` tim `com.example.uhf` | Khong con reference nao con sot trong test tree |  |

### 17. Cleanup Resource Va Dependency Thua

Day la buoc dọn resource legacy va dependency khong dung, uu tien khong doi UI/flow va build phai xanh sau moi batch xoa.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| R1 | Build `assembleDebug` sau khi xoa drawable legacy | Project van build thanh cong |  |
| R2 | Build `testDebugUnitTest` sau khi xoa dependency thua | Unit test van pass, khong con import dep cu |  |
| R3 | Install APK moi len may | App van cai dat duoc binh thuong |  |
| R4 | Mo app tu launcher | Dashboard mo binh thuong, khong crash |  |
| R5 | Vao Inventory / Checkout / Lookup / Dashboard | UI va flow khong doi sau cleanup resource |  |

### 20. Tach Scanner Checkout Sang Controller Rieng

Day la vong refactor chi tach logic scanner cua `CheckoutActivity` sang controller rieng, giu nguyen UI, flow scan, va logic checkout/checkin hien tai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| C1 | Mo man check out/check in | Man mo binh thuong, khong crash |  |
| C2 | Quet QR o tab Check Out | Asset van duoc them vao draft checkout nhu cu |  |
| C3 | Quet QR o tab Check In | Asset van duoc danh dau returned hoac not-in-ticket dung nhu cu |  |
| C4 | Quet RFID o tab Check Out | TID/UHF van match dung, khong doi flow scan |  |
| C5 | Quet RFID o tab Check In | TID/UHF van match dung, khong doi flow scan |  |
| C6 | Quet duplicate QR/RFID trong cung tab | Duplicate van bi chan dung, khong tao item moi |  |
| C7 | Pause app khi dang scan roi resume lai | Scanner release/re-acquire dung, khong leak resource |  |
| C8 | Vao ra man nhieu lan lien tiep | Khong crash, khong mat scanner va khong treo hardware |  |
| C9 | Chuyen giua Check Out va Check In khi da co du lieu | Scanner lifecycle va nut start/stop van cap nhat dung |  |

### 21. Chuan Hoa Shared Scanner Logic

Day la vong cleanup nho cho logic scanner dung chung, chu yeu gom helper RFID/TID normalize va chuan hoa ten callback o Lookup.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| H1 | Chay unit test helper normalize RFID/TID | Sanitize TID, normalize hex va build key van cho ket qua nhu cu |  |
| H2 | Mo man kiem ke, quet RFID lien tuc | Scanner shared helper khong lam doi flow inventory |  |
| H3 | Mo man kiem ke, quet QR | QR flow inventory van binh thuong |  |
| H4 | Mo man check out/check in, quet RFID | Checkout scanner shared helper khong lam doi flow check out/check in |  |
| H5 | Mo man check out/check in, quet QR | QR checkout flow van binh thuong |  |
| H6 | Mo man tra cuu, quet QR | Lookup van scan va tim asset nhu cu |  |
| H7 | Mo man tra cuu, quet RFID | Lookup RFID/TID van match dung |  |
| H8 | Pause/resume va vao ra cac man scanner | Khong crash, khong leak scanner, callback van chuan |  |

### 22. Toi Uu Dashboard Sync Khong Block UI

Day la vong toi uu nho cho Dashboard sync, chi dam bao cac buoc nang duoc chay o background va UI chi render ket qua cuoi.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| D1 | Bam `Tai toan bo` voi du lieu lon | UI khong bi dong, progress van cap nhat |  |
| D2 | Bam `Tai theo bo loc` voi bo loc co san | UI khong lag, ket qua sync van dung |  |
| D3 | Bam `Tai theo phien` | Flow sync phien van chay nhu cu |  |
| D4 | Xoay man hinh trong luc dang sync | Khong crash, khong treo UI, trang thai van hop le |  |
| D5 | Bam sync nhieu lan lien tiep | Nut khong gay treo, chi 1 luong sync hop le duoc chay |  |
| D6 | Quan sat sau khi sync xong | Dropdown bo loc, status va cache van cap nhat dung |  |
| D7 | Re-open dashboard sau sync | Cache va filter options van load lai binh thuong |  |

### 18. Dashboard Filter Header Wrap Fix

Day la buoc chinh UI nho cho dashboard filter header, chi de tranh text bi xuong dong xau mat, khong doi logic dong bo.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| D1 | Mo dashboard tren may C72 | Dong chu filter header khong bi roi xuong dong |  |
| D2 | Quan sat header `Bo loc` va nut `Xoa bo loc` | Hai thanh phan van nam tren cung mot dong, giao dien gon hon |  |
| D3 | Bam `Tải theo bộ lọc` va `Xoa bo loc` | Flow sync va clear filter khong doi, chi thay doi cach hien thi |  |
### 19. Tach Scanner Inventory Sang Controller Rieng

Day la vong refactor chi tach logic scanner cua `InventoryActivity` sang controller rieng, giu nguyen trigger, QR/RFID flow, warmup va cach hien thi scanner hien tai.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| S1 | Mo man kiem ke | Man mo binh thuong, khong crash |  |
| S2 | Kiem tra scanner status ngay khi vao man | Trang thai scanner va nut start/stop hien dung nhu truoc |  |
| S3 | Quet RFID lien tuc trong mot khoang thoi gian | Item van duoc match dung, khong crash va khong bi do state scanner |  |
| S4 | Quet QR | QR van match dung asset, status cap nhat dung |  |
| S5 | Bat / tat scanner bang nut Start/Stop | Scanner start/stop dung luc, khong stop sai thoi diem |  |
| S6 | Pause app khi dang scan roi resume lai | Scanner khong leak resource, man quay lai van binh thuong |  |
| S7 | Vao ra man kiem ke nhieu lan lien tiep | Khong crash, khong giu scanner sai trang thai sau khi quay lai |  |
| S8 | Quet RFID roi QR xen ke | Ca hai che do van hoat dong nhu cu, khong lech flow |  |

### 23. Dashboard Sync UX State Ro Hon

Day la buoc UX nho cho dashboard, tap trung vao trang thai dong bo ro rang hon va thong diep loi de hieu hon.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| U1 | Mo dashboard lan dau | Trang thai idle hien ro, khong co cam giac treo |  |
| U2 | Bam sync | Hien loading state, co step text dang tai du lieu |  |
| U3 | Khi bat dau batch | Step text doi sang dang xu ly du lieu, progress cap nhat |  |
| U4 | Sync thanh cong | Hien success state va thong diep hoan tat |  |
| U5 | Sync that bai do mang | Hien loi mang ro rang, khac voi loi API/parse |  |
| U6 | Sync that bai do API | Hien thong diep loi API ro rang |  |
| U7 | Sync that bai do parse | Hien thong diep loi du lieu / parse ro rang |  |
| U8 | Xem chip trang thai | Chip doi mau tuong ung voi loading / success / error |  |

### 24. Chuan Hoa Error Mapping Sync Update Checkout

Day la buoc regression cho cac thong diep loi moi. Muc tieu la phan biet loi mang, timeout, backend, parse va business error theo tung luong.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| E1 | Gay timeout khi sync dashboard | Hien thong diep timeout, khac loi mang thong thuong |  |
| E2 | Gay loi parse du lieu khi sync dashboard | Hien thong diep loi du lieu / parse ro rang |  |
| E3 | Gay loi backend HTTP khi sync dashboard | Hien thong diep API / backend ro rang |  |
| E4 | Gay loi update asset bang backend khong tra loi dung | Hien thong diep update that bai ro rang, co context endpoint/action |  |
| E5 | Gay loi checkout / handover bang backend khong tra loi dung | Hien thong diep checkout / handover that bai ro rang, khac loi mang |  |
| E6 | Xem log debug cho sync/update/checkout loi | Log co endpoint, action va chi tiet exception / response |  |

### 25. Toc Do Bam Sync Dashboard

Day la buoc regression cho tinh nang bam sync tren dashboard. Muc tieu la khong bi treo UI khi app phai doc cache/dong bo du lieu lon.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| P1 | Mo dashboard va bam `Tai toan bo` ngay lap tuc | Nut phan hoi nhanh, khong co treo khi bat dau sync |  |
| P2 | Bam `Tai toan bo` khi cache local co san | UI khong bi dong truoc khi request chay |  |
| P3 | Bam `Tai theo bo loc` tren bo du lieu lon | Khong co cam giac lag khi bat dau sync |  |
| P4 | Bam sync nhieu lan lien tiep | Chi co 1 luong sync hop le, khong bi treo giao dien |  |
| P5 | Quan sat luc sync dang chay | Progress van cap nhat, UI van tuong tac duoc o muc co the |  |

### 26. Rollback Dong Bo Sang Mot Luot Fetch

Day la buoc rollback de lam dong bo quay ve mot lan fetch duy nhat, bo qua paging/fallback phuc tap de uu tien on dinh va toc do bam sync.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| R1 | Mo dashboard va bam `Tai toan bo` | Sync bat dau ngay, khong bi treo truoc khi gui request |  |
| R2 | Sync voi du lieu lon khoang 4000+ asset | Chi chay mot luot fetch, khong paging nhieu lan |  |
| R3 | Sync voi bo loc san co | Ket qua sync van ra, khong rollback sang chuoi batch/fallback phuc tap |  |
| R4 | Bam sync nhieu lan lien tiep | Chi 1 luong sync hop le duoc chay, khong treo UI |  |
| R5 | Quan sat sau khi sync xong | Cache duoc cap nhat va dashboard load lai binh thuong |  |

### 27. Khoi Phuc Filter Sync

Day la buoc regression cho dong bo theo bo loc sau rollback dong bo all ve mot luot fetch. Muc tieu la giu sync toan bo on dinh, nhung filter sync van hoat dong dung nhu cu.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| F1 | Chon bo loc phong ban + vi tri + loai tai san | Query sync duoc tao dung, khong bi bo mat selection |  |
| F2 | Bam `Tai theo bo loc` | Sync chay theo nhom loc, khong bi chuyen sang luong all |  |
| F3 | Chon bo loc co nhieu gia tri trong mot nhom | Ket qua sync van dung, khong mat gia tri con lai |  |
| F4 | Sync theo phien voi phong ban co san | Session sync van lay dung phong ban, khong bi loi bo loc |  |
| F5 | Sync theo bo loc tren du lieu lon | App khong crash va ket qua van khop bo loc da chon |  |
| F6 | Chon 2-3 vi tri trong filter | Sync phai lay du ca nhung location da chon, khong chi location dau tien |  |

### 28. Toi Uu Multi Location Sync

Day la buoc regression cho truong hop chon nhieu vi tri trong dashboard sync. Muc tieu la giu ket qua dung, nhung khong roi ve luong full fallback neu khong can.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| M1 | Chon `L\u1ea7u 5 - TT16` va `L\u1ea7u 6 - TT16` roi bam sync | Ca hai location duoc tinh dung trong ket qua sync |  |
| M2 | Chon nhieu vi tri + 1 phong ban | Ket qua van la giao cua tat ca selection, khong bi chi lay location dau tien |  |
| M3 | Chon nhieu vi tri + nhieu loai tai san | Sync van chay dung, khong bo mat asset match hop le |  |
| M4 | Chon vi tri co alias legacy | Location alias van map dung, khong loi parse / sync |  |
| M5 | Sync nhieu vi tri tren du lieu lon | Khong crash, khong mat ket qua, khong bi treo UI khi bat dau sync |  |

### 29. Request Builder Multi Filter

Day la buoc regression cho request sync moi. Muc tieu la dam bao query sync gui duoc nhieu gia tri cho department, location va assetType trong cung mot request.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| Q1 | Tao query voi 2 departments | Request co 2 gia tri department, khong chi lay gia tri dau tien |  |
| Q2 | Tao query voi 2 locations | Request co 2 gia tri location, khong chi lay location dau tien |  |
| Q3 | Tao query voi 2 assetTypes | Request co 2 gia tri assetType, khong chi lay gia tri dau tien |  |
| Q4 | Tao query voi full sync khong co filter | Request khong co bo loc, chi co limit/offset neu can |  |
| Q5 | Tao query voi group rong | Request bo qua group rong va khong crash |  |

### 30. Sync Strategy Selection Runtime

Day la buoc regression cho runtime chon strategy dong bo. Muc tieu la full sync va filtered sync deu di qua duong ro rang, con local fallback chi dung khi that su can.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| S1 | Bam `Tai toan bo` | Runtime chon batched full sync, khong nhay lung tung qua nhieu nhanh |  |
| S2 | Chon filter phong ban / vi tri / loai tai san roi bam sync | Runtime chon filtered remote batched, request gui du multi-value |  |
| S3 | Chon nhieu vi tri nhu `L\u1ea7u 5 - TT16` va `L\u1ea7u 6 - TT16` | Runtime van chon remote batched, ket qua khong chi lay phan tu dau |  |
| S4 | Buoc local fallback co du lieu backend rong / khong hop le | App co duong fallback ro rang, khong crash va khong treo UI |  |
| S5 | Filtered sync voi mot nhom rong | Khong crash, selection rong duoc bo qua hop le |  |

### 31. Multi Location Sync Fix

Day la buoc regression cho loi chon nhieu vi tri trong dashboard sync. Muc tieu la dam bao nguoi dung co the chon 2 vi tri khac nhau va sync van tra ve dung ket qua, khong chi location dau tien.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| L1 | Mo dashboard, chon `L\u1ea7u 5 - TT16` va `L\u1ea7u 6 - TT16` | UI van luu duoc ca 2 location da chon |  |
| L2 | Bam `Tai theo bo loc` voi 2 location tren | Sync di qua duong an toan, khong chi lay location dau tien |  |
| L3 | Chon 2-3 location khac nhau trong cung mot lan sync | Ket qua sync phai bao gom tat ca location da chon |  |
| L4 | Chon location + department + assetType | Ket qua la giao cua tat ca selection, khong mat filter nao |  |
| L5 | Sync xong quay lai mo lai filter location | Da chon van hien thi dung, khong bi reset ve 1 muc |  |

### 32. Multi Select Fallback Cho Tat Ca Nhom

Day la buoc regression cho truong hop chon nhieu gia tri o bat ky nhom nao trong dashboard sync. Muc tieu la dam bao department, location va assetType deu duoc xu ly dung khi co 2+ lua chon.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| G1 | Chon 2 department roi bam sync | Sync di qua duong an toan, khong mat 1 trong 2 department |  |
| G2 | Chon 2 location roi bam sync | Sync lay du ca 2 location, khong chi location dau tien |  |
| G3 | Chon 2 assetType roi bam sync | Sync lay du ca 2 assetType, khong chi assetType dau tien |  |
| G4 | Chon 2 department + 2 location + 2 assetType | Ket qua sync la giao cua tat ca selection |  |
| G5 | Chon 1 nhom nhieu va 1 nhom rong | Nhom rong duoc bo qua, nhom nhieu van duoc xu ly dung |  |

### 33. Backend CSV Multi Filter Contract

Day la buoc regression cho contract sync moi voi backend. Muc tieu la dam bao app gui dung format CSV theo key plural de backend doc duoc nhieu gia tri trong cung mot request.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| C1 | Chon 2 department | Request gui `departments=IT,HR` |  |
| C2 | Chon 2 location | Request gui `locations=Lau 5 - TT16,Lau 6 - TT16` |  |
| C3 | Chon 2 assetType | Request gui `assetTypes=Laptop,Adapter` |  |
| C4 | Chon ket hop ca 3 nhom | Request van la 1 call `get-db` voi 3 key plural |  |
| C5 | Mo log runtime sau khi bam sync | Thay log `Chon chien luoc dong bo` va URL request dung format CSV |  |

### 34. Rollback Sync Ve Backend Cu

Day la buoc regression cho viec rollback sync ve contract backend cu. Muc tieu la giu full sync va single-filter sync qua backend, con multi-filter se fallback local de khong loi.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| B1 | Bam `Tai toan bo` | Full sync van chay qua batched sync nhu truoc |  |
| B2 | Chon 1 department roi bam sync | Request gui `department=<gia tri>` |  |
| B3 | Chon 1 location roi bam sync | Request gui `location=<gia tri>` |  |
| B4 | Chon 1 assetType roi bam sync | Request gui `assetType=<gia tri>` |  |
| B5 | Chon 2+ department/location/assetType roi bam sync | Runtime fallback local filter, khong phu thuoc backend multi-value |  |
| B6 | Chon 2+ gia tri o nhieu nhom cung luc | Ket qua sync dung va khong crash |  |

### 35. Remote Sync Bung Alias Location

Day la buoc mo rong request sync cho location de remote get-db nhan du bo alias da co san. Muc tieu la single location canonical cung phai bung ra alias khi can, multi-location thi merge alias on dinh, con local fallback cu van duoc giu nguyen.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| L1 | Chon `Lau 5 - TT16` roi bam sync | Request gui `locations=` gom `Lau 5 - TT16`, `Idoplex - 5`, `Idoplex-5`, `Lau 5 - TT17` |  |
| L2 | Chon `Lau 6 - TT16` roi bam sync | Request gui `locations=` gom `Lau 6 - TT16`, `Idoplex - 6`, `Idoplex-6` |  |
| L3 | Chon `Lau 5 - TT16` + `Lau 6 - TT16` roi bam sync | Alias cua ca hai vi tri duoc merge, bo trung, giu thu tu on dinh |  |
| L4 | Chon `department + location` roi bam sync | Request van gui du department va location alias-expanded trong cung 1 request |  |
| L5 | Chon location khong co alias dac biet | Request van giu key singular `location=` neu danh sach chi con 1 gia tri |  |

### 36. Bo Preview Request Rieng Trong Batched Sync

Day la buoc toi uu runtime sync de giam 1 request mang moi lan dong bo. Muc tieu la dung page dau tien lam preview + batch dau, nhung van giu nguyen parse, paging, alias handling va fallback cu.

| STT | Buoc test | Ky vong | Ket qua |
|---|---|---|---|
| P1 | Bam `Tai toan bo` | App van hien progress va sync thanh cong, nhung so request bat dau it hon truoc |  |
| P2 | Chon 1 location co alias roi bam sync | Sync van ra ket qua dung, khong mat alias-expanded request |  |
| P3 | Chon `department + location + assetType` | Progress van cap nhat binh thuong, khong crash |  |
| P4 | Chon bo loc khong co ket qua | App van tra rong dung, fallback local neu can van chay |  |
| P5 | Bam sync nhieu lan lien tiep | UI khong bi treo them va flow van on dinh |  |
