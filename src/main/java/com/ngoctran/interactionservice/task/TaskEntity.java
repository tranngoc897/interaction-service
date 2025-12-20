package com.ngoctran.interactionservice.task;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flw_task")
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

    @Id
    private String id;

    @Version
    private Integer version;

    @Column(name = "case_id", length = 36, nullable = false)
    private String caseId;

    @Column(name = "interaction_id", length = 36)
    private String interactionId;

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
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters / Setters manually (avoiding Lombok dependency issues)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getInteractionId() { return interactionId; }
    public void setInteractionId(String interactionId) { this.interactionId = interactionId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
