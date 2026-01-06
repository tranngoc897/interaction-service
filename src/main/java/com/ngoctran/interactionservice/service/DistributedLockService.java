 package com.ngoctran.interactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed Lock Service using Redisson
 * Provides distributed locking for scheduler coordination across multiple instances
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    @Value("${workflow.lock.enabled:true}")
    private boolean lockEnabled;

    @Value("${workflow.lock.timeout:30}")
    private int lockTimeoutSeconds;

    @Value("${workflow.lock.wait-time:0}")
    private int lockWaitTimeSeconds;

    /**
     * Execute with distributed lock
     * @param lockKey Unique lock key
     * @param operation Operation to execute
     * @return true if lock acquired and operation executed, false otherwise
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        return executeWithLock(lockKey, operation, null);
    }

    /**
     * Execute with distributed lock and fallback
     * @param lockKey Unique lock key
     * @param operation Operation to execute
     * @param fallback Fallback operation if lock not acquired
     * @return result of operation or fallback
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> operation, Supplier<T> fallback) {
        if (!lockEnabled) {
            log.debug("Distributed locking disabled, executing operation directly");
            return operation.get();
        }

        RLock lock = redissonClient.getLock("workflow:" + lockKey);

        try {
            boolean acquired = lock.tryLock(lockWaitTimeSeconds, lockTimeoutSeconds, TimeUnit.SECONDS);

            if (acquired) {
                try {
                    log.debug("Acquired distributed lock for key: {}", lockKey);
                    return operation.get();
                } finally {
                    lock.unlock();
                    log.debug("Released distributed lock for key: {}", lockKey);
                }
            } else {
                log.warn("Failed to acquire distributed lock for key: {}", lockKey);
                if (fallback != null) {
                    log.info("Executing fallback operation for key: {}", lockKey);
                    return fallback.get();
                }
                return null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for lock: {}", lockKey, e);
            return fallback != null ? fallback.get() : null;
        } catch (Exception e) {
            log.error("Error with distributed lock for key: {}", lockKey, e);
            return fallback != null ? fallback.get() : null;
        }
    }

    /**
     * Try to execute with lock, return null if lock not acquired
     */
    public <T> T tryExecuteWithLock(String lockKey, Supplier<T> operation) {
        return executeWithLock(lockKey, operation, () -> null);
    }

    /**
     * Check if lock is currently held
     */
    public boolean isLocked(String lockKey) {
        if (!lockEnabled) {
            return false;
        }

        RLock lock = redissonClient.getLock("workflow:" + lockKey);
        return lock.isLocked();
    }

    /**
     * Force unlock (for emergency situations)
     */
    public void forceUnlock(String lockKey) {
        if (!lockEnabled) {
            return;
        }

        try {
            RLock lock = redissonClient.getLock("workflow:" + lockKey);
            lock.forceUnlock();
            log.warn("Force unlocked distributed lock for key: {}", lockKey);
        } catch (Exception e) {
            log.error("Error force unlocking: {}", lockKey, e);
        }
    }

    /**
     * Get lock information for monitoring
     */
    public LockInfo getLockInfo(String lockKey) {
        if (!lockEnabled) {
            return new LockInfo(lockKey, false, 0, null);
        }

        RLock lock = redissonClient.getLock("workflow:" + lockKey);
        return new LockInfo(
            lockKey,
            lock.isLocked(),
            lock.remainTimeToLive(),
            null  // Thread ID not available in Redisson RLock
        );
    }

    public static class LockInfo {
        public final String lockKey;
        public final boolean locked;
        public final long remainingTime;
        public final Long threadId;

        public LockInfo(String lockKey, boolean locked, long remainingTime, Long threadId) {
            this.lockKey = lockKey;
            this.locked = locked;
            this.remainingTime = remainingTime;
            this.threadId = threadId;
        }
    }
}
