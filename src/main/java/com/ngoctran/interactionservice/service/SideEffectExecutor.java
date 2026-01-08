package com.ngoctran.interactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.domain.WorkflowEvent;
import com.ngoctran.interactionservice.engine.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class SideEffectExecutor {

    private final WorkflowHistoryService historyService;
    private final ObjectMapper objectMapper;

    /**
     * Executes logic if not replaying, or returns the saved result if replaying.
     * name: Unique name for this side effect within the workflow step.
     */
    public <T> T execute(UUID instanceId, String name, Class<T> returnType, Supplier<T> logic) {
        WorkflowContext ctx = WorkflowContext.get();

        if (ctx != null && ctx.isReplaying()) {
            // REPLAY MODE: Lookup in history
            WorkflowEvent event = ctx.nextEvent("SIDE_EFFECT", name);
            if (event != null) {
                log.info("[REPLAY] Using recorded result for side-effect: {}", name);
                try {
                    return objectMapper.readValue(event.getPayload(), returnType);
                } catch (Exception e) {
                    log.error("Failed to deserialize side-effect result for {}", name, e);
                }
            }
            log.warn("[REPLAY] No recorded result found for side-effect: {}. Fallback to execution.", name);
        }

        // EXECUTION MODE: Run the actual logic
        T result = logic.get();

        // Record the result for future replays
        if (ctx == null || !ctx.isReplaying()) {
            historyService.recordEvent(instanceId, "SIDE_EFFECT", name, result, "SYSTEM");
        }

        return result;
    }
}
