package com.ngoctran.interactionservice.engine;

import com.ngoctran.interactionservice.domain.OnboardingInstance;
import com.ngoctran.interactionservice.domain.Transition;
import org.springframework.stereotype.Component;

@Component
public class ActionValidator {

    public void validate(OnboardingInstance instance, ActionCommand command, Transition transition) {
        // 1. Check if instance is completed
        if (instance.isCompleted()) {
            throw new IllegalStateException("Onboarding already completed");
        }

        // 2. Check if actor is allowed
        if (!transition.isAllowedActor(command.getActor())) {
            throw new SecurityException(
                    "Actor " + command.getActor() + " not allowed for action " + command.getAction()
            );
        }

        // 3. Check same-state retry
        if (transition.isSameState() && !transition.isAllowedActor("SYSTEM")) {
            throw new IllegalStateException(
                    "Retry not allowed for state " + instance.getCurrentState()
            );
        }
    }
}
