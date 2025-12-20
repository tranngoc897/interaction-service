package com.ngoctran.interactionservice.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single entry in the step history (stored in flow_case.audit_trail)
 * This captures what the user actually did at each step
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepHistoryEntry {
    
    /**
     * Name of the step that was completed
     */
    private String stepName;
    
    /**
     * Status of the step: "COMPLETED", "SKIPPED", "FAILED"
     */
    private String status;
    
    /**
     * When the step was started
     */
    private Instant startedAt;
    
    /**
     * When the step was completed
     */
    private Instant completedAt;
    
    /**
     * Data submitted by the user at this step
     * This is a snapshot of what was entered
     */
    private Map<String, Object> data;
    
    /**
     * Any validation errors encountered
     */
    private Map<String, String> errors;
    
    /**
     * Additional metadata (e.g., IP address, device info)
     */
    private Map<String, Object> metadata;
}
