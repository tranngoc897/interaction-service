package com.ngoctran.interactionservice.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event Publisher - Publishes workflow events using Spring Events and Kafka
 * Similar to ABB onboarding's event-driven architecture
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish milestone event
     */
    public void publishMilestoneEvent(String caseId, String milestoneKey, String eventType, Map<String, Object> eventData) {
        log.info("Publishing milestone event: caseId={}, milestoneKey={}, eventType={}", caseId, milestoneKey, eventType);

        MilestoneEvent event = MilestoneEvent.builder()
                .caseId(caseId)
                .milestoneKey(milestoneKey)
                .eventType(eventType)
                .eventData(eventData)
                .timestamp(System.currentTimeMillis())
                .build();

        // Publish via Spring Events
        eventPublisher.publishEvent(event);

        // Publish to Kafka
        try {
            kafkaTemplate.send("milestone-events", caseId, event);
            log.debug("Published milestone event to Kafka: {}", event);
        } catch (Exception e) {
            log.warn("Failed to publish milestone event to Kafka: {}", e.getMessage());
        }
    }

    /**
     * Publish workflow state change event
     */
    public void publishWorkflowStateEvent(String workflowId, String workflowType, String oldState, String newState, Map<String, Object> context) {
        log.info("Publishing workflow state event: workflowId={}, {} -> {}", workflowId, oldState, newState);

        WorkflowStateEvent event = WorkflowStateEvent.builder()
                .workflowId(workflowId)
                .workflowType(workflowType)
                .oldState(oldState)
                .newState(newState)
                .context(context)
                .timestamp(System.currentTimeMillis())
                .build();

        // Publish via Spring Events
        eventPublisher.publishEvent(event);

        // Publish to Kafka
        try {
            kafkaTemplate.send("workflow-state-events", workflowId, event);
            log.debug("Published workflow state event to Kafka: {}", event);
        } catch (Exception e) {
            log.warn("Failed to publish workflow state event to Kafka: {}", e.getMessage());
        }
    }

    /**
     * Publish compliance check event
     */
    public void publishComplianceEvent(String caseId, String applicantId, String checkType, String status, Map<String, Object> checkResult) {
        log.info("Publishing compliance event: caseId={}, applicantId={}, checkType={}, status={}", caseId, applicantId, checkType, status);

        ComplianceEvent event = ComplianceEvent.builder()
                .caseId(caseId)
                .applicantId(applicantId)
                .checkType(checkType)
                .status(status)
                .checkResult(checkResult)
                .timestamp(System.currentTimeMillis())
                .build();

        // Publish via Spring Events
        eventPublisher.publishEvent(event);

        // Publish to Kafka
        try {
            kafkaTemplate.send("compliance-events", caseId + "-" + applicantId, event);
            log.debug("Published compliance event to Kafka: {}", event);
        } catch (Exception e) {
            log.warn("Failed to publish compliance event to Kafka: {}", e.getMessage());
        }
    }

    /**
     * Publish joint account event
     */
    public void publishJointAccountEvent(String caseId, String primaryApplicantId, String coApplicantId, String eventType, Map<String, Object> eventData) {
        log.info("Publishing joint account event: caseId={}, primary={}, coApplicant={}, eventType={}",
                caseId, primaryApplicantId, coApplicantId, eventType);

        JointAccountEvent event = JointAccountEvent.builder()
                .caseId(caseId)
                .primaryApplicantId(primaryApplicantId)
                .coApplicantId(coApplicantId)
                .eventType(eventType)
                .eventData(eventData)
                .timestamp(System.currentTimeMillis())
                .build();

        // Publish via Spring Events
        eventPublisher.publishEvent(event);

        // Publish to Kafka
        try {
            kafkaTemplate.send("joint-account-events", caseId, event);
            log.debug("Published joint account event to Kafka: {}", event);
        } catch (Exception e) {
            log.warn("Failed to publish joint account event to Kafka: {}", e.getMessage());
        }
    }

    /**
     * Publish case update event
     */
    public void publishCaseUpdateEvent(String caseId, String caseDefinitionKey, Map<String, Object> caseData, Map<String, Object> changes) {
        log.info("Publishing case update event: caseId={}, caseDefinitionKey={}", caseId, caseDefinitionKey);

        CaseUpdateEvent event = CaseUpdateEvent.builder()
                .caseId(caseId)
                .caseDefinitionKey(caseDefinitionKey)
                .caseData(caseData)
                .changes(changes)
                .timestamp(System.currentTimeMillis())
                .build();

        // Publish via Spring Events
        eventPublisher.publishEvent(event);

        // Publish to Kafka
        try {
            kafkaTemplate.send("case-update-events", caseId, event);
            log.debug("Published case update event to Kafka: {}", event);
        } catch (Exception e) {
            log.warn("Failed to publish case update event to Kafka: {}", e.getMessage());
        }
    }

    /**
     * Publish interaction step event
     */
    public void publishInteractionStepEvent(String caseId, String interactionKey, String stepName, String action, Map<String, Object> stepData) {
        log.info("Publishing interaction step event: caseId={}, interactionKey={}, stepName={}, action={}",
                caseId, interactionKey, stepName, action);

        InteractionStepEvent event = InteractionStepEvent.builder()
                .caseId(caseId)
                .interactionKey(interactionKey)
                .stepName(stepName)
                .action(action)
                .stepData(stepData)
                .timestamp(System.currentTimeMillis())
                .build();

        // Publish via Spring Events
        eventPublisher.publishEvent(event);

        // Publish to Kafka
        try {
            kafkaTemplate.send("interaction-step-events", caseId + "-" + stepName, event);
            log.debug("Published interaction step event to Kafka: {}", event);
        } catch (Exception e) {
            log.warn("Failed to publish interaction step event to Kafka: {}", e.getMessage());
        }
    }
}
