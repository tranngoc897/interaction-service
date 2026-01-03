package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.bpmn.FlowableEventBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-Driven Data Validation Delegate
 *
 * Instead of processing data directly, this delegate creates an external job
 * and publishes an event to Kafka for asynchronous processing.
 *
 * This enables real-time, scalable external job processing.
 */
@Component("eventDrivenDataValidationDelegate")
@RequiredArgsConstructor
@Slf4j
public class EventDrivenDataValidationDelegate implements JavaDelegate {

    private final FlowableEventBridge eventBridge;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String caseId = (String) execution.getVariable("caseId");

        log.info("Creating event-driven data validation job for process: {}, case: {}",
                processInstanceId, caseId);

        // Prepare job data
        Map<String, Object> jobData = Map.of(
            "caseId", caseId,
            "processInstanceId", processInstanceId,
            "applicantId", execution.getVariable("applicantId"),
            "validationType", "FULL_VALIDATION",
            "timestamp", System.currentTimeMillis()
        );

        // Create external job and publish event
        String jobId = eventBridge.createExternalJob("data-validation", processInstanceId, jobData);

        // Store job tracking information
        execution.setVariable("dataValidationJobId", jobId);
        execution.setVariable("dataValidationStatus", "PENDING");
        execution.setVariable("dataValidationStartedAt", System.currentTimeMillis());

        log.info("Created data validation job: {} for case: {}", jobId, caseId);
    }
}
