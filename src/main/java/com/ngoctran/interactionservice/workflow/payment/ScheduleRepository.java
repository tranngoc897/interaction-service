package com.ngoctran.interactionservice.workflow.payment;

import com.ngoctran.interactionservice.workflow.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, String> {

    Optional<ScheduleEntity> findByScheduleId(String scheduleId);

    List<ScheduleEntity> findByStatus(ScheduleStatus status);

    List<ScheduleEntity> findByCreatedBy(String createdBy);

    List<ScheduleEntity> findByWorkflowType(String workflowType);

    List<ScheduleEntity> findByStatusAndWorkflowType(ScheduleStatus status, String workflowType);

    @Query("SELECT s FROM ScheduleEntity s WHERE s.createdAt >= :since")
    List<ScheduleEntity> findSchedulesCreatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT s FROM ScheduleEntity s WHERE s.status = :status AND s.createdAt BETWEEN :start AND :end")
    List<ScheduleEntity> findSchedulesByStatusAndDateRange(
            @Param("status") ScheduleStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByStatus(ScheduleStatus status);

    boolean existsByScheduleIdAndStatus(String scheduleId, ScheduleStatus status);

    List<ScheduleEntity> findByScheduleIdContainingIgnoreCase(String scheduleIdPart);
}
