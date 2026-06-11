# Build Tu Source

Cap nhat: 2026-06-11

Tai lieu nay danh cho dev/IT clone repo `uhf-truong` ve may moi va build APK tu source.

## 1. Yeu cau moi truong

- Android Studio ban on dinh gan day.
- JDK 17.
- Android SDK Platform 34.
- Android Build Tools do Android Studio de xuat khi sync.
- PowerShell tren Windows neu build bang command line.
- `adb` neu can cai debug len thiet bi.

## 2. Clone repo

```powershell
git clone <repo-url>
cd uhf-truong
```

Neu nhan project qua file zip, giai nen ra mot thu muc khong bi read-only truoc khi mo bang Android Studio.

## 3. Tao file local.properties

Project nay doc file `local.properties` o thu muc goc de lay duong dan Android SDK va API key.

Tao file `local.properties` cung cap voi `build.gradle` goc, vi du:

```properties
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
IDO_API_KEY=your-api-key
```

Ghi chu:

- `sdk.dir` phai dung voi duong dan Android SDK tren may clone.
- `IDO_API_KEY` co the de trong neu chi can build APK, nhung cac tinh nang dong bo API se khong hoat dong dung.
- Khong commit `local.properties` len Git.

Neu khong muon dung file nay cho API key, co the dat bien moi truong `IDO_API_KEY` truoc khi build.

## 4. Mo project va sync

Co 2 cach:

1. Mo thu muc repo bang Android Studio va cho Gradle sync xong.
2. Hoac build truc tiep bang Gradle wrapper tu command line.

Project dang dung:

- Android Gradle Plugin `8.7.2`
- Gradle wrapper `8.10.2`

## 5. Build debug APK

```powershell
.\gradlew.bat assembleDebug
```

Sau khi build thanh cong, APK debug thuong nam trong:

```text
app/build/outputs/apk/debug/
```

Ten APK hien tai duoc dat theo `versionName`:

```text
UHF-serial_v1.4.6.apk
```

## 6. Chay test unit va build

```powershell
.\gradlew.bat --no-daemon --max-workers=1 testDebugUnitTest assembleDebug
```

Lenh nay huu ich de xac nhan repo clone ve van build duoc va khong vo test co ban.

## 7. Cai ban debug len thiet bi

Kiem tra ADB:

```powershell
adb devices
```

Cai ban debug:

```powershell
.\gradlew.bat installDebug
```

Neu thiet bi la Chainway C72, can dam bao da bat USB debugging va cap quyen tren thiet bi.

## 8. Luu y rieng cua repo nay

- File `app/libs/DeviceAPI_ver20251103_release.aar` la bat buoc de build va chay tinh nang scanner/RFID.
- `app/build.gradle` hien dang doc `rootProject.file("local.properties")`, vi vay chi can duy tri file `local.properties` o thu muc goc.
- Cau hinh signing hien tai tham chieu `app/uhf-serial_release.jks` cho ca `debug` va `release`.
- Neu clone repo ma thieu file `app/uhf-serial_release.jks`, can cap nhat signing config trong `app/build.gradle` truoc khi build.
- App co the build tren may Android thong thuong, nhung tinh nang RFID/barcode phu thuoc SDK va service cua thiet bi Chainway tuong thich.

## 9. Loi thuong gap

### SDK location not found

Nguyen nhan:

- Chua tao `local.properties`
- `sdk.dir` sai duong dan

Xu ly:

- Kiem tra lai duong dan Android SDK tren may.
- Mo Android Studio de cai them SDK neu may moi chua co.

### Build duoc nhung app khong dong bo du lieu

Nguyen nhan:

- `IDO_API_KEY` rong, sai, hoac backend khong truy cap duoc

Xu ly:

- Dat lai `IDO_API_KEY` trong `local.properties` hoac environment variable.
- Kiem tra ket noi toi backend `https://n8n.idocean.info:8443/webhook/`.

### App mo duoc nhung khong quet RFID

Nguyen nhan:

- Thiet bi khong co UHF module hoac scanner service khong san sang

Xu ly:

- Thu tren Chainway C72 hoac thiet bi tuong duong.
- Kiem tra service scanner goc cua thiet bi.

## 10. Build nhanh

Neu da co moi truong day du, chi can 3 buoc:

1. Clone repo.
2. Tao `local.properties`.
3. Chay `.\gradlew.bat assembleDebug`.
