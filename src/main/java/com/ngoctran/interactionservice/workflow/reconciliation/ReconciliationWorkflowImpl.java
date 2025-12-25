package com.ngoctran.interactionservice.workflow.reconciliation;

import com.ngoctran.interactionservice.workflow.activity.reconciliation.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payment Reconciliation Workflow Implementation
 *
 * Complete reconciliation process with 5 phases:
 * 1. Data Collection - Gather from multiple sources
 * 2. Transaction Matching - Match and compare transactions
 * 3. Discrepancy Analysis - Analyze and categorize exceptions
 * 4. Escalation & Resolution - Handle manual reviews
 * 5. Reporting & Audit - Generate reports and audit trails
 */
public class ReconciliationWorkflowImpl implements ReconciliationWorkflow {

    private static final Logger log = Workflow.getLogger(ReconciliationWorkflowImpl.class);

    // Workflow state
    private String currentStatus = "INITIALIZED";
    private ReconciliationProgress progress = new ReconciliationProgress("INITIALIZED", 0, 25);
    private ReconciliationStats stats = new ReconciliationStats();
    private Map<String, Object> config;

    // Activity stubs
    private final DataCollectionActivity dataCollectionActivity;
    private final MatchingActivity matchingActivity;
    private final DiscrepancyAnalysisActivity discrepancyAnalysisActivity;
    private final EscalationActivity escalationActivity;
    private final ReportingActivity reportingActivity;

    public ReconciliationWorkflowImpl() {
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(5))
                        .setMaximumInterval(Duration.ofSeconds(30))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();

        this.dataCollectionActivity = Workflow.newActivityStub(DataCollectionActivity.class, defaultOptions);
        this.matchingActivity = Workflow.newActivityStub(MatchingActivity.class, defaultOptions);
        this.discrepancyAnalysisActivity = Workflow.newActivityStub(DiscrepancyAnalysisActivity.class, defaultOptions);
        this.escalationActivity = Workflow.newActivityStub(EscalationActivity.class, defaultOptions);
        this.reportingActivity = Workflow.newActivityStub(ReportingActivity.class, defaultOptions);
    }

    @Override
    public ReconciliationResult reconcilePayments(String reconciliationId, String date, String type,
                                                Map<String, Object> config) {
        this.config = config;
        log.info("Starting Payment Reconciliation - ID: {}, Date: {}, Type: {}", reconciliationId, date, type);

        try {
            // ==================== PHASE 1: DATA COLLECTION ====================
            updateProgress("DATA_COLLECTION", 1, 25, "Collecting transaction data from sources");

            DataCollectionActivity.DataCollectionResult dataResult = collectTransactionData(reconciliationId, date, type);
            updateProgress("DATA_COLLECTION", 5, 25, "Data collection completed");

            // ==================== PHASE 2: TRANSACTION MATCHING ====================
            updateProgress("TRANSACTION_MATCHING", 6, 25, "Matching transactions across sources");

            MatchingActivity.MatchingResult matchingResult = performTransactionMatching(dataResult, config);
            updateProgress("TRANSACTION_MATCHING", 12, 25, "Transaction matching completed");

            // ==================== PHASE 3: DISCREPANCY ANALYSIS ====================
            updateProgress("DISCREPANCY_ANALYSIS", 13, 25, "Analyzing discrepancies and exceptions");

            DiscrepancyAnalysisActivity.DiscrepancyResult discrepancyResult =
                analyzeDiscrepancies(matchingResult, config);
            updateProgress("DISCREPANCY_ANALYSIS", 17, 25, "Discrepancy analysis completed");

            // ==================== PHASE 4: ESCALATION & RESOLUTION ====================
            updateProgress("ESCALATION_RESOLUTION", 18, 25, "Escalating issues and resolving discrepancies");

            EscalationActivity.EscalationResult escalationResult =
                handleEscalationAndResolution(discrepancyResult, reconciliationId);
            updateProgress("ESCALATION_RESOLUTION", 22, 25, "Escalation and resolution completed");

            // ==================== PHASE 5: REPORTING & AUDIT ====================
            updateProgress("REPORTING_AUDIT", 23, 25, "Generating reports and audit trails");

            ReportingActivity.ReportingResult reportingResult =
                generateReportsAndAudit(reconciliationId, date, type, escalationResult);
            updateProgress("REPORTING_AUDIT", 25, 25, "Reporting completed");

            // Create final result
            ReconciliationResult result = new ReconciliationResult(
                reconciliationId,
                "COMPLETED",
                generateSummary(escalationResult),
                escalationResult.getFinalStats()
            );

            result.setMetadata(Map.of(
                "reconciliationDate", date,
                "reconciliationType", type,
                "dataSources", dataResult.getDataSources(),
                "reportUrl", reportingResult.getReportUrl()
            ));

            log.info("Payment Reconciliation completed successfully - ID: {}", reconciliationId);
            return result;

        } catch (Exception e) {
            log.error("Payment Reconciliation failed - ID: {}", reconciliationId, e);

            // Create failure result
            ReconciliationResult failureResult = new ReconciliationResult(
                reconciliationId,
                "FAILED",
                "Reconciliation failed: " + e.getMessage(),
                stats
            );

            return failureResult;
        }
    }

    @Override
    public void manualResolution(String discrepancyId, Map<String, Object> resolution) {
        log.info("Received manual resolution for discrepancy: {}", discrepancyId);
        // Handle manual resolution - update internal state
        // In real implementation, this would update the discrepancy status
    }

    @Override
    public void additionalDataReceived(String sourceId, Map<String, Object> data) {
        log.info("Received additional data from source: {}", sourceId);
        // Handle additional data - could trigger re-matching
        // In real implementation, this would add to data collection
    }

    @Override
    public String getStatus() {
        return currentStatus;
    }

    @Override
    public ReconciliationProgress getProgress() {
        return progress;
    }

    @Override
    public ReconciliationStats getStats() {
        return stats;
    }

    // ==================== PRIVATE METHODS ====================

    private DataCollectionActivity.DataCollectionResult collectTransactionData(String reconciliationId, String date, String type) {
        log.info("Collecting transaction data for reconciliation: {}", reconciliationId);

        return dataCollectionActivity.collectTransactionData(
            reconciliationId,
            date,
            type,
            (List<String>) config.getOrDefault("dataSources", List.of("INTERNAL_DB", "BANK_STATEMENTS")),
            config
        );
    }

    private MatchingActivity.MatchingResult performTransactionMatching(
            DataCollectionActivity.DataCollectionResult dataResult, Map<String, Object> config) {

        log.info("Performing transaction matching for {} transactions",
                dataResult.getTotalTransactions());

        return matchingActivity.performTransactionMatching(
            dataResult.getTransactionData(),
            (Double) config.getOrDefault("matchingTolerance", 0.01), // 1% tolerance
            (List<String>) config.getOrDefault("matchingRules", List.of("EXACT", "AMOUNT_TIME", "REFERENCE")),
            config
        );
    }

    private DiscrepancyAnalysisActivity.DiscrepancyResult analyzeDiscrepancies(
            MatchingActivity.MatchingResult matchingResult, Map<String, Object> config) {

        log.info("Analyzing {} discrepancies",
                matchingResult.getDiscrepancies().size());

        return discrepancyAnalysisActivity.analyzeDiscrepancies(
            matchingResult.getDiscrepancies(),
            (Map<String, String>) config.getOrDefault("discrepancyCategories",
                Map.of("AMOUNT_MISMATCH", "HIGH", "MISSING_TRANSACTION", "CRITICAL")),
            config
        );
    }

    private EscalationActivity.EscalationResult handleEscalationAndResolution(
            DiscrepancyAnalysisActivity.DiscrepancyResult discrepancyResult, String reconciliationId) {

        log.info("Handling escalation and resolution for {} exceptions",
                discrepancyResult.getHighPriorityExceptions().size());

        return escalationActivity.handleEscalationAndResolution(
            discrepancyResult.getCategorizedDiscrepancies(),
            discrepancyResult.getHighPriorityExceptions(),
            reconciliationId,
            (Integer) config.getOrDefault("autoResolveThreshold", 1000), // Auto-resolve under 1k VND
            config
        );
    }

    private ReportingActivity.ReportingResult generateReportsAndAudit(
            String reconciliationId, String date, String type,
            EscalationActivity.EscalationResult escalationResult) {

        log.info("Generating reports and audit trail for reconciliation: {}", reconciliationId);

        return reportingActivity.generateReportsAndAudit(
            reconciliationId,
            date,
            type,
            escalationResult.getFinalStats(),
            escalationResult.getResolvedDiscrepancies(),
            escalationResult.getPendingManualReview(),
            config
        );
    }

    private String generateSummary(EscalationActivity.EscalationResult result) {
        ReconciliationStats finalStats = result.getFinalStats();
        StringBuilder summary = new StringBuilder();

        summary.append("Reconciliation Summary: ");
        summary.append(finalStats.getTotalTransactions()).append(" total transactions, ");
        summary.append(finalStats.getMatchedTransactions()).append(" matched (");
        summary.append(String.format("%.1f%%", finalStats.getMatchingRate() * 100)).append("), ");
        summary.append(finalStats.getExceptionTransactions()).append(" exceptions (");
        summary.append(String.format("%.1f%%", finalStats.getExceptionRate() * 100)).append("), ");
        summary.append(result.getPendingManualReview().size()).append(" pending manual review");

        return summary.toString();
    }

    private void updateProgress(String phase, int completed, int total, String operation) {
        this.currentStatus = phase;
        this.progress = new ReconciliationProgress(phase, completed, total);
        this.progress.setCurrentOperation(operation);
        log.info("Reconciliation Progress: {}/{} - {} - {}", completed, total, phase, operation);
    }
}
