package com.ngoctran.interactionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ngoctran.interactionservice.domain.OutboxEvent;
import com.ngoctran.interactionservice.repo.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbox Service for guaranteed event delivery
 * Implements the Outbox Pattern to ensure events are published reliably
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Store event in outbox for guaranteed delivery
     */
    @Transactional
    public void storeEvent(String eventId, String topic, String key, Object payload, String eventType) throws JsonProcessingException {
        try {
            // Serialize payload to JSON
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .topic(topic)
                    .partitionKey(key)
                    .eventPayload(payloadJson)
                    .eventType(eventType)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Stored event {} in outbox for topic {}", eventId, topic);

        } catch (Exception ex) {
            log.error("Failed to store event {} in outbox: {}", eventId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Publish pending events to Kafka
     * Runs every 10 seconds
     */
    @Scheduled(fixedDelayString = "${outbox.publish.interval:10000}")
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();

            if (pendingEvents.isEmpty()) {
                return;
            }

            // Limit batch size for performance (process first 50)
            int batchSize = Math.min(pendingEvents.size(), 50);
            List<OutboxEvent> eventsToProcess = pendingEvents.subList(0, batchSize);

            log.debug("Found {} pending events, processing batch of {}", pendingEvents.size(), batchSize);

            int successCount = 0;
            int errorCount = 0;

            for (OutboxEvent event : eventsToProcess) {
                try {
                    // Deserialize payload from JSON
                    Object payload = deserializePayload(event.getEventPayload());

                    // Publish to Kafka
                    kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload)
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Failed to publish event {} to Kafka: {}", event.getEventId(), ex.getMessage());
                                    handlePublishFailure(event);
                                } else {
                                    log.debug("Successfully published event {} to topic {}", event.getEventId(), event.getTopic());
                                    markEventPublished(event);
                                }
                            });

                    successCount++;

                } catch (Exception ex) {
                    errorCount++;
                    log.error("Error publishing event {}: {}", event.getEventId(), ex.getMessage(), ex);
                    handlePublishFailure(event);
                }
            }

            log.info("Outbox publish completed: {} successful, {} errors", successCount, errorCount);

        } catch (Exception ex) {
            log.error("Error in outbox publish scheduler", ex);
        }
    }

    /**
     * Handle successful event publication
     */
    @Transactional
    public void markEventPublished(OutboxEvent event) {
        try {
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (Exception ex) {
            log.error("Failed to mark event {} as published: {}", event.getEventId(), ex.getMessage());
        }
    }

    /**
     * Handle failed event publication
     */
    @Transactional
    public void handlePublishFailure(OutboxEvent event) {
        try {
            event.setRetryCount(event.getRetryCount() + 1);

            if (event.getRetryCount() >= 5) { // Max retries
                event.setStatus("FAILED");
                log.error("Event {} failed permanently after {} retries", event.getEventId(), event.getRetryCount());
            } else {
                event.setStatus("PENDING");
                // Event will be retried on next scheduler run
                log.warn("Event {} scheduled for retry {} (next scheduler run)", event.getEventId(), event.getRetryCount());
            }

            outboxEventRepository.save(event);

        } catch (Exception ex) {
            log.error("Failed to handle publish failure for event {}: {}", event.getEventId(), ex.getMessage());
        }
    }

    /**
     * Deserialize payload from JSON string
     */
    private Object deserializePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, Object.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize payload: {}", ex.getMessage());
            // Return the JSON string as fallback
            return payloadJson;
        }
    }

    /**
     * Get outbox statistics for monitoring
     */
    public OutboxStats getStats() {
        long pending = outboxEventRepository.countByStatus("PENDING");
        long published = outboxEventRepository.countByStatus("PUBLISHED");
        long failed = outboxEventRepository.countByStatus("FAILED");

        return new OutboxStats(pending, published, failed);
    }

    public static class OutboxStats {
        public final long pending;
        public final long published;
        public final long failed;

        public OutboxStats(long pending, long published, long failed) {
            this.pending = pending;
            this.published = published;
            this.failed = failed;
        }
    }
}
