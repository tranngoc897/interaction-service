package com.ngoctran.interactionservice.dto;

/**
 * Workflow Execution Status Enum
 *
 * Based on SchedulerService patterns with comprehensive status management
 * Similar to ScheduleStatus but for workflow execution states
 */
public enum WorkflowExecutionStatus {

    // Initial states
    INITIALIZED("Workflow initialized and ready to start"),
    SCHEDULED("Workflow scheduled for future execution"),

    // Running states
    RUNNING("Workflow is currently executing"),
    WAITING("Workflow waiting for external input/signal"),
    SUSPENDED("Workflow temporarily suspended"),

    // Completion states
    COMPLETED("Workflow completed successfully"),
    FAILED("Workflow failed with error"),
    TIMED_OUT("Workflow timed out"),
    CANCELLED("Workflow cancelled by user or system"),

    // Compensation states
    COMPENSATING("Workflow compensating failed activities"),
    COMPENSATED("Workflow compensation completed"),

    // Administrative states
    PAUSED("Workflow paused for maintenance"),
    ARCHIVED("Workflow archived for historical reference");

    private final String description;

    WorkflowExecutionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if workflow is in a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED ||
               this == TIMED_OUT || this == CANCELLED ||
               this == COMPENSATED || this == ARCHIVED;
    }

    /**
     * Check if workflow is actively running
     */
    public boolean isActive() {
        return this == RUNNING || this == WAITING;
    }

    /**
     * Check if workflow can be cancelled
     */
    public boolean canCancel() {
        return this == INITIALIZED || this == SCHEDULED ||
               this == RUNNING || this == WAITING || this == SUSPENDED ||
               this == PAUSED;
    }

    /**
     * Check if workflow can be resumed
     */
    public boolean canResume() {
        return this == SUSPENDED || this == PAUSED;
    }

    /**
     * Get user-friendly status message
     */
    public String getUserMessage() {
        switch (this) {
            case INITIALIZED: return "Getting ready to start";
            case SCHEDULED: return "Scheduled for execution";
            case RUNNING: return "Processing your request";
            case WAITING: return "Waiting for additional information";
            case SUSPENDED: return "Temporarily paused";
            case COMPLETED: return "Completed successfully";
            case FAILED: return "Failed to complete";
            case TIMED_OUT: return "Took too long to complete";
            case CANCELLED: return "Cancelled by user";
            case COMPENSATING: return "Rolling back changes";
            case COMPENSATED: return "Changes rolled back";
            case PAUSED: return "Paused for maintenance";
            case ARCHIVED: return "Archived for records";
            default: return description;
        }
    }

    /**
     * Convert to compatible format for external systems
     */
    public String toExternalStatus() {
        switch (this) {
            case COMPLETED: return "SUCCESS";
            case FAILED: case TIMED_OUT: return "ERROR";
            case CANCELLED: return "CANCELLED";
            case RUNNING: case WAITING: return "IN_PROGRESS";
            case SUSPENDED: case PAUSED: return "PAUSED";
            default: return "UNKNOWN";
        }
    }
}
