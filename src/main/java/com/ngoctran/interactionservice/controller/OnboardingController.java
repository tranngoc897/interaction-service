package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.dto.ActionRequest;
import com.ngoctran.interactionservice.dto.StartOnboardingRequest;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Start a new onboarding process for new/anonymous users
     * No userId required - generates anonymous session
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startOnboarding(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sessionId) {

        try {
            log.info("Starting onboarding - userId: {}, sessionId: {}", userId, sessionId);

            String effectiveUserId = userId;

            // For new users without userId, use sessionId or generate anonymous ID
            if (effectiveUserId == null || effectiveUserId.trim().isEmpty()) {
                if (sessionId != null && !sessionId.trim().isEmpty()) {
                    // Use sessionId as temporary user identifier
                    effectiveUserId = "anonymous:" + sessionId;
                    log.info("Using anonymous user ID: {}", effectiveUserId);
                } else {
                    // Generate completely anonymous ID
                    effectiveUserId = "anonymous:" + UUID.randomUUID().toString();
                    log.info("Generated anonymous user ID: {}", effectiveUserId);
                }
            }

            // Check if user already has active onboarding (only for non-anonymous users)
            if (!effectiveUserId.startsWith("anonymous:") && onboardingService.hasActiveOnboarding(effectiveUserId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Active onboarding already exists",
                        "message", "User already has an active onboarding process"
                ));
            }

            // Start new onboarding through service
            OnboardingInstance instance = onboardingService.start(effectiveUserId);

            log.info("Created onboarding instance {} for user {}", instance.getId(), effectiveUserId);

            return ResponseEntity.ok(Map.of(
                    "instanceId", instance.getId(),
                    "userId", effectiveUserId,
                    "currentState", instance.getCurrentState(),
                    "isAnonymous", effectiveUserId.startsWith("anonymous:"),
                    "message", "Onboarding started successfully"
            ));

        } catch (Exception ex) {
            log.error("Error starting onboarding: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to start onboarding",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Get current onboarding status
     */
    @GetMapping("/{instanceId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable UUID instanceId) {
        try {
            // Use service to get status with business logic
            OnboardingService.OnboardingStatus status = onboardingService.getStatus(instanceId);

            return ResponseEntity.ok(Map.of(
                    "instanceId", status.getInstanceId(),
                    "userId", status.getUserId(),
                    "currentState", status.getCurrentState(),
                    "status", status.getStatus(),
                    "progress", status.getProgress(),
                    "allowedActions", status.getAllowedActions(),
                    "stateStartedAt", status.getStateStartedAt()
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Instance not found: {}", instanceId);
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Error getting status for instance {}: {}", instanceId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get status",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Perform an action on the onboarding process
     */
    @PostMapping("/{instanceId}/action")
    public ResponseEntity<Map<String, Object>> performAction(
            @PathVariable UUID instanceId,
            @RequestBody Map<String, Object> request) {

        try {
            String action = (String) request.get("action");
            String requestId = (String) request.getOrDefault("requestId", UUID.randomUUID().toString());

            if (action == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Action is required"
                ));
            }

            log.info("Performing action {} on instance {}", action, instanceId);

            // Use service to perform action with business logic
            onboardingService.performAction(instanceId, action, requestId);

            // Get updated status after action
            OnboardingService.OnboardingStatus updatedStatus = onboardingService.getStatus(instanceId);

            return ResponseEntity.ok(Map.of(
                    "instanceId", instanceId,
                    "action", action,
                    "currentState", updatedStatus.getCurrentState(),
                    "progress", updatedStatus.getProgress(),
                    "message", "Action processed successfully"
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Instance not found: {}", instanceId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            log.warn("Invalid action for instance {}: {}", instanceId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid action",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error performing action on instance {}: {}", instanceId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to perform action",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Get available actions for current state
     */
    @GetMapping("/{instanceId}/actions")
    public ResponseEntity<Map<String, Object>> getAvailableActions(@PathVariable UUID instanceId) {
        try {
            // Use service to get status with allowed actions
            OnboardingService.OnboardingStatus status = onboardingService.getStatus(instanceId);

            // Convert allowed actions to UI format
            Map<String, Object> actions = convertActionsToUIFormat(status.getAllowedActions());

            return ResponseEntity.ok(Map.of(
                    "instanceId", instanceId,
                    "currentState", status.getCurrentState(),
                    "actions", actions
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Instance not found: {}", instanceId);
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Error getting actions for instance {}: {}", instanceId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get actions",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Cancel onboarding workflow
     */
    @PostMapping("/{instanceId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOnboarding(
            @PathVariable UUID instanceId,
            @RequestParam(defaultValue = "User requested cancellation") String reason) {

        try {
            log.info("Cancelling onboarding instance: {} with reason: {}", instanceId, reason);

            // Use service to cancel with business logic
            onboardingService.cancel(instanceId, reason);

            return ResponseEntity.ok(Map.of(
                    "instanceId", instanceId,
                    "status", "CANCELLED",
                    "message", "Onboarding cancelled successfully"
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Instance not found: {}", instanceId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            log.warn("Cannot cancel instance {}: {}", instanceId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot cancel",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error cancelling instance {}: {}", instanceId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to cancel",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Convert allowed actions from service to UI format
     */
    private Map<String, Object> convertActionsToUIFormat(java.util.List<String> allowedActions) {
        Map<String, Object> uiActions = new java.util.HashMap<>();

        for (String action : allowedActions) {
            switch (action) {
                case "NEXT":
                    uiActions.put("NEXT", Map.of(
                            "label", "Continue",
                            "type", "PRIMARY",
                            "requiresConfirmation", false
                    ));
                    break;
                // Add more action mappings as needed
                default:
                    uiActions.put(action, Map.of(
                            "label", action,
                            "type", "SECONDARY",
                            "requiresConfirmation", false
                    ));
            }
        }

        return uiActions;
    }
}
