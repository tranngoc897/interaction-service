package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
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

    private final OnboardingEngine onboardingEngine;
    private final OnboardingInstanceRepository instanceRepository;

    /**
     * Start a new onboarding process
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startOnboarding(@RequestParam String userId) {
        try {
            log.info("Starting onboarding for user: {}", userId);

            // Create new instance
            OnboardingInstance instance = OnboardingInstance.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .flowVersion("v1")
                    .currentState("PHONE_ENTERED")
                    .status("ACTIVE")
                    .build();

            OnboardingInstance saved = instanceRepository.save(instance);

            log.info("Created onboarding instance {} for user {}", saved.getId(), userId);

            return ResponseEntity.ok(Map.of(
                    "instanceId", saved.getId(),
                    "currentState", saved.getCurrentState(),
                    "message", "Onboarding started successfully"
            ));

        } catch (Exception ex) {
            log.error("Error starting onboarding for user {}: {}", userId, ex.getMessage(), ex);
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
            OnboardingInstance instance = instanceRepository.findById(instanceId).orElse(null);
            if (instance == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of(
                    "instanceId", instance.getId(),
                    "currentState", instance.getCurrentState(),
                    "status", instance.getStatus(),
                    "flowVersion", instance.getFlowVersion(),
                    "stateStartedAt", instance.getStateStartedAt()
            ));

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

            // Create action command
            ActionCommand command = ActionCommand.user(instanceId, action, requestId);

            // Process through engine
            onboardingEngine.handle(command);

            // Get updated status
            OnboardingInstance updated = instanceRepository.findById(instanceId).orElse(null);

            return ResponseEntity.ok(Map.of(
                    "instanceId", instanceId,
                    "action", action,
                    "currentState", updated != null ? updated.getCurrentState() : "UNKNOWN",
                    "message", "Action processed successfully"
            ));

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
            OnboardingInstance instance = instanceRepository.findById(instanceId).orElse(null);
            if (instance == null) {
                return ResponseEntity.notFound().build();
            }

            // In a real implementation, this would query the transition table
            // For now, return mock actions based on state
            Map<String, Object> actions = getMockActionsForState(instance.getCurrentState());

            return ResponseEntity.ok(Map.of(
                    "instanceId", instanceId,
                    "currentState", instance.getCurrentState(),
                    "actions", actions
            ));

        } catch (Exception ex) {
            log.error("Error getting actions for instance {}: {}", instanceId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get actions",
                    "message", ex.getMessage()
            ));
        }
    }

    private Map<String, Object> getMockActionsForState(String state) {
        // Mock implementation - in real system, query transition table
        switch (state) {
            case "PHONE_ENTERED":
                return Map.of("NEXT", Map.of("label", "Continue", "type", "PRIMARY"));
            case "OTP_VERIFIED":
                return Map.of("NEXT", Map.of("label", "Continue", "type", "PRIMARY"));
            case "DOC_UPLOADED":
                return Map.of("NEXT", Map.of("label", "Submit Documents", "type", "PRIMARY"));
            case "EKYC_PENDING":
                return Map.of(); // No user actions, waiting for async
            case "EKYC_APPROVED":
                return Map.of("NEXT", Map.of("label", "Continue", "type", "PRIMARY"));
            case "AML_PENDING":
                return Map.of(); // No user actions, waiting for async
            case "AML_CLEARED":
                return Map.of("NEXT", Map.of("label", "Continue", "type", "PRIMARY"));
            case "ACCOUNT_CREATED":
                return Map.of("NEXT", Map.of("label", "Complete", "type", "PRIMARY"));
            default:
                return Map.of();
        }
    }
}
