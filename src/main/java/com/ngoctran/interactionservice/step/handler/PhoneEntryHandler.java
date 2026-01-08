package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Phone Entry Handler
 * Handles validation of initial phone number entry
 */
@Slf4j
@Component("PHONE_ENTERED")
public class PhoneEntryHandler implements StepHandler {

    @Override
    public StepResult execute(UUID instanceId) {
        log.info("Executing phone entry validation for instance: {}", instanceId);

        // In a real application, we might validate the phone format here
        // or check if it's already registered.

        // For now, we assume it's valid.
        return StepResult.success();
    }
}
