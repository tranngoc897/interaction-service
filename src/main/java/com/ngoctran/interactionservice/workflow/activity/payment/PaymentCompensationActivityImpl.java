package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Payment Compensation Activity Implementation
 * Handles rollback operations for failed payments (Saga Pattern)
 */
@Component
@Slf4j
public class PaymentCompensationActivityImpl implements PaymentCompensationActivity {

    // Mock compensation tracking
    private static final ConcurrentHashMap<String, CompensationRecord> COMPENSATION_LOG = new ConcurrentHashMap<>();

    @Override
    public CompensationResult reverseAccountDebit(String accountId, double amount, String reason) {
        log.info("Reversing account debit: account={}, amount={}, reason={}", accountId, amount, reason);

        try {
            // In real implementation, this would:
            // 1. Call banking API to reverse the debit
            // 2. Update account balance
            // 3. Create reversal transaction record

            String compensationId = "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Simulate API call delay
            Thread.sleep(500);

            // Log compensation
            CompensationRecord record = new CompensationRecord(compensationId, "ACCOUNT_DEBIT_REVERSAL",
                    accountId, amount, reason, System.currentTimeMillis());
            COMPENSATION_LOG.put(compensationId, record);

            log.info("Account debit reversal completed: compensationId={}", compensationId);
            return new CompensationResult(true, compensationId, "COMPLETED", null, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Account debit reversal failed: account={}, amount={}", accountId, amount, e);
            return new CompensationResult(false, null, "FAILED",
                    "Reversal failed: " + e.getMessage(), System.currentTimeMillis());
        }
    }

    @Override
    public CompensationResult cancelPaymentTransaction(String transactionId, String reason) {
        log.info("Cancelling payment transaction: transactionId={}, reason={}", transactionId, reason);

        try {
            // In real implementation, this would:
            // 1. Call payment processor to cancel transaction
            // 2. Update transaction status to CANCELLED
            // 3. Notify downstream systems

            String compensationId = "CANCEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Simulate API call delay
            Thread.sleep(300);

            // Log compensation
            CompensationRecord record = new CompensationRecord(compensationId, "TRANSACTION_CANCELLATION",
                    transactionId, 0.0, reason, System.currentTimeMillis());
            COMPENSATION_LOG.put(compensationId, record);

            log.info("Transaction cancellation completed: compensationId={}", compensationId);
            return new CompensationResult(true, compensationId, "COMPLETED", null, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Transaction cancellation failed: transactionId={}", transactionId, e);
            return new CompensationResult(false, null, "FAILED",
                    "Cancellation failed: " + e.getMessage(), System.currentTimeMillis());
        }
    }

    @Override
    public CompensationResult restoreAccountBalance(String accountId, double amount, String reference) {
        log.info("Restoring account balance: account={}, amount={}, reference={}", accountId, amount, reference);

        try {
            // In real implementation, this would:
            // 1. Query current account balance
            // 2. Add the compensation amount
            // 3. Create adjustment transaction

            String compensationId = "RESTORE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Simulate API call delay
            Thread.sleep(400);

            // Log compensation
            CompensationRecord record = new CompensationRecord(compensationId, "BALANCE_RESTORATION",
                    accountId, amount, reference, System.currentTimeMillis());
            COMPENSATION_LOG.put(compensationId, record);

            log.info("Balance restoration completed: compensationId={}", compensationId);
            return new CompensationResult(true, compensationId, "COMPLETED", null, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Balance restoration failed: account={}, amount={}", accountId, amount, e);
            return new CompensationResult(false, null, "FAILED",
                    "Restoration failed: " + e.getMessage(), System.currentTimeMillis());
        }
    }

    @Override
    public void logCompensationEvent(String paymentId, String compensationType, String details) {
        log.info("Logging compensation event: paymentId={}, type={}, details={}",
                paymentId, compensationType, details);

        try {
            // In real implementation, this would:
            // 1. Write to audit database
            // 2. Send to monitoring systems
            // 3. Trigger compliance alerts if needed

            String eventId = "EVENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            CompensationRecord record = new CompensationRecord(eventId, compensationType,
                    paymentId, 0.0, details, System.currentTimeMillis());
            COMPENSATION_LOG.put(eventId, record);

            log.info("Compensation event logged: eventId={}", eventId);

        } catch (Exception e) {
            log.error("Failed to log compensation event: paymentId={}", paymentId, e);
            // Don't throw exception for logging failures
        }
    }

    /**
     * Mock compensation record
     */
    private static class CompensationRecord {
        final String id;
        final String type;
        final String reference;
        final double amount;
        final String reason;
        final long timestamp;

        CompensationRecord(String id, String type, String reference, double amount, String reason, long timestamp) {
            this.id = id;
            this.type = type;
            this.reference = reference;
            this.amount = amount;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}
