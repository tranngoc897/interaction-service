package com.ngoctran.interactionservice.workflow.payment;

import com.google.common.util.concurrent.RateLimiter;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Payment Scheduler with Enterprise Features
 * Implements: Rate Limiting, Priority Queues, Batch Processing, DLQ, Load Balancing, Circuit Breaker
 */
@Component
@Slf4j
public class PaymentSchedulerAdvanced {

    // ==================== 1. RATE LIMITING & THROTTLING ====================

    private final RateLimiter globalRateLimiter = RateLimiter.create(100.0); // 100 payments/second globally
    private final Map<String, RateLimiter> tenantRateLimiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    /**
     * Check if payment can be processed based on rate limits
     */
    public boolean canProcessPayment(String tenantId, String userId) {
        // Global rate limit
        if (!globalRateLimiter.tryAcquire()) {
            log.warn("Global rate limit exceeded");
            return false;
        }

        // Tenant-specific rate limit (50 payments/minute per tenant)
        RateLimiter tenantLimiter = tenantRateLimiters.computeIfAbsent(tenantId,
            k -> RateLimiter.create(50.0 / 60.0)); // 50 per minute = ~0.833 per second

        if (!tenantLimiter.tryAcquire()) {
            log.warn("Tenant rate limit exceeded for tenant: {}", tenantId);
            return false;
        }

        // User-specific rate limit (10 payments/minute per user)
        RateLimiter userLimiter = userRateLimiters.computeIfAbsent(userId,
            k -> RateLimiter.create(10.0 / 60.0)); // 10 per minute = ~0.167 per second

        if (!userLimiter.tryAcquire()) {
            log.warn("User rate limit exceeded for user: {}", userId);
            return false;
        }

        return true;
    }

    // ==================== 2. PRIORITY QUEUES ====================

    public enum PaymentPriority {
        CRITICAL(1, "Critical payments - system failures"),
        HIGH_VALUE(2, "Payments > 10M VND"),
        VIP_CUSTOMER(3, "Premium/VIP customers"),
        URGENT(4, "Time-sensitive payments"),
        STANDARD(5, "Regular payments"),
        LOW_PRIORITY(6, "Batch/scheduled payments");

        private final int priority;
        private final String description;

        PaymentPriority(int priority, String description) {
            this.priority = priority;
            this.description = description;
        }

        public int getPriority() { return priority; }
        public String getDescription() { return description; }
    }

    public static class PriorityPaymentRequest implements Comparable<PriorityPaymentRequest> {
        private final String paymentId;
        private final String tenantId;
        private final String userId;
        private final PaymentPriority priority;
        private final long createdAt;
        private final double amount;

        public PriorityPaymentRequest(String paymentId, String tenantId, String userId,
                                    PaymentPriority priority, double amount) {
            this.paymentId = paymentId;
            this.tenantId = tenantId;
            this.userId = userId;
            this.priority = priority;
            this.createdAt = System.currentTimeMillis();
            this.amount = amount;
        }

        @Override
        public int compareTo(PriorityPaymentRequest other) {
            // Lower priority number = higher priority
            int priorityCompare = Integer.compare(this.priority.getPriority(), other.priority.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // Same priority: FIFO by creation time
            return Long.compare(this.createdAt, other.createdAt);
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getTenantId() { return tenantId; }
        public String getUserId() { return userId; }
        public PaymentPriority getPriority() { return priority; }
        public long getCreatedAt() { return createdAt; }
        public double getAmount() { return amount; }
    }

    private final PriorityBlockingQueue<PriorityPaymentRequest> priorityQueue =
        new PriorityBlockingQueue<>(1000, Comparator.comparing(PriorityPaymentRequest::getPriority));

    /**
     * Submit payment with priority
     */
    public void submitPaymentWithPriority(String paymentId, String tenantId, String userId,
                                        PaymentPriority priority, double amount) {
        PriorityPaymentRequest request = new PriorityPaymentRequest(
            paymentId, tenantId, userId, priority, amount);

        boolean added = priorityQueue.offer(request);
        if (added) {
            log.info("Payment {} submitted with priority {} for tenant {} user {}",
                    paymentId, priority, tenantId, userId);
        } else {
            log.error("Priority queue full, payment {} rejected", paymentId);
            throw new RuntimeException("Payment queue full");
        }
    }

    /**
     * Process next highest priority payment
     */
    public PriorityPaymentRequest getNextPriorityPayment() {
        try {
            // Non-blocking poll with timeout
            return priorityQueue.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ==================== 3. BATCH PROCESSING OPTIMIZATION ====================

    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(10);
    private final int BATCH_SIZE = 10;
    private final Duration BATCH_TIMEOUT = Duration.ofSeconds(30);

    public static class BatchProcessingResult {
        private final int totalPayments;
        private final int successCount;
        private final int failureCount;
        private final long processingTimeMs;
        private final List<String> failedPaymentIds;

        public BatchProcessingResult(int totalPayments, int successCount, int failureCount,
                                   long processingTimeMs, List<String> failedPaymentIds) {
            this.totalPayments = totalPayments;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.processingTimeMs = processingTimeMs;
            this.failedPaymentIds = failedPaymentIds;
        }

        // Getters
        public int getTotalPayments() { return totalPayments; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public List<String> getFailedPaymentIds() { return failedPaymentIds; }
        public double getSuccessRate() { return totalPayments > 0 ? (double) successCount / totalPayments : 0; }
    }

    /**
     * Process payments in optimized batches
     */
    public BatchProcessingResult processBatchOptimized(List<String> paymentIds) {
        long startTime = System.currentTimeMillis();
        log.info("Starting optimized batch processing of {} payments", paymentIds.size());

        // Split into smaller batches for parallel processing
        List<List<String>> batches = partitionList(paymentIds, BATCH_SIZE);

        List<CompletableFuture<List<PaymentResult>>> futures = batches.stream()
            .map(batch -> CompletableFuture.supplyAsync(() ->
                processSubBatch(batch), batchExecutor))
            .collect(java.util.stream.Collectors.toList());

        // Wait for all batches to complete with timeout
        try {
            List<List<PaymentResult>> batchResults = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(java.util.stream.Collectors.toList()))
                .get(BATCH_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            // Aggregate results
            int totalPayments = paymentIds.size();
            int successCount = 0;
            List<String> failedPaymentIds = new ArrayList<>();

            for (List<PaymentResult> batchResult : batchResults) {
                for (PaymentResult result : batchResult) {
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failedPaymentIds.add(result.getPaymentId());
                    }
                }
            }

            int failureCount = totalPayments - successCount;
            long processingTimeMs = System.currentTimeMillis() - startTime;

            BatchProcessingResult result = new BatchProcessingResult(
                totalPayments, successCount, failureCount, processingTimeMs, failedPaymentIds);

            log.info("Batch processing completed: {}/{} successful in {}ms",
                    successCount, totalPayments, processingTimeMs);

            return result;

        } catch (Exception e) {
            log.error("Batch processing failed", e);
            long processingTimeMs = System.currentTimeMillis() - startTime;
            return new BatchProcessingResult(paymentIds.size(), 0, paymentIds.size(),
                    processingTimeMs, new ArrayList<>(paymentIds));
        }
    }

    private List<PaymentResult> processSubBatch(List<String> paymentIds) {
        // Simulate parallel processing of sub-batch
        return paymentIds.stream()
            .map(this::processSinglePayment)
            .collect(java.util.stream.Collectors.toList());
    }

    private PaymentResult processSinglePayment(String paymentId) {
        // Simulate payment processing with random success/failure
        try {
            Thread.sleep(100 + (int)(Math.random() * 200)); // 100-300ms processing time

            boolean success = Math.random() > 0.05; // 95% success rate
            if (success) {
                return new PaymentResult(true, paymentId, "TXN-" + System.currentTimeMillis(),
                    "SUCCESS", null, System.currentTimeMillis());
            } else {
                return new PaymentResult(false, paymentId, null,
                    "PROCESSING_ERROR", "Random failure", System.currentTimeMillis());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PaymentResult(false, paymentId, null,
                "INTERRUPTED", "Processing interrupted", System.currentTimeMillis());
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    public static class PaymentResult {
        private final boolean success;
        private final String paymentId;
        private final String transactionId;
        private final String status;
        private final String errorMessage;
        private final long timestamp;

        public PaymentResult(boolean success, String paymentId, String transactionId,
                           String status, String errorMessage, long timestamp) {
            this.success = success;
            this.paymentId = paymentId;
            this.transactionId = transactionId;
            this.status = status;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getPaymentId() { return paymentId; }
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getErrorMessage() { return errorMessage; }
        public long getTimestamp() { return timestamp; }
    }

    // ==================== 4. DEAD LETTER QUEUE (DLQ) ====================

    public static class FailedPayment {
        private final String paymentId;
        private final Exception error;
        private final int retryCount;
        private final long failedAt;
        private final String tenantId;
        private final String userId;

        public FailedPayment(String paymentId, Exception error, int retryCount,
                           long failedAt, String tenantId, String userId) {
            this.paymentId = paymentId;
            this.error = error;
            this.retryCount = retryCount;
            this.failedAt = failedAt;
            this.tenantId = tenantId;
            this.userId = userId;
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public Exception getError() { return error; }
        public int getRetryCount() { return retryCount; }
        public long getFailedAt() { return failedAt; }
        public String getTenantId() { return tenantId; }
        public String getUserId() { return userId; }
    }

    private final BlockingQueue<FailedPayment> deadLetterQueue = new LinkedBlockingQueue<>(1000);
    private final ScheduledExecutorService dlqProcessor = Executors.newScheduledThreadPool(1);
    private final int MAX_RETRIES = 3;

    /**
     * Handle payment failure with retry logic and DLQ
     */
    public void handlePaymentFailure(String paymentId, String tenantId, String userId,
                                   Exception error, int currentRetryCount) {
        FailedPayment failedPayment = new FailedPayment(paymentId, error, currentRetryCount,
            System.currentTimeMillis(), tenantId, userId);

        if (currentRetryCount < MAX_RETRIES) {
            // Schedule retry with exponential backoff
            long delayMs = (long) (Math.pow(2, currentRetryCount) * 1000); // 1s, 2s, 4s...
            scheduleRetry(paymentId, tenantId, userId, delayMs, currentRetryCount + 1);
            log.info("Scheduled retry {} for payment {} in {}ms", currentRetryCount + 1, paymentId, delayMs);
        } else {
            // Move to DLQ
            boolean added = deadLetterQueue.offer(failedPayment);
            if (added) {
                log.error("Payment {} moved to DLQ after {} retries. Error: {}",
                    paymentId, currentRetryCount, error.getMessage());

                // Alert operations team
                alertOperationsTeam(failedPayment);
            } else {
                log.error("DLQ full! Payment {} lost after {} retries", paymentId, currentRetryCount);
            }
        }
    }

    private void scheduleRetry(String paymentId, String tenantId, String userId,
                             long delayMs, int retryCount) {
        dlqProcessor.schedule(() -> {
            log.info("Retrying payment {} (attempt {})", paymentId, retryCount);
            // In real implementation, this would trigger workflow retry
            // For demo, just log
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void alertOperationsTeam(FailedPayment failedPayment) {
        log.error("🚨 DLQ ALERT: Payment {} failed permanently after {} retries. " +
                 "Tenant: {}, User: {}, Error: {}",
                 failedPayment.getPaymentId(),
                 failedPayment.getRetryCount(),
                 failedPayment.getTenantId(),
                 failedPayment.getUserId(),
                 failedPayment.getError().getMessage());
    }

    /**
     * Process DLQ items (manual review or reprocessing)
     */
    public void processDeadLetterQueue() {
        FailedPayment failedPayment;
        while ((failedPayment = deadLetterQueue.poll()) != null) {
            try {
                // In real implementation, this might:
                // 1. Send to manual review queue
                // 2. Attempt reprocessing with different logic
                // 3. Notify customer support
                // 4. Generate incident report

                log.info("Processing DLQ item: payment {}", failedPayment.getPaymentId());

                // For demo, just create incident report
                createIncidentReport(failedPayment);

            } catch (Exception e) {
                log.error("Failed to process DLQ item: {}", failedPayment.getPaymentId(), e);
            }
        }
    }

    private void createIncidentReport(FailedPayment failedPayment) {
        log.info("📋 Created incident report for failed payment: {}", failedPayment.getPaymentId());
        // In real implementation, save to database or send to ticketing system
    }

    // ==================== 5. ADAPTIVE LOAD BALANCING ====================

    public static class WorkerStats {
        private final String workerId;
        private final int currentLoad; // 0-100
        private final int maxCapacity;
        private final Set<String> supportedPaymentTypes;
        private final long lastUpdated;

        public WorkerStats(String workerId, int currentLoad, int maxCapacity,
                         Set<String> supportedPaymentTypes) {
            this.workerId = workerId;
            this.currentLoad = currentLoad;
            this.maxCapacity = maxCapacity;
            this.supportedPaymentTypes = supportedPaymentTypes;
            this.lastUpdated = System.currentTimeMillis();
        }

        public String getWorkerId() { return workerId; }
        public int getCurrentLoad() { return currentLoad; }
        public int getMaxCapacity() { return maxCapacity; }
        public Set<String> getSupportedPaymentTypes() { return supportedPaymentTypes; }
        public long getLastUpdated() { return lastUpdated; }
        public int getAvailableCapacity() { return maxCapacity - currentLoad; }
        public boolean canHandle(String paymentType) {
            return supportedPaymentTypes.contains(paymentType) || supportedPaymentTypes.contains("ALL");
        }
        public boolean isOverloaded() { return currentLoad > 80; }
    }

    private final Map<String, WorkerStats> workerStats = new ConcurrentHashMap<>();
    private final int LOAD_THRESHOLD_HIGH = 80;
    private final int LOAD_THRESHOLD_LOW = 20;

    /**
     * Update worker statistics
     */
    public void updateWorkerStats(String workerId, int currentLoad, int maxCapacity,
                                Set<String> supportedPaymentTypes) {
        WorkerStats stats = new WorkerStats(workerId, currentLoad, maxCapacity, supportedPaymentTypes);
        workerStats.put(workerId, stats);

        if (stats.isOverloaded()) {
            log.warn("Worker {} is overloaded: {}/{} capacity",
                workerId, currentLoad, maxCapacity);
        }
    }

    /**
     * Select optimal worker based on load balancing
     */
    public String selectOptimalWorker(String paymentType, String tenantId) {
        return workerStats.entrySet().stream()
            .filter(entry -> entry.getValue().canHandle(paymentType))
            .filter(entry -> !entry.getValue().isOverloaded()) // Exclude overloaded workers
            .min((e1, e2) -> {
                // Prefer workers with lower load
                int loadCompare = Integer.compare(e1.getValue().getCurrentLoad(), e2.getValue().getCurrentLoad());
                if (loadCompare != 0) {
                    return loadCompare;
                }
                // If same load, prefer workers with higher capacity
                return Integer.compare(e2.getValue().getMaxCapacity(), e1.getValue().getMaxCapacity());
            })
            .map(Map.Entry::getKey)
            .orElse("DEFAULT_WORKER");
    }

    /**
     * Get load balancing metrics
     */
    public Map<String, Object> getLoadBalancingMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalWorkers", workerStats.size());
        metrics.put("overloadedWorkers", workerStats.values().stream()
            .mapToInt(stats -> stats.isOverloaded() ? 1 : 0).sum());
        metrics.put("averageLoad", workerStats.values().stream()
            .mapToInt(WorkerStats::getCurrentLoad).average().orElse(0.0));
        metrics.put("totalCapacity", workerStats.values().stream()
            .mapToInt(WorkerStats::getMaxCapacity).sum());
        metrics.put("usedCapacity", workerStats.values().stream()
            .mapToInt(WorkerStats::getCurrentLoad).sum());

        return metrics;
    }

    // ==================== 6. CIRCUIT BREAKER CHO EXTERNAL SERVICES ====================

    public static class CircuitBreakerState {
        enum State { CLOSED, OPEN, HALF_OPEN }

        private State state = State.CLOSED;
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private final int failureThreshold;
        private final long timeoutMs;

        public CircuitBreakerState(int failureThreshold, long timeoutMs) {
            this.failureThreshold = failureThreshold;
            this.timeoutMs = timeoutMs;
        }

        public synchronized boolean canExecute() {
            long currentTime = System.currentTimeMillis();

            switch (state) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (currentTime - lastFailureTime > timeoutMs) {
                        state = State.HALF_OPEN;
                        log.info("Circuit breaker transitioned to HALF_OPEN");
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    return true;
                default:
                    return false;
            }
        }

        public synchronized void recordSuccess() {
            failureCount = 0;
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                log.info("Circuit breaker transitioned to CLOSED (success)");
            }
        }

        public synchronized void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();

            if (failureCount >= failureThreshold) {
                state = State.OPEN;
                log.warn("Circuit breaker transitioned to OPEN ({} failures)", failureCount);
            } else if (state == State.HALF_OPEN) {
                state = State.OPEN;
                log.warn("Circuit breaker back to OPEN (failure in HALF_OPEN)");
            }
        }

        public State getState() { return state; }
        public int getFailureCount() { return failureCount; }
    }

    private final Map<String, CircuitBreakerState> bankCircuitBreakers = new ConcurrentHashMap<>();

    /**
     * Execute payment with circuit breaker protection
     */
    public PaymentResult executePaymentWithCircuitBreaker(String paymentId, String bankId, double amount) {
        CircuitBreakerState circuitBreaker = bankCircuitBreakers.computeIfAbsent(bankId,
            k -> new CircuitBreakerState(5, 60000)); // 5 failures, 60 second timeout

        if (!circuitBreaker.canExecute()) {
            log.warn("Circuit breaker OPEN for bank: {}", bankId);
            return new PaymentResult(false, paymentId, null, "CIRCUIT_BREAKER_OPEN",
                "Bank service temporarily unavailable", System.currentTimeMillis());
        }

        try {
            // Simulate bank API call
            PaymentResult result = callBankAPI(paymentId, bankId, amount);

            if (result.isSuccess()) {
                circuitBreaker.recordSuccess();
                log.debug("Payment {} succeeded for bank {}", paymentId, bankId);
            } else {
                circuitBreaker.recordFailure();
                log.warn("Payment {} failed for bank {}: {}", paymentId, bankId, result.getErrorMessage());
            }

            return result;

        } catch (Exception e) {
            circuitBreaker.recordFailure();
            log.error("Payment {} exception for bank {}: {}", paymentId, bankId, e.getMessage());
            return new PaymentResult(false, paymentId, null, "EXCEPTION",
                e.getMessage(), System.currentTimeMillis());
        }
    }

    private PaymentResult callBankAPI(String paymentId, String bankId, double amount) {
        // Simulate bank API call with various failure scenarios
        try {
            Thread.sleep(200 + (int)(Math.random() * 800)); // 200-1000ms response time

            // Simulate different failure rates per bank
            double failureRate = getBankFailureRate(bankId);
            boolean success = Math.random() > failureRate;

            if (success) {
                return new PaymentResult(true, paymentId, "TXN-" + System.nanoTime(),
                    "SUCCESS", null, System.currentTimeMillis());
            } else {
                // Random error types
                String[] errors = {"INSUFFICIENT_FUNDS", "ACCOUNT_BLOCKED", "NETWORK_ERROR", "TIMEOUT"};
                String error = errors[(int)(Math.random() * errors.length)];
                return new PaymentResult(false, paymentId, null, error,
                    "Bank rejected payment", System.currentTimeMillis());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PaymentResult(false, paymentId, null, "INTERRUPTED",
                "Processing interrupted", System.currentTimeMillis());
        }
    }

    private double getBankFailureRate(String bankId) {
        // Different banks have different reliability
        switch (bankId) {
            case "BANK_A": return 0.02; // 2% failure rate
            case "BANK_B": return 0.05; // 5% failure rate
            case "BANK_C": return 0.10; // 10% failure rate
            default: return 0.03; // 3% default
        }
    }

    /**
     * Get circuit breaker status for monitoring
     */
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        bankCircuitBreakers.forEach((bankId, breaker) -> {
            Map<String, Object> breakerInfo = new HashMap<>();
            breakerInfo.put("state", breaker.getState().toString());
            breakerInfo.put("failureCount", breaker.getFailureCount());
            status.put(bankId, breakerInfo);
        });
        return status;
    }

    // ==================== 7. EVENT SOURCING CHO AUDIT TRAIL ====================

    public static class PaymentEvent {
        private final String paymentId;
        private final String eventType;
        private final Map<String, Object> data;
        private final long timestamp;
        private final String workflowId;
        private final String runId;

        public PaymentEvent(String paymentId, String eventType, Map<String, Object> data,
                          long timestamp, String workflowId, String runId) {
            this.paymentId = paymentId;
            this.eventType = eventType;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
            this.timestamp = timestamp;
            this.workflowId = workflowId;
            this.runId = runId;
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getEventType() { return eventType; }
        public Map<String, Object> getData() { return new HashMap<>(data); }
        public long getTimestamp() { return timestamp; }
        public String getWorkflowId() { return workflowId; }
        public String getRunId() { return runId; }
    }

    public enum EventType {
        PAYMENT_SUBMITTED("Payment submitted to scheduler"),
        PAYMENT_VALIDATED("Payment validation completed"),
        PAYMENT_ROUTED("Payment routed to processor"),
        PAYMENT_EXECUTED("Payment execution attempted"),
        PAYMENT_COMPLETED("Payment completed successfully"),
        PAYMENT_FAILED("Payment failed"),
        PAYMENT_COMPENSATED("Payment compensation applied"),
        PAYMENT_RETRY_SCHEDULED("Payment retry scheduled"),
        PAYMENT_MOVED_TO_DLQ("Payment moved to dead letter queue");

        private final String description;

        EventType(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    // In-memory event store (in real implementation, use database or Kafka)
    private final Map<String, List<PaymentEvent>> eventStore = new ConcurrentHashMap<>();

    /**
     * Record payment event for audit trail
     */
    public void recordPaymentEvent(String paymentId, EventType eventType, Map<String, Object> data) {
        recordPaymentEvent(paymentId, eventType.name(), data);
    }

    public void recordPaymentEvent(String paymentId, String eventType, Map<String, Object> data) {
        PaymentEvent event = new PaymentEvent(
            paymentId,
            eventType,
            data,
            System.currentTimeMillis(),
            "WORKFLOW_ID", // In real workflow, use Workflow.getInfo().getWorkflowId()
            "RUN_ID"       // In real workflow, use Workflow.getInfo().getRunId()
        );

        eventStore.computeIfAbsent(paymentId, k -> Collections.synchronizedList(new ArrayList<>()))
                  .add(event);

        log.info("Recorded event: {} for payment {}", eventType, paymentId);
    }

    /**
     * Get complete audit trail for payment
     */
    public List<PaymentEvent> getPaymentAuditTrail(String paymentId) {
        return new ArrayList<>(eventStore.getOrDefault(paymentId, new ArrayList<>()));
    }

    /**
     * Rebuild payment state from events (Event Sourcing)
     */
    public PaymentState rebuildPaymentState(String paymentId) {
        List<PaymentEvent> events = getPaymentAuditTrail(paymentId);
        PaymentState state = new PaymentState();

        for (PaymentEvent event : events) {
            state = state.apply(event);
        }

        return state;
    }

    public static class PaymentState {
        private String status = "UNKNOWN";
        private int retryCount = 0;
        private String lastError = null;
        private long createdAt = 0;
        private long completedAt = 0;
        private List<String> processors = new ArrayList<>();

        public PaymentState apply(PaymentEvent event) {
            switch (event.getEventType()) {
                case "PAYMENT_SUBMITTED":
                    this.status = "SUBMITTED";
                    this.createdAt = event.getTimestamp();
                    break;
                case "PAYMENT_VALIDATED":
                    this.status = "VALIDATED";
                    break;
                case "PAYMENT_ROUTED":
                    String processor = (String) event.getData().get("processor");
                    if (processor != null) {
                        this.processors.add(processor);
                    }
                    break;
                case "PAYMENT_EXECUTED":
                    this.status = "EXECUTING";
                    break;
                case "PAYMENT_COMPLETED":
                    this.status = "COMPLETED";
                    this.completedAt = event.getTimestamp();
                    break;
                case "PAYMENT_FAILED":
                    this.status = "FAILED";
                    this.lastError = (String) event.getData().get("error");
                    break;
                case "PAYMENT_RETRY_SCHEDULED":
                    this.retryCount = (Integer) event.getData().getOrDefault("retryCount", 0);
                    break;
                case "PAYMENT_COMPENSATED":
                    this.status = "COMPENSATED";
                    break;
            }
            return this;
        }

        // Getters
        public String getStatus() { return status; }
        public int getRetryCount() { return retryCount; }
        public String getLastError() { return lastError; }
        public long getCreatedAt() { return createdAt; }
        public long getCompletedAt() { return completedAt; }
        public List<String> getProcessors() { return new ArrayList<>(processors); }
    }

    // ==================== 8. PREDICTIVE SCALING ====================

    public static class ScalingMetrics {
        private final Map<Integer, Double> hourlyVolumes; // Hour -> Average volume
        private final int currentWorkers;
        private final double averageLoad;
        private final long lastUpdated;

        public ScalingMetrics(Map<Integer, Double> hourlyVolumes, int currentWorkers,
                            double averageLoad) {
            this.hourlyVolumes = new HashMap<>(hourlyVolumes);
            this.currentWorkers = currentWorkers;
            this.averageLoad = averageLoad;
            this.lastUpdated = System.currentTimeMillis();
        }

        public Map<Integer, Double> getHourlyVolumes() { return new HashMap<>(hourlyVolumes); }
        public int getCurrentWorkers() { return currentWorkers; }
        public double getAverageLoad() { return averageLoad; }
        public long getLastUpdated() { return lastUpdated; }
    }

    private final List<ScalingMetrics> scalingHistory = Collections.synchronizedList(new ArrayList<>());
    private final int MAX_WORKERS = 20;
    private final int MIN_WORKERS = 2;
    private final double SCALE_UP_THRESHOLD = 70.0; // 70% average load
    private final double SCALE_DOWN_THRESHOLD = 30.0; // 30% average load

    /**
     * Record scaling metrics for analysis
     */
    public void recordScalingMetrics(Map<Integer, Double> hourlyVolumes, int currentWorkers, double averageLoad) {
        ScalingMetrics metrics = new ScalingMetrics(hourlyVolumes, currentWorkers, averageLoad);
        scalingHistory.add(metrics);

        // Keep only last 24 hours of data
        if (scalingHistory.size() > 24) {
            scalingHistory.remove(0);
        }
    }

    /**
     * Predict peak hours and recommend scaling
     */
    public ScalingRecommendation predictScalingNeeds() {
        if (scalingHistory.size() < 6) { // Need at least 6 hours of data
            return new ScalingRecommendation(0, "INSUFFICIENT_DATA", "Need more historical data");
        }

        // Analyze patterns
        Map<Integer, Double> avgVolumes = analyzeHourlyPatterns();
        List<Integer> predictedPeakHours = identifyPeakHours(avgVolumes);

        // Get current metrics
        ScalingMetrics latest = scalingHistory.get(scalingHistory.size() - 1);
        double currentLoad = latest.getAverageLoad();

        // Make scaling decision
        int recommendedWorkers = calculateRecommendedWorkers(currentLoad, predictedPeakHours);

        String reason;
        if (recommendedWorkers > latest.getCurrentWorkers()) {
            reason = String.format("High load (%.1f%%) detected, scaling up for predicted peak hours: %s",
                currentLoad, predictedPeakHours);
        } else if (recommendedWorkers < latest.getCurrentWorkers()) {
            reason = String.format("Low load (%.1f%%) detected, scaling down to save resources",
                currentLoad);
        } else {
            reason = String.format("Load stable at %.1f%%, no scaling needed", currentLoad);
        }

        return new ScalingRecommendation(recommendedWorkers, "ANALYSIS_COMPLETE", reason);
    }

    private Map<Integer, Double> analyzeHourlyPatterns() {
        Map<Integer, List<Double>> hourlyData = new HashMap<>();

        // Group data by hour
        for (ScalingMetrics metrics : scalingHistory) {
            for (Map.Entry<Integer, Double> entry : metrics.getHourlyVolumes().entrySet()) {
                hourlyData.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                         .add(entry.getValue());
            }
        }

        // Calculate averages
        Map<Integer, Double> averages = new HashMap<>();
        for (Map.Entry<Integer, List<Double>> entry : hourlyData.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            averages.put(entry.getKey(), avg);
        }

        return averages;
    }

    private List<Integer> identifyPeakHours(Map<Integer, Double> avgVolumes) {
        // Find hours with volume > 80% of max volume
        double maxVolume = avgVolumes.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double threshold = maxVolume * 0.8;

        return avgVolumes.entrySet().stream()
            .filter(entry -> entry.getValue() >= threshold)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(java.util.stream.Collectors.toList());
    }

    private int calculateRecommendedWorkers(double currentLoad, List<Integer> peakHours) {
        ScalingMetrics latest = scalingHistory.get(scalingHistory.size() - 1);
        int currentWorkers = latest.getCurrentWorkers();

        // Check if we're in a peak hour
        int currentHour = java.time.LocalDateTime.now().getHour();
        boolean isPeakHour = peakHours.contains(currentHour);

        if (isPeakHour && currentLoad > SCALE_UP_THRESHOLD) {
            // Scale up during peak hours with high load
            return Math.min(currentWorkers + 2, MAX_WORKERS);
        } else if (!isPeakHour && currentLoad < SCALE_DOWN_THRESHOLD && currentWorkers > MIN_WORKERS) {
            // Scale down during off-peak with low load
            return Math.max(currentWorkers - 1, MIN_WORKERS);
        }

        // No change needed
        return currentWorkers;
    }

    public static class ScalingRecommendation {
        private final int recommendedWorkers;
        private final String status;
        private final String reason;

        public ScalingRecommendation(int recommendedWorkers, String status, String reason) {
            this.recommendedWorkers = recommendedWorkers;
            this.status = status;
            this.reason = reason;
        }

        public int getRecommendedWorkers() { return recommendedWorkers; }
        public String getStatus() { return status; }
        public String getReason() { return reason; }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Cleanup resources
     */
    public void shutdown() {
        batchExecutor.shutdown();
        dlqProcessor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
            if (!dlqProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                dlqProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            batchExecutor.shutdownNow();
            dlqProcessor.shutdownNow();
        }
    }
}
