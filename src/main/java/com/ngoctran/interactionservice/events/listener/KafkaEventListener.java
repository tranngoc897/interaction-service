package com.ngoctran.interactionservice.events.listener;

import com.ngoctran.interactionservice.events.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener that consumes events from Kafka topics.
 * This demonstrates how external services (or the same service in a distributed
 * setup)
 * can react to workflow events.
 */
@Component
@Slf4j
public class KafkaEventListener {

    @KafkaListener(topics = "workflow-state-events", groupId = "interaction-service-group")
    public void listenWorkflowState(WorkflowStateEvent event) {
        log.info("Received Kafka WorkflowStateEvent: id={}, state={}", event.getWorkflowId(), event.getNewState());
        // Potential logic: Trigger external system, update caches, etc.
    }

    @KafkaListener(topics = "milestone-events", groupId = "interaction-service-group")
    public void listenMilestone(MilestoneEvent event) {
        log.info("Received Kafka MilestoneEvent: caseId={}, milestone={}", event.getCaseId(), event.getMilestoneKey());
    }

    @KafkaListener(topics = "compliance-events", groupId = "interaction-service-group")
    public void listenCompliance(ComplianceEvent event) {
        log.info("Received Kafka ComplianceEvent: caseId={}, status={}", event.getCaseId(), event.getStatus());
    }

    @KafkaListener(topics = "case-update-events", groupId = "interaction-service-group")
    public void listenCaseUpdate(CaseUpdateEvent event) {
        log.info("Received Kafka CaseUpdateEvent: caseId={}, definition={}", event.getCaseId(),
                event.getCaseDefinitionKey());
    }

    @KafkaListener(topics = "interaction-step-events", groupId = "interaction-service-group")
    public void listenInteractionStep(InteractionStepEvent event) {
        log.info("Received Kafka InteractionStepEvent: caseId={}, step={}", event.getCaseId(), event.getStepName());
    }
}
