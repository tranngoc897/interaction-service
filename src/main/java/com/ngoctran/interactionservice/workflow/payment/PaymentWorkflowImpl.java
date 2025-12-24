package com.ngoctran.interactionservice.workflow.payment;

import com.ngoctran.interactionservice.workflow.activity.payment.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Payment Processing Workflow Implementation
 * Handles banking retail payment scheduling and processing using separate activities
 */
public class PaymentWorkflowImpl implements PaymentWorkflow {

    // Temporal logger (required for workflows)
    private static final Logger log = Workflow.getLogger(PaymentWorkflowImpl.class);

    // Workflow state
    private String currentStatus = "INITIALIZED";
    private PaymentProgress progress = new PaymentProgress("INITIALIZED", 0, "INITIALIZED", "");
    private String paymentId;
    private String accountId;
    private double amount;
    private String currency;

    // Activity stubs with retry policies
    private final PaymentValidationActivity paymentValidation;
    private final AccountVerificationActivity accountVerification;
    private final FraudDetectionActivity fraudDetection;
    private final PaymentExecutionActivity paymentExecution;
    private final PaymentConfirmationActivity paymentConfirmation;

    public PaymentWorkflowImpl() {
        // Configure activity options with retries
        ActivityOptions defaultOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(10))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();

        // Create activity stubs
        this.paymentValidation = Workflow.newActivityStub(PaymentValidationActivity.class, defaultOptions);
        this.accountVerification = Workflow.newActivityStub(AccountVerificationActivity.class, defaultOptions);
        this.fraudDetection = Workflow.newActivityStub(FraudDetectionActivity.class, defaultOptions);
        this.paymentExecution = Workflow.newActivityStub(PaymentExecutionActivity.class, defaultOptions);
        this.paymentConfirmation = Workflow.newActivityStub(PaymentConfirmationActivity.class, defaultOptions);
    }

    @Override
    public void processPayment(String paymentId, String accountId, double amount, String currency) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;

        log.info("Starting Payment Processing Workflow - Payment ID: {}, Account: {}, Amount: {} {}",
                paymentId, accountId, amount, currency);

        try {
            // Step 1: Validate payment details
            updateProgress("VALIDATING_PAYMENT", 20, "VALIDATING");
            validatePaymentDetails();

            // Step 2: Check account balance and status
            updateProgress("CHECKING_ACCOUNT", 40, "VALIDATING");
            checkAccountStatus();

            // Step 3: Fraud detection and risk assessment
            updateProgress("FRAUD_CHECK", 60, "PROCESSING");
            performFraudCheck();

            // Step 4: Process the payment
            updateProgress("PROCESSING_PAYMENT", 80, "PROCESSING");
            executePayment();

            // Step 5: Confirm and complete
            updateProgress("COMPLETING_PAYMENT", 100, "COMPLETED");
            confirmPayment();

            log.info("Payment processing completed successfully - Payment ID: {}", paymentId);
            currentStatus = "COMPLETED";

        } catch (Exception e) {
            log.error("Payment processing failed - Payment ID: {}", paymentId, e);
            currentStatus = "FAILED";
            handlePaymentFailure(e);
            throw e;
        }
    }

    @Override
    public String getStatus() {
        return currentStatus;
    }

    @Override
    public PaymentProgress getProgress() {
        return progress;
    }

    // ==================== Private Helper Methods ====================

    private void validatePaymentDetails() {
        log.info("Calling PaymentValidationActivity for payment: {}", paymentId);

        PaymentValidationActivity.ValidationResult result =
                paymentValidation.validatePayment(paymentId, accountId, amount, currency);

        if (!result.isValid()) {
            throw new IllegalArgumentException("Payment validation failed: " + result.getErrorMessage());
        }

        log.info("Payment validation completed successfully");
    }

    private void checkAccountStatus() {
        log.info("Calling AccountVerificationActivity for account: {}", accountId);

        AccountVerificationActivity.AccountVerificationResult result =
                accountVerification.verifyAccount(accountId, amount, currency);

        if (!result.isVerificationPassed()) {
            throw new RuntimeException("Account verification failed: " + result.getErrorMessage());
        }

        log.info("Account verification completed successfully");
    }

    private void performFraudCheck() {
        log.info("Calling FraudDetectionActivity for payment: {}", paymentId);

        // Get account verification result for fraud detection
        AccountVerificationActivity.AccountVerificationResult accountResult =
                accountVerification.verifyAccount(accountId, amount, currency);

        FraudDetectionActivity.FraudDetectionResult result =
                fraudDetection.detectFraud(paymentId, accountId, amount, currency, accountResult);

        if (!result.isApproved()) {
            throw new RuntimeException("Fraud detection failed: " + result.getRecommendation());
        }

        log.info("Fraud detection completed successfully: score={}, level={}",
                result.getRiskScore(), result.getRiskLevel());
    }

    private void executePayment() {
        log.info("Calling PaymentExecutionActivity for payment: {}", paymentId);

        PaymentExecutionActivity.PaymentExecutionResult result =
                paymentExecution.executePayment(paymentId, accountId, amount, currency);

        if (!result.isSuccess()) {
            throw new RuntimeException("Payment execution failed: " + result.getErrorMessage());
        }

        log.info("Payment execution completed successfully: transactionId={}", result.getTransactionId());
    }

    private void confirmPayment() {
        log.info("Calling PaymentConfirmationActivity for payment: {}", paymentId);

        // Get execution result for confirmation
        PaymentExecutionActivity.PaymentExecutionResult executionResult =
                paymentExecution.executePayment(paymentId, accountId, amount, currency);

        PaymentConfirmationActivity.PaymentConfirmationResult result =
                paymentConfirmation.confirmPayment(paymentId, executionResult.getTransactionId(), executionResult);

        if (!result.isConfirmed()) {
            log.warn("Payment confirmation failed: {}", result.getMessage());
            // Don't throw exception here as payment was successful, just log the issue
        } else {
            log.info("Payment confirmation completed successfully: confirmationId={}", result.getConfirmationId());
        }
    }

    private void handlePaymentFailure(Exception e) {
        log.error("Handling payment failure for payment: {}", paymentId, e);

        // In real implementation, this would:
        // 1. Rollback any partial transactions
        // 2. Update payment status to FAILED
        // 3. Send failure notifications
        // 4. Log for audit purposes

        updateProgress("PAYMENT_FAILED", 0, "FAILED");
    }

    private void updateProgress(String step, int progressPercentage, String status) {
        this.currentStatus = status;
        this.progress = new PaymentProgress(
                step,
                progressPercentage,
                status,
                java.time.LocalDateTime.now().toString()
        );
        log.info("Payment Progress: {}% - {} - {}", progressPercentage, step, status);
    }
}
