package com.ngoctran.interactionservice.events.listener;

import com.ngoctran.interactionservice.history.WorkflowHistoryEntity;
import com.ngoctran.interactionservice.history.WorkflowHistoryRepository;
import com.ngoctran.interactionservice.dto.WorkflowExecutionStatus;
import com.ngoctran.interactionservice.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Listener that captures workflow events and persists them to the history table
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowHistoryEventListener {

    private final WorkflowHistoryRepository historyRepository;

    @EventListener
    public void handleWorkflowStateEvent(WorkflowStateEvent event) {
        log.info("Handling WorkflowStateEvent for history: {}", event.getWorkflowId());

        WorkflowHistoryEntity history = WorkflowHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .workflowId(event.getWorkflowId())
                .workflowType(event.getWorkflowType())
                .action("STATE_CHANGE")
                .statusBefore(mapToStatus(event.getOldState()))
                .statusAfter(mapToStatus(event.getNewState()))
                .changedAt(LocalDateTime.now())
                .changeDetails(event.getContext())
                .reason("Workflow state changed from " + event.getOldState() + " to " + event.getNewState())
                .changedBy("SYSTEM")
                .build();

        historyRepository.save(history);
    }

    @EventListener
    public void handleMilestoneEvent(MilestoneEvent event) {
        log.info("Handling MilestoneEvent for history: {} - {}", event.getCaseId(), event.getMilestoneKey());

        WorkflowHistoryEntity history = WorkflowHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .workflowId(event.getCaseId())
                .workflowType("CASE_MILESTONE")
                .action(event.getEventType())
                .changedAt(LocalDateTime.now())
                .changeDetails(event.getEventData())
                .reason("Milestone " + event.getMilestoneKey() + " reached: " + event.getEventType())
                .changedBy("SYSTEM")
                .build();

        historyRepository.save(history);
    }

    @EventListener
    public void handleComplianceEvent(ComplianceEvent event) {
        log.info("Handling ComplianceEvent for history: {} - {}", event.getCaseId(), event.getCheckType());

        WorkflowHistoryEntity history = WorkflowHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .workflowId(event.getCaseId())
                .workflowType("COMPLIANCE_CHECK")
                .action("COMPLIANCE_" + event.getStatus())
                .changedAt(LocalDateTime.now())
                .changeDetails(event.getCheckResult())
                .reason("Compliance check " + event.getCheckType() + " completed with status: " + event.getStatus())
                .changedBy("SYSTEM")
                .metadata(java.util.Map.of("applicantId", event.getApplicantId()))
                .build();

        historyRepository.save(history);
    }

    @EventListener
    public void handleCaseUpdateEvent(CaseUpdateEvent event) {
        log.info("Handling CaseUpdateEvent for history: {}", event.getCaseId());

        WorkflowHistoryEntity history = WorkflowHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .workflowId(event.getCaseId())
                .workflowType("CASE_UPDATE")
                .action("CASE_UPDATED")
                .changedAt(LocalDateTime.now())
                .changeDetails(event.getChanges())
                .metadata(event.getCaseData())
                .reason("Case updated for definition: " + event.getCaseDefinitionKey())
                .changedBy("SYSTEM")
                .build();

        historyRepository.save(history);
    }

    @EventListener
    public void handleInteractionStepEvent(InteractionStepEvent event) {
        log.info("Handling InteractionStepEvent for history: {} - {}", event.getCaseId(), event.getStepName());

        WorkflowHistoryEntity history = WorkflowHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .workflowId(event.getCaseId())
                .workflowType("USER_INTERACTION")
                .action(event.getAction())
                .changedAt(LocalDateTime.now())
                .changeDetails(event.getStepData())
                .reason("User performed step: " + event.getStepName())
                .changedBy("USER_SESSION")
                .metadata(java.util.Map.of("interactionKey", event.getInteractionKey()))
                .build();

        historyRepository.save(history);
    }

    @EventListener
    public void handleJointAccountEvent(JointAccountEvent event) {
        log.info("Handling JointAccountEvent for history: {}", event.getCaseId());

        WorkflowHistoryEntity history = WorkflowHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .workflowId(event.getCaseId())
                .workflowType("JOINT_ACCOUNT")
                .action(event.getEventType())
                .changedAt(LocalDateTime.now())
                .changeDetails(event.getEventData())
                .reason("Joint account operation: " + event.getEventType())
                .changedBy("SYSTEM")
                .metadata(java.util.Map.of(
                        "primaryApplicantId", event.getPrimaryApplicantId(),
                        "coApplicantId", event.getCoApplicantId()))
                .build();

        historyRepository.save(history);
    }

    private WorkflowExecutionStatus mapToStatus(String state) {
        if (state == null)
            return null;
        try {
            return WorkflowExecutionStatus.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default mapping for custom states
            if ("RUNNING".equalsIgnoreCase(state))
                return WorkflowExecutionStatus.RUNNING;
            if ("STARTED".equalsIgnoreCase(state))
                return WorkflowExecutionStatus.RUNNING;
            if ("COMPLETED".equalsIgnoreCase(state))
                return WorkflowExecutionStatus.COMPLETED;
            if ("FAILED".equalsIgnoreCase(state))
                return WorkflowExecutionStatus.FAILED;
            if ("NONE".equalsIgnoreCase(state))
                return WorkflowExecutionStatus.INITIALIZED;
            return WorkflowExecutionStatus.INITIALIZED;
        }
    }
}
