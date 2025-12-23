package com.ngoctran.interactionservice.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByCaseId(UUID caseId);

    List<TaskEntity> findByAssigneeIdAndStatus(String assigneeId, TaskStatus status);
}
