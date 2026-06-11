# Tham Chieu CSV

Cap nhat: 2026-04-21

Tai lieu nay mo ta cac file CSV ma Asset Manager doc va ghi.

## 1. Nguyen tac chung

- Nen luu file CSV bang UTF-8.
- App co the doc delimiter `,` hoac `;`.
- Header duoc normalize: khong phan biet hoa/thuong, khoang trang, dau gach, dau tieng Viet.
- O trong, `null`, `undefined`, `n/a`, `#n/a` duoc xem nhu rong.
- Dong import tai san chi duoc nhan neu co it nhat mot trong cac thong tin: code, TID, ten tai san hoac serial.

## 2. Import danh sach tai san

Man hinh: `Assets`

App doc CSV tai san va map vao model `Asset`.

### Cot khuyen nghi

```csv
code,tid,asset_name,asset_type,serial_number,department,user,location,inventory_status,condition,tag_date,tag_by,note
```

### Alias cot duoc ho tro

| Truong noi bo | Ten cot/alias chap nhan |
|---|---|
| So thu tu | `stt`, `id`, `row_number`, `row_no`, `index`, `indexid` |
| Ma tai san | `code`, `asset_code`, `ma`, `assetcode`, `item_code`, `so_seri`, `ma_qr_code` |
| TID/RFID | `tid`, `epc`, `rfid`, `rfid_tid`, `tag_id`, `tagid` |
| Ma cu | `old_code`, `code_old`, `ma_cu`, `oldcode` |
| Serial cu | `old_serial`, `serial_old`, `serial_cu`, `oldserial` |
| Ten tai san | `asset_name`, `name`, `assetname`, `ten_tai_san`, `asset`, `ten_nhan_hieu_quy_cach_vat_tu_dung_cu` |
| Loai tai san | `asset_type`, `type`, `category`, `assettype`, `loai_tai_san`, `phan_loai_cap_2`, `phan_loai_cap_1`, `item_type`, `nhom` |
| Serial | `serial_number`, `serial`, `serial_no`, `serialnumber`, `productnumber` |
| Phong ban | `department`, `dept`, `department_name`, `bo_phan`, `phong_ban`, `bo_phan_su_dung` |
| Nguoi su dung | `user`, `assigned_user`, `assigneduser`, `owner`, `employee`, `nguoi_su_dung`, `username` |
| Vi tri | `location`, `location_name`, `room`, `area`, `vi_tri`, `vi_tri_dia_diem` |
| Trang thai kiem ke | `inventory_status`, `status`, `check_status`, `trang_thai_kiem_ke`, `inventorystatus` |
| Tinh trang | `condition`, `asset_condition`, `tinh_trang` |
| Ngay dan tag | `tag_date`, `tagged_at`, `created_at`, `ngay_dan_tag`, `ngay_kiem_ke`, `created` |
| Nguoi dan tag | `tag_by`, `tagged_by`, `created_by`, `nguoi_dan_tag`, `nguoi_tao`, `user_name` |
| Ghi chu | `note`, `notes`, `remark`, `description`, `ghi_chu`, `ghi_chu_kiem_ke` |

## 3. Export inventory

Man hinh: `Inventory`

Ten file:

```text
IDO_INVENTORY_<phong-ban>_<nguoi-thao-tac>_<ngay>.csv
```

Thu muc:

```text
Documents/IDO Asset
```

Header:

```csv
code,tid,epc_hex,scan_source,scanned_at,inventory_status,asset_name,user,department,location,asset_type,serial,operator,note
```

Y nghia:

| Cot | Y nghia |
|---|---|
| `code` | Ma tai san |
| `tid` | TID hien thi |
| `epc_hex` | EPC raw neu co |
| `scan_source` | Nguon quet: RFID/QR/manual tuy app gan nhan |
| `scanned_at` | Thoi diem quet |
| `inventory_status` | Trang thai doi chieu |
| `asset_name` | Ten tai san |
| `user` | Nguoi su dung |
| `department` | Phong ban |
| `location` | Vi tri |
| `asset_type` | Loai tai san |
| `serial` | Serial |
| `operator` | Nguoi thao tac |
| `note` | Ghi chu kiem ke |

## 4. Export checkout

Man hinh: `Check Out`, tab check out.

Ten file:

```text
IDO_CHECKOUT_<phong-ban>_<nguoi-mang>_<ngay-checkout>_<ngay-tra>_<su-kien>.csv
```

Header:

```csv
export_type,export_version,ticket_id,exported_at,carrier_name,department,purpose,event_name,checkout_at,expected_return_at,approver,note,identity_key,matched_from_cache,tid,code,asset_name,asset_type,serial_number,assigned_user,asset_department,location,scan_source,scanned_at
```

Gia tri quan trong:

| Cot | Y nghia |
|---|---|
| `export_type` | Luon la `IDO_CHECKOUT` |
| `export_version` | Version format, hien tai la `1` |
| `ticket_id` | Ma phieu, dang `CO_<timestamp>` |
| `identity_key` | Khoa doi chieu, uu tien `TID:<tid>`, neu khong co TID thi `CODE:<code>` |
| `matched_from_cache` | `true` neu item match voi cache tai san |
| `scan_source` | Nguon quet |
| `scanned_at` | Thoi diem quet item |

File checkout la file dau vao cho quy trinh check in. Khong nen sua tay neu khong can thiet.

## 5. Import checkout de check in

Man hinh: `Check Out`, tab check in.

App chi nhan cac dong co:

```text
export_type = IDO_CHECKOUT
```

Neu file co dong `sep=,` hoac `sep=;`, app se doc delimiter tu dong nay. Neu khong co, app uu tien doc theo format checkout cua app.

Item trung `identity_key` se bi bo qua de tranh tinh 2 lan.

## 6. Export checkin

Man hinh: `Check Out`, tab check in.

Ten file:

```text
checkin_<ticket-id>_<timestamp>.csv
```

Header:

```csv
export_type,export_version,ticket_id,checkout_source_file,checkout_exported_at,checkin_exported_at,carrier_name,department,purpose,event_name,checkout_at,expected_return_at,approver,note,identity_key,result_status,matched_by,expected_in_ticket,tid,code,asset_name,asset_type,serial_number,assigned_user,asset_department,location,checkout_scan_source,checkout_scanned_at,checkin_scan_source,checkin_scanned_at,result_note
```

Gia tri quan trong:

| Cot | Y nghia |
|---|---|
| `export_type` | Luon la `IDO_CHECKIN` |
| `checkout_source_file` | Ten file checkout goc da import |
| `result_status` | Trang thai doi chieu check in |
| `matched_by` | Cach app match item |
| `expected_in_ticket` | Item co nam trong phieu checkout goc hay khong |
| `result_note` | Ghi chu ket qua |

## 7. Mau CSV tai san toi thieu

```csv
code,tid,asset_name,department,user,location
AREXAT,E2801190200089A73CC203CA,Laptop Dell,IT,Truong Vu,Lau 5 - TT16
```

Voi file toi thieu nay, app co the import asset va dung cho lookup/inventory.
