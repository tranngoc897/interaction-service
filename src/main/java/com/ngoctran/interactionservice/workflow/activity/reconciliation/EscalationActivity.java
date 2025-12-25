package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import com.ngoctran.interactionservice.workflow.reconciliation.ReconciliationWorkflow;
import io.temporal.activity.ActivityInterface;
import java.util.List;
import java.util.Map;

/**
 * Escalation Activity
 * Handles escalation of discrepancies and manual review assignments
 */
@ActivityInterface
public interface EscalationActivity {

    /**
     * Handle escalation and resolution of discrepancies
     */
    EscalationResult handleEscalationAndResolution(Map<String, List<MatchingActivity.Discrepancy>> categorizedDiscrepancies,
                                                 List<MatchingActivity.Discrepancy> highPriorityExceptions,
                                                 String reconciliationId, int autoResolveThreshold,
                                                 Map<String, Object> config);

    /**
     * Escalation result
     */
    class EscalationResult {
        private boolean success;
        private List<MatchingActivity.Discrepancy> resolvedDiscrepancies;
        private List<MatchingActivity.Discrepancy> pendingManualReview;
        private Map<String, String> escalationAssignments;
        private ReconciliationWorkflow.ReconciliationStats finalStats;
        private String escalationSummary;
        private long escalatedAt;

        public EscalationResult() {}

        public EscalationResult(boolean success, List<MatchingActivity.Discrepancy> resolvedDiscrepancies,
                              List<MatchingActivity.Discrepancy> pendingManualReview,
                              Map<String, String> escalationAssignments,
                              ReconciliationWorkflow.ReconciliationStats finalStats,
                              String escalationSummary) {
            this.success = success;
            this.resolvedDiscrepancies = resolvedDiscrepancies;
            this.pendingManualReview = pendingManualReview;
            this.escalationAssignments = escalationAssignments;
            this.finalStats = finalStats;
            this.escalationSummary = escalationSummary;
            this.escalatedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<MatchingActivity.Discrepancy> getResolvedDiscrepancies() { return resolvedDiscrepancies; }
        public void setResolvedDiscrepancies(List<MatchingActivity.Discrepancy> resolvedDiscrepancies) { this.resolvedDiscrepancies = resolvedDiscrepancies; }

        public List<MatchingActivity.Discrepancy> getPendingManualReview() { return pendingManualReview; }
        public void setPendingManualReview(List<MatchingActivity.Discrepancy> pendingManualReview) { this.pendingManualReview = pendingManualReview; }

        public Map<String, String> getEscalationAssignments() { return escalationAssignments; }
        public void setEscalationAssignments(Map<String, String> escalationAssignments) { this.escalationAssignments = escalationAssignments; }

        public ReconciliationWorkflow.ReconciliationStats getFinalStats() { return finalStats; }
        public void setFinalStats(ReconciliationWorkflow.ReconciliationStats finalStats) { this.finalStats = finalStats; }

        public String getEscalationSummary() { return escalationSummary; }
        public void setEscalationSummary(String escalationSummary) { this.escalationSummary = escalationSummary; }

        public long getEscalatedAt() { return escalatedAt; }
        public void setEscalatedAt(long escalatedAt) { this.escalatedAt = escalatedAt; }
    }
}
