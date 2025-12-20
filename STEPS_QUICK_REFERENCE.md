# Quick Reference: 3 Types of Steps

## 🎯 At a Glance

| Type | Location | Format | Purpose | Mutable |
|------|----------|--------|---------|---------|
| **BLUEPRINT** | `flw_int_def.steps` | JSONB Array | Template/Config | ❌ Static |
| **CURRENT** | `flw_int.step_name` | VARCHAR | Where user is | ✅ Updates |
| **HISTORY** | `flow_case.audit_trail` | JSONB Array | What user did | ✅ Append-only |

---

## 📋 BLUEPRINT

**Table:** `flw_int_def`  
**Column:** `steps` (JSONB)  
**Purpose:** Định nghĩa TẤT CẢ các bước trong journey

```json
[
  {
    "name": "welcome",
    "type": "info",
    "title": "Chào mừng",
    "next": "personal-info",
    "uiSchema": {...}
  },
  {
    "name": "personal-info",
    "type": "form",
    "fields": [...],
    "next": "address-info",
    "onSubmit": [...]
  }
]
```

**Use when:**
- ✅ Validate user input
- ✅ Determine next step
- ✅ Get UI configuration
- ✅ Execute actions (start workflow, call service)

---

## 📍 CURRENT POSITION

**Table:** `flw_int`  
**Column:** `step_name` (VARCHAR)  
**Purpose:** User đang ở bước NÀO

```sql
SELECT step_name FROM flw_int WHERE id = 'int-001';
-- Result: "address-info"
```

**Use when:**
- ✅ Show current step to user
- ✅ Resume interrupted journey
- ✅ Track user progress
- ✅ Analytics (how many users at each step)

---

## 📸 HISTORY

**Table:** `flow_case`  
**Column:** `audit_trail` (JSONB)  
**Purpose:** Lịch sử các bước đã HOÀN THÀNH

```json
{
  "steps": [
    {
      "stepName": "welcome",
      "status": "COMPLETED",
      "completedAt": "2025-12-20T08:00:00Z",
      "data": {}
    },
    {
      "stepName": "personal-info",
      "status": "COMPLETED",
      "completedAt": "2025-12-20T08:05:00Z",
      "data": {
        "fullName": "Nguyen Van A",
        "dob": "1990-01-01"
      }
    }
  ]
}
```

**Use when:**
- ✅ User reviews submitted data
- ✅ Admin audits journey
- ✅ Compliance reporting
- ✅ Calculate time spent per step

---

## 🔄 Common Operations

### Get Current Step Info

```java
// Combines all 3 types
StepResponse response = stepNavigationService.getCurrentStep(interactionId);

// Returns:
// - stepName (from CURRENT)
// - stepDefinition (from BLUEPRINT)
// - stepData (from HISTORY if resuming)
// - progress info
```

### Submit Step

```java
// Updates CURRENT + HISTORY
StepResponse response = stepNavigationService.submitStep(
    interactionId,
    "personal-info",
    Map.of("fullName", "Nguyen Van A", "dob", "1990-01-01")
);

// What happens:
// 1. Validate against BLUEPRINT
// 2. Append to HISTORY
// 3. Update CURRENT to next step
// 4. Execute actions from BLUEPRINT
```

### Get Blueprint

```java
// Get all possible steps
List<StepDefinition> steps = stepNavigationService.getStepBlueprint(
    "kyc-onboarding",
    1L
);
```

### Get History

```java
// Get completed steps
List<StepHistoryEntry> history = stepNavigationService.getStepHistory(caseId);
```

---

## 🎨 Analogy

| Type | Real World |
|------|-----------|
| **BLUEPRINT** | 🗺️ GPS Route (all possible roads) |
| **CURRENT** | 📍 Your location NOW |
| **HISTORY** | 📸 Photos of places you've been |

---

## 🚀 API Endpoints

```bash
# Get current step
GET /api/interactions/{id}/current-step

# Submit step
POST /api/interactions/{id}/submit-step
{
  "stepName": "personal-info",
  "data": {...}
}

# Get blueprint
GET /api/interactions/definitions/{key}/steps?version=1

# Get history
GET /api/interactions/cases/{caseId}/step-history
```

---

## 💾 Database Schema

```sql
-- BLUEPRINT
flw_int_def (
    interaction_definition_key,
    interaction_definition_version,
    steps JSONB  ◄── All possible steps
)

-- CURRENT POSITION
flw_int (
    id,
    step_name VARCHAR,  ◄── Where user is now
    step_status,
    ...
)

-- HISTORY
flow_case (
    id UUID,
    audit_trail JSONB  ◄── Steps completed
    {
        "steps": [
            {stepName, completedAt, data},
            ...
        ]
    }
)
```

---

## ✅ Quick Checklist

When implementing a new journey:

- [ ] Define BLUEPRINT in `flw_int_def.steps`
- [ ] Create interaction instance with CURRENT = first step
- [ ] Initialize HISTORY as empty array
- [ ] On each submit:
  - [ ] Validate against BLUEPRINT
  - [ ] Append to HISTORY
  - [ ] Update CURRENT to next step
  - [ ] Execute BLUEPRINT actions

---

## 🔍 Common Queries

```sql
-- Get user's current step
SELECT step_name FROM flw_int WHERE id = 'int-001';

-- Get completed steps
SELECT audit_trail->'steps' FROM flow_case WHERE id = '...';

-- Get step config from blueprint
SELECT step_def 
FROM flw_int_def,
     jsonb_array_elements(steps) as step_def
WHERE step_def->>'name' = 'personal-info';

-- Analytics: Users per step
SELECT step_name, COUNT(*) 
FROM flw_int 
WHERE status = 'ACTIVE'
GROUP BY step_name;
```

---

## 📚 More Info

- Full guide: `UNDERSTANDING_STEPS.md`
- Diagrams: `STEPS_DIAGRAM.md`
- Sample data: `demo-steps-example.sql`
- Summary: `STEPS_SUMMARY.md`
