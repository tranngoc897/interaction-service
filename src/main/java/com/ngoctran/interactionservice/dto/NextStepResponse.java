package com.ngoctran.interactionservice.dto;

import java.util.Map;

public record NextStepResponse(
    String nextStep,
    Map<String, Object> uiModel,
    String status
) {}