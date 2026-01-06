package com.ngoctran.interactionservice.engine;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.domain.StepExecution;
import com.ngoctran.interactionservice.domain.Transition;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import com.ngoctran.interactionservice.repo.ProcessedEventRepository;
import com.ngoctran.interactionservice.repo.StepExecutionRepository;
import com.ngoctran.interactionservice.step.StepExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingEngine {

    private final OnboardingInstanceRepository instanceRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TransitionResolver transitionResolver;
    private final ActionValidator actionValidator;
    private final StepExecutor stepExecutor;

    @Transactional
    public void handle(ActionCommand command) {
        log.info("Processing action: {} for instance: {}", command.getAction(), command.getInstanceId());

        // 1. Idempotency check
        if (processedEventRepository.exists(command.getRequestId())) {
            log.warn("Duplicate event detected: {}", command.getRequestId());
            return;
        }

        // 2. Lock instance for update
        OnboardingInstance instance = instanceRepository.findByIdForUpdate(command.getInstanceId());
        if (instance == null) {
            throw new IllegalArgumentException("Instance not found: " + command.getInstanceId());
        }

        // 3. Resolve transition with rule evaluation
        Transition transition = transitionResolver.resolve(
                instance.getFlowVersion(),
                instance.getCurrentState(),
                command.getAction(),
                instance.getId()
        );

        // 4. Validate action
        actionValidator.validate(instance, command, transition);

        // 5. Execute step (retry / async / sync)
        StepExecution execution = stepExecutionRepository.findById(
                new com.ngoctran.interactionservice.domain.StepExecutionId(
                        instance.getId(),
                        instance.getCurrentState()
                )
        ).orElse(null);

        boolean success = stepExecutor.execute(instance, execution, transition);

        // 6. Move state if success and not same-state action
        if (success && !transition.isSameState()) {
            instance.moveTo(transition.getToState());
            log.info("State transitioned: {} -> {} for instance: {}",
                    instance.getCurrentState(), transition.getToState(), instance.getId());

            // 6.1. Attempt auto-progression to next steps
            attemptAutoProgression(instance);
        }

        // 7. Mark event as processed
        processedEventRepository.save(command.getRequestId(), instance.getId(), "ACTION", command.getActor());

        log.info("Action processed successfully: {} for instance: {}", command.getAction(), command.getInstanceId());
    }

    /**
     * Attempt to automatically progress to the next step if possible
     * This enables the workflow to continue automatically through multiple steps
     */
    private void attemptAutoProgression(OnboardingInstance instance) {
        // Prevent infinite loops
        int autoProgressionDepth = getAutoProgressionDepth(instance);
        if (autoProgressionDepth >= getMaxAutoProgressionSteps()) {
            log.warn("Stopping auto-progression after {} steps for instance: {}",
                    autoProgressionDepth, instance.getId());
            return;
        }

        // Check if current state allows auto-progression
        if (!canAutoProgress(instance.getCurrentState())) {
            log.debug("Auto-progression not allowed for state: {} on instance: {}",
                    instance.getCurrentState(), instance.getId());
            return;
        }

        // Find the default auto-progression action (usually "NEXT")
        String autoAction = getAutoProgressionAction(instance.getCurrentState());
        if (autoAction == null) {
            log.debug("No auto-progression action defined for state: {} on instance: {}",
                    instance.getCurrentState(), instance.getId());
            return;
        }

        try {
            log.info("Attempting auto-progression from state: {} with action: {} for instance: {}",
                    instance.getCurrentState(), autoAction, instance.getId());

            // Increment auto-progression depth
            incrementAutoProgressionDepth(instance);

            // Create auto-execution command
            ActionCommand autoCommand = ActionCommand.system(instance.getId(), autoAction);

            // Recursively handle the auto-progression
            // This will call handle() again, potentially leading to more auto-progression
            handle(autoCommand);

        } catch (Exception ex) {
            log.error("Auto-progression failed for instance: {} at state: {}",
                    instance.getId(), instance.getCurrentState(), ex);
            // Don't throw - auto-progression failure shouldn't stop the main flow
            resetAutoProgressionDepth(instance);
        }
    }

    /**
     * Check if a state allows auto-progression
     */
    private boolean canAutoProgress(String currentState) {
        // Define which states can auto-progress
        switch (currentState) {
            case "EKYC_APPROVED":
            case "AML_CLEARED":
                return true; // These states can auto-progress to next steps
            case "ACCOUNT_CREATED":
                return false; // Stop here - requires user confirmation
            default:
                return false; // Most states require user interaction
        }
    }

    /**
     * Get the auto-progression action for a state
     */
    private String getAutoProgressionAction(String currentState) {
        switch (currentState) {
            case "EKYC_APPROVED":
                return "NEXT"; // Auto-progress to AML
            case "AML_CLEARED":
                return "NEXT"; // Auto-progress to account creation
            default:
                return null;
        }
    }

    /**
     * Get maximum allowed auto-progression steps to prevent infinite loops
     */
    private int getMaxAutoProgressionSteps() {
        return 5; // Configurable via properties
    }

    /**
     * Track auto-progression depth to prevent infinite loops
     */
    private int getAutoProgressionDepth(OnboardingInstance instance) {
        // In a real implementation, this would be stored in the instance or a separate tracking table
        // For now, we'll use a simple counter based on state transitions
        return calculateProgressionDepth(instance);
    }

    private void incrementAutoProgressionDepth(OnboardingInstance instance) {
        // Track in instance metadata or separate table
        log.debug("Incremented auto-progression depth for instance: {}", instance.getId());
    }

    private void resetAutoProgressionDepth(OnboardingInstance instance) {
        // Reset counter
        log.debug("Reset auto-progression depth for instance: {}", instance.getId());
    }

    private int calculateProgressionDepth(OnboardingInstance instance) {
        // Simple calculation based on how many auto-executable states we've passed
        String state = instance.getCurrentState();
        switch (state) {
            case "EKYC_APPROVED": return 1;
            case "AML_CLEARED": return 2;
            case "ACCOUNT_CREATED": return 3;
            default: return 0;
        }
    }
}
