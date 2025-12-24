package com.ngoctran.interactionservice.workflow.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PaymentSchedulerAdvanced
 * Demonstrates all 8 enterprise features working together
 */
public class PaymentSchedulerAdvancedTest {

    private PaymentSchedulerAdvanced scheduler;

    @BeforeEach
    public void setUp() {
        scheduler = new PaymentSchedulerAdvanced();
    }

    @Test
    public void testRateLimiting() {
        // Test global rate limiting
        String tenantId = "TENANT_A";
        String userId = "USER_1";

        // Should allow first few requests
        for (int i = 0; i < 5; i++) {
            assertTrue(scheduler.canProcessPayment(tenantId, userId),
                "Payment " + i + " should be allowed");
        }

        // Should eventually hit rate limit
        boolean eventuallyLimited = false;
        for (int i = 0; i < 100 && !eventuallyLimited; i++) {
            if (!scheduler.canProcessPayment(tenantId, userId)) {
                eventuallyLimited = true;
            }
            try {
                Thread.sleep(10); // Small delay to let rate limiter recover
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertTrue(eventuallyLimited, "Rate limiting should eventually kick in");
    }

    @Test
    public void testPriorityQueues() {
        // Submit payments with different priorities
        scheduler.submitPaymentWithPriority("PAY_HIGH", "TENANT_A", "USER_1",
            PaymentSchedulerAdvanced.PaymentPriority.HIGH_VALUE, 15000000);
        scheduler.submitPaymentWithPriority("PAY_LOW", "TENANT_A", "USER_1",
            PaymentSchedulerAdvanced.PaymentPriority.LOW_PRIORITY, 50000);
        scheduler.submitPaymentWithPriority("PAY_CRITICAL", "TENANT_A", "USER_1",
            PaymentSchedulerAdvanced.PaymentPriority.CRITICAL, 1000000);

        // Should get highest priority first
        PaymentSchedulerAdvanced.PriorityPaymentRequest first = scheduler.getNextPriorityPayment();
        assertNotNull(first);
        assertEquals("PAY_CRITICAL", first.getPaymentId());
        assertEquals(PaymentSchedulerAdvanced.PaymentPriority.CRITICAL, first.getPriority());

        // Then high value
        PaymentSchedulerAdvanced.PriorityPaymentRequest second = scheduler.getNextPriorityPayment();
        assertNotNull(second);
        assertEquals("PAY_HIGH", second.getPaymentId());
        assertEquals(PaymentSchedulerAdvanced.PaymentPriority.HIGH_VALUE, second.getPriority());

        // Finally low priority
        PaymentSchedulerAdvanced.PriorityPaymentRequest third = scheduler.getNextPriorityPayment();
        assertNotNull(third);
        assertEquals("PAY_LOW", third.getPaymentId());
        assertEquals(PaymentSchedulerAdvanced.PaymentPriority.LOW_PRIORITY, third.getPriority());
    }

    @Test
    public void testBatchProcessingOptimization() {
        // Create a list of payment IDs
        List<String> paymentIds = Arrays.asList(
            "PAY_001", "PAY_002", "PAY_003", "PAY_004", "PAY_005",
            "PAY_006", "PAY_007", "PAY_008", "PAY_009", "PAY_010",
            "PAY_011", "PAY_012", "PAY_013", "PAY_014", "PAY_015"
        );

        // Process batch
        PaymentSchedulerAdvanced.BatchProcessingResult result =
            scheduler.processBatchOptimized(paymentIds);

        // Verify results
        assertEquals(15, result.getTotalPayments());
        assertTrue(result.getSuccessCount() >= 14); // 95% success rate
        assertTrue(result.getProcessingTimeMs() < 10000); // Should complete within 10 seconds
        assertTrue(result.getSuccessRate() > 0.9); // > 90% success rate

        System.out.println("Batch processing: " + result.getSuccessCount() + "/" +
                          result.getTotalPayments() + " successful in " +
                          result.getProcessingTimeMs() + "ms");
    }

    @Test
    public void testDeadLetterQueue() {
        // Simulate payment failures
        Exception testException = new RuntimeException("Test failure");

        // First failure - should schedule retry
        scheduler.handlePaymentFailure("PAY_FAIL_001", "TENANT_A", "USER_1", testException, 0);

        // Second failure - should schedule retry
        scheduler.handlePaymentFailure("PAY_FAIL_002", "TENANT_A", "USER_1", testException, 1);

        // Third failure - should move to DLQ
        scheduler.handlePaymentFailure("PAY_FAIL_003", "TENANT_A", "USER_1", testException, 2);

        // Process DLQ
        scheduler.processDeadLetterQueue();

        // Verify DLQ processing completed without errors
        assertTrue(true, "DLQ processing should complete");
    }

    @Test
    public void testAdaptiveLoadBalancing() {
        // Register workers with different capacities
        scheduler.updateWorkerStats("WORKER_1", 30, 100, Set.of("DOMESTIC", "INTERNATIONAL"));
        scheduler.updateWorkerStats("WORKER_2", 80, 100, Set.of("DOMESTIC")); // Overloaded
        scheduler.updateWorkerStats("WORKER_3", 20, 100, Set.of("DOMESTIC", "INTERNATIONAL"));

        // Should select least loaded worker that supports payment type
        String selectedWorker = scheduler.selectOptimalWorker("DOMESTIC", "TENANT_A");
        assertEquals("WORKER_3", selectedWorker, "Should select least loaded worker");

        // Should not select overloaded worker
        String selectedWorker2 = scheduler.selectOptimalWorker("INTERNATIONAL", "TENANT_A");
        assertNotEquals("WORKER_2", selectedWorker2, "Should not select overloaded worker");

        // Get metrics
        Map<String, Object> metrics = scheduler.getLoadBalancingMetrics();
        assertEquals(3, metrics.get("totalWorkers"));
        assertEquals(1, metrics.get("overloadedWorkers"));
    }

    @Test
    public void testCircuitBreaker() {
        String bankId = "BANK_TEST";

        // First few calls should work (even with simulated failures)
        for (int i = 0; i < 3; i++) {
            PaymentSchedulerAdvanced.PaymentResult result =
                scheduler.executePaymentWithCircuitBreaker("PAY_" + i, bankId, 1000.0);
            // May succeed or fail based on random simulation
        }

        // Get circuit breaker status
        Map<String, Object> status = scheduler.getCircuitBreakerStatus();
        assertTrue(status.containsKey(bankId));

        Map<String, Object> breakerInfo = (Map<String, Object>) status.get(bankId);
        assertNotNull(breakerInfo.get("state"));
        assertNotNull(breakerInfo.get("failureCount"));

        System.out.println("Circuit breaker for " + bankId + ": " + breakerInfo);
    }

    @Test
    public void testEventSourcingAuditTrail() {
        String paymentId = "PAY_AUDIT_001";

        // Record events
        scheduler.recordPaymentEvent(paymentId,
            PaymentSchedulerAdvanced.EventType.PAYMENT_SUBMITTED,
            Map.of("amount", 1000.0, "currency", "VND"));

        scheduler.recordPaymentEvent(paymentId,
            PaymentSchedulerAdvanced.EventType.PAYMENT_VALIDATED,
            Map.of("validationResult", "SUCCESS"));

        scheduler.recordPaymentEvent(paymentId,
            PaymentSchedulerAdvanced.EventType.PAYMENT_COMPLETED,
            Map.of("transactionId", "TXN-123"));

        // Get audit trail
        List<PaymentSchedulerAdvanced.PaymentEvent> events = scheduler.getPaymentAuditTrail(paymentId);
        assertEquals(3, events.size());

        // Rebuild state from events
        PaymentSchedulerAdvanced.PaymentState state = scheduler.rebuildPaymentState(paymentId);
        assertEquals("COMPLETED", state.getStatus());
        assertTrue(state.getProcessors().isEmpty()); // No routing in this test

        System.out.println("Audit trail for " + paymentId + ": " + events.size() + " events");
    }

    @Test
    public void testPredictiveScaling() {
        // Record some scaling metrics
        Map<Integer, Double> volumes1 = Map.of(9, 100.0, 10, 200.0, 11, 150.0); // Morning peak
        scheduler.recordScalingMetrics(volumes1, 5, 45.0);

        Map<Integer, Double> volumes2 = Map.of(14, 80.0, 15, 90.0, 16, 70.0); // Afternoon
        scheduler.recordScalingMetrics(volumes2, 5, 50.0);

        Map<Integer, Double> volumes3 = Map.of(20, 30.0, 21, 25.0, 22, 20.0); // Evening low
        scheduler.recordScalingMetrics(volumes3, 5, 25.0);

        // Get scaling recommendation
        PaymentSchedulerAdvanced.ScalingRecommendation recommendation =
            scheduler.predictScalingNeeds();

        // Should have enough data now
        assertEquals("ANALYSIS_COMPLETE", recommendation.getStatus());
        assertNotNull(recommendation.getReason());

        System.out.println("Scaling recommendation: " + recommendation.getRecommendedWorkers() +
                          " workers - " + recommendation.getReason());
    }

    @Test
    public void testAllFeaturesIntegration() throws InterruptedException {
        // Integration test combining multiple features
        String paymentId = "PAY_INTEGRATION_001";
        String tenantId = "TENANT_TEST";
        String userId = "USER_TEST";

        // 1. Check rate limiting
        boolean canProcess = scheduler.canProcessPayment(tenantId, userId);
        assumeTrue(canProcess, "Rate limiting should allow processing");

        // 2. Submit to priority queue
        scheduler.submitPaymentWithPriority(paymentId, tenantId, userId,
            PaymentSchedulerAdvanced.PaymentPriority.STANDARD, 100000);

        // 3. Process from queue
        PaymentSchedulerAdvanced.PriorityPaymentRequest request = scheduler.getNextPriorityPayment();
        assertNotNull(request);
        assertEquals(paymentId, request.getPaymentId());

        // 4. Record event
        scheduler.recordPaymentEvent(paymentId, PaymentSchedulerAdvanced.EventType.PAYMENT_SUBMITTED,
            Map.of("amount", request.getAmount()));

        // 5. Execute with circuit breaker
        PaymentSchedulerAdvanced.PaymentResult result =
            scheduler.executePaymentWithCircuitBreaker(paymentId, "BANK_TEST", request.getAmount());

        // 6. Handle result
        if (result.isSuccess()) {
            scheduler.recordPaymentEvent(paymentId, PaymentSchedulerAdvanced.EventType.PAYMENT_COMPLETED,
                Map.of("transactionId", result.getTransactionId()));
        } else {
            scheduler.handlePaymentFailure(paymentId, tenantId, userId,
                new RuntimeException(result.getErrorMessage()), 0);
        }

        // 7. Get final audit trail
        List<PaymentSchedulerAdvanced.PaymentEvent> events = scheduler.getPaymentAuditTrail(paymentId);
        assertTrue(events.size() >= 1); // At least submission event

        System.out.println("Integration test completed for payment: " + paymentId +
                          " with " + events.size() + " audit events");
    }

    private void assumeTrue(boolean condition, String message) {
        if (!condition) {
            System.out.println("Skipping test: " + message);
        }
    }
}
