package com.ngoctran.interactionservice.workflow;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow History Repository
 *
 * Based on SchedulerInstructionRepository pattern for MongoDB
 * Provides comprehensive querying capabilities for workflow audit trails
 */
@Repository
public interface WorkflowHistoryRepository extends MongoRepository<WorkflowHistoryEntity, String> {

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
    @Query("{ 'action': 'FAILURE', 'statusAfter': 'FAILED' }")
    List<WorkflowHistoryEntity> findFailedWorkflows();

    /**
     * Find workflows by IP address (for security audit)
     */
    List<WorkflowHistoryEntity> findByIpAddressOrderByChangedAtDesc(String ipAddress);

    /**
     * Count total actions by workflow type
     */
    @Query(value = "{ 'workflowType': ?0 }", count = true)
    long countByWorkflowType(String workflowType);

    /**
     * Count actions by user
     */
    @Query(value = "{ 'changedBy': ?0 }", count = true)
    long countByChangedBy(String changedBy);

    /**
     * Find recent history (last N records)
     */
    @Query("{}")
    List<WorkflowHistoryEntity> findTop10ByOrderByChangedAtDesc();

    /**
     * Find history with specific metadata key
     */
    @Query("{ 'metadata.?0': { $exists: true } }")
    List<WorkflowHistoryEntity> findByMetadataKey(String metadataKey);



    /**
     * Delete old history records (for cleanup)
     */
    @Query("{ 'changedAt': { $lt: ?0 } }")
    List<WorkflowHistoryEntity> findOldHistoryBeforeDate(LocalDateTime cutoffDate);

    /**
     * Find workflows that changed status multiple times (potential issues)
     */
    @Query("{ 'action': 'STATUS_CHANGE' }")
    List<WorkflowHistoryEntity> findStatusChangeHistory();
}
