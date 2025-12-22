- flw_int_def & flw_case_def = design-time config (định nghĩa luồng & schema).
- flw_int & flw_case = runtime state (thực thi journey + dữ liệu khách hàng).

- Interaction Service sẽ load định nghĩa, tạo instance,
  gắn case, trigger workflow khi tới step hệ thống.

- Điểm mấu chốt
- FE chỉ cần biết interactionId + stepName + case snapshot → không quan tâm logic backend phức tạp.

Cách liên kết

1. Interaction Service tạo interaction + case vào DB.
2. FE submit step → Interaction Service gọi Temporal Workflow với caseId, interactionId.
3. Workflow Engine chạy các activities → khi xong gọi ngược lại Interaction Service (/status).
4. Interaction Service update lại DB (flw_int.status) → FE query thấy kết quả.

Luồng sử dụng

1. FE gọi Interaction Service → Interaction Service tạo interaction và case.
2. Interaction Service khởi chạy workflow instance trên engine → nhận lại process_instance_id.
3. Lưu record vào flw_process_mapping (link giữa case_id, user_id và process_instance_id).
5. FE query Interaction Service → Service trả status từ flw_process_mapping + flw_int.

Flow tổng quan

FE → Interaction Service: gọi API start onboarding.
Interaction Service:
    - Tạo interaction + case trong DB.
    - Gọi Temporal Workflow (WorkflowClient.start) → nhận workflowId (chính là process_instance_id).
    - Lưu vào bảng flw_process_mapping với status RUNNING.
    - 
Workflow Engine (Temporal) chạy các step backend.
Workflow Engine gọi activity notifyInteraction() → callback API Interaction Service để update status.
Interaction Service update flw_process_mapping và flw_int.
FE query API Interaction Service để biết status.



Về mặt nghiệp vụ (Business Logic), để một hệ thống orchestration/interaction như thế này thực sự sẵn sàng cho production, bạn nên cân nhắc bổ sung các tính năng "xương sống" sau đây:

1. Cơ chế Idempotency (Chống trùng lặp)
Trong môi trường production, network có thể chập chờn dẫn đến việc Client gửi một request (ví dụ 
submitStep
) nhiều lần.

Cần thêm: Một idempotency-key cho mỗi request submit step. Hệ thống cần kiểm tra nếu key này đã được xử lý thì trả về kết quả cũ thay vì thực hiện lại logic (đặc biệt quan trọng nếu bước đó có gọi sang các service thanh toán hoặc gửi OTP).
2. Quản lý Phiên (Concurrency Control)
Hiện tại nếu hai request cùng update một 
Case
 hoặc 
Interaction
 cùng lúc, bạn có thể bị mất dữ liệu (Lost Update).

Cần thêm: Optimistic Locking. Thêm cột version vào các Entity (
CaseEntity
, 
InteractionEntity
) và sử dụng @Version của JPA để đảm bảo không có hai tiến trình ghi đè lên nhau.
3. Cơ chế "Human-in-the-loop" & Task Management
Các hệ thống interaction thường có những bước cần con người (Admin/Operator) phê duyệt nếu automation thất bại (ví dụ: OCR không đọc được CMND).

Cần thêm: Một trạng thái AWAITING_REVIEWS và logic để push task cho một nhóm người dùng cụ thể. Hiện tại code của bạn đang tập trung vào User tự làm (Self-service).
4. Quản lý SLA (Service Level Agreement)
Trong thực tế, bạn cần biết một Interaction bị "ngâm" ở một bước quá lâu.

Cần thêm: Logic check expiry_time cho mỗi bước. Nếu quá 24h khách hàng không làm tiếp, hệ thống nên tự động CANCEL hoặc gửi Notification nhắc nhở.
5. Data Masking & Security (Bảo mật dữ liệu)
Bạn đang lưu toàn bộ dữ liệu vào cột case_data (JSONB).

Cần thêm: Một lớp lọc (Filter) để không trả về các thông tin nhạy cảm (như số CVV, mật khẩu, hoặc dữ liệu PII không cần thiết) trong API response cho Frontend.
Audit Log: Lưu vết chi tiết ai đã xem/sửa dữ liệu gì (không chỉ là history của workflow mà là log truy cập dữ liệu).
6. Versioning cho Interaction Definition
Khi bạn thay đổi quy trình (thêm/bớt bước), các Interaction đang chạy dở (In-flight) sẽ như thế nào?

Cần thêm: Logic map chính xác phiên bản definition tại thời điểm start. Tránh việc update code xong làm crash các interaction cũ đang chạy.
7. Phân quyền theo Resource (RBAC/ABAC)
Hiện tại controller đang nhận userId làm path variable.

Cần thêm: Kiểm tra quyền sở hữu. Đảm bảo currentUser (từ Token) thực sự có quyền thao tác trên caseId hoặc interactionId đó.
Lời khuyên: Nếu bạn muốn đi tiếp, tôi đề xuất chúng ta bắt đầu bằng việc thêm Optimistic Locking (Version) và Idempotency vì đây là hai lỗi phổ biến nhất và gây hậu quả nghiêm trọng nhất khi chạy production.

Bạn có muốn tôi giúp triển khai Optimistic Locking cho các Entity trước không?