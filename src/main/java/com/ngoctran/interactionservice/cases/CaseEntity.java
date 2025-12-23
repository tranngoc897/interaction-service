package com.ngoctran.interactionservice.cases;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flw_case", indexes = {
        @Index(name = "idx_onboarding_case_status", columnList = "status"),
        @Index(name = "idx_onboarding_case_workflow_instance", columnList = "workflow_instance_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class CaseEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "case_definition_key")
    private String caseDefinitionKey;

    @Column(name = "case_definition_version")
    private String caseDefinitionVersion;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "current_step", length = 128)
    private String currentStep;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "workflow_instance_id", length = 128)
    private String workflowInstanceId;

    @Column(name = "case_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String caseData;

    @Column(name = "audit_trail", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String auditTrail;

    @Column(name = "sla", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String sla;

    @Column(name = "created_at", columnDefinition = "timestamptz default now()")
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamptz default now()")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }

    public String getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    public void setCaseDefinitionVersion(String caseDefinitionVersion) {
        this.caseDefinitionVersion = caseDefinitionVersion;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getCaseData() {
        return caseData;
    }

    public void setCaseData(String caseData) {
        this.caseData = caseData;
    }

    public String getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(String auditTrail) {
        this.auditTrail = auditTrail;
    }

    public String getSla() {
        return sla;
    }

    public void setSla(String sla) {
        this.sla = sla;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}