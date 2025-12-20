package com.ngoctran.interactionservice;

import com.ngoctran.interactionservice.activity.onboarding.OnboardingWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class WorkflowClientWrapper {
    private final WorkflowClient client;

    public WorkflowClientWrapper() {
        var service = WorkflowServiceStubs.newInstance();
        this.client = WorkflowClient.newInstance(service);
    }

    public String startOnboardingWorkflow(String caseId, Map<String,Object> input) {
        var options = WorkflowOptions.newBuilder()
                .setTaskQueue("ONBOARDING_TASK_QUEUE")
                .setWorkflowExecutionTimeout(Duration.ofHours(4))
                .build();

        var stub = client.newWorkflowStub(OnboardingWorkflow.class, options);

        // start asynchronously
        WorkflowClient.start(() -> stub.startOnboarding(caseId, input));
        // obtain runId & workflowId via WorkflowStub
        WorkflowStub workflowStub = WorkflowStub.fromTyped(stub);
        String workflowId = workflowStub.getExecution().getWorkflowId();
        String runId = workflowStub.getExecution().getRunId();
        return workflowId + ":" + runId;
    }
}