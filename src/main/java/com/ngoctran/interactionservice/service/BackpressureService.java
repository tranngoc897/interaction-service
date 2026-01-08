package com.ngoctran.interactionservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Backpressure Service - Protects system from overload
 * Implements semaphore-based throttling similar to Temporal's worker slots
 */
@Slf4j
@Service
public class BackpressureService {

    private final Semaphore workflowSemaphore;
    private final Semaphore stepSemaphore;
    private final AtomicInteger activeWorkflows = new AtomicInteger(0);
    private final AtomicInteger rejectedWorkflows = new AtomicInteger(0);

    @Value("${backpressure.max-concurrent-workflows:100}")
    private int maxConcurrentWorkflows;

    @Value("${backpressure.max-concurrent-steps:500}")
    private int maxConcurrentSteps;

    @Value("${backpressure.acquire-timeout-ms:5000}")
    private long acquireTimeoutMs;

    public BackpressureService(
            @Value("${backpressure.max-concurrent-workflows:100}") int maxConcurrentWorkflows,
            @Value("${backpressure.max-concurrent-steps:500}") int maxConcurrentSteps) {
        this.workflowSemaphore = new Semaphore(maxConcurrentWorkflows, true); // Fair mode
        this.stepSemaphore = new Semaphore(maxConcurrentSteps, true);
        log.info("Backpressure initialized: maxWorkflows={}, maxSteps={}",
                maxConcurrentWorkflows, maxConcurrentSteps);
    }

    /**
     * Acquire permit for workflow execution
     * 
     * @return WorkflowPermit to be released after execution
     * @throws BackpressureException if system is overloaded
     */
    public WorkflowPermit acquireWorkflowPermit() throws BackpressureException {
        try {
            boolean acquired = workflowSemaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);

            if (!acquired) {
                int rejected = rejectedWorkflows.incrementAndGet();
                log.warn("Workflow rejected due to backpressure. Total rejected: {}, Active: {}/{}",
                        rejected, activeWorkflows.get(), maxConcurrentWorkflows);

                throw new BackpressureException(
                        String.format("System overloaded. Active workflows: %d/%d. Please retry later.",
                                activeWorkflows.get(), maxConcurrentWorkflows));
            }

            int active = activeWorkflows.incrementAndGet();
            log.debug("Workflow permit acquired. Active: {}/{}", active, maxConcurrentWorkflows);

            return new WorkflowPermit(this);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackpressureException("Interrupted while acquiring workflow permit", e);
        }
    }

    /**
     * Acquire permit for step execution
     * 
     * @return StepPermit to be released after execution
     * @throws BackpressureException if system is overloaded
     */
    public StepPermit acquireStepPermit() throws BackpressureException {
        try {
            boolean acquired = stepSemaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("Step execution rejected due to backpressure. Available: {}/{}",
                        stepSemaphore.availablePermits(), maxConcurrentSteps);

                throw new BackpressureException(
                        String.format("System overloaded. Available step slots: %d/%d",
                                stepSemaphore.availablePermits(), maxConcurrentSteps));
            }

            log.debug("Step permit acquired. Available: {}/{}",
                    stepSemaphore.availablePermits(), maxConcurrentSteps);

            return new StepPermit(this);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackpressureException("Interrupted while acquiring step permit", e);
        }
    }

    /**
     * Release workflow permit
     */
    void releaseWorkflowPermit() {
        workflowSemaphore.release();
        int active = activeWorkflows.decrementAndGet();
        log.debug("Workflow permit released. Active: {}/{}", active, maxConcurrentWorkflows);
    }

    /**
     * Release step permit
     */
    void releaseStepPermit() {
        stepSemaphore.release();
        log.debug("Step permit released. Available: {}/{}",
                stepSemaphore.availablePermits(), maxConcurrentSteps);
    }

    /**
     * Get current system load metrics
     */
    public BackpressureMetrics getMetrics() {
        return BackpressureMetrics.builder()
                .maxConcurrentWorkflows(maxConcurrentWorkflows)
                .activeWorkflows(activeWorkflows.get())
                .availableWorkflowSlots(workflowSemaphore.availablePermits())
                .maxConcurrentSteps(maxConcurrentSteps)
                .availableStepSlots(stepSemaphore.availablePermits())
                .totalRejected(rejectedWorkflows.get())
                .loadPercentage(calculateLoadPercentage())
                .build();
    }

    private double calculateLoadPercentage() {
        return (double) activeWorkflows.get() / maxConcurrentWorkflows * 100;
    }

    /**
     * Check if system is healthy (not overloaded)
     */
    public boolean isHealthy() {
        double load = calculateLoadPercentage();
        return load < 90.0; // Healthy if < 90% capacity
    }

    /**
     * Workflow execution permit (AutoCloseable for try-with-resources)
     */
    public static class WorkflowPermit implements AutoCloseable {
        private final BackpressureService service;
        private boolean released = false;

        WorkflowPermit(BackpressureService service) {
            this.service = service;
        }

        @Override
        public void close() {
            if (!released) {
                service.releaseWorkflowPermit();
                released = true;
            }
        }
    }

    /**
     * Step execution permit (AutoCloseable for try-with-resources)
     */
    public static class StepPermit implements AutoCloseable {
        private final BackpressureService service;
        private boolean released = false;

        StepPermit(BackpressureService service) {
            this.service = service;
        }

        @Override
        public void close() {
            if (!released) {
                service.releaseStepPermit();
                released = true;
            }
        }
    }

    /**
     * Backpressure metrics
     */
    @lombok.Builder
    @lombok.Data
    public static class BackpressureMetrics {
        private int maxConcurrentWorkflows;
        private int activeWorkflows;
        private int availableWorkflowSlots;
        private int maxConcurrentSteps;
        private int availableStepSlots;
        private int totalRejected;
        private double loadPercentage;
    }

    /**
     * Exception thrown when system is overloaded
     */
    public static class BackpressureException extends RuntimeException {
        public BackpressureException(String message) {
            super(message);
        }

        public BackpressureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
