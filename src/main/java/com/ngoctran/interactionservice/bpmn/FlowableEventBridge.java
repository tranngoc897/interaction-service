package com.ngoctran.interactionservice.bpmn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bridge component that runs in Flowable service to publish events to Kafka.
 *
 * This component should be deployed with the Flowable standalone service
 * to publish external job creation events to Kafka topics.
 *
 * Usage: Call this from Flowable service tasks or event listeners
 * when external jobs are created.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlowableEventBridge {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish external job creation event to Kafka
     *
     * This should be called from Flowable when an external job is created
     * (e.g., from a service task that creates external jobs)
     */
    public void publishExternalJobCreated(String jobId, String topic, String processInstanceId,
            Map<String, Object> jobData) {

        Map<String, Object> event = Map.of(
            "eventType", "EXTERNAL_JOB_CREATED",
            "jobId", jobId,
            "topic", topic,
            "processInstanceId", processInstanceId,
            "jobData", jobData != null ? jobData : Map.of(),
            "timestamp", System.currentTimeMillis(),
            "source", "flowable-engine"
        );

        try {
            kafkaTemplate.send("flowable-external-jobs", jobId, event);
            log.info("Published external job creation event for job: {} topic: {}", jobId, topic);
        } catch (Exception e) {
            log.error("Failed to publish external job event for job: {}", jobId, e);
        }
    }

    /**
     * Publish process instance events
     */
    public void publishProcessEvent(String processInstanceId, String eventType,
            String processDefinitionKey, Map<String, Object> eventData) {

        Map<String, Object> event = Map.of(
            "eventType", eventType,
            "processInstanceId", processInstanceId,
            "processDefinitionKey", processDefinitionKey,
            "eventData", eventData != null ? eventData : Map.of(),
            "timestamp", System.currentTimeMillis(),
            "source", "flowable-engine"
        );

        try {
            kafkaTemplate.send("flowable-process-events", processInstanceId, event);
            log.debug("Published process event: {} for process: {}", eventType, processInstanceId);
        } catch (Exception e) {
            log.error("Failed to publish process event: {}", e.getMessage());
        }
    }

    /**
     * Publish task events
     */
    public void publishTaskEvent(String taskId, String taskName, String eventType,
            String processInstanceId, Map<String, Object> taskData) {

        Map<String, Object> event = Map.of(
            "eventType", eventType,
            "taskId", taskId,
            "taskName", taskName,
            "processInstanceId", processInstanceId,
            "taskData", taskData != null ? taskData : Map.of(),
            "timestamp", System.currentTimeMillis(),
            "source", "flowable-engine"
        );

        try {
            kafkaTemplate.send("flowable-task-events", taskId, event);
            log.debug("Published task event: {} for task: {}", eventType, taskId);
        } catch (Exception e) {
            log.error("Failed to publish task event: {}", e.getMessage());
        }
    }

    /**
     * Utility method to create external jobs and publish events
     *
     * This can be called from Flowable Java delegates to create external jobs
     * and automatically publish the creation event
     */
    public String createExternalJob(String topic, String processInstanceId, Map<String, Object> jobData) {
        // Generate unique job ID
        String jobId = topic + "-" + processInstanceId + "-" + System.currentTimeMillis();

        // Here you would typically create the job in Flowable's database
        // For now, just publish the event
        publishExternalJobCreated(jobId, topic, processInstanceId, jobData);

        log.info("Created external job: {} for topic: {}", jobId, topic);
        return jobId;
    }
}
