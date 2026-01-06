package com.ngoctran.interactionservice.step;

import lombok.Value;

import java.util.UUID;

@Value
public class StepContext {
    UUID instanceId;
    String state;
    String flowVersion;
}
