# Camunda Best Practices

Tài liệu này tổng hợp các hướng dẫn và thực hành tốt nhất (Best Practices) khi làm việc với Camunda BPM trong dự án này.

## 1. BPMN Modeling

### 1.1. Rõ ràng và Dễ hiểu (Readability)
*   **Đi từ trái sang phải:** Luồng quy trình nên đi theo hướng từ trái sang phải.
*   **Tránh "Spaghetti" flows:** Hạn chế các đường nối chéo nhau quá nhiều. Sử dụng các cổng (Gateways) một cách hợp lý.
*   **Đặt tên rõ ràng:**
    *   **Tasks:** Dùng động từ + danh từ (ví dụ: "Validate Data", "Send Email").
    *   **Events:** Dùng tính từ + danh từ hoặc quá khứ phân từ (ví dụ: "Order Received", "Payment Failed").
    *   **Gateways:** Dùng câu hỏi (ví dụ: "Is Valid?", "Approved?").

### 1.2. Scope & Granularity
*   **Đừng quá chi tiết:** Đừng cố gắng mô hình hóa mọi logic code vào BPMN. BPMN là để orchestrate (điều phối), không phải để implement chi tiết thuật toán.
*   **Sử dụng Call Activities:** Nếu quy trình quá phức tạp, hãy tách thành các subprocesses nhỏ hơn và tái sử dụng bằng Call Activity.

## 2. Implementation (Java & Spring Boot)

### 2.1. External Tasks vs Java Delegates
*   **Ưu tiên External Task (Long Polling):**
    *   Giúp tách biệt (decouple) worker khỏi engine.
    *   Dễ dàng scale độc lập (worker và engine có thể ở các pod khác nhau).
    *   Không làm treo Engine nếu worker xử lý lâu.
*   **Java Delegates:**
    *   Chỉ dùng cho các tác vụ rất nhanh, nội bộ (như tính toán đơn giản, transform biến).
    *   Luôn để `Stateful` bean (nếu dùng) cẩn thận về thread-safety. Tốt nhất là dùng `Stateless` beans.

### 2.2. Idempotency (Tính luỹ đẳng)
*   **Nguyên tắc:** Worker có thể bị gọi lại (retry) bất cứ lúc nào nếu có lỗi mạng hoặc timeout.
*   **Thực hành:**
    *   Luôn kiểm tra xem tác vụ đã được thực hiện trước đó chưa trước khi thực hiện ghi dữ liệu.
    *   Ví dụ: Kiểm tra `accountNumber` đã tồn tại trong `caseData` chưa trước khi tạo mới.

### 2.3. Asynchronous Continuation (Async Before/After)
*   **Tránh Transaction lớn:** Mặc định Camunda chạy các bước liên tiếp trong cùng 1 DB transaction. Nếu 1 bước lỗi, toàn bộ rollback.
*   **Async Before:** Đặt ở các điểm quan trọng (Save points) để ngắt transaction.
    *   Đặt trước các External Task.
    *   Đặt trước các bước gửi Email/Notification.
    *   Đặt sau các bước quan trọng cần lưu trạng thái ngay (ví dụ: sau khi User Task hoàn thành).

## 3. Data Handling (Process Variables)

### 3.1. Hạn chế kích thước biến
*   **Không lưu Object lớn:** Đừng lưu cả file PDF base64 hay JSON Object khổng lồ (vài MB) vào Process Variable.
*   **Lưu tham chiếu:** Thay vì lưu dữ liệu, hãy lưu **ID** hoặc **URL** trỏ đến dữ liệu đó được lưu ở DB/S3 của service khác (ví dụ: `caseId`, `documentUrl`).

### 3.2. Scope của biến
*   **Local Variables:** Sử dụng biến cục bộ (Local Scope) khi biến đó chỉ cần thiết cho một đoạn ngắn hoặc trong Subprocess, để tránh làm "rác" scope global.

## 4. Error Handling

### 4.1. Business Error vs Technical Error
*   **Technical Error (Lỗi kỹ thuật):** DB down, Network timeout.
    *   Sử dụng cơ chế **Retry** (cấu hình `R3/PT1M` - retry 3 lần, mỗi lần cách nhau 1 phút).
    *   Nếu hết retry -> Tạo **Incident** để Admin xử lý.
*   **Business Error (Lỗi nghiệp vụ):** Validation failed, Customer rejected.
    *   Sử dụng **BpmnError** (`throw new BpmnError("CODE")`).
    *   Bắt lỗi bằng **Error Boundary Event** trong BPMN để rẽ nhánh sang luồng xử lý lỗi (ví dụ: Gửi thông báo từ chối -> End Process).

## 5. Versioning & Migration

*   **Versioning:** Khi deploy BPMN mới, Camunda tự tạo version mới. Process đang chạy (Running instances) sẽ tiếp tục chạy ở version cũ.
*   **Tương thích ngược:** Tránh xóa các biến hoặc thay đổi tên biến mà version cũ đang cần.
*   **Process Migration:** Chỉ migrate các running instances sang version mới khi thực sự cần thiết và đã test kỹ mapping giữa các activities.

## 6. Testing

*   **Unit Test Coverage:** Sử dụng `camunda-bpm-assert` để viết unit test cho luồng BPMN.
*   **Kịch bản Test:**
    *   Happy Path (Luồng đi thẳng).
    *   Gateways (Test tất cả các nhánh rẽ).
    *   Error Path (Test các trường hợp BpmnError và Retry).

## 7. Security

*   **Data Masking:** Masking dữ liệu nhạy cảm (PII) trong log của Worker/Delegate.
*   **Authorization:** Phân quyền ai được start process, ai được claim user task nào.

## 8. Production Deployment & Operations

### 8.1. Database Configuration
*   **Isolation Level:** Đảm bảo Database transaction isolation level là **READ COMMITTED**. Camunda được thiết kế tối ưu cho mức này. Tránh dùng `REPEATABLE READ` hoặc `SERIALIZABLE` vì dễ gây Deadlock cho Camunda Engine.
*   **Connection Pool:** Cấu hình HikariCP hợp lý. Số lượng connection thường phải lớn hơn `max-jobs-per-acquisition` của Job Executor.

### 8.2. High Availability & Scaling
*   **Stateless Engine:** Camunda Engine (Spring Boot) nên chạy Stateless. Trạng thái được lưu hoàn toàn ở Database.
*   **Clustering:** Có thể chạy nhiều node Camunda (Replicas) trỏ vào **cùng một Database**.
    *   **Lưu ý:** Cấu hình `camunda.bpm.job-execution.deployment-aware=true` nếu ứng dụng của bạn là Heterogeneous (nhiều cụm engine chạy các process khác nhau). Với Homogeneous cluster (tất cả node giống hệt nhau), có thể để `false`.

### 8.3. Job Executor Tuning
*   **Max Jobs Per Acquisition:** Số lượng job mà executor lấy mỗi lần poll DB. Mặc định là 3. Trong Production high-load, có thể tăng lên (ví dụ: 10-20) nhưng cần theo dõi độ trễ DB.
*   **Wait Time:** Cấu hình `max-wait` và `wait-increase-factor` để cân bằng giữa độ trễ xử lý và tải lên DB khi hệ thống rảnh (idle).

### 8.4. Monitoring & Alerting
*   **Camunda Cockpit (Enterprise/Community):** Dùng để xử lý sự cố (Incidents) thủ công.
*   **Prometheus & Grafana:** Cài đặt `camunda-bpm-spring-boot-starter-actuator` để expose metrics (`/actuator/prometheus`).
    *   **Metrics cần theo dõi:**
        *   Số lượng Incidents đang mở.
        *   Job Acquisition rejected (Pool size quá nhỏ?).
        *   Process Instance duration (Thời gian chạy trung bình).

### 8.5. History & Housekeeping (Cleanup)
*   **History Level:** Trong Production, cân nhắc mức `ACTIVITY` (ghi nhận Start/End của từng Activity) thay vì `FULL` (ghi nhận mọi thay đổi biến) để giảm tải DB và dung lượng lưu trữ.
*   **History Cleanup Strategy:** BẬT tính năng **History Cleanup** của Camunda.
    *   Cấu hình `History Time To Live (TTL)` cho từng Process Definition (ví dụ: 30 ngày).
    *   Camunda sẽ tự động xóa các historical data hết hạn theo batch job chạy ngầm (thường cấu hình chạy vào ban đêm).

### 8.6. Backup & Recovery
*   **Database Backup:** Chiến lược backup DB định kỳ là quan trọng nhất vì toàn bộ State nằm ở đó.
*   **Export/Import:** Không dùng Export/Import data của Cockpit để backup system. Hãy dùng dump data của Database (PostgreSQL/MySQL dump).
