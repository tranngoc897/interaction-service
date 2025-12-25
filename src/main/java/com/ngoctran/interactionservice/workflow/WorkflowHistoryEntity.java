package com.ngoctran.interactionservice.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Workflow History Entity
 *
 * Based on SchedulerInstructionHistoryChangeEntity pattern
 * Tracks all state changes and actions performed on workflows
 */
@Document(collection = "workflow_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryEntity {

    @Id
    private String historyId;

    @Indexed
    private String workflowId;

    @Indexed
    private String workflowType; // KYC_ONBOARDING, PAYMENT, RECONCILIATION, etc.

    private String action; // START, SIGNAL, COMPLETE, FAIL, CANCEL, SUSPEND, RESUME

    private WorkflowExecutionStatus statusBefore;
    private WorkflowExecutionStatus statusAfter;

    private String changedBy; // userId or SYSTEM
    private LocalDateTime changedAt;

    private Map<String, Object> changeDetails; // Action-specific details

    private String reason; // Why the change was made
    private String notes; // Additional notes

    // Audit fields
    private String ipAddress;
    private String userAgent;
    private String sessionId;

    // Metadata for querying
    private Map<String, Object> metadata;

    // For optimistic locking
    private Long version;

    // ==================== Helper Methods ====================

    /**
     * Create a new history entry for workflow start
     */
    public static WorkflowHistoryEntity createWorkflowStart(String workflowId, String workflowType,
                                                          String startedBy, Map<String, Object> initialData) {
        return WorkflowHistoryEntity.builder()
                .historyId("HIST-" + workflowId + "-" + System.nanoTime())
                .workflowId(workflowId)
                .workflowType(workflowType)
                .action("START")
                .statusBefore(null)
                .statusAfter(WorkflowExecutionStatus.INITIALIZED)
                .changedBy(startedBy)
                .changedAt(LocalDateTime.now())
                .changeDetails(Map.of("initialData", initialData))
                .reason("Workflow initialization")
                .build();
    }

    /**
     * Create a new history entry for status change
     */
    public static WorkflowHistoryEntity createStatusChange(String workflowId, String workflowType,
                                                         WorkflowExecutionStatus fromStatus,
                                                         WorkflowExecutionStatus toStatus,
                                                         String changedBy, String reason) {
        return WorkflowHistoryEntity.builder()
                .historyId("HIST-" + workflowId + "-" + System.nanoTime())
                .workflowId(workflowId)
                .workflowType(workflowType)
                .action("STATUS_CHANGE")
                .statusBefore(fromStatus)
                .statusAfter(toStatus)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .reason(reason)
                .build();
    }

    /**
     * Create a new history entry for signal received
     */
    public static WorkflowHistoryEntity createSignalReceived(String workflowId, String workflowType,
                                                           String signalName, Map<String, Object> signalData,
                                                           String sentBy) {
        return WorkflowHistoryEntity.builder()
                .historyId("HIST-" + workflowId + "-" + System.nanoTime())
                .workflowId(workflowId)
                .workflowType(workflowType)
                .action("SIGNAL_" + signalName.toUpperCase())
                .changedBy(sentBy)
                .changedAt(LocalDateTime.now())
                .changeDetails(Map.of("signalData", signalData))
                .reason("Signal received: " + signalName)
                .build();
    }



    /**
     * Create a new history entry for workflow failure
     */
    public static WorkflowHistoryEntity createWorkflowFailure(String workflowId, String workflowType,
                                                            String error, String errorDetails,
                                                            String failedBy) {
        return WorkflowHistoryEntity.builder()
                .historyId("HIST-" + workflowId + "-" + System.nanoTime())
                .workflowId(workflowId)
                .workflowType(workflowType)
                .action("FAILURE")
                .statusBefore(WorkflowExecutionStatus.RUNNING)
                .statusAfter(WorkflowExecutionStatus.FAILED)
                .changedBy(failedBy != null ? failedBy : "SYSTEM")
                .changedAt(LocalDateTime.now())
                .changeDetails(Map.of("error", error, "errorDetails", errorDetails))
                .reason("Workflow execution failed")
                .build();
    }


}
