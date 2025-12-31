package com.ngoctran.interactionservice.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JavaDelegate for credit score checking
 * Retrieves and evaluates customer credit score from credit bureaus
 */
@Component("creditCheckDelegate")
public class CreditCheckDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CreditCheckDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing credit check for process: {}", execution.getProcessInstanceId());

        try {
            // Get customer information from process variables
            String caseId = (String) execution.getVariable("caseId");
            String applicantId = (String) execution.getVariable("applicantId");
            @SuppressWarnings("unchecked")
            Map<String, Object> extractedData = (Map<String, Object>) execution.getVariable("extractedData");

            if (extractedData == null) {
                log.warn("No extracted data found for credit check");
                execution.setVariable("creditCheckCompleted", false);
                execution.setVariable("creditCheckStatus", "NO_DATA");
                return;
            }

            String idNumber = (String) extractedData.get("idNumber");
            String fullName = (String) extractedData.get("fullName");
            String dateOfBirth = (String) extractedData.get("dateOfBirth");

            log.info("Performing credit check for: name={}, id={}", fullName, idNumber);

            // Perform credit bureau check
            Map<String, Object> creditReport = performCreditBureauCheck(idNumber, fullName, dateOfBirth);

            // Calculate credit score
            int creditScore = (int) creditReport.getOrDefault("creditScore", 0);
            String creditRating = determineCreditRating(creditScore);
            String riskCategory = determineRiskCategory(creditScore);

            // Check for negative indicators
            boolean hasDefaults = (boolean) creditReport.getOrDefault("hasDefaults", false);
            boolean hasBankruptcy = (boolean) creditReport.getOrDefault("hasBankruptcy", false);
            int latePayments = (int) creditReport.getOrDefault("latePayments", 0);

            // Determine approval recommendation
            boolean creditCheckPassed = evaluateCreditWorthiness(
                    creditScore, hasDefaults, hasBankruptcy, latePayments);

            // Set process variables for BPMN flow
            execution.setVariable("creditCheckCompleted", true);
            execution.setVariable("creditCheckStatus", creditCheckPassed ? "PASSED" : "FAILED");
            execution.setVariable("creditScore", creditScore);
            execution.setVariable("creditRating", creditRating);
            execution.setVariable("riskCategory", riskCategory);
            execution.setVariable("creditReport", creditReport);
            execution.setVariable("creditCheckPassed", creditCheckPassed);

            log.info("Credit check completed: score={}, rating={}, passed={}",
                    creditScore, creditRating, creditCheckPassed);

        } catch (Exception e) {
            log.error("Credit check failed: {}", e.getMessage(), e);
            execution.setVariable("creditCheckCompleted", false);
            execution.setVariable("creditCheckStatus", "ERROR");
            execution.setVariable("creditCheckError", e.getMessage());
            throw e;
        }
    }

    /**
     * Perform credit bureau check (simulated)
     * In production, integrate with actual credit bureaus (Experian, Equifax, etc.)
     */
    private Map<String, Object> performCreditBureauCheck(
            String idNumber,
            String fullName,
            String dateOfBirth) {

        log.info("Calling credit bureau API...");

        try {
            // Simulate API call delay
            Thread.sleep(1500);

            Map<String, Object> creditReport = new HashMap<>();

            // Generate simulated credit score (300-850 range)
            int creditScore = 300 + (int) (Math.random() * 550);
            creditReport.put("creditScore", creditScore);

            // Generate credit history details
            creditReport.put("creditHistoryLength", (int) (Math.random() * 15) + 1); // 1-15 years
            creditReport.put("totalAccounts", (int) (Math.random() * 10) + 1); // 1-10 accounts
            creditReport.put("activeAccounts", (int) (Math.random() * 5) + 1); // 1-5 active
            creditReport.put("closedAccounts", (int) (Math.random() * 5)); // 0-5 closed

            // Payment history
            creditReport.put("latePayments", (int) (Math.random() * 3)); // 0-3 late payments
            creditReport.put("onTimePaymentPercentage", 85 + (Math.random() * 15)); // 85-100%

            // Negative indicators
            creditReport.put("hasDefaults", Math.random() < 0.05); // 5% chance
            creditReport.put("hasBankruptcy", Math.random() < 0.02); // 2% chance
            creditReport.put("hasCollections", Math.random() < 0.08); // 8% chance

            // Credit utilization
            creditReport.put("totalCreditLimit", 10000 + (Math.random() * 40000)); // $10k-$50k
            creditReport.put("totalCreditUsed", Math.random() * 20000); // $0-$20k
            double utilization = ((double) creditReport.get("totalCreditUsed") /
                    (double) creditReport.get("totalCreditLimit")) * 100;
            creditReport.put("creditUtilization", utilization);

            // Inquiries
            creditReport.put("hardInquiries", (int) (Math.random() * 3)); // 0-3 inquiries
            creditReport.put("softInquiries", (int) (Math.random() * 5)); // 0-5 inquiries

            // Bureau information
            creditReport.put("bureauName", "National Credit Bureau");
            creditReport.put("reportDate", java.time.LocalDate.now().toString());
            creditReport.put("reportId", "CR" + System.currentTimeMillis());

            log.info("Credit bureau check completed: score={}", creditScore);
            return creditReport;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted during credit bureau check", e);
            return Map.of("error", "Credit bureau check interrupted");
        } catch (Exception e) {
            log.error("Error performing credit bureau check: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Determine credit rating based on score
     */
    private String determineCreditRating(int creditScore) {
        if (creditScore >= 800) {
            return "EXCELLENT";
        } else if (creditScore >= 740) {
            return "VERY_GOOD";
        } else if (creditScore >= 670) {
            return "GOOD";
        } else if (creditScore >= 580) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    /**
     * Determine risk category based on credit score
     */
    private String determineRiskCategory(int creditScore) {
        if (creditScore >= 740) {
            return "LOW_RISK";
        } else if (creditScore >= 670) {
            return "MEDIUM_RISK";
        } else if (creditScore >= 580) {
            return "HIGH_RISK";
        } else {
            return "VERY_HIGH_RISK";
        }
    }

    /**
     * Evaluate overall creditworthiness
     */
    private boolean evaluateCreditWorthiness(
            int creditScore,
            boolean hasDefaults,
            boolean hasBankruptcy,
            int latePayments) {

        // Minimum credit score requirement
        if (creditScore < 580) {
            log.info("Credit check failed: score below minimum ({})", creditScore);
            return false;
        }

        // Check for bankruptcy
        if (hasBankruptcy) {
            log.info("Credit check failed: bankruptcy on record");
            return false;
        }

        // Check for defaults
        if (hasDefaults) {
            log.info("Credit check failed: defaults on record");
            return false;
        }

        // Check late payments
        if (latePayments > 2) {
            log.info("Credit check failed: too many late payments ({})", latePayments);
            return false;
        }

        // Passed all checks
        log.info("Credit check passed: score={}, no major negative indicators", creditScore);
        return true;
    }
}
