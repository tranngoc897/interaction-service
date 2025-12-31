package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Milestone Event - Published when milestones are reached
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneEvent {
    private String caseId;
    private String milestoneKey;
    private String eventType; // STARTED, COMPLETED, FAILED
    private Map<String, Object> eventData;
    private long timestamp;
}
