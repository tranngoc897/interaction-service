package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * System Error Event - Published when technical errors occur in the workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemErrorEvent {
    private String caseId;
    private String processInstanceId;
    private String errorSource;
    private String errorCode;
    private String errorMessage;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private boolean retryable;
    private Map<String, Object> errorContext;
    private long timestamp;
}
