package com.ngoctran.interactionservice.bpmn;

import com.ngoctran.interactionservice.cases.CaseRepository;
import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import com.ngoctran.interactionservice.mapping.ProcessMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Event-Driven External Task Handler for Flowable.
 *
 * Instead of polling, this listens to Kafka events published by Flowable
 * when external jobs are created, providing real-time processing.
 *
 * Architecture: Flowable → Kafka Event → EventDrivenExternalWorker → Process Job
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventDrivenExternalWorker {

    private final RestTemplate restTemplate;
    private final WorkflowEventPublisher eventPublisher;
    private final CaseRepository caseRepository;
    private final ProcessMappingService processMappingService;

    // Metrics
    private final AtomicInteger jobsProcessed = new AtomicInteger(0);
    private final AtomicInteger jobsFailed = new AtomicInteger(0);

    // Should be configurable
    private final String flowableBaseUrl = "http://localhost:8080/flowable-rest";

    /**
     * Listen to Flowable external job creation events
     * Process jobs immediately when they're created
     */
    @KafkaListener(
        topics = "flowable-external-jobs",
        groupId = "external-worker-group",
        containerFactory = "concurrentKafkaListenerContainerFactory"
    )
    public void onExternalJobCreated(
            @Payload Map<String, Object> jobEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received external job event: {}", jobEvent);

            String jobId = (String) jobEvent.get("jobId");
            String topicName = (String) jobEvent.get("topic");
            String processInstanceId = (String) jobEvent.get("processInstanceId");

            if (jobId != null && topicName != null) {
                // Process job asynchronously for better throughput
                CompletableFuture.runAsync(() -> processExternalJob(jobId, topicName, processInstanceId))
                    .thenRun(() -> {
                        jobsProcessed.incrementAndGet();
                        acknowledgment.acknowledge();
                        log.debug("Successfully processed job: {}", jobId);
                    })
                    .exceptionally(throwable -> {
                        jobsFailed.incrementAndGet();
                        log.error("Failed to process job {}: {}", jobId, throwable.getMessage());
                        // Still acknowledge to prevent infinite retries
                        acknowledgment.acknowledge();
                        return null;
                    });
            } else {
                log.warn("Invalid job event received: {}", jobEvent);
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing job event: {}", e.getMessage(), e);
            acknowledgment.acknowledge(); // Prevent poison pill scenario
        }
    }

    /**
     * Batch processing for high-throughput scenarios
     */
    @KafkaListener(
        topics = "flowable-external-jobs",
        groupId = "external-worker-batch-group",
        containerFactory = "batchKafkaListenerContainerFactory",
        concurrency = "2"
    )
    public void onExternalJobsBatch(
            @Payload List<Map<String, Object>> jobEvents,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Processing batch of {} external job events", jobEvents.size());

            // Process all jobs in parallel for maximum throughput
            List<CompletableFuture<Void>> futures = jobEvents.stream()
                .map(jobEvent -> CompletableFuture.runAsync(() -> {
                    String jobId = (String) jobEvent.get("jobId");
                    String topicName = (String) jobEvent.get("topic");
                    String processInstanceId = (String) jobEvent.get("processInstanceId");

                    if (jobId != null && topicName != null) {
                        processExternalJob(jobId, topicName, processInstanceId);
                    }
                }))
                .toList();

            // Wait for all jobs to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    jobsProcessed.addAndGet(jobEvents.size());
                    acknowledgment.acknowledge();
                    log.debug("Successfully processed batch of {} jobs", jobEvents.size());
                })
                .exceptionally(throwable -> {
                    jobsFailed.addAndGet(jobEvents.size());
                    log.error("Batch processing failed: {}", throwable.getMessage());
                    acknowledgment.acknowledge();
                    return null;
                });

        } catch (Exception e) {
            log.error("Error in batch processing: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Core job processing logic
     */
    private void processExternalJob(String jobId, String topic, String processInstanceId) {
        try {
            log.info("Processing external job: {} for topic: {}", jobId, topic);

            // Get process variables
            Map<String, Object> variables = getProcessVariables(processInstanceId);

            // Handle based on topic
            Map<String, Object> resultVariables = handleTopic(topic, variables, processInstanceId);

            // Complete the job via REST API
            completeJob(jobId, resultVariables);

            log.info("Successfully completed external job: {}", jobId);

        } catch (Exception e) {
            log.error("Failed to process external job {}: {}", jobId, e.getMessage(), e);
            // Could implement dead letter topic publishing here
            failJob(jobId, e.getMessage());
        }
    }

    private Map<String, Object> getProcessVariables(String processInstanceId) {
        try {
            String url = flowableBaseUrl + "/process-instance/" + processInstanceId + "/variables";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.warn("Failed to get variables for process {}: {}", processInstanceId, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> handleTopic(String topic, Map<String, Object> variables, String processInstanceId) {
        String caseId = (String) variables.get("caseId");

        return switch (topic) {
            case "data-validation" -> handleDataValidation(caseId, processInstanceId);
            case "ocr-processing" -> handleOcrProcessing(caseId, processInstanceId);
            case "compliance-check" -> handleComplianceCheck(caseId, processInstanceId);
            case "account-creation" -> handleAccountCreation(caseId, variables, processInstanceId);
            case "email-notification" -> handleEmailNotification(caseId, processInstanceId);
            case "cleanup-data" -> handleCleanupData(caseId, variables, processInstanceId);
            default -> {
                log.warn("Unknown topic: {}", topic);
                yield Map.of();
            }
        };
    }

    private Map<String, Object> handleDataValidation(String caseId, String processInstanceId) {
        log.info("Processing Data Validation for Case: {}", caseId);

        Map<String, Object> variables = Map.of(
                "validationPassed", true,
                "validationTimestamp", Instant.now().toString());

        if (caseId != null) {
            eventPublisher.publishInteractionStepEvent(caseId, "onboarding", "DATA_VALIDATION", "COMPLETED", variables);
        }

        return variables;
    }

    private Map<String, Object> handleOcrProcessing(String caseId, String processInstanceId) {
        log.info("Processing OCR for Case: {}", caseId);

        Map<String, Object> variables = Map.of(
                "ocrCompleted", true,
                "ocrStatus", "SUCCESS");

        if (caseId != null) {
            eventPublisher.publishPerformanceEvent(caseId, "OCR_PROCESSING", 500, "SUCCESS");
        }

        return variables;
    }

    private Map<String, Object> handleComplianceCheck(String caseId, String processInstanceId) {
        log.info("Processing Compliance Check for Case: {}", caseId);

        Map<String, Object> variables = Map.of("complianceStatus", "PASSED");

        if (caseId != null) {
            eventPublisher.publishComplianceEvent(caseId, "N/A", "AML_CHECK", "PASSED",
                    Map.of("reason", "Applicant is clear"));
        }

        return variables;
    }

    private Map<String, Object> handleAccountCreation(String caseId, Map<String, Object> variables, String processInstanceId) {
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

    private Map<String, Object> handleEmailNotification(String caseId, String processInstanceId) {
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

    private Map<String, Object> handleCleanupData(String caseId, Map<String, Object> variables, String processInstanceId) {
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
            throw new RuntimeException("Job completion failed", e);
        }
    }

    private void failJob(String jobId, String errorMessage) {
        try {
            String url = flowableBaseUrl + "/external-job-api/jobs/" + jobId + "/fail";
            Map<String, Object> errorData = Map.of(
                "errorMessage", errorMessage,
                "retries", 0
            );
            restTemplate.postForObject(url, errorData, Void.class);
            log.warn("Marked job {} as failed: {}", jobId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to mark job {} as failed: {}", jobId, e.getMessage());
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

    /**
     * Get processing metrics
     */
    public Map<String, Object> getMetrics() {
        int total = jobsProcessed.get() + jobsFailed.get();
        return Map.of(
            "jobsProcessed", jobsProcessed.get(),
            "jobsFailed", jobsFailed.get(),
            "totalJobs", total,
            "successRate", total > 0 ? (double) jobsProcessed.get() / total * 100 : 0.0
        );
    }
}
