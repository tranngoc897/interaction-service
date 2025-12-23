package com.ngoctran.interactionservice.interaction;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "flw_int_def")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(InteractionDefinitionId.class)
public class InteractionDefinitionEntity {

    @Id
    @Column(name = "interaction_definition_key")
    private String interactionDefinitionKey;

    @Id
    @Column(name = "interaction_definition_version")
    private Long interactionDefinitionVersion;

    @Version
    private Integer version;

    private String schemaId;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String steps; // JSON array: [{ "name": "...", "next": "...", "uiSchema": {...}}]

    public String getSteps() {
        return steps;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
