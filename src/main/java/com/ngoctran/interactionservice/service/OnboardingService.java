package com.ngoctran.interactionservice.service;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Onboarding Service - Business logic layer for onboarding workflows
 * Provides high-level operations and orchestrates workflow execution
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingInstanceRepository instanceRepository;
    private final OnboardingEngine onboardingEngine;
    private final BackpressureService backpressureService;

    /**
     * Start a new onboarding workflow
     */
    @Transactional
    public OnboardingInstance start(String userId) {
        return start(userId, "v1");
    }

    /**
     * Start a new onboarding workflow with specific version
     * Handles both authenticated users and anonymous users
     */
    @Transactional
    public OnboardingInstance start(String userId, String flowVersion) {
        log.info("Starting onboarding for user: {}, flowVersion: {}", userId, flowVersion);

        // Acquire backpressure permit to prevent system overload
        try (BackpressureService.WorkflowPermit permit = backpressureService.acquireWorkflowPermit()) {

            // For anonymous users, allow multiple active sessions but limit them
            if (!userId.startsWith("anonymous:")) {
                // Check if authenticated user already has active onboarding
                List<OnboardingInstance> activeInstances = instanceRepository.findByUserIdAndStatus(userId, "ACTIVE");
                if (!activeInstances.isEmpty()) {
                    throw new IllegalStateException("User already has active onboarding: " + userId);
                }
            } else {
                // For anonymous users, limit concurrent sessions to prevent abuse
                List<OnboardingInstance> activeAnonymous = instanceRepository.findByUserIdAndStatus(userId, "ACTIVE");
                if (activeAnonymous.size() >= 3) { // Allow max 3 concurrent anonymous sessions
                    log.warn("Too many active anonymous sessions for user: {}, cleaning up old ones", userId);
                    // Clean up old anonymous sessions (older than 24 hours)
                    cleanupOldAnonymousSessions(userId);
                }
            }

            // Create new instance
            OnboardingInstance instance = OnboardingInstance.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .flowVersion(flowVersion)
                    .currentState("PHONE_ENTERED")
                    .status("ACTIVE")
                    .stateStartedAt(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .version(0L)
                    .build();

            OnboardingInstance saved = instanceRepository.save(instance);
            log.info("Created onboarding instance: {} for user: {}", saved.getId(), userId);

            return saved;

        } catch (BackpressureService.BackpressureException e) {
            log.warn("Workflow creation rejected due to backpressure: {}", e.getMessage());
            throw new IllegalStateException("System is currently overloaded. Please try again in a few moments.", e);
        }
    }

    /**
     * Clean up old anonymous sessions to prevent accumulation
     */
    private void cleanupOldAnonymousSessions(String anonymousUserId) {
        try {
            // Find sessions older than 24 hours
            Instant cutoffTime = Instant.now().minusSeconds(24 * 60 * 60); // 24 hours ago

            List<OnboardingInstance> oldSessions = instanceRepository
                    .findByUserId(anonymousUserId)
                    .stream()
                    .filter(instance -> instance.getCreatedAt().isBefore(cutoffTime))
                    .filter(instance -> "ACTIVE".equals(instance.getStatus())
                            || "CANCELLED".equals(instance.getStatus()))
                    .toList();

            if (!oldSessions.isEmpty()) {
                log.info("Cleaning up {} old anonymous sessions for user: {}", oldSessions.size(), anonymousUserId);
                for (OnboardingInstance oldSession : oldSessions) {
                    oldSession.setStatus("EXPIRED");
                    oldSession.setUpdatedAt(Instant.now());
                    instanceRepository.save(oldSession);
                }
            }
        } catch (Exception ex) {
            log.warn("Error cleaning up old anonymous sessions for user: {}", anonymousUserId, ex);
            // Don't fail the main operation for cleanup issues
        }
    }

    /**
     * Get onboarding status for user
     */
    public OnboardingStatus getStatus(UUID instanceId) {
        OnboardingInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        // Calculate progress based on current state
        int progress = calculateProgress(instance.getCurrentState());

        // Get available actions (simplified - in real implementation would check
        // transitions)
        List<String> allowedActions = getAllowedActions(instance.getCurrentState());

        return OnboardingStatus.builder()
                .instanceId(instance.getId())
                .userId(instance.getUserId())
                .currentState(instance.getCurrentState())
                .status(instance.getStatus())
                .progress(progress)
                .allowedActions(allowedActions)
                .stateStartedAt(instance.getStateStartedAt())
                .build();
    }

    /**
     * Perform an action on the onboarding workflow
     */
    @Transactional
    public void performAction(UUID instanceId, String action, String requestId) {
        log.info("Performing action: {} on instance: {}", action, instanceId);

        ActionCommand command = ActionCommand.builder()
                .instanceId(instanceId)
                .action(action)
                .actor("USER")
                .requestId(requestId != null ? requestId : UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .build();

        onboardingEngine.handle(command);
        log.info("Action completed: {} on instance: {}", action, instanceId);
    }

    /**
     * Cancel onboarding workflow
     */
    @Transactional
    public void cancel(UUID instanceId, String reason) {
        log.info("Cancelling onboarding instance: {} with reason: {}", instanceId, reason);

        OnboardingInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if ("COMPLETED".equals(instance.getStatus()) || "CANCELLED".equals(instance.getStatus())) {
            throw new IllegalStateException("Cannot cancel completed or already cancelled instance");
        }

        instance.setStatus("CANCELLED");
        instance.setUpdatedAt(Instant.now());
        instanceRepository.save(instance);

        log.info("Cancelled onboarding instance: {}", instanceId);
    }

    /**
     * Get all instances for a user
     */
    public List<OnboardingInstance> getUserInstances(String userId) {
        return instanceRepository.findByUserId(userId);
    }

    /**
     * Check if user has active onboarding
     */
    public boolean hasActiveOnboarding(String userId) {
        List<OnboardingInstance> active = instanceRepository.findByUserIdAndStatus(userId, "ACTIVE");
        return !active.isEmpty();
    }

    // Helper methods

    private int calculateProgress(String currentState) {
        switch (currentState) {
            case "PHONE_ENTERED":
                return 10;
            case "OTP_VERIFIED":
                return 25;
            case "PROFILE_COMPLETED":
                return 40;
            case "DOC_UPLOADED":
                return 55;
            case "EKYC_PENDING":
                return 70;
            case "EKYC_APPROVED":
                return 80;
            case "AML_PENDING":
                return 90;
            case "AML_CLEARED":
                return 95;
            case "ACCOUNT_CREATED":
                return 98;
            case "COMPLETED":
                return 100;
            default:
                return 0;
        }
    }

    private List<String> getAllowedActions(String currentState) {
        switch (currentState) {
            case "PHONE_ENTERED":
                return List.of("NEXT");
            case "OTP_VERIFIED":
                return List.of("NEXT");
            case "PROFILE_COMPLETED":
                return List.of("NEXT");
            case "DOC_UPLOADED":
                return List.of("NEXT");
            case "EKYC_APPROVED":
                return List.of("NEXT");
            case "AML_CLEARED":
                return List.of("NEXT");
            case "ACCOUNT_CREATED":
                return List.of("NEXT");
            default:
                return List.of();
        }
    }

    // DTO for status response
    public static class OnboardingStatus {
        private UUID instanceId;
        private String userId;
        private String currentState;
        private String status;
        private int progress;
        private List<String> allowedActions;
        private Instant stateStartedAt;

        // Constructor, getters, setters
        public OnboardingStatus() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private OnboardingStatus status = new OnboardingStatus();

            public Builder instanceId(UUID instanceId) {
                status.instanceId = instanceId;
                return this;
            }

            public Builder userId(String userId) {
                status.userId = userId;
                return this;
            }

            public Builder currentState(String currentState) {
                status.currentState = currentState;
                return this;
            }

            public Builder status(String statusValue) {
                status.status = statusValue;
                return this;
            }

            public Builder progress(int progress) {
                status.progress = progress;
                return this;
            }

            public Builder allowedActions(List<String> allowedActions) {
                status.allowedActions = allowedActions;
                return this;
            }

            public Builder stateStartedAt(Instant stateStartedAt) {
                status.stateStartedAt = stateStartedAt;
                return this;
            }

            public OnboardingStatus build() {
                return status;
            }
        }

        // Getters
        public UUID getInstanceId() {
            return instanceId;
        }

        public String getUserId() {
            return userId;
        }

        public String getCurrentState() {
            return currentState;
        }

        public String getStatus() {
            return status;
        }

        public int getProgress() {
            return progress;
        }

        public List<String> getAllowedActions() {
            return allowedActions;
        }

        public Instant getStateStartedAt() {
            return stateStartedAt;
        }
    }
}
