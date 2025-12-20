package com.ngoctran.interactionservice.interaction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.StepSubmissionDto;
import com.ngoctran.interactionservice.cases.CaseEntity;
import com.ngoctran.interactionservice.cases.CaseRepository;
import com.ngoctran.interactionservice.interaction.*;
import com.ngoctran.interactionservice.interaction.dto.StepDefinition;
import com.ngoctran.interactionservice.interaction.dto.StepResponse;
import io.temporal.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final InteractionDefinitionRepository intDefRepo;
    private final InteractionRepository intRepo;
    private final CaseRepository caseRepo;
    private final WorkflowClient workflowClient;
    private final ObjectMapper mapper;
    private final StepNavigationService stepNavigationService;


    @Transactional
    public InteractionDto startInteraction(InteractionStartRequest req) {
        log.info("Starting interaction: key={}, version={}", req.interactionDefinitionKey(), req.interactionDefinitionVersion());

        // 1. Load Definition
        InteractionDefinitionEntity def = intDefRepo.findByInteractionDefinitionKeyAndInteractionDefinitionVersion(
                req.interactionDefinitionKey(),
                req.interactionDefinitionVersion()
        ).orElseThrow(() -> new RuntimeException("Interaction Definition not found"));

        // 2. Create a new Case
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setCaseDefinitionKey(def.getInteractionDefinitionKey());
        caseEntity.setStatus("ACTIVE");
        caseEntity = caseRepo.save(caseEntity);

        // 3. Create Interaction Instance
        InteractionEntity interaction = new InteractionEntity();
        interaction.setId(UUID.randomUUID().toString());
        interaction.setUserId(req.userId());
        interaction.setInteractionDefinitionKey(req.interactionDefinitionKey());
        interaction.setInteractionDefinitionVersion(req.interactionDefinitionVersion());
        interaction.setCaseId(caseEntity.getId().toString());

        // Find initial step from blueprint
        List<StepDefinition> steps = loadSteps(def.getSteps());
        if (!steps.isEmpty()) {
            interaction.setStepName(steps.get(0).getName());
            interaction.setStepStatus("PENDING");
        }

        interaction.setStatus("ACTIVE");
        interaction.setResumable(true);

        interaction = intRepo.save(interaction);

        return mapToDto(interaction);
    }

    @Transactional
    public InteractionDto submitStep(String interactionId, StepSubmissionDto dto) {
        log.info("Submitting step for interaction {}: {}", interactionId, dto.stepName());

        // Delegate to StepNavigationService for the heavy lifting
        stepNavigationService.submitStep(interactionId, dto.stepName(), dto.stepData());

        // Load the updated interaction
        InteractionEntity interaction = intRepo.findById(interactionId)
                .orElseThrow(() -> new RuntimeException("Interaction not found"));

        return mapToDto(interaction);
    }

    private List<StepDefinition> loadSteps(String stepsJson) {
        try {
            return mapper.readValue(stepsJson, new TypeReference<List<StepDefinition>>() {});
        } catch (Exception e) {
            log.error("Failed to parse steps JSON", e);
            return List.of();
        }
    }

    private InteractionDto mapToDto(InteractionEntity entity) {
        return new InteractionDto(
                entity.getId(),
                entity.getStatus(),
                entity.getStepName(),
                entity.getStepStatus(),
                entity.getResumable(),
                readJson(entity.getTempData()),
                Map.of()
        );
    }

    private Map<String, Object> readJson(String json) {
        try {
            return json == null ? Map.of() : mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

