package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.bpmn.FlowableEventBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-Driven Account Creation Delegate
 */
@Component("eventDrivenAccountCreationDelegate")
@RequiredArgsConstructor
@Slf4j
public class EventDrivenAccountCreationDelegate implements JavaDelegate {

    private final FlowableEventBridge eventBridge;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String caseId = (String) execution.getVariable("caseId");

        log.info("Creating event-driven account creation job for process: {}, case: {}",
                processInstanceId, caseId);

        Map<String, Object> jobData = Map.of(
            "caseId", caseId,
            "processInstanceId", processInstanceId,
            "applicantId", execution.getVariable("applicantId"),
            "accountDetails", Map.of(
                "accountType", execution.getVariable("approvedAccountType"),
                "productCode", execution.getVariable("productCode"),
                "branchCode", execution.getVariable("branchCode"),
                "currency", execution.getVariable("currency"),
                "initialDeposit", execution.getVariable("initialDepositAmount")
            ),
            "customerInfo", Map.of(
                "customerId", execution.getVariable("customerId"),
                "customerType", execution.getVariable("customerType"),
                "riskProfile", execution.getVariable("riskProfile"),
                "segment", execution.getVariable("customerSegment")
            ),
            "complianceInfo", Map.of(
                "amlStatus", execution.getVariable("amlStatus"),
                "kycStatus", execution.getVariable("kycStatus"),
                "complianceApproved", true
            ),
            "timestamp", System.currentTimeMillis()
        );

        String jobId = eventBridge.createExternalJob("account-creation", processInstanceId, jobData);

        execution.setVariable("accountCreationJobId", jobId);
        execution.setVariable("accountCreationStatus", "PENDING");
        execution.setVariable("accountCreationStartedAt", System.currentTimeMillis());

        log.info("Created account creation job: {} for case: {}", jobId, caseId);
    }
}
