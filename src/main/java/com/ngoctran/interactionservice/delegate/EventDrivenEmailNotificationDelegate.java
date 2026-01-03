package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.bpmn.FlowableEventBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-Driven Email Notification Delegate
 */
@Component("eventDrivenEmailNotificationDelegate")
@RequiredArgsConstructor
@Slf4j
public class EventDrivenEmailNotificationDelegate implements JavaDelegate {

    private final FlowableEventBridge eventBridge;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String caseId = (String) execution.getVariable("caseId");

        log.info("Creating event-driven email notification job for process: {}, case: {}",
                processInstanceId, caseId);

        Map<String, Object> jobData = Map.of(
            "caseId", caseId,
            "processInstanceId", processInstanceId,
            "applicantId", execution.getVariable("applicantId"),
            "notificationType", "WELCOME_EMAIL",
            "emailDetails", Map.of(
                "to", execution.getVariable("emailAddress"),
                "template", "welcome-customer",
                "language", execution.getVariable("preferredLanguage"),
                "accountNumber", execution.getVariable("accountNumber"),
                "customerName", execution.getVariable("customerName")
            ),
            "deliveryOptions", Map.of(
                "priority", "HIGH",
                "scheduledTime", null, // Send immediately
                "maxRetries", 3,
                "trackOpens", true,
                "trackClicks", true
            ),
            "context", Map.of(
                "processStatus", "COMPLETED",
                "onboardingDate", System.currentTimeMillis(),
                "branchInfo", execution.getVariable("branchInfo"),
                "productInfo", execution.getVariable("productInfo")
            ),
            "timestamp", System.currentTimeMillis()
        );

        String jobId = eventBridge.createExternalJob("email-notification", processInstanceId, jobData);

        execution.setVariable("emailNotificationJobId", jobId);
        execution.setVariable("emailNotificationStatus", "PENDING");
        execution.setVariable("emailNotificationStartedAt", System.currentTimeMillis());

        log.info("Created email notification job: {} for case: {}", jobId, caseId);
    }
}
