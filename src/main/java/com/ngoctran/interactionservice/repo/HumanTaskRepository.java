package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.HumanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HumanTaskRepository extends JpaRepository<HumanTask, UUID> {

    List<HumanTask> findByInstanceId(UUID instanceId);

    List<HumanTask> findByStatus(String status);

    List<HumanTask> findByAssignedRole(String assignedRole);

    List<HumanTask> findByAssignedUser(String assignedUser);

    List<HumanTask> findByTaskType(String taskType);

    @Query("SELECT t FROM HumanTask t WHERE t.status = 'OPEN' AND t.assignedRole = :role")
    List<HumanTask> findOpenTasksByRole(@Param("role") String role);

    @Query("SELECT t FROM HumanTask t WHERE t.status = 'CLAIMED' AND t.assignedUser = :userId")
    List<HumanTask> findClaimedTasksByUser(@Param("userId") String userId);

    @Query("SELECT t FROM HumanTask t WHERE t.dueAt < :now AND t.status IN ('OPEN', 'CLAIMED')")
    List<HumanTask> findOverdueTasks(@Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM HumanTask t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(t) FROM HumanTask t WHERE t.assignedRole = :role AND t.status = 'OPEN'")
    long countOpenTasksByRole(@Param("role") String role);
}
