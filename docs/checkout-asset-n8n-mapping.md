# Checkout Asset n8n Mapping

Muc tieu: ghi lich su ban giao dung "tu ai -> den ai" va "tu phong ban/vi tri nao -> den phong ban/vi tri nao" ma khong bi lay nham gia tri sau khi asset master da duoc cap nhat.

## Thu tu dung

1. Nhan webhook `checkout-asset`
2. Ghi lich su ban giao ngay tu payload webhook
3. Sau do moi goi `update-asset` de cap nhat asset master

Khong dung row asset master sau khi da update de build note lich su, vi luc nay gia tri cu da bi ghi de.

## Payload app gui len

Android hien gui cac field sau:

- `code`
- `tid`
- `assignedUser`
- `fromUser`
- `oldDepartment`
- `fromDepartment`
- `oldLocation`
- `fromLocation`
- `newUser`
- `toUser`
- `newDepartment`
- `toDepartment`
- `newLocation`
- `toLocation`
- `handoverDate`
- `status`
- `action`
- `checkoutStatus`
- `transactionType`
- `type`

## Mapping de xuat cho DB lich su ban giao

- `asset_code` <- `{{$json.code}}`
- `tid` <- `{{$json.tid}}`
- `from_user` <- `{{$json.fromUser || $json.assignedUser}}`
- `to_user` <- `{{$json.toUser || $json.newUser}}`
- `from_department` <- `{{$json.fromDepartment || $json.oldDepartment}}`
- `to_department` <- `{{$json.toDepartment || $json.newDepartment}}`
- `from_location` <- `{{$json.fromLocation || $json.oldLocation}}`
- `to_location` <- `{{$json.toLocation || $json.newLocation}}`
- `handover_date` <- `{{$json.handoverDate}}`
- `status` <- `{{$json.checkoutStatus || $json.status || "Checked Out"}}`
- `updated_by` <- `{{$json.toUser || $json.newUser}}`

## Note lich su de xuat

Neu DB van can luu cot `note`, dung payload webhook de tao:

```text
[Ban giao {{$json.handoverDate}}] Nguoi nhan: {{$json.toUser || $json.newUser}} | Tu nguoi dung: {{$json.fromUser || $json.assignedUser}} | Phong ban: {{$json.fromDepartment || $json.oldDepartment}} -> {{$json.toDepartment || $json.newDepartment}} | Vi tri: {{$json.fromLocation || $json.oldLocation}} -> {{$json.toLocation || $json.newLocation}}
```

## Dieu can tranh

Khong map sai theo cach nay:

- `Tu nguoi dung` <- `newUser`
- `Nguoi nhan` <- `assignedUser`
- `Phong ban cu` <- `newDepartment`
- `Vi tri cu` <- `newLocation`
- Build note sau khi `update-asset` da chay xong

## Case AREXAT mong muon

Payload dung phai tuong duong:

```json
{
  "code": "AREXAT",
  "tid": "E2801190200089A73CC203CA",
  "assignedUser": "Truong Vu",
  "fromUser": "Truong Vu",
  "oldDepartment": "BOD",
  "fromDepartment": "BOD",
  "oldLocation": "Lầu 5 - TT16",
  "fromLocation": "Lầu 5 - TT16",
  "newUser": "Thang Nguyen",
  "toUser": "Thang Nguyen",
  "newDepartment": "IT",
  "toDepartment": "IT",
  "newLocation": "Lầu 2 - TT16",
  "toLocation": "Lầu 2 - TT16",
  "handoverDate": "2026-04-09",
  "status": "Checked Out"
}
```

Tu payload nay, note dung phai ra:

```text
[Ban giao 2026-04-09] Nguoi nhan: Thang Nguyen | Tu nguoi dung: Truong Vu | Phong ban: BOD -> IT | Vi tri: Lầu 5 - TT16 -> Lầu 2 - TT16
```
