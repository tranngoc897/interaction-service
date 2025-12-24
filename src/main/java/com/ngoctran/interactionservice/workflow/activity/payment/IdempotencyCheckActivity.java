package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Idempotency Check Activity
 * Prevents duplicate payment processing
 */
@ActivityInterface
public interface IdempotencyCheckActivity {

    /**
     * Check if payment request is idempotent
     */
    IdempotencyResult checkPaymentIdempotency(String paymentId, String idempotencyKey, String accountId, double amount);

    /**
     * Idempotency result DTO
     */
    class IdempotencyResult {
        private boolean isIdempotent;
        private String existingPaymentId;
        private String status;
        private String errorMessage;

        public IdempotencyResult() {}

        public IdempotencyResult(boolean isIdempotent, String existingPaymentId, String status, String errorMessage) {
            this.isIdempotent = isIdempotent;
            this.existingPaymentId = existingPaymentId;
            this.status = status;
            this.errorMessage = errorMessage;
        }

        // Getters and setters
        public boolean isIdempotent() { return isIdempotent; }
        public void setIdempotent(boolean isIdempotent) { this.isIdempotent = isIdempotent; }

        public String getExistingPaymentId() { return existingPaymentId; }
        public void setExistingPaymentId(String existingPaymentId) { this.existingPaymentId = existingPaymentId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
