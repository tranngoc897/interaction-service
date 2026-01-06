package com.ngoctran.interactionservice.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class WorkflowException extends RuntimeException {

    private final String errorCode;
    private final String errorType;
    private final Map<String, Object> details;

    public WorkflowException(String errorCode, String message, String errorType) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.details = null;
    }

    public WorkflowException(String errorCode, String message, String errorType, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.details = details;
    }

    public WorkflowException(String errorCode, String message, String errorType, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.details = null;
    }

    public WorkflowException(String errorCode, String message, String errorType, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.details = details;
    }
}
