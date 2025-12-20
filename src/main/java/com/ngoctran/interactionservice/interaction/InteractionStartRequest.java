package com.ngoctran.interactionservice.interaction;

public record InteractionStartRequest(
    String userId,
    String interactionDefinitionKey,
    Long interactionDefinitionVersion
) {}
