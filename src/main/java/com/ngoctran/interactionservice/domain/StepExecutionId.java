package com.ngoctran.interactionservice.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class StepExecutionId implements Serializable {

    private UUID instanceId;
    private String state;

    public StepExecutionId() {}

    public StepExecutionId(UUID instanceId, String state) {
        this.instanceId = instanceId;
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StepExecutionId that = (StepExecutionId) o;
        return Objects.equals(instanceId, that.instanceId) &&
               Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, state);
    }
}
