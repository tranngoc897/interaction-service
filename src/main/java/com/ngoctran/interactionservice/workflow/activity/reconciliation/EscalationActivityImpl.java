package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import com.ngoctran.interactionservice.workflow.reconciliation.ReconciliationWorkflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Escalation Activity Implementation
 * Handles escalation of discrepancies and manual review assignments
 */
@Component
@Slf4j
public class EscalationActivityImpl implements EscalationActivity {

    @Override
    public EscalationResult handleEscalationAndResolution(Map<String, List<MatchingActivity.Discrepancy>> categorizedDiscrepancies,
                                                        List<MatchingActivity.Discrepancy> highPriorityExceptions,
                                                        String reconciliationId, int autoResolveThreshold,
                                                        Map<String, Object> config) {
        log.info("Handling escalation and resolution for {} discrepancies",
                categorizedDiscrepancies.values().stream().mapToInt(List::size).sum());

        try {
            List<MatchingActivity.Discrepancy> resolvedDiscrepancies = new ArrayList<>();
            List<MatchingActivity.Discrepancy> pendingManualReview = new ArrayList<>();
            Map<String, String> escalationAssignments = new HashMap<>();

            // Process auto-resolution for low-value discrepancies
            for (Map.Entry<String, List<MatchingActivity.Discrepancy>> entry : categorizedDiscrepancies.entrySet()) {
                String category = entry.getKey();
                List<MatchingActivity.Discrepancy> discrepancies = entry.getValue();

                for (MatchingActivity.Discrepancy discrepancy : discrepancies) {
                    if (canAutoResolve(discrepancy, autoResolveThreshold)) {
                        // Auto-resolve
                        resolveDiscrepancy(discrepancy, "AUTO_RESOLVED");
                        resolvedDiscrepancies.add(discrepancy);
                        log.info("Auto-resolved discrepancy: {}", discrepancy.getDiscrepancyId());
                    } else {
                        // Requires manual review
                        pendingManualReview.add(discrepancy);
                        assignForManualReview(discrepancy, escalationAssignments);
                    }
                }
            }

            // Process high priority exceptions (always manual)
            for (MatchingActivity.Discrepancy exception : highPriorityExceptions) {
                if (!pendingManualReview.contains(exception)) {
                    pendingManualReview.add(exception);
                    assignForManualReview(exception, escalationAssignments);
                }
            }

            // Create final reconciliation stats
            ReconciliationWorkflow.ReconciliationStats finalStats = calculateFinalStats(
                categorizedDiscrepancies, resolvedDiscrepancies, pendingManualReview);

            // Generate escalation summary
            String summary = generateEscalationSummary(resolvedDiscrepancies, pendingManualReview,
                    escalationAssignments);

            // Simulate processing time
            Thread.sleep(1000 + (int)(Math.random() * 2000));

            log.info("Escalation completed: {} resolved, {} pending manual review",
                    resolvedDiscrepancies.size(), pendingManualReview.size());

            return new EscalationResult(true, resolvedDiscrepancies, pendingManualReview,
                    escalationAssignments, finalStats, summary);

        } catch (Exception e) {
            log.error("Escalation and resolution failed", e);
            return new EscalationResult(false, new ArrayList<>(), new ArrayList<>(),
                    new HashMap<>(), new ReconciliationWorkflow.ReconciliationStats(),
                    "Escalation failed: " + e.getMessage());
        }
    }

    private boolean canAutoResolve(MatchingActivity.Discrepancy discrepancy, int autoResolveThreshold) {
        // Auto-resolve criteria
        if (discrepancy.getSeverity().equals("CRITICAL") ||
            discrepancy.getSeverity().equals("HIGH")) {
            return false; // Never auto-resolve high severity
        }

        if (discrepancy.getExpectedAmount() > autoResolveThreshold) {
            return false; // Above auto-resolve threshold
        }

        // Auto-resolve specific types of low-risk discrepancies
        switch (discrepancy.getDiscrepancyType()) {
            case "AMOUNT_MISMATCH":
                // Small amount differences (rounding, fees)
                double diff = Math.abs(discrepancy.getExpectedAmount() - discrepancy.getActualAmount());
                return diff < 10000; // < 10k VND difference

            case "EXTRA_TRANSACTION":
                // Small extra transactions (likely test data)
                return discrepancy.getExpectedAmount() < 50000; // < 50k VND

            default:
                return false; // Don't auto-resolve other types
        }
    }

    private void resolveDiscrepancy(MatchingActivity.Discrepancy discrepancy, String resolution) {
        // Simulate resolution process
        // In real implementation, this would update databases, send notifications, etc.

        Map<String, Object> resolutionDetails = new HashMap<>();
        resolutionDetails.put("resolution", resolution);
        resolutionDetails.put("resolvedAt", System.currentTimeMillis());
        resolutionDetails.put("resolvedBy", "SYSTEM_AUTO_RESOLVE");
        resolutionDetails.put("notes", "Automatically resolved based on predefined rules");

        if (discrepancy.getDetails() == null) {
            discrepancy.setDetails(new HashMap<>());
        }
        discrepancy.getDetails().put("resolution", resolutionDetails);
    }

    private void assignForManualReview(MatchingActivity.Discrepancy discrepancy,
                                     Map<String, String> escalationAssignments) {
        // Assign based on severity and amount
        String assignee;

        if (discrepancy.getSeverity().equals("CRITICAL")) {
            assignee = "SENIOR_RECONCILIATION_OFFICER";
        } else if (discrepancy.getExpectedAmount() > 10000000) { // > 10M VND
            assignee = "RECONCILIATION_SUPERVISOR";
        } else if (discrepancy.getExpectedAmount() > 1000000) { // > 1M VND
            assignee = "RECONCILIATION_OFFICER";
        } else {
            assignee = "JUNIOR_RECONCILIATION_CLERK";
        }

        escalationAssignments.put(discrepancy.getDiscrepancyId(), assignee);

        // Add assignment details to discrepancy
        Map<String, Object> assignmentDetails = new HashMap<>();
        assignmentDetails.put("assignedTo", assignee);
        assignmentDetails.put("assignedAt", System.currentTimeMillis());
        assignmentDetails.put("priority", calculatePriority(discrepancy));
        assignmentDetails.put("slaHours", calculateSLAHours(discrepancy));

        if (discrepancy.getDetails() == null) {
            discrepancy.setDetails(new HashMap<>());
        }
        discrepancy.getDetails().put("assignment", assignmentDetails);
    }

    private String calculatePriority(MatchingActivity.Discrepancy discrepancy) {
        switch (discrepancy.getSeverity()) {
            case "CRITICAL": return "URGENT";
            case "HIGH": return "HIGH";
            case "MEDIUM": return "MEDIUM";
            case "LOW": return "LOW";
            default: return "NORMAL";
        }
    }

    private int calculateSLAHours(MatchingActivity.Discrepancy discrepancy) {
        switch (discrepancy.getSeverity()) {
            case "CRITICAL": return 4;  // 4 hours
            case "HIGH": return 24;     // 24 hours
            case "MEDIUM": return 72;   // 3 days
            case "LOW": return 168;     // 1 week
            default: return 24;
        }
    }

    private ReconciliationWorkflow.ReconciliationStats calculateFinalStats(
            Map<String, List<MatchingActivity.Discrepancy>> categorizedDiscrepancies,
            List<MatchingActivity.Discrepancy> resolvedDiscrepancies,
            List<MatchingActivity.Discrepancy> pendingManualReview) {

        int totalDiscrepancies = categorizedDiscrepancies.values()
                .stream().mapToInt(List::size).sum();

        ReconciliationWorkflow.ReconciliationStats stats = new ReconciliationWorkflow.ReconciliationStats();
        stats.setTotalTransactions(2300); // Mock total transactions (would be passed in)
        stats.setMatchedTransactions(2100); // Mock matched transactions
        stats.setUnmatchedTransactions(totalDiscrepancies);
        stats.setExceptionTransactions(totalDiscrepancies);
        stats.setResolvedExceptions(resolvedDiscrepancies.size());
        stats.setPendingManualReview(pendingManualReview.size());

        return stats;
    }

    private String generateEscalationSummary(List<MatchingActivity.Discrepancy> resolvedDiscrepancies,
                                           List<MatchingActivity.Discrepancy> pendingManualReview,
                                           Map<String, String> escalationAssignments) {
        StringBuilder summary = new StringBuilder();
        summary.append("Escalation & Resolution Summary:\n");
        summary.append("- Auto-resolved discrepancies: ").append(resolvedDiscrepancies.size()).append("\n");
        summary.append("- Pending manual review: ").append(pendingManualReview.size()).append("\n");
        summary.append("- Escalation assignments: ").append(escalationAssignments.size()).append("\n");

        // Add assignment breakdown
        Map<String, Integer> assignmentStats = new HashMap<>();
        escalationAssignments.values().forEach(role ->
            assignmentStats.put(role, assignmentStats.getOrDefault(role, 0) + 1));

        summary.append("- Assignment breakdown:\n");
        assignmentStats.forEach((role, count) ->
            summary.append("  * ").append(role).append(": ").append(count).append("\n"));

        // Calculate resolution rate
        int totalDiscrepancies = resolvedDiscrepancies.size() + pendingManualReview.size();
        if (totalDiscrepancies > 0) {
            double autoResolutionRate = (double) resolvedDiscrepancies.size() / totalDiscrepancies * 100;
            summary.append("- Auto-resolution rate: ").append(String.format("%.1f%%", autoResolutionRate)).append("\n");
        }

        return summary.toString();
    }
}
