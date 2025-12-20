package com.ngoctran.interactionservice.mapping.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProcessMappingNotFoundException extends RuntimeException {
    public ProcessMappingNotFoundException(String processInstanceId) {
        super("Process mapping not found: " + processInstanceId);
    }
}
