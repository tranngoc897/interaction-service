package com.ngoctran.interactionservice.service;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.domain.StepExecution;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import com.ngoctran.interactionservice.repo.StepExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Workflow Recovery Service
 * Handles automatic recovery of workflows after application restart or crash.
 * Similar to Temporal's automatic workflow continuation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowRecoveryService {

    private final OnboardingInstanceRepository instanceRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final OnboardingEngine onboardingEngine;
    private final WorkflowHistoryService historyService;

    private static final int RECOVERY_BATCH_SIZE = 50;
    private static final int STUCK_THRESHOLD_MINUTES = 30;

    /**
     * Triggered when application starts up.
     * Scans for workflows that were interrupted and resumes them.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== WORKFLOW RECOVERY: Application started, scanning for interrupted workflows ===");

        try {
            // Wait a bit for other services to initialize
            Thread.sleep(5000);

            recoverInterruptedWorkflows();
            recoverStuckSteps();

            log.info("=== WORKFLOW RECOVERY: Completed ===");
        } catch (Exception e) {
            log.error("Error during workflow recovery on startup", e);
        }
    }

    /**
     * Periodic health check to detect and recover stuck workflows.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutes
    public void periodicHealthCheck() {
        log.debug("Running periodic workflow health check");

        try {
            recoverStuckSteps();
        } catch (Exception e) {
            log.error("Error during periodic health check", e);
        }
    }

    /**
     * Recover workflows that were active but interrupted (e.g., by crash or
     * deployment)
     */
    private void recoverInterruptedWorkflows() {
        log.info("Scanning for interrupted workflows...");

        // Find all ACTIVE instances
        List<OnboardingInstance> activeInstances = instanceRepository
                .findByStatus("ACTIVE", PageRequest.of(0, RECOVERY_BATCH_SIZE))
                .getContent();

        if (activeInstances.isEmpty()) {
            log.info("No interrupted workflows found");
            return;
        }

        log.info("Found {} active workflows, checking for interruptions", activeInstances.size());

        int recoveredCount = 0;
        for (OnboardingInstance instance : activeInstances) {
            try {
                if (shouldRecover(instance)) {
                    recoverWorkflow(instance);
                    recoveredCount++;
                }
            } catch (Exception e) {
                log.error("Failed to recover workflow {}", instance.getId(), e);
            }
        }

        log.info("Recovered {} interrupted workflows", recoveredCount);
    }

    /**
     * Recover steps that are stuck in RUNNING or PENDING state for too long
     */
    private void recoverStuckSteps() {
        log.debug("Scanning for stuck step executions...");

        Instant stuckThreshold = Instant.now().minusSeconds(STUCK_THRESHOLD_MINUTES * 60);

        // Find steps that have been running for too long
        List<StepExecution> stuckSteps = stepExecutionRepository
                .findAll()
                .stream()
                .filter(step -> "RUNNING".equals(step.getStatus()) || "PENDING".equals(step.getStatus()))
                .filter(step -> step.getUpdatedAt().isBefore(stuckThreshold))
                .limit(RECOVERY_BATCH_SIZE)
                .toList();

        if (stuckSteps.isEmpty()) {
            log.debug("No stuck steps found");
            return;
        }

        log.warn("Found {} stuck step executions, attempting recovery", stuckSteps.size());

        for (StepExecution step : stuckSteps) {
            try {
                recoverStuckStep(step);
            } catch (Exception e) {
                log.error("Failed to recover stuck step {} for instance {}",
                        step.getState(), step.getInstanceId(), e);
            }
        }
    }

    /**
     * Determine if a workflow should be recovered
     */
    private boolean shouldRecover(OnboardingInstance instance) {
        // Check if there are any pending or running steps
        List<StepExecution> pendingSteps = stepExecutionRepository
                .findAll()
                .stream()
                .filter(step -> step.getInstanceId().equals(instance.getId()))
                .filter(step -> "PENDING".equals(step.getStatus()) || "RUNNING".equals(step.getStatus()))
                .toList();

        if (!pendingSteps.isEmpty()) {
            log.info("Instance {} has {} pending/running steps, will recover",
                    instance.getId(), pendingSteps.size());
            return true;
        }

        // Check if instance is in a state that requires action
        if (isActionableState(instance.getCurrentState())) {
            log.info("Instance {} is in actionable state {}, will recover",
                    instance.getId(), instance.getCurrentState());
            return true;
        }

        return false;
    }

    /**
     * Recover a specific workflow by triggering appropriate action
     */
    private void recoverWorkflow(OnboardingInstance instance) {
        log.info("Recovering workflow {} in state {}", instance.getId(), instance.getCurrentState());

        // Record recovery event
        historyService.recordEvent(
                instance.getId(),
                "RECOVERY",
                "AUTO_RECOVERY_ON_STARTUP",
                java.util.Map.of(
                        "state", instance.getCurrentState(),
                        "reason", "Application restart",
                        "timestamp", Instant.now().toString()),
                "SYSTEM");

        // Determine appropriate recovery action based on state
        String recoveryAction = determineRecoveryAction(instance.getCurrentState());

        if (recoveryAction != null) {
            try {
                ActionCommand command = ActionCommand.system(
                        instance.getId(),
                        recoveryAction,
                        0 // Reset recursion depth
                );

                onboardingEngine.handle(command);
                log.info("Successfully recovered workflow {} with action {}",
                        instance.getId(), recoveryAction);
            } catch (Exception e) {
                log.error("Failed to execute recovery action {} for workflow {}",
                        recoveryAction, instance.getId(), e);
            }
        }
    }

    /**
     * Recover a stuck step execution
     */
    private void recoverStuckStep(StepExecution step) {
        log.warn("Recovering stuck step {} for instance {} (stuck for {} minutes)",
                step.getState(), step.getInstanceId(),
                java.time.Duration.between(step.getUpdatedAt(), Instant.now()).toMinutes());

        // Record recovery event
        historyService.recordEvent(
                step.getInstanceId(),
                "RECOVERY",
                "STUCK_STEP_RECOVERY",
                java.util.Map.of(
                        "state", step.getState(),
                        "status", step.getStatus(),
                        "stuckDuration", java.time.Duration.between(step.getUpdatedAt(), Instant.now()).toMinutes(),
                        "timestamp", Instant.now().toString()),
                "SYSTEM");

        // Mark as failed and let retry mechanism handle it
        step.setStatus("FAILED");
        step.setLastError("STUCK_STEP", "Step stuck for too long, marked for retry");
        step.setUpdatedAt(Instant.now());
        stepExecutionRepository.save(step);

        log.info("Marked stuck step {} as FAILED, retry scheduler will handle it", step.getState());
    }

    /**
     * Check if a state requires user or system action
     */
    private boolean isActionableState(String state) {
        // States that are waiting for external events or user action
        return state.equals("EKYC_PENDING") ||
                state.equals("AML_PENDING") ||
                state.equals("PHONE_ENTERED") ||
                state.equals("OTP_VERIFIED");
    }

    /**
     * Determine the appropriate recovery action for a given state
     */
    private String determineRecoveryAction(String state) {
        switch (state) {
            case "PHONE_ENTERED":
            case "OTP_VERIFIED":
            case "PROFILE_COMPLETED":
            case "DOC_UPLOADED":
                return "NEXT"; // Continue to next step
            case "EKYC_APPROVED":
            case "AML_CLEARED":
                return "NEXT"; // Auto-progress
            default:
                return null; // No automatic recovery action
        }
    }
}
