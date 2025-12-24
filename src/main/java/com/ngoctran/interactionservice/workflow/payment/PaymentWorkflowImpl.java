package com.ngoctran.interactionservice.workflow.payment;

import com.ngoctran.interactionservice.workflow.activity.payment.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

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

    // Activity stubs with different retry policies for different operations
    private final IdempotencyCheckActivity idempotencyCheck;
    private final PaymentValidationActivity paymentValidation;
    private final AccountVerificationActivity accountVerification;
    private final ComplianceCheckActivity complianceCheck;
    private final FraudDetectionActivity fraudDetection;
    private final PaymentRoutingActivity paymentRouting;
    private final PaymentExecutionActivity paymentExecution;
    private final PaymentConfirmationActivity paymentConfirmation;
    private final PaymentCompensationActivity paymentCompensation;

    // Circuit breaker state (workflow-level state)
    private boolean circuitBreakerOpen = false;
    private int consecutiveFailures = 0;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;

    // Advanced scheduler features (static instance for workflow compatibility)
    private static final PaymentSchedulerAdvanced advancedScheduler = new PaymentSchedulerAdvanced();

    // Workflow state for advanced features
    private String tenantId = "DEFAULT_TENANT";
    private String userId = "DEFAULT_USER";

    public PaymentWorkflowImpl() {
        // Configure different activity options for different operations

        // Idempotency check - no retries, fast failure
        ActivityOptions idempotencyOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(1) // No retries for idempotency checks
                        .build())
                .build();

        // Validation activities - minimal retries
        ActivityOptions validationOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(2)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .build())
                .build();

        // Execution activities - higher retries with circuit breaker awareness
        ActivityOptions executionOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setMaximumInterval(Duration.ofSeconds(30))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();

        // Create activity stubs
        this.idempotencyCheck = Workflow.newActivityStub(IdempotencyCheckActivity.class, idempotencyOptions);
        this.paymentValidation = Workflow.newActivityStub(PaymentValidationActivity.class, validationOptions);
        this.accountVerification = Workflow.newActivityStub(AccountVerificationActivity.class, validationOptions);
        this.complianceCheck = Workflow.newActivityStub(ComplianceCheckActivity.class, validationOptions);
        this.fraudDetection = Workflow.newActivityStub(FraudDetectionActivity.class, validationOptions);
        this.paymentRouting = Workflow.newActivityStub(PaymentRoutingActivity.class, validationOptions);
        this.paymentExecution = Workflow.newActivityStub(PaymentExecutionActivity.class, executionOptions);
        this.paymentConfirmation = Workflow.newActivityStub(PaymentConfirmationActivity.class, validationOptions);
        this.paymentCompensation = Workflow.newActivityStub(PaymentCompensationActivity.class, executionOptions);
    }

    @Override
    public void processPayment(String paymentId, String accountId, double amount, String currency) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;

        log.info("Starting Advanced Payment Processing Workflow - Payment ID: {}, Account: {}, Amount: {} {}",
                paymentId, accountId, amount, currency);

        try {
            // Record payment submission event
            advancedScheduler.recordPaymentEvent(paymentId, PaymentSchedulerAdvanced.EventType.PAYMENT_SUBMITTED,
                    Map.of("amount", amount, "currency", currency, "accountId", accountId,
                           "tenantId", tenantId, "userId", userId));

            // Step 0: Rate Limiting Check (Advanced Feature)
            updateProgress("CHECKING_RATE_LIMITS", 2, "INITIALIZING");
            if (!advancedScheduler.canProcessPayment(tenantId, userId)) {
                throw new RuntimeException("Rate limit exceeded for tenant: " + tenantId + ", user: " + userId);
            }

            // Step 0.5: Idempotency Check
            updateProgress("CHECKING_IDEMPOTENCY", 5, "INITIALIZING");
            checkIdempotency();

            // Step 1: Payment Validation
            updateProgress("VALIDATING_PAYMENT", 15, "VALIDATING");
            validatePaymentDetails();

            // Record validation event
            advancedScheduler.recordPaymentEvent(paymentId, PaymentSchedulerAdvanced.EventType.PAYMENT_VALIDATED,
                    Map.of("validationResult", "SUCCESS"));

            // Step 2: Account Verification
            updateProgress("VERIFYING_ACCOUNT", 25, "VALIDATING");
            checkAccountStatus();

            // Step 3: Compliance Check
            updateProgress("COMPLIANCE_CHECK", 35, "VALIDATING");
            performComplianceCheck();

            // Step 4: Fraud Detection
            updateProgress("FRAUD_DETECTION", 50, "PROCESSING");
            performFraudCheck();

            // Step 5: Payment Routing
            updateProgress("ROUTING_PAYMENT", 60, "PROCESSING");
            routePayment();

            // Step 6: Circuit Breaker Check
            updateProgress("CHECKING_CIRCUIT_BREAKER", 65, "PROCESSING");
            checkCircuitBreaker();

            // Step 7: Execute Payment (with Circuit Breaker & Saga Pattern)
            updateProgress("EXECUTING_PAYMENT", 80, "PROCESSING");

            // Record execution start event
            advancedScheduler.recordPaymentEvent(paymentId, PaymentSchedulerAdvanced.EventType.PAYMENT_EXECUTED,
                    Map.of("executionStarted", "true"));

            PaymentExecutionActivity.PaymentExecutionResult executionResult = executePayment();

            // Record successful execution
            if (executionResult.isSuccess()) {
                advancedScheduler.recordPaymentEvent(paymentId, PaymentSchedulerAdvanced.EventType.PAYMENT_COMPLETED,
                        Map.of("transactionId", executionResult.getTransactionId(),
                              "processingTime", executionResult.getProcessingTime()));
            }

            // Step 8: Confirm Payment
            updateProgress("CONFIRMING_PAYMENT", 95, "FINALIZING");
            confirmPayment(executionResult);

            // Step 9: Manual Approval for High-Value (if needed)
            if (requiresManualApproval(amount)) {
                updateProgress("AWAITING_MANUAL_APPROVAL", 98, "PENDING_APPROVAL");
                // In real implementation, this would wait for manual approval signal
                log.info("High-value payment {} requires manual approval", paymentId);
            }

            updateProgress("PAYMENT_COMPLETED", 100, "COMPLETED");
            log.info("Advanced payment processing completed successfully - Payment ID: {}", paymentId);
            currentStatus = "COMPLETED";

        } catch (Exception e) {
            log.error("Payment processing failed - Payment ID: {}, initiating compensation", paymentId, e);
            currentStatus = "FAILED";

            // Saga Pattern: Attempt compensation
            performCompensation(e);

            handlePaymentFailure(e);
            throw e;
        }
    }

    // ==================== Advanced Feature Methods ====================

    private void checkIdempotency() {
        log.info("Checking idempotency for payment: {}", paymentId);

        IdempotencyCheckActivity.IdempotencyResult result =
                idempotencyCheck.checkPaymentIdempotency(paymentId, null, accountId, amount);

        if (!result.isIdempotent()) {
            throw new IllegalStateException("Payment request not idempotent: " + result.getErrorMessage());
        }

        log.info("Idempotency check passed");
    }

    private void performComplianceCheck() {
        log.info("Performing compliance check for payment: {}", paymentId);

        ComplianceCheckActivity.ComplianceResult result =
                complianceCheck.checkRegulatoryCompliance(paymentId, accountId, amount, currency, null);

        if (!result.isCompliant()) {
            log.warn("Compliance check failed for payment: {}, violations: {}",
                    paymentId, String.join(", ", result.getViolations()));
            throw new RuntimeException("Regulatory compliance check failed: " +
                    String.join(", ", result.getRequiredActions()));
        }

        log.info("Compliance check passed: riskLevel={}", result.getRiskLevel());
    }

    private void routePayment() {
        log.info("Routing payment: {}", paymentId);

        PaymentRoutingActivity.RoutingDecision decision =
                paymentRouting.routePayment(paymentId, amount, currency, accountId);

        log.info("Payment routed to processor: {} ({}) - reason: {}",
                decision.getProcessorName(), decision.getProcessorType(), decision.getRoutingReason());
    }

    private void checkCircuitBreaker() {
        if (circuitBreakerOpen) {
            log.warn("Circuit breaker is OPEN for payment processing");
            throw new RuntimeException("Payment service temporarily unavailable (circuit breaker open)");
        }
        log.info("Circuit breaker check passed");
    }

    private boolean requiresManualApproval(double amount) {
        // High-value payments require manual approval
        return amount > 10000000; // 10M VND threshold
    }

    private void performCompensation(Exception originalException) {
        log.info("Initiating compensation saga for payment: {}", paymentId);

        try {
            // Step 1: Reverse account debit if it was charged
            if (originalException.getMessage().contains("execution")) {
                PaymentCompensationActivity.CompensationResult debitReversal =
                        paymentCompensation.reverseAccountDebit(accountId, amount, originalException.getMessage());
                log.info("Account debit reversal result: {}", debitReversal.isSuccess() ?
                        "SUCCESS" : "FAILED: " + debitReversal.getErrorMessage());
            }

            // Step 2: Cancel transaction if it was created
            // This would be called if we have a transaction ID

            // Step 3: Log compensation event
            paymentCompensation.logCompensationEvent(paymentId, "PAYMENT_FAILURE_COMPENSATION",
                    "Original error: " + originalException.getMessage());

            log.info("Compensation saga completed for payment: {}", paymentId);

        } catch (Exception compensationException) {
            log.error("Compensation saga failed for payment: {}", paymentId, compensationException);
            // Escalate to manual intervention
            log.error("MANUAL INTERVENTION REQUIRED: Payment {} failed and compensation unsuccessful",
                    paymentId);
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

    private void confirmPayment(PaymentExecutionActivity.PaymentExecutionResult executionResult) {
        log.info("Calling PaymentConfirmationActivity for payment: {}", paymentId);

        PaymentConfirmationActivity.PaymentConfirmationResult result =
                paymentConfirmation.confirmPayment(paymentId, executionResult.getTransactionId(), executionResult);

        if (!result.isConfirmed()) {
            log.warn("Payment confirmation failed: {}", result.getMessage());
            // Don't throw exception here as payment was successful, just log the issue
        } else {
            log.info("Payment confirmation completed successfully: confirmationId={}", result.getConfirmationId());
        }
    }

    private PaymentExecutionActivity.PaymentExecutionResult executePayment() {
        log.info("Calling PaymentExecutionActivity for payment: {}", paymentId);

        PaymentExecutionActivity.PaymentExecutionResult result =
                paymentExecution.executePayment(paymentId, accountId, amount, currency);

        if (!result.isSuccess()) {
            throw new RuntimeException("Payment execution failed: " + result.getErrorMessage());
        }

        log.info("Payment execution completed successfully: transactionId={}", result.getTransactionId());
        return result;
    }

    private void handlePaymentFailure(Exception e) {
        log.error("Handling payment failure for payment: {}", paymentId, e);

        // Record failure event
        advancedScheduler.recordPaymentEvent(paymentId, PaymentSchedulerAdvanced.EventType.PAYMENT_FAILED,
                Map.of("error", e.getMessage(), "errorType", e.getClass().getSimpleName()));

        // Use Dead Letter Queue for failed payments (Advanced Feature)
        advancedScheduler.handlePaymentFailure(paymentId, tenantId, userId, e, 0);

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
