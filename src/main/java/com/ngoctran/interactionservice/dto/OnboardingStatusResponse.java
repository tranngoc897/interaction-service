package com.ngoctran.interactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for onboarding status queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStatusResponse {

    private UUID instanceId;
    private String userId;
    private String currentState;
    private String status;
    private int progress;
    private String uiStep;
    private List<ActionDto> allowedActions;
    private ErrorDto error;
    private RetryInfo retryInfo;
    private Instant stateStartedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Nested DTOs
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionDto {
        private String action;
        private String label;
        private String type; // PRIMARY, SECONDARY, WARNING
        private boolean requiresConfirmation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDto {
        private String code;
        private String message;
        private String type; // BUSINESS, SYSTEM, TRANSIENT
        private Instant timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetryInfo {
        private int count;
        private int maxRetries;
        private Instant nextRetryAt;
        private String lastError;
    }
}
