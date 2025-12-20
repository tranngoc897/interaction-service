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

