# Backpressure Handling Guide

## 📚 Overview
Backpressure is a resilience pattern that protects your system from overload by limiting concurrent operations. This prevents cascading failures, OOM errors, and system crashes during traffic spikes.

## 🎯 Problem Statement

### Without Backpressure
```
Normal Load: 100 workflows/sec ✅
Traffic Spike: 10,000 workflows/sec 💥
    ↓
Thread Pool Exhausted
    ↓
DB Connection Pool Exhausted
    ↓
Memory Exhausted (OOM)
    ↓
System Crash 💀
```

### With Backpressure
```
Normal Load: 100 workflows/sec ✅
Traffic Spike: 10,000 workflows/sec
    ↓
System accepts 100 workflows/sec ✅
    ↓
Remaining 9,900 requests get HTTP 503 (Retry Later)
    ↓
System Stable 💪
```

---

## 🔧 How It Works

### 1. **Semaphore-Based Throttling**

```java
// System configured for max 100 concurrent workflows
Semaphore workflowSemaphore = new Semaphore(100);

// Request 1-100: Acquire permit ✅
workflowSemaphore.tryAcquire(); // Success

// Request 101: No permits available ❌
workflowSemaphore.tryAcquire(); // Fails → HTTP 503
```

### 2. **Try-With-Resources Pattern**

```java
// Automatic permit release even on exceptions
try (WorkflowPermit permit = backpressureService.acquireWorkflowPermit()) {
    // Execute workflow
    createWorkflow();
} // Permit automatically released here
```

### 3. **Fair Queuing**

```java
// Fair mode ensures FIFO ordering
Semaphore semaphore = new Semaphore(100, true); // true = fair
```

---

## 📊 Configuration

### Basic Configuration

```yaml
# application.yml
backpressure:
  max-concurrent-workflows: 100    # Max parallel workflows
  max-concurrent-steps: 500        # Max parallel steps
  acquire-timeout-ms: 5000         # Wait time before rejection
```

### Environment-Specific Settings

| Environment | Workflows | Steps | Timeout | Notes |
|-------------|-----------|-------|---------|-------|
| **Development** | 10 | 50 | 3000ms | Low capacity for local testing |
| **Staging** | 50 | 250 | 5000ms | Medium capacity |
| **Production** | 200 | 1000 | 10000ms | High capacity |

### Capacity Planning Formula

```
Max Workflows = (Available CPU Cores × 25)
Max Steps = Max Workflows × 5
Timeout = Average Workflow Duration × 2
```

**Example:**
- 8 CPU cores → 200 max workflows
- 200 workflows → 1000 max steps
- 2s avg duration → 4000ms timeout

---

## 🚀 Usage

### In Service Layer

```java
@Service
public class OnboardingService {
    
    private final BackpressureService backpressureService;
    
    public OnboardingInstance start(String userId) {
        // Acquire permit (blocks if at capacity)
        try (WorkflowPermit permit = backpressureService.acquireWorkflowPermit()) {
            
            // Create workflow
            return createWorkflow(userId);
            
        } catch (BackpressureException e) {
            // System overloaded
            throw new ServiceUnavailableException("Please retry later");
        }
    }
}
```

### In Controller Layer

```java
@PostMapping("/start")
public ResponseEntity<?> startOnboarding(@RequestBody Request req) {
    try {
        OnboardingInstance instance = onboardingService.start(req.getUserId());
        return ResponseEntity.ok(instance);
        
    } catch (ServiceUnavailableException e) {
        return ResponseEntity.status(503)
            .header("Retry-After", "60") // Retry after 60 seconds
            .body(Map.of(
                "error", "System overloaded",
                "message", "Please try again in 1 minute"
            ));
    }
}
```

---

## 📈 Monitoring

### Metrics Endpoint

```bash
# Get current metrics
curl http://localhost:8080/admin/backpressure/metrics

# Response
{
  "maxConcurrentWorkflows": 100,
  "activeWorkflows": 75,
  "availableWorkflowSlots": 25,
  "maxConcurrentSteps": 500,
  "availableStepSlots": 350,
  "totalRejected": 42,
  "loadPercentage": 75.0
}
```

### Health Check

```bash
# Check system health
curl http://localhost:8080/admin/backpressure/health

# Healthy Response (200 OK)
{
  "healthy": true,
  "status": "UP",
  "loadPercentage": 65.0,
  "message": "System operating normally"
}

# Overloaded Response (503 Service Unavailable)
{
  "healthy": false,
  "status": "DEGRADED",
  "loadPercentage": 95.0,
  "message": "System under high load"
}
```

### Capacity Information

```bash
curl http://localhost:8080/admin/backpressure/capacity
```

---

## 🎯 Best Practices

### 1. **Set Appropriate Limits**

```yaml
# ❌ Too Low - Wastes resources
backpressure:
  max-concurrent-workflows: 10  # System can handle 100

# ❌ Too High - Risk of OOM
backpressure:
  max-concurrent-workflows: 1000  # System can only handle 100

# ✅ Just Right
backpressure:
  max-concurrent-workflows: 100  # Matches system capacity
```

### 2. **Monitor Rejection Rate**

```sql
-- Alert if > 5% rejection rate
SELECT 
    total_rejected,
    total_requests,
    (total_rejected::float / total_requests * 100) as rejection_rate
FROM metrics
WHERE rejection_rate > 5.0;
```

### 3. **Implement Retry Logic on Client**

```javascript
// Client-side retry with exponential backoff
async function startOnboarding(userId) {
    let retries = 3;
    let delay = 1000; // 1 second
    
    while (retries > 0) {
        try {
            return await api.post('/onboarding/start', { userId });
        } catch (error) {
            if (error.status === 503 && retries > 0) {
                await sleep(delay);
                delay *= 2; // Exponential backoff
                retries--;
            } else {
                throw error;
            }
        }
    }
}
```

### 4. **Auto-Scaling Based on Load**

```yaml
# Kubernetes HPA (Horizontal Pod Autoscaler)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: interaction-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: interaction-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Pods
    pods:
      metric:
        name: backpressure_load_percentage
      target:
        type: AverageValue
        averageValue: "70"  # Scale up when > 70%
```

---

## 🔍 Troubleshooting

### High Rejection Rate

**Symptom:** `totalRejected` increasing rapidly

**Causes:**
1. Traffic spike
2. Slow external services
3. Database bottleneck
4. Insufficient capacity

**Solutions:**
1. Scale horizontally (add more pods)
2. Increase `max-concurrent-workflows`
3. Optimize slow queries
4. Add caching

### System Always at 100% Capacity

**Symptom:** `loadPercentage` always 95-100%

**Causes:**
1. Workflows not completing (stuck)
2. Permits not being released (memory leak)
3. Capacity too low

**Solutions:**
1. Check for stuck workflows
2. Review try-with-resources usage
3. Increase capacity limits

### Permits Not Released

**Symptom:** `availableWorkflowSlots` decreasing over time

**Causes:**
1. Exception before permit release
2. Missing try-with-resources
3. Thread interruption

**Solutions:**
```java
// ❌ Bad - permit may not be released
WorkflowPermit permit = backpressureService.acquireWorkflowPermit();
doWork();
permit.close(); // May not execute if exception

// ✅ Good - always released
try (WorkflowPermit permit = backpressureService.acquireWorkflowPermit()) {
    doWork();
} // Automatically released
```

---

## 📊 Comparison with Other Patterns

| Pattern | Purpose | When to Use |
|---------|---------|-------------|
| **Backpressure** | Limit concurrent operations | Protect from overload |
| **Rate Limiting** | Limit requests per time | Prevent abuse |
| **Circuit Breaker** | Stop calling failing service | Prevent cascading failures |
| **Bulkhead** | Isolate thread pools | Prevent resource starvation |

**Use Together:**
```
Request → Rate Limiter → Backpressure → Circuit Breaker → Service
```

---

## 🎓 Summary

**What you get:**
- ✅ Protection from traffic spikes
- ✅ Prevents OOM errors
- ✅ Graceful degradation
- ✅ System stability
- ✅ Predictable performance

**Your system is now overload-resistant! 💪**
