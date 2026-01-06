package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "instance_id")
    private UUID instanceId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "processed_at")
    private Instant processedAt;
}
