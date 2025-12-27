package com.ngoctran.interactionservice.workflow.onboarding;

import com.ngoctran.interactionservice.workflow.activity.CIFActivity;
import com.ngoctran.interactionservice.workflow.activity.CorebankAccountActivity;
import com.ngoctran.interactionservice.workflow.activity.IDVerificationActivity;
import com.ngoctran.interactionservice.workflow.activity.InteractionCallbackActivity;
import com.ngoctran.interactionservice.workflow.activity.KeycloakAccountActivity;
import com.ngoctran.interactionservice.workflow.activity.NotificationActivity;
import com.ngoctran.interactionservice.workflow.activity.OCRActivity;
import com.ngoctran.interactionservice.workflow.activity.OnboardingNotificationActivity;
import com.ngoctran.interactionservice.workflow.activity.RetailAccountActivity;
import com.ngoctran.interactionservice.workflow.activity.TaskActivity;
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
public class OnboardingWorkflowImpl implements OnboardingWorkflow {

    // Temporal logger (required for workflows)
    private static final Logger log = Workflow.getLogger(OnboardingWorkflowImpl.class);

    // Workflow state
    private String currentStatus = "INITIALIZED";
    private WorkflowProgress progress = new WorkflowProgress("INITIALIZED", 0, 20); // Updated for more steps
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

    // New onboarding activities
    private final CIFActivity cifActivity;
    private final CorebankAccountActivity corebankAccountActivity;
    private final RetailAccountActivity retailAccountActivity;
    private final KeycloakAccountActivity keycloakAccountActivity;
    private final OnboardingNotificationActivity onboardingNotificationActivity;

    public OnboardingWorkflowImpl() {
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

        // Initialize new onboarding activities
        this.cifActivity = Workflow.newActivityStub(CIFActivity.class, defaultOptions);
        this.corebankAccountActivity = Workflow.newActivityStub(CorebankAccountActivity.class, defaultOptions);
        this.retailAccountActivity = Workflow.newActivityStub(RetailAccountActivity.class, defaultOptions);
        this.keycloakAccountActivity = Workflow.newActivityStub(KeycloakAccountActivity.class, defaultOptions);
        this.onboardingNotificationActivity = Workflow.newActivityStub(OnboardingNotificationActivity.class, defaultOptions);
    }

    @Override
    public KYCWorkflowResult execute(String caseId, String interactionId, Map<String, Object> initialData) {
        log.info("Starting Complete KYC Onboarding Workflow for case: {}, interaction: {}", caseId, interactionId);

        try {
            // ==================== PHASE 1: KYC VERIFICATION ====================

            // Step 1: Validate initial data
            updateProgress("VALIDATING_DATA", 1, 20);
            validateInitialData(initialData);

            // Step 2: Document Processing (OCR + Verification)
            updateProgress("PROCESSING_DOCUMENTS", 2, 20);
            DocumentProcessingWorkflow documentChild = Workflow.newChildWorkflowStub(DocumentProcessingWorkflow.class);
            Map<String, Object> childResult = documentChild.processDocuments(caseId, initialData);

            // Step 3: Risk Assessment
            updateProgress("RISK_ASSESSMENT", 3, 20);
            performRiskAssessment(caseId, initialData);

            // Step 4: Determine KYC Approval
            updateProgress("DETERMINING_KYC_APPROVAL", 4, 20);
            IDVerificationActivity.IDVerificationResult vResult = new IDVerificationActivity.IDVerificationResult();
            vResult.setVerified(Boolean.TRUE.equals(childResult.get("isVerified")));
            vResult.setConfidenceScore((Double) childResult.get("verificationScore"));

            KYCWorkflowResult result = determineApproval(vResult, (Map<String, Object>) childResult.get("ocrData"), interactionId);

            // Only proceed with onboarding if KYC is approved
            if (!"APPROVED".equals(result.getStatus())) {
                log.info("KYC not approved, stopping onboarding workflow");
                updateProgress("KYC_REJECTED", 20, 20);
                return result;
            }

            // ==================== PHASE 2: SYSTEM ACCOUNT CREATION ====================

            // Step 5: Create CIF in Core Banking
            updateProgress("CREATING_CIF", 5, 20);
            CIFActivity.CIFResult cifResult = createCIFRecord(caseId, initialData);
            String cifNumber = cifResult.getCifNumber();
            String coreCustomerId = cifResult.getCoreCustomerId();

            // Step 6: Create VND Account in Core Banking
            updateProgress("CREATING_COREBANK_VND_ACCOUNT", 6, 20);
            CorebankAccountActivity.AccountResult coreVndAccount = createCorebankVNDAccount(
                caseId, cifNumber, coreCustomerId);

            // Step 7: Create Retail Banking Accounts (VND + USD)
            updateProgress("CREATING_RETAIL_ACCOUNTS", 7, 20);
            RetailAccountActivity.RetailAccountResult retailAccounts = createRetailAccounts(
                caseId, cifNumber, coreCustomerId);

            // Step 8: Create Keycloak Account
            updateProgress("CREATING_KEYCLOAK_ACCOUNT", 8, 20);
            KeycloakAccountActivity.KeycloakResult keycloakAccount = createKeycloakAccount(
                caseId, cifNumber, (String) initialData.get("phoneNumber"), (String) initialData.get("email"));

            // ==================== PHASE 3: DEVICE & SECURITY ====================

            // Step 9: Device Registration
            updateProgress("REGISTERING_DEVICE", 9, 20);
            performDeviceRegistration(caseId, keycloakAccount.getUserId());

            // Step 10: Push Token Setup
            updateProgress("SETTING_UP_PUSH_TOKEN", 10, 20);
            setupPushToken(caseId, keycloakAccount.getUserId());

            // ==================== PHASE 4: DATA SYNCHRONIZATION ====================

            // Step 11: Sync Customer Data (Core → RDB)
            updateProgress("SYNCING_CUSTOMER_DATA", 11, 20);
            syncCustomerData(caseId, cifNumber, coreCustomerId);

            // ==================== PHASE 5: FINALIZATION & NOTIFICATIONS ====================

            // Step 12: Send Onboarding Completion SMS
            updateProgress("SENDING_COMPLETION_SMS", 12, 20);
            sendOnboardingSMSToCustomer(caseId, (String) initialData.get("phoneNumber"), cifNumber);

            // Step 13: Send Onboarding Completion Push Notification
            updateProgress("SENDING_COMPLETION_PUSH", 13, 20);
            sendOnboardingPushToCustomer(caseId, "DEVICE_TOKEN_PLACEHOLDER", cifNumber);

            // Step 14: Send Onboarding Completion Email
            updateProgress("SENDING_COMPLETION_EMAIL", 14, 20);
            sendOnboardingEmailToCustomer(caseId, (String) initialData.get("email"), cifNumber);

            // Step 15: Update Interaction Service
            updateProgress("UPDATING_INTERACTION_SERVICE", 15, 20);
            result.setOnboardingData(Map.of(
                "cifNumber", cifNumber,
                "coreCustomerId", coreCustomerId,
                "keycloakUserId", keycloakAccount.getUserId(),
                "retailAccounts", retailAccounts.getAccounts(),
                "completionTimestamp", System.currentTimeMillis()
            ));
            notifyInteractionService(caseId, interactionId, result);

            // Step 16: Send Final Success Notification
            updateProgress("SENDING_FINAL_NOTIFICATION", 16, 20);
            sendOnboardingCompletionNotification(caseId, cifNumber);

            log.info("Complete Onboarding Workflow finished successfully for case: {}", caseId);
            updateProgress("ONBOARDING_COMPLETED", 20, 20);

            return result;

        } catch (Exception e) {
            log.error("Onboarding Workflow failed for case: {}", caseId, e);
            currentStatus = "FAILED";

            // Notify about failure
            notifyFailure(caseId, interactionId, e.getMessage());

            return new KYCWorkflowResult("FAILED", "Onboarding workflow failed: " + e.getMessage());
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

        // Result logic based on verification
        if (verificationResult != null && verificationResult.isVerified()) {
            result.setStatus("APPROVED");
            result.setReason("Auto-approved: ID Verification successful");
            log.info("KYC APPROVED for case");
        } else {
            result.setStatus("MANUAL_REVIEW");
            result.setReason("Verification failed or confidence low. Manual review required.");
            log.info("KYC needs MANUAL_REVIEW");
        }

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

    private void updateProgress(String step, int completed, int totalSteps) {
        this.currentStatus = step;
        this.progress = new WorkflowProgress(step, completed, totalSteps);
        log.info("Progress: {}/{} - {}", completed, totalSteps, step);
    }

    // ==================== ONBOARDING HELPER METHODS ====================

    private void performRiskAssessment(String caseId, Map<String, Object> initialData) {
        log.info("Performing risk assessment for case: {}", caseId);
        // In real implementation, this would call risk assessment service
        Workflow.sleep(Duration.ofSeconds(1));
        log.info("Risk assessment completed - Low risk");
    }

    private CIFActivity.CIFResult createCIFRecord(String caseId, Map<String, Object> initialData) {
        log.info("Creating CIF record for case: {}", caseId);

        CIFActivity.CIFData cifData = new CIFActivity.CIFData(
            (String) initialData.get("fullName"),
            (String) initialData.get("dob"),
            (String) initialData.get("idNumber"),
            (String) initialData.get("phoneNumber"),
            (String) initialData.get("email"),
            (String) initialData.get("address"),
            (String) initialData.get("nationality"),
            (String) initialData.get("occupation")
        );

        CIFActivity.CIFResult result = cifActivity.createCIF(caseId, caseId, cifData);

        if (!result.isSuccess()) {
            throw new RuntimeException("CIF creation failed: " + result.getErrorMessage());
        }

        log.info("CIF created successfully: {}", result.getCifNumber());
        return result;
    }

    private CorebankAccountActivity.AccountResult createCorebankVNDAccount(String caseId, String cifNumber, String coreCustomerId) {
        log.info("Creating corebank VND account for case: {}, CIF: {}", caseId, cifNumber);

        CorebankAccountActivity.AccountResult result = corebankAccountActivity.createVNDAccount(caseId, cifNumber, coreCustomerId);

        if (!result.isSuccess()) {
            throw new RuntimeException("Corebank VND account creation failed: " + result.getErrorMessage());
        }

        log.info("Corebank VND account created: {}", result.getAccountNumber());
        return result;
    }

    private RetailAccountActivity.RetailAccountResult createRetailAccounts(String caseId, String cifNumber, String coreCustomerId) {
        log.info("Creating retail accounts for case: {}, CIF: {}", caseId, cifNumber);

        RetailAccountActivity.RetailAccountResult result = retailAccountActivity.createRetailAccounts(caseId, cifNumber, coreCustomerId);

        if (!result.isSuccess()) {
            throw new RuntimeException("Retail accounts creation failed: " + result.getErrorMessage());
        }

        log.info("Retail accounts created: {} accounts", result.getAccounts().size());
        return result;
    }

    private KeycloakAccountActivity.KeycloakResult createKeycloakAccount(String caseId, String cifNumber, String phoneNumber, String email) {
        log.info("Creating Keycloak account for case: {}, CIF: {}, phone: {}", caseId, cifNumber, phoneNumber);

        KeycloakAccountActivity.KeycloakResult result = keycloakAccountActivity.createKeycloakAccount(caseId, cifNumber, phoneNumber, email);

        if (!result.isSuccess()) {
            throw new RuntimeException("Keycloak account creation failed: " + result.getErrorMessage());
        }

        log.info("Keycloak account created: {}", result.getUsername());
        return result;
    }

    private void performDeviceRegistration(String caseId, String keycloakUserId) {
        log.info("Performing device registration for case: {}, user: {}", caseId, keycloakUserId);
        // In real implementation, this would integrate with device registration service
        Workflow.sleep(Duration.ofSeconds(2));
        log.info("Device registration completed");
    }

    private void setupPushToken(String caseId, String keycloakUserId) {
        log.info("Setting up push token for case: {}, user: {}", caseId, keycloakUserId);
        // In real implementation, this would integrate with push notification service
        Workflow.sleep(Duration.ofSeconds(1));
        log.info("Push token setup completed");
    }

    private void syncCustomerData(String caseId, String cifNumber, String coreCustomerId) {
        log.info("Syncing customer data from core to RDB for case: {}, CIF: {}", caseId, cifNumber);
        // In real implementation, this would sync data from core banking to relational database
        Workflow.sleep(Duration.ofSeconds(3));
        log.info("Customer data sync completed");
    }

    private void sendOnboardingSMSToCustomer(String caseId, String phoneNumber, String cifNumber) {
        log.info("Sending onboarding completion SMS to: {}", phoneNumber);

        OnboardingNotificationActivity.NotificationResult result =
            onboardingNotificationActivity.sendOnboardingSMSToCustomer(caseId, phoneNumber, cifNumber);

        if (!result.isSuccess()) {
            log.warn("SMS sending failed: {}", result.getErrorMessage());
            // Don't throw exception for notification failures
        } else {
            log.info("Onboarding SMS sent successfully: {}", result.getMessageId());
        }
    }

    private void sendOnboardingPushToCustomer(String caseId, String deviceToken, String cifNumber) {
        log.info("Sending onboarding completion push notification to device: {}", deviceToken);

        OnboardingNotificationActivity.NotificationResult result =
            onboardingNotificationActivity.sendOnboardingPushToCustomer(caseId, deviceToken, cifNumber);

        if (!result.isSuccess()) {
            log.warn("Push notification sending failed: {}", result.getErrorMessage());
        } else {
            log.info("Onboarding push notification sent successfully: {}", result.getMessageId());
        }
    }

    private void sendOnboardingEmailToCustomer(String caseId, String email, String cifNumber) {
        log.info("Sending onboarding completion email to: {}", email);

        OnboardingNotificationActivity.NotificationResult result =
            onboardingNotificationActivity.sendOnboardingEmailToCustomer(caseId, email, cifNumber);

        if (!result.isSuccess()) {
            log.warn("Email sending failed: {}", result.getErrorMessage());
        } else {
            log.info("Onboarding email sent successfully: {}", result.getMessageId());
        }
    }

    private void sendOnboardingCompletionNotification(String caseId, String cifNumber) {
        log.info("Sending final onboarding completion notification for case: {}", caseId);

        String message = String.format("🎉 Chúc mừng! Quy trình Onboarding của bạn đã hoàn tất. CIF: %s", cifNumber);
        notificationActivity.sendNotification(caseId, "ONBOARDING_COMPLETED", message);

        log.info("Final onboarding notification sent");
    }
}
