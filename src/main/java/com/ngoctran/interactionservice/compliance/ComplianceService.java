package com.ngoctran.interactionservice.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compliance Service - Enhanced AML/KYC compliance checking
 * Similar to ABB onboarding's compliance features
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    private final ComplianceCheckRepository complianceCheckRepository;
    private final ObjectMapper objectMapper;

    /**
     * Perform AML screening
     */
    @Transactional
    public ComplianceCheckEntity performAmlScreening(String caseId, String applicantId, Map<String, Object> applicantData) {
        log.info("Performing AML screening: caseId={}, applicantId={}", caseId, applicantId);

        try {
            String checkDataJson = objectMapper.writeValueAsString(applicantData);

            // Simulate AML check (in real implementation, call external AML service)
            Map<String, Object> amlResult = performExternalAmlCheck(applicantData);

            String checkResultJson = objectMapper.writeValueAsString(amlResult);
            String riskLevel = determineRiskLevel(amlResult);

            ComplianceCheckEntity check = ComplianceCheckEntity.builder()
                    .caseId(caseId)
                    .applicantId(applicantId)
                    .checkType("AML")
                    .status("PASSED") // Default to passed, change based on result
                    .checkData(checkDataJson)
                    .checkResult(checkResultJson)
                    .riskLevel(riskLevel)
                    .manualReviewRequired(Boolean.FALSE)
                    .auditTrail(createAuditEntry("AML_CHECK_COMPLETED", "System", "Automated AML screening completed"))
                    .build();

            // Determine if manual review is needed
            if ("HIGH".equals(riskLevel) || hasAmlHits(amlResult)) {
                check.setStatus("REVIEW_NEEDED");
                check.setManualReviewRequired(Boolean.TRUE);
            }

            return complianceCheckRepository.save(check);

        } catch (Exception e) {
            log.error("AML screening failed: {}", e.getMessage(), e);

            // Create failed check record
            ComplianceCheckEntity failedCheck = ComplianceCheckEntity.builder()
                    .caseId(caseId)
                    .applicantId(applicantId)
                    .checkType("AML")
                    .status("FAILED")
                    .manualReviewRequired(Boolean.TRUE)
                    .auditTrail(createAuditEntry("AML_CHECK_FAILED", "System", e.getMessage()))
                    .build();

            return complianceCheckRepository.save(failedCheck);
        }
    }

    /**
     * Perform KYC verification
     */
    @Transactional
    public ComplianceCheckEntity performKycVerification(String caseId, String applicantId, Map<String, Object> verificationData) {
        log.info("Performing KYC verification: caseId={}, applicantId={}", caseId, applicantId);

        try {
            String checkDataJson = objectMapper.writeValueAsString(verificationData);

            // Simulate KYC check (in real implementation, call external KYC service)
            Map<String, Object> kycResult = performExternalKycCheck(verificationData);

            String checkResultJson = objectMapper.writeValueAsString(kycResult);
            boolean passed = (Boolean) kycResult.getOrDefault("verified", false);

            ComplianceCheckEntity check = ComplianceCheckEntity.builder()
                    .caseId(caseId)
                    .applicantId(applicantId)
                    .checkType("KYC")
                    .status(passed ? "PASSED" : "FAILED")
                    .checkData(checkDataJson)
                    .checkResult(checkResultJson)
                    .riskLevel(passed ? "LOW" : "HIGH")
                    .manualReviewRequired(!passed)
                    .auditTrail(createAuditEntry("KYC_CHECK_COMPLETED", "System", "KYC verification completed"))
                    .build();

            return complianceCheckRepository.save(check);

        } catch (Exception e) {
            log.error("KYC verification failed: {}", e.getMessage(), e);

            ComplianceCheckEntity failedCheck = ComplianceCheckEntity.builder()
                    .caseId(caseId)
                    .applicantId(applicantId)
                    .checkType("KYC")
                    .status("FAILED")
                    .manualReviewRequired(Boolean.TRUE)
                    .auditTrail(createAuditEntry("KYC_CHECK_FAILED", "System", e.getMessage()))
                    .build();

            return complianceCheckRepository.save(failedCheck);
        }
    }

    /**
     * Perform sanctions screening
     */
    @Transactional
    public ComplianceCheckEntity performSanctionsScreening(String caseId, String applicantId, Map<String, Object> applicantData) {
        log.info("Performing sanctions screening: caseId={}, applicantId={}", caseId, applicantId);

        try {
            String checkDataJson = objectMapper.writeValueAsString(applicantData);

            // Check against sanctions lists
            Map<String, Object> sanctionsResult = checkSanctionsLists(applicantData);

            String checkResultJson = objectMapper.writeValueAsString(sanctionsResult);
            boolean hasHits = (Boolean) sanctionsResult.getOrDefault("hasSanctionsHits", false);

            ComplianceCheckEntity check = ComplianceCheckEntity.builder()
                    .caseId(caseId)
                    .applicantId(applicantId)
                    .checkType("SANCTIONS")
                    .status(hasHits ? "REVIEW_NEEDED" : "PASSED")
                    .checkData(checkDataJson)
                    .checkResult(checkResultJson)
                    .riskLevel(hasHits ? "HIGH" : "LOW")
                    .manualReviewRequired(hasHits)
                    .auditTrail(createAuditEntry("SANCTIONS_CHECK_COMPLETED", "System", "Sanctions screening completed"))
                    .build();

            return complianceCheckRepository.save(check);

        } catch (Exception e) {
            log.error("Sanctions screening failed: {}", e.getMessage(), e);

            ComplianceCheckEntity failedCheck = ComplianceCheckEntity.builder()
                    .caseId(caseId)
                    .applicantId(applicantId)
                    .checkType("SANCTIONS")
                    .status("FAILED")
                    .manualReviewRequired(Boolean.TRUE)
                    .auditTrail(createAuditEntry("SANCTIONS_CHECK_FAILED", "System", e.getMessage()))
                    .build();

            return complianceCheckRepository.save(failedCheck);
        }
    }

    /**
     * Manual review of compliance check
     */
    @Transactional
    public ComplianceCheckEntity performManualReview(Long checkId, String reviewer, String decision, String comments) {
        log.info("Performing manual review: checkId={}, reviewer={}, decision={}", checkId, reviewer, decision);

        ComplianceCheckEntity check = complianceCheckRepository.findById(checkId)
                .orElseThrow(() -> new RuntimeException("Compliance check not found: " + checkId));

        check.setReviewedBy(reviewer);
        check.setReviewedAt(LocalDateTime.now());
        check.setManualReviewRequired(Boolean.FALSE);

        if ("APPROVE".equals(decision)) {
            check.setStatus("PASSED");
            check.setRiskLevel("LOW");
        } else if ("REJECT".equals(decision)) {
            check.setStatus("FAILED");
            check.setRiskLevel("HIGH");
        } else {
            check.setStatus("REVIEW_NEEDED");
            check.setManualReviewRequired(Boolean.TRUE);
        }

        // Add to audit trail
        String auditEntry = createAuditEntry("MANUAL_REVIEW_COMPLETED", reviewer,
                "Decision: " + decision + ". Comments: " + comments);
        check.setAuditTrail(check.getAuditTrail() + "," + auditEntry);

        return complianceCheckRepository.save(check);
    }

    /**
     * Get compliance checks for a case
     */
    public List<ComplianceCheckEntity> getComplianceChecksForCase(String caseId) {
        return complianceCheckRepository.findByCaseId(caseId);
    }

    /**
     * Get compliance checks for an applicant
     */
    public List<ComplianceCheckEntity> getComplianceChecksForApplicant(String applicantId) {
        return complianceCheckRepository.findByApplicantId(applicantId);
    }

    /**
     * Get pending manual reviews
     */
    public List<ComplianceCheckEntity> getPendingManualReviews() {
        return complianceCheckRepository.findByManualReviewRequiredAndStatus(Boolean.TRUE, "REVIEW_NEEDED");
    }

    /**
     * Check if applicant passed all compliance checks
     */
    public boolean hasPassedAllComplianceChecks(String caseId, String applicantId) {
        List<ComplianceCheckEntity> checks = complianceCheckRepository
                .findByCaseIdAndApplicantId(caseId, applicantId);

        return checks.stream().allMatch(check ->
                "PASSED".equals(check.getStatus()) && !Boolean.TRUE.equals(check.getManualReviewRequired()));
    }

    /**
     * Get compliance summary for case
     */
    public Map<String, Object> getComplianceSummary(String caseId) {
        List<ComplianceCheckEntity> checks = complianceCheckRepository.findByCaseId(caseId);

        long passed = checks.stream().filter(c -> "PASSED".equals(c.getStatus())).count();
        long failed = checks.stream().filter(c -> "FAILED".equals(c.getStatus())).count();
        long pending = checks.stream().filter(c -> "PENDING".equals(c.getStatus())).count();
        long reviewNeeded = checks.stream().filter(c -> "REVIEW_NEEDED".equals(c.getStatus())).count();

        String overallStatus = "UNKNOWN";
        if (failed > 0) {
            overallStatus = "FAILED";
        } else if (reviewNeeded > 0 || pending > 0) {
            overallStatus = "PENDING_REVIEW";
        } else if (passed > 0) {
            overallStatus = "PASSED";
        }

        return Map.of(
                "totalChecks", checks.size(),
                "passed", passed,
                "failed", failed,
                "pending", pending,
                "reviewNeeded", reviewNeeded,
                "overallStatus", overallStatus
        );
    }

    /**
     * Expire old compliance checks
     */
    @Transactional
    public int expireOldChecks() {
        log.info("Expiring old compliance checks");

        List<ComplianceCheckEntity> expiredChecks = complianceCheckRepository
                .findExpiredChecks(LocalDateTime.now());

        expiredChecks.forEach(check -> {
            check.setStatus("EXPIRED");
            check.setManualReviewRequired(Boolean.TRUE);
        });

        complianceCheckRepository.saveAll(expiredChecks);
        return expiredChecks.size();
    }

    // Private helper methods

    private Map<String, Object> performExternalAmlCheck(Map<String, Object> applicantData) {
        // Simulate external AML service call
        // In real implementation, call actual AML service
        return Map.of(
                "amlHits", List.of(),
                "riskScore", Math.random() * 100,
                "checkedAt", LocalDateTime.now(),
                "hasMatches", false
        );
    }

    private Map<String, Object> performExternalKycCheck(Map<String, Object> verificationData) {
        // Simulate external KYC service call
        // In real implementation, call actual KYC service
        return Map.of(
                "verified", Math.random() > 0.1, // 90% pass rate
                "confidence", Math.random() * 100,
                "biometricMatch", Math.random() > 0.05,
                "documentValid", Math.random() > 0.05
        );
    }

    private Map<String, Object> checkSanctionsLists(Map<String, Object> applicantData) {
        // Simulate sanctions list check
        // In real implementation, check against actual sanctions lists
        return Map.of(
                "hasSanctionsHits", Math.random() > 0.95, // 5% hit rate
                "sanctionsLists", List.of("OFAC", "EU_SANCTIONS"),
                "checkedAt", LocalDateTime.now()
        );
    }

    private String determineRiskLevel(Map<String, Object> amlResult) {
        double riskScore = ((Number) amlResult.getOrDefault("riskScore", 0)).doubleValue();
        if (riskScore > 70) return "HIGH";
        if (riskScore > 30) return "MEDIUM";
        return "LOW";
    }

    private boolean hasAmlHits(Map<String, Object> amlResult) {
        List<?> hits = (List<?>) amlResult.getOrDefault("amlHits", List.of());
        return !hits.isEmpty();
    }

    private String createAuditEntry(String action, String user, String details) {
        try {
            Map<String, Object> auditEntry = Map.of(
                    "timestamp", LocalDateTime.now(),
                    "action", action,
                    "user", user,
                    "details", details
            );
            return objectMapper.writeValueAsString(auditEntry);
        } catch (Exception e) {
            log.warn("Failed to create audit entry", e);
            return "{}";
        }
    }
}
