package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.service.BackpressureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Backpressure Monitoring Controller
 * Provides endpoints to monitor system load and capacity
 */
@Slf4j
@RestController
@RequestMapping("/admin/backpressure")
@RequiredArgsConstructor
public class BackpressureController {

    private final BackpressureService backpressureService;

    /**
     * Get current backpressure metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<BackpressureService.BackpressureMetrics> getMetrics() {
        BackpressureService.BackpressureMetrics metrics = backpressureService.getMetrics();
        log.debug("Backpressure metrics requested: load={}%", metrics.getLoadPercentage());
        return ResponseEntity.ok(metrics);
    }

    /**
     * Health check endpoint for load balancer
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean healthy = backpressureService.isHealthy();
        BackpressureService.BackpressureMetrics metrics = backpressureService.getMetrics();

        Map<String, Object> response = Map.of(
                "healthy", healthy,
                "status", healthy ? "UP" : "DEGRADED",
                "loadPercentage", metrics.getLoadPercentage(),
                "activeWorkflows", metrics.getActiveWorkflows(),
                "maxWorkflows", metrics.getMaxConcurrentWorkflows(),
                "message", healthy ? "System operating normally" : "System under high load");

        if (healthy) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response); // Service Unavailable
        }
    }

    /**
     * Get detailed capacity information
     */
    @GetMapping("/capacity")
    public ResponseEntity<Map<String, Object>> getCapacity() {
        BackpressureService.BackpressureMetrics metrics = backpressureService.getMetrics();

        Map<String, Object> capacity = Map.of(
                "workflows", Map.of(
                        "max", metrics.getMaxConcurrentWorkflows(),
                        "active", metrics.getActiveWorkflows(),
                        "available", metrics.getAvailableWorkflowSlots(),
                        "utilizationPercent", metrics.getLoadPercentage()),
                "steps", Map.of(
                        "max", metrics.getMaxConcurrentSteps(),
                        "available", metrics.getAvailableStepSlots(),
                        "utilizationPercent",
                        (double) (metrics.getMaxConcurrentSteps() - metrics.getAvailableStepSlots())
                                / metrics.getMaxConcurrentSteps() * 100),
                "rejected", Map.of(
                        "total", metrics.getTotalRejected(),
                        "message", metrics.getTotalRejected() > 0
                                ? "System has rejected requests due to overload"
                                : "No requests rejected"));

        return ResponseEntity.ok(capacity);
    }
}
