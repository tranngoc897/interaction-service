package com.ngoctran.interactionservice.mapping.dto;

import com.ngoctran.interactionservice.mapping.ProcessMappingEntity;

public class ProcessMappingMapper {

    public static ProcessMappingResponse toResponse(ProcessMappingEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProcessMappingResponse.builder()
                .id(entity.getId())
                .engineType(entity.getEngineType())
                .processInstanceId(entity.getProcessInstanceId())
                .processDefinitionKey(entity.getProcessDefinitionKey())
                .businessKey(entity.getBusinessKey())
                .caseId(entity.getCaseId() != null ? entity.getCaseId().toString() : null)
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .metadata(entity.getMetadata())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
