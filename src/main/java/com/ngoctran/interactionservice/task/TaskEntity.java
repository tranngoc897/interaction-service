package com.ngoctran.interactionservice.task;

import com.ngoctran.interactionservice.cases.CaseEntity;
import com.ngoctran.interactionservice.interaction.InteractionEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flw_task", indexes = {
        @Index(name = "idx_flw_task_case_id", columnList = "case_id"),
        @Index(name = "idx_flw_task_status", columnList = "status"),
        @Index(name = "idx_flw_task_assignee", columnList = "assignee_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    private Integer version;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "interaction_id", length = 36)
    private String interactionId;

    /**
     * JPA Relationship for navigation (Read-only)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", insertable = false, updatable = false)
    private CaseEntity caseEntity;

    /**
     * JPA Relationship for navigation (Read-only)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id", insertable = false, updatable = false)
    private InteractionEntity interaction;

    @Column(name = "task_type", length = 50, nullable = false)
    private String taskType; // e.g. "MANUAL_REVIEW", "ID_APPROVAL"

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "assignee_id", length = 36)
    private String assigneeId;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload; // Data for the reviewer to look at

    @Column(name = "result", columnDefinition = "jsonb")
    private String result; // Decision/Outcome of the review

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
