package com.ngoctran.interactionservice.service;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.domain.Transition;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.engine.TransitionResolver;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SAGA Orchestrator - Handles compensation (rollback) of workflow steps
 * Similar to Temporal's SAGA pattern for distributed transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final OnboardingInstanceRepository instanceRepository;
    private final TransitionResolver transitionResolver;
    private final OnboardingEngine onboardingEngine;
    private final WorkflowHistoryService historyService;

    /**
     * Execute compensation (rollback) for a failed workflow.
     * Walks backward through completed steps and executes their compensation
     * actions.
     */
    @Transactional
    public void compensate(UUID instanceId, String failureReason) {
        log.warn("Starting SAGA compensation for instance {} due to: {}", instanceId, failureReason);

        OnboardingInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        // Record compensation start
        historyService.recordEvent(
                instanceId,
                "SAGA_COMPENSATION",
                "COMPENSATION_STARTED",
                Map.of(
                        "currentState", instance.getCurrentState(),
                        "reason", failureReason,
                        "timestamp", Instant.now().toString()),
                "SYSTEM");

        // Get the history of completed transitions
        List<Transition> completedTransitions = getCompletedTransitions(instance);

        if (completedTransitions.isEmpty()) {
            log.info("No completed transitions to compensate for instance {}", instanceId);
            return;
        }

        log.info("Found {} transitions to compensate for instance {}",
                completedTransitions.size(), instanceId);

        // Execute compensations in reverse order (LIFO - Last In First Out)
        Collections.reverse(completedTransitions);

        int compensatedCount = 0;
        for (Transition transition : completedTransitions) {
            if (transition.getCompensationAction() != null && !transition.getCompensationAction().isEmpty()) {
                try {
                    executeCompensation(instance, transition);
                    compensatedCount++;
                } catch (Exception e) {
                    log.error("Failed to execute compensation for transition {} -> {}: {}",
                            transition.getFromState(), transition.getToState(), e.getMessage(), e);
                    // Continue with other compensations even if one fails
                }
            } else {
                log.debug("No compensation action defined for transition {} -> {}",
                        transition.getFromState(), transition.getToState());
            }
        }

        // Mark instance as compensated
        instance.setStatus("COMPENSATED");
        instance.setUpdatedAt(Instant.now());
        instanceRepository.save(instance);

        // Record compensation completion
        historyService.recordEvent(
                instanceId,
                "SAGA_COMPENSATION",
                "COMPENSATION_COMPLETED",
                Map.of(
                        "compensatedSteps", compensatedCount,
                        "totalSteps", completedTransitions.size(),
                        "timestamp", Instant.now().toString()),
                "SYSTEM");

        log.info("SAGA compensation completed for instance {}. Compensated {}/{} steps",
                instanceId, compensatedCount, completedTransitions.size());
    }

    /**
     * Execute a single compensation action
     */
    private void executeCompensation(OnboardingInstance instance, Transition transition) {
        String compensationAction = transition.getCompensationAction();

        log.info("Executing compensation: {} for transition {} -> {}",
                compensationAction, transition.getFromState(), transition.getToState());

        // Record compensation event
        historyService.recordEvent(
                instance.getId(),
                "SAGA_COMPENSATION",
                compensationAction,
                Map.of(
                        "originalTransition", Map.of(
                                "from", transition.getFromState(),
                                "to", transition.getToState(),
                                "action", transition.getAction()),
                        "timestamp", Instant.now().toString()),
                "SYSTEM");

        // Create compensation command
        ActionCommand compensationCommand = ActionCommand.builder()
                .instanceId(instance.getId())
                .action(compensationAction)
                .actor("SYSTEM")
                .requestId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .build();

        // Execute compensation through engine
        try {
            onboardingEngine.handle(compensationCommand);
            log.info("Successfully executed compensation: {}", compensationAction);
        } catch (Exception e) {
            log.error("Compensation action {} failed: {}", compensationAction, e.getMessage());
            throw e;
        }
    }

    /**
     * Get the list of completed transitions for an instance by analyzing history
     */
    private List<Transition> getCompletedTransitions(OnboardingInstance instance) {
        // Get all state transitions from history
        List<com.ngoctran.interactionservice.domain.WorkflowEvent> events = historyService.getEventRepository()
                .findByInstanceIdOrderBySequenceNumberAsc(instance.getId());

        List<Transition> completedTransitions = new ArrayList<>();

        for (com.ngoctran.interactionservice.domain.WorkflowEvent event : events) {
            if ("STATE_TRANSITION".equals(event.getEventType())) {
                try {
                    // Parse the payload to get from/to states
                    Map<String, Object> payload = parsePayload(event.getPayload());
                    String fromState = (String) payload.get("from");
                    String toState = (String) payload.get("to");
                    String action = (String) payload.get("action");

                    // Resolve the transition to get compensation action
                    try {
                        Transition transition = transitionResolver.resolve(
                                instance.getFlowVersion(),
                                fromState,
                                action,
                                instance.getId());
                        completedTransitions.add(transition);
                    } catch (Exception e) {
                        log.warn("Could not resolve transition for compensation: {} -> {}",
                                fromState, toState);
                    }
                } catch (Exception e) {
                    log.warn("Could not parse event payload for compensation: {}", e.getMessage());
                }
            }
        }

        return completedTransitions;
    }

    /**
     * Parse JSON payload
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    payload,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            log.error("Failed to parse payload: {}", e.getMessage());
            return Map.of();
        }
    }
}
