package com.ngoctran.interactionservice.jointaccount;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Joint Account Entity - Manages joint account relationships
 * Similar to onboarding's joint account support
 */
@Entity
@Table(name = "joint_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JointAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String caseId; // Main case ID

    @Column(nullable = false)
    private String primaryApplicantId; // Main applicant

    @Column(nullable = false)
    private String coApplicantId; // Co-applicant

    @Column(nullable = false)
    private String relationshipType; // SPOUSE, FAMILY_MEMBER, BUSINESS_PARTNER

    @Column(nullable = false)
    private String status; // PENDING, ACTIVE, COMPLETED, CANCELLED

    @Column(name = "invitation_sent_at")
    private LocalDateTime invitationSentAt;

    @Column(name = "invitation_accepted_at")
    private LocalDateTime invitationAcceptedAt;

    @Column(name = "co_applicant_joined_at")
    private LocalDateTime coApplicantJoinedAt;

    @Column(columnDefinition = "TEXT")
    private String invitationToken; // Secure token for co-applicant invitation

    @Column(columnDefinition = "TEXT")
    private String sharedData; // JSON data shared between applicants

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
