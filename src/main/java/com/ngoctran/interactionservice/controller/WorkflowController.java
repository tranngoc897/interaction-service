package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.WorkflowHistoryEntity;
import com.ngoctran.interactionservice.workflow.WorkflowService;
import com.ngoctran.interactionservice.workflow.onboarding.OnboardingWorkflow;

import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final WorkflowService workflowService;

    /**
     * Start KYC Onboarding Workflow
     */

    /*
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
    */

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
    public ResponseEntity<OnboardingWorkflow.WorkflowProgress> getWorkflowProgress(
            @PathVariable String workflowId) {
        log.info("Getting onboarding progress: {}", workflowId);
        OnboardingWorkflow.WorkflowProgress progress = workflowService.queryWorkflowProgress(workflowId);
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
