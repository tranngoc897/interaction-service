package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.WorkflowEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, Long> {

    List<WorkflowEvent> findByInstanceIdOrderBySequenceNumberAsc(UUID instanceId);

    @Query("SELECT COALESCE(MAX(e.sequenceNumber), 0) FROM WorkflowEvent e WHERE e.instanceId = :instanceId")
    Integer findMaxSequenceNumber(@Param("instanceId") UUID instanceId);
}
