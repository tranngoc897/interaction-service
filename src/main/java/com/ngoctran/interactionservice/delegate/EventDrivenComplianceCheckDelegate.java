package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.bpmn.FlowableEventBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-Driven Compliance Check Delegate
 */
@Component("eventDrivenComplianceCheckDelegate")
@RequiredArgsConstructor
@Slf4j
public class EventDrivenComplianceCheckDelegate implements JavaDelegate {

    private final FlowableEventBridge eventBridge;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String caseId = (String) execution.getVariable("caseId");

        log.info("Creating event-driven compliance check job for process: {}, case: {}",
                processInstanceId, caseId);

        Map<String, Object> jobData = Map.of(
            "caseId", caseId,
            "processInstanceId", processInstanceId,
            "applicantId", execution.getVariable("applicantId"),
            "complianceChecks", Map.of(
                "aml", Map.of(
                    "enabled", true,
                    "riskThreshold", "HIGH",
                    "checkPEP", true,
                    "checkSanctions", true
                ),
                "kyc", Map.of(
                    "enabled", true,
                    "documentVerification", true,
                    "biometricCheck", false,
                    "addressVerification", true
                ),
                "fraud", Map.of(
                    "enabled", true,
                    "velocityChecks", true,
                    "deviceFingerprinting", true,
                    "behaviorAnalysis", false
                )
            ),
            "jurisdiction", execution.getVariable("jurisdiction"),
            "timestamp", System.currentTimeMillis()
        );

        String jobId = eventBridge.createExternalJob("compliance-check", processInstanceId, jobData);

        execution.setVariable("complianceCheckJobId", jobId);
        execution.setVariable("complianceCheckStatus", "PENDING");
        execution.setVariable("complianceCheckStartedAt", System.currentTimeMillis());

        log.info("Created compliance check job: {} for case: {}", jobId, caseId);
    }
}
