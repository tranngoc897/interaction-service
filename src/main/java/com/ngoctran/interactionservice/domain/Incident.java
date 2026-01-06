package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    private UUID incidentId;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(nullable = false)
    private String state;

    @Column(name = "error_code", nullable = false)
    private String errorCode;

    @Column(nullable = false)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false)
    private String status; // OPEN, ACKNOWLEDGED, RESOLVED

    @Column
    private String owner; // Assigned person

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public boolean isOpen() {
        return "OPEN".equals(this.status);
    }

    public boolean isAcknowledged() {
        return "ACKNOWLEDGED".equals(this.status);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(this.status);
    }

    public boolean isHighSeverity() {
        return "HIGH".equals(this.severity) || "CRITICAL".equals(this.severity);
    }

    public void acknowledge(String owner) {
        this.owner = owner;
        this.status = "ACKNOWLEDGED";
        this.acknowledgedAt = Instant.now();
    }

    public void resolve(String resolution) {
        this.resolution = resolution;
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
    }
}
