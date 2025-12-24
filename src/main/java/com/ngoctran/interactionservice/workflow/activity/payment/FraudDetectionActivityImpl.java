package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fraud Detection Activity Implementation
 * Performs risk assessment and fraud detection on payments
 */
@Component
@Slf4j
public class FraudDetectionActivityImpl implements FraudDetectionActivity {

    private static final Random RANDOM = new Random();

    // Risk thresholds
    private static final int HIGH_RISK_THRESHOLD = 80;
    private static final int MEDIUM_RISK_THRESHOLD = 50;

    // Risk rules
    private static final String RULE_HIGH_AMOUNT = "HIGH_AMOUNT";
    private static final String RULE_FREQUENCY_SPIKE = "FREQUENCY_SPIKE";
    private static final String RULE_TIME_ANOMALY = "TIME_ANOMALY";
    private static final String RULE_LOCATION_CHANGE = "LOCATION_CHANGE";

    @Override
    public FraudDetectionResult detectFraud(String paymentId, String accountId, double amount, String currency,
                                          AccountVerificationActivity.AccountVerificationResult accountResult) {
        log.info("Performing fraud detection for payment: id={}, account={}, amount={}",
                paymentId, accountId, amount);

        try {
            // Simulate fraud detection analysis
            FraudDetails details = analyzePaymentPatterns(paymentId, accountId, amount, currency);

            // Calculate risk score (0-100)
            int riskScore = calculateRiskScore(amount, details);

            // Determine risk level
            String riskLevel = determineRiskLevel(riskScore);

            // Make approval decision
            boolean approved = riskScore < HIGH_RISK_THRESHOLD;

            // Generate recommendation
            String recommendation = generateRecommendation(riskScore, riskLevel, approved);

            FraudDetectionResult result = new FraudDetectionResult(
                    approved,
                    riskScore,
                    riskLevel,
                    recommendation,
                    details,
                    null
            );

            log.info("Fraud detection completed: score={}, level={}, approved={}",
                    riskScore, riskLevel, approved);

            return result;

        } catch (Exception e) {
            log.error("Fraud detection failed for payment: {}", paymentId, e);
            return new FraudDetectionResult(false, 100, "CRITICAL",
                    "Manual review required due to system error", null,
                    "Fraud detection service error: " + e.getMessage());
        }
    }

    /**
     * Analyze payment patterns for anomalies
     */
    private FraudDetails analyzePaymentPatterns(String paymentId, String accountId, double amount, String currency) {
        // Simulate analysis of payment patterns
        boolean amountAnomaly = amount > 50000000; // High amount threshold
        boolean frequencyAnomaly = RANDOM.nextDouble() < 0.1; // 10% chance of frequency anomaly
        boolean locationAnomaly = RANDOM.nextDouble() < 0.05; // 5% chance of location anomaly
        boolean timeAnomaly = RANDOM.nextDouble() < 0.15; // 15% chance of time anomaly

        // Mock transaction history data
        int transactionCountLast24h = RANDOM.nextInt(20) + 1;
        double averageAmountLast30d = 500000 + RANDOM.nextDouble() * 2000000;

        // Determine triggered rules
        List<String> triggeredRules = new ArrayList<>();
        if (amountAnomaly) triggeredRules.add(RULE_HIGH_AMOUNT);
        if (frequencyAnomaly) triggeredRules.add(RULE_FREQUENCY_SPIKE);
        if (timeAnomaly) triggeredRules.add(RULE_TIME_ANOMALY);
        if (locationAnomaly) triggeredRules.add(RULE_LOCATION_CHANGE);

        return new FraudDetails(
                amountAnomaly,
                frequencyAnomaly,
                locationAnomaly,
                timeAnomaly,
                transactionCountLast24h,
                averageAmountLast30d,
                triggeredRules.toArray(new String[0])
        );
    }

    /**
     * Calculate risk score based on various factors
     */
    private int calculateRiskScore(double amount, FraudDetails details) {
        int score = 0;

        // Amount-based scoring
        if (amount > 100000000) score += 40; // Very high amount
        else if (amount > 50000000) score += 25; // High amount
        else if (amount > 10000000) score += 15; // Medium-high amount

        // Anomaly-based scoring
        if (details.isAmountAnomaly()) score += 20;
        if (details.isFrequencyAnomaly()) score += 15;
        if (details.isLocationAnomaly()) score += 25;
        if (details.isTimeAnomaly()) score += 10;

        // Frequency-based scoring
        if (details.getTransactionCountLast24h() > 15) score += 10;
        if (details.getTransactionCountLast24h() > 10) score += 5;

        // Deviation from average
        double deviation = Math.abs(amount - details.getAverageAmountLast30d()) / details.getAverageAmountLast30d();
        if (deviation > 5.0) score += 15; // 500% above average
        else if (deviation > 2.0) score += 8; // 200% above average

        // Rule-based scoring
        score += details.getTriggeredRules().length * 5;

        // Ensure score is within 0-100 range
        return Math.min(100, Math.max(0, score));
    }

    /**
     * Determine risk level based on score
     */
    private String determineRiskLevel(int riskScore) {
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            return "HIGH";
        } else if (riskScore >= MEDIUM_RISK_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Generate recommendation based on risk assessment
     */
    private String generateRecommendation(int riskScore, String riskLevel, boolean approved) {
        if (!approved) {
            if ("HIGH".equals(riskLevel)) {
                return "Block transaction and flag account for investigation";
            } else {
                return "Require additional verification or manual approval";
            }
        } else {
            if ("MEDIUM".equals(riskLevel)) {
                return "Monitor transaction closely";
            } else {
                return "Approve transaction";
            }
        }
    }
}
