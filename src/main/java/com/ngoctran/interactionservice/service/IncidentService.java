package com.ngoctran.interactionservice.service;

import com.ngoctran.interactionservice.domain.Incident;
import com.ngoctran.interactionservice.repo.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;

    /**
     * Create a new incident
     */
    @Transactional
    public Incident createIncident(UUID instanceId, String state, String errorCode,
                                 String severity, String description) {
        Incident incident = Incident.builder()
                .incidentId(UUID.randomUUID())
                .instanceId(instanceId)
                .state(state)
                .errorCode(errorCode)
                .severity(severity)
                .status("OPEN")
                .description(description)
                .createdAt(Instant.now())
                .build();

        Incident saved = incidentRepository.save(incident);
        log.info("Created incident {} for instance {} state {} error {}",
                saved.getIncidentId(), instanceId, state, errorCode);
        return saved;
    }

    /**
     * Acknowledge an incident
     */
    @Transactional
    public Incident acknowledgeIncident(UUID incidentId, String owner) {
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident == null) {
            throw new IllegalArgumentException("Incident not found: " + incidentId);
        }

        if (!incident.isOpen()) {
            throw new IllegalStateException("Incident is not open: " + incidentId);
        }

        incident.acknowledge(owner);
        Incident saved = incidentRepository.save(incident);
        log.info("Incident {} acknowledged by {}", incidentId, owner);
        return saved;
    }

    /**
     * Resolve an incident
     */
    @Transactional
    public Incident resolveIncident(UUID incidentId, String resolution) {
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident == null) {
            throw new IllegalArgumentException("Incident not found: " + incidentId);
        }

        if (!incident.isAcknowledged()) {
            throw new IllegalStateException("Incident must be acknowledged before resolution: " + incidentId);
        }

        incident.resolve(resolution);
        Incident saved = incidentRepository.save(incident);
        log.info("Incident {} resolved with: {}", incidentId, resolution);
        return saved;
    }

    /**
     * Get incidents for an instance
     */
    public List<Incident> getIncidentsForInstance(UUID instanceId) {
        return incidentRepository.findByInstanceId(instanceId);
    }

    /**
     * Get high severity open incidents
     */
    public List<Incident> getHighSeverityIncidents() {
        return incidentRepository.findHighSeverityOpenIncidents();
    }

    /**
     * Get incident statistics
     */
    public Map<String, Object> getIncidentStatistics() {
        return Map.of(
                "totalOpen", incidentRepository.countByStatus("OPEN"),
                "totalAcknowledged", incidentRepository.countByStatus("ACKNOWLEDGED"),
                "totalResolved", incidentRepository.countByStatus("RESOLVED"),
                "highSeverityOpen", incidentRepository.countBySeverityAndNotResolved("HIGH"),
                "criticalSeverityOpen", incidentRepository.countBySeverityAndNotResolved("CRITICAL"),
                "recentIncidents", incidentRepository.findRecentIncidents(
                        Instant.now().minusSeconds(3600)) // Last hour
        );
    }

    /**
     * Auto-create incident for system errors
     */
    public void createSystemIncident(UUID instanceId, String state, String errorCode, String description) {
        String severity = determineSeverity(errorCode);
        createIncident(instanceId, state, errorCode, severity, description);
    }

    private String determineSeverity(String errorCode) {
        if (errorCode.startsWith("SYSTEM_") || errorCode.contains("EXCEPTION")) {
            return "CRITICAL";
        } else if (errorCode.startsWith("TIMEOUT") || errorCode.startsWith("RETRY")) {
            return "HIGH";
        } else if (errorCode.startsWith("VALIDATION")) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
