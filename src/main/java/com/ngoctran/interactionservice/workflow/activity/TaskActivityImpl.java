package com.ngoctran.interactionservice.workflow.activity;

import com.ngoctran.interactionservice.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskActivityImpl implements TaskActivity {

    private final TaskService taskService;

    @Override
    public void createManualTask(String caseId, String interactionId, String taskType, String payload) {
        taskService.createTask(caseId, interactionId, taskType, payload);
    }
}
