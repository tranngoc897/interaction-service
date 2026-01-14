# External Tasks & Database Updates

Tài liệu này tổng hợp các tác động của External Tasks (Camunda Workers) lên cơ sở dữ liệu và hệ thống thông qua các cập nhật trực tiếp và các sự kiện (Events).

## 1. Cập Nhật Trực Tiếp (Direct Database Updates)

Các task này thực hiện thay đổi trạng thái trực tiếp lên các bảng chính (Entities) thông qua Repository.

| External Task | Bảng (Table/Entity) | Hành Động | Trạng Thái Mới | Chi Tiết |
| :--- | :--- | :--- | :--- | :--- |
| **`account-creation`** | `case_entity` | Update | **`APPROVED`** | Cập nhật trạng thái hồ sơ sau khi tạo tài khoản thành công. |
| **`email-notification`** | `process_mapping` | Update | **`COMPLETED`** | Đánh dấu sự kết nối giữa Case và Process là hoàn tất. |
| **`cleanup-data`** | *(Dự kiến `case_entity`)* | *(Pending)* | *(CANCELLED)* | Hiện tại code đang để `TODO`, chưa thực hiện update DB thực tế. |

## 2. Cập Nhật Gián Tiếp (Indirect Updates via Events)

Hầu hết các External Task không ghi trực tiếp vào DB nghiệp vụ mà bắn ra các **Events**. Các Events này thường được các Listeners hứng để ghi vào bảng lịch sử (`workflow_history`, `audit_log`, v.v.) hoặc gửi sang Kafka.

### Chi tiết các Events theo Task:

#### `data-validation`
*   **Event:** `InteractionStepEvent`
*   **Mục đích:** Ghi nhận bước validation đã hoàn thành.
*   **Dữ liệu:** `validationPassed=true`, `timestamp`.

#### `ocr-processing`
*   **Event:** `PerformanceMetricsEvent`
*   **Mục đích:** Ghi nhận hiệu năng xử lý OCR.
*   **Dữ liệu:** `durationMs`, `status=SUCCESS`.

#### `compliance-check`
*   **Event:** `ComplianceEvent`
*   **Mục đích:** Ghi nhận kết quả kiểm tra tuân thủ (AML/KYC).
*   **Dữ liệu:** `checkType=AML_CHECK`, `status=PASSED`.

#### `account-creation`
*   **Event 1:** `AccountCreatedEvent`
    *   **Mục đích:** Thông báo tài khoản mới đã được tạo.
    *   **Dữ liệu:** `accountNumber`, `customerName`, `accountType`.
*   **Event 2:** `CaseUpdateEvent`
    *   **Mục đích:** Audit log cho việc thay đổi thông tin Case.
    *   **Dữ liệu:** Thay đổi trạng thái sang Approved, số tài khoản mới.

#### `email-notification`
*   **Event:** `WorkflowStateEvent`
*   **Mục đích:** Đánh dấu workflow đã hoàn tất vòng đời.
*   **Dữ liệu:** `oldState=RUNNING`, `newState=COMPLETED`.

#### `cleanup-data`
*   **Event:** `SystemErrorEvent`
*   **Mục đích:** Ghi log lỗi hệ thống hoặc hủy bỏ quy trình.
*   **Dữ liệu:** `errorCode=GLOBAL_CANCEL`, `severity=INFO`.

## 3. Lưu Ý Quan Trọng (Notes)

1.  **Tính Nhất Quán (Consistency):** Việc cập nhật `case_entity` và `process_mapping` diễn ra trong các transaction riêng biệt của từng External Task.
2.  **TODO Items:**
    *   Task `cleanup-data` hiện tại chưa cập nhật status `CANCELLED` vào DB (đang comment code). Cần mở lại hoặc implement logic này nếu muốn trạng thái hủy được lưu lại.
    *   Task `cleanup-data` cũng có các TODO về việc xóa file tạm và cleanup resource chưa được implement.
