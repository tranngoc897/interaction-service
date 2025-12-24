package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Payment Routing Activity
 * Routes payments to appropriate processors based on rules
 */
@ActivityInterface
public interface PaymentRoutingActivity {

    /**
     * Route payment to appropriate processor
     */
    RoutingDecision routePayment(String paymentId, double amount, String currency, String accountId);

    /**
     * Routing decision DTO
     */
    class RoutingDecision {
        private String processorName;
        private String processorType;
        private String routingReason;
        private double estimatedFee;
        private long estimatedProcessingTime;
        private String fallbackProcessor;

        public RoutingDecision() {}

        public RoutingDecision(String processorName, String processorType, String routingReason,
                             double estimatedFee, long estimatedProcessingTime, String fallbackProcessor) {
            this.processorName = processorName;
            this.processorType = processorType;
            this.routingReason = routingReason;
            this.estimatedFee = estimatedFee;
            this.estimatedProcessingTime = estimatedProcessingTime;
            this.fallbackProcessor = fallbackProcessor;
        }

        // Getters and setters
        public String getProcessorName() { return processorName; }
        public void setProcessorName(String processorName) { this.processorName = processorName; }

        public String getProcessorType() { return processorType; }
        public void setProcessorType(String processorType) { this.processorType = processorType; }

        public String getRoutingReason() { return routingReason; }
        public void setRoutingReason(String routingReason) { this.routingReason = routingReason; }

        public double getEstimatedFee() { return estimatedFee; }
        public void setEstimatedFee(double estimatedFee) { this.estimatedFee = estimatedFee; }

        public long getEstimatedProcessingTime() { return estimatedProcessingTime; }
        public void setEstimatedProcessingTime(long estimatedProcessingTime) { this.estimatedProcessingTime = estimatedProcessingTime; }

        public String getFallbackProcessor() { return fallbackProcessor; }
        public void setFallbackProcessor(String fallbackProcessor) { this.fallbackProcessor = fallbackProcessor; }
    }
}
