package com.ngoctran.interactionservice.workflow.onboarding;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.List;
import java.util.Map;

@WorkflowInterface
public interface DynamicStepWorkflow {

    @WorkflowMethod
    Map<String, Object> executeStepActions(String caseId, List<Map<String, Object>> actions,
            Map<String, Object> initialData);

}
