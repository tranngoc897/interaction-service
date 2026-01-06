package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.domain.HumanTask;
import com.ngoctran.interactionservice.service.HumanTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/human-tasks")
@RequiredArgsConstructor
public class HumanTaskController {

    private final HumanTaskService humanTaskService;

    /**
     * Get dashboard statistics for human tasks
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        try {
            Map<String, Object> stats = humanTaskService.getTaskStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception ex) {
            log.error("Error getting human task dashboard: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get dashboard",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Get tasks for a specific user/role
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTasks(
            @RequestParam String userId,
            @RequestParam String role) {

        try {
            List<HumanTask> tasks = humanTaskService.getTasksForUser(userId, role);
            List<Map<String, Object>> taskSummaries = tasks.stream()
                    .map(this::toTaskSummary)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(taskSummaries);

        } catch (Exception ex) {
            log.error("Error getting tasks for user {} role {}: {}", userId, role, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    /**
     * Get detailed task information
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskDetails(@PathVariable UUID taskId) {
        try {
            // For now, return basic task info - in real implementation, fetch from repository
            // HumanTask task = humanTaskService.getTask(taskId);
            return ResponseEntity.ok(Map.of(
                    "taskId", taskId,
                    "message", "Task details endpoint - implement HumanTaskService.getTask()"
            ));

        } catch (Exception ex) {
            log.error("Error getting task details for {}: {}", taskId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get task details",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Claim a task
     */
    @PostMapping("/{taskId}/claim")
    public ResponseEntity<Map<String, Object>> claimTask(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> request) {

        try {
            String userId = (String) request.get("userId");
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "userId is required"
                ));
            }

            HumanTask task = humanTaskService.claimTask(taskId, userId);

            return ResponseEntity.ok(Map.of(
                    "taskId", task.getTaskId(),
                    "status", task.getStatus(),
                    "assignedUser", task.getAssignedUser(),
                    "claimedAt", task.getClaimedAt(),
                    "message", "Task claimed successfully"
            ));

        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Invalid claim request for task {}: {}", taskId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid claim request",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error claiming task {}: {}", taskId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to claim task",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Complete a task
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> request) {

        try {
            String userId = (String) request.get("userId");
            String result = (String) request.get("result");
            String comment = (String) request.get("comment");

            if (userId == null || result == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "userId and result are required"
                ));
            }

            HumanTask task = humanTaskService.completeTask(taskId, userId, result, comment);

            return ResponseEntity.ok(Map.of(
                    "taskId", task.getTaskId(),
                    "status", task.getStatus(),
                    "result", task.getResult(),
                    "completedAt", task.getCompletedAt(),
                    "message", "Task completed successfully"
            ));

        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Invalid complete request for task {}: {}", taskId, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid complete request",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error completing task {}: {}", taskId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to complete task",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * Get overdue tasks
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<Map<String, Object>>> getOverdueTasks() {
        try {
            List<HumanTask> overdueTasks = humanTaskService.getOverdueTasks();
            List<Map<String, Object>> taskSummaries = overdueTasks.stream()
                    .map(this::toTaskSummary)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(taskSummaries);

        } catch (Exception ex) {
            log.error("Error getting overdue tasks: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    private Map<String, Object> toTaskSummary(HumanTask task) {
        return Map.of(
                "taskId", task.getTaskId(),
                "instanceId", task.getInstanceId(),
                "taskType", task.getTaskType(),
                "status", task.getStatus(),
                "priority", task.getPriority(),
                "assignedRole", task.getAssignedRole(),
                "assignedUser", task.getAssignedUser(),
                "createdAt", task.getCreatedAt(),
                "dueAt", task.getDueAt(),
                "isOverdue", task.isOverdue()
        );
    }
}
