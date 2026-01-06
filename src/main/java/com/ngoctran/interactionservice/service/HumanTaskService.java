package com.ngoctran.interactionservice.service;

import com.ngoctran.interactionservice.domain.HumanTask;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.HumanTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanTaskService {

    private final HumanTaskRepository humanTaskRepository;
    private final OnboardingEngine onboardingEngine;

    /**
     * Create a new human task
     */
    @Transactional
    public HumanTask createTask(UUID instanceId, String state, String taskType,
                               String assignedRole, String payload, Instant dueAt) {
        HumanTask task = HumanTask.builder()
                .taskId(UUID.randomUUID())
                .instanceId(instanceId)
                .state(state)
                .taskType(taskType)
                .assignedRole(assignedRole)
                .status("OPEN")
                .priority("NORMAL")
                .payload(payload)
                .createdAt(Instant.now())
                .dueAt(dueAt)
                .build();

        HumanTask saved = humanTaskRepository.save(task);
        log.info("Created human task {} for instance {} state {}", saved.getTaskId(), instanceId, state);
        return saved;
    }

    /**
     * Claim a task for a user
     */
    @Transactional
    public HumanTask claimTask(UUID taskId, String userId) {
        HumanTask task = humanTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (!task.isOpen()) {
            throw new IllegalStateException("Task is not available for claiming: " + taskId);
        }

        task.claim(userId);
        HumanTask saved = humanTaskRepository.save(task);
        log.info("User {} claimed task {}", userId, taskId);
        return saved;
    }

    /**
     * Complete a task with result
     */
    @Transactional
    public HumanTask completeTask(UUID taskId, String userId, String result, String comment) {
        HumanTask task = humanTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (!task.isClaimed() || !userId.equals(task.getAssignedUser())) {
            throw new IllegalStateException("Task not claimed by user: " + taskId);
        }

        task.complete(result);

        // Trigger workflow continuation based on result
        String action = mapResultToAction(task.getTaskType(), result);
        if (action != null) {
            ActionCommand command = ActionCommand.admin(
                    task.getInstanceId(),
                    action,
                    UUID.randomUUID().toString(),
                    userId
            );
            onboardingEngine.handle(command);
        }

        HumanTask saved = humanTaskRepository.save(task);
        log.info("User {} completed task {} with result {}", userId, taskId, result);
        return saved;
    }

    /**
     * Get tasks for a user/role
     */
    public List<HumanTask> getTasksForUser(String userId, String role) {
        // Return both claimed tasks and available tasks for the role
        List<HumanTask> claimedTasks = humanTaskRepository.findClaimedTasksByUser(userId);
        List<HumanTask> availableTasks = humanTaskRepository.findOpenTasksByRole(role);

        claimedTasks.addAll(availableTasks);
        return claimedTasks;
    }

    /**
     * Get overdue tasks
     */
    public List<HumanTask> getOverdueTasks() {
        return humanTaskRepository.findOverdueTasks(Instant.now());
    }

    /**
     * Get task statistics
     */
    public Map<String, Object> getTaskStatistics() {
        return Map.of(
                "totalOpen", humanTaskRepository.countByStatus("OPEN"),
                "totalClaimed", humanTaskRepository.countByStatus("CLAIMED"),
                "totalCompleted", humanTaskRepository.countByStatus("COMPLETED"),
                "riskOfficerOpen", humanTaskRepository.countOpenTasksByRole("RISK_OFFICER"),
                "complianceOfficerOpen", humanTaskRepository.countOpenTasksByRole("COMPLIANCE_OFFICER"),
                "overdueCount", getOverdueTasks().size()
        );
    }

    private String mapResultToAction(String taskType, String result) {
        switch (taskType) {
            case "AML_REVIEW":
                return "CLEAR".equalsIgnoreCase(result) ? "AML_CALLBACK_OK" : "AML_CALLBACK_FAIL";
            case "EKYC_REVIEW":
                return "APPROVE".equalsIgnoreCase(result) ? "EKYC_CALLBACK_OK" : "EKYC_CALLBACK_FAIL";
            case "MANUAL_APPROVAL":
                return "APPROVE".equalsIgnoreCase(result) ? "APPROVE" : "REJECT";
            default:
                return null;
        }
    }
}
