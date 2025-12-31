package com.ngoctran.interactionservice.jointaccount;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Joint Account Service - Manages joint account relationships and parallel processing
 * Similar to ABB onboarding's joint account support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JointAccountService {

    private final JointAccountRepository jointAccountRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a joint account invitation
     */
    @Transactional
    public JointAccountEntity createJointAccountInvitation(String caseId, String primaryApplicantId,
                                                         String coApplicantId, String relationshipType,
                                                         Map<String, Object> sharedData) {
        log.info("Creating joint account invitation: caseId={}, primary={}, coApplicant={}",
                caseId, primaryApplicantId, coApplicantId);

        // Check if invitation already exists
        Optional<JointAccountEntity> existing = jointAccountRepository
                .findByCaseIdAndCoApplicantId(caseId, coApplicantId);
        if (existing.isPresent()) {
            throw new RuntimeException("Joint account invitation already exists for this co-applicant");
        }

        String sharedDataJson = null;
        if (sharedData != null) {
            try {
                sharedDataJson = objectMapper.writeValueAsString(sharedData);
            } catch (Exception e) {
                log.warn("Failed to serialize shared data", e);
            }
        }

        JointAccountEntity jointAccount = JointAccountEntity.builder()
                .caseId(caseId)
                .primaryApplicantId(primaryApplicantId)
                .coApplicantId(coApplicantId)
                .relationshipType(relationshipType)
                .status("PENDING")
                .invitationToken(generateInvitationToken())
                .sharedData(sharedDataJson)
                .invitationSentAt(LocalDateTime.now())
                .build();

        return jointAccountRepository.save(jointAccount);
    }

    /**
     * Accept joint account invitation
     */
    @Transactional
    public JointAccountEntity acceptJointAccountInvitation(String invitationToken) {
        log.info("Accepting joint account invitation: token={}", invitationToken);

        JointAccountEntity jointAccount = jointAccountRepository
                .findByInvitationToken(invitationToken)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        if (!"PENDING".equals(jointAccount.getStatus())) {
            throw new RuntimeException("Invitation is not in pending status: " + jointAccount.getStatus());
        }

        jointAccount.setStatus("ACTIVE");
        jointAccount.setInvitationAcceptedAt(LocalDateTime.now());
        jointAccount.setCoApplicantJoinedAt(LocalDateTime.now());

        return jointAccountRepository.save(jointAccount);
    }

    /**
     * Get joint account by case ID
     */
    public List<JointAccountEntity> getJointAccountsByCase(String caseId) {
        return jointAccountRepository.findByCaseId(caseId);
    }

    /**
     * Get active joint accounts for a case
     */
    public List<JointAccountEntity> getActiveJointAccountsByCase(String caseId) {
        return jointAccountRepository.findByCaseIdAndStatus(caseId, "ACTIVE");
    }

    /**
     * Get joint account by invitation token
     */
    public Optional<JointAccountEntity> getJointAccountByToken(String invitationToken) {
        return jointAccountRepository.findByInvitationToken(invitationToken);
    }

    /**
     * Update shared data between applicants
     */
    @Transactional
    public JointAccountEntity updateSharedData(String caseId, String coApplicantId, Map<String, Object> sharedData) {
        log.info("Updating shared data: caseId={}, coApplicantId={}", caseId, coApplicantId);

        JointAccountEntity jointAccount = jointAccountRepository
                .findByCaseIdAndCoApplicantId(caseId, coApplicantId)
                .orElseThrow(() -> new RuntimeException("Joint account not found"));

        try {
            String sharedDataJson = objectMapper.writeValueAsString(sharedData);
            jointAccount.setSharedData(sharedDataJson);
        } catch (Exception e) {
            log.error("Failed to serialize shared data", e);
            throw new RuntimeException("Shared data update failed: " + e.getMessage(), e);
        }

        return jointAccountRepository.save(jointAccount);
    }

    /**
     * Get shared data as Map
     */
    public Map<String, Object> getSharedData(String caseId, String coApplicantId) {
        JointAccountEntity jointAccount = jointAccountRepository
                .findByCaseIdAndCoApplicantId(caseId, coApplicantId)
                .orElse(null);

        if (jointAccount == null || jointAccount.getSharedData() == null) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(jointAccount.getSharedData(), Map.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize shared data", e);
            return Map.of();
        }
    }

    /**
     * Complete joint account
     */
    @Transactional
    public JointAccountEntity completeJointAccount(String caseId, String coApplicantId) {
        log.info("Completing joint account: caseId={}, coApplicantId={}", caseId, coApplicantId);

        JointAccountEntity jointAccount = jointAccountRepository
                .findByCaseIdAndCoApplicantId(caseId, coApplicantId)
                .orElseThrow(() -> new RuntimeException("Joint account not found"));

        jointAccount.setStatus("COMPLETED");

        return jointAccountRepository.save(jointAccount);
    }

    /**
     * Cancel joint account invitation
     */
    @Transactional
    public JointAccountEntity cancelJointAccountInvitation(String caseId, String coApplicantId) {
        log.info("Cancelling joint account invitation: caseId={}, coApplicantId={}", caseId, coApplicantId);

        JointAccountEntity jointAccount = jointAccountRepository
                .findByCaseIdAndCoApplicantId(caseId, coApplicantId)
                .orElseThrow(() -> new RuntimeException("Joint account not found"));

        jointAccount.setStatus("CANCELLED");

        return jointAccountRepository.save(jointAccount);
    }

    /**
     * Check if case has joint accounts
     */
    public boolean hasJointAccounts(String caseId) {
        return !jointAccountRepository.findByCaseId(caseId).isEmpty();
    }

    /**
     * Check if all joint accounts are completed
     */
    public boolean areAllJointAccountsCompleted(String caseId) {
        List<JointAccountEntity> jointAccounts = jointAccountRepository.findByCaseId(caseId);
        return jointAccounts.stream().allMatch(ja -> "COMPLETED".equals(ja.getStatus()));
    }

    /**
     * Get pending joint account invitations
     */
    public List<JointAccountEntity> getPendingInvitations() {
        return jointAccountRepository.findByStatus("PENDING");
    }

    /**
     * Validate invitation token
     */
    public boolean validateInvitationToken(String token) {
        return jointAccountRepository.findByInvitationToken(token).isPresent();
    }

    /**
     * Get joint account statistics
     */
    public Map<String, Long> getJointAccountStatistics() {
        return Map.of(
                "pending", jointAccountRepository.countByStatus("PENDING"),
                "active", jointAccountRepository.countByStatus("ACTIVE"),
                "completed", jointAccountRepository.countByStatus("COMPLETED"),
                "cancelled", jointAccountRepository.countByStatus("CANCELLED")
        );
    }

    /**
     * Synchronize data between primary and co-applicant workflows
     */
    public void synchronizeApplicantData(String caseId, String applicantId, Map<String, Object> applicantData) {
        log.info("Synchronizing applicant data: caseId={}, applicantId={}", caseId, applicantId);

        // Find all joint accounts for this case
        List<JointAccountEntity> jointAccounts = jointAccountRepository.findByCaseId(caseId);

        for (JointAccountEntity jointAccount : jointAccounts) {
            // Update shared data with applicant's progress
            Map<String, Object> sharedData = getSharedData(caseId,
                    jointAccount.getPrimaryApplicantId().equals(applicantId) ?
                    jointAccount.getCoApplicantId() : jointAccount.getPrimaryApplicantId());

            sharedData.put(applicantId + "_progress", applicantData);
            sharedData.put(applicantId + "_last_updated", LocalDateTime.now());

            updateSharedData(caseId,
                    jointAccount.getPrimaryApplicantId().equals(applicantId) ?
                    jointAccount.getCoApplicantId() : jointAccount.getPrimaryApplicantId(),
                    sharedData);
        }
    }

    private String generateInvitationToken() {
        return UUID.randomUUID().toString();
    }
}
