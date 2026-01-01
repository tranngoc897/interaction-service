package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Credit Check Event - Published when credit bureau check is performed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCheckEvent {
    private String caseId;
    private int creditScore;
    private String creditRating;
    private String riskCategory;
    private boolean passed;
    private Map<String, Object> bureauDetails;
    private long timestamp;
}
