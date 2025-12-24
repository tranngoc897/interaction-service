package com.ngoctran.interactionservice.workflow.payment;

import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Payment Monitor Workflow Implementation
 * Monitors payment processing activities and handles scheduled tasks
 */
public class PaymentMonitorWorkflowImpl implements PaymentMonitorWorkflow {

    private static final Logger log = Workflow.getLogger(PaymentMonitorWorkflowImpl.class);
    // Configurable threshold - can be overridden for testing
    private static final int CONTINUE_AS_NEW_THRESHOLD = Integer.parseInt(
        System.getProperty("payment.monitor.threshold", "10")
    );

    // Configurable wait duration - can be overridden for testing
    private static final Duration WAIT_DURATION = Duration.ofMillis(Long.parseLong(
        System.getProperty("payment.monitor.wait.duration", "3600000") // 1 hour default
    ));

    private String lastStatus = "STARTED";
    private int eventCount = 0;
    private boolean exit = false;
    private String pendingPaymentBatchId = null; // Changed to pending to indicate it needs processing

    @Override
    public void monitorPayments(String accountId, int iterationCount) {
        log.info("Monitoring Payments for Account: {} | Iteration: {} | Current History Size: {}",
                accountId, iterationCount, Workflow.getInfo().getHistorySize());

        // Event loop for monitoring payment activities
        while (!exit) {
            try {
                // Wait for a signal, pending batch, or timeout (configurable for testing)
                boolean signalReceived = Workflow.await(WAIT_DURATION,
                        () -> eventCount >= CONTINUE_AS_NEW_THRESHOLD || exit || pendingPaymentBatchId != null);

                if (exit) {
                    log.info("Payment Monitor for account {} exiting", accountId);
                    break;
                }

                // Process pending payment batch if triggered by signal
                if (pendingPaymentBatchId != null) {
                    String batchId = pendingPaymentBatchId;
                    pendingPaymentBatchId = null; // Clear the flag
                    processPaymentBatch(batchId);
                }
                // If no signal received within timeout, perform scheduled payment checks
                else if (!signalReceived) {
                    performScheduledPaymentChecks(accountId);
                }

                // CRITICAL: ContinueAsNew Logic
                // When history grows too large, restart the workflow with fresh history
                if (eventCount >= CONTINUE_AS_NEW_THRESHOLD) {
                    log.info("History threshold reached for account {}. Continuing as New...", accountId);
                    // Reset history but continue monitoring with updated iteration count
                    Workflow.continueAsNew(accountId, iterationCount + 1);
                    // Code after continueAsNew is never executed
                    return;
                }

            } catch (Exception e) {
                log.error("Error in payment monitoring loop for account {}: {}", accountId, e.getMessage(), e);
                // Continue monitoring despite errors
                Workflow.sleep(Duration.ofMinutes(5)); // Brief pause before continuing
            }
        }
    }

    @Override
    public void updateStatus(String status) {
        this.lastStatus = status;
        this.eventCount++;
        log.info("Received payment monitor status update: {}. Total events in this iteration: {}", status, eventCount);
    }

    @Override
    public void triggerPaymentCheck(String paymentBatchId) {
        this.pendingPaymentBatchId = paymentBatchId; // Just set the flag, don't process here
        this.eventCount++;
        log.info("Payment check triggered for batch: {}. Will be processed in main loop. Total events: {}",
                paymentBatchId, eventCount);
    }

    // ==================== Private Helper Methods ====================

    private void performScheduledPaymentChecks(String accountId) {
        log.info("Performing scheduled payment checks for account: {}", accountId);

        try {
            // Simulate scheduled payment monitoring activities
            // In real implementation, this would:
            // 1. Check for pending payments
            // 2. Validate payment statuses
            // 3. Process scheduled transfers
            // 4. Send payment reminders
            // 5. Handle failed payment retries

            checkPendingPayments(accountId);
            validatePaymentStatuses(accountId);
            processScheduledTransfers(accountId);

            log.info("Scheduled payment checks completed for account: {}", accountId);

        } catch (Exception e) {
            log.error("Error during scheduled payment checks for account {}: {}", accountId, e.getMessage(), e);
        }
    }

    private void checkPendingPayments(String accountId) {
        // Simulate checking for pending payments that need processing
        log.debug("Checking pending payments for account: {}", accountId);
        // Implementation would query payment database/service
    }

    private void validatePaymentStatuses(String accountId) {
        // Simulate validating payment statuses and handling discrepancies
        log.debug("Validating payment statuses for account: {}", accountId);
        // Implementation would cross-check payment statuses with banking systems
    }

    private void processScheduledTransfers(String accountId) {
        // Simulate processing scheduled/recurring transfers
        log.debug("Processing scheduled transfers for account: {}", accountId);
        // Implementation would execute pending scheduled payments
    }

    private void processPaymentBatch(String paymentBatchId) {
        log.info("Processing payment batch: {}", paymentBatchId);

        try {
            // Simulate batch payment processing
            // In real implementation, this would:
            // 1. Retrieve batch details
            // 2. Validate all payments in batch
            // 3. Process payments
            // 4. Update batch status
            // 5. Send notifications

            validatePaymentBatch(paymentBatchId);
            executeBatchPayments(paymentBatchId);
            updateBatchStatus(paymentBatchId, "COMPLETED");

            log.info("Payment batch {} processed successfully", paymentBatchId);

        } catch (Exception e) {
            log.error("Failed to process payment batch {}: {}", paymentBatchId, e.getMessage(), e);
            updateBatchStatus(paymentBatchId, "FAILED");
        }
    }

    private void validatePaymentBatch(String paymentBatchId) {
        // Simulate batch validation
        log.debug("Validating payment batch: {}", paymentBatchId);
    }

    private void executeBatchPayments(String paymentBatchId) {
        // Simulate batch execution
        log.debug("Executing payments in batch: {}", paymentBatchId);
        Workflow.sleep(Duration.ofSeconds(2)); // Simulate processing time
    }

    private void updateBatchStatus(String paymentBatchId, String status) {
        // Simulate status update
        log.debug("Updating batch {} status to: {}", paymentBatchId, status);
    }
}
