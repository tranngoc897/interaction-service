package com.ngoctran.interactionservice.jointaccount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JointAccountRepository extends JpaRepository<JointAccountEntity, Long> {

    List<JointAccountEntity> findByCaseId(String caseId);

    List<JointAccountEntity> findByCaseIdAndStatus(String caseId, String status);

    Optional<JointAccountEntity> findByCaseIdAndCoApplicantId(String caseId, String coApplicantId);

    Optional<JointAccountEntity> findByInvitationToken(String invitationToken);

    List<JointAccountEntity> findByStatus(String status);

    List<JointAccountEntity> findByPrimaryApplicantId(String primaryApplicantId);

    List<JointAccountEntity> findByCoApplicantId(String coApplicantId);

    @Query("SELECT COUNT(ja) FROM JointAccountEntity ja WHERE ja.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT ja FROM JointAccountEntity ja WHERE ja.invitationSentAt < :cutoffDate AND ja.status = 'PENDING'")
    List<JointAccountEntity> findExpiredPendingInvitations(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
