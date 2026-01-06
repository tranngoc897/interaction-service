package com.ngoctran.interactionservice.scheduler;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
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

    @Value("${workflow.timeout.ekyc:300}")
    private long ekycTimeoutSeconds;

    @Value("${workflow.timeout.aml:120}")
    private long amlTimeoutSeconds;

    /**
     * Scheduled job to handle timeouts for async steps
     * Runs every 60 seconds
     */
    @Scheduled(fixedDelayString = "${scheduler.timeout.interval:60000}")
    public void handleTimeouts() {
        log.debug("Checking for timed out workflow instances");

        Instant now = Instant.now();

        // Handle eKYC timeouts
        handleStepTimeout("EKYC_PENDING", ekycTimeoutSeconds, now);

        // Handle AML timeouts
        handleStepTimeout("AML_PENDING", amlTimeoutSeconds, now);

        log.debug("Timeout check completed");
    }

    private void handleStepTimeout(String state, long timeoutSeconds, Instant now) {
        Instant timeoutThreshold = now.minus(timeoutSeconds, ChronoUnit.SECONDS);

        List<OnboardingInstance> timedOutInstances =
                instanceRepository.findTimedOutInstances(state, timeoutThreshold);

        if (timedOutInstances.isEmpty()) {
            return;
        }

        log.info("Found {} instances timed out in state {}", timedOutInstances.size(), state);

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

                log.info("Successfully processed timeout for instance {}", instance.getId());

            } catch (Exception ex) {
                log.error("Error processing timeout for instance {}: {}",
                        instance.getId(), ex.getMessage(), ex);
                // Continue with other timeouts
            }
        }
    }
}
