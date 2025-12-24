package com.ngoctran.interactionservice.workflow.payment;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Payment Monitor Workflow Interface
 * Monitors payment processing activities and handles scheduled tasks
 */
@WorkflowInterface
public interface PaymentMonitorWorkflow {

    /**
     * Main workflow method for monitoring payment activities
     * @param accountId Account to monitor
     * @param iterationCount Current iteration count for the monitor
     */
    @WorkflowMethod
    void monitorPayments(String accountId, int iterationCount);

    /**
     * Signal to update monitor status
     * @param status New status for the monitor
     */
    @SignalMethod
    void updateStatus(String status);

    /**
     * Signal to trigger payment processing check
     * @param paymentBatchId Batch identifier for payment processing
     */
    @SignalMethod
    void triggerPaymentCheck(String paymentBatchId);
}
