package com.ngoctran.interactionservice.workflow.payment;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CaseMonitorWorkflow {

    @WorkflowMethod
    void monitorCase(String caseId, int iterationCount);

    @SignalMethod
    void updateStatus(String status);
}
