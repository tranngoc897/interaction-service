package com.ngoctran.interactionservice.cases;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "flw_case_def")
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

}
