package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JavaDelegate for centralized error handling
 * Handles errors from all service tasks and logs them appropriately
 */
@Component("errorHandlingDelegate")
public class ErrorHandlingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingDelegate.class);
    private final WorkflowEventPublisher eventPublisher;

    public ErrorHandlingDelegate(WorkflowEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(DelegateExecution execution)  {
        log.error("Handling error for process: {}", execution.getProcessInstanceId());

        try {
            // Get error information
            String errorSource = determineErrorSource(execution);
            String errorMessage = (String) execution.getVariable("errorMessage");
            String errorCode = (String) execution.getVariable("errorCode");

            // Get case information
            String caseId = (String) execution.getVariable("caseId");
            String applicantId = (String) execution.getVariable("applicantId");

            log.error("Error Details - Source: {}, Code: {}, Message: {}, Case: {}",
                    errorSource, errorCode, errorMessage, caseId);

            // Create error record
            Map<String, Object> errorRecord = createErrorRecord(
                    execution.getProcessInstanceId(),
                    caseId,
                    applicantId,
                    errorSource,
                    errorCode,
                    errorMessage);

            // Publish system error event
            String severity = (String) errorRecord.get("severity");
            boolean retryable = (boolean) errorRecord.get("retryable");
            eventPublisher.publishSystemErrorEvent(caseId, execution.getProcessInstanceId(),
                    errorSource, errorCode, errorMessage, severity, retryable, errorRecord);

            // Save error record (in production, save to database)
            saveErrorRecord(errorRecord);

            // Send error notification
            sendErrorNotification(execution, errorRecord);

            // Update case status
            updateCaseStatus(caseId, "ERROR", errorMessage);

            // Set error variables for process
            execution.setVariable("errorHandled", true);
            execution.setVariable("errorTimestamp", Instant.now().toString());
            execution.setVariable("errorRecord", errorRecord);

            log.info("Error handling completed for process: {}", execution.getProcessInstanceId());

        } catch (Exception e) {
            log.error("Error handling itself failed: {}", e.getMessage(), e);
            // Throw BPMN error to escalate
            throw new BpmnError("ERROR_HANDLING_FAILED",
                    "Failed to handle error: " + e.getMessage());
        }
    }

    /**
     * Determine which task caused the error
     */
    private String determineErrorSource(DelegateExecution execution) {
        // Check which error flow was taken
        String currentActivityId = execution.getCurrentActivityId();

        // Get the activity that triggered the error
        String errorSource = (String) execution.getVariable("errorSource");
        if (errorSource != null) {
            return errorSource;
        }

        // Try to determine from activity ID
        if (currentActivityId != null) {
            if (currentActivityId.contains("Ocr")) {
                return "OCR_PROCESSING";
            } else if (currentActivityId.contains("Verification")) {
                return "DOCUMENT_VERIFICATION";
            } else if (currentActivityId.contains("Credit")) {
                return "CREDIT_CHECK";
            } else if (currentActivityId.contains("Compliance")) {
                return "COMPLIANCE_CHECK";
            }
        }

        return "UNKNOWN";
    }

    /**
     * Create error record for logging and auditing
     */
    private Map<String, Object> createErrorRecord(
            String processInstanceId,
            String caseId,
            String applicantId,
            String errorSource,
            String errorCode,
            String errorMessage) {

        Map<String, Object> errorRecord = new HashMap<>();
        errorRecord.put("processInstanceId", processInstanceId);
        errorRecord.put("caseId", caseId);
        errorRecord.put("applicantId", applicantId);
        errorRecord.put("errorSource", errorSource);
        errorRecord.put("errorCode", errorCode != null ? errorCode : "UNKNOWN_ERROR");
        errorRecord.put("errorMessage", errorMessage != null ? errorMessage : "No error message provided");
        errorRecord.put("timestamp", Instant.now().toString());
        errorRecord.put("severity", determineSeverity(errorSource));
        errorRecord.put("retryable", isRetryable(errorSource));

        return errorRecord;
    }

    /**
     * Determine error severity
     */
    private String determineSeverity(String errorSource) {
        switch (errorSource) {
            case "OCR_PROCESSING":
            case "DOCUMENT_VERIFICATION":
                return "HIGH"; // Document issues are critical
            case "CREDIT_CHECK":
                return "MEDIUM"; // Can potentially proceed without credit check
            case "COMPLIANCE_CHECK":
                return "CRITICAL"; // Compliance is mandatory
            default:
                return "MEDIUM";
        }
    }

    /**
     * Determine if error is retryable
     */
    private boolean isRetryable(String errorSource) {
        // Most external service errors are retryable
        return !errorSource.equals("COMPLIANCE_CHECK"); // Compliance failures are usually not retryable
    }

    /**
     * Save error record to database
     * In production, implement actual database persistence
     */
    private void saveErrorRecord(Map<String, Object> errorRecord) {
        log.info("Saving error record: {}", errorRecord);

        // TODO: In production, save to database
        // errorRepository.save(ErrorEntity.from(errorRecord));

        // For now, just log
        log.info("Error record saved: processInstanceId={}, errorSource={}, severity={}",
                errorRecord.get("processInstanceId"),
                errorRecord.get("errorSource"),
                errorRecord.get("severity"));
    }

    /**
     * Send error notification to support team
     */
    private void sendErrorNotification(DelegateExecution execution, Map<String, Object> errorRecord) {
        try {
            String severity = (String) errorRecord.get("errorSource");

            // For critical errors, send immediate notification
            if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
                log.warn("CRITICAL ERROR - Sending notification to support team");

                // TODO: In production, send actual notification
                // notificationService.sendErrorAlert(errorRecord);

                // Simulate notification
                String notification = String.format(
                        "ALERT: Critical error in onboarding process\n" +
                                "Process: %s\n" +
                                "Case: %s\n" +
                                "Error Source: %s\n" +
                                "Error Message: %s\n" +
                                "Timestamp: %s",
                        errorRecord.get("processInstanceId"),
                        errorRecord.get("caseId"),
                        errorRecord.get("errorSource"),
                        errorRecord.get("errorMessage"),
                        errorRecord.get("timestamp"));

                log.warn("Error notification: {}", notification);
            }
        } catch (Exception e) {
            log.error("Failed to send error notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't stop error handling
        }
    }

    /**
     * Update case status in database
     */
    private void updateCaseStatus(String caseId, String status, String errorMessage) {
        try {
            log.info("Updating case {} status to {} with error: {}", caseId, status, errorMessage);

            // TODO: In production, update actual database
            // caseRepository.updateStatus(caseId, status, errorMessage);

            log.info("Case status updated successfully");
        } catch (Exception e) {
            log.error("Failed to update case status: {}", e.getMessage(), e);
            // Don't throw - status update failure shouldn't stop error handling
        }
    }
}
