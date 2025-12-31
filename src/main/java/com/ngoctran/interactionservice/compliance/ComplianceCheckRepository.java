package com.ngoctran.interactionservice.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplianceCheckRepository extends JpaRepository<ComplianceCheckEntity, Long> {

    List<ComplianceCheckEntity> findByCaseId(String caseId);

    List<ComplianceCheckEntity> findByApplicantId(String applicantId);

    List<ComplianceCheckEntity> findByCaseIdAndApplicantId(String caseId, String applicantId);

    List<ComplianceCheckEntity> findByCheckType(String checkType);

    List<ComplianceCheckEntity> findByStatus(String status);

    List<ComplianceCheckEntity> findByManualReviewRequiredAndStatus(Boolean manualReviewRequired, String status);

    @Query("SELECT c FROM ComplianceCheckEntity c WHERE c.expiresAt < :now")
    List<ComplianceCheckEntity> findExpiredChecks(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM ComplianceCheckEntity c WHERE c.createdAt >= :since")
    List<ComplianceCheckEntity> findRecentChecks(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM ComplianceCheckEntity c WHERE c.status = :status")
    Long countByStatus(@Param("status") String status);
}
