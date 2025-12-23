package com.ngoctran.interactionservice.cases;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.dto.NextStepResponse;
import com.ngoctran.interactionservice.dto.StepSubmissionDto;
import com.ngoctran.interactionservice.mapping.ProcessMappingRepository;
import com.ngoctran.interactionservice.workflow.TemporalWorkflowService;
import com.ngoctran.interactionservice.task.TaskRepository;
import com.ngoctran.interactionservice.task.TaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepo;
    private final ObjectMapper objectMapper;
    private final TemporalWorkflowService temporalWorkflowService;
    private final ProcessMappingRepository processMappingRepo;
    private final TaskRepository taskRepo;

    @Transactional
    public UUID createCase(Map<String, Object> initialData) {
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setStatus("ACTIVE");

        if (initialData != null) {
            if (initialData.containsKey("customerId")) {
                caseEntity.setCustomerId(String.valueOf(initialData.get("customerId")));
            }
            if (initialData.containsKey("caseDefinitionKey")) {
                caseEntity.setCaseDefinitionKey(String.valueOf(initialData.get("caseDefinitionKey")));
            }
            caseEntity.setCaseData(toJson(initialData));
        }

        caseEntity = caseRepo.save(caseEntity);
        log.info("Created new case with ID: {} for customer: {}", caseEntity.getId(), caseEntity.getCustomerId());
        return caseEntity.getId();
    }

    @Transactional(readOnly = true)
    public CaseEntity getCase(UUID caseId) {
        return caseRepo.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
    }

    @Transactional(readOnly = true)
    public List<CaseEntity> listCases(String customerId, String status) {
        if (customerId != null && status != null) {
            return caseRepo.findByCustomerIdAndStatus(customerId, status);
        } else if (customerId != null) {
            return caseRepo.findByCustomerId(customerId);
        } else if (status != null) {
            return caseRepo.findByStatus(status);
        } else {
            return caseRepo.findAll();
        }
    }

    @Transactional(readOnly = true)
    public List<TaskEntity> getTasksByCase(UUID caseId) {
        return taskRepo.findByCaseId(caseId);
    }

    @Transactional
    public void cancelCase(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setStatus("CANCELLED");

        // Cancel Temporal workflow if exists
        String workflowInstanceId = caseEntity.getWorkflowInstanceId();
        if (workflowInstanceId != null) {
            try {
                String simpleWorkflowId = workflowInstanceId.contains(":") ? workflowInstanceId.split(":")[0]
                        : workflowInstanceId;
                temporalWorkflowService.cancelWorkflow(simpleWorkflowId);
                log.info("Cancelled Temporal workflow {} for case {}", simpleWorkflowId, caseId);
            } catch (Exception e) {
                log.warn("Failed to cancel workflow for case {}: {}", caseId, e.getMessage());
            }
        }

        caseRepo.save(caseEntity);
        log.info("Case {} cancelled", caseId);
    }

    @Transactional
    public NextStepResponse submitStep(UUID caseId, StepSubmissionDto submission) {
        log.info("Submitting step for case {}: {}", caseId, submission.stepName());
        CaseEntity caseEntity = getCase(caseId);

        // 1. Update Case Data (Merge)
        Map<String, Object> caseData = parseCaseData(caseEntity.getCaseData());
        if (submission.stepData() != null) {
            caseData.putAll(submission.stepData());
        }
        caseEntity.setCaseData(toJson(caseData));

        // 2. Add to Audit Trail (History)
        updateAuditTrail(caseEntity, submission);

        // 3. Update Current Step
        caseEntity.setCurrentStep(submission.stepName());

        // 4. Find and Handle Workflow Signals (Temporal)
        if (caseEntity.getWorkflowInstanceId() == null) {
            processMappingRepo.findRunningProcessesByCaseId(caseId)
                    .stream().findFirst()
                    .ifPresent(p -> caseEntity.setWorkflowInstanceId(p.getProcessInstanceId()));
        }

        handleWorkflowSignals(caseEntity, submission);

        caseRepo.save(caseEntity);

        // 5. Determine Response (Next Step context)
        String nextStep = "COMPLETED";
        Map<String, Object> uiModel = new HashMap<>();

        if (caseEntity.getWorkflowInstanceId() != null) {
            try {
                String workflowId = caseEntity.getWorkflowInstanceId();
                String simpleWorkflowId = workflowId.contains(":") ? workflowId.split(":")[0] : workflowId;
                var progress = temporalWorkflowService.queryWorkflowProgress(simpleWorkflowId);
                if (progress != null) {
                    nextStep = progress.getCurrentStep();
                    uiModel.put("percentComplete", progress.getPercentComplete());
                }
            } catch (Exception e) {
                log.warn("Could not query workflow progress for case {}: {}", caseId, e.getMessage());
            }
        }

        return new NextStepResponse(nextStep, uiModel, caseEntity.getStatus());
    }

    @SuppressWarnings("unchecked")
    private void handleWorkflowSignals(CaseEntity caseEntity, StepSubmissionDto submission) {
        String workflowId = caseEntity.getWorkflowInstanceId();
        if (workflowId == null)
            return;

        String signalWorkflowId = workflowId.contains(":") ? workflowId.split(":")[0] : workflowId;
        String stepName = submission.stepName();
        Map<String, Object> data = submission.stepData();

        try {
            if ("document-upload".equalsIgnoreCase(stepName)) {
                // temporalWorkflowService.signalDocumentsUploaded(signalWorkflowId,
                // (Map<String, String>) data);
            } else if ("personal-info".equalsIgnoreCase(stepName)) {
                temporalWorkflowService.signalUserDataUpdated(signalWorkflowId, data);
            } else if ("manual-review".equalsIgnoreCase(stepName)) {
                boolean approved = Boolean.parseBoolean(String.valueOf(data.getOrDefault("approved", "false")));
                String reason = String.valueOf(data.getOrDefault("reason", ""));
                temporalWorkflowService.signalManualReview(signalWorkflowId, approved, reason);
            }
        } catch (Exception e) {
            log.error("Failed to signal workflow for case {}: {}", caseEntity.getId(), e.getMessage());
        }
    }

    private void updateAuditTrail(CaseEntity caseEntity, StepSubmissionDto submission) {
        try {
            Map<String, Object> auditTrail = new HashMap<>();
            if (caseEntity.getAuditTrail() != null && !caseEntity.getAuditTrail().isEmpty()) {
                auditTrail = objectMapper.readValue(caseEntity.getAuditTrail(),
                        new TypeReference<Map<String, Object>>() {
                        });
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) auditTrail.getOrDefault("steps",
                    new ArrayList<>());

            Map<String, Object> entry = new HashMap<>();
            entry.put("stepName", submission.stepName());
            entry.put("submittedAt", Instant.now().toString());
            entry.put("data", submission.stepData());
            entry.put("clientContext", submission.clientContext());

            steps.add(entry);
            auditTrail.put("steps", steps);
            auditTrail.put("lastUpdated", Instant.now().toString());

            caseEntity.setAuditTrail(objectMapper.writeValueAsString(auditTrail));
        } catch (Exception e) {
            log.error("Failed to update audit trail for case {}", caseEntity.getId(), e);
        }
    }

    private Map<String, Object> parseCaseData(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse case data", e);
            return new HashMap<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}