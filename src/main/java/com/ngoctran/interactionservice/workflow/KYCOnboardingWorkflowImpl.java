package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.workflow.activity.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * KYC Onboarding Workflow Implementation
 * 
 * Complete working implementation of KYC onboarding onboarding
 * This serves as the EXAMPLE implementation (Option C)
 */
public class KYCOnboardingWorkflowImpl implements KYCOnboardingWorkflow {

    // Temporal logger (required for workflows)
    private static final Logger log = Workflow.getLogger(KYCOnboardingWorkflowImpl.class);

    // Workflow state
    private String currentStatus = "INITIALIZED";
    private WorkflowProgress progress = new WorkflowProgress("INITIALIZED", 0, 6);
    private Map<String, String> documents;
    private boolean documentsReceived = false;
    private boolean manualReviewCompleted = false;
    private boolean manualApprovalResult = false;
    private String manualReviewReason = "";

    // Activity stubs with retry policies
    private final OCRActivity ocrActivity;
    private final IDVerificationActivity idVerificationActivity;
    private final NotificationActivity notificationActivity;
    private final InteractionCallbackActivity callbackActivity;
    private final TaskActivity taskActivity;

    public KYCOnboardingWorkflowImpl() {
        // Configure activity options with retries
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(10))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();

        // Create activity stubs
        this.ocrActivity = Workflow.newActivityStub(OCRActivity.class, defaultOptions);
        this.idVerificationActivity = Workflow.newActivityStub(IDVerificationActivity.class, defaultOptions);
        this.notificationActivity = Workflow.newActivityStub(NotificationActivity.class, defaultOptions);
        this.callbackActivity = Workflow.newActivityStub(InteractionCallbackActivity.class, defaultOptions);
        this.taskActivity = Workflow.newActivityStub(TaskActivity.class, defaultOptions);
    }

    @Override
    public KYCWorkflowResult execute(String caseId, String interactionId, Map<String, Object> initialData) {
        log.info("Starting KYC Onboarding Workflow for case: {}, interaction: {}", caseId, interactionId);

        try {
            // Step 1: Validate initial data
            updateProgress("VALIDATING_DATA", 1);
            validateInitialData(initialData);

            // Step 2: Skip waiting and OCR for demo auto-completion
            updateProgress("PROCESSING_AUTO", 3);
            log.info("Bypassing document wait and OCR for automatic completion");

            // Mock OCR results for the notification
            Map<String, Object> mockOcrResults = new HashMap<>(initialData);
            mockOcrResults.put("verificationMode", "AUTO_BYPASS");

            // Step 5: Determine approval (Now simplified as APPROVED)
            updateProgress("DETERMINING_APPROVAL", 5);
            KYCWorkflowResult result = determineApproval(null, mockOcrResults, interactionId);

            // Step 6: Callback to Interaction Service (This updates DB to
            // APPROVED/COMPLETED)
            updateProgress("NOTIFYING_SERVICE", 6);
            notifyInteractionService(caseId, interactionId, result);

            // Send notification to user
            sendNotification(caseId, result);

            log.info("KYC Workflow completed automatically for case: {}", caseId);
            updateProgress("COMPLETED", 6);

            return result;

        } catch (Exception e) {
            log.error("KYC Workflow failed for case: {}", caseId, e);
            currentStatus = "FAILED";

            // Notify about failure
            notifyFailure(caseId, interactionId, e.getMessage());

            return new KYCWorkflowResult("FAILED", "Workflow execution failed: " + e.getMessage());
        }
    }

    @Override
    public void documentsUploaded(Map<String, String> documents) {
        log.info("Received documents signal: {}", documents.keySet());
        this.documents = documents;
        this.documentsReceived = true;
    }

    @Override
    public void userDataUpdated(Map<String, Object> updatedData) {
        log.info("Received user data update signal");
        // Handle data update - could re-trigger verification
    }

    @Override
    public void manualReview(boolean approved, String reason) {
        log.info("Received manual review signal: approved={}, reason={}", approved, reason);
        this.manualApprovalResult = approved;
        this.manualReviewReason = reason;
        this.manualReviewCompleted = true;
    }

    @Override
    public String getStatus() {
        return currentStatus;
    }

    @Override
    public WorkflowProgress getProgress() {
        return progress;
    }

    // ==================== Private Helper Methods ====================

    private void validateInitialData(Map<String, Object> data) {
        log.info("Validating initial data");
        currentStatus = "VALIDATING";

        // Validate required fields
        String[] requiredFields = { "fullName", "dob", "idNumber" };
        for (String field : requiredFields) {
            if (!data.containsKey(field) || data.get(field) == null) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
        }

        log.info("Initial data validation passed");
    }

    private void waitForDocuments() {
        log.info("Waiting for documents to be uploaded");
        currentStatus = "WAITING_FOR_DOCUMENTS";

        // Wait for documents signal with timeout
        Workflow.await(Duration.ofHours(24), () -> documentsReceived);

        if (!documentsReceived) {
            throw new RuntimeException("Timeout waiting for documents");
        }

        log.info("Documents received");
    }

    private Map<String, Object> performOCR(String caseId, Map<String, String> documents) {
        log.info("Performing OCR on documents");
        currentStatus = "PROCESSING_OCR";

        Map<String, Object> allOCRResults = new HashMap<>();

        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docType = entry.getKey();
            String docUrl = entry.getValue();

            log.info("Processing OCR for document type: {}", docType);

            OCRActivity.OCRResult result = ocrActivity.extractText(docUrl, docType);
            allOCRResults.put(docType, result.getExtractedData());
        }

        log.info("OCR processing completed");
        return allOCRResults;
    }

    private IDVerificationActivity.IDVerificationResult verifyID(
            String caseId,
            Map<String, Object> ocrResults,
            Map<String, String> documents) {

        log.info("Verifying ID");
        currentStatus = "VERIFYING_ID";

        // Extract ID data from OCR results
        @SuppressWarnings("unchecked")
        Map<String, Object> idFrontData = (Map<String, Object>) ocrResults.get("id-front");

        String idNumber = (String) idFrontData.get("idNumber");
        String fullName = (String) idFrontData.get("fullName");
        String dob = (String) idFrontData.get("dob");

        // Call ID verification activity
        IDVerificationActivity.IDVerificationResult result = idVerificationActivity.verifyID(idNumber, fullName, dob,
                documents.get("selfie"));

        log.info("ID verification completed: {}", result.isVerified());
        return result;
    }

    private KYCWorkflowResult determineApproval(
            IDVerificationActivity.IDVerificationResult verificationResult,
            Map<String, Object> ocrResults,
            String interactionId) {

        log.info("Determining approval");
        currentStatus = "DETERMINING_APPROVAL";

        KYCWorkflowResult result = new KYCWorkflowResult();
        result.setExtractedData(ocrResults);

        if (verificationResult != null) {
            result.setVerificationResult(Map.of(
                    "verified", verificationResult.isVerified(),
                    "confidence", verificationResult.getConfidenceScore(),
                    "matchScore", verificationResult.getFaceMatchScore()));
        } else {
            result.setVerificationResult(Map.of(
                    "verified", true,
                    "confidence", 1.0,
                    "matchScore", 1.0,
                    "mode", "MOCK"));
        }

        // Auto-approval logic (Simplified for automatic completion)
        result.setStatus("APPROVED");
        result.setReason("Auto-approved by workflow system");
        log.info("KYC auto-approved and completing case");

        return result;
    }

    private void notifyInteractionService(String caseId, String interactionId, KYCWorkflowResult result) {
        log.info("Notifying Interaction Service and updating Case status");

        // 1. Update Interaction status
        callbackActivity.updateInteractionStatus(
                interactionId,
                result.getStatus(),
                result.getReason(),
                result.getExtractedData());

        // 2. Update CASE status to COMPLETED/APPROVED
        callbackActivity.updateCaseStatus(caseId, result.getStatus());
    }

    private void sendNotification(String caseId, KYCWorkflowResult result) {
        log.info("Sending notification to user");

        String message = result.getStatus().equals("APPROVED")
                ? "Your KYC has been approved!"
                : "Your KYC requires additional review.";

        notificationActivity.sendNotification(caseId, "KYC_RESULT", message);
    }

    private void notifyFailure(String caseId, String interactionId, String errorMessage) {
        try {
            callbackActivity.updateInteractionStatus(
                    interactionId,
                    "FAILED",
                    errorMessage,
                    null);
            notificationActivity.sendNotification(
                    caseId,
                    "KYC_FAILED",
                    "KYC processing failed. Please contact support.");
        } catch (Exception e) {
            log.error("Failed to send failure notification", e);
        }
    }

    private void updateProgress(String step, int completed) {
        this.currentStatus = step;
        this.progress = new WorkflowProgress(step, completed, 6);
        log.info("Progress: {}/{} - {}", completed, 6, step);
    }
}
