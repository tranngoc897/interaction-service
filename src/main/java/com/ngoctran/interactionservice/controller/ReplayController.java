package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.service.WorkflowHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/replay")
@RequiredArgsConstructor
public class ReplayController {

    private final WorkflowHistoryService historyService;
    private final OnboardingEngine onboardingEngine;

    /**
     * Trigger a replay of the workflow history for a given instance.
     * This will re-run all actions recorded in the history without re-triggering
     * side effects.
     */
    @PostMapping("/{instanceId}")
    public ResponseEntity<Map<String, Object>> triggerReplay(@PathVariable UUID instanceId) {
        log.info("Admin triggered replay for instance: {}", instanceId);

        try {
            historyService.replay(instanceId, onboardingEngine);

            return ResponseEntity.ok(Map.of(
                    "instanceId", instanceId,
                    "status", "SUCCESS",
                    "message", "Workflow replay completed. Check logs for side-effect suppression details."));
        } catch (Exception e) {
            log.error("Replay failed for instance: {}", instanceId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Replay failed",
                    "message", e.getMessage()));
        }
    }
}
