package com.ngoctran.interactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.domain.WorkflowEvent;
import com.ngoctran.interactionservice.repo.WorkflowEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowHistoryService {

    private final WorkflowEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public WorkflowEventRepository getEventRepository() {
        return eventRepository;
    }

    /**
     * Record an event in the history
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(UUID instanceId, String type, String name, Object payload, String actor) {
        recordEvent(instanceId, type, name, payload, actor, 1); // Default to version 1
    }

    /**
     * Record an event in the history with specific code version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(UUID instanceId, String type, String name, Object payload, String actor, int codeVersion) {
        try {
            int nextSeq = eventRepository.findMaxSequenceNumber(instanceId) + 1;

            WorkflowEvent event = WorkflowEvent.builder()
                    .instanceId(instanceId)
                    .eventType(type)
                    .eventName(name)
                    .payload(objectMapper.writeValueAsString(payload))
                    .sequenceNumber(nextSeq)
                    .codeVersion(codeVersion)
                    .createdBy(actor)
                    .build();

            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record workflow event for instance {}", instanceId, e);
        }
    }

    /**
     * Replay workflow history to reconstruct state or validate logic changes.
     * Re-executes all historical ACTION_RECEIVED events in replay mode.
     */
    public void replay(java.util.UUID instanceId, com.ngoctran.interactionservice.engine.OnboardingEngine engine) {
        log.info("Starting replay for instance: {}", instanceId);

        java.util.List<WorkflowEvent> history = eventRepository.findByInstanceIdOrderBySequenceNumberAsc(instanceId);

        if (history.isEmpty()) {
            log.warn("No history found for instance: {}", instanceId);
            return;
        }

        com.ngoctran.interactionservice.engine.WorkflowContext ctx = new com.ngoctran.interactionservice.engine.WorkflowContext();
        ctx.setReplaying(true);
        ctx.setHistory(history);

        com.ngoctran.interactionservice.engine.WorkflowContext.set(ctx);
        try {
            // Find all historical actions and re-run them
            for (WorkflowEvent event : history) {
                if ("ACTION_RECEIVED".equals(event.getEventType())) {
                    try {
                        com.ngoctran.interactionservice.engine.ActionCommand cmd = objectMapper.readValue(
                                event.getPayload(), com.ngoctran.interactionservice.engine.ActionCommand.class);

                        log.info("[REPLAY] Re-executing action: {}", cmd.getAction());
                        engine.handle(cmd);
                    } catch (Exception e) {
                        log.error("[REPLAY] Error during replay of action {}: {}", event.getEventName(),
                                e.getMessage());
                    }
                }
            }
        } finally {
            com.ngoctran.interactionservice.engine.WorkflowContext.clear();
            log.info("Replay finished for instance: {}", instanceId);
        }
    }
}
