package com.ngoctran.interactionservice.service;

import com.ngoctran.interactionservice.domain.WorkflowMetrics;
import com.ngoctran.interactionservice.repo.WorkflowMetricsRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowMetricsService {

    private final MeterRegistry meterRegistry;
    private final WorkflowMetricsRepository metricsRepository;

    // State transition counters
    private final Map<String, Counter> stateTransitionCounters = new ConcurrentHashMap<>();

    // Step execution timers
    private final Map<String, Timer> stepExecutionTimers = new ConcurrentHashMap<>();

    // Error counters by type
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();

    // Active instances gauge
    private final AtomicLong activeInstances = new AtomicLong(0);

    // Retry counters
    private final Map<String, Counter> retryCounters = new ConcurrentHashMap<>();

    public void recordStateTransition(String fromState, String toState, String action) {
        String metricName = "workflow.state.transition";
        Counter counter = stateTransitionCounters.computeIfAbsent(
                metricName + "." + fromState + "." + toState,
                key -> Counter.builder(metricName)
                        .tag("from_state", fromState)
                        .tag("to_state", toState)
                        .tag("action", action)
                        .register(meterRegistry)
        );
        counter.increment();

        // Persist to database for long-term storage
        saveMetric("state_transition", fromState, 1L, Map.of(
                "to_state", toState,
                "action", action
        ));
    }

    public void recordStepExecution(String state, long durationMs, boolean success) {
        String metricName = "workflow.step.execution";
        Timer timer = stepExecutionTimers.computeIfAbsent(
                metricName + "." + state,
                key -> Timer.builder(metricName)
                        .tag("state", state)
                        .tag("result", success ? "success" : "failure")
                        .register(meterRegistry)
        );
        timer.record(java.time.Duration.ofMillis(durationMs));

        // Persist to database
        saveMetric("step_execution", state, durationMs, Map.of(
                "result", success ? "success" : "failure"
        ));
    }

    public void recordError(String state, String errorCode, String errorType) {
        String metricName = "workflow.error";
        Counter counter = errorCounters.computeIfAbsent(
                metricName + "." + errorCode,
                key -> Counter.builder(metricName)
                        .tag("state", state)
                        .tag("error_code", errorCode)
                        .tag("error_type", errorType)
                        .register(meterRegistry)
        );
        counter.increment();

        // Persist to database
        saveMetric("error", state, 1L, Map.of(
                "error_code", errorCode,
                "error_type", errorType
        ));
    }

    public void recordRetry(String state, int retryCount) {
        String metricName = "workflow.retry";
        Counter counter = retryCounters.computeIfAbsent(
                metricName + "." + state,
                key -> Counter.builder(metricName)
                        .tag("state", state)
                        .register(meterRegistry)
        );
        counter.increment();

        // Persist to database
        saveMetric("retry", state, (long) retryCount, Map.of());
    }

    public void updateActiveInstances(long count) {
        activeInstances.set(count);
        Gauge.builder("workflow.instances.active", activeInstances, AtomicLong::get)
                .register(meterRegistry);
    }

    public void recordHumanTaskEvent(String taskType, String action) {
        Counter.builder("workflow.human_task")
                .tag("task_type", taskType)
                .tag("action", action)
                .register(meterRegistry)
                .increment();

        saveMetric("human_task", taskType, 1L, Map.of("action", action));
    }

    public void recordIncidentEvent(String severity, String status) {
        Counter.builder("workflow.incident")
                .tag("severity", severity)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        saveMetric("incident", severity, 1L, Map.of("status", status));
    }

    public void recordKafkaEvent(String topic, String eventType, boolean success) {
        Counter.builder("workflow.kafka.event")
                .tag("topic", topic)
                .tag("event_type", eventType)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();

        saveMetric("kafka_event", topic, 1L, Map.of(
                "event_type", eventType,
                "result", success ? "success" : "failure"
        ));
    }

    /**
     * Get workflow health metrics
     */
    public Map<String, Object> getHealthMetrics() {
        return Map.of(
                "activeInstances", activeInstances.get(),
                "totalStateTransitions", getTotalStateTransitions(),
                "errorRate", calculateErrorRate(),
                "averageStepDuration", getAverageStepDuration(),
                "retryRate", getRetryRate()
        );
    }

    /**
     * Get performance metrics for monitoring
     */
    public Map<String, Object> getPerformanceMetrics() {
        return Map.of(
                "stepExecutionStats", getStepExecutionStats(),
                "errorStats", getErrorStats(),
                "throughput", getThroughputMetrics()
        );
    }

    private void saveMetric(String metricName, String state, Long value, Map<String, String> tags) {
        try {
            WorkflowMetrics metric = WorkflowMetrics.builder()
                    .metricName(metricName)
                    .state(state)
                    .value(value)
                    .tags(tags)
                    .recordedAt(Instant.now())
                    .build();

            metricsRepository.save(metric);
        } catch (Exception ex) {
            log.warn("Failed to persist metric {}: {}", metricName, ex.getMessage());
        }
    }

    private long getTotalStateTransitions() {
        // This would query the metrics repository for aggregated data
        return 0L; // Placeholder
    }

    private double calculateErrorRate() {
        // Calculate error rate from recent metrics
        return 0.0; // Placeholder
    }

    private double getAverageStepDuration() {
        // Calculate average step duration
        return 0.0; // Placeholder
    }

    private double getRetryRate() {
        // Calculate retry rate
        return 0.0; // Placeholder
    }

    private Map<String, Object> getStepExecutionStats() {
        return Map.of(); // Placeholder
    }

    private Map<String, Object> getErrorStats() {
        return Map.of(); // Placeholder
    }

    private Map<String, Object> getThroughputMetrics() {
        return Map.of(); // Placeholder
    }
}
