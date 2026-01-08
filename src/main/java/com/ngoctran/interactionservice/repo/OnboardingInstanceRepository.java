package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OnboardingInstanceRepository extends JpaRepository<OnboardingInstance, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM OnboardingInstance i WHERE i.id = :id")
    OnboardingInstance findByIdForUpdate(@Param("id") UUID id);

    List<OnboardingInstance> findByUserId(String userId);

    List<OnboardingInstance> findByCurrentState(String currentState);

    List<OnboardingInstance> findByStatus(String status);

    long countByStatus(String status);

    List<OnboardingInstance> findByUserIdAndStatus(String userId, String status);

    @Query("SELECT i FROM OnboardingInstance i WHERE i.currentState = :state AND i.status = 'ACTIVE' AND i.stateStartedAt < :timeoutTime")
    List<OnboardingInstance> findTimedOutInstances(@Param("state") String state,
            @Param("timeoutTime") Instant timeoutTime,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT i FROM OnboardingInstance i WHERE i.status = 'ACTIVE' AND i.stateStartedAt < :breachTime")
    List<OnboardingInstance> findSlaBreaches(@Param("breachTime") Instant breachTime);
}
