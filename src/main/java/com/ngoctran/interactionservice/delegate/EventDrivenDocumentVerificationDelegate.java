package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.bpmn.FlowableEventBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-Driven Document Verification Delegate
 */
@Component("eventDrivenDocumentVerificationDelegate")
@RequiredArgsConstructor
@Slf4j
public class EventDrivenDocumentVerificationDelegate implements JavaDelegate {

    private final FlowableEventBridge eventBridge;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String caseId = (String) execution.getVariable("caseId");

        log.info("Creating event-driven document verification job for process: {}, case: {}",
                processInstanceId, caseId);

        Map<String, Object> jobData = Map.of(
            "caseId", caseId,
            "processInstanceId", processInstanceId,
            "applicantId", execution.getVariable("applicantId"),
            "documentTypes", execution.getVariable("requiredDocuments"),
            "verificationRules", Map.of(
                "checkExpiry", true,
                "validateSignatures", true,
                "crossReferenceData", true,
                "fraudDetection", true
            ),
            "timestamp", System.currentTimeMillis()
        );

        String jobId = eventBridge.createExternalJob("document-verification", processInstanceId, jobData);

        execution.setVariable("docVerificationJobId", jobId);
        execution.setVariable("docVerificationStatus", "PENDING");
        execution.setVariable("docVerificationStartedAt", System.currentTimeMillis());

        log.info("Created document verification job: {} for case: {}", jobId, caseId);
    }
}
