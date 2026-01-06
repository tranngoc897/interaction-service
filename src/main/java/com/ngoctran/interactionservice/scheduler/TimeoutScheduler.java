package com.ngoctran.interactionservice.scheduler;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import com.ngoctran.interactionservice.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeoutScheduler {

    private final OnboardingInstanceRepository instanceRepository;
    private final OnboardingEngine onboardingEngine;
    private final DistributedLockService distributedLockService;

    @Value("${workflow.timeout.ekyc:300}")
    private long ekycTimeoutSeconds;

    @Value("${workflow.timeout.aml:120}")
    private long amlTimeoutSeconds;

    /**
     * Scheduled job to handle timeouts for async steps
     * Uses distributed locking to coordinate across multiple instances
     * Runs every 60 seconds
     */
    @Scheduled(fixedDelayString = "${scheduler.timeout.interval:60000}")
    public void handleTimeouts() {
        log.debug("Starting timeout scheduler job");

        // Use distributed lock to ensure only one instance processes timeouts
        distributedLockService.executeWithLock("timeout-scheduler", () -> {
            try {
                log.debug("Checking for timed out workflow instances");

                Instant now = Instant.now();

                // Handle eKYC timeouts
                int ekycTimeouts = handleStepTimeout("EKYC_PENDING", ekycTimeoutSeconds, now);

                // Handle AML timeouts
                int amlTimeouts = handleStepTimeout("AML_PENDING", amlTimeoutSeconds, now);

                log.info("Timeout scheduler completed: {} eKYC timeouts, {} AML timeouts",
                        ekycTimeouts, amlTimeouts);

                return null;

            } catch (Exception ex) {
                log.error("Error in timeout scheduler", ex);
                return null;
            }
        });
    }

    private int handleStepTimeout(String state, long timeoutSeconds, Instant now) {
        Instant timeoutThreshold = now.minus(timeoutSeconds, ChronoUnit.SECONDS);

        List<OnboardingInstance> timedOutInstances =
                instanceRepository.findTimedOutInstances(state, timeoutThreshold);

        if (timedOutInstances.isEmpty()) {
            return 0;
        }

        log.info("Found {} instances timed out in state {}", timedOutInstances.size(), state);

        int successCount = 0;
        int errorCount = 0;

        for (OnboardingInstance instance : timedOutInstances) {
            try {
                log.warn("Instance {} timed out in state {} (started at {})",
                        instance.getId(), state, instance.getStateStartedAt());

                // Create timeout action
                ActionCommand timeoutCommand = ActionCommand.system(
                        instance.getId(),
                        "TIMEOUT"
                );

                // Process through engine
                onboardingEngine.handle(timeoutCommand);

                successCount++;
                log.info("Successfully processed timeout for instance {}", instance.getId());

            } catch (Exception ex) {
                errorCount++;
                log.error("Error processing timeout for instance {}: {}",
                        instance.getId(), ex.getMessage(), ex);
                // Continue with other timeouts
            }
        }

        log.info("Timeout processing for {} completed: {} successful, {} errors",
                state, successCount, errorCount);

        return successCount;
    }
}
