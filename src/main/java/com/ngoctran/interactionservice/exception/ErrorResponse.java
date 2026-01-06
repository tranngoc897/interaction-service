package com.ngoctran.interactionservice.exception;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class ErrorResponse {
    String correlationId;
    String code;
    String message;
    String type;
    Map<String, Object> details;
    Instant timestamp;
}
