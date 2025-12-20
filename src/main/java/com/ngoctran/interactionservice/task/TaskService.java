package com.ngoctran.interactionservice.task;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskEntity createTask(String caseId, String interactionId, String taskType, String payload) {
        TaskEntity task = new TaskEntity();
        task.setCaseId(caseId);
        task.setInteractionId(interactionId);
        task.setTaskType(taskType);
        task.setPayload(payload);
        task.setStatus(TaskStatus.PENDING);
        return taskRepository.save(task);
    }

    @Transactional
    public TaskEntity completeTask(String taskId, String result, String userId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        
        task.setStatus(TaskStatus.COMPLETED);
        task.setResult(result);
        task.setAssigneeId(userId);
        task.setCompletedAt(LocalDateTime.now());
        
        return taskRepository.save(task);
    }

    public List<TaskEntity> getPendingTasksForUser(String userId) {
        return taskRepository.findByAssigneeIdAndStatus(userId, TaskStatus.PENDING);
    }
}
