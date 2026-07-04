# Checklist Dọn Dẹp và Tối Ưu Hóa Mã Nguồn (UHF Asset Manager)

Tài liệu này là danh sách công việc (TODO) cần triển khai cho quá trình cải tiến chất lượng code của ứng dụng. Bạn có thể sử dụng file này để theo dõi tiến độ từng bước.

---

## 📋 Danh Sách Công Việc (TODO List)

### 🧹 Phase 1: Dọn Dẹp Mã Nguồn Dư Thừa (Dead Code)
*Mục tiêu: Loại bỏ các hàm không còn sử dụng để thu gọn các file UI nóng, đảm bảo an toàn biên dịch và runtime.*
- [x] Xóa các hàm private helper dư thừa trong [LookupActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/lookup/LookupActivity.java):
  - [x] `validateHandoverForm`
  - [x] `hasHandoverChanges`
  - [x] `normalizeDepartmentForHandover`
  - [x] `normalizeLocationForHandover`
  - [x] `sanitizeNoteForMasterAsset`
  - [x] `looksLikeLegacyHandoverTrail`
  - [x] `mergeHandoverNote`
  - [x] `buildHandoverTrail`
  - [x] `buildHandoverCurrentSummary`
  - [x] `buildHandoverSummary`
  - [x] `assetSummaryForLog`
  - [x] `parseDateMillis`
  - [x] `formatDate`
  - [x] `todayDateString`
  - [x] `isValidTagDate`
- [x] Xóa các hàm private helper dư thừa trong [InventoryActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/inventory/InventoryActivity.java):
  - [x] `getCurrentOperatorName`
  - [x] `normalize`
- [x] Xóa các hàm private helper dư thừa trong [CheckoutActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/checkout/CheckoutActivity.java):
  - [x] `describeTab`
  - [x] `safe`
- [x] Xóa hàm private helper dư thừa trong [CheckoutCsvRepository.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/repository/CheckoutCsvRepository.java):
  - [x] `isValidDateTime`
- [x] **Kiểm thử:** Chạy lệnh `./gradlew testDebugUnitTest` ở terminal để xác nhận việc xóa code thừa hoàn toàn không làm lỗi biên dịch và các unit test vẫn chạy thành công.

---

### 🧵 Phase 2: Chuẩn Hóa Đa Luồng & Tránh Rò Rỉ Bộ Nhớ (Memory Leak)
*Mục tiêu: Đưa các tác vụ nặng vào Thread Pool hiện có, loại bỏ luồng thô và tránh giữ tham chiếu mạnh đến Activity Context.*
- [ ] Sửa đổi hàm `importAssetsFromCsv` trong [AssetRepository.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/repository/AssetRepository.java):
  - [ ] Đổi từ `new Thread(...)` sang `syncExecutor.execute(...)`.
  - [ ] Sử dụng `context.getApplicationContext()` thay cho `context` trong quá trình import.
- [ ] **Kiểm thử:**
  - [ ] Build & cài đặt ứng dụng.
  - [ ] Thực hiện import CSV thử nghiệm trên màn hình kiểm kê / danh sách tài sản để đảm bảo chức năng này hoạt động ổn định.

---

### ⚡ Phase 3: Tối Ưu Hiệu Năng & Đồng Bộ Ngày Tháng (java.time)
*Mục tiêu: Thay thế SimpleDateFormat cũ và các khối synchronized bằng DateTimeFormatter bất biến và thread-safe.*
- [ ] Khai báo và sử dụng `DateTimeFormatter` tĩnh thay vì tạo mới `SimpleDateFormat` liên tục trong [InventoryCheckinPayloadMapper.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/mapper/InventoryCheckinPayloadMapper.java).
- [ ] Cập nhật định dạng ngày tháng trong [CheckoutCsvRepository.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/repository/CheckoutCsvRepository.java) sang `DateTimeFormatter` và gỡ bỏ các khối synchronized.
- [ ] Cập nhật định dạng ngày tháng trong [LookupController.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/lookup/LookupController.java) sang `DateTimeFormatter`.
- [ ] **Kiểm thử:**
  - [ ] Chạy lại toàn bộ unit test.
  - [ ] Kiểm tra các file CSV xuất ra từ checkout/checkin/kiểm kê để xác nhận định dạng ngày tháng vẫn chính xác như cũ.

---

### 🌐 Phase 4: Chuẩn Hóa Chuỗi Hiển Thị (String Resources)
*Mục tiêu: Loại bỏ hoàn toàn viết cứng chuỗi văn bản (Hardcoded Strings), phục vụ quốc tế hóa.*
- [ ] Di chuyển các chuỗi văn bản tiếng Việt viết cứng trong [LookupController.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/lookup/LookupController.java) và [InventoryActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/inventory/InventoryActivity.java) sang file tài nguyên `strings.xml`.
- [ ] Cập nhật các callback hiển thị giao diện để lấy thông tin từ tệp tài nguyên XML.
- [ ] **Kiểm thử:**
  - [ ] Chạy smoke test toàn bộ app.
  - [ ] Kiểm tra xem các thông báo lỗi và thông báo trạng thái có hiển thị đúng chính tả và định dạng trên thiết bị hay không.
