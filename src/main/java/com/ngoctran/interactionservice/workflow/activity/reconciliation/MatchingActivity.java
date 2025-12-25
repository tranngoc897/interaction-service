package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import io.temporal.activity.ActivityInterface;
import java.util.List;
import java.util.Map;

/**
 * Transaction Matching Activity
 * Matches transactions across different data sources
 */
@ActivityInterface
public interface MatchingActivity {

    /**
     * Perform transaction matching across all data sources
     */
    MatchingResult performTransactionMatching(Map<String, List<DataCollectionActivity.TransactionData>> transactionData,
                                            double matchingTolerance, List<String> matchingRules,
                                            Map<String, Object> config);

    /**
     * Matching result
     */
    class MatchingResult {
        private boolean success;
        private List<MatchedTransaction> matchedTransactions;
        private List<Discrepancy> discrepancies;
        private Map<String, Integer> matchStats;
        private long matchedAt;

        public MatchingResult() {}

        public MatchingResult(boolean success, List<MatchedTransaction> matchedTransactions,
                            List<Discrepancy> discrepancies, Map<String, Integer> matchStats) {
            this.success = success;
            this.matchedTransactions = matchedTransactions;
            this.discrepancies = discrepancies;
            this.matchStats = matchStats;
            this.matchedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<MatchedTransaction> getMatchedTransactions() { return matchedTransactions; }
        public void setMatchedTransactions(List<MatchedTransaction> matchedTransactions) { this.matchedTransactions = matchedTransactions; }

        public List<Discrepancy> getDiscrepancies() { return discrepancies; }
        public void setDiscrepancies(List<Discrepancy> discrepancies) { this.discrepancies = discrepancies; }

        public Map<String, Integer> getMatchStats() { return matchStats; }
        public void setMatchStats(Map<String, Integer> matchStats) { this.matchStats = matchStats; }

        public long getMatchedAt() { return matchedAt; }
        public void setMatchedAt(long matchedAt) { this.matchedAt = matchedAt; }
    }

    /**
     * Matched transaction
     */
    class MatchedTransaction {
        private String transactionId;
        private List<String> sourceTransactions;
        private double amount;
        private String matchType; // EXACT, FUZZY, REFERENCE
        private double confidence;
        private long matchedAt;

        public MatchedTransaction() {}

        public MatchedTransaction(String transactionId, List<String> sourceTransactions,
                                double amount, String matchType, double confidence) {
            this.transactionId = transactionId;
            this.sourceTransactions = sourceTransactions;
            this.amount = amount;
            this.matchType = matchType;
            this.confidence = confidence;
            this.matchedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public List<String> getSourceTransactions() { return sourceTransactions; }
        public void setSourceTransactions(List<String> sourceTransactions) { this.sourceTransactions = sourceTransactions; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getMatchType() { return matchType; }
        public void setMatchType(String matchType) { this.matchType = matchType; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public long getMatchedAt() { return matchedAt; }
        public void setMatchedAt(long matchedAt) { this.matchedAt = matchedAt; }
    }

    /**
     * Transaction discrepancy
     */
    class Discrepancy {
        private String discrepancyId;
        private String transactionId;
        private String sourceSystem;
        private String discrepancyType; // AMOUNT_MISMATCH, MISSING_TRANSACTION, EXTRA_TRANSACTION
        private double expectedAmount;
        private double actualAmount;
        private String expectedDate;
        private String actualDate;
        private String description;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private Map<String, Object> details;

        public Discrepancy() {}

        public Discrepancy(String discrepancyId, String transactionId, String sourceSystem,
                         String discrepancyType, double expectedAmount, double actualAmount,
                         String expectedDate, String actualDate, String description, String severity) {
            this.discrepancyId = discrepancyId;
            this.transactionId = transactionId;
            this.sourceSystem = sourceSystem;
            this.discrepancyType = discrepancyType;
            this.expectedAmount = expectedAmount;
            this.actualAmount = actualAmount;
            this.expectedDate = expectedDate;
            this.actualDate = actualDate;
            this.description = description;
            this.severity = severity;
        }

        // Getters and setters
        public String getDiscrepancyId() { return discrepancyId; }
        public void setDiscrepancyId(String discrepancyId) { this.discrepancyId = discrepancyId; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

        public String getDiscrepancyType() { return discrepancyType; }
        public void setDiscrepancyType(String discrepancyType) { this.discrepancyType = discrepancyType; }

        public double getExpectedAmount() { return expectedAmount; }
        public void setExpectedAmount(double expectedAmount) { this.expectedAmount = expectedAmount; }

        public double getActualAmount() { return actualAmount; }
        public void setActualAmount(double actualAmount) { this.actualAmount = actualAmount; }

        public String getExpectedDate() { return expectedDate; }
        public void setExpectedDate(String expectedDate) { this.expectedDate = expectedDate; }

        public String getActualDate() { return actualDate; }
        public void setActualDate(String actualDate) { this.actualDate = actualDate; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}
