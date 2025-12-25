package com.ngoctran.interactionservice.workflow.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Confirm Workflow Request DTO
 *
 * Transaction data for workflow confirmations
 */
@Data
@Builder
public class ConfirmWorkflowRequest {
    private String workflowId;
    private String action;
    private String requestedBy;
}
