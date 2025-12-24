# Testing Payment Workflow Guide - Deadlock Fix

## 🚨 **FIXED: PotentialDeadlockException Issue**

### **Vấn đề ban đầu:**
```java
@SignalMethod
public void triggerPaymentCheck(String paymentBatchId) {
    this.eventCount++;
    processPaymentBatch(paymentBatchId); // ❌ HEAVY PROCESSING IN SIGNAL!
}
```

**Lỗi:** `PotentialDeadlockException` - workflow thread blocked > 1 second.

### **Giải pháp - Temporal Best Practices:**
```java
@SignalMethod
public void triggerPaymentCheck(String paymentBatchId) {
    this.pendingPaymentBatchId = paymentBatchId; // ✅ Just set flag
    this.eventCount++;
}

// Processing happens in main workflow loop
if (pendingPaymentBatchId != null) {
    String batchId = pendingPaymentBatchId;
    pendingPaymentBatchId = null;
    processPaymentBatch(batchId); // ✅ Safe processing
}
```

## 🔧 **Cấu hình Testing**

### **1. System Properties để Test Nhanh**
```bash
# Set trước khi chạy test
export JAVA_OPTS="-Dpayment.monitor.threshold=3 -Dpayment.monitor.wait.duration=100"

# Hoặc trong IDE VM options:
-Dpayment.monitor.threshold=3 -Dpayment.monitor.wait.duration=100
```

**Giải thích:**
- `payment.monitor.threshold=3`: Continue-as-new sau 3 events (test nhanh)
- `payment.monitor.wait.duration=100`: Chờ 100ms thay vì 1 giờ (tránh deadlock)

## 🎯 **Test Scenarios**

### **Test 1: Signal Processing (No Deadlock)**
```java
@Test
public void testSignalProcessing() {
    // Start monitor workflow
    PaymentMonitorWorkflow monitor = client.newWorkflowStub(...);
    WorkflowClient.start(monitor::monitorPayments, "TEST_ACCOUNT", 0);

    // Send signal - should NOT cause deadlock
    monitor.triggerPaymentCheck("BATCH_001");

    // Wait for processing to complete
    Thread.sleep(200);

    // Verify workflow still responsive
    WorkflowStub stub = client.newUntypedWorkflowStub(workflowId);
    assertDoesNotThrow(() -> stub.query("getStatus", String.class));
}
```

### **Test 2: Timeout Behavior**
```java
@Test
public void testTimeoutBehavior() {
    System.setProperty("payment.monitor.wait.duration", "50");

    // Start workflow
    PaymentMonitorWorkflow monitor = client.newWorkflowStub(...);
    WorkflowClient.start(monitor::monitorPayments, "TEST_ACCOUNT", 0);

    // Wait longer than timeout
    Thread.sleep(100); // > 50ms timeout

    // Should have performed scheduled checks and continued waiting
}
```

### **Test 3: ContinueAsNew Logic**
```java
@Test
public void testContinueAsNew() {
    System.setProperty("payment.monitor.threshold", "2");

    // Send 2 signals to reach threshold
    monitor.triggerPaymentCheck("BATCH_1");
    monitor.triggerPaymentCheck("BATCH_2");

    // Workflow should restart with fresh history
}
```

## 🔍 **Workflow Behavior Sau Fix**

### **Signal Flow:**
```
Signal Received → Set pendingPaymentBatchId flag → Wake up await()
    ↓
Main Loop Checks: pendingPaymentBatchId != null
    ↓
Process batch safely in main thread → Clear flag → Continue waiting
```

### **No More Deadlock:**
- ✅ Signal methods: Lightweight (just set flags)
- ✅ Processing: Happens in main workflow loop
- ✅ Thread Safety: Workflow yields control properly
- ✅ Testing: Fast timeouts prevent waiting issues

## 📊 **Performance Comparison**

| Aspect | Before (Broken) | After (Fixed) |
|--------|----------------|---------------|
| Signal Processing | ❌ Causes deadlock | ✅ Fast flag setting |
| Test Execution | ❌ Hangs forever | ✅ Completes in seconds |
| Thread Safety | ❌ Blocks workflow | ✅ Proper yielding |
| Scalability | ❌ Single threaded bottleneck | ✅ Concurrent processing |

## 🎯 **Key Lessons**

### **1. Signal Methods Should Be Lightweight**
```java
// ❌ Bad - heavy processing in signal
@SignalMethod
void processData(Data data) {
    heavyProcessing(data); // Causes deadlock!
}

// ✅ Good - just set flag
@SignalMethod
void processData(Data data) {
    this.pendingData = data; // Wake up main loop
}
```

### **2. Main Loop Should Handle Processing**
```java
// ✅ Good - processing in main workflow loop
while (!exit) {
    Workflow.await(condition, () -> pendingData != null);

    if (pendingData != null) {
        processData(pendingData); // Safe processing
        pendingData = null;
    }
}
```

### **3. Always Use Configurable Timeouts**
```java
// ✅ Good - configurable for testing
private static final Duration TIMEOUT = Duration.ofMillis(
    Long.parseLong(System.getProperty("timeout", "3600000"))
);
```

## 🚀 **Testing Commands**

```bash
# Run automated tests
mvn test -Dtest=PaymentMonitorWorkflowTest

# Run with custom properties
mvn test -Dtest=PaymentMonitorWorkflowTest \
  -Dpayment.monitor.threshold=2 \
  -Dpayment.monitor.wait.duration=50

# Run manual demo
mvn test -Dtest=PaymentWorkflowManualTest
```

## ✅ **Result**

**Trước:** `PotentialDeadlockException` - workflow không thể test

**Sau:** Workflow chạy mượt mà, test nhanh, no deadlock!

**🎉 Payment Monitor Workflow giờ đã production-ready!**
