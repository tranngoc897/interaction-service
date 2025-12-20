package com.ngoctran.interactionservice.interaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "flw_int_def")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionDefinitionEntity {

    @Id
    private String interactionDefinitionKey;
    private Long interactionDefinitionVersion;

    @Version
    private Integer version;

    private String schemaId;

    @Column(columnDefinition = "jsonb")
    private String steps; // JSON array: [{ "name": "...", "next": "...", "uiSchema": {...}}]

    public String getSteps() {
        return steps;
    }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
