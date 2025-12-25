package com.ngoctran.interactionservice.workflow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Confirm Service Create Request DTO
 *
 * Based on your ConfirmServiceCreateRequest in SchedulerService
 */
@Data
@Builder
public class ConfirmServiceCreateRequest {
    private String userId;
    private String method;
    private int expiresIn;
    private Confirmation confirmation;
    private String transactionType;

    @Data
    @Builder
    public static class Confirmation {
        private String idpUserId;
        private String dbsUserId;
        private String idpIssuer;
        private ConfirmWorkflowRequest transactionData;
        private String callbackUrl;
        private String confirmationType;
    }
}
