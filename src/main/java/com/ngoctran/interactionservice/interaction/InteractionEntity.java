package com.ngoctran.interactionservice.interaction;

import com.ngoctran.interactionservice.cases.CaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Interaction Instance - represents a user's journey/session
 * 
 * Relationship: Many Interactions can belong to ONE Case
 * - Multiple journeys for the same case (e.g., onboarding, update, add document)
 * - Multi-channel interactions (web, mobile, call center)
 * - Resume/retry scenarios
 */
@Entity
@Table(name = "flw_int")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionEntity {
    
    @Id
    private String id;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "interaction_definition_key", length = 255)
    private String interactionDefinitionKey;
    
    @Column(name = "interaction_definition_version")
    private Long interactionDefinitionVersion;

    @Column(name = "case_definition_key", length = 255)
    private String caseDefinitionKey;
    
    @Column(name = "case_definition_version")
    private Long caseDefinitionVersion;

    /**
     * Foreign key to flow_case.id
     * Many interactions can belong to one case (1:N relationship)
     */
    @Column(name = "case_id", length = 36)
    private String caseId;
    
    /**
     * Optional: JPA relationship to CaseEntity
     * Uncomment if you want to use JPA navigation
     */
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "case_id", insertable = false, updatable = false)
    // private CaseEntity caseEntity;

    @Column(name = "case_version")
    private Long caseVersion;

    /**
     * Current step in the journey (CURRENT POSITION)
     */
    @Column(name = "step_name", length = 255)
    private String stepName;
    
    @Column(name = "step_status", length = 20)
    private String stepStatus;
    
    /**
     * Overall interaction status
     * Values: ACTIVE, WAITING_SYSTEM, COMPLETED, FAILED, CANCELLED
     */
    @Column(name = "status", length = 20)
    private String status;
    
    @Column(name = "resumable")
    private Boolean resumable;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = java.time.Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.Instant.now();
    }

    /**
     * Temporary data for current step
     */
    @Column(name = "temp_data", columnDefinition = "jsonb")
    private String tempData;

    // Helper method to get case ID as UUID
    public UUID getCaseIdAsUUID() {
        return caseId != null ? UUID.fromString(caseId) : null;
    }
    
    // Helper method to set case ID from UUID
    public void setCaseIdFromUUID(UUID uuid) {
        this.caseId = uuid != null ? uuid.toString() : null;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public void setStepStatus(String stepStatus) {
        this.stepStatus = stepStatus;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
