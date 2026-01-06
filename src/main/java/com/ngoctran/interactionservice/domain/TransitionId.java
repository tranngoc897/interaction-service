package com.ngoctran.interactionservice.domain;

import java.io.Serializable;
import java.util.Objects;

public class TransitionId implements Serializable {

    private String flowVersion;
    private String fromState;
    private String action;

    public TransitionId() {}

    public TransitionId(String flowVersion, String fromState, String action) {
        this.flowVersion = flowVersion;
        this.fromState = fromState;
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransitionId that = (TransitionId) o;
        return Objects.equals(flowVersion, that.flowVersion) &&
               Objects.equals(fromState, that.fromState) &&
               Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowVersion, fromState, action);
    }
}
