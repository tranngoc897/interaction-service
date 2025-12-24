package com.ngoctran.interactionservice.payment;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Payment Processing Workflow Interface
 * Handles banking retail payment scheduling and processing
 */
@WorkflowInterface
public interface PaymentWorkflow {

    /**
     * Main workflow method for payment processing
     */
    @WorkflowMethod
    void processPayment(String paymentId, String accountId, double amount, String currency);

    /**
     * Query current payment status
     */
    String getStatus();

    /**
     * Query payment progress
     */
    PaymentProgress getProgress();

    // DTOs
    class PaymentProgress {
        private String currentStep;
        private int progressPercentage;
        private String status;
        private String lastUpdated;

        public PaymentProgress() {}

        public PaymentProgress(String currentStep, int progressPercentage, String status, String lastUpdated) {
            this.currentStep = currentStep;
            this.progressPercentage = progressPercentage;
            this.status = status;
            this.lastUpdated = lastUpdated;
        }

        // Getters and setters
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }

        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
