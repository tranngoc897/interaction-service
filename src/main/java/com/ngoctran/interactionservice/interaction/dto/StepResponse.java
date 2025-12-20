package com.ngoctran.interactionservice.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO sent to frontend containing current step information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepResponse {
    
    /**
     * Interaction instance ID
     */
    private String interactionId;
    
    /**
     * Current step name (from flw_int.step_name)
     */
    private String stepName;
    
    /**
     * Current step status: "PENDING", "IN_PROGRESS", "COMPLETED", "WAITING_SYSTEM"
     */
    private String stepStatus;
    
    /**
     * Overall interaction status: "ACTIVE", "WAITING_SYSTEM", "COMPLETED", "FAILED"
     */
    private String interactionStatus;
    
    /**
     * Step configuration from the blueprint (flw_int_def.steps)
     */
    private StepDefinition stepDefinition;
    
    /**
     * Pre-filled data for this step (if resuming or editing)
     */
    private Map<String, Object> stepData;
    
    /**
     * Whether the interaction can be resumed later
     */
    private Boolean resumable;
    
    /**
     * Case ID associated with this interaction
     */
    private String caseId;
    
    /**
     * SLA Status: "ON_TIME", "NEARLY_OVERDUE", "OVERDUE"
     */
    private String slaStatus;

    /**
     * Progress information
     */
    private ProgressInfo progress;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfo {
        private Integer currentStepIndex;
        private Integer totalSteps;
        private Integer percentComplete;
    }
}
