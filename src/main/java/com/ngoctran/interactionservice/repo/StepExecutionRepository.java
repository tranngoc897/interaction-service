package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.StepExecution;
import com.ngoctran.interactionservice.domain.StepExecutionId;
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
public interface StepExecutionRepository extends JpaRepository<StepExecution, StepExecutionId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StepExecution s WHERE s.instanceId = :instanceId AND s.state = :state")
    StepExecution findByIdForUpdate(@Param("instanceId") UUID instanceId, @Param("state") String state);

    List<StepExecution> findByInstanceId(UUID instanceId);

    @Query("SELECT s FROM StepExecution s WHERE s.status = 'FAILED' AND s.nextRetryAt <= :now")
    List<StepExecution> findScheduledRetries(@Param("now") Instant now,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM StepExecution s WHERE s.status = 'RUNNING' AND s.updatedAt < :stuckTime")
    List<StepExecution> findStuckRunning(@Param("stuckTime") Instant stuckTime);

    default StepExecution lock(UUID instanceId, String state) {
        return findByIdForUpdate(instanceId, state);
    }
}
