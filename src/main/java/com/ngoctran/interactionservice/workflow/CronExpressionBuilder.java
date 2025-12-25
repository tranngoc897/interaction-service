package com.ngoctran.interactionservice.workflow;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cron Expression Builder Utility
 *
 * Based on your CronExpressionBuilder pattern in SchedulerService
 * Generates cron expressions for workflow scheduling
 */
@Slf4j
public class CronExpressionBuilder {

    /**
     * Build cron expression for workflow scheduling
     *
     * @param scheduleType Type of schedule (ONCE, DAILY, WEEKLY, MONTHLY)
     * @param scheduleDetail Schedule configuration details
     * @return Cron expression string
     */
    public static String buildWorkflowCronExpression(String scheduleType, WorkflowScheduleDetail scheduleDetail) {
        switch (scheduleType.toUpperCase()) {
            case "ONCE":
                return buildOnceSchedule(scheduleDetail);
            case "DAILY":
                return buildDailySchedule(scheduleDetail);
            case "WEEKLY":
                return buildWeeklySchedule(scheduleDetail);
            case "MONTHLY":
                return buildMonthlySchedule(scheduleDetail);
            case "CUSTOM":
                return buildCustomSchedule(scheduleDetail);
            default:
                throw new IllegalArgumentException("Unsupported schedule type: " + scheduleType);
        }
    }

    /**
     * Build cron for one-time execution
     */
    private static String buildOnceSchedule(WorkflowScheduleDetail detail) {
        if (detail.getExecutionDate() == null || detail.getExecutionTime() == null) {
            throw new IllegalArgumentException("Execution date and time required for ONCE schedule");
        }

        // Cron format: second minute hour day month dayOfWeek
        // For one-time: * minute hour day month *
        return String.format("0 %d %d %d %d ?",
                detail.getExecutionTime().getMinute(),
                detail.getExecutionTime().getHour(),
                detail.getExecutionDate().getDayOfMonth(),
                detail.getExecutionDate().getMonthValue());
    }

    /**
     * Build cron for daily execution
     */
    private static String buildDailySchedule(WorkflowScheduleDetail detail) {
        if (detail.getExecutionTime() == null) {
            throw new IllegalArgumentException("Execution time required for DAILY schedule");
        }

        // Daily at specific time: 0 minute hour * * ?
        return String.format("0 %d %d * * ?",
                detail.getExecutionTime().getMinute(),
                detail.getExecutionTime().getHour());
    }

    /**
     * Build cron for weekly execution
     */
    private static String buildWeeklySchedule(WorkflowScheduleDetail detail) {
        if (detail.getExecutionTime() == null || detail.getDaysOfWeek() == null || detail.getDaysOfWeek().isEmpty()) {
            throw new IllegalArgumentException("Execution time and days of week required for WEEKLY schedule");
        }

        // Weekly on specific days: 0 minute hour ? * dayOfWeek
        String daysOfWeek = String.join(",", detail.getDaysOfWeek());
        return String.format("0 %d %d ? * %s",
                detail.getExecutionTime().getMinute(),
                detail.getExecutionTime().getHour(),
                daysOfWeek);
    }

    /**
     * Build cron for monthly execution
     */
    private static String buildMonthlySchedule(WorkflowScheduleDetail detail) {
        if (detail.getExecutionTime() == null || detail.getDayOfMonth() == null) {
            throw new IllegalArgumentException("Execution time and day of month required for MONTHLY schedule");
        }

        // Monthly on specific day: 0 minute hour dayOfMonth * ?
        return String.format("0 %d %d %d * ?",
                detail.getExecutionTime().getMinute(),
                detail.getExecutionTime().getHour(),
                detail.getDayOfMonth());
    }

    /**
     * Build custom cron expression
     */
    private static String buildCustomSchedule(WorkflowScheduleDetail detail) {
        if (detail.getCustomCronExpression() == null || detail.getCustomCronExpression().trim().isEmpty()) {
            throw new IllegalArgumentException("Custom cron expression required for CUSTOM schedule");
        }

        // Validate cron expression format (basic validation)
        String[] parts = detail.getCustomCronExpression().trim().split("\\s+");
        if (parts.length != 6 && parts.length != 7) {
            throw new IllegalArgumentException("Invalid cron expression format. Expected 6 or 7 fields.");
        }

        return detail.getCustomCronExpression();
    }

    // ==================== Helper Methods ====================

    /**
     * Convert day name to cron day number
     */
    public static String dayNameToCronDay(String dayName) {
        switch (dayName.toUpperCase()) {
            case "SUNDAY": case "SUN": return "1";
            case "MONDAY": case "MON": return "2";
            case "TUESDAY": case "TUE": return "3";
            case "WEDNESDAY": case "WED": return "4";
            case "THURSDAY": case "THU": return "5";
            case "FRIDAY": case "FRI": return "6";
            case "SATURDAY": case "SAT": return "7";
            default: throw new IllegalArgumentException("Invalid day name: " + dayName);
        }
    }

    /**
     * Validate cron expression format
     */
    public static boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        String[] parts = cronExpression.trim().split("\\s+");
        return parts.length >= 6 && parts.length <= 7;
    }

    /**
     * Get next execution time description
     */
    public static String getScheduleDescription(String scheduleType, WorkflowScheduleDetail detail) {
        switch (scheduleType.toUpperCase()) {
            case "ONCE":
                return String.format("Once on %s at %s",
                        detail.getExecutionDate(), detail.getExecutionTime());
            case "DAILY":
                return String.format("Daily at %s", detail.getExecutionTime());
            case "WEEKLY":
                return String.format("Weekly on %s at %s",
                        String.join(", ", detail.getDaysOfWeek()), detail.getExecutionTime());
            case "MONTHLY":
                return String.format("Monthly on day %d at %s",
                        detail.getDayOfMonth(), detail.getExecutionTime());
            case "CUSTOM":
                return String.format("Custom schedule: %s", detail.getCustomCronExpression());
            default:
                return "Unknown schedule type";
        }
    }

    /**
     * Workflow Schedule Detail DTO
     */
    public static class WorkflowScheduleDetail {
        private LocalDate executionDate;
        private LocalTime executionTime;
        private Integer dayOfMonth;
        private java.util.List<String> daysOfWeek;
        private String customCronExpression;
        private String timezone;

        // Getters and setters
        public LocalDate getExecutionDate() { return executionDate; }
        public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }

        public LocalTime getExecutionTime() { return executionTime; }
        public void setExecutionTime(LocalTime executionTime) { this.executionTime = executionTime; }

        public Integer getDayOfMonth() { return dayOfMonth; }
        public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

        public java.util.List<String> getDaysOfWeek() { return daysOfWeek; }
        public void setDaysOfWeek(java.util.List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

        public String getCustomCronExpression() { return customCronExpression; }
        public void setCustomCronExpression(String customCronExpression) { this.customCronExpression = customCronExpression; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    // ==================== Predefined Cron Expressions ====================

    public static final class CommonCronExpressions {
        // Every minute
        public static final String EVERY_MINUTE = "0 * * * * ?";

        // Every hour
        public static final String EVERY_HOUR = "0 0 * * * ?";

        // Every day at midnight
        public static final String DAILY_MIDNIGHT = "0 0 0 * * ?";

        // Every weekday at 9 AM
        public static final String WEEKDAYS_9AM = "0 0 9 ? * MON-FRI";

        // Every first day of month at 8 AM
        public static final String MONTHLY_FIRST_DAY_8AM = "0 0 8 1 * ?";

        // Every Sunday at 2 AM
        public static final String WEEKLY_SUNDAY_2AM = "0 0 2 ? * SUN";

        // Business hours (9 AM - 5 PM, weekdays)
        public static final String BUSINESS_HOURS = "0 0 9-17 ? * MON-FRI";

        // End of day reconciliation (6 PM daily)
        public static final String EOD_RECONCILIATION = "0 0 18 * * ?";

        // Start of day processing (6 AM daily)
        public static final String SOD_PROCESSING = "0 0 6 * * ?";
    }
}
