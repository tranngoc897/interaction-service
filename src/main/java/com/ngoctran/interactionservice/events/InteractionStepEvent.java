package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Interaction Step Event - Published when interaction steps are executed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionStepEvent {
    private String caseId;
    private String interactionKey;
    private String stepName;
    private String action; // STARTED, COMPLETED, FAILED, SKIPPED
    private Map<String, Object> stepData;
    private long timestamp;
}
