# Payment Workflow Testing with Postman

## 🚀 **Setup Postman Environment**

### **1. Create Environment**
```json
{
  "baseUrl": "http://localhost:8080/api",
  "workflowId": "",
  "paymentId": "",
  "scheduleId": ""
}
```

### **2. Start Application**
```bash
# Set fast testing properties for quick feedback
export JAVA_OPTS="-Dpayment.monitor.threshold=3 -Dpayment.monitor.wait.duration=5000"

# Start Spring Boot
mvn spring-boot:run
```

---

## 📋 **Test Scenarios với Postman**

### **Scenario 1: Payment Schedule Management**

#### **1.1 Tạo Payment Schedule**
```
Method: POST
URL: {{baseUrl}}/workflows/schedules/payment
Headers:
  Content-Type: application/json

Body:
{
    "scheduleId": "daily-payment-test-001",
    "cron": "0 2 * * *"  // 2 AM daily
}
```

**Expected Response:**
```json
{
    "status": "success",
    "message": "Payment processing schedule created/updated successfully",
    "scheduleId": "daily-payment-test-001"
}
```

#### **1.2 Xem Tất Cả Schedules**
```
Method: GET
URL: {{baseUrl}}/workflows/schedules
```

**Expected Response:**
```json
[
    {
        "scheduleId": "daily-payment-test-001",
        "cronExpression": "0 2 * * *",
        "workflowType": "PaymentMonitorWorkflow",
        "status": "ACTIVE"
    }
]
```

#### **1.3 Xem Chi Tiết Schedule**
```
Method: GET
URL: {{baseUrl}}/workflows/schedules/daily-payment-test-001
```

---

### **Scenario 2: Manual Payment Processing**

#### **2.1 Tạo Payment Workflow (Không qua Schedule)**
```
Method: POST
URL: {{baseUrl}}/workflows/payment/start
Headers:
  Content-Type: application/json

Body:
{
    "paymentId": "PAY_TEST_001",
    "accountId": "1234567890123456",
    "amount": 500000.0,
    "currency": "VND",
    "tenantId": "BANK_A",
    "userId": "USER_123"
}
```

**Expected Response:**
```json
{
    "processInstanceId": "PAY_TEST_001:runId",
    "workflowId": "payment-workflow-PAY_TEST_001",
    "status": "RUNNING"
}
```

#### **2.2 Theo Dõi Payment Status**
```
Method: GET
URL: {{baseUrl}}/workflows/payment-workflow-PAY_TEST_001/status
```

**Expected Response:**
```json
{
    "workflowId": "payment-workflow-PAY_TEST_001",
    "status": "VALIDATING_PAYMENT"
}
```

#### **2.3 Theo Dõi Payment Progress**
```
Method: GET
URL: {{baseUrl}}/workflows/payment-workflow-PAY_TEST_001/progress
```

**Expected Response:**
```json
{
    "currentStep": "EXECUTING_PAYMENT",
    "progressPercentage": 80,
    "status": "PROCESSING",
    "timestamp": "2025-12-24T15:30:00"
}
```

---

### **Scenario 3: Monitor Workflow Management**

#### **3.1 Trigger Payment Check (Signal)**
```
Method: POST
URL: {{baseUrl}}/workflows/payment-monitor-daily-payment-test-001/signal/trigger-payment-check
Headers:
  Content-Type: application/json

Body:
{
    "paymentBatchId": "BATCH_TEST_001"
}
```

**Expected Response:**
```json
{
    "status": "success",
    "message": "Payment check triggered successfully"
}
```

#### **3.2 Stop Monitor Workflow**
```
Method: POST
URL: {{baseUrl}}/workflows/payment-monitor-daily-payment-test-001/stop-monitoring
```

**Expected Response:**
```json
{
    "status": "success",
    "message": "Payment monitoring stopped successfully"
}
```

---

### **Scenario 4: Advanced Features Testing**

#### **4.1 Test Rate Limiting**
```
Method: POST
URL: {{baseUrl}}/workflows/payment/start
Headers:
  Content-Type: application/json

Body (Gửi nhiều lần nhanh):
{
    "paymentId": "PAY_RATE_TEST_001",
    "accountId": "1234567890123456",
    "amount": 100000.0,
    "currency": "VND",
    "tenantId": "BANK_A",
    "userId": "USER_123"
}
```

**Expected Response (sau vài lần):**
```json
{
    "error": "Rate limit exceeded for tenant: BANK_A, user: USER_123"
}
```

#### **4.2 Test Priority Queue (Nếu implement API)**
```
Method: POST
URL: {{baseUrl}}/workflows/payment/submit-priority
Headers:
  Content-Type: application/json

Body:
{
    "paymentId": "PAY_PRIORITY_001",
    "accountId": "1234567890123456",
    "amount": 15000000.0,
    "currency": "VND",
    "priority": "HIGH_VALUE",
    "tenantId": "BANK_A",
    "userId": "USER_123"
}
```

#### **4.3 Test Batch Processing (Nếu implement API)**
```
Method: POST
URL: {{baseUrl}}/workflows/payment/process-batch
Headers:
  Content-Type: application/json

Body:
{
    "paymentIds": ["PAY_BATCH_001", "PAY_BATCH_002", "PAY_BATCH_003"],
    "tenantId": "BANK_A",
    "userId": "USER_123"
}
```

**Expected Response:**
```json
{
    "totalPayments": 3,
    "successCount": 3,
    "failureCount": 0,
    "processingTimeMs": 2500,
    "successRate": 1.0
}
```

---

### **Scenario 5: Error Handling & Recovery**

#### **5.1 Test Payment Failure (Invalid Account)**
```
Method: POST
URL: {{baseUrl}}/workflows/payment/start
Headers:
  Content-Type: application/json

Body:
{
    "paymentId": "PAY_FAIL_TEST_001",
    "accountId": "9999999999999999",  // Invalid account
    "amount": 500000.0,
    "currency": "VND",
    "tenantId": "BANK_A",
    "userId": "USER_123"
}
```

**Expected Response:**
```json
{
    "error": "Account verification failed: Account not found"
}
```

#### **5.2 Check Dead Letter Queue Status (Nếu implement API)**
```
Method: GET
URL: {{baseUrl}}/workflows/dlq/status
```

**Expected Response:**
```json
{
    "queueSize": 1,
    "failedPayments": [
        {
            "paymentId": "PAY_FAIL_TEST_001",
            "error": "Account verification failed",
            "retryCount": 3,
            "tenantId": "BANK_A"
        }
    ]
}
```

---

## 🔧 **Postman Collection Setup**

### **1. Import Collection**
```json
{
    "info": {
        "name": "Payment Workflow API",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "Schedule Management",
            "item": [
                {
                    "name": "Create Payment Schedule",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\"scheduleId\": \"daily-payment-test-001\", \"cron\": \"0 2 * * *\"}"
                        },
                        "url": {
                            "raw": "{{baseUrl}}/workflows/schedules/payment",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflows", "schedules", "payment"]
                        }
                    }
                },
                {
                    "name": "Get All Schedules",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{baseUrl}}/workflows/schedules",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflows", "schedules"]
                        }
                    }
                }
            ]
        },
        {
            "name": "Payment Processing",
            "item": [
                {
                    "name": "Start Payment Workflow",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\"paymentId\": \"PAY_TEST_001\", \"accountId\": \"1234567890123456\", \"amount\": 500000.0, \"currency\": \"VND\", \"tenantId\": \"BANK_A\", \"userId\": \"USER_123\"}"
                        },
                        "url": {
                            "raw": "{{baseUrl}}/workflows/payment/start",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflows", "payment", "start"]
                        }
                    }
                },
                {
                    "name": "Get Payment Status",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{baseUrl}}/workflows/{{workflowId}}/status",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflows", "{{workflowId}}", "status"]
                        }
                    }
                },
                {
                    "name": "Get Payment Progress",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{baseUrl}}/workflows/{{workflowId}}/progress",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflows", "{{workflowId}}", "progress"]
                        }
                    }
                }
            ]
        },
        {
            "name": "Monitor Management",
            "item": [
                {
                    "name": "Trigger Payment Check",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\"paymentBatchId\": \"BATCH_TEST_001\"}"
                        },
                        "url": {
                            "raw": "{{baseUrl}}/workflows/{{workflowId}}/signal/trigger-payment-check",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflows", "{{workflowId}}", "signal", "trigger-payment-check"]
                        }
                    }
                },
                {
                    "name": "Stop Monitoring",
                    "request": {
                        "method": "POST",
                        "header": [],
                        "url": {
                            "raw": "{{baseUrl}}/workflows/{{workflowId}}/stop-monitoring",
                            "host": ["{{baseUrl}}"],
                            "path": ["workflows", "{{workflowId}}", "stop-monitoring"]
                        }
                    }
                }
            ]
        }
    ]
}
```

### **2. Test Scripts (Postman)**
```javascript
// Test script for status checking
pm.test("Status is valid", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.be.oneOf(["INITIALIZED", "VALIDATING", "PROCESSING", "COMPLETED", "FAILED"]);
});

// Test script for progress monitoring
pm.test("Progress is valid", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.progressPercentage).to.be.at.least(0).and.at.most(100);
});

// Set environment variables
pm.environment.set("workflowId", pm.response.json().workflowId);
```

---

## 📊 **Monitoring & Debugging**

### **1. Workflow Logs**
```bash
# View application logs
tail -f logs/spring.log

# Look for workflow events
grep "Payment Progress" logs/spring.log
grep "Advanced scheduler" logs/spring.log
```

### **2. Temporal UI**
```
Open: http://localhost:8233 (Temporal Web UI)

- View active workflows
- Check workflow history
- Monitor activity executions
- Debug failed workflows
```

### **3. Health Check Endpoints**
```
GET /actuator/health        # Application health
GET /actuator/metrics       # Application metrics
GET /actuator/workflow-stats # Custom workflow metrics
```

---

## 🎯 **Common Test Cases**

### **✅ Happy Path Test**
1. Create payment schedule
2. Start payment workflow
3. Monitor progress until COMPLETED
4. Verify all events recorded

### **❌ Error Path Test**
1. Start payment with invalid account
2. Monitor failure status
3. Check DLQ contains failed payment
4. Verify compensation events

### **⚡ Performance Test**
1. Send multiple payments simultaneously
2. Monitor rate limiting kicks in
3. Check batch processing performance
4. Verify circuit breaker activation

### **🔄 Recovery Test**
1. Stop monitoring workflow
2. Verify graceful shutdown
3. Restart and check state recovery
4. Test continue-as-new functionality

---

## 🚀 **Advanced Testing Scenarios**

### **Load Testing với JMeter/Postman Runner**
```javascript
// Postman Runner configuration
{
    "iterations": 100,
    "delay": 100,  // 100ms between requests
    "data": [
        {"paymentId": "PAY_LOAD_001", "amount": 100000},
        {"paymentId": "PAY_LOAD_002", "amount": 200000},
        // ... 98 more entries
    ]
}
```

### **Chaos Engineering Test**
```javascript
// Simulate bank API failures
// Test circuit breaker activation
// Verify fallback behavior
// Check recovery mechanisms
```

### **Multi-Tenant Test**
```javascript
// Test different tenants hit rate limits independently
// Verify tenant isolation
// Check tenant-specific metrics
```

---

## 🎉 **Quick Start**

1. **Import Postman Collection** (JSON above)
2. **Set Environment Variables** (`baseUrl`, etc.)
3. **Start Application** with test properties
4. **Run Tests**:
   - Create schedule → Start workflow → Monitor progress
   - Test rate limiting → Test error handling → Test monitoring

**🎯 Với Postman, bạn có thể test toàn bộ payment workflow enterprise-grade chỉ với vài clicks!** 🚀
