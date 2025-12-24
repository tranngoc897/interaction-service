package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Compliance Check Activity
 * Performs regulatory and compliance checks
 */
@ActivityInterface
public interface ComplianceCheckActivity {

    /**
     * Check regulatory compliance for payment
     */
    ComplianceResult checkRegulatoryCompliance(String paymentId, String accountId, double amount,
                                             String currency, String destination);

    /**
     * Compliance result DTO
     */
    class ComplianceResult {
        private boolean compliant;
        private String riskLevel;
        private String[] violations;
        private String[] requiredActions;
        private String complianceReportId;
        private long checkTimestamp;

        public ComplianceResult() {}

        public ComplianceResult(boolean compliant, String riskLevel, String[] violations,
                              String[] requiredActions, String complianceReportId, long checkTimestamp) {
            this.compliant = compliant;
            this.riskLevel = riskLevel;
            this.violations = violations;
            this.requiredActions = requiredActions;
            this.complianceReportId = complianceReportId;
            this.checkTimestamp = checkTimestamp;
        }

        // Getters and setters
        public boolean isCompliant() { return compliant; }
        public void setCompliant(boolean compliant) { this.compliant = compliant; }

        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

        public String[] getViolations() { return violations; }
        public void setViolations(String[] violations) { this.violations = violations; }

        public String[] getRequiredActions() { return requiredActions; }
        public void setRequiredActions(String[] requiredActions) { this.requiredActions = requiredActions; }

        public String getComplianceReportId() { return complianceReportId; }
        public void setComplianceReportId(String complianceReportId) { this.complianceReportId = complianceReportId; }

        public long getCheckTimestamp() { return checkTimestamp; }
        public void setCheckTimestamp(long checkTimestamp) { this.checkTimestamp = checkTimestamp; }
    }
}
