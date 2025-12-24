package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Payment Confirmation Activity Implementation
 * Confirms and finalizes payment processing
 */
@Component
@Slf4j
public class PaymentConfirmationActivityImpl implements PaymentConfirmationActivity {

    @Override
    public PaymentConfirmationResult confirmPayment(String paymentId, String transactionId,
                                                  PaymentExecutionActivity.PaymentExecutionResult executionResult) {
        log.info("Confirming payment: id={}, transactionId={}", paymentId, transactionId);

        try {
            // Check if execution was successful
            if (!executionResult.isSuccess()) {
                log.warn("Cannot confirm failed payment: {}", paymentId);
                return new PaymentConfirmationResult(false, null, "FAILED",
                        "Payment execution failed, cannot confirm", null);
            }

            // Generate confirmation ID
            String confirmationId = "CONF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Simulate confirmation steps
            String notificationSent = sendConfirmationNotification(paymentId, transactionId);
            String receiptGenerated = generatePaymentReceipt(paymentId, transactionId, executionResult);
            String auditLogged = logPaymentAudit(paymentId, transactionId, executionResult);

            ConfirmationDetails details = new ConfirmationDetails(
                    notificationSent,
                    receiptGenerated,
                    auditLogged,
                    System.currentTimeMillis()
            );

            log.info("Payment confirmed successfully: confirmationId={}, paymentId={}", confirmationId, paymentId);

            return new PaymentConfirmationResult(true, confirmationId, "CONFIRMED",
                    "Payment confirmed successfully", details);

        } catch (Exception e) {
            log.error("Payment confirmation failed for payment: {}", paymentId, e);
            return new PaymentConfirmationResult(false, null, "ERROR",
                    "Confirmation failed: " + e.getMessage(), null);
        }
    }

    /**
     * Send confirmation notification
     */
    private String sendConfirmationNotification(String paymentId, String transactionId) {
        // Simulate sending notification (email, SMS, push notification)
        log.debug("Sending confirmation notification for payment: {}", paymentId);
        return "NOTIFICATION_SENT";
    }

    /**
     * Generate payment receipt
     */
    private String generatePaymentReceipt(String paymentId, String transactionId,
                                        PaymentExecutionActivity.PaymentExecutionResult executionResult) {
        // Simulate generating PDF receipt or receipt record
        log.debug("Generating payment receipt for payment: {}", paymentId);
        return "RECEIPT_GENERATED";
    }

    /**
     * Log payment audit trail
     */
    private String logPaymentAudit(String paymentId, String transactionId,
                                 PaymentExecutionActivity.PaymentExecutionResult executionResult) {
        // Simulate logging to audit system
        log.debug("Logging payment audit for payment: {}", paymentId);
        return "AUDIT_LOGGED";
    }
}
