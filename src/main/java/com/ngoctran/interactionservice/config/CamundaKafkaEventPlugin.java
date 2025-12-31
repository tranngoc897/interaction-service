package com.ngoctran.interactionservice.config;

import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camunda Plugin that hooks into the History Event system
 * Automatically publishes all Camunda events (Activity Start, Task Complete,
 * etc.) to Kafka
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CamundaKafkaEventPlugin extends AbstractProcessEnginePlugin {

    private final WorkflowEventPublisher eventPublisher;

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        log.info("Initializing Camunda Kafka Event Plugin");

        // Add custom history event handler
        HistoryEventHandler historyEventHandler = new HistoryEventHandler() {
            @Override
            public void handleEvent(HistoryEvent historyEvent) {
                publishHistoryEvent(historyEvent);
            }

            @Override
            public void handleEvents(List<HistoryEvent> historyEvents) {
                for (HistoryEvent historyEvent : historyEvents) {
                    handleEvent(historyEvent);
                }
            }
        };

        processEngineConfiguration.setHistoryEventHandler(historyEventHandler);
    }

    private void publishHistoryEvent(HistoryEvent event) {
        try {
            String eventType = event.getEventType();

            // Map context data
            Map<String, Object> context = new HashMap<>();
            context.put("processInstanceId", event.getProcessInstanceId());
            context.put("processDefinitionId", event.getProcessDefinitionId());
            context.put("executionId", event.getExecutionId());

            // Filter and publish specific events to keep Kafka clean
            if (isProcessEvent(eventType)) {
                eventPublisher.publishWorkflowStateEvent(
                        event.getProcessInstanceId(),
                        "BPMN_PROCESS",
                        "PREVIOUS_STATE", // History doesn't easily give "old" state in a single event
                        eventType,
                        context);
            } else if (isActivityEvent(eventType)) {
                eventPublisher.publishInteractionStepEvent(
                        event.getProcessInstanceId(),
                        "BPMN_ACTIVITY",
                        event.getEventType(), // stepName as event type
                        eventType, // action
                        context);
            }

            log.trace("Camunda event captured: {} for process {}", eventType, event.getProcessInstanceId());
        } catch (Exception e) {
            log.warn("Failed to publish Camunda history event to Kafka: {}", e.getMessage());
        }
    }

    private boolean isProcessEvent(String eventType) {
        return eventType.equals(HistoryEventTypes.PROCESS_INSTANCE_START.getEventName()) ||
                eventType.equals(HistoryEventTypes.PROCESS_INSTANCE_END.getEventName());
    }

    private boolean isActivityEvent(String eventType) {
        return eventType.equals(HistoryEventTypes.ACTIVITY_INSTANCE_START.getEventName()) ||
                eventType.equals(HistoryEventTypes.ACTIVITY_INSTANCE_END.getEventName()) ||
                eventType.equals(HistoryEventTypes.TASK_INSTANCE_COMPLETE.getEventName());
    }
}
