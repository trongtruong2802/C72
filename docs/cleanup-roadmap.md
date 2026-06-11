# Cleanup Roadmap

Muc tieu: loai bo phan legacy/khong con lien quan, giam namespace lan, giu behavior va UI hien tai nguyen ven.

## Nguyen tac bat buoc

- Khong doi UI, layout XML, text hien thi, hay flow nguoi dung.
- Khong doi API contract neu chua co lop tuong thich.
- Khong big-bang rewrite.
- Moi buoc phai build duoc va rollback duoc.
- Chi xu ly tung cum nho, uu tien co gate build/install sau moi cum.

## Pham vi giu lai

- Toan bo `com.idocean.asset.*` la code loi hien tai.
- `DeviceAPI_ver20251103_release.aar` phai giu.
- `DashboardActivity` giu lai den cuoi cung vi la launcher entry point.
- `AppContext`, `FileUtils`, `ToastUtil` chi loai sau khi da co lop thay the/an toan.

## Phan con legacy can don

### Nhom 1: thu vien/asset cu

- `app/libs/jxl.jar`
- `app/libs/poi-3.12-android-a.jar`
- `app/libs/poi-ooxml-schemas-3.12-20150511-a.jar`
- `app/libs/xUtils-2.5.5.jar`
- resource cu neu khong con reference:
  - `drawable/phone.png`
  - `drawable/triangle.png`
  - `drawable/goroot.png`
  - `drawable/uponelevel.png`
  - `raw/barcodebeep.ogg`
  - `raw/serror.ogg`

### Nhom 2: namespace legacy `com.example.uhf`

- `com.example.uhf.AppContext`
- `com.example.uhf.tools.FileUtils`
- `com.example.uhf.tools.ToastUtil`
- `com.example.uhf.activity.DashboardActivity`

### Nhom 3: dependency co the thua

- `androidx.lifecycle:lifecycle-livedata`
- `androidx.lifecycle:lifecycle-viewmodel`

## Thu tu cleanup an toan

### Phase 0: Khoa hien trang

Muc tieu:
- Chot lai checklist regression hien co.
- Xac dinh tat ca reference legacy con tai.

Cong viec:
- Search toan repo cho `com.example.uhf`, `jxl`, `poi`, `xUtils`.
- Danh dau resource/asset con reference va resource co the xoa.

Rollback:
- Khong doi code runtime.

### Phase 1: Xoa thu vien JAR thua

Muc tieu:
- Loai cac jar khong con dung trong `app/libs`.

Thu tu:
1. `xUtils-2.5.5.jar`
2. `jxl.jar`
3. `poi-3.12-android-a.jar`
4. `poi-ooxml-schemas-3.12-20150511-a.jar`

Gatet:
- Moi lan xoa xong phai `testDebugUnitTest`, `assembleDebug`, `installDebug`.

Rollback:
- Neu build fail, khoi phuc dung file jar vua xoa.

### Phase 2: Don legacy tools

Muc tieu:
- Danh gia va thay the `AppContext / FileUtils / ToastUtil`.

Huong:
- Neu con can `AppContext`, chuyen singleton/utility sang `com.idocean.asset`.
- Neu `FileUtils` chi con dung crash log hoac storage cu, tach sang helper moi truoc khi xoa.
- Neu `ToastUtil` khong con duoc goi, thay bang helper app hien tai va xoa moi file cu.

Rollback:
- Chi xoa sau khi da co file moi tuong thich va pass test tay.

### Phase 3: Move launcher activity

Muc tieu:
- Dua launcher entry point ve namespace thong nhat.

Huong:
- Di chuyen `DashboardActivity` sang `com.idocean.asset.feature.dashboard` hoac namespace tuong thich.
- Cap nhat manifest va cac import lien quan.

Rollback:
- Giua lai manifest cu cho toi khi build/install xanh.

### Phase 4: Dao het reference `com.example.uhf`

Muc tieu:
- Khi toan bo code da no reference cu, xoa package `com.example.uhf`.

Cong viec:
- Xac nhan khong con import/qualified name `com.example.uhf.*`.
- Xoa package legacy co soan o tren.

Rollback:
- Neu con 1 reference cu, dung xoa package.

### Phase 5: Prune resource va dependency

Muc tieu:
- Xoa resource/dep khong con dung sau khi da co evidence.

Cong viec:
- Xoa resource thua sau khi search khong con reference.
- Loai `lifecycle-livedata` va `lifecycle-viewmodel` neu scan source khong con dung.

Rollback:
- Them lai resource/dependency neu co manh gia tri build/runtime.

## Checklist build bat buoc sau moi phase

- `testDebugUnitTest`
- `assembleDebug`
- `installDebug`
- Mo app tren `C72 - 13`
- Dashboard load binh thuong
- Lookup / Inventory / Checkout van mo duoc

## No change zone trong cleanup

- Khong chinh layout XML.
- Khong doi text hien thi.
- Khong doi API contract.
- Khong doi scanner flow.
- Khong doi export format.
- Khong doi behavior mutation/sync/filter/cache da on dinh.
