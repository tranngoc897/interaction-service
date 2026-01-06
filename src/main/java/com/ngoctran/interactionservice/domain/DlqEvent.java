package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Entity
@Table(name = "dlq_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlqEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key")
    private String partitionKey;

    @Column(name = "event_payload", columnDefinition = "jsonb", nullable = false)
    private String eventPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(nullable = false)
    private String status; // NEW, RETRIED, IGNORED

    public boolean canRetry() {
        return "NEW".equals(this.status) && (this.retryCount == null || this.retryCount < 3);
    }

    public void markRetried() {
        this.status = "RETRIED";
        this.retryCount = this.retryCount != null ? this.retryCount + 1 : 1;
    }

    public void markIgnored() {
        this.status = "IGNORED";
    }
}
