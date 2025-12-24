package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency Check Activity Implementation
 * Prevents duplicate payment processing
 */
@Component
@Slf4j
public class IdempotencyCheckActivityImpl implements IdempotencyCheckActivity {

    // Mock idempotency store (in real implementation, use Redis or database)
    private static final Map<String, IdempotencyRecord> IDEMPOTENCY_STORE = new ConcurrentHashMap<>();

    @Override
    public IdempotencyResult checkPaymentIdempotency(String paymentId, String idempotencyKey,
                                                   String accountId, double amount) {
        log.info("Checking idempotency: paymentId={}, key={}, account={}, amount={}",
                paymentId, idempotencyKey, accountId, amount);

        try {
            // Check by idempotency key first (more reliable)
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                IdempotencyRecord existing = IDEMPOTENCY_STORE.get(idempotencyKey);
                if (existing != null) {
                    log.warn("Duplicate request detected by idempotency key: {}", idempotencyKey);

                    // Check if it's the same payment parameters
                    if (existing.accountId.equals(accountId) && existing.amount == amount) {
                        return new IdempotencyResult(false, existing.paymentId, "DUPLICATE_REQUEST",
                                "Duplicate payment request detected");
                    } else {
                        return new IdempotencyResult(false, null, "CONFLICT",
                                "Idempotency key used for different payment parameters");
                    }
                }
            }

            // Check by payment ID
            IdempotencyRecord existingByPaymentId = IDEMPOTENCY_STORE.values().stream()
                    .filter(record -> record.paymentId.equals(paymentId))
                    .findFirst()
                    .orElse(null);

            if (existingByPaymentId != null) {
                log.warn("Payment ID already exists: {}", paymentId);
                return new IdempotencyResult(false, paymentId, "PAYMENT_EXISTS",
                        "Payment ID already processed");
            }

            // Store idempotency record
            IdempotencyRecord record = new IdempotencyRecord(paymentId, idempotencyKey, accountId, amount);
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                IDEMPOTENCY_STORE.put(idempotencyKey, record);
            }

            log.info("Idempotency check passed for payment: {}", paymentId);
            return new IdempotencyResult(true, null, "ALLOWED", null);

        } catch (Exception e) {
            log.error("Idempotency check failed: paymentId={}", paymentId, e);
            // Allow payment to proceed on idempotency check failure
            return new IdempotencyResult(true, null, "CHECK_FAILED",
                    "Idempotency check failed, proceeding: " + e.getMessage());
        }
    }

    /**
     * Mock idempotency record
     */
    private static class IdempotencyRecord {
        final String paymentId;
        final String idempotencyKey;
        final String accountId;
        final double amount;
        final long timestamp;

        IdempotencyRecord(String paymentId, String idempotencyKey, String accountId, double amount) {
            this.paymentId = paymentId;
            this.idempotencyKey = idempotencyKey;
            this.accountId = accountId;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
