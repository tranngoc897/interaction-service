package com.ngoctran.interactionservice.dto;

import java.util.Map;

public record StepSubmissionDto(
    String stepName,
    Map<String, Object> stepData,
    Map<String, Object> clientContext
) {}