package com.ngoctran.interactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redisson.enabled", havingValue = "true")
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * Executes a task only if the lock can be acquired.
     * If the lock is held by another instance (Pod), it returns immediately.
     */
    public <T> T executeWithLock(String lockKey, long waitTimeSeconds, long leaseTimeSeconds, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // Try to acquire lock. If we can't get it within waitTime, someone else is
            // running the job.
            if (lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS)) {
                log.debug("Acquired distributed lock for: {}", lockKey);
                try {
                    return task.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Released distributed lock for: {}", lockKey);
                    }
                }
            } else {
                log.debug("Could not acquire lock {}, another instance is likely running this task", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for lock: {}", lockKey);
        }
        return null;
    }

    public void runWithLock(String lockKey, long waitTimeSeconds, long leaseTimeSeconds, Runnable task) {
        executeWithLock(lockKey, waitTimeSeconds, leaseTimeSeconds, () -> {
            task.run();
            return null;
        });
    }
}
