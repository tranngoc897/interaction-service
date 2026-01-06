package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(String status);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents();

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND e.createdAt < :cutoff ORDER BY e.createdAt ASC")
    List<OutboxEvent> findOldPendingEvents(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :cutoff")
    int deletePublishedEventsOlderThan(@Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);
}
