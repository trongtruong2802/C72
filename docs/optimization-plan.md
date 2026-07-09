# Kế Hoạch Tối Ưu Hóa & Nâng Cấp Ứng Dụng (Optimization & Enhancement Plan)

Tài liệu này trình bày kế hoạch chi tiết từng bước để triển khai các cải tiến mã nguồn, tối ưu hóa hiệu năng bộ nhớ và tích hợp thêm các tính năng ngoại tuyến (offline) cho ứng dụng **UHF Asset Manager** nhằm nâng cao trải nghiệm sử dụng thực tế trên thiết bị C72.

---

## 1. Mục Tiêu & Nguyên Tắc Triển Khai

### Mục tiêu:
* Khắc phục triệt để các rủi ro về rò rỉ bộ nhớ (Memory Leak) và giật lag giao diện (Garbage Collector Overhead).
* Tăng tốc độ truy vấn cơ sở dữ liệu nội bộ bằng cơ chế index của SQLite (Room).
* Hỗ trợ làm việc tốt trong môi trường kho bãi không có sóng Internet thông qua cơ chế lưu hàng chờ đồng bộ ngoại tuyến.
* Bảo mật tối đa thông tin cấu hình API endpoint.

### Nguyên tắc bắt buộc:
* **Không làm vỡ luồng quét**: Giữ nguyên logic xử lý của đầu đọc RFID UHF và quét mã vạch Barcode 2D.
* **Không làm thay đổi giao diện người dùng**: Giữ nguyên thiết kế layout XML, các nút bấm và trải nghiệm hiện có của người dùng, ngoại trừ việc bổ sung các thông báo/chỉ báo tiến trình (nếu cần).
* **Triển khai an toàn**: Thực hiện theo từng Phase độc lập, chạy test đơn vị (`testDebugUnitTest`) và kiểm tra chạy thực tế trên máy C72 sau mỗi giai đoạn.

---

## 2. Lộ Trình Triển Khai Chi Tiết (Roadmap)

### Phase 1: Dọn dẹp Dead Code & Chuẩn hóa Tài nguyên tiếng Việt
* **Mục tiêu**: Làm sạch mã nguồn dư thừa, loại bỏ các chuỗi viết cứng trong Java.
* **Các bước thực hiện**:
  1. Xóa các hàm `private` không sử dụng ở màn hình Tra cứu, Kiểm kê, Checkout đã liệt kê trong báo cáo đánh giá.
  2. Tạo các thẻ string tương ứng trong `app/src/main/res/values/strings.xml` cho các chuỗi tiếng Việt.
  3. Cập nhật các Activity và Controller sử dụng `context.getString(R.string.id)` để lấy chuỗi hiển thị.
* **Cách kiểm chứng**: Chạy lệnh `./gradlew assembleDebug` để đảm bảo code biên dịch thành công.

---

### Phase 2: Nâng cấp Đa luồng và Xử lý Ngày tháng
* **Mục tiêu**: Thay thế luồng thô và định dạng cũ bằng các thư viện hiện đại để tối ưu hiệu năng và RAM.
* **Các bước thực hiện**:
  1. Cấu hình luồng import CSV tại [AssetRepository.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/data/repository/AssetRepository.java) chạy qua `syncExecutor` (Thread Pool) thay cho `new Thread`. Sử dụng `ApplicationContext` thay cho Activity Context để tránh rò rỉ bộ nhớ.
  2. Định nghĩa các hằng số `java.time.format.DateTimeFormatter` tĩnh thay thế cho các đối tượng `SimpleDateFormat` cục bộ tại các lớp Mapper và Helper.
  3. Bỏ toàn bộ các khối `synchronized` ngày tháng không còn cần thiết.
* **Cách kiểm chứng**: Chạy `./gradlew testDebugUnitTest` để đảm bảo logic phân tích và định dạng ngày tháng không bị sai lệch.

---

### Phase 3: Tích hợp Room Database thay cho Cache JSON tĩnh
* **Mục tiêu**: Chuyển đổi việc lưu cache từ file JSON sang SQLite thông qua Room để tăng tốc độ truy vấn tài sản.
* **Các bước thực hiện**:
  1. Khai báo các thư viện Room (`androidx.room:room-runtime`, `androidx.room:room-compiler`) vào `app/build.gradle`.
  2. Định nghĩa thực thể `AssetEntity` tương ứng với model `Asset` hiện tại.
  3. Xây dựng lớp truy xuất dữ liệu `AssetDao` hỗ trợ các truy vấn:
     - Tìm kiếm nhanh tài sản theo mã quét (`assetCode`) hoặc `TID` sử dụng cơ chế Index.
     - Lọc danh sách tài sản theo Phòng ban, Vị trí trực tiếp bằng lệnh SQL.
  4. Cập nhật `AssetDiskCacheStore` và `AssetRepository` chuyển sang sử dụng Room Database làm nguồn dữ liệu chính.
* **Cách kiểm chứng**: Đo thử nghiệm thời gian khởi chạy app và tốc độ hiển thị kết quả sau khi quét.

---

### Phase 4: Xây dựng Cơ chế Đồng bộ Ngoại tuyến (Offline Sync Queue)
* **Mục tiêu**: Giúp nhân viên kho thực hiện bàn giao hoặc cập nhật tài sản ngay cả khi không có kết nối mạng.
* **Các bước thực hiện**:
  1. Tạo bảng `PendingMutation` trong SQLite để lưu trữ danh sách các thao tác chờ đồng bộ (bao gồm payload, loại hành động: UPDATE hoặc HANDOVER).
  2. Khi người dùng thực hiện cập nhật/bàn giao tài sản:
     - Nếu có mạng: Thực hiện gửi API lên n8n như bình thường.
     - Nếu không có mạng: Lưu payload thao tác vào bảng `PendingMutation`, hiển thị thông báo "Đã lưu tạm thời ngoại tuyến".
  3. Sử dụng `WorkManager` của Android để định nghĩa tác vụ đồng bộ chạy ngầm (`SyncWorker`). Worker này sẽ tự động được kích hoạt bởi hệ điều hành khi thiết bị C72 kết nối mạng trở lại.
* **Cách kiểm chứng**: Ngắt kết nối Wi-Fi trên máy C72, thực hiện cập nhật thông tin tài sản, sau đó bật lại Wi-Fi và kiểm tra xem dữ liệu có tự động gửi lên Google Sheet/n8n hay không.

---

### Phase 5: Tối ưu Bảo mật API & Phân trang RecyclerView
* **Mục tiêu**: Giấu API URL để tránh dịch ngược ứng dụng và tăng độ mượt khi cuộn danh sách lớn.
* **Các bước thực hiện**:
  1. Di chuyển cấu hình `BASE_URL` của n8n ra file `local.properties` và đọc thông qua `buildConfigField` trong `app/build.gradle`.
  2. Bổ sung header xác thực `X-API-KEY` hoặc `Authorization` cho các yêu cầu qua Retrofit.
  3. Sử dụng phân trang ảo (Endless Scrolling) hoặc thư viện `Paging 3` cho danh sách tài sản trong `AssetsActivity`.
