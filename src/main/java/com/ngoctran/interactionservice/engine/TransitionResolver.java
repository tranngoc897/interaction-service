package com.ngoctran.interactionservice.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.domain.StateContext;
import com.ngoctran.interactionservice.domain.Transition;
import com.ngoctran.interactionservice.repo.StateContextRepository;
import com.ngoctran.interactionservice.repo.TransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransitionResolver {

    private final TransitionRepository transitionRepository;
    private final StateContextRepository stateContextRepository;
    private final RuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    public Transition resolve(String flowVersion, String fromState, String action, UUID instanceId) {
        Transition transition = transitionRepository.findById(
                new com.ngoctran.interactionservice.domain.TransitionId(flowVersion, fromState, action)
        ).orElseThrow(() ->
                new IllegalStateException(
                        "No transition found: " + flowVersion + " | " + fromState + " | " + action
                )
        );

        // Evaluate conditions if present
        if (transition.getConditionsJson() != null && !transition.getConditionsJson().trim().isEmpty()) {
            try {
                List<String> conditions = objectMapper.readValue(
                        transition.getConditionsJson(),
                        new TypeReference<List<String>>() {}
                );

                if (!conditions.isEmpty()) {
                    Map<String, Object> context = getWorkflowContext(instanceId);
                    boolean conditionsMet = ruleEngine.evaluateConditions(
                            conditions.toArray(new String[0]), context
                    );

                    if (!conditionsMet) {
                        throw new IllegalStateException(
                                "Transition conditions not met for action: " + action +
                                " | Conditions: " + conditions
                        );
                    }
                }
            } catch (Exception ex) {
                log.error("Error evaluating conditions for transition: {} -> {} -> {}",
                        fromState, action, transition.getToState(), ex);
                throw new IllegalStateException("Failed to evaluate transition conditions", ex);
            }
        }

        return transition;
    }

    /**
     * Legacy method for backward compatibility
     */
    public Transition resolve(String flowVersion, String fromState, String action) {
        return transitionRepository.findById(
                new com.ngoctran.interactionservice.domain.TransitionId(flowVersion, fromState, action)
        ).orElseThrow(() ->
                new IllegalStateException(
                        "No transition found: " + flowVersion + " | " + fromState + " | " + action
                )
        );
    }

    private Map<String, Object> getWorkflowContext(UUID instanceId) {
        StateContext context = stateContextRepository.findById(instanceId).orElse(null);
        if (context != null && context.getContextData() != null) {
            try {
                return objectMapper.readValue(context.getContextData(), Map.class);
            } catch (Exception ex) {
                log.warn("Failed to parse context data for instance: {}", instanceId, ex);
            }
        }
        return Map.of(); // Empty context
    }
}
