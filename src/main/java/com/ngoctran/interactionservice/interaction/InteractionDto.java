package com.ngoctran.interactionservice.interaction;

import java.util.Map;

public record InteractionDto(
    String interactionId,
    String status,
    String stepName,
    String stepStatus,
    boolean resumable,
    Map<String, Object> tempData,
    Map<String, Object> caseData
) {}
