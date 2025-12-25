package com.ngoctran.interactionservice.workflow.reconciliation;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.QueryMethod;

import java.time.LocalDate;
import java.util.Map;

/**
 * Payment Reconciliation Workflow Interface
 *
 * Handles end-to-end payment reconciliation process:
 * 1. Data collection from multiple sources
 * 2. Transaction matching and comparison
 * 3. Discrepancy analysis and categorization
 * 4. Escalation and manual review
 * 5. Reporting and audit trail
 *
 * Workflow ID format: "reconciliation-{date}-{type}"
 * Task Queue: RECONCILIATION_QUEUE
 */
@WorkflowInterface
public interface ReconciliationWorkflow {

    /**
     * Main reconciliation method
     *
     * @param reconciliationId Unique ID for this reconciliation run
     * @param date Date to reconcile (YYYY-MM-DD)
     * @param type Type of reconciliation (INTERNAL, EXTERNAL, EOD, etc.)
     * @param config Reconciliation configuration
     * @return Reconciliation result with summary and status
     */
    @WorkflowMethod
    ReconciliationResult reconcilePayments(String reconciliationId, String date, String type,
                                         Map<String, Object> config);

    /**
     * Signal: Manual resolution provided
     * Called when manual reviewer resolves a discrepancy
     *
     * @param discrepancyId ID of the resolved discrepancy
     * @param resolution Resolution details
     */
    @SignalMethod
    void manualResolution(String discrepancyId, Map<String, Object> resolution);

    /**
     * Signal: Add additional data source
     * Called when new data source becomes available during reconciliation
     *
     * @param sourceId Source identifier
     * @param data Additional transaction data
     */
    @SignalMethod
    void additionalDataReceived(String sourceId, Map<String, Object> data);

    /**
     * Query: Get current reconciliation status
     *
     * @return Current status of reconciliation
     */
    @QueryMethod
    String getStatus();

    /**
     * Query: Get reconciliation progress
     *
     * @return Progress information
     */
    @QueryMethod
    ReconciliationProgress getProgress();

    /**
     * Query: Get reconciliation statistics
     *
     * @return Current statistics
     */
    @QueryMethod
    ReconciliationStats getStats();

    // ==================== DTOs ====================

    /**
     * Reconciliation Result DTO
     */
    class ReconciliationResult {
        private String reconciliationId;
        private String status; // COMPLETED, FAILED, PARTIAL
        private String summary;
        private ReconciliationStats stats;
        private Map<String, Object> metadata;
        private long completedAt;

        public ReconciliationResult() {}

        public ReconciliationResult(String reconciliationId, String status,
                                  String summary, ReconciliationStats stats) {
            this.reconciliationId = reconciliationId;
            this.status = status;
            this.summary = summary;
            this.stats = stats;
            this.completedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public String getReconciliationId() { return reconciliationId; }
        public void setReconciliationId(String reconciliationId) { this.reconciliationId = reconciliationId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public ReconciliationStats getStats() { return stats; }
        public void setStats(ReconciliationStats stats) { this.stats = stats; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    }

    /**
     * Reconciliation Progress DTO
     */
    class ReconciliationProgress {
        private String currentPhase;
        private int completedSteps;
        private int totalSteps;
        private int percentComplete;
        private String currentOperation;
        private long lastUpdated;

        public ReconciliationProgress() {}

        public ReconciliationProgress(String currentPhase, int completedSteps, int totalSteps) {
            this.currentPhase = currentPhase;
            this.completedSteps = completedSteps;
            this.totalSteps = totalSteps;
            this.percentComplete = (int) ((completedSteps * 100.0) / totalSteps);
            this.lastUpdated = System.currentTimeMillis();
        }

        // Getters and setters
        public String getCurrentPhase() { return currentPhase; }
        public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

        public int getCompletedSteps() { return completedSteps; }
        public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }

        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

        public int getPercentComplete() { return percentComplete; }
        public void setPercentComplete(int percentComplete) { this.percentComplete = percentComplete; }

        public String getCurrentOperation() { return currentOperation; }
        public void setCurrentOperation(String currentOperation) { this.currentOperation = currentOperation; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    /**
     * Reconciliation Statistics DTO
     */
    class ReconciliationStats {
        private int totalTransactions;
        private int matchedTransactions;
        private int unmatchedTransactions;
        private int exceptionTransactions;
        private int resolvedExceptions;
        private int pendingManualReview;
        private double matchingRate;
        private double exceptionRate;
        private Map<String, Integer> discrepanciesByType;

        public ReconciliationStats() {}

        public ReconciliationStats(int totalTransactions, int matchedTransactions,
                                 int unmatchedTransactions, int exceptionTransactions) {
            this.totalTransactions = totalTransactions;
            this.matchedTransactions = matchedTransactions;
            this.unmatchedTransactions = unmatchedTransactions;
            this.exceptionTransactions = exceptionTransactions;
            this.matchingRate = totalTransactions > 0 ? (double) matchedTransactions / totalTransactions : 0.0;
            this.exceptionRate = totalTransactions > 0 ? (double) exceptionTransactions / totalTransactions : 0.0;
        }

        // Getters and setters
        public int getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }

        public int getMatchedTransactions() { return matchedTransactions; }
        public void setMatchedTransactions(int matchedTransactions) { this.matchedTransactions = matchedTransactions; }

        public int getUnmatchedTransactions() { return unmatchedTransactions; }
        public void setUnmatchedTransactions(int unmatchedTransactions) { this.unmatchedTransactions = unmatchedTransactions; }

        public int getExceptionTransactions() { return exceptionTransactions; }
        public void setExceptionTransactions(int exceptionTransactions) { this.exceptionTransactions = exceptionTransactions; }

        public int getResolvedExceptions() { return resolvedExceptions; }
        public void setResolvedExceptions(int resolvedExceptions) { this.resolvedExceptions = resolvedExceptions; }

        public int getPendingManualReview() { return pendingManualReview; }
        public void setPendingManualReview(int pendingManualReview) { this.pendingManualReview = pendingManualReview; }

        public double getMatchingRate() { return matchingRate; }
        public void setMatchingRate(double matchingRate) { this.matchingRate = matchingRate; }

        public double getExceptionRate() { return exceptionRate; }
        public void setExceptionRate(double exceptionRate) { this.exceptionRate = exceptionRate; }

        public Map<String, Integer> getDiscrepanciesByType() { return discrepanciesByType; }
        public void setDiscrepanciesByType(Map<String, Integer> discrepanciesByType) { this.discrepanciesByType = discrepanciesByType; }
    }
}
