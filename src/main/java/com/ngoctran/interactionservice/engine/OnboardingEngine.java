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
        }

        // 7. Mark event as processed
        processedEventRepository.save(command.getRequestId(), instance.getId(), "ACTION", command.getActor());

        log.info("Action processed successfully: {} for instance: {}", command.getAction(), command.getInstanceId());
    }
}
