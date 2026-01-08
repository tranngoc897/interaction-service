package com.ngoctran.interactionservice.step;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.domain.StepExecution;
import com.ngoctran.interactionservice.domain.Transition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StepExecutor {

    private final Map<String, StepHandler> handlers;
    private final com.ngoctran.interactionservice.repo.IncidentRepository incidentRepository;

    /**
     * Execute a step with retry logic and error handling
     * 
     * @return true if step completed successfully and can proceed to next state
     */
    @Transactional
    public boolean execute(OnboardingInstance instance, StepExecution execution, Transition transition) {
        // Initialize execution if first time
        if (execution == null || execution.isNew()) {
            execution = createNewExecution(instance, transition);
        }

        // Skip if already successful
        if (execution.isSuccess()) {
            return true;
        }

        // Check if can retry
        if (execution.isFailed() && !execution.canRetry()) {
            log.warn("Step {} for instance {} has exhausted retries", instance.getCurrentState(), instance.getId());
            createIncident(instance, "RETRIES_EXHAUSTED",
                    "Maximum retries reached for state " + instance.getCurrentState());
            return false;
        }

        StepContext context = new StepContext(instance.getId(), instance.getCurrentState(), instance.getFlowVersion());

        Instant start = Instant.now();
        try {
            // Execute the business logic
            StepHandler handler = handlers.get(instance.getCurrentState());
            if (handler == null) {
                throw new IllegalStateException("No StepHandler found for state: " + instance.getCurrentState());
            }

            StepResult result = handler.execute(instance.getId());

            if (result.isSuccess()) {
                execution.markSuccess();
                log.info("Step {} completed successfully for instance {}", instance.getCurrentState(),
                        instance.getId());
                return true;
            } else {
                // Handle failure
                return handleFailure(instance, execution, result.getError(), start);
            }

        } catch (Exception ex) {
            // System error
            StepError error = new StepError(
                    "STEP_EXECUTION_EXCEPTION",
                    ErrorType.SYSTEM,
                    ex.getMessage());
            return handleFailure(instance, execution, error, start);
        }
    }

    private StepExecution createNewExecution(OnboardingInstance instance, Transition transition) {
        StepExecution execution = StepExecution.builder()
                .instanceId(instance.getId())
                .state(instance.getCurrentState())
                .status("RUNNING")
                .retryCount(0)
                .maxRetry(transition.getMaxRetry() != null ? transition.getMaxRetry() : 3)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return execution;
    }

    private boolean handleFailure(OnboardingInstance instance, StepExecution execution, StepError error,
            Instant start) {
        execution.incrementRetry();
        execution.setLastError(error.getCode(), error.getMessage());

        log.warn("Step {} failed for instance {}: {} - {}", instance.getCurrentState(), instance.getId(),
                error.getCode(), error.getMessage());

        // Determine if can retry
        if (error.getType() == ErrorType.TRANSIENT && execution.canRetry()) {
            // Schedule next retry with exponential backoff
            Instant nextRetry = Instant.now().plusSeconds((long) Math.pow(2, execution.getRetryCount()));
            execution.scheduleNextRetry(nextRetry);
            log.info("Scheduled retry for step {} instance {} at {}", instance.getCurrentState(), instance.getId(),
                    nextRetry);
            return false;
        } else {
            // Permanent failure
            execution.markFailed();
            log.error("Step {} permanently failed for instance {}: {}", instance.getCurrentState(), instance.getId(),
                    error.getCode());

            createIncident(instance, error.getCode(), error.getMessage());

            return false;
        }
    }

    private void createIncident(OnboardingInstance instance, String errorCode, String message) {
        try {
            com.ngoctran.interactionservice.domain.Incident incident = com.ngoctran.interactionservice.domain.Incident
                    .builder()
                    .incidentId(java.util.UUID.randomUUID())
                    .instanceId(instance.getId())
                    .state(instance.getCurrentState())
                    .errorCode(errorCode)
                    .severity("HIGH")
                    .status("OPEN")
                    .description(message)
                    .createdAt(Instant.now())
                    .build();
            incidentRepository.save(incident);
            log.info("Created incident for instance {} with error {}", instance.getId(), errorCode);
        } catch (Exception e) {
            log.error("Failed to create incident for instance {}", instance.getId(), e);
        }
    }
}
