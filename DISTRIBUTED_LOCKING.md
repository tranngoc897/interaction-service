# Distributed Locking with Redisson

## Overview

The Custom Workflow Engine supports distributed locking using Redisson to coordinate scheduler operations across multiple application instances. This ensures that background jobs (retry, timeout, cleanup) run only once even when multiple pods are deployed.

## Why Distributed Locking?

### Problem
When running multiple instances of the workflow engine:
- Retry scheduler runs on every pod
- Multiple pods process the same failed steps
- Timeout scheduler triggers duplicate timeouts
- Race conditions cause inconsistent state

### Solution
Distributed locking ensures only one instance processes each type of scheduled job at a time.

## Configuration

### 1. Enable Redis

```yaml
# application.yaml
redis:
  enabled: true
  host: your-redis-host
  port: 6379
  timeout: 2000ms
```

### 2. Enable Redisson

```yaml
# application.yaml
redisson:
  enabled: true
  config: |
    singleServerConfig:
      address: redis://your-redis-host:6379
      timeout: 2000
      retryAttempts: 3
      retryInterval: 1500
      database: 0
      connectionMinimumIdleSize: 2
      connectionPoolSize: 10
```

### 3. Enable Workflow Locking

```yaml
# application.yaml
workflow:
  lock:
    enabled: true
    timeout: 30     # Lock timeout in seconds
    wait-time: 0    # Wait time for lock acquisition
```

## Lock Keys

The system uses the following lock keys:

- `workflow:retry-scheduler` - Coordinates retry job execution
- `workflow:timeout-scheduler` - Coordinates timeout job execution
- `workflow:resume-scheduler` - Coordinates resume job execution (if implemented)

## How It Works

### Lock Acquisition Flow

```java
distributedLockService.executeWithLock("retry-scheduler", () -> {
    // This code runs only on one instance at a time
    List<StepExecution> steps = findRetryableSteps();
    processRetries(steps);
    return null;
});
```

### Lock Behavior

1. **Try Lock**: Attempts to acquire lock immediately
2. **No Wait**: If lock unavailable, skips execution (no blocking)
3. **Auto Release**: Lock automatically released after timeout or execution
4. **Fault Tolerant**: If Redis unavailable, falls back to no locking

## Monitoring Locks

### Check Lock Status

```bash
# Via API (if implemented)
GET /admin/locks/retry-scheduler

# Response
{
  "lockKey": "workflow:retry-scheduler",
  "locked": true,
  "remainingTime": 25000,
  "threadId": 12345
}
```

### Redis CLI

```bash
# Check if locks exist
KEYS workflow:*

# Check lock details
HGETALL workflow:retry-scheduler
```

## Deployment Scenarios

### Single Instance (Development)

```yaml
workflow:
  lock:
    enabled: false  # No locking needed
```

### Multiple Instances (Production)

```yaml
workflow:
  lock:
    enabled: true   # Enable distributed locking
```

### Kubernetes with Redis

```yaml
# Redis Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379

---
# Workflow Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: workflow-engine
spec:
  replicas: 3  # Multiple instances
  template:
    spec:
      containers:
      - name: workflow
        env:
        - name: WORKFLOW_LOCK_ENABLED
          value: "true"
        - name: REDISSON_ENABLED
          value: "true"
```

## Troubleshooting

### Lock Not Acquired

**Symptoms:**
- Scheduler jobs not running
- Multiple instances processing same items

**Causes:**
- Redis connection issues
- Lock timeout too short
- High contention

**Solutions:**
```yaml
workflow:
  lock:
    timeout: 60      # Increase timeout
    wait-time: 5     # Allow brief waiting
```

### Redis Connection Issues

**Symptoms:**
- Lock service logs errors
- Fallback to no locking

**Solutions:**
- Check Redis connectivity
- Verify Redis configuration
- Monitor Redis metrics

### Lock Leaks

**Symptoms:**
- Jobs stop running after some time
- Lock keys persist in Redis

**Causes:**
- Application crashes during lock holding
- Long-running jobs exceed timeout

**Solutions:**
- Set appropriate lock timeouts
- Implement lock monitoring
- Use emergency unlock if needed

## Performance Considerations

### Lock Timeout
- Too short: Jobs may not complete
- Too long: Delays when instances crash
- Recommended: 30-60 seconds for scheduler jobs

### Redis Performance
- Use connection pooling
- Monitor Redis CPU/memory
- Consider Redis cluster for high availability

### Application Impact
- Locking adds minimal latency (< 10ms)
- Failed lock acquisition is fast (no waiting)
- No impact on user-facing APIs

## Best Practices

### 1. Lock Granularity
- Use coarse-grained locks for schedulers
- Avoid fine-grained locks per workflow instance
- Balance between safety and performance

### 2. Error Handling
- Always handle lock acquisition failures gracefully
- Log lock-related events for monitoring
- Implement fallback behavior

### 3. Monitoring
- Monitor lock acquisition rates
- Alert on lock failures
- Track lock holding times

### 4. Configuration
- Use environment-specific configurations
- Test locking behavior in staging
- Document lock key usage

## Migration Guide

### From Single Instance to Multi-Instance

1. **Deploy Redis**
   ```bash
   helm install redis bitnami/redis
   ```

2. **Update Configuration**
   ```yaml
   redisson:
     enabled: true
   workflow:
     lock:
       enabled: true
   ```

3. **Scale Application**
   ```bash
   kubectl scale deployment workflow-engine --replicas=3
   ```

4. **Monitor Lock Behavior**
   - Check scheduler logs
   - Verify no duplicate processing
   - Monitor Redis performance

### Rollback Plan

If distributed locking causes issues:

1. **Disable locking temporarily**
   ```yaml
   workflow:
     lock:
       enabled: false
   ```

2. **Scale to single instance**
   ```bash
   kubectl scale deployment workflow-engine --replicas=1
   ```

3. **Investigate and fix issues**

4. **Re-enable locking**

This ensures the system remains operational during troubleshooting.
