package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Account Creation Handler
 * Handles the final account creation step in the onboarding process
 * This typically involves calling core banking systems to create the actual account
 */
@Slf4j
@Component("ACCOUNT_CREATED")
@RequiredArgsConstructor
public class AccountCreationHandler implements StepHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public StepResult execute(UUID instanceId) {
        log.info("Executing account creation for instance: {}", instanceId);

        try {
            // In real implementation, this would:
            // 1. Gather all customer information from previous steps
            // 2. Call core banking system to create account
            // 3. Generate account number, IBAN, etc.
            // 4. Set up initial account settings
            // 5. Send welcome notifications

            // For this implementation, we'll simulate calling an external service
            // In production, this might be synchronous or asynchronous depending on the banking system

            boolean accountCreated = createAccount(instanceId);

            if (accountCreated) {
                log.info("Account creation successful for instance: {}", instanceId);

                // Publish account creation event for downstream systems
                publishAccountCreatedEvent(instanceId);

                return StepResult.success();
            } else {
                log.error("Account creation failed for instance: {}", instanceId);
                return StepResult.failure(
                        new com.ngoctran.interactionservice.step.StepError(
                                "ACCOUNT_CREATION_FAILED",
                                com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                                "Failed to create account in core banking system"
                        )
                );
            }

        } catch (Exception ex) {
            log.error("Error during account creation for instance: {}", instanceId, ex);
            return StepResult.failure(
                    new com.ngoctran.interactionservice.step.StepError(
                            "ACCOUNT_CREATION_ERROR",
                            com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                            "Account creation service error: " + ex.getMessage()
                    )
            );
        }
    }

    private boolean createAccount(UUID instanceId) {
        // Simulate account creation logic
        // In production, this would:
        // - Call core banking API
        // - Create customer record
        // - Generate account numbers
        // - Set up initial balances
        // - Configure account settings

        // For demo purposes, simulate success/failure
        return simulateAccountCreation(instanceId);
    }

    private boolean simulateAccountCreation(UUID instanceId) {
        // Simulate calling external banking system
        // In real implementation, this would make HTTP call to core banking API

        try {
            // Simulate network call delay
            Thread.sleep(100);

            // Simulate 95% success rate
            return Math.random() > 0.05;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void publishAccountCreatedEvent(UUID instanceId) {
        try {
            // Publish account creation event for downstream processing
            Map<String, Object> event = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", "ACCOUNT_CREATED",
                    "correlation", Map.of(
                            "instanceId", instanceId.toString()
                    ),
                    "payload", Map.of(
                            "instanceId", instanceId.toString(),
                            "accountCreated", true,
                            "timestamp", System.currentTimeMillis()
                    )
            );

            kafkaTemplate.send("account-events", instanceId.toString(), event);
            log.info("Published account creation event for instance: {}", instanceId);

        } catch (Exception ex) {
            log.warn("Failed to publish account creation event for instance: {}, but account was created successfully", instanceId, ex);
            // Don't fail the step for event publishing issues
        }
    }
}
