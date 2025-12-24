package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Payment Execution Activity
 * Executes the actual payment transaction
 */
@ActivityInterface
public interface PaymentExecutionActivity {

    /**
     * Execute payment transaction
     * @param paymentId Unique payment identifier
     * @param accountId Account making the payment
     * @param amount Payment amount
     * @param currency Payment currency
     * @return Payment execution result
     */
    PaymentExecutionResult executePayment(String paymentId, String accountId, double amount, String currency);

    /**
     * Payment execution result DTO
     */
    class PaymentExecutionResult {
        private boolean success;
        private String transactionId;
        private String processingTime;
        private String status;
        private String errorMessage;
        private PaymentDetails details;

        public PaymentExecutionResult() {}

        public PaymentExecutionResult(boolean success, String transactionId, String processingTime,
                                    String status, String errorMessage, PaymentDetails details) {
            this.success = success;
            this.transactionId = transactionId;
            this.processingTime = processingTime;
            this.status = status;
            this.errorMessage = errorMessage;
            this.details = details;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getProcessingTime() { return processingTime; }
        public void setProcessingTime(String processingTime) { this.processingTime = processingTime; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public PaymentDetails getDetails() { return details; }
        public void setDetails(PaymentDetails details) { this.details = details; }
    }

    /**
     * Payment execution details
     */
    class PaymentDetails {
        private String bankReference;
        private String processingNode;
        private long processingTimestamp;
        private String settlementDate;

        public PaymentDetails() {}

        public PaymentDetails(String bankReference, String processingNode,
                            long processingTimestamp, String settlementDate) {
            this.bankReference = bankReference;
            this.processingNode = processingNode;
            this.processingTimestamp = processingTimestamp;
            this.settlementDate = settlementDate;
        }

        // Getters and setters
        public String getBankReference() { return bankReference; }
        public void setBankReference(String bankReference) { this.bankReference = bankReference; }

        public String getProcessingNode() { return processingNode; }
        public void setProcessingNode(String processingNode) { this.processingNode = processingNode; }

        public long getProcessingTimestamp() { return processingTimestamp; }
        public void setProcessingTimestamp(long processingTimestamp) { this.processingTimestamp = processingTimestamp; }

        public String getSettlementDate() { return settlementDate; }
        public void setSettlementDate(String settlementDate) { this.settlementDate = settlementDate; }
    }
}
