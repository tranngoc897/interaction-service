package com.ngoctran.interactionservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "workflow_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column
    private String state;

    @Column(nullable = false)
    private Long value;

    @Column(columnDefinition = "jsonb")
    private Map<String, String> tags;

    @Column(name = "recorded_at")
    private Instant recordedAt;
}
