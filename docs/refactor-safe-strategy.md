# Quy Ước Refactor An Toàn

Tài liệu này chốt lại nguyên tắc refactor cho app Android Java hiện tại.

Mục tiêu:
- Giảm độ phình của các file nóng.
- Tách dần business logic khỏi Activity.
- Giữ nguyên giao diện, flow người dùng và behavior bên ngoài.

## Quy Ước Bắt Buộc

1. Không làm thay đổi giao diện.
2. Không thay đổi layout XML, view id, text hiển thị, flow thao tác người dùng nếu không thật sự bắt buộc.
3. Không đổi API contract hiện tại nếu chưa có lớp tương thích.
4. Không big-bang rewrite.
5. Không tự ý đổi sang Compose.
6. Không tự ý migrate toàn bộ sang MVVM/ViewModel nếu chưa được yêu cầu ở bước đó.
7. Chỉ refactor cấu trúc bên trong để code dễ bảo trì hơn.
8. Mọi thay đổi phải theo hướng giữ nguyên behavior cũ.
9. Nếu cần class mới thì ưu tiên tạo class mới rồi cho code cũ gọi qua facade/adapter, tránh sửa lan rộng.
10. Mỗi bước phải hạn chế phạm vi thay đổi.
11. Không xóa code cũ nếu chưa chắc phần mới đã thay thế an toàn.
12. Ưu tiên giữ `AssetRepository` làm facade tạm thời trong giai đoạn đầu.
13. Nếu gặp chỗ chưa chắc chắn, ưu tiên giữ nguyên behavior hiện tại thay vì tối ưu mạnh tay.
14. File nào chưa tới giai đoạn thì không đụng.
15. Mỗi bước đều phải có khả năng rollback.

## Chiến Lược Chung

Refactor theo hướng:
- ruột thay dần, vỏ giữ nguyên
- thêm lớp mới trước, chuyển logic dần sang lớp mới
- giữ nguyên entry point hiện tại của Activity và Repository
- chỉ mở rộng phạm vi sau khi bước trước đã ổn

Nguyên tắc triển khai:
- ưu tiên tách logic nội bộ, không thay màn hình
- không đổi tên public method đang được dùng rộng nếu chưa có lớp tương thích
- hạn chế sửa nhiều file trong một bước
- ưu tiên tạo facade/adaptor tạm thời hơn là sửa đồng loạt

## Vùng Nóng Hiện Tại

Các file cần ưu tiên refactor nhưng chưa làm cùng lúc:
- `app/src/main/java/com/idocean/asset/data/repository/AssetRepository.java`
- `app/src/main/java/com/idocean/asset/ui/checkout/CheckoutActivity.java`
- `app/src/main/java/com/idocean/asset/ui/lookup/LookupActivity.java`
- `app/src/main/java/com/idocean/asset/ui/inventory/InventoryActivity.java`
- `app/src/main/java/com/example/uhf/activity/DashboardActivity.java`

## Thứ Tự Refactor Đề Xuất

### Giai đoạn 1
- Dựng hàng rào an toàn.
- Thêm test đặc tả hành vi hiện có cho sync, update, handover, filter.
- Chưa thay đổi UI, chưa đổi flow.

### Giai đoạn 2
- Tách logic trong `AssetRepository` thành các lớp nhỏ hơn.
- Vẫn giữ `AssetRepository` làm facade.
- Ưu tiên tách:
  - sync
  - mutation update/handover
  - cache
  - filter/distinct values

### Giai đoạn 3
- Tách logic scanner và xử lý nghiệp vụ ra khỏi Activity.
- Mỗi màn chỉ refactor từng phần nhỏ.
- Entry point Activity giữ nguyên.

### Giai đoạn 4
- Chuẩn hóa các service nội bộ cho export/import/storage/permission.
- Không thay behavior ngoài.

### Giai đoạn 5
- Dọn package, naming, cleanup code cũ sau khi lớp mới đã ổn định.
- Không làm ở giai đoạn đầu.

## Quy Tắc Khi Tạo Class Mới

Khi cần tạo class mới:
- class mới phải có trách nhiệm hẹp
- không làm lộ thay đổi ra UI
- code cũ gọi qua facade hoặc helper mới
- tránh kéo thêm phụ thuộc chéo giữa các feature

Ví dụ phù hợp:
- `AssetSyncService`
- `AssetMutationService`
- `AssetCacheStore`
- `AssetFilterService`
- `LookupScannerController`

## Quy Tắc Báo Cáo Sau Mỗi Bước

Sau mỗi bước refactor phải báo rõ:
1. file đã sửa
2. file mới tạo
3. rủi ro có thể ảnh hưởng behavior
4. checklist test tay cần chạy

## Nguyên Tắc Ra Quyết Định

Nếu có 2 lựa chọn:
- lựa chọn A sạch hơn nhưng rủi ro cao
- lựa chọn B ít đẹp hơn nhưng giữ nguyên behavior tốt hơn

Thì ưu tiên lựa chọn B trong giai đoạn đầu.

## Điều Không Làm Ở Giai Đoạn Đầu

- không đổi UI tổng thể
- không đổi package hàng loạt
- không thay toàn bộ singleton bằng framework DI
- không chuyển toàn bộ app sang ViewModel
- không đổi toàn bộ storage strategy
- không rewrite scanner flow

## Mục Tiêu Kết Quả

Sau các bước refactor đầu:
- file nóng nhỏ lại
- logic dễ đọc hơn
- dễ test hơn
- rollback được theo từng bước
- người dùng không nhận ra app đã bị thay ruột bên trong
