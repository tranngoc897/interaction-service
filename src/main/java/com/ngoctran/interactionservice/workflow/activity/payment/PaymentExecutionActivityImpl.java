package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Payment Execution Activity Implementation
 * Executes the actual payment transaction
 */
@Component
@Slf4j
public class PaymentExecutionActivityImpl implements PaymentExecutionActivity {

    @Override
    public PaymentExecutionResult executePayment(String paymentId, String accountId, double amount, String currency) {
        log.info("Executing payment: id={}, account={}, amount={}, currency={}",
                paymentId, accountId, amount, currency);

        try {
            // Simulate payment processing delay
            Thread.sleep(2000 + (int)(Math.random() * 3000)); // 2-5 seconds

            // Generate transaction details
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            long processingTimestamp = System.currentTimeMillis();
            String settlementDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Simulate occasional failures (5% failure rate)
            boolean success = Math.random() > 0.05;

            if (success) {
                PaymentDetails details = new PaymentDetails(
                        "BANK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                        "NODE-" + (int)(Math.random() * 10),
                        processingTimestamp,
                        settlementDate
                );

                log.info("Payment executed successfully: transactionId={}", transactionId);
                return new PaymentExecutionResult(true, transactionId, "2.5s", "COMPLETED", null, details);
            } else {
                String errorMessage = "Payment processing failed due to system timeout";
                log.warn("Payment execution failed: {}", errorMessage);
                return new PaymentExecutionResult(false, null, null, "FAILED", errorMessage, null);
            }

        } catch (Exception e) {
            log.error("Payment execution error for payment: {}", paymentId, e);
            return new PaymentExecutionResult(false, null, null, "ERROR",
                    "Payment execution service error: " + e.getMessage(), null);
        }
    }
}
