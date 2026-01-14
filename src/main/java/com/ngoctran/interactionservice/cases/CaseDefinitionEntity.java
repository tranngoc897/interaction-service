package com.ngoctran.interactionservice.cases;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "flw_case_def")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseDefinitionEntity {

    @Id
    private String caseDefinitionKey;

    private Long caseDefinitionVersion;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String defaultValue;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String caseSchema;

    // Onboarding Pattern Fields
    @Column(length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(length = 255)
    private String createdBy;

    @Column(length = 255)
    private String updatedBy;


    // Getters and Setters
    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }

    public Long getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    public void setCaseDefinitionVersion(Long caseDefinitionVersion) {
        this.caseDefinitionVersion = caseDefinitionVersion;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getCaseSchema() {
        return caseSchema;
    }

    public void setCaseSchema(String caseSchema) {
        this.caseSchema = caseSchema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

}
