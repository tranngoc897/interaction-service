package com.ngoctran.interactionservice.step;

import lombok.Value;

@Value
public class StepResult {
    boolean success;
    StepError error;

    public static StepResult success() {
        return new StepResult(true, null);
    }

    public static StepResult failure(StepError error) {
        return new StepResult(false, error);
    }
}
