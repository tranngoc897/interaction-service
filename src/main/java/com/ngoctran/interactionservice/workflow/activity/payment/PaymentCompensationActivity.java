package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Payment Compensation Activity
 * Handles rollback operations for failed payments (Saga Pattern)
 */
@ActivityInterface
public interface PaymentCompensationActivity {

    /**
     * Reverse account debit for failed payment
     */
    CompensationResult reverseAccountDebit(String accountId, double amount, String reason);

    /**
     * Cancel payment transaction
     */
    CompensationResult cancelPaymentTransaction(String transactionId, String reason);

    /**
     * Restore account balance
     */
    CompensationResult restoreAccountBalance(String accountId, double amount, String reference);

    /**
     * Log compensation event for audit
     */
    void logCompensationEvent(String paymentId, String compensationType, String details);

    /**
     * Compensation result DTO
     */
    class CompensationResult {
        private boolean success;
        private String compensationId;
        private String status;
        private String errorMessage;
        private long timestamp;

        public CompensationResult() {}

        public CompensationResult(boolean success, String compensationId, String status,
                                String errorMessage, long timestamp) {
            this.success = success;
            this.compensationId = compensationId;
            this.status = status;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getCompensationId() { return compensationId; }
        public void setCompensationId(String compensationId) { this.compensationId = compensationId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
