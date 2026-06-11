# Cleanup Baseline

Muc tieu cua file nay la ghi nhan hien trang truoc khi cleanup package / naming / legacy code, de rollback va doi chieu behavior sau nay.

## Current Structure

### Launcher and app entry

- Launcher activity hien tai:
  - `com.example.uhf.activity.DashboardActivity`
- Application class:
  - `com.example.uhf.AppContext`
- Android namespace / applicationId:
  - `com.example.uhf`

### Package roots hien co

- `com.example.uhf`
  - `AppContext`
  - `activity/DashboardActivity`
  - `tools/FileUtils`
  - `tools/ToastUtil`
- `com.idocean.asset`
  - `config`
  - `data/api`
  - `data/dto`
  - `data/mapper`
  - `data/repository`
  - `export`
  - `feature/dashboard`
  - `importer`
  - `model`
  - `scanner/barcode`
  - `scanner/rfid`
  - `storage`
  - `ui/assets`
  - `ui/checkout`
  - `ui/inventory`
  - `ui/logs`
  - `ui/lookup`
  - `ui/settings`
  - `utils`

### Helper / util classes dang ton tai

- Legacy helpers:
  - `com.example.uhf.tools.FileUtils`
  - `com.example.uhf.tools.ToastUtil`
- App helpers:
  - `com.idocean.asset.utils.AssetDepartmentUtils`
  - `com.idocean.asset.utils.AssetFieldNormalizer`
  - `com.idocean.asset.utils.AssetLocationUtils`
  - `com.idocean.asset.utils.EpcUtils`
  - `com.idocean.asset.utils.HardwareKeyUtils`
  - `com.idocean.asset.utils.NetworkUtils`
  - `com.idocean.asset.utils.TimeFormatUtils`
  - `com.idocean.asset.ui.checkout.CheckoutStateUtils`

## Dependency List

### libs/

Files dang co trong `app/libs/`:

- `DeviceAPI_ver20251103_release.aar`
- `jxl.jar`
- `poi-3.12-android-a.jar`
- `poi-ooxml-schemas-3.12-20150511-a.jar`
- `xUtils-2.5.5.jar`

### app/build.gradle

Implementation dependencies:

- `implementation files('libs/DeviceAPI_ver20251103_release.aar')`
- `androidx.appcompat:appcompat:1.7.0`
- `androidx.activity:activity:1.9.3`
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `androidx.lifecycle:lifecycle-livedata:2.7.0`
- `androidx.lifecycle:lifecycle-viewmodel:2.7.0`
- `androidx.recyclerview:recyclerview:1.3.2`
- `com.google.android.material:material:1.12.0`
- `com.squareup.retrofit2:retrofit:2.11.0`
- `com.squareup.retrofit2:converter-gson:2.11.0`
- `com.squareup.okhttp3:logging-interceptor:4.12.0`

Test dependencies:

- `junit:junit:4.13.2`
- `org.json:json:20240303`

Android test dependencies:

- `androidx.test.espresso:espresso-core:3.6.1`
- `androidx.test.ext:junit:1.2.1`

### Note ve dependency risk

- `androidx.activity` dang duoc dung.
- `androidx.lifecycle:lifecycle-livedata` va `androidx.lifecycle:lifecycle-viewmodel` da duoc khai bao nhung chua thay dung ro trong source hien tai, can audit them truoc khi loai bo.
- Cac file jar trong `libs/` co nguy co la legacy/khong con dung, nhung can build gate tung buoc truoc khi xoa.

## Risk Area

### 1. Namespace lai cu va moi dang tron

- Manifest launcher con tro vao `com.example.uhf.activity.DashboardActivity`.
- `AppContext`, `FileUtils`, `ToastUtil` van nam trong `com.example.uhf`.
- Cac class moi phan lon da nam trong `com.idocean.asset`.
- `R` hien dang thong qua namespace cu `com.example.uhf`, nen doi namespace se can nhieu buoc de khong vo build.

### 2. Legacy helper con duoc dung

- `AppContext` con duoc dung cho cache, crash log, va init toast.
- `FileUtils` con duoc dung cho crash file / storage helper.
- `ToastUtil` con duoc goi trong application lifecycle.
- Day la nhom khong the xoa ngay, chi co the thay the co kiem soat.

### 3. Device SDK la dependency core

- `DeviceAPI_ver20251103_release.aar` la SDK thiet bi.
- Day la dependency can giu de scanner RFID / barcode hoat dong.

### 4. Resource cu co the la rui ro xoa nham

Mot so resource legacy/khong ro reference:

- `drawable/phone.png`
- `drawable/triangle.png`
- `drawable/goroot.png`
- `drawable/uponelevel.png`
- `raw/barcodebeep.ogg`
- `raw/serror.ogg`

### 5. File / package legacy co the la nhanh xoa sau cung

- `com.example.uhf.activity.DashboardActivity`
- `com.example.uhf.tools.FileUtils`
- `com.example.uhf.tools.ToastUtil`
- `com.example.uhf.AppContext`

### 6. GUI / behavior sensitive zones

- Dashboard sync/filter/load.
- Lookup edit + handover 2 buoc.
- Inventory scan + export.
- Checkout scan + import/export.
- Storage/export/import path `Documents/IDO Asset`.

## Cleanup Guardrails

- Khong xoa file nao neu chua co build/install pass sau buoc xoa.
- Khong doi UI, layout XML, text, hay flow nguoi dung trong cleanup baseline.
- Khong doi API contract.
- Khong doi scanner logic tru khi vao phase refactor co kiem tra.
- Moi buoc cleanup phai co rollback qua git hoac khoi phuc file cuc bo.

## Baseline Build Status

- App hien tai da co cac vung refactor logic chinh on dinh va build/install xanh.
- File nay khong thay doi code runtime, chi ghi nhan hien trang de lam moc doi chieu cleanup.

