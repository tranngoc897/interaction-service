package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.workflow.dto.ConfirmServiceCreateRequest;
import com.ngoctran.interactionservice.workflow.dto.ConfirmServiceCreateResponse;
import com.ngoctran.interactionservice.workflow.dto.ConfirmWorkflowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Workflow Confirmation Service
 *
 * Based on your ConfirmService integration pattern in SchedulerService
 * Handles workflow action confirmations through external confirmation service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowConfirmationService {

    private final WorkflowHistoryRepository workflowHistoryRepository;
    // Note: In real implementation, inject ConfirmServiceClient
    // private final ConfirmServiceClient confirmServiceClient;

    @Value("${workflow.confirmation.callback.base-url:http://localhost:8080}")
    private String callbackBaseUrl;

    /**
     * Request confirmation for workflow action
     *
     * @param workflowId Workflow to confirm action for
     * @param action Action to confirm (CANCEL, SUSPEND, RESUME, etc.)
     * @param userId User requesting the action
     * @return Confirmation response with confirmation ID
     */
    public ConfirmationResponse requestWorkflowConfirmation(String workflowId, String action, String userId) {
        log.info("Requesting confirmation for workflow action: {} on workflow: {} by user: {}",
                action, workflowId, userId);

        try {
            // Create confirmation request (similar to your ConfirmServiceCreateRequest)
            ConfirmServiceCreateRequest confirmRequest = ConfirmServiceCreateRequest.builder()
                    .userId(userId)
                    .method("PASSCODE") // Based on your AuthenticatorMethod
                    .expiresIn(300) // 5 minutes
                    .confirmation(
                            ConfirmServiceCreateRequest.Confirmation.builder()
                                    .idpUserId(userId)
                                    .dbsUserId(userId)
                                    .idpIssuer("RDB") // Based on your UserType
                                    .transactionData(
                                            ConfirmWorkflowRequest.builder()
                                                    .workflowId(workflowId)
                                                    .action(action)
                                                    .requestedBy(userId)
                                                    .build())
                                    .callbackUrl(buildCallbackUrl(workflowId, action))
                                    .confirmationType("WORKFLOW_" + action.toUpperCase())
                                    .build())
                    .transactionType("SFX") // Based on your transaction type
                    .build();

            // Call external confirmation service
            // ConfirmServiceCreateResponse confirmResponse = confirmServiceClient.create(confirmRequest);
            // For demo purposes, simulate response
            ConfirmServiceCreateResponse confirmResponse = ConfirmServiceCreateResponse.builder()
                    .confirmationId("CONF-" + System.nanoTime())
                    .txnRefNumber("TXN-" + System.nanoTime())
                    .build();

            // Record confirmation request in history
            WorkflowHistoryEntity historyEntry = WorkflowHistoryEntity.createConfirmationRequest(
                    workflowId, "WORKFLOW", action, confirmResponse.getConfirmationId(),
                    confirmResponse.getTxnRefNumber(), userId);
            workflowHistoryRepository.save(historyEntry);

            log.info("Confirmation request created: {} for workflow: {}", confirmResponse.getConfirmationId(), workflowId);

            return ConfirmationResponse.builder()
                    .confirmationId(confirmResponse.getConfirmationId())
                    .txnRefNumber(confirmResponse.getTxnRefNumber())
                    .status("CONFIRMATION_REQUIRED")
                    .expiresIn(300)
                    .build();

        } catch (Exception e) {
            log.error("Failed to request workflow confirmation for workflow: {}", workflowId, e);
            throw new RuntimeException("Confirmation request failed: " + e.getMessage());
        }
    }

    /**
     * Confirm workflow action after user authentication
     *
     * @param workflowId Workflow ID
     * @param action Action to perform
     * @param confirmationId Confirmation ID from request
     */
    public void confirmWorkflowAction(String workflowId, String action, String confirmationId) {
        log.info("Confirming workflow action: {} for workflow: {} with confirmation: {}",
                action, workflowId, confirmationId);

        try {
            // Find the confirmation request in history
            WorkflowHistoryEntity confirmationRequest = workflowHistoryRepository
                    .findByConfirmationIdOrderByChangedAtDesc(confirmationId)
                    .stream()
                    .filter(WorkflowHistoryEntity::isConfirmationRequest)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Confirmation request not found"));

            // Record confirmation completion
            WorkflowHistoryEntity completionEntry = WorkflowHistoryEntity.createConfirmationCompleted(
                    workflowId, "WORKFLOW", action, confirmationId, confirmationRequest.getChangedBy());
            workflowHistoryRepository.save(completionEntry);

            // Execute the confirmed action
            executeConfirmedAction(workflowId, action, confirmationRequest.getChangedBy());

            log.info("Workflow action confirmed and executed: {} on workflow: {}", action, workflowId);

        } catch (Exception e) {
            log.error("Failed to confirm workflow action for workflow: {}", workflowId, e);
            throw new RuntimeException("Confirmation failed: " + e.getMessage());
        }
    }

    /**
     * Execute the confirmed workflow action
     */
    private void executeConfirmedAction(String workflowId, String action, String confirmedBy) {
        switch (action.toUpperCase()) {
            case "CANCEL":
                // In real implementation: workflowService.cancelWorkflow(workflowId, confirmedBy);
                log.info("Executing CANCEL action on workflow: {}", workflowId);
                break;

            case "SUSPEND":
                // In real implementation: workflowService.suspendWorkflow(workflowId, confirmedBy);
                log.info("Executing SUSPEND action on workflow: {}", workflowId);
                break;

            case "RESUME":
                // In real implementation: workflowService.resumeWorkflow(workflowId, confirmedBy);
                log.info("Executing RESUME action on workflow: {}", workflowId);
                break;

            case "TERMINATE":
                // In real implementation: workflowService.terminateWorkflow(workflowId, confirmedBy);
                log.info("Executing TERMINATE action on workflow: {}", workflowId);
                break;

            default:
                throw new IllegalArgumentException("Unsupported confirmed action: " + action);
        }
    }

    /**
     * Build callback URL for confirmation
     */
    private String buildCallbackUrl(String workflowId, String action) {
        return String.format("%s/api/workflows/%s/confirm/%s",
                callbackBaseUrl, workflowId, action.toLowerCase());
    }

    /**
     * Check if confirmation is still valid
     */
    public boolean isConfirmationValid(String confirmationId) {
        return workflowHistoryRepository.findByConfirmationIdOrderByChangedAtDesc(confirmationId)
                .stream()
                .anyMatch(entry -> entry.isConfirmationRequest() &&
                                 !entry.isConfirmationCompleted() &&
                                 isConfirmationWithinTimeLimit(entry));
    }

    /**
     * Check if confirmation is within time limit (5 minutes)
     */
    private boolean isConfirmationWithinTimeLimit(WorkflowHistoryEntity confirmationRequest) {
        return confirmationRequest.getChangedAt()
                .isAfter(java.time.LocalDateTime.now().minusMinutes(5));
    }

    /**
     * Get confirmation details
     */
    public ConfirmationDetails getConfirmationDetails(String confirmationId) {
        WorkflowHistoryEntity request = workflowHistoryRepository
                .findByConfirmationIdOrderByChangedAtDesc(confirmationId)
                .stream()
                .filter(WorkflowHistoryEntity::isConfirmationRequest)
                .findFirst()
                .orElse(null);

        if (request == null) {
            return null;
        }

        return ConfirmationDetails.builder()
                .confirmationId(confirmationId)
                .workflowId(request.getWorkflowId())
                .action(request.getConfirmedAction())
                .requestedBy(request.getChangedBy())
                .requestedAt(request.getChangedAt())
                .isValid(isConfirmationWithinTimeLimit(request))
                .build();
    }

    // ==================== DTOs ====================

    /**
     * Confirmation Response DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ConfirmationResponse {
        private String confirmationId;
        private String txnRefNumber;
        private String status;
        private int expiresIn;
    }

    /**
     * Confirmation Details DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ConfirmationDetails {
        private String confirmationId;
        private String workflowId;
        private String action;
        private String requestedBy;
        private java.time.LocalDateTime requestedAt;
        private boolean isValid;
    }
}
