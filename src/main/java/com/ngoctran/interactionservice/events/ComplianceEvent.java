package com.ngoctran.interactionservice.events;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Compliance Event - Published when compliance checks are performed
 */
@Data
@Builder
public class ComplianceEvent {
    private String caseId;
    private String applicantId;
    private String checkType;
    private String status;
    private Map<String, Object> checkResult;
    private long timestamp;
}
