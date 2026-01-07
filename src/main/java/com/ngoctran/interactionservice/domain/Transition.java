package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Entity
@Table(name = "onboarding_transition")
@IdClass(TransitionId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transition {

    @Id
    @Column(name = "flow_version")
    private String flowVersion;

    @Id
    @Column(name = "from_state")
    private String fromState;

    @Id
    @Column(name = "action")
    private String action;

    @Column(name = "to_state")
    private String toState;

    @Column(name = "is_async")
    private Boolean async;

    @Column(name = "source_service")
    private String sourceService;

    @Column(name = "allowed_actors")
    private String[] allowedActors;

    @Column(name = "max_retry")
    private Integer maxRetry;

    @Column(columnDefinition = "jsonb")
    private String conditionsJson; // JSON array of conditions like ["otp_status == SUCCESS"]

    public boolean isAsync() {
        return Boolean.TRUE.equals(this.async);
    }

    public boolean isSameState() {
        return this.fromState.equals(this.toState);
    }

    public boolean isAllowedActor(String actor) {
        if (allowedActors == null || allowedActors.length == 0) {
            return true; // No restrictions
        }
        for (String allowed : allowedActors) {
            if (allowed.equals(actor)) {
                return true;
            }
        }
        return false;
    }
}
