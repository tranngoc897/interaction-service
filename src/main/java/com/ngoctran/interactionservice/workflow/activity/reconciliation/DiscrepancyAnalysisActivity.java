package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import io.temporal.activity.ActivityInterface;
import java.util.List;
import java.util.Map;

/**
 * Discrepancy Analysis Activity
 * Analyzes and categorizes transaction discrepancies
 */
@ActivityInterface
public interface DiscrepancyAnalysisActivity {

    /**
     * Analyze discrepancies and categorize them
     */
    DiscrepancyResult analyzeDiscrepancies(List<MatchingActivity.Discrepancy> discrepancies,
                                         Map<String, String> categoryMapping,
                                         Map<String, Object> config);

    /**
     * Analysis result
     */
    class DiscrepancyResult {
        private boolean success;
        private Map<String, List<MatchingActivity.Discrepancy>> categorizedDiscrepancies;
        private List<MatchingActivity.Discrepancy> highPriorityExceptions;
        private Map<String, Integer> categoryStats;
        private String analysisSummary;
        private long analyzedAt;

        public DiscrepancyResult() {}

        public DiscrepancyResult(boolean success, Map<String, List<MatchingActivity.Discrepancy>> categorizedDiscrepancies,
                               List<MatchingActivity.Discrepancy> highPriorityExceptions,
                               Map<String, Integer> categoryStats, String analysisSummary) {
            this.success = success;
            this.categorizedDiscrepancies = categorizedDiscrepancies;
            this.highPriorityExceptions = highPriorityExceptions;
            this.categoryStats = categoryStats;
            this.analysisSummary = analysisSummary;
            this.analyzedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Map<String, List<MatchingActivity.Discrepancy>> getCategorizedDiscrepancies() { return categorizedDiscrepancies; }
        public void setCategorizedDiscrepancies(Map<String, List<MatchingActivity.Discrepancy>> categorizedDiscrepancies) { this.categorizedDiscrepancies = categorizedDiscrepancies; }

        public List<MatchingActivity.Discrepancy> getHighPriorityExceptions() { return highPriorityExceptions; }
        public void setHighPriorityExceptions(List<MatchingActivity.Discrepancy> highPriorityExceptions) { this.highPriorityExceptions = highPriorityExceptions; }

        public Map<String, Integer> getCategoryStats() { return categoryStats; }
        public void setCategoryStats(Map<String, Integer> categoryStats) { this.categoryStats = categoryStats; }

        public String getAnalysisSummary() { return analysisSummary; }
        public void setAnalysisSummary(String analysisSummary) { this.analysisSummary = analysisSummary; }

        public long getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(long analyzedAt) { this.analyzedAt = analyzedAt; }
    }
}
