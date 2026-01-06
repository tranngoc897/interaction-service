package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Entity
@Table(name = "outbox_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key")
    private String partitionKey;

    @Column(name = "event_payload", columnDefinition = "jsonb", nullable = false)
    private String eventPayload;

    @Column(name = "event_type")
    private String eventType;

    @Column(nullable = false)
    private String status; // PENDING, PUBLISHED, FAILED

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;
}
