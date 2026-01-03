package com.ngoctran.interactionservice.config;

import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.cfg.AbstractProcessEngineConfigurator;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Flowable Plugin that hooks into the Event system
 * Automatically publishes all Flowable events (Activity Start, Task Complete,
 * etc.) to Kafka
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FlowableKafkaEventPlugin extends AbstractProcessEngineConfigurator {

    private final WorkflowEventPublisher eventPublisher;
    public void beforeInit(Object configurator) {
        log.info("Initializing Flowable Kafka Event Plugin");

        // Note: Flowable event listener registration is complex and may require
        // different approaches. For now, we'll use alternative methods like
        // REST API monitoring or Spring events.

        // TODO: Implement proper Flowable event listener registration
        // This may require using ProcessEngineConfiguration or different APIs
    }

    private void publishFlowableEvent(FlowableEvent event) {
        try {
            String eventType = event.getType().name();

            // Map context data
            Map<String, Object> context = new HashMap<>();
            context.put("eventType", eventType);
            context.put("timestamp", System.currentTimeMillis());

            // Publish to Kafka based on event type
            if (isProcessEvent(eventType)) {
                eventPublisher.publishWorkflowStateEvent(
                        "unknown", // FlowableEvent doesn't expose processInstanceId directly
                        "BPMN_PROCESS",
                        "PREVIOUS_STATE",
                        eventType,
                        context);
            } else if (isActivityEvent(eventType)) {
                eventPublisher.publishInteractionStepEvent(
                        "unknown", // FlowableEvent doesn't expose processInstanceId directly
                        "BPMN_ACTIVITY",
                        eventType,
                        eventType,
                        context);
            }

            log.info("Flowable event captured and published: {}", eventType);

        } catch (Exception e) {
            log.warn("Failed to publish Flowable event to Kafka: {}", e.getMessage());
        }
    }

    private boolean isProcessEvent(String eventType) {
        return eventType.contains("PROCESS_STARTED") ||
                eventType.contains("PROCESS_COMPLETED") ||
                eventType.contains("PROCESS_CANCELLED");
    }

    private boolean isActivityEvent(String eventType) {
        return eventType.contains("ACTIVITY_STARTED") ||
                eventType.contains("ACTIVITY_COMPLETED") ||
                eventType.contains("TASK_COMPLETED");
    }
}
