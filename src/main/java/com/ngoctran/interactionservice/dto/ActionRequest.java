package com.ngoctran.interactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for performing workflow actions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequest {

    private String action;
    private String requestId;
    private Map<String, Object> data;
    private String comment;

    public ActionRequest(String action) {
        this.action = action;
    }

    public ActionRequest(String action, String requestId) {
        this.action = action;
        this.requestId = requestId;
    }
}
