package com.ngoctran.interactionservice.interaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.activity.onboarding.OnboardingWorkflow;
import com.ngoctran.interactionservice.cases.CaseEntity;
import com.ngoctran.interactionservice.cases.CaseRepository;
import com.ngoctran.interactionservice.interaction.InteractionDefinitionEntity;
import com.ngoctran.interactionservice.interaction.InteractionDefinitionId;
import com.ngoctran.interactionservice.interaction.InteractionDefinitionRepository;
import com.ngoctran.interactionservice.interaction.InteractionDto;
import com.ngoctran.interactionservice.interaction.InteractionEntity;
import com.ngoctran.interactionservice.interaction.InteractionRepository;
import com.ngoctran.interactionservice.interaction.InteractionStartRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionDefinitionRepository intDefRepo;
    private final InteractionRepository intRepo;


    private final CaseRepository caseRepo;
    private final WorkflowClient workflowClient;
    private final ObjectMapper mapper;

    private final WorkflowClient workflowClient;
    private final InteractionRepository interactionRepo;

    public InteractionService(InteractionDefinitionRepository intDefRepo, InteractionRepository intRepo, CaseRepository caseRepo, WorkflowClient workflowClient, ObjectMapper mapper, InteractionRepository interactionRepo) {
      this.intDefRepo = intDefRepo;
      this.intRepo = intRepo;
      this.caseRepo = caseRepo;
      this.workflowClient = workflowClient;
      this.mapper = mapper;
      this.interactionRepo = interactionRepo;
    }

    public InteractionEntity startOnboarding(InteractionEntity interaction, CaseData caseData) {
        // Save InteractionEntity & Case vào DB
        interaction.setStatus("WAITING_SYSTEM");
        interactionRepo.save(interaction);

        // Trigger Temporal Workflow
        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setTaskQueue("ONBOARDING_TASK_QUEUE")
            .setWorkflowId("onboarding-" + interaction.getId())
            .build();

        OnboardingWorkflow workflow = workflowClient.newWorkflowStub(OnboardingWorkflow.class, options);
        WorkflowClient.start(workflow::start, caseData.getId(), interaction.getId());

        return interaction;
    }


    public InteractionDto startInteraction(InteractionStartRequest req) {
        // Load definition
        InteractionDefinitionEntity def = intDefRepo.findById(
            new InteractionDefinitionId(req.interactionDefinitionKey(), req.interactionDefinitionVersion())
        ).orElseThrow(() -> new RuntimeException("Definition not found"));

        // Create Case
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setId(UUID.randomUUID().toString());
        caseEntity.setCaseDefinitionKey(def.getCaseDefinitionKey());
        caseEntity.setCaseDefinitionVersion(def.getCaseDefinitionVersion());
        caseEntity.setCaseData(def.getDefaultValue()); // init with default
        caseEntity.setPreliminary(true);
        caseEntity.setVersion(1L);
        caseRepo.save(caseEntity);

        // Create InteractionEntity Instance
        InteractionInstanceEntity inst = new InteractionInstanceEntity();
        inst.setId(UUID.randomUUID().toString());
        inst.setUserId(req.userId());
        inst.setInteractionDefinitionKey(def.getInteractionDefinitionKey());
        inst.setInteractionDefinitionVersion(def.getInteractionDefinitionVersion());
        inst.setCaseDefinitionKey(def.getCaseDefinitionKey());
        inst.setCaseDefinitionVersion(def.getCaseDefinitionVersion());
        inst.setCaseId(caseEntity.getId());
        inst.setCaseVersion(caseEntity.getVersion());
        inst.setStepName("start");
        inst.setStepStatus("PENDING");
        inst.setStatus("ACTIVE");
        inst.setResumable(true);
        inst.setVersion(1L);
        intRepo.save(inst);

        return toDto(inst, caseEntity);
    }

    public InteractionDto submitStep(String interactionId, StepSubmissionDto dto) {
        InteractionInstanceEntity inst = intRepo.findById(interactionId)
                .orElseThrow(() -> new RuntimeException("InteractionEntity not found"));

        CaseEntity caseEntity = caseRepo.findById(inst.getCaseId())
                .orElseThrow(() -> new RuntimeException("Case not found"));

        // Save step data temporarily
        inst.setStepName(dto.stepName());
        inst.setStepStatus("COMPLETED");
        inst.setTempData(writeJson(dto.data()));

        // Update case data (merge)
        caseEntity.setCaseData(writeJson(dto.data())); // simplified
        caseEntity.setVersion(caseEntity.getVersion() + 1);

        // Example: if step == "personal-info", trigger workflow
        if ("personal-info".equals(dto.stepName())) {
            workflowClient.startOnboardingWorkflow(interactionId, inst.getCaseId(), caseEntity.getCaseData());
            inst.setStatus("WAITING_SYSTEM");
        }

        caseRepo.save(caseEntity);
        intRepo.save(inst);
        return toDto(inst, caseEntity);
    }

    private InteractionDto toDto(InteractionInstanceEntity inst, CaseEntity caseEntity) {
        return new InteractionDto(
                inst.getId(),
                inst.getStatus(),
                inst.getStepName(),
                inst.getStepStatus(),
                inst.getResumable(),
                readJson(inst.getTempData()),
                readJson(caseEntity.getCaseData())
        );
    }

    private Map<String,Object> readJson(String json) {
        try { return json == null ? Map.of() : mapper.readValue(json, Map.class); }
        catch (Exception e) { return Map.of(); }
    }

    private String writeJson(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
