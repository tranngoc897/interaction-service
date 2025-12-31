package com.ngoctran.interactionservice.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JavaDelegate for document verification
 * Verifies authenticity and validity of uploaded documents
 */
@Component("documentVerificationDelegate")
public class DocumentVerificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(DocumentVerificationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing document verification for process: {}", execution.getProcessInstanceId());

        try {
            // Get OCR results and extracted data
            String caseId = (String) execution.getVariable("caseId");
            @SuppressWarnings("unchecked")
            Map<String, Object> ocrResults = (Map<String, Object>) execution.getVariable("ocrResults");
            @SuppressWarnings("unchecked")
            Map<String, Object> extractedData = (Map<String, Object>) execution.getVariable("extractedData");

            if (ocrResults == null || extractedData == null) {
                log.warn("No OCR results found for document verification");
                execution.setVariable("documentsVerified", false);
                execution.setVariable("verificationStatus", "NO_DATA");
                return;
            }

            log.info("Verifying documents for case: {}", caseId);

            Map<String, Object> verificationResults = new HashMap<>();
            boolean allVerified = true;

            // Verify ID document
            if (ocrResults.containsKey("idDocument")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> idOcrResult = (Map<String, Object>) ocrResults.get("idDocument");
                Map<String, Object> idVerification = verifyIdDocument(idOcrResult, extractedData);
                verificationResults.put("idDocument", idVerification);

                if (!"VERIFIED".equals(idVerification.get("status"))) {
                    allVerified = false;
                }
            }

            // Verify proof of address
            if (ocrResults.containsKey("proofOfAddress")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> addressOcrResult = (Map<String, Object>) ocrResults.get("proofOfAddress");
                Map<String, Object> addressVerification = verifyAddressDocument(addressOcrResult, extractedData);
                verificationResults.put("proofOfAddress", addressVerification);

                if (!"VERIFIED".equals(addressVerification.get("status"))) {
                    allVerified = false;
                }
            }

            // Verify income proof
            if (ocrResults.containsKey("incomeProof")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> incomeOcrResult = (Map<String, Object>) ocrResults.get("incomeProof");
                Map<String, Object> incomeVerification = verifyIncomeDocument(incomeOcrResult, extractedData);
                verificationResults.put("incomeProof", incomeVerification);

                if (!"VERIFIED".equals(incomeVerification.get("status"))) {
                    allVerified = false;
                }
            }

            // Cross-verify data consistency
            Map<String, Object> crossVerification = performCrossVerification(verificationResults, extractedData);
            boolean crossVerified = "PASSED".equals(crossVerification.get("status"));

            // Overall verification result
            boolean overallVerified = allVerified && crossVerified;

            // Set process variables for BPMN flow
            execution.setVariable("documentsVerified", overallVerified);
            execution.setVariable("verificationStatus", overallVerified ? "VERIFIED" : "VERIFICATION_FAILED");
            execution.setVariable("verificationResults", verificationResults);
            execution.setVariable("crossVerification", crossVerification);
            execution.setVariable("verificationScore", calculateVerificationScore(verificationResults));

            log.info("Document verification completed: verified={}, score={}",
                    overallVerified, execution.getVariable("verificationScore"));

        } catch (Exception e) {
            log.error("Document verification failed: {}", e.getMessage(), e);
            execution.setVariable("documentsVerified", false);
            execution.setVariable("verificationStatus", "ERROR");
            execution.setVariable("verificationError", e.getMessage());
            throw e;
        }
    }

    /**
     * Verify ID document authenticity
     */
    private Map<String, Object> verifyIdDocument(Map<String, Object> ocrResult, Map<String, Object> extractedData) {
        log.info("Verifying ID document authenticity...");

        try {
            // Simulate verification API call
            Thread.sleep(1200);

            Map<String, Object> verification = new HashMap<>();

            // Check document quality
            double confidence = (double) ocrResult.getOrDefault("confidence", 0.0);
            if (confidence < 0.85) {
                verification.put("status", "LOW_QUALITY");
                verification.put("reason", "Document quality too low for verification");
                return verification;
            }

            // Simulate government database check
            boolean govDbCheck = Math.random() < 0.95; // 95% pass rate

            // Simulate document tampering detection
            boolean tamperingCheck = Math.random() < 0.98; // 98% pass rate

            // Simulate expiry date check
            boolean expiryCheck = true; // Assume not expired for simulation

            if (govDbCheck && tamperingCheck && expiryCheck) {
                verification.put("status", "VERIFIED");
                verification.put("verificationMethod", "GOVERNMENT_DATABASE");
                verification.put("tamperingDetected", false);
                verification.put("documentExpired", false);
                verification.put("matchScore", 0.96);
            } else {
                verification.put("status", "FAILED");
                verification.put("govDbCheck", govDbCheck);
                verification.put("tamperingCheck", tamperingCheck);
                verification.put("expiryCheck", expiryCheck);
            }

            log.info("ID document verification: {}", verification.get("status"));
            return verification;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("status", "ERROR", "error", "Verification interrupted");
        } catch (Exception e) {
            log.error("Error verifying ID document: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    /**
     * Verify proof of address document
     */
    private Map<String, Object> verifyAddressDocument(Map<String, Object> ocrResult,
            Map<String, Object> extractedData) {
        log.info("Verifying proof of address document...");

        try {
            // Simulate verification API call
            Thread.sleep(800);

            Map<String, Object> verification = new HashMap<>();

            // Check document recency (should be within 3 months)
            boolean recencyCheck = Math.random() < 0.90; // 90% pass rate

            // Check address matches ID document
            boolean addressMatch = Math.random() < 0.85; // 85% match rate

            // Verify utility company
            boolean utilityVerification = Math.random() < 0.95; // 95% pass rate

            if (recencyCheck && addressMatch && utilityVerification) {
                verification.put("status", "VERIFIED");
                verification.put("documentRecent", true);
                verification.put("addressMatch", true);
                verification.put("utilityVerified", true);
            } else {
                verification.put("status", "FAILED");
                verification.put("recencyCheck", recencyCheck);
                verification.put("addressMatch", addressMatch);
                verification.put("utilityVerification", utilityVerification);
            }

            log.info("Address document verification: {}", verification.get("status"));
            return verification;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("status", "ERROR", "error", "Verification interrupted");
        } catch (Exception e) {
            log.error("Error verifying address document: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    /**
     * Verify income proof document
     */
    private Map<String, Object> verifyIncomeDocument(Map<String, Object> ocrResult, Map<String, Object> extractedData) {
        log.info("Verifying income proof document...");

        try {
            // Simulate verification API call
            Thread.sleep(700);

            Map<String, Object> verification = new HashMap<>();

            // Verify employer exists
            boolean employerCheck = Math.random() < 0.92; // 92% pass rate

            // Check income consistency
            boolean incomeConsistency = Math.random() < 0.88; // 88% pass rate

            // Verify document format
            boolean formatCheck = Math.random() < 0.95; // 95% pass rate

            if (employerCheck && incomeConsistency && formatCheck) {
                verification.put("status", "VERIFIED");
                verification.put("employerVerified", true);
                verification.put("incomeConsistent", true);
                verification.put("formatValid", true);
            } else {
                verification.put("status", "FAILED");
                verification.put("employerCheck", employerCheck);
                verification.put("incomeConsistency", incomeConsistency);
                verification.put("formatCheck", formatCheck);
            }

            log.info("Income document verification: {}", verification.get("status"));
            return verification;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("status", "ERROR", "error", "Verification interrupted");
        } catch (Exception e) {
            log.error("Error verifying income document: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    /**
     * Perform cross-verification of data across documents
     */
    private Map<String, Object> performCrossVerification(
            Map<String, Object> verificationResults,
            Map<String, Object> extractedData) {

        log.info("Performing cross-verification of document data...");

        Map<String, Object> crossVerification = new HashMap<>();
        int matchCount = 0;
        int totalChecks = 0;

        // Check name consistency across documents
        totalChecks++;
        if (checkNameConsistency(extractedData)) {
            matchCount++;
            crossVerification.put("nameConsistent", true);
        } else {
            crossVerification.put("nameConsistent", false);
        }

        // Check address consistency
        totalChecks++;
        if (checkAddressConsistency(extractedData)) {
            matchCount++;
            crossVerification.put("addressConsistent", true);
        } else {
            crossVerification.put("addressConsistent", false);
        }

        // Calculate match percentage
        double matchPercentage = (double) matchCount / totalChecks;
        crossVerification.put("matchPercentage", matchPercentage);
        crossVerification.put("status", matchPercentage >= 0.8 ? "PASSED" : "FAILED");

        log.info("Cross-verification completed: matchPercentage={}", matchPercentage);
        return crossVerification;
    }

    private boolean checkNameConsistency(Map<String, Object> extractedData) {
        // Simplified name consistency check
        return Math.random() < 0.90; // 90% consistency rate
    }

    private boolean checkAddressConsistency(Map<String, Object> extractedData) {
        // Simplified address consistency check
        return Math.random() < 0.85; // 85% consistency rate
    }

    /**
     * Calculate overall verification score
     */
    private double calculateVerificationScore(Map<String, Object> verificationResults) {
        int verifiedCount = 0;
        int totalDocuments = verificationResults.size();

        for (Object result : verificationResults.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> verificationResult = (Map<String, Object>) result;
            if ("VERIFIED".equals(verificationResult.get("status"))) {
                verifiedCount++;
            }
        }

        return totalDocuments > 0 ? (double) verifiedCount / totalDocuments : 0.0;
    }
}
