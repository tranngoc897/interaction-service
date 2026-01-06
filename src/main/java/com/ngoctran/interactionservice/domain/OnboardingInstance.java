package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "onboarding_instance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingInstance {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "flow_version", nullable = false)
    private String flowVersion;

    @Column(name = "current_state", nullable = false)
    private String currentState;

    @Column(nullable = false)
    private String status;

    @Version
    private Long version;

    @Column(name = "state_started_at")
    private Instant stateStartedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public void moveTo(String newState) {
        this.currentState = newState;
        this.stateStartedAt = Instant.now();
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
}
