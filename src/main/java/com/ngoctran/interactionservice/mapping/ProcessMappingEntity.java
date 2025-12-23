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
 * Maps business processes (Cases/Interactions) to onboarding engine instances
 * (Temporal, Camunda, etc.)
 */
@Entity
@Table(name = "process_mapping", indexes = {
        @Index(name = "idx_process_mapping_case_id", columnList = "case_id"),
        @Index(name = "idx_process_mapping_user_id", columnList = "user_id"),
        @Index(name = "idx_process_mapping_process_instance_id", columnList = "process_instance_id"),
        @Index(name = "idx_process_mapping_status", columnList = "status"),
})
@NoArgsConstructor
@AllArgsConstructor
@Data
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

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

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

    // Getters and setters handled by Lombok @Data
}
