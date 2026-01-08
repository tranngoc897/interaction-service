package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.DlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DlqEventRepository extends JpaRepository<DlqEvent, Long> {

    List<DlqEvent> findByTopic(String topic);

    List<DlqEvent> findByStatus(String status);

    @Query("SELECT d FROM DlqEvent d WHERE d.status = 'NEW' ORDER BY d.failedAt ASC")
    List<DlqEvent> findRetryableEvents();

    @Query("SELECT COUNT(d) FROM DlqEvent d WHERE d.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(d) FROM DlqEvent d WHERE d.topic = :topic AND d.status = :status")
    long countByTopicAndStatus(@Param("topic") String topic, @Param("status") String status);
}
