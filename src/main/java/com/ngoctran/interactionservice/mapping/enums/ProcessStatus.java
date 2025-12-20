package com.ngoctran.interactionservice.mapping.enums;

public enum ProcessStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    TERMINATED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == TERMINATED;
    }
}
