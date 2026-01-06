package com.ngoctran.interactionservice.engine;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ActionCommand {
    UUID instanceId;
    String action;
    String actor;        // USER, ADMIN, SYSTEM, KAFKA
    String requestId;    // idempotent
    String comment;
    Instant occurredAt;

    public static ActionCommand user(UUID instanceId, String action, String requestId) {
        return ActionCommand.builder()
                .instanceId(instanceId)
                .action(action)
                .actor("USER")
                .requestId(requestId)
                .occurredAt(Instant.now())
                .build();
    }

    public static ActionCommand admin(UUID instanceId, String action, String requestId, String operator) {
        return ActionCommand.builder()
                .instanceId(instanceId)
                .action(action)
                .actor("ADMIN")
                .requestId(requestId)
                .comment(operator)
                .occurredAt(Instant.now())
                .build();
    }

    public static ActionCommand system(UUID instanceId, String action) {
        return ActionCommand.builder()
                .instanceId(instanceId)
                .action(action)
                .actor("SYSTEM")
                .requestId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .build();
    }

    public static ActionCommand kafka(UUID instanceId, String action, String eventId) {
        return ActionCommand.builder()
                .instanceId(instanceId)
                .action(action)
                .actor("KAFKA")
                .requestId(eventId)
                .occurredAt(Instant.now())
                .build();
    }
}
