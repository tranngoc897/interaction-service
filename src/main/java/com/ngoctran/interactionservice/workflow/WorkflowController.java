package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.workflow.onboarding.KYCOnboardingWorkflow;
import com.ngoctran.interactionservice.workflow.reconciliation.ReconciliationWorkflow;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Temporal Workflow operations
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkflowController.class);
    private final TemporalWorkflowService workflowService;

    /**
     * Start KYC Onboarding Workflow
     */
    @PostMapping("/kyc/start")
    public ResponseEntity<WorkflowStartResponse> startKYCWorkflow(@RequestBody KYCStartRequest request) {
        log.info("Starting KYC onboarding for case: {}", request.getCaseId());
        String processInstanceId = workflowService.startKYCOnboardingWorkflow(
                request.getCaseId(),
                request.getInteractionId(),
                request.getUserId(),
                request.getInitialData());
        return ResponseEntity.ok(new WorkflowStartResponse(
                processInstanceId,
                "kyc-onboarding-" + request.getCaseId(),
                "RUNNING"));
    }

    /**
     * Signal: Documents uploaded
     */
    @PostMapping("/{workflowId}/signal/documents")
    public ResponseEntity<Void> signalDocumentsUploaded(
            @PathVariable String workflowId,
            @RequestBody Map<String, String> documents) {

        log.info("Signaling documents uploaded to onboarding: {}", workflowId);
        workflowService.signalDocumentsUploaded(workflowId, documents);
        return ResponseEntity.ok().build();
    }

    /**
     * Signal: User data updated
     */
    @PostMapping("/{workflowId}/signal/user-data")
    public ResponseEntity<Void> signalUserDataUpdated(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> updatedData) {

        log.info("Signaling user data updated to onboarding: {}", workflowId);
        workflowService.signalUserDataUpdated(workflowId, updatedData);
        return ResponseEntity.ok().build();
    }

    /**
     * Signal: Manual review
     */
    @PostMapping("/{workflowId}/signal/manual-review")
    public ResponseEntity<Void> signalManualReview(
            @PathVariable String workflowId,
            @RequestBody ManualReviewRequest request) {

        log.info("Signaling manual review to onboarding: {}, approved={}", workflowId, request.isApproved());
        workflowService.signalManualReview(workflowId, request.isApproved(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{workflowId}/status")
    public ResponseEntity<WorkflowStatusResponse> getWorkflowStatus(
            @PathVariable String workflowId) {

        log.info("Getting onboarding status: {}", workflowId);
        String status = workflowService.queryWorkflowStatus(workflowId);
        return ResponseEntity.ok(new WorkflowStatusResponse(workflowId, status));
    }

    /**
     * Query onboarding progress
     */
    @GetMapping("/{workflowId}/progress")
    public ResponseEntity<KYCOnboardingWorkflow.WorkflowProgress> getWorkflowProgress(
            @PathVariable String workflowId) {
        log.info("Getting onboarding progress: {}", workflowId);
        KYCOnboardingWorkflow.WorkflowProgress progress = workflowService.queryWorkflowProgress(workflowId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Cancel onboarding
     */
    @PostMapping("/{workflowId}/cancel")
    public ResponseEntity<Void> cancelWorkflow(@PathVariable String workflowId) {
        log.info("Cancelling onboarding: {}", workflowId);
        workflowService.cancelWorkflow(workflowId);
        return ResponseEntity.ok().build();
    }

    /**
     * Start Advanced Dynamic Pipeline
     */
    @PostMapping("/pipeline/run")
    public ResponseEntity<WorkflowStartResponse> startPipeline(@RequestBody PipelineRunRequest request) {
        log.info("Starting advanced pipeline: {}", request.getPipelineName());
        String workflowId = workflowService.startAdvancedPipeline(
                request.getPipelineName(),
                request.getTasks(),
                request.getData());

        return ResponseEntity.ok(new WorkflowStartResponse(
                workflowId,
                workflowId,
                "RUNNING"));
    }

    /**
     * Create/Update Onboarding Monitor Schedule
     */
    @PostMapping("/schedules/onboarding")
    public ResponseEntity<String> createOnboardingSchedule(@RequestBody OnboardingScheduleRequest request) {
        log.info("Request to create onboarding schedule: {}", request.getScheduleId());
        workflowService.createOnboardingMonitorSchedule(request.getScheduleId(), request.getCron());
        return ResponseEntity.ok("Onboarding schedule created/updated successfully");
    }

    /**
     * Create/Update Payment Processing Schedule
     */
    @PostMapping("/schedules/payment")
    public ResponseEntity<String> createPaymentSchedule(@RequestBody OnboardingScheduleRequest request) {
        log.info("Request to create payment processing schedule: {}", request.getScheduleId());
        workflowService.createPaymentProcessingSchedule(request.getScheduleId(), request.getCron());
        return ResponseEntity.ok("Payment processing schedule created/updated successfully");
    }

    /**
     * Get all schedules
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules() {
        log.info("Request to get all schedules");
        List<ScheduleEntity> schedules = workflowService.getAllSchedules();
        List<ScheduleResponse> responses = schedules.stream()
                .map(this::convertToScheduleResponse)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get schedule by ID
     */
    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable String scheduleId) {
        log.info("Request to get schedule: {}", scheduleId);
        ScheduleEntity schedule = workflowService.getScheduleById(scheduleId);
        return ResponseEntity.ok(convertToScheduleResponse(schedule));
    }

    /**
     * Delete/Pause schedule
     */
    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<String> deleteSchedule(@PathVariable String scheduleId) {
        log.info("Request to delete schedule: {}", scheduleId);
        workflowService.deleteSchedule(scheduleId);
        return ResponseEntity.ok("Schedule deleted successfully");
    }

    /**
     * Stop payment monitoring workflow
     */
    @PostMapping("/workflows/{workflowId}/stop-monitoring")
    public ResponseEntity<String> stopPaymentMonitoring(@PathVariable String workflowId) {
        log.info("Request to stop payment monitoring workflow: {}", workflowId);
        workflowService.stopPaymentMonitoring(workflowId);
        return ResponseEntity.ok("Payment monitoring stopped successfully");
    }

    /**
     * Start Payment Workflow Manually (for testing)
     */
    @PostMapping("/workflows/payment/start")
    public ResponseEntity<WorkflowStartResponse> startPaymentWorkflow(@RequestBody PaymentStartRequest request) {
        log.info("Starting payment workflow manually: {}", request.getPaymentId());
        String workflowId = workflowService.startPaymentWorkflow(
                request.getPaymentId(),
                request.getAccountId(),
                request.getAmount(),
                request.getCurrency(),
                request.getTenantId(),
                request.getUserId());
        return ResponseEntity.ok(new WorkflowStartResponse(
                request.getPaymentId(),
                workflowId,
                "RUNNING"));
    }

    /**
     * Submit Payment with Priority (Advanced Feature)
     */
    @PostMapping("/workflows/payment/submit-priority")
    public ResponseEntity<String> submitPaymentWithPriority(@RequestBody PriorityPaymentRequest request) {
        log.info("Submitting payment with priority: {} - {}", request.getPaymentId(), request.getPriority());
        // In real implementation, this would integrate with PaymentSchedulerAdvanced
        return ResponseEntity.ok("Payment submitted with priority: " + request.getPriority());
    }

    /**
     * Process Batch Payments (Advanced Feature)
     */
    @PostMapping("/workflows/payment/process-batch")
    public ResponseEntity<BatchProcessingResponse> processBatchPayments(@RequestBody BatchPaymentRequest request) {
        log.info("Processing batch payments: {} items", request.getPaymentIds().size());
        // In real implementation, this would use PaymentSchedulerAdvanced.processBatchOptimized()
        BatchProcessingResponse response = new BatchProcessingResponse(
                request.getPaymentIds().size(),
                request.getPaymentIds().size(), // Assume all success for demo
                0,
                2500L,
                new ArrayList<>());
        return ResponseEntity.ok(response);
    }

    /**
     * Get Dead Letter Queue Status (Advanced Feature)
     */
    @GetMapping("/workflows/dlq/status")
    public ResponseEntity<DLQStatusResponse> getDLQStatus() {
        log.info("Getting DLQ status");
        // In real implementation, this would query PaymentSchedulerAdvanced
        DLQStatusResponse response = new DLQStatusResponse(0, new ArrayList<>());
        return ResponseEntity.ok(response);
    }

    /**
     * Get Advanced Scheduler Metrics
     */
    @GetMapping("/workflows/advanced/metrics")
    public ResponseEntity<AdvancedMetricsResponse> getAdvancedMetrics() {
        log.info("Getting advanced scheduler metrics");
        // In real implementation, this would aggregate metrics from PaymentSchedulerAdvanced
        AdvancedMetricsResponse response = new AdvancedMetricsResponse(
                Map.of("rateLimiter", "ACTIVE", "circuitBreaker", "CLOSED"),
                Map.of("totalWorkers", 3, "overloadedWorkers", 0),
                Map.of("eventStore", 150, "auditTrail", 150));
        return ResponseEntity.ok(response);
    }

    // ==================== RECONCILIATION WORKFLOW ENDPOINTS ====================

    /**
     * Start Payment Reconciliation Workflow
     */
    @PostMapping("/reconciliation/start")
    public ResponseEntity<WorkflowStartResponse> startReconciliationWorkflow(@RequestBody ReconciliationStartRequest request) {
        log.info("Starting payment reconciliation for date: {}, type: {}", request.getDate(), request.getType());
        String processInstanceId = workflowService.startReconciliationWorkflow(
                request.getReconciliationId(), request.getDate(), request.getType(), request.getConfig());
        return ResponseEntity.ok(new WorkflowStartResponse(
                request.getReconciliationId(),
                "reconciliation-" + request.getReconciliationId(),
                "RUNNING"));
    }

    /**
     * Get Reconciliation Status
     */
/*    @GetMapping("/reconciliation/{workflowId}/status")
    public ResponseEntity<WorkflowStatusResponse> getReconciliationStatus(@PathVariable String workflowId) {
        log.info("Getting reconciliation status: {}", workflowId);
        ReconciliationWorkflow workflow = workflowService.queryReconciliationProgress(workflowId);
        return ResponseEntity.ok(new WorkflowStatusResponse(workflowId, workflow.getCurrentPhase()));
    }*/

    /**
     * Get Reconciliation Progress
     */
    @GetMapping("/reconciliation/{workflowId}/progress")
    public ResponseEntity<ReconciliationWorkflow.ReconciliationProgress> getReconciliationProgress(@PathVariable String workflowId) {
        log.info("Getting reconciliation progress: {}", workflowId);
        ReconciliationWorkflow.ReconciliationProgress progress = workflowService.queryReconciliationProgress(workflowId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Get Reconciliation Statistics
     */
    @GetMapping("/reconciliation/{workflowId}/stats")
    public ResponseEntity<ReconciliationWorkflow.ReconciliationStats> getReconciliationStats(@PathVariable String workflowId) {
        log.info("Getting reconciliation statistics: {}", workflowId);
        ReconciliationWorkflow.ReconciliationStats stats = workflowService.queryReconciliationStats(workflowId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Signal Manual Resolution for Discrepancy
     */
    @PostMapping("/reconciliation/{workflowId}/resolve")
    public ResponseEntity<Void> resolveDiscrepancy(@PathVariable String workflowId,
                                                  @RequestBody ManualResolutionRequest request) {
        log.info("Signaling manual resolution for discrepancy: {} in workflow: {}",
                request.getDiscrepancyId(), workflowId);

        Map<String, Object> signalData = Map.of(
                "discrepancyId", request.getDiscrepancyId(),
                "resolution", Map.of(
                        "resolution", request.getResolution(),
                        "notes", request.getNotes(),
                        "resolvedBy", request.getResolvedBy()
                )
        );

        workflowService.signalReconciliationWorkflow(workflowId, "manual_resolution", signalData);
        return ResponseEntity.ok().build();
    }

    /**
     * Signal Additional Data Received
     */
    @PostMapping("/reconciliation/{workflowId}/additional-data")
    public ResponseEntity<Void> addAdditionalData(@PathVariable String workflowId,
                                                @RequestBody AdditionalDataRequest request) {
        log.info("Signaling additional data received for source: {} in workflow: {}",
                request.getSourceId(), workflowId);

        workflowService.signalReconciliationWorkflow(workflowId, "additional_data",
                Map.of("sourceId", request.getSourceId(), "data", request.getData()));
        return ResponseEntity.ok().build();
    }



    // ==================== WORKFLOW HISTORY & ANALYTICS ====================

    /**
     * Get Workflow History
     */
    @GetMapping("/{workflowId}/history")
    public ResponseEntity<List<WorkflowHistoryEntity>> getWorkflowHistory(@PathVariable String workflowId) {
        log.info("Getting workflow history for: {}", workflowId);
        List<WorkflowHistoryEntity> history = workflowService.getWorkflowHistory(workflowId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get Workflow Statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getWorkflowStatistics(
            @RequestParam(required = false) String workflowType,
            @RequestParam(required = false, defaultValue = "LAST_30_DAYS") String dateRange) {

        log.info("Getting workflow statistics: type={}, dateRange={}", workflowType, dateRange);
        Map<String, Object> stats = workflowService.getWorkflowStatistics(workflowType, dateRange);
        return ResponseEntity.ok(stats);
    }

    // ==================== WORKFLOW SCHEDULING ENDPOINTS ====================

    /**
     * Schedule Workflow Execution
     */
    @PostMapping("/schedule")
    public ResponseEntity<WorkflowScheduleResponse> scheduleWorkflow(@RequestBody WorkflowScheduleRequest request) {
        log.info("Scheduling workflow: {} with schedule type: {}", request.getWorkflowType(), request.getScheduleType());

        String scheduledWorkflowId = workflowService.scheduleWorkflow(request.getWorkflowType(),
                convertToScheduleConfig(request));

        return ResponseEntity.ok(WorkflowScheduleResponse.builder()
                .scheduledWorkflowId(scheduledWorkflowId)
                .status("SCHEDULED")
                .build());
    }

    /**
     * List Scheduled Workflows
     */
    @GetMapping("/scheduled")
    public ResponseEntity<List<WorkflowSchedulerService.ScheduledWorkflowSummary>> getScheduledWorkflows(
            @RequestParam(required = false) String workflowType) {

        log.info("Getting scheduled workflows: type={}", workflowType);
        // In real implementation, inject WorkflowSchedulerService
        // List<WorkflowSchedulerService.ScheduledWorkflowSummary> schedules =
        //         workflowSchedulerService.listScheduledWorkflows(workflowType);

        // Mock response for demo
        List<WorkflowSchedulerService.ScheduledWorkflowSummary> mockSchedules = List.of(
                WorkflowSchedulerService.ScheduledWorkflowSummary.builder()
                        .scheduledWorkflowId("SCH-RECONCILIATION-EOD")
                        .workflowType("RECONCILIATION")
                        .cronExpression("0 0 18 * * ?")
                        .status("ACTIVE")
                        .description("End of day payment reconciliation")
                        .build()
        );

        return ResponseEntity.ok(mockSchedules);
    }

    // ==================== HELPER METHODS ====================

    private WorkflowSchedulerService.WorkflowScheduleConfig convertToScheduleConfig(WorkflowScheduleRequest request) {
        return WorkflowSchedulerService.WorkflowScheduleConfig.builder()
                .scheduleType(request.getScheduleType())
                .executionDate(request.getExecutionDate())
                .executionTime(request.getExecutionTime())
                .dayOfMonth(request.getDayOfMonth())
                .daysOfWeek(request.getDaysOfWeek())
                .customCronExpression(request.getCustomCronExpression())
                .timezone(request.getTimezone())
                .createdBy(request.getCreatedBy())
                .description(request.getDescription())
                .workflowConfig(request.getWorkflowConfig())
                .build();
    }

    // ==================== DTOs ====================

    public static class PipelineRunRequest {
        private String pipelineName;
        private java.util.List<Map<String, Object>> tasks;
        private Map<String, Object> data;

        public String getPipelineName() {
            return pipelineName;
        }

        public void setPipelineName(String pipelineName) {
            this.pipelineName = pipelineName;
        }

        public java.util.List<Map<String, Object>> getTasks() {
            return tasks;
        }

        public void setTasks(java.util.List<Map<String, Object>> tasks) {
            this.tasks = tasks;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }

    public static class OnboardingScheduleRequest {
        private String scheduleId;
        private String cron;

        public String getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(String scheduleId) {
            this.scheduleId = scheduleId;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class KYCStartRequest {
        private String caseId;
        private String interactionId;
        private String userId;
        private Map<String, Object> initialData;

        public String getCaseId() {
            return caseId;
        }

        public void setCaseId(String caseId) {
            this.caseId = caseId;
        }

        public String getInteractionId() {
            return interactionId;
        }

        public void setInteractionId(String interactionId) {
            this.interactionId = interactionId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Map<String, Object> getInitialData() {
            return initialData;
        }

        public void setInitialData(Map<String, Object> initialData) {
            this.initialData = initialData;
        }
    }

    public static class WorkflowStartResponse {
        private String processInstanceId;
        private String workflowId;
        private String status;

        public WorkflowStartResponse(String processInstanceId, String workflowId, String status) {
            this.processInstanceId = processInstanceId;
            this.workflowId = workflowId;
            this.status = status;
        }

        public String getProcessInstanceId() {
            return processInstanceId;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class WorkflowStatusResponse {
        private String workflowId;
        private String status;

        public WorkflowStatusResponse(String workflowId, String status) {
            this.workflowId = workflowId;
            this.status = status;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class ManualReviewRequest {
        private boolean approved;
        private String reason;

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class ScheduleResponse {
        private String scheduleId;
        private String cronExpression;
        private String workflowType;
        private String taskQueue;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private ScheduleStatus status;
        private String description;
        private String workflowArguments;

        public ScheduleResponse(String scheduleId, String cronExpression, String workflowType,
                               String taskQueue, String createdBy, LocalDateTime createdAt,
                               LocalDateTime updatedAt, ScheduleStatus status, String description,
                               String workflowArguments) {
            this.scheduleId = scheduleId;
            this.cronExpression = cronExpression;
            this.workflowType = workflowType;
            this.taskQueue = taskQueue;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.status = status;
            this.description = description;
            this.workflowArguments = workflowArguments;
        }

        // Getters
        public String getScheduleId() { return scheduleId; }
        public String getCronExpression() { return cronExpression; }
        public String getWorkflowType() { return workflowType; }
        public String getTaskQueue() { return taskQueue; }
        public String getCreatedBy() { return createdBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public ScheduleStatus getStatus() { return status; }
        public String getDescription() { return description; }
        public String getWorkflowArguments() { return workflowArguments; }
    }

    private ScheduleResponse convertToScheduleResponse(ScheduleEntity entity) {
        return new ScheduleResponse(
                entity.getScheduleId(),
                entity.getCronExpression(),
                entity.getWorkflowType(),
                entity.getTaskQueue(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getWorkflowArguments()
        );
    }

    // ==================== Payment Workflow DTOs ====================

    public static class PaymentStartRequest {
        private String paymentId;
        private String accountId;
        private double amount;
        private String currency;
        private String tenantId;
        private String userId;

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class PriorityPaymentRequest {
        private String paymentId;
        private String accountId;
        private double amount;
        private String currency;
        private String priority;
        private String tenantId;
        private String userId;

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class BatchPaymentRequest {
        private List<String> paymentIds;
        private String tenantId;
        private String userId;

        // Getters and setters
        public List<String> getPaymentIds() { return paymentIds; }
        public void setPaymentIds(List<String> paymentIds) { this.paymentIds = paymentIds; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class BatchProcessingResponse {
        private int totalPayments;
        private int successCount;
        private int failureCount;
        private long processingTimeMs;
        private List<String> failedPaymentIds;

        public BatchProcessingResponse(int totalPayments, int successCount, int failureCount,
                                     long processingTimeMs, List<String> failedPaymentIds) {
            this.totalPayments = totalPayments;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.processingTimeMs = processingTimeMs;
            this.failedPaymentIds = failedPaymentIds;
        }

        // Getters
        public int getTotalPayments() { return totalPayments; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public List<String> getFailedPaymentIds() { return failedPaymentIds; }
    }

    public static class DLQStatusResponse {
        private int queueSize;
        private List<DLQItem> failedPayments;

        public DLQStatusResponse(int queueSize, List<DLQItem> failedPayments) {
            this.queueSize = queueSize;
            this.failedPayments = failedPayments;
        }

        // Getters
        public int getQueueSize() { return queueSize; }
        public List<DLQItem> getFailedPayments() { return failedPayments; }
    }

    public static class DLQItem {
        private String paymentId;
        private String error;
        private int retryCount;
        private String tenantId;

        public DLQItem(String paymentId, String error, int retryCount, String tenantId) {
            this.paymentId = paymentId;
            this.error = error;
            this.retryCount = retryCount;
            this.tenantId = tenantId;
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getError() { return error; }
        public int getRetryCount() { return retryCount; }
        public String getTenantId() { return tenantId; }
    }

    public static class AdvancedMetricsResponse {
        private Map<String, String> circuitBreakerStatus;
        private Map<String, Integer> loadBalancingMetrics;
        private Map<String, Integer> eventStoreMetrics;

        public AdvancedMetricsResponse(Map<String, String> circuitBreakerStatus,
                                     Map<String, Integer> loadBalancingMetrics,
                                     Map<String, Integer> eventStoreMetrics) {
            this.circuitBreakerStatus = circuitBreakerStatus;
            this.loadBalancingMetrics = loadBalancingMetrics;
            this.eventStoreMetrics = eventStoreMetrics;
        }

        // Getters
        public Map<String, String> getCircuitBreakerStatus() { return circuitBreakerStatus; }
        public Map<String, Integer> getLoadBalancingMetrics() { return loadBalancingMetrics; }
        public Map<String, Integer> getEventStoreMetrics() { return eventStoreMetrics; }
    }

    // ==================== RECONCILIATION WORKFLOW DTOs ====================

    public static class ReconciliationStartRequest {
        private String reconciliationId;
        private String date;
        private String type;
        private Map<String, Object> config;

        // Getters and setters
        public String getReconciliationId() { return reconciliationId; }
        public void setReconciliationId(String reconciliationId) { this.reconciliationId = reconciliationId; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }

    public static class ManualResolutionRequest {
        private String discrepancyId;
        private String resolution;
        private String notes;
        private String resolvedBy;

        // Getters and setters
        public String getDiscrepancyId() { return discrepancyId; }
        public void setDiscrepancyId(String discrepancyId) { this.discrepancyId = discrepancyId; }

        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getResolvedBy() { return resolvedBy; }
        public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    }

    public static class AdditionalDataRequest {
        private String sourceId;
        private Map<String, Object> data;

        // Getters and setters
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }



    // ==================== WORKFLOW SCHEDULING DTOs ====================

    public static class WorkflowScheduleRequest {
        private String workflowType;
        private String scheduleType;
        private java.time.LocalDate executionDate;
        private java.time.LocalTime executionTime;
        private Integer dayOfMonth;
        private java.util.List<String> daysOfWeek;
        private String customCronExpression;
        private String timezone;
        private String createdBy;
        private String description;
        private Map<String, Object> workflowConfig;

        // Getters and setters
        public String getWorkflowType() { return workflowType; }
        public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }

        public String getScheduleType() { return scheduleType; }
        public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

        public java.time.LocalDate getExecutionDate() { return executionDate; }
        public void setExecutionDate(java.time.LocalDate executionDate) { this.executionDate = executionDate; }

        public java.time.LocalTime getExecutionTime() { return executionTime; }
        public void setExecutionTime(java.time.LocalTime executionTime) { this.executionTime = executionTime; }

        public Integer getDayOfMonth() { return dayOfMonth; }
        public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

        public java.util.List<String> getDaysOfWeek() { return daysOfWeek; }
        public void setDaysOfWeek(java.util.List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

        public String getCustomCronExpression() { return customCronExpression; }
        public void setCustomCronExpression(String customCronExpression) { this.customCronExpression = customCronExpression; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Map<String, Object> getWorkflowConfig() { return workflowConfig; }
        public void setWorkflowConfig(Map<String, Object> workflowConfig) { this.workflowConfig = workflowConfig; }
    }

    public static class WorkflowScheduleResponse {
        private String scheduledWorkflowId;
        private String status;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String scheduledWorkflowId;
            private String status;

            public Builder scheduledWorkflowId(String scheduledWorkflowId) {
                this.scheduledWorkflowId = scheduledWorkflowId;
                return this;
            }

            public Builder status(String status) {
                this.status = status;
                return this;
            }

            public WorkflowScheduleResponse build() {
                WorkflowScheduleResponse response = new WorkflowScheduleResponse();
                response.scheduledWorkflowId = this.scheduledWorkflowId;
                response.status = this.status;
                return response;
            }
        }

        // Getters
        public String getScheduledWorkflowId() { return scheduledWorkflowId; }
        public String getStatus() { return status; }
    }
}
