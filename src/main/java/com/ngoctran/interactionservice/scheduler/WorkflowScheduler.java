package com.ngoctran.interactionservice.scheduler;

import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

/**
 * Scheduler for background workflow tasks
 * Handles timeouts and potentially retries for async tasks
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowScheduler {

    private final OnboardingInstanceRepository instanceRepository;
    private final com.ngoctran.interactionservice.repo.StepExecutionRepository stepExecutionRepository;
    private final OnboardingEngine onboardingEngine;

    /**
     * Scan for timed-out instances and trigger TIMEOUT action
     * Runs every 1 minute
     */
    @Scheduled(fixedDelayString = "${scheduler.timeout-interval:60000}")
    public void processTimeouts() {
        log.debug("Scanning for timed-out onboarding instances...");

        // Define timeout threshold (e.g., 5 minutes for demonstration)
        Instant timeoutThreshold = Instant.now().minus(Duration.ofMinutes(5));

        // Find instances at PENDING states that have been there too long
        String[] pendingStates = { "EKYC_PENDING", "AML_PENDING" };

        for (String state : pendingStates) {
            instanceRepository.findTimedOutInstances(state, timeoutThreshold).forEach(instance -> {
                log.warn("Found timed-out instance: {} in state: {}", instance.getId(), state);
                try {
                    ActionCommand command = ActionCommand.system(instance.getId(), "TIMEOUT", 0);
                    onboardingEngine.handle(command);
                } catch (Exception e) {
                    log.error("Failed to process timeout for instance: {}", instance.getId(), e);
                }
            });
        }
    }

    /**
     * Scan for steps scheduled for retry and trigger RETRY action
     */
    @Scheduled(fixedDelayString = "${scheduler.retry-interval:30000}")
    public void processRetries() {
        log.debug("Scanning for scheduled retries...");

        stepExecutionRepository.findScheduledRetries(Instant.now()).forEach(execution -> {
            log.info("Triggering automatic retry for instance: {} state: {}",
                    execution.getInstanceId(), execution.getState());
            try {
                // Trigger 'RETRY' action which is defined in our transition table
                ActionCommand command = ActionCommand.system(execution.getInstanceId(), "RETRY", 0);
                onboardingEngine.handle(command);
            } catch (Exception e) {
                log.error("Failed to trigger retry for instance: {}", execution.getInstanceId(), e);
            }
        });
    }
}
