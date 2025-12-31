package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Workflow State Event - Published when workflow state changes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStateEvent {
    private String workflowId;
    private String workflowType;
    private String oldState;
    private String newState;
    private Map<String, Object> context;
    private long timestamp;
}
