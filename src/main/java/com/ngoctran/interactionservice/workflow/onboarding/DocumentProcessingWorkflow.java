package com.ngoctran.interactionservice.workflow.onboarding;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.Map;

@WorkflowInterface
public interface DocumentProcessingWorkflow {

    @WorkflowMethod
    Map<String, Object> processDocuments(String caseId, Map<String, Object> initialData);
}
