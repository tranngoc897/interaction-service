package com.ngoctran.interactionservice.scheduler;

import com.ngoctran.interactionservice.domain.StepExecution;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.StepExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private final StepExecutionRepository stepExecutionRepository;
    private final OnboardingEngine onboardingEngine;

    /**
     * Scheduled job to retry failed steps
     * Runs every 30 seconds
     */
    @Scheduled(fixedDelayString = "${scheduler.retry.interval:30000}")
    public void retryFailedSteps() {
        log.debug("Checking for failed steps to retry");

        Instant now = Instant.now();
        List<StepExecution> retryableSteps = stepExecutionRepository.findRetryable(now);

        if (retryableSteps.isEmpty()) {
            log.debug("No steps to retry at this time");
            return;
        }

        log.info("Found {} steps to retry", retryableSteps.size());

        for (StepExecution step : retryableSteps) {
            try {
                log.info("Retrying step {} for instance {}",
                        step.getState(), step.getInstanceId());

                // Create retry action command
                ActionCommand retryCommand = ActionCommand.system(
                        step.getInstanceId(),
                        "RETRY"
                );

                // Process through engine
                onboardingEngine.handle(retryCommand);

                log.info("Successfully triggered retry for step {} instance {}",
                        step.getState(), step.getInstanceId());

            } catch (Exception ex) {
                log.error("Error retrying step {} for instance {}: {}",
                        step.getState(), step.getInstanceId(), ex.getMessage(), ex);
                // Continue with other retries
            }
        }
    }
}
