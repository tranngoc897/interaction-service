package com.ngoctran.interactionservice.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;

/**
 * Workflow Scheduler Service
 *
 * Based on your SchedulerService patterns for managing scheduled workflows
 * Handles creation and management of workflow schedules
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowSchedulerService {

    private final WorkflowHistoryRepository workflowHistoryRepository;
    // Note: In real implementation, inject SchedulerClient
    // private final SchedulerClient schedulerClient;

    /**
     * Create a scheduled workflow
     *
     * @param workflowType Type of workflow to schedule
     * @param config Scheduling configuration
     * @return Scheduled workflow ID
     */
    public String scheduleWorkflowExecution(String workflowType, WorkflowScheduleConfig config) {
        log.info("Scheduling workflow execution: {} with config: {}", workflowType, config);

        try {
            // Build cron expression from config
            String cronExpression = CronExpressionBuilder.buildWorkflowCronExpression(
                    config.getScheduleType(),
                    mapToScheduleDetail(config)
            );

            // Create scheduled workflow (similar to your SchedulerCreateRequest)
            ScheduledWorkflowRequest scheduledRequest = ScheduledWorkflowRequest.builder()
                    .workflowType(workflowType)
                    .cronExpression(cronExpression)
                    .config(config.getWorkflowConfig())
                    .scheduledBy(config.getCreatedBy())
                    .description(config.getDescription())
                    .build();

            // In real implementation, call scheduler service
            // String scheduledWorkflowId = schedulerClient.createScheduledWorkflow(scheduledRequest);
            String scheduledWorkflowId = "SCH-" + workflowType + "-" + System.nanoTime();

            // Record scheduling in history
            WorkflowHistoryEntity historyEntry = WorkflowHistoryEntity.builder()
                    .historyId("HIST-SCH-" + System.nanoTime())
                    .workflowId(scheduledWorkflowId)
                    .workflowType(workflowType)
                    .action("SCHEDULE_CREATED")
                    .statusAfter(WorkflowExecutionStatus.SCHEDULED)
                    .changedBy(config.getCreatedBy())
                    .changedAt(java.time.LocalDateTime.now())
                    .changeDetails(Map.of(
                            "cronExpression", cronExpression,
                            "scheduleType", config.getScheduleType(),
                            "scheduleConfig", config
                    ))
                    .reason("Workflow scheduled for execution")
                    .build();

            workflowHistoryRepository.save(historyEntry);

            log.info("Workflow scheduled successfully: {} with cron: {}", scheduledWorkflowId, cronExpression);
            return scheduledWorkflowId;

        } catch (Exception e) {
            log.error("Failed to schedule workflow: {}", workflowType, e);
            throw new RuntimeException("Workflow scheduling failed: " + e.getMessage());
        }
    }

    /**
     * Update workflow schedule
     *
     * @param scheduledWorkflowId ID of scheduled workflow
     * @param config New schedule configuration
     */
    public void updateWorkflowSchedule(String scheduledWorkflowId, WorkflowScheduleConfig config) {
        log.info("Updating workflow schedule: {}", scheduledWorkflowId);

        try {
            // Build new cron expression
            String newCronExpression = CronExpressionBuilder.buildWorkflowCronExpression(
                    config.getScheduleType(),
                    mapToScheduleDetail(config)
            );

            // Update schedule (similar to your SchedulerUpdateRequest)
            ScheduledWorkflowUpdateRequest updateRequest = ScheduledWorkflowUpdateRequest.builder()
                    .scheduledWorkflowId(scheduledWorkflowId)
                    .cronExpression(newCronExpression)
                    .config(config.getWorkflowConfig())
                    .updatedBy(config.getCreatedBy())
                    .build();

            // In real implementation, call scheduler service
            // schedulerClient.updateScheduledWorkflow(updateRequest);

            // Record update in history
            WorkflowHistoryEntity historyEntry = WorkflowHistoryEntity.builder()
                    .historyId("HIST-SCH-UPD-" + System.nanoTime())
                    .workflowId(scheduledWorkflowId)
                    .workflowType("SCHEDULED_WORKFLOW")
                    .action("SCHEDULE_UPDATED")
                    .changedBy(config.getCreatedBy())
                    .changedAt(java.time.LocalDateTime.now())
                    .changeDetails(Map.of(
                            "newCronExpression", newCronExpression,
                            "scheduleConfig", config
                    ))
                    .reason("Workflow schedule updated")
                    .build();

            workflowHistoryRepository.save(historyEntry);

            log.info("Workflow schedule updated: {}", scheduledWorkflowId);

        } catch (Exception e) {
            log.error("Failed to update workflow schedule: {}", scheduledWorkflowId, e);
            throw new RuntimeException("Schedule update failed: " + e.getMessage());
        }
    }

    /**
     * Pause workflow schedule
     *
     * @param scheduledWorkflowId ID of scheduled workflow
     * @param pausedBy User pausing the schedule
     */
    public void pauseWorkflowSchedule(String scheduledWorkflowId, String pausedBy) {
        log.info("Pausing workflow schedule: {} by user: {}", scheduledWorkflowId, pausedBy);

        try {
            // Pause schedule (similar to your status change)
            // schedulerClient.pauseScheduledWorkflow(scheduledWorkflowId);

            // Record pause in history
            WorkflowHistoryEntity historyEntry = WorkflowHistoryEntity.builder()
                    .historyId("HIST-SCH-PAUSE-" + System.nanoTime())
                    .workflowId(scheduledWorkflowId)
                    .workflowType("SCHEDULED_WORKFLOW")
                    .action("SCHEDULE_PAUSED")
                    .statusAfter(WorkflowExecutionStatus.SUSPENDED)
                    .changedBy(pausedBy)
                    .changedAt(java.time.LocalDateTime.now())
                    .reason("Workflow schedule paused")
                    .build();

            workflowHistoryRepository.save(historyEntry);

            log.info("Workflow schedule paused: {}", scheduledWorkflowId);

        } catch (Exception e) {
            log.error("Failed to pause workflow schedule: {}", scheduledWorkflowId, e);
            throw new RuntimeException("Schedule pause failed: " + e.getMessage());
        }
    }

    /**
     * Resume workflow schedule
     *
     * @param scheduledWorkflowId ID of scheduled workflow
     * @param resumedBy User resuming the schedule
     */
    public void resumeWorkflowSchedule(String scheduledWorkflowId, String resumedBy) {
        log.info("Resuming workflow schedule: {} by user: {}", scheduledWorkflowId, resumedBy);

        try {
            // Resume schedule
            // schedulerClient.resumeScheduledWorkflow(scheduledWorkflowId);

            // Record resume in history
            WorkflowHistoryEntity historyEntry = WorkflowHistoryEntity.builder()
                    .historyId("HIST-SCH-RESUME-" + System.nanoTime())
                    .workflowId(scheduledWorkflowId)
                    .workflowType("SCHEDULED_WORKFLOW")
                    .action("SCHEDULE_RESUMED")
                    .statusAfter(WorkflowExecutionStatus.SCHEDULED)
                    .changedBy(resumedBy)
                    .changedAt(java.time.LocalDateTime.now())
                    .reason("Workflow schedule resumed")
                    .build();

            workflowHistoryRepository.save(historyEntry);

            log.info("Workflow schedule resumed: {}", scheduledWorkflowId);

        } catch (Exception e) {
            log.error("Failed to resume workflow schedule: {}", scheduledWorkflowId, e);
            throw new RuntimeException("Schedule resume failed: " + e.getMessage());
        }
    }

    /**
     * Delete workflow schedule
     *
     * @param scheduledWorkflowId ID of scheduled workflow
     * @param deletedBy User deleting the schedule
     */
    public void deleteWorkflowSchedule(String scheduledWorkflowId, String deletedBy) {
        log.info("Deleting workflow schedule: {} by user: {}", scheduledWorkflowId, deletedBy);

        try {
            // Delete schedule
            // schedulerClient.deleteScheduledWorkflow(scheduledWorkflowId);

            // Record deletion in history
            WorkflowHistoryEntity historyEntry = WorkflowHistoryEntity.builder()
                    .historyId("HIST-SCH-DELETE-" + System.nanoTime())
                    .workflowId(scheduledWorkflowId)
                    .workflowType("SCHEDULED_WORKFLOW")
                    .action("SCHEDULE_DELETED")
                    .statusAfter(WorkflowExecutionStatus.ARCHIVED)
                    .changedBy(deletedBy)
                    .changedAt(java.time.LocalDateTime.now())
                    .reason("Workflow schedule deleted")
                    .build();

            workflowHistoryRepository.save(historyEntry);

            log.info("Workflow schedule deleted: {}", scheduledWorkflowId);

        } catch (Exception e) {
            log.error("Failed to delete workflow schedule: {}", scheduledWorkflowId, e);
            throw new RuntimeException("Schedule deletion failed: " + e.getMessage());
        }
    }

    /**
     * Get scheduled workflow details
     *
     * @param scheduledWorkflowId ID of scheduled workflow
     * @return Schedule details
     */
    public ScheduledWorkflowDetails getScheduledWorkflowDetails(String scheduledWorkflowId) {
        // In real implementation, query scheduler service
        // return schedulerClient.getScheduledWorkflowDetails(scheduledWorkflowId);

        // Mock implementation
        return ScheduledWorkflowDetails.builder()
                .scheduledWorkflowId(scheduledWorkflowId)
                .workflowType("RECONCILIATION")
                .cronExpression(CronExpressionBuilder.CommonCronExpressions.EOD_RECONCILIATION)
                .status("ACTIVE")
                .nextExecution(java.time.LocalDateTime.now().plusDays(1).withHour(18))
                .createdBy("SYSTEM")
                .build();
    }

    /**
     * List all scheduled workflows
     *
     * @param workflowType Optional filter by workflow type
     * @return List of scheduled workflows
     */
    public java.util.List<ScheduledWorkflowSummary> listScheduledWorkflows(String workflowType) {
        // In real implementation, query scheduler service
        // return schedulerClient.listScheduledWorkflows(workflowType);

        // Mock implementation
        return Arrays.asList(
                ScheduledWorkflowSummary.builder()
                        .scheduledWorkflowId("SCH-RECONCILIATION-EOD")
                        .workflowType("RECONCILIATION")
                        .cronExpression(CronExpressionBuilder.CommonCronExpressions.EOD_RECONCILIATION)
                        .status("ACTIVE")
                        .description("End of day payment reconciliation")
                        .build()
        );
    }

    /**
     * Map WorkflowScheduleConfig to CronExpressionBuilder.WorkflowScheduleDetail
     */
    private CronExpressionBuilder.WorkflowScheduleDetail mapToScheduleDetail(WorkflowScheduleConfig config) {
        CronExpressionBuilder.WorkflowScheduleDetail detail = new CronExpressionBuilder.WorkflowScheduleDetail();

        if (config.getExecutionDate() != null) {
            detail.setExecutionDate(config.getExecutionDate());
        }

        if (config.getExecutionTime() != null) {
            detail.setExecutionTime(config.getExecutionTime());
        }

        if (config.getDayOfMonth() != null) {
            detail.setDayOfMonth(config.getDayOfMonth());
        }

        if (config.getDaysOfWeek() != null && !config.getDaysOfWeek().isEmpty()) {
            detail.setDaysOfWeek(config.getDaysOfWeek());
        }

        if (config.getCustomCronExpression() != null) {
            detail.setCustomCronExpression(config.getCustomCronExpression());
        }

        detail.setTimezone(config.getTimezone());

        return detail;
    }

    // ==================== DTOs ====================

    /**
     * Workflow Schedule Config DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class WorkflowScheduleConfig {
        private String scheduleType; // ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM
        private LocalDate executionDate;
        private LocalTime executionTime;
        private Integer dayOfMonth;
        private java.util.List<String> daysOfWeek;
        private String customCronExpression;
        private String timezone;
        private String createdBy;
        private String description;
        private Map<String, Object> workflowConfig;
    }

    /**
     * Scheduled Workflow Request DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ScheduledWorkflowRequest {
        private String workflowType;
        private String cronExpression;
        private Map<String, Object> config;
        private String scheduledBy;
        private String description;
    }

    /**
     * Scheduled Workflow Update Request DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ScheduledWorkflowUpdateRequest {
        private String scheduledWorkflowId;
        private String cronExpression;
        private Map<String, Object> config;
        private String updatedBy;
    }

    /**
     * Scheduled Workflow Details DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ScheduledWorkflowDetails {
        private String scheduledWorkflowId;
        private String workflowType;
        private String cronExpression;
        private String status;
        private java.time.LocalDateTime nextExecution;
        private java.time.LocalDateTime lastExecution;
        private String createdBy;
        private java.time.LocalDateTime createdAt;
        private String description;
    }

    /**
     * Scheduled Workflow Summary DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ScheduledWorkflowSummary {
        private String scheduledWorkflowId;
        private String workflowType;
        private String cronExpression;
        private String status;
        private String description;
        private java.time.LocalDateTime nextExecution;
    }
}
