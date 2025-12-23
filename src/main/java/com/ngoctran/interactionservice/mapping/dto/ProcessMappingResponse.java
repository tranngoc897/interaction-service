package com.ngoctran.interactionservice.mapping.dto;

import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProcessMappingResponse {
    private String id;
    private EngineType engineType;
    private String processInstanceId;
    private String processDefinitionKey;
    private String caseId;
    private String userId;
    private ProcessStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorDetails;
}
