package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Performance Metrics Event - Published to track execution time of workflow
 * steps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsEvent {
    private String caseId;
    private String stepName;
    private long durationMs;
    private String status; // SUCCESS, FAILED
    private long timestamp;
}
