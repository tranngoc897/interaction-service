package com.ngoctran.interactionservice.workflow.onboarding;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.QueryMethod;

import java.util.Map;

/**
 * KYC Onboarding Workflow Interface
 * 
 * This onboarding orchestrates the complete KYC onboarding journey:
 * 1. User submits personal information
 * 2. User uploads documents (ID front, ID back, selfie)
 * 3. System performs OCR on documents
 * 4. System verifies ID with external service
 * 5. System approves or rejects KYC
 * 6. Callback to Interaction Service with result
 * 
 * Workflow ID format: "kyc-onboarding-{caseId}"
 * Task Queue: KYC_ONBOARDING_QUEUE
 */
@WorkflowInterface
public interface KYCOnboardingWorkflow {

    /**
     * Main onboarding method
     * 
     * @param caseId The case ID from flow_case table
     * @param interactionId The interaction ID from flw_int table
     * @param initialData Initial data submitted by user
     * @return Workflow result with status and data
     */
    @WorkflowMethod
    KYCWorkflowResult execute(String caseId, String interactionId, Map<String, Object> initialData);

    /**
     * Signal: Documents uploaded
     * Called when user completes document upload step
     * 
     * @param documents Map of document types to URLs
     */
    @SignalMethod
    void documentsUploaded(Map<String, String> documents);

    /**
     * Signal: User data updated
     * Called when user updates their information
     * 
     * @param updatedData Updated user data
     */
    @SignalMethod
    void userDataUpdated(Map<String, Object> updatedData);

    /**
     * Signal: Manual approval/rejection
     * Called by admin for manual review
     * 
     * @param approved Whether KYC is approved
     * @param reason Reason for approval/rejection
     */
    @SignalMethod
    void manualReview(boolean approved, String reason);

    /**
     * Query: Get current onboarding status
     * 
     * @return Current status of the onboarding
     */
    @QueryMethod
    String getStatus();

    /**
     * Query: Get onboarding progress
     * 
     * @return Progress information
     */
    @QueryMethod
    WorkflowProgress getProgress();

    /**
     * Workflow Result DTO
     */
    class KYCWorkflowResult {
        private String status; // APPROVED, REJECTED, PENDING_MANUAL_REVIEW
        private String reason;
        private Map<String, Object> extractedData; // Data extracted from OCR
        private Map<String, Object> verificationResult;
        private Map<String, Object> onboardingData; // Onboarding completion data

        // Constructors, getters, setters
        public KYCWorkflowResult() {}

        public KYCWorkflowResult(String status, String reason) {
            this.status = status;
            this.reason = reason;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Map<String, Object> getExtractedData() { return extractedData; }
        public void setExtractedData(Map<String, Object> extractedData) { this.extractedData = extractedData; }
        public Map<String, Object> getVerificationResult() { return verificationResult; }
        public void setVerificationResult(Map<String, Object> verificationResult) { this.verificationResult = verificationResult; }
        public Map<String, Object> getOnboardingData() { return onboardingData; }
        public void setOnboardingData(Map<String, Object> onboardingData) { this.onboardingData = onboardingData; }
    }

    /**
     * Workflow Progress DTO
     */
    class WorkflowProgress {
        private String currentStep;
        private int completedSteps;
        private int totalSteps;
        private int percentComplete;

        public WorkflowProgress() {}

        public WorkflowProgress(String currentStep, int completedSteps, int totalSteps) {
            this.currentStep = currentStep;
            this.completedSteps = completedSteps;
            this.totalSteps = totalSteps;
            this.percentComplete = (int) ((completedSteps * 100.0) / totalSteps);
        }

        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
        public int getCompletedSteps() { return completedSteps; }
        public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        public int getPercentComplete() { return percentComplete; }
        public void setPercentComplete(int percentComplete) { this.percentComplete = percentComplete; }
    }
}
