package com.ngoctran.interactionservice.workflow.activity.reconciliation;

import com.ngoctran.interactionservice.workflow.reconciliation.ReconciliationWorkflow;
import io.temporal.activity.ActivityInterface;
import java.util.List;
import java.util.Map;

/**
 * Reporting Activity
 * Generates reconciliation reports and audit trails
 */
@ActivityInterface
public interface ReportingActivity {

    /**
     * Generate reports and audit trail
     */
    ReportingResult generateReportsAndAudit(String reconciliationId, String date, String type,
                                          ReconciliationWorkflow.ReconciliationStats stats,
                                          List<MatchingActivity.Discrepancy> resolvedDiscrepancies,
                                          List<MatchingActivity.Discrepancy> pendingManualReview,
                                          Map<String, Object> config);

    /**
     * Reporting result
     */
    class ReportingResult {
        private boolean success;
        private String reportUrl;
        private String auditTrailUrl;
        private Map<String, Object> reportMetadata;
        private List<String> generatedReports;
        private String summary;
        private long generatedAt;

        public ReportingResult() {}

        public ReportingResult(boolean success, String reportUrl, String auditTrailUrl,
                             Map<String, Object> reportMetadata, List<String> generatedReports,
                             String summary) {
            this.success = success;
            this.reportUrl = reportUrl;
            this.auditTrailUrl = auditTrailUrl;
            this.reportMetadata = reportMetadata;
            this.generatedReports = generatedReports;
            this.summary = summary;
            this.generatedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getReportUrl() { return reportUrl; }
        public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }

        public String getAuditTrailUrl() { return auditTrailUrl; }
        public void setAuditTrailUrl(String auditTrailUrl) { this.auditTrailUrl = auditTrailUrl; }

        public Map<String, Object> getReportMetadata() { return reportMetadata; }
        public void setReportMetadata(Map<String, Object> reportMetadata) { this.reportMetadata = reportMetadata; }

        public List<String> getGeneratedReports() { return generatedReports; }
        public void setGeneratedReports(List<String> generatedReports) { this.generatedReports = generatedReports; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public long getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }
    }
}
