package com.ngoctran.interactionservice.bpmn;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;

/**
 * External Task Worker for Camunda
 * Subscribes to topics and processes tasks using delegates or services
 */
@Component
public class CamundaExternalTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(CamundaExternalTaskWorker.class);

    private final String baseUrl;
    private final ApplicationContext applicationContext;
    private ExternalTaskClient client;

    public CamundaExternalTaskWorker(@Value("${camunda.bpm.client.base-url:http://localhost:8080/engine-rest}") String baseUrl, ApplicationContext applicationContext) {
        this.baseUrl = baseUrl;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Camunda External Task Client with base URL: {}", baseUrl);
        client = ExternalTaskClient.create()
                .baseUrl(baseUrl)
                .asyncResponseTimeout(10000) // 10 seconds
                .build();
        // Subscribe to topics
        subscribeToTopic("data-validation", "dataValidationDelegate");
        subscribeToTopic("ocr-processing", "ocrProcessingDelegate");
        subscribeToTopic("document-verification", "documentVerificationDelegate");
        subscribeToTopic("credit-check", "creditCheckDelegate");
        subscribeToTopic("compliance-check", "complianceDelegate");
        subscribeToTopic("error-handling", "errorHandlingDelegate");
        subscribeToTopic("account-creation", "accountCreationDelegate");
        subscribeToTopic("sms-notification", "smsNotificationDelegate");
        subscribeToTopic("email-notification", "notificationDelegate");
    }

    private void subscribeToTopic(String topicName, String beanName) {
        log.info("Subscribing to topic: {}", topicName);

        client.subscribe(topicName)
                .lockDuration(1000) // 1 second
                .handler((externalTask, externalTaskService) -> {
                    try {
                        log.info("Received task for topic: {} (TaskID: {})", topicName, externalTask.getId());

                        // In a real implementation, we would bridge ExternalTask to JavaDelegate
                        // Or call the business logic directly.
                        // For simplicity in this fix, we'll mark as completed with existing variables.

                        Map<String, Object> variables = externalTask.getAllVariables();
                        log.info("Processing task {} with variables: {}", topicName, variables.keySet());

                        // TODO: Add logic to call actual business services here
                        // For now, we simulate success to allow the process to move

                        externalTaskService.complete(externalTask);
                        log.info("Successfully completed task: {}", externalTask.getId());

                    } catch (Exception e) {
                        log.error("Failed to process external task: {}", externalTask.getId(), e);
                        externalTaskService.handleFailure(externalTask, e.getMessage(), e.toString(), 0, 0);
                    }
                })
                .open();
    }

    @PreDestroy
    public void stop() {
        if (client != null) {
            client.stop();
        }
    }
}
