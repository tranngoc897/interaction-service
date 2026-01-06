package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Profile Completion Handler
 * Handles profile completion validation after OTP verification
 * This step validates that all required profile information is provided
 */
@Slf4j
@Component("PROFILE_COMPLETED")
public class ProfileCompletionHandler implements StepHandler {

    @Override
    public StepResult execute(UUID instanceId) {
        log.info("Executing profile completion validation for instance: {}", instanceId);

        try {
            // In real implementation, this would:
            // 1. Retrieve profile data from database
            // 2. Validate required fields (name, email, address, etc.)
            // 3. Check data format and completeness
            // 4. Perform basic validation rules

            boolean profileComplete = validateProfile(instanceId);

            if (profileComplete) {
                log.info("Profile completion validation successful for instance: {}", instanceId);
                return StepResult.success();
            } else {
                log.warn("Profile completion validation failed for instance: {}", instanceId);
                return StepResult.failure(
                        new com.ngoctran.interactionservice.step.StepError(
                                "PROFILE_INCOMPLETE",
                                com.ngoctran.interactionservice.step.ErrorType.BUSINESS,
                                "Profile information is incomplete or invalid"
                        )
                );
            }

        } catch (Exception ex) {
            log.error("Error during profile completion validation for instance: {}", instanceId, ex);
            return StepResult.failure(
                    new com.ngoctran.interactionservice.step.StepError(
                            "PROFILE_VALIDATION_ERROR",
                            com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                            "Profile validation service error: " + ex.getMessage()
                    )
            );
        }
    }

    private boolean validateProfile(UUID instanceId) {
        // Simulate profile validation logic
        // In production, this would:
        // - Check if all required fields are filled
        // - Validate email format
        // - Validate phone number format
        // - Check address completeness
        // - Validate date of birth
        // - Check for duplicate customers

        // For demo purposes, assume validation passes
        return true;
    }
}
