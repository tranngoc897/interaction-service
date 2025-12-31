package com.ngoctran.interactionservice.events;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Joint Account Event - Published for joint account operations
 */
@Data
@Builder
public class JointAccountEvent {
    private String caseId;
    private String primaryApplicantId;
    private String coApplicantId;
    private String eventType; // INVITATION_SENT, ACCEPTED, JOINED, COMPLETED
    private Map<String, Object> eventData;
    private long timestamp;
}
