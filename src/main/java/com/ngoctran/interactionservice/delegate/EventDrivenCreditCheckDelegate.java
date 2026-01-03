package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.bpmn.FlowableEventBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-Driven Credit Check Delegate
 */
@Component("eventDrivenCreditCheckDelegate")
@RequiredArgsConstructor
@Slf4j
public class EventDrivenCreditCheckDelegate implements JavaDelegate {

    private final FlowableEventBridge eventBridge;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String caseId = (String) execution.getVariable("caseId");

        log.info("Creating event-driven credit check job for process: {}, case: {}",
                processInstanceId, caseId);

        Map<String, Object> jobData = Map.of(
            "caseId", caseId,
            "processInstanceId", processInstanceId,
            "applicantId", execution.getVariable("applicantId"),
            "creditCheckRequest", Map.of(
                "ssn", execution.getVariable("socialSecurityNumber"),
                "address", execution.getVariable("currentAddress"),
                "employmentInfo", execution.getVariable("employmentDetails"),
                "bankReferences", execution.getVariable("bankAccounts")
            ),
            "bureauConfig", Map.of(
                "primaryBureau", "EXPERIAN",
                "secondaryBureau", "EQUIFAX",
                "tertiaryBureau", "TRANSUNION",
                "scoreModel", "V2"
            ),
            "timestamp", System.currentTimeMillis()
        );

        String jobId = eventBridge.createExternalJob("credit-check", processInstanceId, jobData);

        execution.setVariable("creditCheckJobId", jobId);
        execution.setVariable("creditCheckStatus", "PENDING");
        execution.setVariable("creditCheckStartedAt", System.currentTimeMillis());

        log.info("Created credit check job: {} for case: {}", jobId, caseId);
    }
}
