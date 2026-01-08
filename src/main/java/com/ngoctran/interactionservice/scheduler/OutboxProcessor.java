package com.ngoctran.interactionservice.scheduler;

import com.ngoctran.interactionservice.domain.OutboxEvent;
import com.ngoctran.interactionservice.repo.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Processor to poll outbox_event table and publish to Kafka.
 * This ensures "Transactional Outbox" pattern - events are only sent if DB
 * transaction succeeded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${scheduler.outbox-interval:5000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} pending events in outbox", events.size());

        for (OutboxEvent event : events) {
            try {
                log.debug("Publishing outbox event: {} to topic: {}", event.getEventId(), event.getTopic());

                // Publish to Kafka
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getEventPayload())
                        .get(2, java.util.concurrent.TimeUnit.SECONDS);

                // Mark as published
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getEventId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                }
                outboxRepository.save(event);
            }
        }
    }
}
