# Báo Cáo Đánh Giá Chất Lượng Mã Nguồn (Code Review Report)

Tài liệu này tổng hợp kết quả đánh giá toàn bộ mã nguồn của dự án **UHF Asset Manager** (`e:\design\uhf-truong`). Báo cáo tập trung vào việc tìm ra các điểm chưa tốt (code smells, memory leaks, rủi ro đa luồng, mã nguồn dư thừa) và đưa ra các đề xuất cải tiến cụ thể cùng ví dụ minh họa mà không làm thay đổi giao diện, luồng quét hay logic nghiệp vụ cốt lõi của ứng dụng.

---

## 1. Tổng Quan Kết Quả Đánh Giá
Mã nguồn hiện tại đã được tổ chức khá tốt sau đợt dọn dẹp (cleanup) hệ thống legacy:
- Đã loại bỏ hoàn toàn package cũ `com.example.uhf` khỏi source code.
- Đã loại bỏ các thư viện phụ thuộc không cần thiết (`poi`, `jxl`, `xUtils`).
- Đã tách biệt tốt tầng giao diện (View/Activity) và tầng xử lý logic (Controller/Repository).
- Các unit test được thiết lập đầy đủ và đang chạy thành công (`BUILD SUCCESSFUL`).

Tuy nhiên, hệ thống vẫn tồn tại **5 vấn đề kỹ thuật lớn** dưới đây cần được cải tiến để tăng độ ổn định, hiệu năng và khả năng bảo trì lâu dài.

---

## 2. Các Vấn Đề Chưa Tốt & Rủi Ro Chi Tiết

### Vấn đề 2.1: Mã nguồn dư thừa (Dead Code) sau khi Refactor
Trong các đợt refactor trước, nhiều logic xử lý đã được di chuyển từ Activity sang Controller tương ứng. Tuy nhiên, các phương thức private cũ ở lớp Activity vẫn chưa được xóa bỏ hoàn toàn, gây rối mã nguồn và tốn dung lượng bảo trì.

**Chi tiết các vùng mã dư thừa:**
- **[LookupActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/lookup/LookupActivity.java):** Tồn tại hàng loạt phương thức helper private không còn được gọi ở bất cứ đâu vì Activity đã chuyển sang gọi trực tiếp static/controller methods:
  - `validateHandoverForm(EditText, EditText)`
  - `hasHandoverChanges(Asset, String, String, String, String)`
  - `normalizeDepartmentForHandover(Asset, String)`
  - `normalizeLocationForHandover(Asset, String)`
  - `sanitizeNoteForMasterAsset(String)`
  - `looksLikeLegacyHandoverTrail(String)`
  - `mergeHandoverNote(Asset, String, String, String, String, String)`
  - `buildHandoverTrail(Asset, String, String, String, String, String)`
  - `buildHandoverCurrentSummary(Asset)`
  - `buildHandoverSummary(Asset)`
  - `assetSummaryForLog(Asset)`
  - `parseDateMillis(String)`
  - `formatDate(long)`
  - `todayDateString()`
  - `isValidTagDate(String)`
- **[InventoryActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/inventory/InventoryActivity.java):** 
  - Phương thức `getCurrentOperatorName()` và `normalize(String)` được khai báo nhưng hoàn toàn không được sử dụng.
- **[CheckoutActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/checkout/CheckoutActivity.java):**
  - Phương thức `describeTab(ScreenTab)` và `safe(String)` không được gọi ở bất kỳ đâu trong file.
- **[CheckoutCsvRepository.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/repository/CheckoutCsvRepository.java):**
  - Phương thức `isValidDateTime(String)` hoàn toàn không được sử dụng (chỉ có `isValidDate(String)` được dùng).

---

### Vấn đề 2.2: Rủi ro rò rỉ bộ nhớ (Memory Leak) và Quản lý luồng thô (Raw Thread)
Trong lớp [AssetRepository.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/repository/AssetRepository.java), phương thức `importAssetsFromCsv` đang tự khởi tạo luồng chạy nền bằng luồng thô thay vì sử dụng ExecutorService sẵn có:
```java
// Dòng 470 trong AssetRepository.java
new Thread(() -> {
    try {
        List<Asset> assets = importManager.importFromUri(context, uri);
        // ...
```

**Rủi ro gặp phải:**
1. **Memory Leak (Rò rỉ bộ nhớ):** Phương thức nhận tham số `Context context` (ở đây được truyền trực tiếp từ `InventoryActivity` hoặc `AssetsActivity` qua từ khóa `this`). Luồng thô chạy ẩn danh này sẽ giữ một tham chiếu ngầm đến Activity đó. Nếu người dùng xoay màn hình hoặc thoát khỏi màn hình trong khi file CSV dung lượng lớn đang được import, Activity cũ sẽ không thể bị Garbage Collector thu hồi, dẫn tới rò rỉ bộ nhớ nghiêm trọng.
2. **Thiếu quản lý luồng tập trung:** Việc tạo mới `Thread` liên tục thay vì đưa vào thread pool làm tăng hao phí CPU (thread creation overhead) và gây khó khăn khi cần cancel tác vụ chạy nền.

---

### Vấn đề 2.3: Chênh lệch hiệu năng và an toàn luồng với `SimpleDateFormat`
Ứng dụng sử dụng rất nhiều định dạng ngày tháng thông qua `SimpleDateFormat`. Tuy nhiên, lớp này có các vấn đề sau:
1. **Không an toàn đa luồng (Not Thread-Safe):** Để khắc phục điều này, dự án đang sử dụng khối đồng bộ `synchronized (DATE_FORMAT)`. Cách này gây nghẽn luồng (thread contention) nếu nhiều nơi gọi định dạng ngày tháng cùng lúc.
2. **Khởi tạo đối tượng liên tục (Object Churn):** Trong lớp [InventoryCheckinPayloadMapper.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/mapper/InventoryCheckinPayloadMapper.java), phương thức `formatApiTimestamp` được gọi lặp đi lặp lại trong vòng lặp chuyển đổi danh sách tài sản:
```java
static String formatApiTimestamp(long scannedAt) {
    if (scannedAt <= 0L) {
        return null;
    }
    // Tạo mới một SimpleDateFormat cho mỗi item trong vòng lặp!
    return new SimpleDateFormat(API_TIMESTAMP_PATTERN, Locale.US).format(new Date(scannedAt));
}
```
Nếu danh sách tài sản kiêm kê có hàng nghìn dòng, việc này sẽ tạo ra hàng nghìn object `SimpleDateFormat` và `Date` vô ích, làm kích hoạt Garbage Collector thường xuyên gây lag ứng dụng (GC overhead).

---

### Vấn đề 2.4: Quản lý callback bất đồng bộ thiếu an toàn vòng đời (Lifecycle Safety)
Các Activity (như `LookupActivity`, `InventoryActivity`) sử dụng các callback bất đồng bộ để nhận kết quả từ `AssetRepository` (như `loadCacheSnapshotAsync`). 
Mặc dù đã có các kiểm tra:
```java
if (isFinishing() || isDestroyed()) {
    return;
}
```
Nhưng những dòng code này nằm bên trong runnable được đẩy về main thread. Điều này có nghĩa là Activity vẫn bị tham chiếu mạnh bởi Runnable đó cho tới khi nó chạy xong trên Main Looper. Nếu Repository mất thời gian xử lý dài, luồng hoạt động của Activity đã tắt nhưng object vẫn tồn tại trong RAM.

---

### Vấn đề 2.5: Viết cứng chuỗi thông điệp tiếng Việt (Hardcoded String Literals)
Nhiều chuỗi hiển thị giao diện tiếng Việt vẫn được gõ trực tiếp trong file Java thay vì khai báo trong file tài nguyên `strings.xml`.
- **Ví dụ trong [LookupController.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/lookup/LookupController.java):**
  - Dòng 167: `"Tài sản chưa có trong hệ thống. Hãy nhập thông tin để thêm."`
  - Dòng 205: `"Phát hiện tài sản mới! Đã hiển thị mã quét và TID."`
  - Dòng 244: `"Nhập thông tin tài sản mới."`
  - Dòng 270: `"Đã hủy đăng ký."`
  - Dòng 286: `"Mã tài sản (Code) là bắt buộc"`
- **Ví dụ trong [InventoryActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/inventory/InventoryActivity.java):**
  - Dòng 959: `"Du liệu kiem ke dang khoi tao. Vui long thu lai sau."`

Việc này làm giảm tính thẩm mỹ của code và ngăn cản khả năng quốc tế hóa / đa ngôn ngữ sau này.

---

## 3. Hướng Dẫn Sửa Đổi & Mã Nguồn Đề Xuất (Code Recommendations)

### Giải pháp 3.1: Dọn dẹp mã nguồn dư thừa
Tiến hành xóa bỏ toàn bộ các phương thức private không sử dụng đã liệt kê tại mục 2.1 ở các file:
- `LookupActivity.java`
- `InventoryActivity.java`
- `CheckoutActivity.java`
- `CheckoutCsvRepository.java`

*Lưu ý: Sau khi xóa các phương thức này, chạy lại `./gradlew testDebugUnitTest` để chắc chắn không ảnh hưởng tới compilation.*

---

### Giải pháp 3.2: Khắc phục Memory Leak và Chuyển sang Thread Pool
Thay đổi phương thức `importAssetsFromCsv` trong [AssetRepository.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/repository/AssetRepository.java):
1. Chuyển sang sử dụng `syncExecutor` (ThreadPool 1 luồng đã khai báo sẵn trong lớp).
2. Lấy `ApplicationContext` thay vì Activity Context để tránh rò rỉ bộ nhớ.

**Trước khi sửa:**
```java
public void importAssetsFromCsv(final Context context, final Uri uri, final AssetRepositoryCallback callback) {
    ensureDiskCacheLoaded();
    new Thread(() -> {
        try {
            List<Asset> assets = importManager.importFromUri(context, uri);
            // ...
```

**Sau khi sửa (Đề xuất):**
```java
public void importAssetsFromCsv(final Context context, final Uri uri, final AssetRepositoryCallback callback) {
    ensureDiskCacheLoaded();
    // 1. Tránh rò rỉ bằng cách chuyển sang ApplicationContext
    final Context appContext = context != null ? context.getApplicationContext() : null;
    
    // 2. Chạy trên thread pool hiện có thay vì tạo luồng thô
    syncExecutor.execute(() -> {
        try {
            if (appContext == null) {
                throw new IllegalArgumentException("Context is null");
            }
            List<Asset> assets = importManager.importFromUri(appContext, uri);
            updateCache(assets, "CSV");
            logRepository.logInfo("IMPORT_FILE", "Da import danh sach tai san tu CSV", assets.size() + " asset(s)");
            dispatchSuccess(callback, assets, "Da import " + assets.size() + " tai san tu CSV.");
        } catch (IllegalArgumentException illegalArgumentException) {
            logRepository.logError("IMPORT_FILE", "Import CSV tai san that bai", illegalArgumentException.getMessage());
            dispatchError(callback, illegalArgumentException.getMessage());
        } catch (Exception exception) {
            logRepository.logError("IMPORT_FILE", "Khong the import CSV tai san", exception.getMessage());
            dispatchError(callback, "Khong the import CSV. Vui long kiem tra dinh dang file.");
        }
    });
}
```

---

### Giải pháp 3.3: Nâng cấp lên `java.time` (Java 8) để xử lý ngày tháng
Vì ứng dụng có `minSdk 26` và cài đặt java compile Java 8, chúng ta có thể sử dụng `java.time.format.DateTimeFormatter` (hoàn toàn thread-safe, hiệu năng rất cao) thay vì `SimpleDateFormat`.

**Ví dụ sửa đổi trong `InventoryCheckinPayloadMapper.java`:**

**Trước khi sửa (tạo object liên tục trong loop):**
```java
static String formatApiTimestamp(long scannedAt) {
    if (scannedAt <= 0L) {
        return null;
    }
    return new SimpleDateFormat(API_TIMESTAMP_PATTERN, Locale.US).format(new Date(scannedAt));
}
```

**Sau khi sửa (Đảm bảo hiệu năng & Thread-Safe):**
```java
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class InventoryCheckinPayloadMapper {
    private static final DateTimeFormatter API_TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);

    static String formatApiTimestamp(long scannedAt) {
        if (scannedAt <= 0L) {
            return null;
        }
        // Tránh tạo mới formatter, chạy cực kỳ nhanh và an toàn
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(scannedAt), ZoneId.systemDefault())
                .format(API_TIMESTAMP_FORMATTER);
    }
    // ...
}
```

Tương tự, có thể tối ưu hóa định dạng ngày tháng trong `CheckoutCsvRepository.java` và `LookupController.java` bằng cách định nghĩa tĩnh các `DateTimeFormatter`:
```java
public static final DateTimeFormatter EXPORT_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
public static final DateTimeFormatter CHECKOUT_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
```
Sau đó loại bỏ hoàn toàn các khối đồng bộ `synchronized` tốn chi phí.

---

### Giải pháp 3.4: Đưa chuỗi tiếng Việt vào tài nguyên XML
Khai báo toàn bộ các câu thông báo cứng vào file tài nguyên ngôn ngữ:
- `app/src/main/res/values/strings.xml` hoặc `strings_assets_lookup_logs.xml`.

Ví dụ thêm vào `strings.xml`:
```xml
<string name="lookup_status_not_found_in_system">Tài sản chưa có trong hệ thống. Hãy nhập thông tin để thêm.</string>
<string name="lookup_status_new_scanned_tag">Phát hiện tài sản mới! Đã hiển thị mã quét và TID.</string>
<string name="lookup_status_manual_add_title">Nhập thông tin tài sản mới.</string>
<string name="lookup_status_cancelled_registration">Đã hủy đăng ký.</string>
```

Trong code Java, thay vì truyền chuỗi trực tiếp, ta sẽ truy xuất qua `Context.getString(id)`. Ví dụ ở Controller:
```java
// Định nghĩa giao diện Callback giao tiếp tốt hơn
public interface LookupUi {
    // ...
    String lookupStatusNotFoundInSystem();
}
```
Tại Activity cài đặt phương thức này:
```java
@Override
public String lookupStatusNotFoundInSystem() {
    return getString(R.string.lookup_status_not_found_in_system);
}
```

---

## 4. Kế Hoạch Triển Khai An Toàn (Safe Execution Plan)

Để thực thi dọn dẹp và tối ưu hóa mà không gây ra bất cứ lỗi tiềm ẩn (regression) nào, đề xuất triển khai theo 3 bước nhỏ riêng biệt:

1. **Bước 1: Clean Dead Code & Chuẩn hóa Strings**
   - Xóa bỏ các hàm thừa đã chỉ ra ở mục 2.1.
   - Di chuyển các chuỗi hardcode sang file XML.
   - Chạy lệnh `./gradlew assembleDebug` và `./gradlew testDebugUnitTest` để xác nhận build thành công.
2. **Bước 2: Sửa đổi Đa luồng và Tránh Memory Leak**
   - Thay đổi hàm `importAssetsFromCsv` trong `AssetRepository` sang sử dụng Thread Pool và `ApplicationContext`.
   - Chạy thử tính năng import file CSV trên ứng dụng thực tế để xác nhận hoạt động bình thường.
3. **Bước 3: Tối ưu hóa xử lý Ngày tháng với `java.time`**
   - Thay thế `SimpleDateFormat` bằng `DateTimeFormatter` tĩnh trong các mapper, helper, repository.
   - Chạy bộ unit test để đảm bảo các bộ lọc, định dạng xuất CSV và API payload vẫn giữ nguyên chuỗi đầu ra.
