package com.ngoctran.interactionservice.bpmn;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Modern, Cloud-Native External Task Worker for Kubernetes.
 * Uses the Spring Boot Starter for Camunda External Task Client.
 * This approach integrates perfectly with Spring Boot lifecycle,
 * Actuator health checks, and is easily scalable in K8s.
 */
@Component
public class CamundaExternalTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(CamundaExternalTaskWorker.class);

    @Component
    @ExternalTaskSubscription("data-validation")
    public static class DataValidationHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing Data Validation for Task: {}", externalTask.getId());
            // Map variables and call business logic here
            externalTaskService.complete(externalTask);
        }
    }

    @Component
    @ExternalTaskSubscription("ocr-processing")
    public static class OcrProcessingHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing OCR for Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask);
        }
    }

    @Component
    @ExternalTaskSubscription("document-verification")
    public static class DocumentVerificationHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing Document Verification for Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask);
        }
    }

    @Component
    @ExternalTaskSubscription("credit-check")
    public static class CreditCheckHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing Credit Check for Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask);
        }
    }

    @Component
    @ExternalTaskSubscription("compliance-check")
    public static class ComplianceCheckHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing Compliance Check for Task: {}", externalTask.getId());
            // Example: setting a required variable for the gateway
            Map<String, Object> variables = Map.of("complianceStatus", "PASSED");
            externalTaskService.complete(externalTask, variables);
        }
    }

    @Component
    @ExternalTaskSubscription("account-creation")
    public static class AccountCreationHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing Account Creation for Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask);
        }
    }

    @Component
    @ExternalTaskSubscription("sms-notification")
    public static class SmsNotificationHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing SMS Notification for Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask);
        }
    }

    @Component
    @ExternalTaskSubscription("email-notification")
    public static class EmailNotificationHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.info("Processing Email Notification for Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask);
        }
    }

    @Component
    @ExternalTaskSubscription("error-handling")
    public static class ErrorHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            log.warn("Handling Process Error for Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask);
        }
    }
}
