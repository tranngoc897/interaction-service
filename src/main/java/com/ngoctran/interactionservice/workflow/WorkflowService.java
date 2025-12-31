package com.ngoctran.interactionservice.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Workflow Service - Handles workflow operations
 * This is a placeholder implementation for ABB onboarding integration
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    /**
     * Cancel a workflow by its ID
     */
    public void cancelWorkflow(String workflowId) {
        log.info("Cancelling workflow: {}", workflowId);
        // TODO: Implement workflow cancellation logic
    }

    /**
     * Signal user data update to workflow
     */
    public void signalUserDataUpdated(String workflowId, Map<String, Object> data) {
        log.info("Signaling user data update for workflow: {} with data: {}", workflowId, data);
        // TODO: Implement workflow signaling logic
    }

    /**
     * Signal manual review decision
     */
    public void signalManualReview(String workflowId, boolean approved, String reason) {
        log.info("Signaling manual review for workflow: {} - approved: {}, reason: {}", workflowId, approved, reason);
        // TODO: Implement manual review signaling logic
    }

    /**
     * Query workflow progress
     */
    public WorkflowProgress queryWorkflowProgress(String workflowId) {
        log.info("Querying workflow progress for: {}", workflowId);
        // TODO: Implement workflow progress querying
        return new WorkflowProgress("COMPLETED", 100);
    }

    /**
     * Workflow progress data class
     */
    public static class WorkflowProgress {
        private String currentStep;
        private int percentComplete;

        public WorkflowProgress(String currentStep, int percentComplete) {
            this.currentStep = currentStep;
            this.percentComplete = percentComplete;
        }

        public String getCurrentStep() {
            return currentStep;
        }

        public int getPercentComplete() {
            return percentComplete;
        }
    }
}
