# 🚀 Camunda JavaDelegate - Quick Reference

## 📋 All JavaDelegates

| # | Delegate | Bean Name | Purpose | BPMN Task |
|---|----------|-----------|---------|-----------|
| 1 | `ComplianceDelegate` | `complianceDelegate` | AML/KYC checks | `Task_ComplianceCheck` |
| 2 | `AccountCreationDelegate` | `accountCreationDelegate` | Create account | `Task_CreateAccount` |
| 3 | `NotificationDelegate` | `notificationDelegate` | Send emails | `Task_SendWelcomeEmail` |
| 4 | `ComplianceCheckDelegate` | `complianceCheckDelegate` | AML/KYC (original) | - |
| 5 | `ProductRecommendationDelegate` | `productRecommendationDelegate` | Product recommendations | - |

---

## 🔧 Usage Examples

### Start Process
```java
Map<String, Object> vars = Map.of(
    "caseId", "case-123",
    "applicantId", "app-456",
    "applicantData", Map.of(
        "fullName", "John Doe",
        "email", "john@example.com"
    )
);
runtimeService.startProcessInstanceByKey("onboarding-process", vars);
```

### Complete User Task
```java
taskService.complete(taskId, Map.of(
    "reviewDecision", "APPROVED",
    "reviewComments", "All good"
));
```

---

## 📊 Process Variables

### Input
- `caseId` - Case ID
- `applicantId` - Applicant ID
- `applicantData` - Customer info (Map)

### Output (ComplianceDelegate)
- `complianceStatus` - PASSED/REVIEW_NEEDED/FAILED
- `compliancePassed` - true/false
- `riskLevel` - LOW/MEDIUM/HIGH

### Output (AccountCreationDelegate)
- `accountNumber` - Generated account number
- `customerId` - Generated customer ID
- `accountCreated` - true/false

### Output (NotificationDelegate)
- `notificationSent` - true/false
- `notificationStatus` - SENT/FAILED

---

## 🎯 Gateway Conditions

```java
// Manual Review Gateway
${complianceStatus == 'PASSED'} → Skip review
${complianceStatus == 'REVIEW_NEEDED'} → Manual review

// Review Decision Gateway
${reviewDecision == 'APPROVED'} → Create account
${reviewDecision == 'REJECTED'} → End rejected
```

---

## 📁 File Locations

```
src/main/java/com/ngoctran/interactionservice/delegate/
├── ComplianceDelegate.java
├── ComplianceCheckDelegate.java
├── AccountCreationDelegate.java
├── NotificationDelegate.java
└── ProductRecommendationDelegate.java

bpmn-processes/
└── onboarding-process.bpmn
```

---

## 🧪 Testing

```java
@Test
void testDelegate() {
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable("caseId")).thenReturn("case-123");
    
    delegate.execute(execution);
    
    verify(execution).setVariable(eq("accountCreated"), eq(true));
}
```

---

## 📚 Documentation

- **CAMUNDA_DELEGATES_GUIDE.md** - Full guide
- **CAMUNDA_IMPLEMENTATION_SUMMARY.md** - Summary
- **CAMUNDA_DELEGATES_README.md** - Overview

---

**Build:** `mvn clean compile -DskipTests` ✅
