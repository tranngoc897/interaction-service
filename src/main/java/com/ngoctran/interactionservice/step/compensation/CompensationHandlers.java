package com.ngoctran.interactionservice.step.compensation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * EXAMPLE: Compensation handlers for SAGA pattern
 * These handlers undo the side effects of completed steps
 */
@Slf4j
public class CompensationHandlers {

    /**
     * Compensate account creation by deleting the account
     */
    @Component("UNDO_ACCOUNT_CREATION")
    public static class UndoAccountCreationHandler implements com.ngoctran.interactionservice.step.StepHandler {

        @Override
        public com.ngoctran.interactionservice.step.StepResult execute(UUID instanceId) {
            log.warn("[COMPENSATION] Undoing account creation for instance: {}", instanceId);

            try {
                // In real implementation:
                // 1. Call account service to delete account
                // 2. Remove from database
                // 3. Notify user (optional)

                log.info("[COMPENSATION] Successfully deleted account for instance: {}", instanceId);
                return com.ngoctran.interactionservice.step.StepResult.success();

            } catch (Exception e) {
                log.error("[COMPENSATION] Failed to delete account for instance: {}", instanceId, e);
                return com.ngoctran.interactionservice.step.StepResult.failure(
                        new com.ngoctran.interactionservice.step.StepError(
                                "COMPENSATION_FAILED",
                                com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                                "Failed to undo account creation: " + e.getMessage()));
            }
        }
    }

    /**
     * Compensate wallet creation by deleting the wallet
     */
    @Component("UNDO_WALLET_CREATION")
    public static class UndoWalletCreationHandler implements com.ngoctran.interactionservice.step.StepHandler {

        @Override
        public com.ngoctran.interactionservice.step.StepResult execute(UUID instanceId) {
            log.warn("[COMPENSATION] Undoing wallet creation for instance: {}", instanceId);

            try {
                // In real implementation:
                // 1. Call wallet service to deactivate/delete wallet
                // 2. Reverse any initial balance credits
                // 3. Update audit logs

                log.info("[COMPENSATION] Successfully deleted wallet for instance: {}", instanceId);
                return com.ngoctran.interactionservice.step.StepResult.success();

            } catch (Exception e) {
                log.error("[COMPENSATION] Failed to delete wallet for instance: {}", instanceId, e);
                return com.ngoctran.interactionservice.step.StepResult.failure(
                        new com.ngoctran.interactionservice.step.StepError(
                                "COMPENSATION_FAILED",
                                com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                                "Failed to undo wallet creation: " + e.getMessage()));
            }
        }
    }

    /**
     * Compensate card activation by deactivating the card
     */
    @Component("UNDO_CARD_ACTIVATION")
    public static class UndoCardActivationHandler implements com.ngoctran.interactionservice.step.StepHandler {

        @Override
        public com.ngoctran.interactionservice.step.StepResult execute(UUID instanceId) {
            log.warn("[COMPENSATION] Undoing card activation for instance: {}", instanceId);

            try {
                // In real implementation:
                // 1. Call card service to deactivate card
                // 2. Block the card number
                // 3. Notify card provider

                log.info("[COMPENSATION] Successfully deactivated card for instance: {}", instanceId);
                return com.ngoctran.interactionservice.step.StepResult.success();

            } catch (Exception e) {
                log.error("[COMPENSATION] Failed to deactivate card for instance: {}", instanceId, e);
                return com.ngoctran.interactionservice.step.StepResult.failure(
                        new com.ngoctran.interactionservice.step.StepError(
                                "COMPENSATION_FAILED",
                                com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                                "Failed to undo card activation: " + e.getMessage()));
            }
        }
    }

    /**
     * Compensate email sending by sending cancellation email
     */
    @Component("UNDO_WELCOME_EMAIL")
    public static class UndoWelcomeEmailHandler implements com.ngoctran.interactionservice.step.StepHandler {

        @Override
        public com.ngoctran.interactionservice.step.StepResult execute(UUID instanceId) {
            log.warn("[COMPENSATION] Sending cancellation email for instance: {}", instanceId);

            try {
                // In real implementation:
                // 1. Send "Application Cancelled" email
                // 2. Explain reason for cancellation
                // 3. Provide next steps

                log.info("[COMPENSATION] Successfully sent cancellation email for instance: {}", instanceId);
                return com.ngoctran.interactionservice.step.StepResult.success();

            } catch (Exception e) {
                log.error("[COMPENSATION] Failed to send cancellation email for instance: {}", instanceId, e);
                // Email failure shouldn't block compensation
                return com.ngoctran.interactionservice.step.StepResult.success();
            }
        }
    }
}
