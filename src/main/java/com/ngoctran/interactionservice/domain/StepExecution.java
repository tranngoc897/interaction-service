package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "step_execution")
@IdClass(StepExecutionId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepExecution {

    @Id
    @Column(name = "instance_id")
    private UUID instanceId;

    @Id
    @Column(name = "state")
    private String state;

    @Column(nullable = false)
    private String status; // NEW, RUNNING, SUCCESS, FAILED

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retry")
    private Integer maxRetry;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isNew() {
        return "NEW".equals(this.status);
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetry;
    }

    public void start(Integer maxRetry, Instant now) {
        this.status = "RUNNING";
        this.retryCount = 0;
        this.maxRetry = maxRetry != null ? maxRetry : 3;
        this.nextRetryAt = now;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public void scheduleNextRetry(Instant nextRetryAt) {
        this.status = "FAILED";
        this.nextRetryAt = nextRetryAt;
    }

    public void markSuccess() {
        this.status = "SUCCESS";
    }

    public void markFailed() {
        this.status = "FAILED";
    }

    public void setLastError(String errorCode, String errorMessage) {
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
    }
}
