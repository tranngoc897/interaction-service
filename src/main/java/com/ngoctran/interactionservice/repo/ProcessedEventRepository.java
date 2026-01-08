package com.ngoctran.interactionservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository
        extends JpaRepository<com.ngoctran.interactionservice.domain.ProcessedEvent, String> {

    boolean existsByEventId(String eventId);

    @Modifying
    @Query(value = "INSERT INTO processed_event (event_id, instance_id, event_type, processed_at) " +
            "VALUES (:eventId, :instanceId, :eventType, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (event_id) DO NOTHING", nativeQuery = true)
    void saveIdempotent(@Param("eventId") String eventId,
            @Param("instanceId") UUID instanceId,
            @Param("eventType") String eventType);

    default void save(String eventId, UUID instanceId, String eventType, String actor) {
        saveIdempotent(eventId, instanceId, eventType);
    }

}
