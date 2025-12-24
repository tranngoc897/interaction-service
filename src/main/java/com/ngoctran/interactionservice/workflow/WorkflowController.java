package com.ngoctran.interactionservice.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
