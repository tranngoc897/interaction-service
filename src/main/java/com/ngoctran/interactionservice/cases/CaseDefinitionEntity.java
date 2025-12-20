package com.ngoctran.interactionservice.cases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "flw_case_def")
public class CaseDefinitionEntity {

    @Id
    private String caseDefinitionKey;

    private Long caseDefinitionVersion;

    @Column(columnDefinition = "jsonb")
    private String defaultValue;

    @Column(columnDefinition = "jsonb")
    private String caseSchema;

}
