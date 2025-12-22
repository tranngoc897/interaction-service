# Relationship: flow_case và flw_int

## ✅ Kết luận: **1:N (One-to-Many)**

```
flow_case (1) ────────── (N) flw_int
    │                         │
    │                         │
    └─ id (UUID)              └─ case_id (VARCHAR/UUID)
```

**Một Case có thể có NHIỀU Interactions**

---

## 🎯 Tại sao 1:N?

### **Concept:**

- **Case** = **Dữ liệu business** của customer (KYC profile, loan application, account info...)
- **Interaction** = **Phiên làm việc/Journey** của user với hệ thống

**Một customer có thể có NHIỀU lần tương tác với CÙNG MỘT case!**

---

## 📊 Use Cases thực tế

### **Scenario 1: Multiple Journeys cho cùng Case**

```
┌─────────────────────────────────────────────────────────────┐
│  Case: Customer Profile (case-abc-123)                     │
│  Customer: Nguyễn Văn A                                    │
│  Data: {fullName, dob, address, idNumber, documents...}    │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ owns
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│Interaction 1 │    │Interaction 2 │    │Interaction 3 │
├──────────────┤    ├──────────────┤    ├──────────────┤
│ Journey:     │    │ Journey:     │    │ Journey:     │
│ KYC          │    │ Update       │    │ Add          │
│ Onboarding   │    │ Address      │    │ Document     │
│              │    │              │    │              │
│ Started:     │    │ Started:     │    │ Started:     │
│ 2025-12-01   │    │ 2025-12-10   │    │ 2025-12-15   │
│              │    │              │    │              │
│ Status:      │    │ Status:      │    │ Status:      │
│ COMPLETED    │    │ COMPLETED    │    │ IN_PROGRESS  │
└──────────────┘    └──────────────┘    └──────────────┘
```

**Timeline:**

```
Dec 1: User completes KYC onboarding
       → Interaction 1 created, linked to Case ABC-123
       → Case data populated with KYC info

Dec 10: User needs to update address
        → Interaction 2 created, SAME case ABC-123
        → Case data updated with new address

Dec 15: User uploads additional document
        → Interaction 3 created, SAME case ABC-123
        → Case data updated with new document
```

---

### **Scenario 2: Multi-Channel Interactions**

```
┌─────────────────────────────────────────────────────────────┐
│  Case: Loan Application (case-xyz-789)                     │
│  Customer: Trần Thị B                                      │
│  Data: {loanAmount, purpose, income, collateral...}        │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┬──────────────┐
        ▼                   ▼                   ▼              ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐  ┌──────────────┐
│ Web          │    │ Mobile App   │    │ Call Center  │  │ Web          │
│ Submit       │    │ Upload       │    │ Verify       │  │ Sign         │
│ Application  │    │ Documents    │    │ Info         │  │ Contract     │
│              │    │              │    │              │  │              │
│ int-001      │    │ int-002      │    │ int-003      │  │ int-004      │
│ COMPLETED    │    │ COMPLETED    │    │ COMPLETED    │  │ COMPLETED    │
└──────────────┘    └──────────────┘    └──────────────┘  └──────────────┘
```

---

### **Scenario 3: Resume/Retry**

```
┌─────────────────────────────────────────────────────────────┐
│  Case: Account Opening (case-def-456)                      │
│  Customer: Lê Văn C                                        │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│Interaction 1 │    │Interaction 2 │    │Interaction 3 │
│              │    │              │    │              │
│ Started but  │    │ Resumed and  │    │ Update info  │
│ abandoned    │    │ completed    │    │ after open   │
│              │    │              │    │              │
│ Status:      │    │ Status:      │    │ Status:      │
│ CANCELLED    │    │ COMPLETED    │    │ COMPLETED    │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## 📐 Database Schema

### **Tables:**

```sql
-- Parent: flow_case (1)
CREATE TABLE flow_case (
    id UUID PRIMARY KEY,                    -- ← Parent ID
    customer_id VARCHAR(64),
    case_definition_key VARCHAR,
    case_definition_version VARCHAR,
    status VARCHAR(32),
    case_data JSONB,                        -- ← Business data
    audit_trail JSONB,                      -- ← History
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

-- Child: flw_int (N)
CREATE TABLE flw_int (
    id VARCHAR(36) PRIMARY KEY,
    case_id VARCHAR(36),                    -- ← FK to flow_case.id
    user_id VARCHAR(36),
    interaction_definition_key VARCHAR(255),
    interaction_definition_version BIGINT,
    step_name VARCHAR(255),                 -- ← Current position
    step_status VARCHAR(20),
    status VARCHAR(20),
    resumable BOOLEAN,
    temp_data JSONB,
    
    -- Should add:
    CONSTRAINT fk_case 
        FOREIGN KEY (case_id) 
        REFERENCES flow_case(id)
);
```

---

## 🔍 Query Examples

### **Get all interactions for a case:**

```sql
SELECT 
    i.id as interaction_id,
    i.interaction_definition_key as journey_type,
    i.status,
    i.step_name as current_step,
    c.customer_id
FROM flw_int i
JOIN flow_case c ON c.id::text = i.case_id
WHERE c.id = 'case-abc-123'
ORDER BY i.id;
```

**Result:**
```
interaction_id | journey_type    | status    | current_step | customer_id
int-001        | kyc-onboarding  | COMPLETED | completed    | user-001
int-002        | update-address  | COMPLETED | completed    | user-001
int-003        | add-document    | ACTIVE    | upload-doc   | user-001
```

---

### **Get case with all its interactions:**

```sql
SELECT 
    c.id as case_id,
    c.customer_id,
    c.status as case_status,
    json_agg(
        json_build_object(
            'interactionId', i.id,
            'journey', i.interaction_definition_key,
            'status', i.status,
            'currentStep', i.step_name
        )
    ) as interactions
FROM flow_case c
LEFT JOIN flw_int i ON i.case_id = c.id::text
WHERE c.customer_id = 'user-001'
GROUP BY c.id, c.customer_id, c.status;
```

---

### **Analytics: How many interactions per case?**

```sql
SELECT 
    c.id as case_id,
    c.customer_id,
    COUNT(i.id) as interaction_count,
    array_agg(i.interaction_definition_key) as journey_types
FROM flow_case c
LEFT JOIN flw_int i ON i.case_id = c.id::text
GROUP BY c.id, c.customer_id
HAVING COUNT(i.id) > 1  -- Cases with multiple interactions
ORDER BY interaction_count DESC;
```

---

## 💻 Java Code Examples

### **Get all interactions for a case:**

```java
@Repository
public interface InteractionRepository extends JpaRepository<InteractionEntity, String> {
    
    // Find all interactions for a case
    List<InteractionEntity> findByCaseId(String caseId);
    
    // Find active interactions for a case
    List<InteractionEntity> findByCaseIdAndStatus(String caseId, String status);
    
    // Count interactions for a case
    long countByCaseId(String caseId);
}
```

### **Usage:**

```java
@Service
public class CaseInteractionService {
    
    @Autowired
    private InteractionRepository interactionRepo;
    
    @Autowired
    private CaseRepository caseRepo;
    
    public CaseWithInteractionsDto getCaseWithInteractions(UUID caseId) {
        // Get case
        CaseEntity caseEntity = caseRepo.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));
        
        // Get all interactions for this case
        List<InteractionEntity> interactions = interactionRepo.findByCaseId(caseId.toString());
        
        return new CaseWithInteractionsDto(
                caseEntity,
                interactions
        );
    }
    
    public InteractionSummary getInteractionSummary(UUID caseId) {
        List<InteractionEntity> interactions = interactionRepo.findByCaseId(caseId.toString());
        
        long completed = interactions.stream()
                .filter(i -> "COMPLETED".equals(i.getStatus()))
                .count();
        
        long active = interactions.stream()
                .filter(i -> "ACTIVE".equals(i.getStatus()))
                .count();
        
        return new InteractionSummary(
                interactions.size(),
                completed,
                active
        );
    }
}
```

---

## 🎨 Visual Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    CASE (Business Data)                     │
│  - Customer profile                                         │
│  - KYC information                                          │
│  - Documents                                                │
│  - Audit trail                                              │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ 1:N
                            │
        ┌───────────────────┼───────────────────┬──────────────┐
        │                   │                   │              │
        ▼                   ▼                   ▼              ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐  ┌──────────────┐
│ INTERACTION  │    │ INTERACTION  │    │ INTERACTION  │  │ INTERACTION  │
│              │    │              │    │              │  │              │
│ Journey 1:   │    │ Journey 2:   │    │ Journey 3:   │  │ Journey 4:   │
│ Onboarding   │    │ Update       │    │ Add Doc      │  │ Verify       │
│              │    │              │    │              │  │              │
│ - Session    │    │ - Session    │    │ - Session    │  │ - Session    │
│ - UI State   │    │ - UI State   │    │ - UI State   │  │ - UI State   │
│ - Progress   │    │ - Progress   │    │ - Progress   │  │ - Progress   │
└──────────────┘    └──────────────┘    └──────────────┘  └──────────────┘
```

---

## 🔑 Key Points

### **Case (1)**
- ✅ **Long-lived** - Tồn tại lâu dài
- ✅ **Business data** - Chứa dữ liệu thực tế của customer
- ✅ **Single source of truth** - Nguồn dữ liệu chính
- ✅ **Persistent** - Lưu vĩnh viễn

### **Interaction (N)**
- ✅ **Session-based** - Theo phiên làm việc
- ✅ **UI/UX state** - Quản lý trạng thái journey
- ✅ **Multiple per case** - Nhiều interactions cho 1 case
- ✅ **Can be completed/cancelled** - Có thể kết thúc hoặc hủy

---

## ✅ Best Practices

### **1. Always link Interaction to Case:**

```java
// When creating new interaction
InteractionEntity interaction = new InteractionEntity();
interaction.setId(UUID.randomUUID().toString());
interaction.setCaseId(caseId.toString());  // ← Link to case
interaction.setUserId(userId);
interaction.setStatus("ACTIVE");
```

### **2. Query efficiently:**

```java
// Good: Get all interactions for a case
List<InteractionEntity> interactions = interactionRepo.findByCaseId(caseId);

// Bad: Loop through all interactions
// Don't do this!
```

### **3. Handle orphaned interactions:**

```sql
-- Find interactions without a case (data integrity issue)
SELECT i.* 
FROM flw_int i
LEFT JOIN flow_case c ON c.id::text = i.case_id
WHERE c.id IS NULL;
```

### **4. Add Foreign Key constraint:**

```sql
ALTER TABLE flw_int 
ADD CONSTRAINT fk_interaction_case 
FOREIGN KEY (case_id) 
REFERENCES flow_case(id) 
ON DELETE CASCADE;  -- Or ON DELETE RESTRICT
```

---

## 📊 Summary

| Aspect | Case | Interaction |
|--------|------|-------------|
| **Cardinality** | 1 | N (Many) |
| **Purpose** | Business data | UI/Journey session |
| **Lifetime** | Long-lived | Session-based |
| **Data** | Persistent | Temporary + state |
| **Relationship** | Parent | Child |

**Analogy:**
- **Case** = Hồ sơ bệnh án của bệnh nhân (lưu vĩnh viễn)
- **Interaction** = Các lần khám bệnh (nhiều lần, mỗi lần một mục đích khác nhau)

---

Bạn đã hiểu rõ về relationship 1:N này chưa? 🎯
