package com.ngoctran.interactionservice.mapping;

import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data; // keeping it but not relying on it
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Process Mapping Entity
 * 
 * Maps business processes (Cases/Interactions) to workflow engine instances (Temporal, Camunda, etc.)
 */
@Entity
@Table(name = "flw_process_mapping", indexes = {
    @Index(name = "idx_process_mapping_case_id", columnList = "case_id"),
    @Index(name = "idx_process_mapping_user_id", columnList = "user_id"),
    @Index(name = "idx_process_mapping_process_instance_id", columnList = "process_instance_id"),
    @Index(name = "idx_process_mapping_status", columnList = "status"),
    // Composite index for finding latest process of a specific type for a case
    @Index(name = "idx_process_mapping_case_def", columnList = "case_id, process_definition_key")
})
@NoArgsConstructor
@AllArgsConstructor
public class ProcessMappingEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Version
    @Column(name = "version")
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", length = 50, nullable = false)
    private EngineType engineType;

    @Column(name = "process_instance_id", length = 128, nullable = false, unique = true)
    private String processInstanceId;

    @Column(name = "process_definition_key", length = 255, nullable = false)
    private String processDefinitionKey;

    @Column(name = "business_key", length = 255)
    private String businessKey;

    @Column(name = "case_id", length = 36, nullable = false)
    private String caseId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProcessStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (startedAt == null) {
            startedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isRunning() {
        return ProcessStatus.RUNNING.equals(status);
    }
    
    public boolean isCompleted() {
        return ProcessStatus.COMPLETED.equals(status);
    }

    public boolean isFailed() {
        return ProcessStatus.FAILED.equals(status);
    }

    public boolean isCancelled() {
        return ProcessStatus.CANCELLED.equals(status);
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    public void markCompleted() {
        this.status = ProcessStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ProcessStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void markCancelled() {
        this.status = ProcessStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public EngineType getEngineType() { return engineType; }
    public void setEngineType(EngineType engineType) { this.engineType = engineType; }

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ProcessStatus getStatus() { return status; }
    public void setStatus(ProcessStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
