# Kế Hoạch Cải Tiến Bộ Lọc Và Tiến Trình Đồng Bộ Dashboard

Bản kế hoạch này thực hiện cải tiến hai khu vực quan trọng của Dashboard:
1. **Nút xóa nhanh bộ lọc (Mục 4)**: Tích hợp nút hủy/xóa nhanh (`clear text`) trực tiếp trên từng ô nhập bộ lọc (Phòng ban, Vị trí, Loại tài sản) của `TextInputLayout`.
2. **Thanh tiến trình đồng bộ (Mục 5)**: Nâng cấp LinearProgressIndicator trực quan hơn (độ dày 6dp, bo tròn) kết hợp cùng text phần trăm hiển thị động (ví dụ: `45%`) khi đang chạy đồng bộ.

---

## Các Tệp Thay Đổi

### [Component: Dashboard UI]

#### [MODIFY] [activity_dashboard.xml](file:///e:/design/uhf-truong/app/src/main/res/layout/activity_dashboard.xml)
- Cập nhật 3 `TextInputLayout` chứa bộ lọc bằng cách thêm ID và cấu hình End Icon xóa nhanh:
  - Thêm ID: `@+id/layoutDashboardDepartmentFilter`, `@+id/layoutDashboardLocationFilter`, `@+id/layoutDashboardAssetTypeFilter`.
  - Cấu hình End Icon xóa: `app:endIconMode="custom"`, `app:endIconDrawable="@android:drawable/ic_menu_close_clear_cancel"`, `app:endIconTint="@color/dashboard_muted"`.
- Thiết kế lại khu vực hiển thị tiến trình đồng bộ trong Hero Card:
  - Đưa `tvDashboardPreviewCount` (màu trắng) và `tvDashboardSyncPercentage` (hiển thị phần trăm đồng bộ, màu xanh lá cây đậm `#59C13F`) vào một `LinearLayout` nằm ngang nằm phía trên thanh progress.
  - Thay đổi thuộc tính của `progressDashboardSync`: Tăng độ dày lên `6dp` (`app:trackThickness="6dp"`), bo tròn góc `3dp` (`app:trackCornerRadius="3dp"`), đổi màu indicator thành màu xanh lá cây `#59C13F` và đổi màu track nền thành màu trắng mờ `#40FFFFFF` để tương phản rõ ràng trên nền gradient tối.
  - Đổi màu chữ của `tvDashboardSyncProgress` thành màu trắng mờ `#CCFFFFFF` giúp tăng độ tương phản dễ đọc.

#### [MODIFY] [DashboardActivity.java](file:///e:/design/uhf-truong/app/src/main/java/com/idocean/asset/ui/dashboard/DashboardActivity.java)
- Khai báo các biến `TextInputLayout` mới cho 3 bộ lọc và `TextView tvSyncPercentage`.
- Liên kết (bind) các view mới trong hàm `bindViews()`.
- Thiết lập sự kiện click end icon của từng `TextInputLayout` trong `bindActions()` để xóa riêng từng bộ lọc đó khi người dùng nhấn nút "X".
- Cập nhật logic hiển thị/ẩn end icon xóa nhanh dựa trên việc bộ lọc có đang được chọn hay không trong hàm `updateFilterTexts()`.
- Trong hàm `renderDashboardFromState()`, thêm logic tính toán phần trăm hoàn thành khi đang đồng bộ: `percent = (loadedCount * 100) / totalCount` và cập nhật hiển thị lên `tvSyncPercentage`.

---

## Kế Hoạch Xác Minh (Verification Plan)

### Xác minh thủ công
1. **Kiểm tra chức năng bộ lọc**:
   - Chọn một vị trí (hoặc phòng ban) bất kỳ.
   - Kiểm tra xem nút **"X"** (Clear) có xuất hiện ở bên phải ô bộ lọc hay không.
   - Nhấn nút **"X"** đó và xác nhận bộ lọc được xóa ngay lập tức mà không ảnh hưởng tới các bộ lọc khác.
2. **Kiểm tra tiến trình đồng bộ**:
   - Nhấn nút "Đồng bộ bộ lọc" hoặc "Tải tất cả".
   - Xác nhận thanh tiến trình xuất hiện dày dặn, có màu xanh nổi bật và có chỉ số phần trăm hiển thị chạy cùng (từ `0%` đến `100%`).
   - Xác nhận text trạng thái hiển thị màu trắng rõ ràng, dễ đọc trên nền gradient.
