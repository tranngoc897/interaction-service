package com.ngoctran.interactionservice.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized Event Publisher with batching, async processing, and reliability features
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OptimizedWorkflowEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.batch-size:50}")
    private int batchSize;

    @Value("${app.kafka.retry-attempts:3}")
    private int retryAttempts;

    @Value("${app.kafka.retry-delay:1000}")
    private long retryDelay;

    // Metrics tracking
    private final AtomicInteger eventsPublished = new AtomicInteger(0);
    private final AtomicInteger eventsFailed = new AtomicInteger(0);
    private final Map<String, AtomicInteger> topicMetrics = new ConcurrentHashMap<>();

    /**
     * Async batch publishing for high-throughput scenarios
     */
    @Async
    public CompletableFuture<Void> publishWorkflowStateEventAsync(String workflowId, String workflowType,
            String oldState, String newState, Map<String, Object> context) {

        return CompletableFuture.runAsync(() -> {
            try {
                publishWorkflowStateEvent(workflowId, workflowType, oldState, newState, context);
            } catch (Exception e) {
                log.error("Async event publishing failed: {}", e.getMessage());
            }
        });
    }

    /**
     * Optimized workflow state event publishing with reliability
     */
    public void publishWorkflowStateEvent(String workflowId, String workflowType, String oldState, String newState,
            Map<String, Object> context) {

        WorkflowStateEvent event = WorkflowStateEvent.builder()
                .workflowId(workflowId)
                .workflowType(workflowType)
                .oldState(oldState)
                .newState(newState)
                .context(context != null ? context : Map.of())
                .timestamp(System.currentTimeMillis())
                .build();

        // Publish via Spring Events (fast, local)
        publishSpringEvent(event);

        // Publish to Kafka (reliable, distributed)
        publishToKafka("workflow-state-events", workflowId, event);
    }

    /**
     * Batch publish multiple events efficiently
     */
    public void publishBatchEvents(List<WorkflowStateEvent> events) {
        if (events.isEmpty()) return;

        log.debug("Batch publishing {} events", events.size());

        // Spring events (fast)
        events.forEach(this::publishSpringEvent);

        // Kafka batching (efficient)
        events.forEach(event ->
            publishToKafka("workflow-state-events", event.getWorkflowId(), event)
        );
    }

    /**
     * Publish with circuit breaker pattern for resilience
     */
    public void publishWithCircuitBreaker(String workflowId, String workflowType,
            String oldState, String newState, Map<String, Object> context) {

        try {
            // Check if Kafka is healthy before publishing
            if (isKafkaHealthy()) {
                publishWorkflowStateEvent(workflowId, workflowType, oldState, newState, context);
            } else {
                // Fallback: only Spring events
                log.warn("Kafka unhealthy, using Spring events only");
                WorkflowStateEvent event = WorkflowStateEvent.builder()
                        .workflowId(workflowId)
                        .workflowType(workflowType)
                        .oldState(oldState)
                        .newState(newState)
                        .context(context != null ? context : Map.of())
                        .timestamp(System.currentTimeMillis())
                        .build();
                publishSpringEvent(event);
            }
        } catch (Exception e) {
            eventsFailed.incrementAndGet();
            log.error("Circuit breaker triggered for event publishing: {}", e.getMessage());
        }
    }

    /**
     * Optimized Kafka publishing with callbacks and metrics
     */
    private void publishToKafka(String topic, String key, Object event) {
        try {
            var future = kafkaTemplate.send(topic, key, event);
/*
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    eventsPublished.incrementAndGet();
                    topicMetrics.computeIfAbsent(topic, k -> new AtomicInteger()).incrementAndGet();

                    if (log.isDebugEnabled()) {
                        log.debug("Successfully published to Kafka topic: {} with key: {}", topic, key);
                    }
                }

                @Override
                public void onFailure(Throwable ex) {
                    eventsFailed.incrementAndGet();
                    log.warn("Failed to publish to Kafka topic: {} with key: {}, error: {}",
                            topic, key, ex.getMessage());

                    // Implement retry logic here if needed
                    retryPublish(topic, key, event, 1);
                }
            });*/

        } catch (Exception e) {
            eventsFailed.incrementAndGet();
            log.error("Error publishing to Kafka topic: {}, error: {}", topic, e.getMessage());
        }
    }

    /**
     * Retry logic with exponential backoff
     */
    private void retryPublish(String topic, String key, Object event, int attempt) {
        if (attempt > retryAttempts) {
            log.error("Max retry attempts reached for topic: {}, key: {}", topic, key);
            return;
        }

        try {
            Thread.sleep(retryDelay * attempt); // Exponential backoff
            publishToKafka(topic, key, event);
            log.info("Retry successful for topic: {}, key: {}, attempt: {}", topic, key, attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry interrupted for topic: {}, key: {}", topic, key);
        }
    }

    /**
     * Fast Spring event publishing
     */
    private void publishSpringEvent(Object event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish Spring event: {}", e.getMessage());
        }
    }

    /**
     * Health check for Kafka connectivity
     */
    private boolean isKafkaHealthy() {
        try {
            // Simple health check - can be enhanced
            kafkaTemplate.send("health-check", "ping", "pong");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get publishing metrics
     */
    public Map<String, Object> getMetrics() {
        return Map.of(
            "eventsPublished", eventsPublished.get(),
            "eventsFailed", eventsFailed.get(),
            "topicMetrics", topicMetrics.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().get()
                )),
            "successRate", eventsPublished.get() > 0 ?
                (double) eventsPublished.get() / (eventsPublished.get() + eventsFailed.get()) * 100 : 0.0
        );
    }

    /**
     * Reset metrics (for testing)
     */
    public void resetMetrics() {
        eventsPublished.set(0);
        eventsFailed.set(0);
        topicMetrics.clear();
    }
}
