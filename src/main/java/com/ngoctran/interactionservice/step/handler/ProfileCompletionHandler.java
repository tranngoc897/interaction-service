package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.engine.WorkflowVersionManager;
import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * EXAMPLE: Profile Completion Handler with Versioning
 * Demonstrates how to use WorkflowVersionManager for backward-compatible code
 * changes.
 */
@Slf4j
@Component("PROFILE_COMPLETED")
@RequiredArgsConstructor
public class ProfileCompletionHandler implements StepHandler {

    private final com.ngoctran.interactionservice.service.WorkflowHistoryService historyService;

    @Override
    public StepResult execute(UUID instanceId) {
        log.info("Executing profile completion for instance: {}", instanceId);

        // Example: We changed validation logic in version 2
        int version = WorkflowVersionManager.getVersion("profile-validation-logic", 1, 2);

        // Record the version decision
        WorkflowVersionManager.recordVersionMarker(instanceId, "profile-validation-logic", version, historyService);

        try {
            if (version == 1) {
                // OLD LOGIC (for workflows started before the change)
                log.info("[V1] Using legacy profile validation for instance: {}", instanceId);
                return validateProfileV1(instanceId);
            } else {
                // NEW LOGIC (for new workflows)
                log.info("[V2] Using enhanced profile validation for instance: {}", instanceId);
                return validateProfileV2(instanceId);
            }
        } finally {
            WorkflowVersionManager.clear();
        }
    }

    private StepResult validateProfileV1(UUID instanceId) {
        // Old validation: Only check if name is not empty
        log.debug("V1: Basic validation - checking name only");
        return StepResult.success();
    }

    private StepResult validateProfileV2(UUID instanceId) {
        // New validation: Check name + email format + phone format
        log.debug("V2: Enhanced validation - checking name, email, and phone");
        return StepResult.success();
    }
}
