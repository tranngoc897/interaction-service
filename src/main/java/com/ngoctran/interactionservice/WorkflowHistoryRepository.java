package com.ngoctran.interactionservice;

import com.ngoctran.interactionservice.dto.WorkflowExecutionStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow History Repository
 *
 * Based on SchedulerInstructionRepository pattern for PostgreSQL
 * Provides comprehensive querying capabilities for workflow audit trails
 */
@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistoryEntity, UUID> {

        /**
         * Find all history for a specific workflow
         */
        List<WorkflowHistoryEntity> findByWorkflowIdOrderByChangedAtAsc(String workflowId);

        /**
         * Find history by workflow type
         */
        List<WorkflowHistoryEntity> findByWorkflowTypeOrderByChangedAtDesc(String workflowType);

        /**
         * Find history by user who made changes
         */
        List<WorkflowHistoryEntity> findByChangedByOrderByChangedAtDesc(String changedBy);

        /**
         * Find history by action type
         */
        List<WorkflowHistoryEntity> findByActionOrderByChangedAtDesc(String action);

        /**
         * Find history by status change
         */
        List<WorkflowHistoryEntity> findByStatusBeforeOrStatusAfterOrderByChangedAtDesc(
                        WorkflowExecutionStatus statusBefore, WorkflowExecutionStatus statusAfter);

        /**
         * Find history within date range
         */
        List<WorkflowHistoryEntity> findByChangedAtBetweenOrderByChangedAtDesc(
                        LocalDateTime startDate, LocalDateTime endDate);

        /**
         * Find history by workflow and action
         */
        List<WorkflowHistoryEntity> findByWorkflowIdAndActionOrderByChangedAtDesc(
                        String workflowId, String action);

        /**
         * Find failed workflows
         */
        @Query("SELECT h FROM WorkflowHistoryEntity h WHERE h.action = 'FAILURE' AND h.statusAfter = 'FAILED'")
        List<WorkflowHistoryEntity> findFailedWorkflows();

        /**
         * Find workflows by IP address (for security audit)
         */
        List<WorkflowHistoryEntity> findByIpAddressOrderByChangedAtDesc(String ipAddress);

        /**
         * Count total actions by workflow type
         */
        @Query("SELECT COUNT(h) FROM WorkflowHistoryEntity h WHERE h.workflowType = :workflowType")
        long countByWorkflowType(@Param("workflowType") String workflowType);

        /**
         * Count actions by user
         */
        @Query("SELECT COUNT(h) FROM WorkflowHistoryEntity h WHERE h.changedBy = :changedBy")
        long countByChangedBy(@Param("changedBy") String changedBy);

        /**
         * Find recent history (last N records)
         */
        List<WorkflowHistoryEntity> findTop10ByOrderByChangedAtDesc();

        /**
         * Find history with specific metadata key (JSON query for PostgreSQL)
         */
        @Query(value = "SELECT * FROM workflow_history WHERE metadata::jsonb ?1 :key", nativeQuery = true)
        List<WorkflowHistoryEntity> findByMetadataKey(@Param("key") String metadataKey);

        /**
         * Delete old history records (for cleanup)
         */
        @Query("SELECT h FROM WorkflowHistoryEntity h WHERE h.changedAt < :cutoffDate")
        List<WorkflowHistoryEntity> findOldHistoryBeforeDate(@Param("cutoffDate") LocalDateTime cutoffDate);

        /**
         * Find workflows that changed status multiple times (potential issues)
         */
        @Query("SELECT h FROM WorkflowHistoryEntity h WHERE h.action = 'STATUS_CHANGE'")
        List<WorkflowHistoryEntity> findStatusChangeHistory();
}
