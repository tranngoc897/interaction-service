package com.ngoctran.interactionservice.cases;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.dto.NextStepResponse;
import com.ngoctran.interactionservice.dto.StepSubmissionDto;
import com.ngoctran.interactionservice.mapping.ProcessMappingRepository;
import com.ngoctran.interactionservice.bpmn.BpmnProcessService;
import com.ngoctran.interactionservice.compliance.ComplianceService;
import com.ngoctran.interactionservice.dmn.DmnDecisionService;
import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

@Service
public class MyCaseService {

    private static final Logger log = LoggerFactory.getLogger(MyCaseService.class);

    private final CaseRepository caseRepo;
    private final ObjectMapper objectMapper;
    private final ProcessMappingRepository processMappingRepo;
    private final BpmnProcessService bpmnProcessService;
    private final ComplianceService complianceService;
    private final DmnDecisionService dmnDecisionService;
    private final WorkflowEventPublisher eventPublisher;
    private final CaseDefinitionRepository caseDefinitionRepo;

    public MyCaseService(CaseRepository caseRepo, ObjectMapper objectMapper,
            ProcessMappingRepository processMappingRepo,
            BpmnProcessService bpmnProcessService, ComplianceService complianceService,
            DmnDecisionService dmnDecisionService, WorkflowEventPublisher eventPublisher,
            CaseDefinitionRepository caseDefinitionRepo) {
        this.caseRepo = caseRepo;
        this.objectMapper = objectMapper;
        this.processMappingRepo = processMappingRepo;

        this.bpmnProcessService = bpmnProcessService;
        this.complianceService = complianceService;
        this.dmnDecisionService = dmnDecisionService;
        this.eventPublisher = eventPublisher;
        this.caseDefinitionRepo = caseDefinitionRepo;
    }

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

        // Publish case creation event
        eventPublisher.publishCaseUpdateEvent(caseEntity.getId().toString(),
                caseEntity.getCaseDefinitionKey(),
                initialData,
                Map.of("action", "CREATE"));

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

    @Transactional
    public NextStepResponse submitStep(UUID caseId, StepSubmissionDto submission) {
        log.info("Submitting step for case {}: {}", caseId, submission.getStepName());
        CaseEntity caseEntity = getCase(caseId);

        // 1. Update Case Data (Merge)
        Map<String, Object> caseData = parseCaseData(caseEntity.getCaseData());
        if (submission.getStepData() != null) {
            caseData.putAll(submission.getStepData());
        }
        caseEntity.setCaseData(toJson(caseData));

        // 2. Add to Audit Trail (History)
        updateAuditTrail(caseEntity, submission);

        // 3. Update Current Step
        caseEntity.setCurrentStep(submission.getStepName());

        // 4. Find and Handle Workflow Signals
        if (caseEntity.getBpmnProcessId() == null) {
            processMappingRepo.findRunningProcessesByCaseId(caseId)
                    .stream().findFirst()
                    .ifPresent(p -> caseEntity.setBpmnProcessId(p.getProcessInstanceId()));
        }

        handleWorkflowSignals(caseEntity, submission);

        caseRepo.save(caseEntity);

        // Publish interaction step event
        eventPublisher.publishInteractionStepEvent(caseId.toString(),
                caseEntity.getCaseDefinitionKey(),
                submission.getStepName(),
                "SUBMIT",
                submission.getStepData());

        // 5. Determine Response (Next Step context)
        String nextStep = "COMPLETED";
        Map<String, Object> uiModel = new HashMap<>();

        if (caseEntity.getBpmnProcessId() != null) {
            try {
                String processId = caseEntity.getBpmnProcessId();
                // Query BPMN variables or state if needed
                // For now, we'll keep the current step from the entity
                nextStep = caseEntity.getCurrentStep();
            } catch (Exception e) {
                log.warn("Could not query BPMN progress for case {}: {}", caseId, e.getMessage());
            }
        }

        return new NextStepResponse(nextStep, uiModel, caseEntity.getStatus());
    }

    @SuppressWarnings("unchecked")
    private void handleWorkflowSignals(CaseEntity caseEntity, StepSubmissionDto submission) {
        String bpmnProcessId = caseEntity.getBpmnProcessId();
        if (bpmnProcessId == null)
            return;

        String stepName = submission.getStepName();
        Map<String, Object> data = submission.getStepData();

        try {
            if ("personal-info".equalsIgnoreCase(stepName)) {
                bpmnProcessService.updateVariables(bpmnProcessId, data);
            } else if ("manual-review".equalsIgnoreCase(stepName)) {
                boolean approved = Boolean.parseBoolean(String.valueOf(data.getOrDefault("approved", "false")));
                String reason = String.valueOf(data.getOrDefault("reason", ""));
                bpmnProcessService.signalProcess(bpmnProcessId, "manualReviewSignal",
                        Map.of("approved", approved, "reason", reason));
            }
        } catch (Exception e) {
            log.error("Failed to signal BPMN process for case {}: {}", caseEntity.getId(), e.getMessage());
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
            entry.put("stepName", submission.getStepName());
            entry.put("submittedAt", Instant.now().toString());
            entry.put("data", submission.getStepData());
            entry.put("clientContext", submission.getClientContext());

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

    // ===========================================
    // ABB ONBOARDING PATTERN METHODS
    // ===========================================

    /**
     * Set resume token for workflow resumability
     */
    @Transactional
    public void setResumeToken(UUID caseId, String resumeToken) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setResumeToken(resumeToken);
        caseRepo.save(caseEntity);
        log.info("Set resume token for case: {}", caseId);
    }

    /**
     * Validate resume token
     */
    public boolean validateResumeToken(UUID caseId, String resumeToken) {
        CaseEntity caseEntity = getCase(caseId);
        return resumeToken.equals(caseEntity.getResumeToken());
    }

    /**
     * Update workflow state for resumability
     */
    @Transactional
    public void updateWorkflowState(UUID caseId, Map<String, Object> workflowState) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setWorkflowState(toJson(workflowState));
        caseRepo.save(caseEntity);
        log.info("Updated workflow state for case: {}", caseId);

        // Publish workflow state event
        eventPublisher.publishWorkflowStateEvent(caseId.toString(),
                "CASE_WORKFLOW",
                "UPDATING",
                "UPDATED",
                workflowState);
    }

    /**
     * Get workflow state
     */
    public Map<String, Object> getWorkflowState(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        return parseCaseData(caseEntity.getWorkflowState());
    }

    /**
     * Update epic data (milestones and progress tracking)
     */
    @Transactional
    public void updateEpicData(UUID caseId, Map<String, Object> epicData) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setEpicData(toJson(epicData));
        caseRepo.save(caseEntity);
        log.info("Updated epic data for case: {}", caseId);
    }

    /**
     * Get epic data
     */
    public Map<String, Object> getEpicData(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        return parseCaseData(caseEntity.getEpicData());
    }

    /**
     * Update compliance status
     */
    @Transactional
    public void updateComplianceStatus(UUID caseId, Map<String, Object> complianceStatus) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setComplianceStatus(toJson(complianceStatus));
        caseRepo.save(caseEntity);
        log.info("Updated compliance status for case: {}", caseId);
    }

    /**
     * Get compliance status
     */
    public Map<String, Object> getComplianceStatus(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        return parseCaseData(caseEntity.getComplianceStatus());
    }

    /**
     * Update joint account data
     */
    @Transactional
    public void updateJointAccountData(UUID caseId, Map<String, Object> jointAccountData) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setJointAccountData(toJson(jointAccountData));
        caseRepo.save(caseEntity);
        log.info("Updated joint account data for case: {}", caseId);
    }

    /**
     * Get joint account data
     */
    public Map<String, Object> getJointAccountData(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        return parseCaseData(caseEntity.getJointAccountData());
    }

    /**
     * Set BPMN process ID
     */
    @Transactional
    public void setBpmnProcessId(UUID caseId, String bpmnProcessId) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setBpmnProcessId(bpmnProcessId);
        caseRepo.save(caseEntity);
        log.info("Set BPMN process ID for case: {} -> {}", caseId, bpmnProcessId);
    }

    /**
     * Get BPMN process ID
     */
    public String getBpmnProcessId(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        return caseEntity.getBpmnProcessId();
    }

    /**
     * Pause workflow (set expiration for cleanup)
     */
    @Transactional
    public void pauseWorkflow(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setStatus("PAUSED");
        caseEntity.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days
        caseRepo.save(caseEntity);
        log.info("Paused workflow for case: {}", caseId);
    }

    /**
     * Resume workflow
     */
    @Transactional
    public void resumeWorkflow(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        caseEntity.setStatus("ACTIVE");
        caseEntity.setExpiresAt(null); // Remove expiration
        caseRepo.save(caseEntity);
        log.info("Resumed workflow for case: {}", caseId);
    }

    /**
     * Check if workflow can be resumed
     */
    public boolean canResumeWorkflow(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        return "PAUSED".equals(caseEntity.getStatus()) &&
                (caseEntity.getExpiresAt() == null || caseEntity.getExpiresAt().isAfter(Instant.now()));
    }

    /**
     * Add milestone to epic data
     */
    @Transactional
    public void addMilestone(UUID caseId, String epicKey, String milestoneKey, String name, String status) {
        Map<String, Object> epicData = getEpicData(caseId);
        if (epicData == null) {
            epicData = new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> epics = (Map<String, Object>) epicData.getOrDefault("epics", new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> epic = (Map<String, Object>) epics.getOrDefault(epicKey, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) epic.getOrDefault("milestones", new HashMap<>());

        Map<String, Object> milestone = new HashMap<>();
        milestone.put("name", name);
        milestone.put("status", status);
        milestone.put("startedAt", LocalDateTime.now());
        if ("COMPLETED".equals(status)) {
            milestone.put("completedAt", LocalDateTime.now());
        }

        milestones.put(milestoneKey, milestone);
        epic.put("milestones", milestones);
        epics.put(epicKey, epic);
        epicData.put("epics", epics);

        updateEpicData(caseId, epicData);
        log.info("Added milestone {} to epic {} for case {}", milestoneKey, epicKey, caseId);

        // Publish milestone event
        eventPublisher.publishMilestoneEvent(caseId.toString(),
                milestoneKey,
                "STARTED".equals(status) ? "MILESTONE_STARTED" : "MILESTONE_REACHED",
                Map.of("epicKey", epicKey, "name", name, "status", status));
    }

    /**
     * Complete milestone
     */
    @Transactional
    public void completeMilestone(UUID caseId, String epicKey, String milestoneKey) {
        Map<String, Object> epicData = getEpicData(caseId);
        if (epicData == null)
            return;

        @SuppressWarnings("unchecked")
        Map<String, Object> epics = (Map<String, Object>) epicData.getOrDefault("epics", new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> epic = (Map<String, Object>) epics.getOrDefault(epicKey, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) epic.getOrDefault("milestones", new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> milestone = (Map<String, Object>) milestones.get(milestoneKey);

        if (milestone != null) {
            milestone.put("status", "COMPLETED");
            milestone.put("completedAt", LocalDateTime.now());
            milestones.put(milestoneKey, milestone);
            epic.put("milestones", milestones);
            epics.put(epicKey, epic);
            epicData.put("epics", epics);
            updateEpicData(caseId, epicData);
            log.info("Completed milestone {} in epic {} for case {}", milestoneKey, epicKey, caseId);

            // Publish milestone event
            eventPublisher.publishMilestoneEvent(caseId.toString(),
                    milestoneKey,
                    "MILESTONE_COMPLETED",
                    Map.of("epicKey", epicKey, "status", "COMPLETED"));
        }
    }

    /**
     * Get milestone status
     */
    public String getMilestoneStatus(UUID caseId, String epicKey, String milestoneKey) {
        Map<String, Object> epicData = getEpicData(caseId);
        if (epicData == null)
            return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> epics = (Map<String, Object>) epicData.getOrDefault("epics", new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> epic = (Map<String, Object>) epics.getOrDefault(epicKey, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> milestones = (Map<String, Object>) epic.getOrDefault("milestones", new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> milestone = (Map<String, Object>) milestones.get(milestoneKey);

        return milestone != null ? (String) milestone.get("status") : null;
    }

    /**
     * Add compliance check result
     */
    @Transactional
    public void addComplianceCheck(UUID caseId, String checkType, String status, Map<String, Object> checkResult) {
        Map<String, Object> complianceStatus = getComplianceStatus(caseId);
        if (complianceStatus == null) {
            complianceStatus = new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) complianceStatus.getOrDefault("checks",
                new ArrayList<>());

        Map<String, Object> check = new HashMap<>();
        check.put("checkType", checkType);
        check.put("status", status);
        check.put("checkResult", checkResult);
        check.put("checkedAt", LocalDateTime.now());

        checks.add(check);
        complianceStatus.put("checks", checks);

        // Update overall compliance status
        boolean allPassed = checks.stream().allMatch(c -> "PASSED".equals(c.get("status")));
        complianceStatus.put("overallStatus", allPassed ? "PASSED" : "REVIEW_NEEDED");

        updateComplianceStatus(caseId, complianceStatus);
        log.info("Added compliance check {} with status {} for case {}", checkType, status, caseId);

        // Publish compliance event
        eventPublisher.publishComplianceEvent(caseId.toString(),
                "UNKNOWN", // applicantId might not be available here directly from call
                checkType,
                status,
                checkResult);
    }

    /**
     * Get overall compliance status
     */
    public String getOverallComplianceStatus(UUID caseId) {
        Map<String, Object> complianceStatus = getComplianceStatus(caseId);
        if (complianceStatus == null)
            return "UNKNOWN";
        return (String) complianceStatus.getOrDefault("overallStatus", "UNKNOWN");
    }

    // ===========================================
    // BPMN PROCESS INTEGRATION METHODS
    // ===========================================

    /**
     * Start BPMN process for case
     */
    @Transactional
    public void startBpmnProcess(UUID caseId, String processDefinitionKey, Map<String, Object> variables) {
        CaseEntity caseEntity = getCase(caseId);

        try {
            // Add case data to variables
            if (variables == null) {
                variables = new HashMap<>();
            }
            variables.put("caseId", caseId.toString());
            variables.put("customerId", caseEntity.getCustomerId());

            // Start BPMN process
            var processInstance = bpmnProcessService.startProcess(processDefinitionKey, caseId.toString(), variables);

            // Update case with BPMN process ID
            caseEntity.setBpmnProcessId(processInstance.id);
            caseRepo.save(caseEntity);

            log.info("Started BPMN process {} for case {}", processInstance.id, caseId);

            // Publish workflow state event
            eventPublisher.publishWorkflowStateEvent(processInstance.id,
                    processDefinitionKey,
                    "NONE",
                    "STARTED",
                    variables);
        } catch (Exception e) {
            log.error("Failed to start BPMN process for case {}: {}", caseId, e.getMessage());
            throw new RuntimeException("BPMN process start failed: " + e.getMessage(), e);
        }
    }

    /**
     * Signal BPMN process for case
     */
    @Transactional
    public void signalBpmnProcess(UUID caseId, String signalName, Map<String, Object> signalData) {
        CaseEntity caseEntity = getCase(caseId);
        String bpmnProcessId = caseEntity.getBpmnProcessId();

        if (bpmnProcessId == null) {
            log.warn("No BPMN process found for case {}", caseId);
            return;
        }

        try {
            bpmnProcessService.signalProcess(bpmnProcessId, signalName, signalData);
            log.info("Signaled BPMN process {} for case {} with signal {}", bpmnProcessId, caseId, signalName);
        } catch (Exception e) {
            log.error("Failed to signal BPMN process for case {}: {}", caseId, e.getMessage());
            throw new RuntimeException("BPMN process signal failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update BPMN process variables
     */
    @Transactional
    public void updateBpmnVariables(UUID caseId, Map<String, Object> variables) {
        CaseEntity caseEntity = getCase(caseId);
        String bpmnProcessId = caseEntity.getBpmnProcessId();

        if (bpmnProcessId == null) {
            log.warn("No BPMN process found for case {}", caseId);
            return;
        }

        try {
            bpmnProcessService.updateVariables(bpmnProcessId, variables);
            log.info("Updated BPMN variables for case {}", caseId);
        } catch (Exception e) {
            log.error("Failed to update BPMN variables for case {}: {}", caseId, e.getMessage());
            throw new RuntimeException("BPMN variables update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get BPMN process variables
     */
    public Map<String, Object> getBpmnVariables(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        String bpmnProcessId = caseEntity.getBpmnProcessId();

        if (bpmnProcessId == null) {
            log.warn("No BPMN process found for case {}", caseId);
            return new HashMap<>();
        }

        try {
            return bpmnProcessService.getVariables(bpmnProcessId);
        } catch (Exception e) {
            log.error("Failed to get BPMN variables for case {}: {}", caseId, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Check if BPMN process is active
     */
    public boolean isBpmnProcessActive(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        String bpmnProcessId = caseEntity.getBpmnProcessId();

        if (bpmnProcessId == null) {
            return false;
        }

        try {
            return bpmnProcessService.isProcessActive(caseId.toString());
        } catch (Exception e) {
            log.error("Failed to check BPMN process status for case {}: {}", caseId, e.getMessage());
            return false;
        }
    }

    /**
     * Suspend BPMN process
     */
    @Transactional
    public void suspendBpmnProcess(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        String bpmnProcessId = caseEntity.getBpmnProcessId();

        if (bpmnProcessId == null) {
            log.warn("No BPMN process found for case {}", caseId);
            return;
        }

        try {
            bpmnProcessService.suspendProcess(bpmnProcessId);
            log.info("Suspended BPMN process {} for case {}", bpmnProcessId, caseId);
        } catch (Exception e) {
            log.error("Failed to suspend BPMN process for case {}: {}", caseId, e.getMessage());
            throw new RuntimeException("BPMN process suspend failed: " + e.getMessage(), e);
        }
    }

    /**
     * Activate BPMN process
     */
    @Transactional
    public void activateBpmnProcess(UUID caseId) {
        CaseEntity caseEntity = getCase(caseId);
        String bpmnProcessId = caseEntity.getBpmnProcessId();

        if (bpmnProcessId == null) {
            log.warn("No BPMN process found for case {}", caseId);
            return;
        }

        try {
            bpmnProcessService.activateProcess(bpmnProcessId);
            log.info("Activated BPMN process {} for case {}", bpmnProcessId, caseId);
        } catch (Exception e) {
            log.error("Failed to activate BPMN process for case {}: {}", caseId, e.getMessage());
            throw new RuntimeException("BPMN process activate failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete BPMN process
     */
    @Transactional
    public void deleteBpmnProcess(UUID caseId, String reason) {
        CaseEntity caseEntity = getCase(caseId);
        String bpmnProcessId = caseEntity.getBpmnProcessId();

        if (bpmnProcessId == null) {
            log.warn("No BPMN process found for case {}", caseId);
            return;
        }

        try {
            bpmnProcessService.deleteProcessInstance(bpmnProcessId, reason);
            caseEntity.setBpmnProcessId(null);
            caseRepo.save(caseEntity);
            log.info("Deleted BPMN process {} for case {}", bpmnProcessId, caseId);
        } catch (Exception e) {
            log.error("Failed to delete BPMN process for case {}: {}", caseId, e.getMessage());
            throw new RuntimeException("BPMN process delete failed: " + e.getMessage(), e);
        }
    }

    // ===========================================
    // COMPLIANCE & DMN INTEGRATION METHODS
    // ===========================================

    /**
     * Perform compliance check for case using ComplianceService
     */
    @Transactional
    public void performComplianceCheck(UUID caseId, String applicantId) {
        log.info("Performing compliance check for case: {}, applicant: {}", caseId, applicantId);

        CaseEntity caseEntity = getCase(caseId);
        Map<String, Object> applicantData = parseCaseData(caseEntity.getCaseData());

        try {
            // Perform AML screening
            var amlResult = complianceService.performAmlScreening(caseId.toString(), applicantId, applicantData);

            // Perform KYC verification
            var kycResult = complianceService.performKycVerification(caseId.toString(), applicantId, applicantData);

            // Perform sanctions screening
            var sanctionsResult = complianceService.performSanctionsScreening(caseId.toString(), applicantId,
                    applicantData);

            // Update compliance status
            Map<String, Object> complianceStatus = Map.of(
                    "amlStatus", amlResult.getStatus(),
                    "kycStatus", kycResult.getStatus(),
                    "sanctionsStatus", sanctionsResult.getStatus(),
                    "overallStatus", determineOverallComplianceStatus(amlResult, kycResult, sanctionsResult),
                    "lastChecked", LocalDateTime.now());

            updateComplianceStatus(caseId, complianceStatus);
            log.info("Compliance check completed for case {}", caseId);

        } catch (Exception e) {
            log.error("Compliance check failed for case {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Compliance check failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get product recommendations using DMN
     */
    public List<String> getProductRecommendations(UUID caseId) {
        log.info("Getting product recommendations for case: {}", caseId);

        CaseEntity caseEntity = getCase(caseId);
        Map<String, Object> applicantData = parseCaseData(caseEntity.getCaseData());

        try {
            return dmnDecisionService.recommendProducts(applicantData);
        } catch (Exception e) {
            log.warn("Product recommendation failed for case {}: {}", caseId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Check eligibility using DMN
     */
    public boolean checkEligibility(UUID caseId) {
        log.info("Checking eligibility for case: {}", caseId);

        CaseEntity caseEntity = getCase(caseId);
        Map<String, Object> applicantData = parseCaseData(caseEntity.getCaseData());

        try {
            return dmnDecisionService.checkEligibility(applicantData);
        } catch (Exception e) {
            log.warn("Eligibility check failed for case {}: {}", caseId, e.getMessage());
            return false;
        }
    }

    /**
     * Assess AML risk using DMN
     */
    public String assessAmlRisk(UUID caseId) {
        log.info("Assessing AML risk for case: {}", caseId);

        CaseEntity caseEntity = getCase(caseId);
        Map<String, Object> applicantData = parseCaseData(caseEntity.getCaseData());

        try {
            return dmnDecisionService.assessAmlRisk(applicantData);
        } catch (Exception e) {
            log.warn("AML risk assessment failed for case {}: {}", caseId, e.getMessage());
            return "MEDIUM";
        }
    }

    /**
     * Perform compliance check using DMN
     */
    public Map<String, Object> performComplianceCheckWithDmn(UUID caseId) {
        log.info("Performing compliance check with DMN for case: {}", caseId);

        CaseEntity caseEntity = getCase(caseId);
        Map<String, Object> applicantData = parseCaseData(caseEntity.getCaseData());

        try {
            return dmnDecisionService.performComplianceCheck(applicantData);
        } catch (Exception e) {
            log.warn("DMN compliance check failed for case {}: {}", caseId, e.getMessage());
            return Map.of("compliant", false, "reviewRequired", true);
        }
    }

    private String determineOverallComplianceStatus(Object amlResult, Object kycResult, Object sanctionsResult) {
        // Simple logic - in real implementation, use proper status checking
        return "PASSED"; // Default to passed for now
    }

    /**
     * Get the JSON Schema for a specific case definition for dynamic forms
     */
    public String getCaseSchema(String definitionKey) {
        log.info("Getting schema for case definition: {}", definitionKey);
        return caseDefinitionRepo.findById(definitionKey)
                .map(CaseDefinitionEntity::getCaseSchema)
                .orElseThrow(() -> new RuntimeException("Case definition not found: " + definitionKey));
    }

    /**
     * Migrate process instances from an old BPMN version to a new one
     */
    @Transactional
    public Map<String, Object> migrateBpmVersion(String sourceDefId, String targetDefId, List<String> caseIds) {
        log.info("Migrating BPMN version for {} cases: {} -> {}", caseIds.size(), sourceDefId, targetDefId);

        List<String> processInstanceIds = new ArrayList<>();
        for (String caseId : caseIds) {
            caseRepo.findById(UUID.fromString(caseId)).ifPresent(entity -> {
                if (entity.getWorkflowInstanceId() != null) {
                    processInstanceIds.add(entity.getWorkflowInstanceId());
                }
            });
        }

        if (processInstanceIds.isEmpty()) {
            return Map.of("status", "SKIPPED", "message", "No active process instances found for provided cases");
        }

        // 1. Generate plan
        Map<String, Object> plan = bpmnProcessService.generateMigrationPlan(sourceDefId, targetDefId);

        // 2. Execute plan
        bpmnProcessService.executeMigrationPlan(plan, processInstanceIds);

        // 3. Update local mapping if needed (optional)

        return Map.of("status", "SUCCESS", "migratedInstances", processInstanceIds.size(), "targetDefId", targetDefId);
    }
}
