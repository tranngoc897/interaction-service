# Camunda JavaDelegate Implementation Guide

## 📋 Overview

This document describes all JavaDelegate implementations for the Camunda BPMN onboarding process.

## 🎯 BPMN Process: `onboarding-process`

### Process Flow

```
Start Event
    ↓
Collect Personal Information (User Task)
    ↓
Upload Documents (User Task)
    ↓
AML/KYC Compliance Check (Service Task) ← ComplianceDelegate
    ↓
Manual Review Required? (Gateway)
    ├─ YES → Manual Review (User Task) → Review Decision (Gateway)
    │                                        ├─ APPROVED → Continue
    │                                        └─ REJECTED → End (Rejected)
    └─ NO → Continue
    ↓
Create Customer Account (Service Task) ← AccountCreationDelegate
    ↓
Send Welcome Email (Service Task) ← NotificationDelegate
    ↓
End Event (Completed)
```

---

## 🔧 JavaDelegate Implementations

### 1. **ComplianceDelegate** ✅

**Location:** `com.ngoctran.interactionservice.delegate.ComplianceDelegate`

**BPMN Task:** `Task_ComplianceCheck` - AML/KYC Compliance Check

**Purpose:** Perform comprehensive compliance checks including AML screening, KYC verification, and sanctions screening.

**Input Variables:**
- `caseId` (String) - Case identifier
- `applicantId` (String) - Applicant identifier
- `applicantData` (Map<String, Object>) - Applicant information

**Output Variables:**
- `compliancePassed` (Boolean) - Overall compliance result
- `complianceStatus` (String) - Status: PASSED, REVIEW_NEEDED, FAILED, ERROR
- `riskLevel` (String) - Risk assessment level
- `amlStatus` (String) - AML screening status
- `kycStatus` (String) - KYC verification status
- `sanctionsStatus` (String) - Sanctions screening status
- `complianceError` (String) - Error message if failed

**Dependencies:**
- `ComplianceService` - Performs actual compliance checks
- `DmnDecisionService` - Risk assessment using DMN rules

**Business Logic:**
1. Perform AML screening via ComplianceService
2. Perform KYC verification via ComplianceService
3. Perform sanctions screening via ComplianceService
4. Assess risk level using DMN decision service
5. Determine overall compliance status
6. Set process variables for BPMN gateway decisions

**Gateway Decision:**
```xml
${complianceStatus == 'PASSED'} → Proceed to Account Creation
${complianceStatus == 'REVIEW_NEEDED'} → Manual Review Required
```

---

### 2. **AccountCreationDelegate** ✅

**Location:** `com.ngoctran.interactionservice.delegate.AccountCreationDelegate`

**BPMN Task:** `Task_CreateAccount` - Create Customer Account

**Purpose:** Create customer account in core banking system after successful compliance checks.

**Input Variables:**
- `caseId` (String) - Case identifier
- `applicantId` (String) - Applicant identifier
- `applicantData` (Map<String, Object>) - Customer information
  - `fullName` (String)
  - `email` (String)
  - `phoneNumber` (String)
  - `idNumber` (String)

**Output Variables:**
- `accountCreated` (Boolean) - Account creation result
- `accountCreationStatus` (String) - Status: SUCCESS, FAILED, ERROR
- `accountNumber` (String) - Generated account number
- `customerId` (String) - Generated customer ID (UUID)
- `customerName` (String) - Customer full name
- `customerEmail` (String) - Customer email
- `accountCreationError` (String) - Error message if failed

**Business Logic:**
1. Extract customer information from applicantData
2. Generate unique account number (format: ACC + timestamp + random)
3. Generate customer ID (UUID)
4. Call core banking system to create account (simulated)
5. Update case status to "APPROVED" in database
6. Set process variables for next steps

**Account Number Format:**
```
ACC{timestamp:6}{random:4}
Example: ACC123456789
```

**Core Banking Integration:**
- Currently simulated with 95% success rate
- In production: Replace with actual banking API call
- Includes retry logic and error handling

**Database Updates:**
- Updates `flw_case` table status to "APPROVED"
- Updates `updated_at` timestamp

---

### 3. **NotificationDelegate** ✅

**Location:** `com.ngoctran.interactionservice.delegate.NotificationDelegate`

**BPMN Task:** `Task_SendWelcomeEmail` - Send Welcome Email

**Purpose:** Send email notifications to customers at various stages of onboarding.

**Input Variables:**
- `notificationType` (String) - Type of notification (default: WELCOME_EMAIL)
- `customerEmail` (String) - Recipient email address
- `customerName` (String) - Customer name
- `accountNumber` (String) - Account number (if available)

**Output Variables:**
- `notificationSent` (Boolean) - Notification result
- `notificationStatus` (String) - Status: SENT, FAILED, SKIPPED, ERROR
- `notificationTimestamp` (String) - Timestamp of notification
- `notificationError` (String) - Error message if failed

**Supported Notification Types:**

1. **WELCOME_EMAIL** (Default)
   - Sent after successful account creation
   - Includes account number and next steps

2. **ACCOUNT_CREATED**
   - Detailed account creation confirmation
   - Includes account details and activation steps

3. **MANUAL_REVIEW_REQUIRED**
   - Notifies customer that application is under review
   - Provides expected timeline

4. **ONBOARDING_REJECTED**
   - Informs customer of application rejection
   - Provides contact information for support

5. **COMPLIANCE_ALERT**
   - Requests additional documentation
   - Includes deadline for submission

6. **Generic Notification**
   - Fallback for unknown notification types

**Email Service Integration:**
- Currently simulated with 98% success rate
- In production: Integrate with SendGrid, AWS SES, or similar
- Includes template-based email body generation

**Error Handling:**
- Non-blocking: Notification failure doesn't stop the process
- Logs all notification attempts
- Sets error variables for monitoring

---

### 4. **ComplianceCheckDelegate** ✅

**Location:** `com.ngoctran.interactionservice.delegate.ComplianceCheckDelegate`

**Note:** This is the original delegate. `ComplianceDelegate` is an alias that references the same functionality.

**Purpose:** Same as ComplianceDelegate - maintained for backward compatibility.

---

### 5. **ProductRecommendationDelegate** ✅

**Location:** `com.ngoctran.interactionservice.delegate.ProductRecommendationDelegate`

**Purpose:** Recommend banking products based on customer profile using DMN decision rules.

**Input Variables:**
- `applicantData` (Map<String, Object>) - Customer profile data

**Output Variables:**
- `recommendedProducts` (List<String>) - List of recommended products
- `eligible` (Boolean) - Product eligibility status
- `productCount` (Integer) - Number of recommended products
- `recommendationError` (String) - Error message if failed

**Dependencies:**
- `DmnDecisionService` - Product recommendation and eligibility rules

---

## 📊 Process Variables Summary

### Key Process Variables

| Variable | Type | Set By | Used By | Description |
|----------|------|--------|---------|-------------|
| `caseId` | String | Process Start | All Delegates | Case identifier |
| `applicantId` | String | Process Start | Compliance, Account | Applicant identifier |
| `applicantData` | Map | User Tasks | All Delegates | Customer information |
| `complianceStatus` | String | ComplianceDelegate | Gateway | Compliance result |
| `compliancePassed` | Boolean | ComplianceDelegate | Gateway | Pass/fail flag |
| `accountNumber` | String | AccountCreationDelegate | NotificationDelegate | Account number |
| `customerEmail` | String | AccountCreationDelegate | NotificationDelegate | Customer email |
| `customerName` | String | AccountCreationDelegate | NotificationDelegate | Customer name |
| `reviewDecision` | String | Manual Review Task | Gateway | APPROVED/REJECTED |

---

## 🔄 BPMN Gateway Conditions

### Gateway: Manual Review Required?

```xml
<!-- Proceed to Account Creation -->
<bpmn:conditionExpression>
  ${complianceStatus == 'PASSED'}
</bpmn:conditionExpression>

<!-- Manual Review Required -->
<bpmn:conditionExpression>
  ${complianceStatus == 'REVIEW_NEEDED'}
</bpmn:conditionExpression>
```

### Gateway: Review Decision

```xml
<!-- Approved -->
<bpmn:conditionExpression>
  ${reviewDecision == 'APPROVED'}
</bpmn:conditionExpression>

<!-- Rejected -->
<bpmn:conditionExpression>
  ${reviewDecision == 'REJECTED'}
</bpmn:conditionExpression>
```

---

## 🚀 Usage Examples

### Starting the Onboarding Process

```java
// Start BPMN process with required variables
Map<String, Object> variables = new HashMap<>();
variables.put("caseId", "case-123");
variables.put("applicantId", "applicant-456");
variables.put("applicantData", Map.of(
    "fullName", "John Doe",
    "email", "john.doe@example.com",
    "phoneNumber", "+1234567890",
    "idNumber", "ID123456",
    "dateOfBirth", "1990-01-01",
    "nationality", "US"
));

ProcessInstance processInstance = runtimeService
    .startProcessInstanceByKey("onboarding-process", variables);
```

### Completing Manual Review Task

```java
// Complete manual review with decision
Map<String, Object> variables = new HashMap<>();
variables.put("reviewDecision", "APPROVED"); // or "REJECTED"
variables.put("reviewComments", "All documents verified");

taskService.complete(taskId, variables);
```

---

## 🧪 Testing

### Unit Testing JavaDelegates

```java
@Test
void testAccountCreationDelegate() {
    // Mock dependencies
    CaseRepository caseRepository = mock(CaseRepository.class);
    AccountCreationDelegate delegate = new AccountCreationDelegate(caseRepository);
    
    // Mock execution
    DelegateExecution execution = mock(DelegateExecution.class);
    when(execution.getVariable("caseId")).thenReturn("case-123");
    when(execution.getVariable("applicantData")).thenReturn(Map.of(
        "fullName", "John Doe",
        "email", "john@example.com"
    ));
    
    // Execute
    delegate.execute(execution);
    
    // Verify
    verify(execution).setVariable(eq("accountCreated"), eq(true));
    verify(execution).setVariable(eq("accountCreationStatus"), eq("SUCCESS"));
}
```

---

## 📝 Configuration

### Spring Bean Registration

All delegates are automatically registered as Spring beans:

```java
@Component("complianceDelegate")
public class ComplianceDelegate implements JavaDelegate { ... }

@Component("accountCreationDelegate")
public class AccountCreationDelegate implements JavaDelegate { ... }

@Component("notificationDelegate")
public class NotificationDelegate implements JavaDelegate { ... }
```

### BPMN Configuration

```xml
<!-- Service Task Configuration -->
<bpmn:serviceTask id="Task_ComplianceCheck" 
                  name="AML/KYC Compliance Check"
                  camunda:class="com.ngoctran.interactionservice.delegate.ComplianceDelegate">
</bpmn:serviceTask>

<bpmn:serviceTask id="Task_CreateAccount" 
                  name="Create Customer Account"
                  camunda:class="com.ngoctran.interactionservice.delegate.AccountCreationDelegate">
</bpmn:serviceTask>

<bpmn:serviceTask id="Task_SendWelcomeEmail" 
                  name="Send Welcome Email"
                  camunda:class="com.ngoctran.interactionservice.delegate.NotificationDelegate">
</bpmn:serviceTask>
```

---

## 🔍 Monitoring & Logging

All delegates include comprehensive logging:

```
INFO  - Executing compliance check for process: process-instance-123
INFO  - Compliance check completed: passed=true, riskLevel=LOW, aml=PASSED, kyc=PASSED, sanctions=PASSED
INFO  - Executing account creation for process: process-instance-123
INFO  - Creating account for customer: name=John Doe, email=john@example.com, phone=+1234567890
INFO  - Account created successfully: accountNumber=ACC123456789, customerId=uuid-123, customer=John Doe
INFO  - Executing notification for process: process-instance-123
INFO  - Sending WELCOME_EMAIL notification to: John Doe (john@example.com)
INFO  - Email sent successfully to: john@example.com
```

---

## 🛠️ Production Considerations

### 1. **Core Banking Integration**
- Replace simulated account creation with actual banking API
- Implement proper authentication and security
- Add retry logic with exponential backoff
- Implement circuit breaker pattern

### 2. **Email Service Integration**
- Integrate with SendGrid, AWS SES, or similar
- Use email templates from database or CMS
- Implement email queue for reliability
- Add unsubscribe functionality

### 3. **Error Handling**
- Implement BPMN error events for critical failures
- Add compensation logic for rollback scenarios
- Configure retry policies in Camunda
- Set up dead letter queue for failed tasks

### 4. **Security**
- Encrypt sensitive data in process variables
- Implement audit logging for compliance
- Add authentication for manual review tasks
- Secure external API calls with certificates

### 5. **Performance**
- Implement caching for frequently accessed data
- Use async service tasks where appropriate
- Monitor delegate execution times
- Optimize database queries

---

## 📚 Related Documentation

- [BPMN Process Definition](../bpmn-processes/onboarding-process.bpmn)
- [Camunda Setup Guide](../CAMUNDA_SETUP_README.md)
- [Temporal Integration Design](../TEMPORAL_INTEGRATION_DESIGN.md)
- [ABB Onboarding Integration](../ABB_ONBOARDING_INTEGRATION.md)

---

## ✅ Summary

**Total JavaDelegates: 5**

1. ✅ ComplianceDelegate - AML/KYC compliance checks
2. ✅ AccountCreationDelegate - Customer account creation
3. ✅ NotificationDelegate - Email notifications
4. ✅ ComplianceCheckDelegate - Compliance (original)
5. ✅ ProductRecommendationDelegate - Product recommendations

**All delegates are:**
- ✅ Fully implemented
- ✅ Spring-managed beans
- ✅ Integrated with BPMN process
- ✅ Include comprehensive logging
- ✅ Handle errors gracefully
- ✅ Ready for production (with noted integrations)

---

**Last Updated:** 2025-12-31
**Author:** Antigravity AI
**Version:** 1.0
