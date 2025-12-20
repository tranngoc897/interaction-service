package com.ngoctran.interactionservice.interaction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.cases.CaseEntity;
import com.ngoctran.interactionservice.cases.CaseRepository;
import com.ngoctran.interactionservice.interaction.InteractionDefinitionEntity;
import com.ngoctran.interactionservice.interaction.InteractionDefinitionRepository;
import com.ngoctran.interactionservice.interaction.InteractionEntity;
import com.ngoctran.interactionservice.interaction.InteractionRepository;
import com.ngoctran.interactionservice.interaction.dto.*;
import com.ngoctran.interactionservice.temporal.service.TemporalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service demonstrating how to work with 3 types of "steps":
 * 1. flw_int_def.steps (JSONB) - BLUEPRINT: Step definitions/templates
 * 2. flw_int.step_name - CURRENT POSITION: Where user is now
 * 3. flow_case.audit_trail (JSONB) - HISTORY: Steps completed with data
 */
@Service
@RequiredArgsConstructor
public class StepNavigationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StepNavigationService.class);

    private final InteractionDefinitionRepository intDefRepo;
    private final InteractionRepository intRepo;
    private final CaseRepository caseRepo;
    private final ObjectMapper objectMapper;
    private final com.ngoctran.interactionservice.task.TaskService taskService;
    private final TemporalWorkflowService temporalWorkflowService;

    /**
     * Get current step information for an interaction
     * This combines:
     * - Current position from flw_int.step_name
     * - Step configuration from flw_int_def.steps (BLUEPRINT)
     * - Previous data from flow_case.audit_trail (HISTORY)
     */
    @Transactional(readOnly = true)
    public StepResponse getCurrentStep(String interactionId) {
        log.info("Getting current step for interaction: {}", interactionId);

        // 1. Load interaction instance to get CURRENT POSITION
        InteractionEntity interaction = intRepo.findById(interactionId)
                .orElseThrow(() -> new RuntimeException("Interaction not found: " + interactionId));

        String currentStepName = interaction.getStepName(); // ← CURRENT POSITION
        log.info("Current step name: {}", currentStepName);

        // 2. Load BLUEPRINT to get step configuration
        List<StepDefinition> allSteps = loadStepBlueprint(
                interaction.getInteractionDefinitionKey(),
                interaction.getInteractionDefinitionVersion()
        );

        // 3. Find current step definition in BLUEPRINT
        StepDefinition currentStepDef = allSteps.stream()
                .filter(step -> step.getName().equals(currentStepName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Step definition not found: " + currentStepName));

        log.info("Found step definition: type={}, title={}", 
                currentStepDef.getType(), currentStepDef.getTitle());

        // 4. Load case to get HISTORY and pre-filled data
        CaseEntity caseEntity = caseRepo.findById(UUID.fromString(interaction.getCaseId()))
                .orElseThrow(() -> new RuntimeException("Case not found"));

        // 5. Get step data from history (if user is resuming/editing)
        Map<String, Object> stepData = getStepDataFromHistory(caseEntity, currentStepName);

        // 6. Calculate progress
        int currentIndex = findStepIndex(allSteps, currentStepName);
        StepResponse.ProgressInfo progress = new StepResponse.ProgressInfo(
                currentIndex + 1,
                allSteps.size(),
                (int) ((currentIndex + 1) * 100.0 / allSteps.size())
        );

        // 6.5 Calculate SLA Status
        String slaStatus = calculateSLAStatus(interaction, currentStepDef);

        // 7. Build response
        return new StepResponse(
                interactionId,
                currentStepName,
                interaction.getStepStatus(),
                interaction.getStatus(),
                currentStepDef,
                stepData,
                interaction.getResumable(),
                interaction.getCaseId(),
                slaStatus,
                progress
        );
    }

    private String calculateSLAStatus(InteractionEntity interaction, StepDefinition stepDef) {
        if (stepDef.getEstimatedTime() == null) return "ON_TIME";

        try {
            // Very simple parser: "24h", "10m"
            String timeStr = stepDef.getEstimatedTime().toLowerCase();
            long durationMinutes;
            if (timeStr.endsWith("h")) {
                durationMinutes = Long.parseLong(timeStr.replace("h", "")) * 60;
            } else if (timeStr.endsWith("m")) {
                durationMinutes = Long.parseLong(timeStr.replace("m", ""));
            } else {
                return "ON_TIME";
            }

            Instant startedAt = interaction.getUpdatedAt(); // Last time the record was touched
            if (startedAt == null) startedAt = Instant.now();

            long minutesElapsed = java.time.Duration.between(startedAt, Instant.now()).toMinutes();

            if (minutesElapsed > durationMinutes) return "OVERDUE";
            if (minutesElapsed > (durationMinutes * 0.8)) return "NEARLY_OVERDUE";
            
            return "ON_TIME";
        } catch (Exception e) {
            log.warn("Failed to calculate SLA", e);
            return "ON_TIME";
        }
    }

    /**
     * Submit a step and navigate to the next one
     * This updates:
     * - flw_int.step_name (CURRENT POSITION)
     * - flow_case.audit_trail (HISTORY)
     * - flow_case.case_data (merged data)
     */
    @Transactional
    public StepResponse submitStep(String interactionId, String stepName, Map<String, Object> data) {
        log.info("Submitting step: interaction={}, step={}", interactionId, stepName);

        // 1. Load interaction and validate current step
        InteractionEntity interaction = intRepo.findById(interactionId)
                .orElseThrow(() -> new RuntimeException("Interaction not found"));

        if (!interaction.getStepName().equals(stepName)) {
            throw new RuntimeException("Step mismatch. Expected: " + interaction.getStepName() 
                    + ", got: " + stepName);
        }

        // 2. Load BLUEPRINT
        List<StepDefinition> allSteps = loadStepBlueprint(
                interaction.getInteractionDefinitionKey(),
                interaction.getInteractionDefinitionVersion()
        );

        StepDefinition currentStepDef = allSteps.stream()
                .filter(s -> s.getName().equals(stepName))
                .findFirst()
                .orElseThrow();

        // 3. Validate submitted data (simplified)
        validateStepData(currentStepDef, data);

        // 4. Load case
        CaseEntity caseEntity = caseRepo.findById(UUID.fromString(interaction.getCaseId()))
                .orElseThrow();

        // 5. Update HISTORY in audit_trail
        List<StepHistoryEntry> history = loadStepHistory(caseEntity);
        StepHistoryEntry newEntry = new StepHistoryEntry(
                stepName,
                "COMPLETED",
                Instant.now().minusSeconds(30), // Assume started 30s ago
                Instant.now(),
                data,
                null, // no errors
                Map.of("userAgent", "Mozilla/5.0", "ipAddress", "192.168.1.1")
        );
        history.add(newEntry);
        saveStepHistory(caseEntity, history);

        log.info("Added step to history: {}", stepName);

        // 6. Update case_data (merge submitted data)
        Map<String, Object> caseData = parseCaseData(caseEntity.getCaseData());
        caseData.putAll(data);
        caseEntity.setCaseData(toJson(caseData));

        // 7. Execute onSubmit actions (if any)
        if (currentStepDef.getOnSubmit() != null) {
            executeStepActions(currentStepDef.getOnSubmit(), interaction, caseEntity);
        }

        // 8. Determine next step from BLUEPRINT
        String nextStepName = currentStepDef.getNext();
        if (nextStepName == null) {
            // This is the last step
            interaction.setStepName(stepName);
            interaction.setStepStatus("COMPLETED");
            interaction.setStatus("COMPLETED");
            log.info("Journey completed!");
        } else {
            // Move to next step - Update CURRENT POSITION
            interaction.setStepName(nextStepName);
            interaction.setStepStatus("PENDING");
            log.info("Moving to next step: {}", nextStepName);
        }

        // 9. Save changes
        caseRepo.save(caseEntity);
        intRepo.save(interaction);

        // 10. Return next step info
        return getCurrentStep(interactionId);
    }

    /**
     * Get step history for a case
     * Returns all steps completed so far from flow_case.audit_trail
     */
    @Transactional(readOnly = true)
    public List<StepHistoryEntry> getStepHistory(String caseId) {
        CaseEntity caseEntity = caseRepo.findById(UUID.fromString(caseId))
                .orElseThrow(() -> new RuntimeException("Case not found"));
        
        return loadStepHistory(caseEntity);
    }

    /**
     * Get all step definitions from the blueprint
     * Returns the complete journey map from flw_int_def.steps
     */
    @Transactional(readOnly = true)
    public List<StepDefinition> getStepBlueprint(String interactionDefinitionKey, Long version) {
        return loadStepBlueprint(interactionDefinitionKey, version);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Load step blueprint from flw_int_def.steps (JSONB)
     */
    private List<StepDefinition> loadStepBlueprint(String key, Long version) {
        InteractionDefinitionEntity def = intDefRepo.findByInteractionDefinitionKeyAndInteractionDefinitionVersion(key, version)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + key + " v" + version));

        try {
            return objectMapper.readValue(
                    def.getSteps(),
                    new TypeReference<List<StepDefinition>>() {}
            );
        } catch (Exception e) {
            log.error("Failed to parse step definitions", e);
            throw new RuntimeException("Invalid step definitions", e);
        }
    }

    /**
     * Load step history from flow_case.audit_trail (JSONB)
     */
    private List<StepHistoryEntry> loadStepHistory(CaseEntity caseEntity) {
        if (caseEntity.getAuditTrail() == null || caseEntity.getAuditTrail().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Map<String, Object> auditTrail = objectMapper.readValue(
                    caseEntity.getAuditTrail(),
                    new TypeReference<Map<String, Object>>() {}
            );
            
            List<Map<String, Object>> stepsHistory = (List<Map<String, Object>>) 
                    auditTrail.getOrDefault("steps", new ArrayList<>());
            
            return stepsHistory.stream()
                    .map(this::mapToStepHistoryEntry)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse audit trail", e);
            return new ArrayList<>();
        }
    }

    /**
     * Save step history to flow_case.audit_trail (JSONB)
     */
    private void saveStepHistory(CaseEntity caseEntity, List<StepHistoryEntry> history) {
        try {
            Map<String, Object> auditTrail = new HashMap<>();
            auditTrail.put("steps", history);
            auditTrail.put("lastUpdated", Instant.now());
            
            caseEntity.setAuditTrail(objectMapper.writeValueAsString(auditTrail));
        } catch (Exception e) {
            log.error("Failed to save audit trail", e);
            throw new RuntimeException("Failed to save step history", e);
        }
    }

    /**
     * Get pre-filled data for a step from history
     */
    private Map<String, Object> getStepDataFromHistory(CaseEntity caseEntity, String stepName) {
        List<StepHistoryEntry> history = loadStepHistory(caseEntity);
        
        return history.stream()
                .filter(entry -> entry.getStepName().equals(stepName))
                .findFirst()
                .map(StepHistoryEntry::getData)
                .orElse(new HashMap<>());
    }

    private int findStepIndex(List<StepDefinition> steps, String stepName) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getName().equals(stepName)) {
                return i;
            }
        }
        return 0;
    }

    private void validateStepData(StepDefinition stepDef, Map<String, Object> data) {
        // Simplified validation - check required fields
        if (stepDef.getFields() != null) {
            for (FieldDefinition field : stepDef.getFields()) {
                if (Boolean.TRUE.equals(field.getRequired()) && !data.containsKey(field.getName())) {
                    throw new RuntimeException("Required field missing: " + field.getName());
                }
            }
        }
    }

    private void executeStepActions(List<Map<String, Object>> actions, 
                                    InteractionEntity interaction, 
                                    CaseEntity caseEntity) {
        for (Map<String, Object> action : actions) {
            String actionType = (String) action.get("action");
            log.info("Executing action: {}", actionType);
            
            // Handle different action types
            switch (actionType) {
                case "startWorkflow":
                    String workflowName = (String) action.get("workflow");
                    log.info("Starting Temporal workflow: {}", workflowName);
                    
                    Map<String, Object> initialData = parseCaseData(caseEntity.getCaseData());
                    
                    temporalWorkflowService.startKYCOnboardingWorkflow(
                            caseEntity.getId().toString(),
                            interaction.getId(),
                            interaction.getUserId(),
                            initialData
                    );
                    
                    interaction.setStatus("WAITING_SYSTEM");
                    break;
                case "createTask":
                    String taskType = (String) action.get("taskType");
                    log.info("Creating manual task: {}", taskType);
                    taskService.createTask(
                            interaction.getCaseId(),
                            interaction.getId(),
                            taskType != null ? taskType : "MANUAL_REVIEW",
                            toJson(action.getOrDefault("metadata", Map.of()))
                    );
                    interaction.setStatus("AWAITING_REVIEW");
                    break;
                default:
                    log.warn("Unknown action type: {}", actionType);
            }
        }
    }

    private Map<String, Object> parseCaseData(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
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

    private StepHistoryEntry mapToStepHistoryEntry(Map<String, Object> map) {
        try {
            return objectMapper.convertValue(map, StepHistoryEntry.class);
        } catch (Exception e) {
            log.error("Failed to map step history entry", e);
            return new StepHistoryEntry();
        }
    }
}
