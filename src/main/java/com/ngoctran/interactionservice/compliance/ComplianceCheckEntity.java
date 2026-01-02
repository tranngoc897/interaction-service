package com.ngoctran.interactionservice.compliance;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Compliance Check Entity - Enhanced AML/KYC compliance tracking
 * Similar to onboarding's compliance features
 */
@Entity
@Table(name = "compliance_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String applicantId;

    @Column(nullable = false)
    private String checkType; // AML, KYC, SANCTIONS, PEPS, ADVERSE_MEDIA

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, PASSED, FAILED, REVIEW_NEEDED

    @Column(columnDefinition = "TEXT")
    private String checkData; // JSON input data for the check

    @Column(columnDefinition = "TEXT")
    private String checkResult; // JSON result data

    private String riskLevel; // LOW, MEDIUM, HIGH

    private Boolean manualReviewRequired;

    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // When check needs re-validation

    @Column(columnDefinition = "TEXT")
    private String auditTrail; // JSON audit log

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(90); // Default 90 days
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
