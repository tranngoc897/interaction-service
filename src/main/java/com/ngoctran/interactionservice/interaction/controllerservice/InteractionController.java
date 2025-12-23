package com.ngoctran.interactionservice.interaction.controllerservice;

import com.ngoctran.interactionservice.interaction.dto.StepDefinition;
import com.ngoctran.interactionservice.interaction.dto.StepHistoryEntry;
import com.ngoctran.interactionservice.interaction.dto.StepResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller demonstrating the 3 types of "steps":
 * 
 * 1. GET /api/interactions/{id}/current-step
 * - Returns current step info (combines BLUEPRINT + CURRENT POSITION + HISTORY)
 * 
 * 2. POST /api/interactions/{id}/submit-step
 * - Submit step data (updates CURRENT POSITION + HISTORY)
 * 
 * 3. GET /api/interactions/definitions/{key}/steps
 * - Get step blueprint (BLUEPRINT only)
 * 
 * 4. GET /api/cases/{caseId}/step-history
 * - Get step history (HISTORY only)
 */
@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
@Slf4j
public class InteractionController {

    private final InteractionService interactionService;

    /**
     * Start a new interaction journey
     * 
     * This API uses BLUEPRINT data (flw_int_def) to:
     * 1. Create a new Case record (flow_case)
     * 2. Create a new Interaction record (flw_int)
     * 3. Position the user at the first step
     */
    @PostMapping("/start")
    public ResponseEntity<StepResponse> startInteraction(
            @RequestBody com.ngoctran.interactionservice.interaction.InteractionStartRequest request) {
        log.info("POST /api/interactions/start - key: {}", request.interactionDefinitionKey());

        StepResponse response = interactionService.startInteraction(request);

        log.info("Interaction started: {} (first step: {})",
                response.getInteractionId(),
                response.getStepName());

        return ResponseEntity.ok(response);
    }

    /**
     * Get current step information for an interaction
     * public ResponseEntity<StepResponse> getCurrentStep(@PathVariable String
     * interactionId) {
     * log.info("GET /api/interactions/{}/current-step", interactionId);
     * 
     * StepResponse response = interactionService.getCurrentStep(interactionId);
     * 
     * log.info("Current step: {} (status: {})",
     * response.getStepName(),
     * response.getStepStatus());
     * 
     * return ResponseEntity.ok(response);
     * }
     * 
     * /**
     * Submit step data and move to next step
     * 
     * This demonstrates how submission:
     * 1. Validates data against BLUEPRINT (flw_int_def.steps[x].fields)
     * 2. Adds entry to HISTORY (flow_case.audit_trail)
     * 3. Updates CURRENT POSITION (flw_int.step_name = next step)
     * 4. Executes actions from BLUEPRINT (flw_int_def.steps[x].onSubmit)
     * 
     * Request body:
     * {
     * "stepName": "personal-info",
     * "data": {
     * "fullName": "Nguyen Van A",
     * "dob": "1990-01-01",
     * "idNumber": "123456789"
     * }
     * }
     */
    @PostMapping("/{interactionId}/submit-step")
    public ResponseEntity<StepResponse> submitStep(
            @PathVariable String interactionId,
            @RequestBody StepSubmitRequest request) {

        log.info("POST /api/interactions/{}/submit-step - step: {}",
                interactionId,
                request.getStepName());

        StepResponse response = interactionService.submitStep(
                interactionId,
                request.getStepName(),
                request.getData());

        log.info("Step submitted. Next step: {}", response.getStepName());

        return ResponseEntity.ok(response);
    }

    /**
     * Get the complete step blueprint for an interaction definition
     * 
     * This returns the BLUEPRINT (flw_int_def.steps) - the template
     * showing all possible steps in the journey.
     * 
     * Use case: Frontend wants to show a progress bar or journey map
     * 
     * Example response:
     * [
     * {"name": "welcome", "type": "info", "title": "Chào mừng", ...},
     * {"name": "personal-info", "type": "form", "title": "Thông tin cá nhân", ...},
     * {"name": "address-info", "type": "form", "title": "Địa chỉ", ...},
     * ...
     * ]
     */
    @GetMapping("/definitions/{key}/steps")
    public ResponseEntity<List<StepDefinition>> getStepBlueprint(
            @PathVariable String key,
            @RequestParam(defaultValue = "1") Long version) {

        log.info("GET /api/interactions/definitions/{}/steps?version={}", key, version);

        List<StepDefinition> steps = interactionService.getStepBlueprint(key, version);

        log.info("Found {} steps in blueprint", steps.size());

        return ResponseEntity.ok(steps);
    }

    /**
     * Get step history for a case
     * 
     * This returns the HISTORY (flow_case.audit_trail.steps) -
     * all steps completed so far with timestamps and submitted data.
     * 
     * Use case:
     * - User wants to review what they've submitted
     * - Admin wants to audit the journey
     * - Compliance/reporting
     * 
     * Example response:
     * [
     * {
     * "stepName": "welcome",
     * "status": "COMPLETED",
     * "completedAt": "2025-12-20T08:00:00Z",
     * "data": {}
     * },
     * {
     * "stepName": "personal-info",
     * "status": "COMPLETED",
     * "completedAt": "2025-12-20T08:05:00Z",
     * "data": {
     * "fullName": "Nguyen Van A",
     * "dob": "1990-01-01"
     * }
     * }
     * ]
     */
    @GetMapping("/cases/{caseId}/step-history")
    public ResponseEntity<List<StepHistoryEntry>> getStepHistory(@PathVariable String caseId) {
        log.info("GET /api/interactions/cases/{}/step-history", caseId);

        List<StepHistoryEntry> history = interactionService.getStepHistory(caseId);

        log.info("Found {} steps in history", history.size());

        return ResponseEntity.ok(history);
    }

    /**
     * Request DTO for step submission
     */
    @lombok.Data
    public static class StepSubmitRequest {
        private String stepName;
        private Map<String, Object> data;
    }

}
