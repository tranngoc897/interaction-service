package com.ngoctran.interactionservice.events;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Milestone Event - Published when milestones are reached
 */
@Data
@Builder
public class MilestoneEvent {
    private String caseId;
    private String milestoneKey;
    private String eventType; // STARTED, COMPLETED, FAILED
    private Map<String, Object> eventData;
    private long timestamp;
}
