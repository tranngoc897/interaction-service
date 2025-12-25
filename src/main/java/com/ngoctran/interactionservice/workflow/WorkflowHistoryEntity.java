package com.ngoctran.interactionservice.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Workflow History Entity
 *
 * Based on SchedulerInstructionHistoryChangeEntity pattern
 * Tracks all state changes and actions performed on workflows
 */
@Entity
@Table(name = "workflow_history", indexes = {
    @Index(name = "idx_workflow_history_workflow_id", columnList = "workflow_id"),
    @Index(name = "idx_workflow_history_workflow_type", columnList = "workflow_type"),
    @Index(name = "idx_workflow_history_changed_by", columnList = "changed_by"),
    @Index(name = "idx_workflow_history_action", columnList = "action"),
    @Index(name = "idx_workflow_history_changed_at", columnList = "changed_at"),
    @Index(name = "idx_workflow_history_workflow_action", columnList = "workflow_id, action"),
    @Index(name = "idx_workflow_history_type_date", columnList = "workflow_type, changed_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id")
    private String historyId;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "workflow_type")
    private String workflowType; // KYC_ONBOARDING, PAYMENT, RECONCILIATION, etc.

    private String action; // START, SIGNAL, COMPLETE, FAIL, CANCEL, SUSPEND, RESUME

    @Enumerated(EnumType.STRING)
    private WorkflowExecutionStatus statusBefore;

    @Enumerated(EnumType.STRING)
    private WorkflowExecutionStatus statusAfter;

    private String changedBy; // userId or SYSTEM

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "change_details", columnDefinition = "jsonb")
    private Map<String, Object> changeDetails; // Action-specific details

    private String reason; // Why the change was made
    private String notes; // Additional notes

    // Audit fields
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    // Metadata for querying
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // For optimistic locking
    @Version
    private Long version;

    // Helper method to convert Map to JSON string if needed
    public String getChangeDetailsJson() {
        try {
            if (changeDetails != null) {
                return new ObjectMapper().writeValueAsString(changeDetails);
            }
        } catch (Exception e) {
            // Handle serialization error
        }
        return null;
    }

    // Helper method to convert JSON string to Map if needed
    public void setChangeDetailsJson(String json) {
        try {
            if (json != null && !json.trim().isEmpty()) {
                changeDetails = new ObjectMapper().readValue(json, Map.class);
            }
        } catch (Exception e) {
            // Handle deserialization error
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Create a new history entry for workflow start
     */
    public static WorkflowHistoryEntity createWorkflowStart(String workflowId, String workflowType,
                                                          String startedBy, Map<String, Object> initialData) {
        WorkflowHistoryEntity entity = new WorkflowHistoryEntity();
        entity.setHistoryId("HIST-" + workflowId + "-" + System.nanoTime());
        entity.setWorkflowId(workflowId);
        entity.setWorkflowType(workflowType);
        entity.setAction("START");
        entity.setStatusBefore(null);
        entity.setStatusAfter(WorkflowExecutionStatus.INITIALIZED);
        entity.setChangedBy(startedBy);
        entity.setChangedAt(LocalDateTime.now());
        entity.setChangeDetails(Map.of("initialData", initialData));
        entity.setReason("Workflow initialization");
        return entity;
    }

    /**
     * Create a new history entry for status change
     */
    public static WorkflowHistoryEntity createStatusChange(String workflowId, String workflowType,
                                                         WorkflowExecutionStatus fromStatus,
                                                         WorkflowExecutionStatus toStatus,
                                                         String changedBy, String reason) {
        WorkflowHistoryEntity entity = new WorkflowHistoryEntity();
        entity.setHistoryId("HIST-" + workflowId + "-" + System.nanoTime());
        entity.setWorkflowId(workflowId);
        entity.setWorkflowType(workflowType);
        entity.setAction("STATUS_CHANGE");
        entity.setStatusBefore(fromStatus);
        entity.setStatusAfter(toStatus);
        entity.setChangedBy(changedBy);
        entity.setChangedAt(LocalDateTime.now());
        entity.setReason(reason);
        return entity;
    }

    /**
     * Create a new history entry for signal received
     */
    public static WorkflowHistoryEntity createSignalReceived(String workflowId, String workflowType,
                                                           String signalName, Map<String, Object> signalData,
                                                           String sentBy) {
        WorkflowHistoryEntity entity = new WorkflowHistoryEntity();
        entity.setHistoryId("HIST-" + workflowId + "-" + System.nanoTime());
        entity.setWorkflowId(workflowId);
        entity.setWorkflowType(workflowType);
        entity.setAction("SIGNAL_" + signalName.toUpperCase());
        entity.setChangedBy(sentBy);
        entity.setChangedAt(LocalDateTime.now());
        entity.setChangeDetails(Map.of("signalData", signalData));
        entity.setReason("Signal received: " + signalName);
        return entity;
    }

    /**
     * Create a new history entry for workflow failure
     */
    public static WorkflowHistoryEntity createWorkflowFailure(String workflowId, String workflowType,
                                                            String error, String errorDetails,
                                                            String failedBy) {
        WorkflowHistoryEntity entity = new WorkflowHistoryEntity();
        entity.setHistoryId("HIST-" + workflowId + "-" + System.nanoTime());
        entity.setWorkflowId(workflowId);
        entity.setWorkflowType(workflowType);
        entity.setAction("FAILURE");
        entity.setStatusBefore(WorkflowExecutionStatus.RUNNING);
        entity.setStatusAfter(WorkflowExecutionStatus.FAILED);
        entity.setChangedBy(failedBy != null ? failedBy : "SYSTEM");
        entity.setChangedAt(LocalDateTime.now());
        entity.setChangeDetails(Map.of("error", error, "errorDetails", errorDetails));
        entity.setReason("Workflow execution failed");
        return entity;
    }


}
