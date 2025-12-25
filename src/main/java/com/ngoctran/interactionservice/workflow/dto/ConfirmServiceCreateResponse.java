package com.ngoctran.interactionservice.workflow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Confirm Service Create Response DTO
 *
 * Based on your ConfirmServiceCreateResponse in SchedulerService
 */
@Data
@Builder
public class ConfirmServiceCreateResponse {
    private String confirmationId;
    private String txnRefNumber;
}
