package com.ngoctran.interactionservice.step;

import lombok.Value;

@Value
public class StepError {
    String code;
    ErrorType type;
    String message;

    public StepError(String code, ErrorType type, String message) {
        this.code = code;
        this.type = type;
        this.message = message;
    }
}
