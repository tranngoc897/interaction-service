package com.ngoctran.interactionservice.bpmn;

import com.ngoctran.interactionservice.cases.CaseRepository;
import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import com.ngoctran.interactionservice.mapping.ProcessMappingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flowable External Task Handler using REST API polling.
 * Since Flowable doesn't have external workers like Camunda,
 * we poll for external tasks and handle them via REST API.
 */
@Component
@RequiredArgsConstructor
public class FlowableExternalWorker {

    private static final Logger log = LoggerFactory.getLogger(FlowableExternalWorker.class);

    private final RestTemplate restTemplate;
    private final WorkflowEventPublisher eventPublisher;
    private final CaseRepository caseRepository;
    private final ProcessMappingService processMappingService;
    private final String flowableBaseUrl = "http://localhost:8080/flowable-rest"; // Should be configurable

    /**
     * Poll for external tasks every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
    public void pollExternalTasks() {
        try {
            // Get external tasks from Flowable REST API
            String url = flowableBaseUrl + "/external-job-api/jobs";
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> jobs = restTemplate.getForObject(url, List.class);

            if (jobs != null && !jobs.isEmpty()) {
                for (Map<String, Object> job : jobs) {
                    handleExternalJob(job);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to poll external tasks: {}", e.getMessage());
        }
    }

    private void handleExternalJob(Map<String, Object> job) {
        String jobId = (String) job.get("id");
        String topic = (String) job.get("topic");
        String processInstanceId = (String) job.get("processInstanceId");

        try {
            log.info("Processing external job: {} for topic: {}", jobId, topic);

            // Get process variables
            String variablesUrl = flowableBaseUrl + "/process-instance/" + processInstanceId + "/variables";
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = restTemplate.getForObject(variablesUrl, Map.class);

            // Handle based on topic
            Map<String, Object> resultVariables = handleTopic(topic, variables);

            // Complete the job
            completeJob(jobId, resultVariables);

        } catch (Exception e) {
            log.error("Failed to handle external job {}: {}", jobId, e.getMessage(), e);
            // Could implement retry logic here
        }
    }

    private Map<String, Object> handleTopic(String topic, Map<String, Object> variables) {
        String caseId = (String) variables.get("caseId");

        switch (topic) {
            case "data-validation":
                return handleDataValidation(caseId);
            case "ocr-processing":
                return handleOcrProcessing(caseId);
            case "compliance-check":
                return handleComplianceCheck(caseId);
            case "account-creation":
                return handleAccountCreation(caseId, variables);
            case "email-notification":
                return handleEmailNotification(caseId, variables);
            case "cleanup-data":
                return handleCleanupData(caseId, variables);
            default:
                log.warn("Unknown topic: {}", topic);
                return Map.of();
        }
    }

    private Map<String, Object> handleDataValidation(String caseId) {
        log.info("Processing Data Validation for Case: {}", caseId);

        Map<String, Object> variables = Map.of(
                "validationPassed", true,
                "validationTimestamp", Instant.now().toString());

        if (caseId != null) {
            eventPublisher.publishInteractionStepEvent(caseId, "onboarding", "DATA_VALIDATION", "COMPLETED", variables);
        }

        return variables;
    }

    private Map<String, Object> handleOcrProcessing(String caseId) {
        log.info("Processing OCR for Case: {}", caseId);

        Map<String, Object> variables = Map.of(
                "ocrCompleted", true,
                "ocrStatus", "SUCCESS");

        if (caseId != null) {
            eventPublisher.publishPerformanceEvent(caseId, "OCR_PROCESSING", 500, "SUCCESS");
        }

        return variables;
    }

    private Map<String, Object> handleComplianceCheck(String caseId) {
        log.info("Processing Compliance Check for Case: {}", caseId);

        Map<String, Object> variables = Map.of("complianceStatus", "PASSED");

        if (caseId != null) {
            eventPublisher.publishComplianceEvent(caseId, "N/A", "AML_CHECK", "PASSED",
                    Map.of("reason", "Applicant is clear"));
        }

        return variables;
    }

    private Map<String, Object> handleAccountCreation(String caseId, Map<String, Object> variables) {
        log.info("Processing Account Creation for Case: {}", caseId);

        String accountNumber = "ACC" + System.currentTimeMillis();

        if (caseId != null) {
            caseRepository.findById(UUID.fromString(caseId)).ifPresent(caseEntity -> {
                caseEntity.setStatus("APPROVED");
                caseRepository.save(caseEntity);
                log.info("Case {} updated to APPROVED", caseId);
            });

            eventPublisher.publishAccountCreatedEvent(caseId, UUID.randomUUID().toString(), accountNumber,
                    "Customer", "SAVINGS");
            eventPublisher.publishCaseUpdateEvent(caseId, "onboarding", Map.of("accountNumber", accountNumber),
                    Map.of("status", "APPROVED"));
        }

        return Map.of("accountNumber", accountNumber, "accountCreated", true);
    }

    private Map<String, Object> handleEmailNotification(String caseId, Map<String, Object> variables) {
        String processInstanceId = (String) variables.get("processInstanceId");
        log.info("Finalizing Workflow for Case: {}", caseId);

        if (caseId != null) {
            eventPublisher.publishWorkflowStateEvent(processInstanceId, "onboarding", "RUNNING", "COMPLETED",
                    Map.of("caseId", caseId));
        }

        try {
            processMappingService.markProcessCompleted(processInstanceId);
        } catch (Exception e) {
            log.warn("Could not mark process mapping as completed: {}", e.getMessage());
        }

        return Map.of();
    }

    private Map<String, Object> handleCleanupData(String caseId, Map<String, Object> variables) {
        String processInstanceId = (String) variables.get("processInstanceId");
        log.info("Processing Global Cancel Cleanup for Case: {}", caseId);

        try {
            String applicantId = (String) variables.get("applicantId");

            Map<String, Object> cleanupRecord = Map.of(
                    "processInstanceId", processInstanceId,
                    "caseId", caseId,
                    "applicantId", applicantId,
                    "cleanupType", "GLOBAL_CANCEL",
                    "timestamp", Instant.now().toString(),
                    "status", "COMPLETED"
            );

            eventPublisher.publishSystemErrorEvent(caseId, processInstanceId,
                    "GLOBAL_CANCEL", "CANCEL_REQUESTED", "Process cancelled by global cancel message",
                    "INFO", false, cleanupRecord);

            updateCaseStatus(caseId, "CANCELLED", "Process cancelled globally");

            return Map.of(
                    "cancelled", true,
                    "cancelTimestamp", Instant.now().toString(),
                    "cleanupRecord", cleanupRecord
            );

        } catch (Exception e) {
            log.error("Global cancel cleanup failed: {}", e.getMessage(), e);
            return Map.of("cleanupFailed", true);
        }
    }

    private void completeJob(String jobId, Map<String, Object> variables) {
        try {
            String url = flowableBaseUrl + "/external-job-api/jobs/" + jobId + "/complete";
            restTemplate.postForObject(url, variables, Void.class);
            log.info("Completed external job: {}", jobId);
        } catch (Exception e) {
            log.error("Failed to complete job {}: {}", jobId, e.getMessage());
        }
    }

    private void updateCaseStatus(String caseId, String status, String reason) {
        try {
            log.info("Updating case {} status to {} with reason: {}", caseId, status, reason);
            // TODO: Implement actual status update
        } catch (Exception e) {
            log.error("Failed to update case status: {}", e.getMessage());
        }
    }
}
