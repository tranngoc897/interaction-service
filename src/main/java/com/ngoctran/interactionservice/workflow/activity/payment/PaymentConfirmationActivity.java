package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Payment Confirmation Activity
 * Confirms and finalizes payment processing
 */
@ActivityInterface
public interface PaymentConfirmationActivity {

    /**
     * Confirm payment completion
     * @param paymentId Unique payment identifier
     * @param transactionId Transaction ID from execution
     * @param executionResult Result from payment execution
     * @return Payment confirmation result
     */
    PaymentConfirmationResult confirmPayment(String paymentId, String transactionId,
                                           PaymentExecutionActivity.PaymentExecutionResult executionResult);

    /**
     * Payment confirmation result DTO
     */
    class PaymentConfirmationResult {
        private boolean confirmed;
        private String confirmationId;
        private String status;
        private String message;
        private ConfirmationDetails details;

        public PaymentConfirmationResult() {}

        public PaymentConfirmationResult(boolean confirmed, String confirmationId, String status,
                                       String message, ConfirmationDetails details) {
            this.confirmed = confirmed;
            this.confirmationId = confirmationId;
            this.status = status;
            this.message = message;
            this.details = details;
        }

        // Getters and setters
        public boolean isConfirmed() { return confirmed; }
        public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

        public String getConfirmationId() { return confirmationId; }
        public void setConfirmationId(String confirmationId) { this.confirmationId = confirmationId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ConfirmationDetails getDetails() { return details; }
        public void setDetails(ConfirmationDetails details) { this.details = details; }
    }

    /**
     * Confirmation details
     */
    class ConfirmationDetails {
        private String notificationSent;
        private String receiptGenerated;
        private String auditLogged;
        private long confirmationTimestamp;

        public ConfirmationDetails() {}

        public ConfirmationDetails(String notificationSent, String receiptGenerated,
                                 String auditLogged, long confirmationTimestamp) {
            this.notificationSent = notificationSent;
            this.receiptGenerated = receiptGenerated;
            this.auditLogged = auditLogged;
            this.confirmationTimestamp = confirmationTimestamp;
        }

        // Getters and setters
        public String getNotificationSent() { return notificationSent; }
        public void setNotificationSent(String notificationSent) { this.notificationSent = notificationSent; }

        public String getReceiptGenerated() { return receiptGenerated; }
        public void setReceiptGenerated(String receiptGenerated) { this.receiptGenerated = receiptGenerated; }

        public String getAuditLogged() { return auditLogged; }
        public void setAuditLogged(String auditLogged) { this.auditLogged = auditLogged; }

        public long getConfirmationTimestamp() { return confirmationTimestamp; }
        public void setConfirmationTimestamp(long confirmationTimestamp) { this.confirmationTimestamp = confirmationTimestamp; }
    }
}
