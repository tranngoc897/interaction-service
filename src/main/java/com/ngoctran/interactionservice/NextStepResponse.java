package com.ngoctran.interactionservice;

import java.util.Map;

public record NextStepResponse(
    String nextStep,
    Map<String, Object> uiModel,
    String status
) {}