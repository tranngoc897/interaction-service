# Workflow Versioning Guide

## 📚 Overview
This guide explains how to safely evolve your workflow code while maintaining backward compatibility with running instances, similar to Temporal's `Workflow.getVersion()`.

## 🎯 Problem Statement
**Scenario:** You have 1000 users in the middle of onboarding. You need to deploy new validation logic. How do you ensure:
- Old users continue with old logic (V1)
- New users use new logic (V2)
- No disruption to running workflows

## ✅ Solution: WorkflowVersionManager

### Step 1: Identify the Change Point
When you need to change business logic, wrap it with version checking:

```java
@Component("MY_STEP_HANDLER")
public class MyStepHandler implements StepHandler {
    
    private final WorkflowHistoryService historyService;
    
    @Override
    public StepResult execute(UUID instanceId) {
        // Get version for this specific change
        int version = WorkflowVersionManager.getVersion(
            "my-validation-change",  // Unique ID for this change
            1,                        // Minimum supported version
            2                         // Current/maximum version
        );
        
        // Record the decision
        WorkflowVersionManager.recordVersionMarker(
            instanceId, "my-validation-change", version, historyService
        );
        
        try {
            if (version == 1) {
                return executeV1Logic();
            } else {
                return executeV2Logic();
            }
        } finally {
            WorkflowVersionManager.clear();
        }
    }
}
```

### Step 2: How It Works

#### First Execution (New Workflow)
1. `getVersion()` returns `maxVersion` (2)
2. System records `VERSION_MARKER` event with version=2
3. New logic (V2) executes

#### Replay/Resume (Old Workflow)
1. `getVersion()` finds `VERSION_MARKER` in history with version=1
2. Returns historical version (1)
3. Old logic (V1) executes

## 📋 Best Practices

### 1. **Use Descriptive Change IDs**
```java
// ❌ Bad
getVersion("change1", 1, 2)

// ✅ Good
getVersion("email-validation-regex-update", 1, 2)
```

### 2. **Always Clean Up**
```java
try {
    int version = WorkflowVersionManager.getVersion(...);
    // Your logic
} finally {
    WorkflowVersionManager.clear(); // IMPORTANT!
}
```

### 3. **Version Incrementally**
```java
// First change
int v1 = getVersion("validation-logic", 1, 2);

// Later, another change
int v2 = getVersion("notification-logic", 1, 2);

// Much later, third change
int v3 = getVersion("validation-logic", 1, 3); // Increment from 2 to 3
```

### 4. **Clean Up Old Code Paths**
After all old workflows complete (e.g., 30 days), you can remove V1:

```java
// Before cleanup
int version = getVersion("my-change", 1, 3);
if (version == 1) { /* old code */ }
else if (version == 2) { /* newer code */ }
else { /* latest code */ }

// After cleanup (set minVersion = 2)
int version = getVersion("my-change", 2, 3);
if (version == 2) { /* newer code */ }
else { /* latest code */ }
```

## 🔍 Real-World Example

### Scenario: Changing Phone Validation Logic

**Week 1:** Initial code
```java
public StepResult execute(UUID instanceId) {
    // Simple validation
    return validatePhone(phone);
}
```

**Week 2:** Need to add international format support
```java
public StepResult execute(UUID instanceId) {
    int version = WorkflowVersionManager.getVersion(
        "phone-validation-international", 1, 2
    );
    
    WorkflowVersionManager.recordVersionMarker(
        instanceId, "phone-validation-international", version, historyService
    );
    
    try {
        if (version == 1) {
            // Old users: domestic format only
            return validatePhoneDomestic(phone);
        } else {
            // New users: international format
            return validatePhoneInternational(phone);
        }
    } finally {
        WorkflowVersionManager.clear();
    }
}
```

**Result:**
- User A (started Week 1): Always uses domestic validation
- User B (started Week 2): Always uses international validation
- No errors, no data corruption

## 🚨 Common Pitfalls

### ❌ Don't: Change the changeId
```java
// Week 1
getVersion("validation", 1, 2)

// Week 2 - WRONG!
getVersion("new-validation", 1, 2) // Different ID = breaks replay
```

### ❌ Don't: Skip version numbers
```java
getVersion("my-change", 1, 2)
// Later...
getVersion("my-change", 1, 4) // Skipped 3 - confusing!
```

### ❌ Don't: Forget to record marker
```java
int version = getVersion(...);
// Missing: WorkflowVersionManager.recordVersionMarker(...)
// Result: Replay will fail!
```

## 📊 Monitoring

Check version distribution in your workflows:
```sql
SELECT 
    event_name,
    code_version,
    COUNT(*) as workflow_count
FROM workflow_event
WHERE event_type = 'VERSION_MARKER'
GROUP BY event_name, code_version
ORDER BY event_name, code_version;
```

## 🎓 Summary

| Aspect | Temporal | Our Implementation |
|--------|----------|-------------------|
| Version Tracking | ✅ `Workflow.getVersion()` | ✅ `WorkflowVersionManager.getVersion()` |
| Automatic Recording | ✅ Yes | ✅ Yes (via `recordVersionMarker`) |
| Replay Safety | ✅ Yes | ✅ Yes |
| Multiple Versions | ✅ Yes | ✅ Yes |

**You now have production-grade workflow versioning! 🎉**
