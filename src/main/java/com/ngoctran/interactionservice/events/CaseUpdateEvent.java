package com.ngoctran.interactionservice.events;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Case Update Event - Published when case data is updated
 */
@Data
@Builder
public class CaseUpdateEvent {
    private String caseId;
    private String caseDefinitionKey;
    private Map<String, Object> caseData;
    private Map<String, Object> changes;
    private long timestamp;
}
