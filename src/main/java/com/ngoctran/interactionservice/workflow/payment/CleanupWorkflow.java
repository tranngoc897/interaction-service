package com.ngoctran.interactionservice.workflow.payment;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CleanupWorkflow {

    @WorkflowMethod
    void runCleanup();
}
