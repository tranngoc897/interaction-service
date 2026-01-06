package com.ngoctran.interactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for starting a new onboarding workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartOnboardingRequest {

    private String userId;
    private String flowVersion = "v1";
    private Map<String, Object> initialData;

    public StartOnboardingRequest(String userId) {
        this.userId = userId;
        this.flowVersion = "v1";
    }
}
