package com.ngoctran.interactionservice.events.listener;

import com.ngoctran.interactionservice.events.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized Kafka Event Listener with batch processing, error handling, and metrics
 */
@Component
@Slf4j
public class OptimizedKafkaEventListener {

    private final AtomicInteger messagesProcessed = new AtomicInteger(0);
    private final AtomicInteger messagesFailed = new AtomicInteger(0);
    private final AtomicInteger batchesProcessed = new AtomicInteger(0);

    /**
     * Batch processing for workflow state events - High throughput
     */
    @KafkaListener(
        topics = "workflow-state-events",
        groupId = "interaction-service-optimized",
        containerFactory = "batchKafkaListenerContainerFactory",
        concurrency = "3"
    )
    public void listenWorkflowStateBatch(
            @Payload List<WorkflowStateEvent> events,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Processing batch of {} workflow state events", events.size());

            // Process events in batch
            for (WorkflowStateEvent event : events) {
                processWorkflowStateEvent(event);
                messagesProcessed.incrementAndGet();
            }

            batchesProcessed.incrementAndGet();

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

            log.debug("Successfully processed batch of {} events from topics: {}", events.size(), topics);

        } catch (Exception e) {
            messagesFailed.addAndGet(events.size());
            log.error("Failed to process batch of {} events: {}", events.size(), e.getMessage(), e);

            // Don't acknowledge failed batches - they will be retried
            // acknowledgment.nack(); // If using manual nack support
        }
    }

    /**
     * Single message processing for critical events - Low latency
     */
    @KafkaListener(
        topics = "system-error-events",
        groupId = "interaction-service-critical",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenSystemErrors(
            @Payload SystemErrorEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.error("Processing critical system error event: {}", event);

            // Immediate processing for critical errors
            processSystemError(event);
            messagesProcessed.incrementAndGet();

            acknowledgment.acknowledge();

        } catch (Exception e) {
            messagesFailed.incrementAndGet();
            log.error("Failed to process critical error event: {}", e.getMessage(), e);
            // Let it retry or go to dead letter topic
        }
    }

    /**
     * Filtered listener - Only process high-priority compliance events
     */
    @KafkaListener(
        topics = "compliance-events",
        groupId = "interaction-service-filtered",
        containerFactory = "filteringKafkaListenerContainerFactory",
        filter = "complianceEventFilter"
    )
    public void listenHighPriorityCompliance(@Payload ComplianceEvent event) {
        log.info("Processing high-priority compliance event: caseId={}, status={}",
                event.getCaseId(), event.getStatus());

        processComplianceEvent(event);
        messagesProcessed.incrementAndGet();
    }

    /**
     * Dead letter topic processing for failed messages
     */
    @KafkaListener(
        topics = "workflow-events.DLT",
        groupId = "interaction-service-dlt",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenDeadLetterTopic(@Payload String failedMessage) {
        log.warn("Processing failed message from DLT: {}", failedMessage);

        // Implement dead letter processing logic
        // - Store in database for manual review
        // - Send alerts to operations team
        // - Attempt manual reprocessing
    }

    /**
     * Business logic processing methods
     */
    private void processWorkflowStateEvent(WorkflowStateEvent event) {
        // Implement your business logic here
        log.info("Workflow state changed: {} -> {} (ID: {})",
                event.getOldState(), event.getNewState(), event.getWorkflowId());

        // Example: Update workflow status in database
        // Example: Trigger downstream processes
        // Example: Send notifications
    }

    private void processSystemError(SystemErrorEvent event) {
        // Critical error processing
        log.error("System error detected: {} - {} (Severity: {})",
                event.getErrorCode(), event.getErrorMessage(), event.getSeverity());

        // Example: Send alerts to monitoring systems
        // Example: Create incident tickets
        // Example: Trigger failover procedures
    }

    private void processComplianceEvent(ComplianceEvent event) {
        // High-priority compliance processing
        log.info("Compliance check completed: {} - {} (Case: {})",
                event.getCheckType(), event.getStatus(), event.getCaseId());

        // Example: Update compliance status
        // Example: Trigger next workflow steps
        // Example: Send notifications to compliance officers
    }

    /**
     * Get listener metrics
     */
    public java.util.Map<String, Object> getMetrics() {
        return java.util.Map.of(
            "messagesProcessed", messagesProcessed.get(),
            "messagesFailed", messagesFailed.get(),
            "batchesProcessed", batchesProcessed.get(),
            "successRate", messagesProcessed.get() > 0 ?
                (double) messagesProcessed.get() / (messagesProcessed.get() + messagesFailed.get()) * 100 : 0.0
        );
    }

    /**
     * Reset metrics (for testing)
     */
    public void resetMetrics() {
        messagesProcessed.set(0);
        messagesFailed.set(0);
        batchesProcessed.set(0);
    }
}
