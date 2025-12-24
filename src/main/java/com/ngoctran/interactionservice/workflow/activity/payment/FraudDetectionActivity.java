package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Fraud Detection Activity
 * Performs risk assessment and fraud detection on payments
 */
@ActivityInterface
public interface FraudDetectionActivity {

    /**
     * Perform fraud detection and risk assessment
     * @param paymentId Unique payment identifier
     * @param accountId Account making the payment
     * @param amount Payment amount
     * @param currency Payment currency
     * @param accountVerificationResult Result from account verification
     * @return Fraud detection result
     */
    FraudDetectionResult detectFraud(String paymentId, String accountId, double amount, String currency,
                                   AccountVerificationActivity.AccountVerificationResult accountVerificationResult);

    /**
     * Fraud detection result DTO
     */
    class FraudDetectionResult {
        private boolean approved;
        private int riskScore;
        private String riskLevel;
        private String recommendation;
        private FraudDetails details;
        private String errorMessage;

        public FraudDetectionResult() {}

        public FraudDetectionResult(boolean approved, int riskScore, String riskLevel,
                                  String recommendation, FraudDetails details, String errorMessage) {
            this.approved = approved;
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.recommendation = recommendation;
            this.details = details;
            this.errorMessage = errorMessage;
        }

        // Getters and setters
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }

        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

        public FraudDetails getDetails() { return details; }
        public void setDetails(FraudDetails details) { this.details = details; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Detailed fraud detection information
     */
    class FraudDetails {
        private boolean amountAnomaly;
        private boolean frequencyAnomaly;
        private boolean locationAnomaly;
        private boolean timeAnomaly;
        private int transactionCountLast24h;
        private double averageAmountLast30d;
        private String[] triggeredRules;

        public FraudDetails() {}

        public FraudDetails(boolean amountAnomaly, boolean frequencyAnomaly, boolean locationAnomaly,
                          boolean timeAnomaly, int transactionCountLast24h, double averageAmountLast30d,
                          String[] triggeredRules) {
            this.amountAnomaly = amountAnomaly;
            this.frequencyAnomaly = frequencyAnomaly;
            this.locationAnomaly = locationAnomaly;
            this.timeAnomaly = timeAnomaly;
            this.transactionCountLast24h = transactionCountLast24h;
            this.averageAmountLast30d = averageAmountLast30d;
            this.triggeredRules = triggeredRules;
        }

        // Getters and setters
        public boolean isAmountAnomaly() { return amountAnomaly; }
        public void setAmountAnomaly(boolean amountAnomaly) { this.amountAnomaly = amountAnomaly; }

        public boolean isFrequencyAnomaly() { return frequencyAnomaly; }
        public void setFrequencyAnomaly(boolean frequencyAnomaly) { this.frequencyAnomaly = frequencyAnomaly; }

        public boolean isLocationAnomaly() { return locationAnomaly; }
        public void setLocationAnomaly(boolean locationAnomaly) { this.locationAnomaly = locationAnomaly; }

        public boolean isTimeAnomaly() { return timeAnomaly; }
        public void setTimeAnomaly(boolean timeAnomaly) { this.timeAnomaly = timeAnomaly; }

        public int getTransactionCountLast24h() { return transactionCountLast24h; }
        public void setTransactionCountLast24h(int transactionCountLast24h) { this.transactionCountLast24h = transactionCountLast24h; }

        public double getAverageAmountLast30d() { return averageAmountLast30d; }
        public void setAverageAmountLast30d(double averageAmountLast30d) { this.averageAmountLast30d = averageAmountLast30d; }

        public String[] getTriggeredRules() { return triggeredRules; }
        public void setTriggeredRules(String[] triggeredRules) { this.triggeredRules = triggeredRules; }
    }
}
