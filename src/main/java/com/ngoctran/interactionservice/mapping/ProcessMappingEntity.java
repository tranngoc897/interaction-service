package com.ngoctran.interactionservice.mapping;

import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Process Mapping Entity
 * 
 * Maps business processes (Cases/Interactions) to onboarding engine instances
 * (Temporal, Camunda, etc.)
 */
@Entity
@Table(name = "process_mapping", indexes = {
        @Index(name = "idx_proc_map_case", columnList = "case_id"),
        @Index(name = "idx_proc_map_instance", columnList = "process_instance_id"),
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProcessMappingEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", length = 50)
    private EngineType engineType;

    @Column(name = "process_instance_id", length = 255)
    private String processInstanceId;

    @Column(name = "process_definition_key", length = 255)
    private String processDefinitionKey;

    @Column(name = "case_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID caseId;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ProcessStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_details", columnDefinition = "text")
    private String errorDetails;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
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
        this.errorDetails = errorMessage;
    }

    public void markCancelled() {
        this.status = ProcessStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    // Manual getter for processInstanceId (needed by MyCaseService)
    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
