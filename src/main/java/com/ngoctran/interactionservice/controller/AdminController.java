package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/onboarding")
@RequiredArgsConstructor
public class AdminController {

    private final OnboardingEngine onboardingEngine;
    private final OnboardingInstanceRepository instanceRepository;

    /**
     * Get dashboard summary
     */
/*    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        try {
            // Get counts by status
            Map<String, Long> statusCounts = Map.of(
                    "ACTIVE", instanceRepository.countByStatus("ACTIVE"),
                    "COMPLETED", instanceRepository.countByStatus("COMPLETED"),
                    "CANCELLED", instanceRepository.countByStatus("CANCELLED"),
                    "FAILED", instanceRepository.countByStatus("FAILED")
            );

            // Get counts by current state
            List<Object[]> stateCounts = instanceRepository.findAll().stream()
                    .collect(Collectors.groupingBy(OnboardingInstance::getCurrentState))
                    .entrySet().stream()
                    .map(entry -> new Object[]{entry.getKey(), (long) entry.getValue().size()})
                    .collect(Collectors.toList());

            Map<String, Long> stateCountMap = stateCounts.stream()
                    .collect(Collectors.toMap(
                            arr -> (String) arr[0],
                            arr -> (Long) arr[1]
                    ));

            return ResponseEntity.ok(Map.of(
                    "totalInstances", instanceRepository.count(),
                    "statusCounts", statusCounts,
                    "stateCounts", stateCountMap,
                    "activeRate", calculateActiveRate(statusCounts)
            ));

        } catch (Exception ex) {
            log.error("Error getting dashboard data: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get dashboard data",
                    "message", ex.getMessage()
            ));
        }
    }*/

    /**
     * List onboarding instances with filtering
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String userId) {

        try {
            Pageable pageable = PageRequest.of(page, size);

            // This is simplified - in real implementation, you'd have a custom repository method
            // with proper filtering and pagination
            List<OnboardingInstance> instances = instanceRepository.findAll();

            // Apply filters
            if (status != null) {
                instances = instances.stream()
                        .filter(inst -> status.equals(inst.getStatus()))
                        .collect(Collectors.toList());
            }
            if (state != null) {
                instances = instances.stream()
                        .filter(inst -> state.equals(inst.getCurrentState()))
                        .collect(Collectors.toList());
            }
            if (userId != null) {
                instances = instances.stream()
                        .filter(inst -> userId.equals(inst.getUserId()))
                        .collect(Collectors.toList());
            }

            // Apply pagination
            int totalElements = instances.size();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), totalElements);
            List<OnboardingInstance> pageContent = instances.subList(start, end);

            List<Map<String, Object>> instanceSummaries = pageContent.stream()
                    .map(this::toInstanceSummary)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "instances", instanceSummaries,
                    "totalElements", totalElements,
                    "totalPages", (totalElements + size - 1) / size,
                    "currentPage", page,
                    "size", size
            ));

        } catch (Exception ex) {
            log.error("Error listing instances: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to list instances",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Get detailed instance information
     */
    @GetMapping("/{instanceId}")
    public ResponseEntity<Map<String, Object>> getInstanceDetails(@PathVariable UUID instanceId) {
        try {
            OnboardingInstance instance = instanceRepository.findById(instanceId).orElse(null);
            if (instance == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of(
                    "instance", toInstanceDetail(instance),
                    "actions", getAvailableAdminActions(instance)
            ));

        } catch (Exception ex) {
            log.error("Error getting instance details for {}: {}", instanceId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get instance details",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Admin action on instance
     */
    @PostMapping("/{instanceId}/action")
    public ResponseEntity<Map<String, Object>> performAdminAction(
            @PathVariable UUID instanceId,
            @RequestBody Map<String, Object> request) {

        try {
            String action = (String) request.get("action");
            String operator = (String) request.getOrDefault("operator", "admin");
            String comment = (String) request.get("comment");

            if (action == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Action is required"
                ));
            }

            log.info("Admin {} performing action {} on instance {}", operator, action, instanceId);

            // Create admin action command
            ActionCommand command = ActionCommand.admin(instanceId, action, UUID.randomUUID().toString(), operator);

            // Process through engine
            onboardingEngine.handle(command);

            // Get updated instance
            OnboardingInstance updated = instanceRepository.findById(instanceId).orElse(null);

            return ResponseEntity.ok(Map.of(
                    "instanceId", instanceId,
                    "action", action,
                    "operator", operator,
                    "currentState", updated != null ? updated.getCurrentState() : "UNKNOWN",
                    "message", "Admin action processed successfully"
            ));

        } catch (IllegalStateException ex) {
            log.warn("Invalid admin action for instance {}: {}", instanceId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid action",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error performing admin action on instance {}: {}", instanceId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to perform admin action",
                    "message", ex.getMessage()
            ));
        }
    }

    private Map<String, Object> toInstanceSummary(OnboardingInstance instance) {
        return Map.of(
                "id", instance.getId(),
                "userId", instance.getUserId(),
                "currentState", instance.getCurrentState(),
                "status", instance.getStatus(),
                "flowVersion", instance.getFlowVersion(),
                "stateStartedAt", instance.getStateStartedAt(),
                "createdAt", instance.getCreatedAt()
        );
    }

    private Map<String, Object> toInstanceDetail(OnboardingInstance instance) {
        return Map.of(
                "id", instance.getId(),
                "userId", instance.getUserId(),
                "currentState", instance.getCurrentState(),
                "status", instance.getStatus(),
                "flowVersion", instance.getFlowVersion(),
                "version", instance.getVersion(),
                "stateStartedAt", instance.getStateStartedAt(),
                "createdAt", instance.getCreatedAt(),
                "updatedAt", instance.getUpdatedAt()
        );
    }

    private List<Map<String, Object>> getAvailableAdminActions(OnboardingInstance instance) {
        // Simplified - in real implementation, query transition table for admin-allowed actions
        return List.of(
                Map.of("action", "RETRY", "label", "Retry Current Step", "type", "SECONDARY"),
                Map.of("action", "TIMEOUT", "label", "Force Timeout", "type", "WARNING"),
                Map.of("action", "CANCEL", "label", "Cancel Onboarding", "type", "DANGER")
        );
    }

    private double calculateActiveRate(Map<String, Long> statusCounts) {
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return 0.0;
        return (double) statusCounts.getOrDefault("ACTIVE", 0L) / total * 100.0;
    }
}
