package com.ngoctran.interactionservice.engine;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.domain.StepExecution;
import com.ngoctran.interactionservice.domain.Transition;
import com.ngoctran.interactionservice.repo.OnboardingInstanceRepository;
import com.ngoctran.interactionservice.repo.ProcessedEventRepository;
import com.ngoctran.interactionservice.repo.StepExecutionRepository;
import com.ngoctran.interactionservice.service.OutboxService;
import com.ngoctran.interactionservice.step.StepExecutor;
import java.util.Map;
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
    private final OutboxService outboxService;
    private final com.ngoctran.interactionservice.repo.StateSnapshotRepository snapshotRepository;
    private final com.ngoctran.interactionservice.repo.StateContextRepository contextRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.springframework.transaction.annotation.Transactional
    public void handle(ActionCommand command) {
        log.info("Processing action: {} for instance: {}", command.getAction(), command.getInstanceId());

        // 1. Idempotency check
        if (processedEventRepository.existsByEventId(command.getRequestId())) {
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
                instance.getId());

        // 4. Validate action
        actionValidator.validate(instance, command, transition);

        // 5. Execute step based on sync/async nature
        executeStepBasedOnType(instance, transition);

        // 6. Move state if not same-state action
        if (!transition.isSameState()) {
            instance.moveTo(transition.getToState());
            log.info("State transitioned: {} -> {} for instance: {}",
                    instance.getCurrentState(), transition.getToState(), instance.getId());

            // Persist state change immediately so recursive calls see the new state
            instanceRepository.saveAndFlush(instance);

            // 6.2 Save state snapshot for auditing
            saveSnapshot(instance);

            // 6.1. Attempt auto-progression to next steps
            attemptAutoProgression(instance, command);
        }

        // 7. Mark event as processed
        processedEventRepository.save(command.getRequestId(), instance.getId(), "ACTION", command.getActor());

        log.info("Action processed successfully: {} for instance: {}", command.getAction(), command.getInstanceId());
    }

    /**
     * Execute step based on whether it's sync or async to avoid long-running
     * transactions
     */
    /**
     * Execute step based on whether it's sync or async to avoid long-running
     * transactions
     * Async steps: Publish to outbox, execute later via Kafka consumers
     * Sync steps: Execute immediately but keep transaction short
     */
    private void executeStepBasedOnType(OnboardingInstance instance, Transition transition) {
        if (transition.isAsync()) {
            handleAsyncStep(instance, transition);
        } else {
            handleSyncStep(instance, transition);
        }
    }

    /**
     * Handle async steps - publish to outbox instead of executing immediately
     */
    private void handleAsyncStep(OnboardingInstance instance, Transition transition) {
        log.info("Handling async step: {} for instance: {}", transition.getToState(), instance.getId());

        // Create step execution record
        StepExecution execution = createStepExecution(instance, transition);
        stepExecutionRepository.save(execution);

        // For async steps, we just mark as accepted and publish to outbox
        publishAsyncStepToOutbox(instance, transition);
    }

    private void handleSyncStep(OnboardingInstance instance, Transition transition) {
        log.info("Handling sync step: {} for instance: {}", transition.getToState(), instance.getId());

        // Get or create step execution for auditing
        StepExecution execution = stepExecutionRepository.findById(
                new com.ngoctran.interactionservice.domain.StepExecutionId(
                        instance.getId(),
                        instance.getCurrentState()))
                .orElseGet(() -> {
                    StepExecution newExec = createStepExecution(instance, transition);
                    newExec.setStatus("RUNNING");
                    return stepExecutionRepository.save(newExec);
                });

        // Execute the step (It throws exception on failure)
        boolean success = stepExecutor.execute(instance, execution, transition);
        if (!success) {
            throw new RuntimeException("Sync step execution failed for " + instance.getCurrentState());
        }
    }

    /**
     * Publish async step to outbox for deferred execution
     */
    private void publishAsyncStepToOutbox(OnboardingInstance instance, Transition transition) {
        try {
            // Create event payload for async execution
            Object event = Map.of(
                    "instanceId", instance.getId().toString(),
                    "step", transition.getToState(),
                    "flowVersion", instance.getFlowVersion(),
                    "timestamp", java.time.Instant.now().toString(),
                    "correlationId", java.util.UUID.randomUUID().toString());

            // Determine topic based on step type
            String topic = getTopicForStep(transition.getToState());

            // Store in outbox for guaranteed delivery
            outboxService.storeEvent(
                    java.util.UUID.randomUUID().toString(),
                    topic,
                    instance.getId().toString(),
                    event,
                    "ASYNC_STEP_EXECUTION");

            log.info("Published async step {} to outbox for instance: {}", transition.getToState(), instance.getId());

        } catch (Exception ex) {
            log.error("Failed to publish async step to outbox: {}", ex.getMessage(), ex);
            // Don't fail the transaction - outbox will retry
        }
    }

    /**
     * Get Kafka topic for step type
     */
    private String getTopicForStep(String step) {
        switch (step) {
            case "EKYC_PENDING":
                return "ekyc-request";
            case "AML_PENDING":
                return "aml-request";
            default:
                return "workflow-events";
        }
    }

    /**
     * Create step execution record
     */
    private StepExecution createStepExecution(OnboardingInstance instance, Transition transition) {
        return StepExecution.builder()
                .instanceId(instance.getId())
                .state(instance.getCurrentState())
                .status("PENDING") // Will be updated when async execution completes
                .retryCount(0)
                .maxRetry(transition.getMaxRetry() != null ? transition.getMaxRetry() : 3)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
    }

    /**
     * Attempt to automatically progress to the next step if possible
     * This enables the workflow to continue automatically through multiple steps
     */
    /**
     * Attempt to automatically progress to the next step if possible
     * This enables the workflow to continue automatically through multiple steps
     */
    private void attemptAutoProgression(OnboardingInstance instance, ActionCommand parentCommand) {
        // Prevent infinite loops using explicit recursion depth
        int currentDepth = parentCommand.getRecursionDepth();
        if (currentDepth >= getMaxAutoProgressionSteps()) {
            log.warn("Stopping auto-progression after {} steps for instance: {}",
                    currentDepth, instance.getId());
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
            log.info("Attempting auto-progression from state: {} with action: {} for instance: {} (depth: {})",
                    instance.getCurrentState(), autoAction, instance.getId(), currentDepth + 1);

            // Create auto-execution command with incremented depth
            ActionCommand autoCommand = ActionCommand.system(instance.getId(), autoAction, currentDepth + 1);

            // Recursively handle the auto-progression
            // This will call handle() again, potentially leading to more auto-progression
            handle(autoCommand);

        } catch (Exception ex) {
            log.error("Auto-progression failed for instance: {} at state: {}",
                    instance.getId(), instance.getCurrentState(), ex);
            // Don't throw - auto-progression failure shouldn't stop the main flow
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
     * Save a snapshot of the current state and context data
     */
    private void saveSnapshot(OnboardingInstance instance) {
        try {
            String snapshotData = objectMapper.writeValueAsString(instance);

            String contextData = contextRepository.findById(instance.getId())
                    .map(com.ngoctran.interactionservice.domain.StateContext::getContextData)
                    .orElse("{}");

            com.ngoctran.interactionservice.domain.StateSnapshot snapshot = com.ngoctran.interactionservice.domain.StateSnapshot
                    .builder()
                    .instanceId(instance.getId())
                    .state(instance.getCurrentState())
                    .snapshotData(snapshotData)
                    .contextData(contextData)
                    .createdAt(java.time.Instant.now())
                    .build();

            snapshotRepository.save(snapshot);
            log.debug("Saved state snapshot for instance {} at state {}", instance.getId(), instance.getCurrentState());
        } catch (Exception ex) {
            log.error("Failed to save state snapshot for instance {}: {}", instance.getId(), ex.getMessage());
        }
    }

    /**
     * Get maximum allowed auto-progression steps to prevent infinite loops
     */
    private int getMaxAutoProgressionSteps() {
        return 5; // Configurable via properties
    }

}
