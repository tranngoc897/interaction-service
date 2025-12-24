package com.ngoctran.interactionservice.workflow;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Workflow Schedule Entity
 *
 * Stores metadata for Temporal scheduled workflows
 */
@Entity
@Table(name = "workflow_schedule", indexes = {
        @Index(name = "idx_schedule_id", columnList = "schedule_id"),
        @Index(name = "idx_schedule_status", columnList = "status"),
        @Index(name = "idx_schedule_created_by", columnList = "created_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntity {

    @Id
    @Column(name = "schedule_id", length = 255)
    private String scheduleId;

    @Column(name = "cron_expression", length = 255, nullable = false)
    private String cronExpression;

    @Column(name = "workflow_type", length = 255, nullable = false)
    private String workflowType; // e.g., "CaseMonitorWorkflow"

    @Column(name = "task_queue", length = 255)
    private String taskQueue;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ScheduleStatus status;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "workflow_arguments", columnDefinition = "text")
    private String workflowArguments; // JSON string of workflow arguments

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ScheduleStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
