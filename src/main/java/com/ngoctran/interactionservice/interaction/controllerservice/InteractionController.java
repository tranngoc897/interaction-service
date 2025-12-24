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

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
@Slf4j
public class InteractionController {

    private final InteractionService interactionService;

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

    @GetMapping("/{interactionId}/current-step")
    public ResponseEntity<StepResponse> getCurrentStep(@PathVariable String interactionId) {
        log.info("GET /api/interactions/{}/current-step", interactionId);

        StepResponse response = interactionService.getCurrentStep(interactionId);

        log.info("Current step: {} (status: {})",
                response.getStepName(),
                response.getStepStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Submit step data and move to next step
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

    @GetMapping("/definitions/{key}/steps")
    public ResponseEntity<List<StepDefinition>> getStepBlueprint(
            @PathVariable String key,
            @RequestParam(defaultValue = "1") Long version) {

        log.info("GET /api/interactions/definitions/{}/steps?version={}", key, version);

        List<StepDefinition> steps = interactionService.getStepBlueprint(key, version);

        log.info("Found {} steps in blueprint", steps.size());

        return ResponseEntity.ok(steps);
    }


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
