package com.ngoctran.interactionservice.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * State Context for storing workflow data used in rule evaluation
 * Based on PDF requirement for state data storage
 */
@Entity
@Table(name = "state_context")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateContext {

    @Id
    private UUID instanceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data")
    private String contextData; // JSON data like {"otp_status": "SUCCESS", "ekyc_score": 0.85}

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "version")
    private Long version;
}
