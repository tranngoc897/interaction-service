package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import com.ngoctran.interactionservice.workflow.reconciliation.ReconciliationWorkflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Reporting Activity Implementation
 * Generates reconciliation reports and audit trails
 */
@Component
@Slf4j
public class ReportingActivityImpl implements ReportingActivity {

    @Override
    public ReportingResult generateReportsAndAudit(String reconciliationId, String date, String type,
                                                 ReconciliationWorkflow.ReconciliationStats stats,
                                                 List<MatchingActivity.Discrepancy> resolvedDiscrepancies,
                                                 List<MatchingActivity.Discrepancy> pendingManualReview,
                                                 Map<String, Object> config) {
        log.info("Generating reports and audit trail for reconciliation: {}", reconciliationId);

        try {
            // Generate reconciliation report
            String reportUrl = generateReconciliationReport(reconciliationId, date, type, stats,
                    resolvedDiscrepancies, pendingManualReview);

            // Generate audit trail
            String auditTrailUrl = generateAuditTrail(reconciliationId, date, type);

            // Create report metadata
            Map<String, Object> reportMetadata = new HashMap<>();
            reportMetadata.put("reconciliationId", reconciliationId);
            reportMetadata.put("reportDate", date);
            reportMetadata.put("reportType", type);
            reportMetadata.put("generatedAt", System.currentTimeMillis());
            reportMetadata.put("totalTransactions", stats.getTotalTransactions());
            reportMetadata.put("matchingRate", stats.getMatchingRate());
            reportMetadata.put("exceptionRate", stats.getExceptionRate());

            // Generate report files list
            List<String> generatedReports = Arrays.asList(
                "reconciliation-summary-" + reconciliationId + ".pdf",
                "discrepancy-details-" + reconciliationId + ".xlsx",
                "audit-trail-" + reconciliationId + ".json",
                "compliance-report-" + reconciliationId + ".pdf"
            );

            // Generate summary
            String summary = generateReportingSummary(stats, resolvedDiscrepancies, pendingManualReview);

            // Simulate report generation time
            Thread.sleep(2000 + (int)(Math.random() * 3000));

            log.info("Reports generated successfully for reconciliation: {}", reconciliationId);

            return new ReportingResult(true, reportUrl, auditTrailUrl, reportMetadata,
                    generatedReports, summary);

        } catch (Exception e) {
            log.error("Report generation failed for reconciliation: {}", reconciliationId, e);
            return new ReportingResult(false, null, null, new HashMap<>(), new ArrayList<>(),
                    "Report generation failed: " + e.getMessage());
        }
    }

    private String generateReconciliationReport(String reconciliationId, String date, String type,
                                              ReconciliationWorkflow.ReconciliationStats stats,
                                              List<MatchingActivity.Discrepancy> resolvedDiscrepancies,
                                              List<MatchingActivity.Discrepancy> pendingManualReview) {
        // Simulate report generation
        // In real implementation, this would generate PDF/Excel reports

        String reportFileName = "reconciliation-report-" + reconciliationId + "-" + date + ".pdf";
        String reportUrl = "/reports/reconciliation/" + reportFileName;

        log.info("Generated reconciliation report: {}", reportFileName);

        // Simulate report content creation
        Map<String, Object> reportContent = new HashMap<>();
        reportContent.put("reconciliationId", reconciliationId);
        reportContent.put("date", date);
        reportContent.put("type", type);
        reportContent.put("stats", stats);
        reportContent.put("resolvedCount", resolvedDiscrepancies.size());
        reportContent.put("pendingCount", pendingManualReview.size());

        // In real implementation, this would be saved to file storage
        // FileStorageService.saveReport(reportFileName, reportContent);

        return reportUrl;
    }

    private String generateAuditTrail(String reconciliationId, String date, String type) {
        // Simulate audit trail generation
        // In real implementation, this would create comprehensive audit logs

        String auditFileName = "audit-trail-" + reconciliationId + "-" + date + ".json";
        String auditUrl = "/audit/reconciliation/" + auditFileName;

        log.info("Generated audit trail: {}", auditFileName);

        // Simulate audit trail content
        Map<String, Object> auditContent = new HashMap<>();
        auditContent.put("reconciliationId", reconciliationId);
        auditContent.put("date", date);
        auditContent.put("type", type);
        auditContent.put("timestamp", System.currentTimeMillis());
        auditContent.put("user", "SYSTEM");
        auditContent.put("actions", Arrays.asList(
            "DATA_COLLECTION_STARTED",
            "TRANSACTION_MATCHING_COMPLETED",
            "DISCREPANCY_ANALYSIS_FINISHED",
            "ESCALATION_COMPLETED",
            "REPORT_GENERATION_FINISHED"
        ));

        // In real implementation, this would be saved securely
        // AuditService.saveAuditTrail(auditFileName, auditContent);

        return auditUrl;
    }

    private String generateReportingSummary(ReconciliationWorkflow.ReconciliationStats stats,
                                          List<MatchingActivity.Discrepancy> resolvedDiscrepancies,
                                          List<MatchingActivity.Discrepancy> pendingManualReview) {
        StringBuilder summary = new StringBuilder();
        summary.append("Reconciliation Reporting Summary:\n\n");

        // Overall statistics
        summary.append("📊 OVERALL STATISTICS:\n");
        summary.append(String.format("- Total Transactions: %,d\n", stats.getTotalTransactions()));
        summary.append(String.format("- Matched Transactions: %,d (%.1f%%)\n",
                stats.getMatchedTransactions(), stats.getMatchingRate() * 100));
        summary.append(String.format("- Exception Transactions: %,d (%.1f%%)\n",
                stats.getExceptionTransactions(), stats.getExceptionRate() * 100));

        // Resolution statistics
        summary.append("\n🔧 RESOLUTION STATISTICS:\n");
        summary.append(String.format("- Auto-resolved: %,d\n", stats.getResolvedExceptions()));
        summary.append(String.format("- Pending Manual Review: %,d\n", stats.getPendingManualReview()));

        if (stats.getExceptionTransactions() > 0) {
            double resolutionRate = (double) stats.getResolvedExceptions() / stats.getExceptionTransactions() * 100;
            summary.append(String.format("- Resolution Rate: %.1f%%\n", resolutionRate));
        }

        // Discrepancy breakdown
        summary.append("\n⚠️  DISCREPANCY ANALYSIS:\n");
        if (stats.getDiscrepanciesByType() != null) {
            stats.getDiscrepanciesByType().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> summary.append(String.format("- %s: %,d\n",
                    formatDiscrepancyType(entry.getKey()), entry.getValue())));
        }

        // Compliance status
        summary.append("\n✅ COMPLIANCE STATUS:\n");
        String complianceStatus = determineComplianceStatus(stats);
        summary.append("- Status: ").append(complianceStatus).append("\n");

        if (stats.getMatchingRate() >= 0.95) {
            summary.append("- ✓ Matching rate meets regulatory requirements (>95%)\n");
        } else {
            summary.append("- ⚠️  Matching rate below regulatory threshold\n");
        }

        // Recommendations
        summary.append("\n💡 RECOMMENDATIONS:\n");
        List<String> recommendations = generateRecommendations(stats);
        for (String rec : recommendations) {
            summary.append("- ").append(rec).append("\n");
        }

        // Generated reports
        summary.append("\n📄 GENERATED REPORTS:\n");
        summary.append("- Reconciliation Summary Report (PDF)\n");
        summary.append("- Discrepancy Details Report (Excel)\n");
        summary.append("- Audit Trail (JSON)\n");
        summary.append("- Compliance Report (PDF)\n");

        return summary.toString();
    }

    private String formatDiscrepancyType(String type) {
        switch (type.toLowerCase()) {
            case "amount_mismatch": return "Amount Mismatch";
            case "missing_transaction": return "Missing Transaction";
            case "extra_transaction": return "Extra Transaction";
            case "failed_transaction": return "Failed Transaction";
            case "high_value_missing": return "High Value Missing";
            case "critical_missing": return "Critical Missing";
            default: return type.replace("_", " ").toUpperCase();
        }
    }

    private String determineComplianceStatus(ReconciliationWorkflow.ReconciliationStats stats) {
        if (stats.getMatchingRate() >= 0.98) {
            return "EXCELLENT";
        } else if (stats.getMatchingRate() >= 0.95) {
            return "COMPLIANT";
        } else if (stats.getMatchingRate() >= 0.90) {
            return "ACCEPTABLE";
        } else {
            return "REQUIRES_ATTENTION";
        }
    }

    private List<String> generateRecommendations(ReconciliationWorkflow.ReconciliationStats stats) {
        List<String> recommendations = new ArrayList<>();

        if (stats.getMatchingRate() < 0.95) {
            recommendations.add("Review and improve matching algorithms to achieve >95% match rate");
        }

        if (stats.getExceptionRate() > 0.05) {
            recommendations.add("Implement additional automated resolution rules for common discrepancies");
        }

        if (stats.getPendingManualReview() > 10) {
            recommendations.add("Consider increasing reconciliation team capacity for manual reviews");
        }

        if (stats.getDiscrepanciesByType() != null) {
            // Check for specific discrepancy types
            stats.getDiscrepanciesByType().forEach((type, count) -> {
                if (type.contains("MISSING") && count > 5) {
                    recommendations.add("Investigate high number of missing transactions - possible system issues");
                }
                if (type.contains("AMOUNT") && count > 10) {
                    recommendations.add("Review fee structures and rounding rules for amount discrepancies");
                }
            });
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Reconciliation process performing well - continue monitoring");
        }

        return recommendations;
    }
}
