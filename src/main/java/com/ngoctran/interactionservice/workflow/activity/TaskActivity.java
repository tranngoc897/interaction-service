package com.ngoctran.interactionservice.workflow.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TaskActivity {

    @ActivityMethod
    void createManualTask(String caseId, String interactionId, String taskType, String payload);
}
