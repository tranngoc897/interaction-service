package com.ngoctran.interactionservice.scheduler;

import com.ngoctran.interactionservice.domain.StepExecution;
import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import com.ngoctran.interactionservice.repo.StepExecutionRepository;
import com.ngoctran.interactionservice.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Retry Scheduler - Automatically retries failed step executions
 * Uses distributed locking to coordinate across multiple instances
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private final StepExecutionRepository stepExecutionRepository;
    private final OnboardingEngine onboardingEngine;
    private final DistributedLockService distributedLockService;

    @Scheduled(fixedDelayString = "${workflow.scheduler.retry.interval:30000}")
    public void retryFailedSteps() {
        log.debug("Starting retry scheduler job");

        // Use distributed lock to ensure only one instance processes retries
        distributedLockService.executeWithLock("retry-scheduler", () -> {
            try {
                List<StepExecution> retryableSteps = stepExecutionRepository.findRetryable(Instant.now());

                if (retryableSteps.isEmpty()) {
                    log.debug("No retryable steps found");
                    return null;
                }

                log.info("Found {} retryable steps", retryableSteps.size());

                int successCount = 0;
                int errorCount = 0;

                for (StepExecution step : retryableSteps) {
                    try {
                        log.info("Retrying step for instance: {}, state: {}",
                                step.getInstanceId(), step.getState());

                        // Create retry action command
                        ActionCommand retryCommand = ActionCommand.system(
                                step.getInstanceId(),
                                "RETRY"
                        );

                        // Process through engine
                        onboardingEngine.handle(retryCommand);

                        successCount++;
                        log.debug("Successfully triggered retry for instance: {}", step.getInstanceId());

                    } catch (Exception ex) {
                        errorCount++;
                        log.error("Error retrying step for instance: {}, state: {}",
                                step.getInstanceId(), step.getState(), ex);
                    }
                }

                log.info("Retry scheduler completed: {} successful, {} errors", successCount, errorCount);
                return null;

            } catch (Exception ex) {
                log.error("Error in retry scheduler", ex);
                return null;
            }
        });
    }
}
