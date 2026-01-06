package com.ngoctran.interactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.domain.OutboxEvent;
import com.ngoctran.interactionservice.repo.OutboxEventRepository;
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
 * Implements the outbox pattern to prevent dual writes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Store event in outbox (called within transaction)
     */
    @Transactional
    public void storeEvent(String eventId, String topic, String partitionKey,
                          Object payload, String eventType) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .topic(topic)
                    .partitionKey(partitionKey)
                    .eventPayload(payloadJson)
                    .eventType(eventType)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Stored event {} in outbox for topic {}", eventId, topic);

        } catch (Exception ex) {
            log.error("Failed to store event {} in outbox: {}", eventId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to store event in outbox", ex);
        }
    }

    /**
     * Publish pending events to Kafka
     * Runs every 10 seconds
     */
    @Scheduled(fixedDelayString = "${outbox.publish.interval:10000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Publishing {} pending events from outbox", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Parse payload
                Object payload = objectMapper.readValue(event.getEventPayload(), Object.class);

                // Send to Kafka
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                // Mark as published
                                markEventPublished(event.getId());
                                log.debug("Successfully published event {} to topic {}",
                                        event.getEventId(), event.getTopic());
                            } else {
                                // Increment retry count
                                incrementRetryCount(event.getId());
                                log.warn("Failed to publish event {} to topic {}: {}",
                                        event.getEventId(), event.getTopic(), ex.getMessage());
                            }
                        });

            } catch (Exception ex) {
                incrementRetryCount(event.getId());
                log.error("Error processing outbox event {}: {}", event.getId(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * Clean up old published events
     * Runs daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void cleanupOldEvents() {
        Instant cutoff = Instant.now().minusSeconds(7 * 24 * 60 * 60); // 7 days ago
        int deletedCount = outboxEventRepository.deletePublishedEventsOlderThan(cutoff);
        log.info("Cleaned up {} old published events from outbox", deletedCount);
    }

    private void markEventPublished(Long eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
        });
    }

    private void incrementRetryCount(Long eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() + 1 : 1);
            // Mark as failed if too many retries
            if (event.getRetryCount() >= 5) {
                event.setStatus("FAILED");
            }
            outboxEventRepository.save(event);
        });
    }
}
