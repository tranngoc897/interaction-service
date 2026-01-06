package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.WorkflowMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WorkflowMetricsRepository extends JpaRepository<WorkflowMetrics, Long> {

    List<WorkflowMetrics> findByMetricName(String metricName);

    List<WorkflowMetrics> findByState(String state);

    @Query("SELECT m FROM WorkflowMetrics m WHERE m.recordedAt >= :since ORDER BY m.recordedAt DESC")
    List<WorkflowMetrics> findRecentMetrics(@Param("since") Instant since);

    @Query("SELECT m FROM WorkflowMetrics m WHERE m.metricName = :metricName AND m.recordedAt >= :since")
    List<WorkflowMetrics> findMetricsByNameAndTime(@Param("metricName") String metricName, @Param("since") Instant since);

    @Query("SELECT AVG(m.value) FROM WorkflowMetrics m WHERE m.metricName = :metricName AND m.recordedAt >= :since")
    Double getAverageValue(@Param("metricName") String metricName, @Param("since") Instant since);

    @Query("SELECT SUM(m.value) FROM WorkflowMetrics m WHERE m.metricName = :metricName AND m.recordedAt >= :since")
    Long getTotalValue(@Param("metricName") String metricName, @Param("since") Instant since);
}
