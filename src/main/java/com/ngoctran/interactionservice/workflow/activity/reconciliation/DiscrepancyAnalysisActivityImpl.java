package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Discrepancy Analysis Activity Implementation
 * Analyzes and categorizes transaction discrepancies
 */
@Component
@Slf4j
public class DiscrepancyAnalysisActivityImpl implements DiscrepancyAnalysisActivity {

    @Override
    public DiscrepancyResult analyzeDiscrepancies(List<MatchingActivity.Discrepancy> discrepancies,
                                                Map<String, String> categoryMapping,
                                                Map<String, Object> config) {
        log.info("Analyzing {} discrepancies", discrepancies.size());

        try {
            // Categorize discrepancies
            Map<String, List<MatchingActivity.Discrepancy>> categorized = new HashMap<>();
            List<MatchingActivity.Discrepancy> highPriority = new ArrayList<>();
            Map<String, Integer> categoryStats = new HashMap<>();

            for (MatchingActivity.Discrepancy discrepancy : discrepancies) {
                String category = categorizeDiscrepancy(discrepancy, categoryMapping);
                categorized.computeIfAbsent(category, k -> new ArrayList<>()).add(discrepancy);

                categoryStats.put(category, categoryStats.getOrDefault(category, 0) + 1);

                // Identify high priority exceptions
                if (isHighPriority(discrepancy)) {
                    highPriority.add(discrepancy);
                }

                // Perform root cause analysis
                analyzeRootCause(discrepancy);
            }

            // Generate analysis summary
            String summary = generateAnalysisSummary(categorized, highPriority, categoryStats);

            // Simulate processing time
            Thread.sleep(1500 + (int)(Math.random() * 2000));

            log.info("Analysis completed: {} categories, {} high priority exceptions",
                    categorized.size(), highPriority.size());

            return new DiscrepancyResult(true, categorized, highPriority, categoryStats, summary);

        } catch (Exception e) {
            log.error("Discrepancy analysis failed", e);
            return new DiscrepancyResult(false, new HashMap<>(), new ArrayList<>(),
                    new HashMap<>(), "Analysis failed: " + e.getMessage());
        }
    }

    private String categorizeDiscrepancy(MatchingActivity.Discrepancy discrepancy,
                                       Map<String, String> categoryMapping) {
        // Check explicit mapping first
        String mappedCategory = categoryMapping.get(discrepancy.getDiscrepancyType());
        if (mappedCategory != null) {
            return mappedCategory;
        }

        // Auto-categorize based on rules
        switch (discrepancy.getDiscrepancyType()) {
            case "AMOUNT_MISMATCH":
                double diff = Math.abs(discrepancy.getExpectedAmount() - discrepancy.getActualAmount());
                if (diff > 1000000) { // > 1M VND difference
                    return "HIGH_VALUE_DISCREPANCY";
                } else {
                    return "AMOUNT_DISCREPANCY";
                }

            case "MISSING_TRANSACTION":
                if (discrepancy.getExpectedAmount() > 50000000) { // > 50M VND
                    return "CRITICAL_MISSING";
                } else {
                    return "MISSING_TRANSACTION";
                }

            case "EXTRA_TRANSACTION":
                return "UNEXPECTED_TRANSACTION";

            case "FAILED_TRANSACTION":
                return "FAILED_TRANSACTION";

            case "HIGH_VALUE_MISSING":
                return "CRITICAL_MISSING";

            default:
                return "OTHER_DISCREPANCY";
        }
    }

    private boolean isHighPriority(MatchingActivity.Discrepancy discrepancy) {
        // High priority criteria
        if (discrepancy.getSeverity().equals("CRITICAL")) {
            return true;
        }

        if (discrepancy.getExpectedAmount() > 10000000) { // > 10M VND
            return true;
        }

        if (discrepancy.getDiscrepancyType().equals("MISSING_TRANSACTION") &&
            discrepancy.getExpectedAmount() > 5000000) { // > 5M VND missing
            return true;
        }

        return false;
    }

    private void analyzeRootCause(MatchingActivity.Discrepancy discrepancy) {
        // Simulate root cause analysis
        // In real implementation, this would analyze patterns and determine likely causes

        Map<String, Object> details = new HashMap<>();
        details.put("analyzedAt", System.currentTimeMillis());
        details.put("analysisMethod", "PATTERN_ANALYSIS");

        // Determine likely root causes
        List<String> possibleCauses = new ArrayList<>();

        switch (discrepancy.getDiscrepancyType()) {
            case "AMOUNT_MISMATCH":
                possibleCauses.add("Rounding difference");
                possibleCauses.add("Fee deduction");
                possibleCauses.add("Currency conversion");
                break;

            case "MISSING_TRANSACTION":
                possibleCauses.add("System outage");
                possibleCauses.add("Processing delay");
                possibleCauses.add("Duplicate processing");
                break;

            case "EXTRA_TRANSACTION":
                possibleCauses.add("Test transaction");
                possibleCauses.add("Duplicate submission");
                possibleCauses.add("System error");
                break;
        }

        details.put("possibleRootCauses", possibleCauses);
        details.put("recommendedAction", determineRecommendedAction(discrepancy));
        details.put("impactAssessment", assessImpact(discrepancy));

        discrepancy.setDetails(details);
    }

    private String determineRecommendedAction(MatchingActivity.Discrepancy discrepancy) {
        switch (discrepancy.getSeverity()) {
            case "CRITICAL":
                return "IMMEDIATE_MANUAL_REVIEW";
            case "HIGH":
                return "URGENT_MANUAL_REVIEW";
            case "MEDIUM":
                return "SCHEDULED_REVIEW";
            case "LOW":
                return "AUTO_RESOLVE_IF_UNDER_THRESHOLD";
            default:
                return "REVIEW_REQUIRED";
        }
    }

    private String assessImpact(MatchingActivity.Discrepancy discrepancy) {
        double amount = Math.max(discrepancy.getExpectedAmount(), discrepancy.getActualAmount());

        if (amount > 100000000) { // > 100M VND
            return "HIGH_FINANCIAL_IMPACT";
        } else if (amount > 10000000) { // > 10M VND
            return "MEDIUM_FINANCIAL_IMPACT";
        } else if (amount > 1000000) { // > 1M VND
            return "LOW_FINANCIAL_IMPACT";
        } else {
            return "MINIMAL_IMPACT";
        }
    }

    private String generateAnalysisSummary(Map<String, List<MatchingActivity.Discrepancy>> categorized,
                                         List<MatchingActivity.Discrepancy> highPriority,
                                         Map<String, Integer> categoryStats) {
        StringBuilder summary = new StringBuilder();
        summary.append("Discrepancy Analysis Summary:\n");
        summary.append("- Total discrepancies: ").append(
            categorized.values().stream().mapToInt(List::size).sum()).append("\n");
        summary.append("- Categories: ").append(categorized.size()).append("\n");
        summary.append("- High priority exceptions: ").append(highPriority.size()).append("\n");

        // Add category breakdown
        summary.append("- Category breakdown:\n");
        categoryStats.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> summary.append("  * ").append(entry.getKey())
                .append(": ").append(entry.getValue()).append("\n"));

        // Add severity analysis
        Map<String, Integer> severityStats = new HashMap<>();
        for (MatchingActivity.Discrepancy disc : highPriority) {
            severityStats.put(disc.getSeverity(),
                severityStats.getOrDefault(disc.getSeverity(), 0) + 1);
        }

        summary.append("- Severity distribution:\n");
        severityStats.forEach((severity, count) ->
            summary.append("  * ").append(severity).append(": ").append(count).append("\n"));

        return summary.toString();
    }
}
