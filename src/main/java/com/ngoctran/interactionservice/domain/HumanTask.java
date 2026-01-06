package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "human_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanTask {

    @Id
    private UUID taskId;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(nullable = false)
    private String state;

    @Column(name = "task_type", nullable = false)
    private String taskType; // AML_REVIEW, EKYC_REVIEW, MANUAL_APPROVAL

    @Column(name = "assigned_role")
    private String assignedRole; // RISK_OFFICER, COMPLIANCE_OFFICER

    @Column(name = "assigned_user")
    private String assignedUser;

    @Column(nullable = false)
    private String status; // OPEN, CLAIMED, COMPLETED

    @Column
    private String priority; // LOW, NORMAL, HIGH, CRITICAL

    @Column(columnDefinition = "jsonb")
    private String payload; // Task data for display

    @Column(columnDefinition = "jsonb")
    private String result; // Approval result

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    public boolean isOpen() {
        return "OPEN".equals(this.status);
    }

    public boolean isClaimed() {
        return "CLAIMED".equals(this.status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    public boolean isOverdue() {
        return this.dueAt != null && Instant.now().isAfter(this.dueAt);
    }

    public void claim(String userId) {
        this.assignedUser = userId;
        this.status = "CLAIMED";
        this.claimedAt = Instant.now();
    }

    public void complete(String result) {
        this.result = result;
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
    }
}
