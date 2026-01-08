package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "state_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(nullable = false)
    private String state;

    @Column(name = "snapshot_data")
    @JdbcTypeCode(SqlTypes.JSON)
    private String snapshotData;

    @Column(name = "context_data")
    @JdbcTypeCode(SqlTypes.JSON)
    private String contextData;

    @Column(name = "created_at")
    private Instant createdAt;
}
