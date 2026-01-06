package com.ngoctran.interactionservice.step;

import java.util.UUID;

public interface StepHandler {
    StepResult execute(UUID instanceId);
}
