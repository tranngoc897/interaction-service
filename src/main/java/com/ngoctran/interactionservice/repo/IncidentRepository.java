package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByInstanceId(UUID instanceId);

    List<Incident> findByStatus(String status);

    List<Incident> findBySeverity(String severity);

    List<Incident> findByOwner(String owner);

    @Query("SELECT i FROM Incident i WHERE i.status IN ('OPEN', 'ACKNOWLEDGED') AND i.severity IN ('HIGH', 'CRITICAL')")
    List<Incident> findHighSeverityOpenIncidents();

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.severity = :severity AND i.status != 'RESOLVED'")
    long countBySeverityAndNotResolved(@Param("severity") String severity);

    @Query("SELECT i FROM Incident i WHERE i.createdAt >= :since ORDER BY i.createdAt DESC")
    List<Incident> findRecentIncidents(@Param("since") Instant since);
}
