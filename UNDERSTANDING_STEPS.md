# Understanding "Steps" in Interaction Service

## 📚 Overview

Trong Interaction Service, khái niệm **"steps"** xuất hiện ở **3 nơi khác nhau**, mỗi nơi phục vụ một mục đích riêng:

```
┌─────────────────────────────────────────────────────────────┐
│  1. flw_int_def.steps (JSONB)                               │
│     = BLUEPRINT: Định nghĩa CÁC BƯỚC trong journey          │
│     = "Kịch bản" cho toàn bộ flow                           │
└─────────────────────────────────────────────────────────────┘
                            ↓ defines
┌─────────────────────────────────────────────────────────────┐
│  2. flw_int.step_name + step_status                         │
│     = CURRENT POSITION: User đang ở bước NÀO                │
│     = "Con trỏ" chỉ vào 1 step trong blueprint              │
└─────────────────────────────────────────────────────────────┘
                            ↓ saves to
┌─────────────────────────────────────────────────────────────┐
│  3. flow_case.audit_trail (JSONB)                           │
│     = HISTORY: Lịch sử các bước đã HOÀN THÀNH               │
│     = "Audit trail" của journey                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 1️⃣ Blueprint: `flw_int_def.steps`

### Mục đích
Định nghĩa **TẤT CẢ các bước** có thể có trong một journey (template/kịch bản).

### Đặc điểm
- ✅ **Static** - Không thay đổi theo từng user
- 📐 **Template** - Áp dụng cho TẤT CẢ users
- 🎨 **UI Config** - Chứa thông tin về UI/UX
- 🔀 **Routing** - Định nghĩa step nào đi tiếp theo step nào
- ⚙️ **Actions** - Định nghĩa hành động khi submit (call service, start workflow...)

### Ví dụ

```json
// Table: flw_int_def
// Column: steps (JSONB)
[
  {
    "name": "welcome",
    "type": "info",
    "title": "Chào mừng",
    "next": "personal-info",
    "uiSchema": {
      "component": "WelcomeScreen"
    }
  },
  {
    "name": "personal-info",
    "type": "form",
    "title": "Thông tin cá nhân",
    "fields": [
      {"name": "fullName", "type": "text", "required": true},
      {"name": "dob", "type": "date", "required": true}
    ],
    "next": "address-info",
    "onSubmit": [
      {"action": "validateWithService", "service": "id-verification"}
    ]
  },
  {
    "name": "address-info",
    "type": "form",
    "title": "Địa chỉ",
    "fields": [...],
    "next": "completed"
  }
]
```

### Use Cases
- 🎨 Frontend cần biết step này hiển thị UI gì
- ✅ Backend validate data theo fields definition
- 🔀 Xác định next step sau khi submit
- ⚙️ Thực thi actions (start workflow, call service...)

---

## 2️⃣ Current Position: `flw_int.step_name`

### Mục đích
Chỉ ra user **ĐANG Ở** bước nào trong journey.

### Đặc điểm
- ✅ **Dynamic** - Thay đổi khi user submit step
- 📍 **Pointer** - Trỏ vào 1 step trong blueprint
- 🏃 **Runtime** - Theo dõi tiến trình của từng user
- 🔄 **Mutable** - Update liên tục

### Ví dụ

```sql
-- Table: flw_int
SELECT 
    id,
    user_id,
    step_name,        -- ← CURRENT POSITION
    step_status,
    status
FROM flw_int;

-- Results:
-- id       | user_id  | step_name      | step_status | status
-- int-001  | user-001 | welcome        | PENDING     | ACTIVE
-- int-002  | user-002 | address-info   | PENDING     | ACTIVE
-- int-003  | user-003 | waiting-approval| COMPLETED  | WAITING_SYSTEM
```

### Use Cases
- 📍 Biết user đang ở đâu trong journey
- 🔄 Resume journey từ step đã dừng
- 📊 Analytics: Bao nhiêu user đang ở step nào

---

## 3️⃣ History: `flow_case.audit_trail`

### Mục đích
Lưu lại **LỊCH SỬ** các bước đã hoàn thành + dữ liệu đã submit.

### Đặc điểm
- ✅ **Append-only** - Chỉ thêm, không xóa
- 📜 **Audit trail** - Theo dõi toàn bộ hành trình
- 💾 **Data snapshot** - Lưu dữ liệu đã submit ở mỗi step
- ⏰ **Timestamps** - Biết user submit lúc nào
- 🔍 **Compliance** - Phục vụ audit, compliance

### Ví dụ

```json
// Table: flow_case
// Column: audit_trail (JSONB)
{
  "steps": [
    {
      "stepName": "welcome",
      "status": "COMPLETED",
      "startedAt": "2025-12-20T08:00:00Z",
      "completedAt": "2025-12-20T08:00:30Z",
      "data": {},
      "metadata": {
        "userAgent": "Mozilla/5.0",
        "ipAddress": "192.168.1.100"
      }
    },
    {
      "stepName": "personal-info",
      "status": "COMPLETED",
      "startedAt": "2025-12-20T08:00:30Z",
      "completedAt": "2025-12-20T08:05:00Z",
      "data": {
        "fullName": "Nguyen Van A",
        "dob": "1990-01-01",
        "idNumber": "123456789"
      },
      "metadata": {
        "userAgent": "Mozilla/5.0",
        "ipAddress": "192.168.1.100"
      }
    }
  ],
  "lastUpdated": "2025-12-20T08:05:00Z"
}
```

### Use Cases
- 📝 User xem lại thông tin đã điền
- 🔍 Admin audit journey của user
- 📊 Compliance reporting
- ⏱️ Phân tích thời gian hoàn thành mỗi step

---

## 🔄 Workflow: 3 loại Steps hoạt động cùng nhau

### Scenario: User làm KYC Onboarding

#### **Step 1: User bắt đầu journey**

```http
POST /api/interactions/start
{
  "interactionDefinitionKey": "kyc-onboarding",
  "userId": "user-456"
}
```

**Backend xử lý:**
1. Load **BLUEPRINT** từ `flw_int_def.steps`
2. Set **CURRENT POSITION** = first step (`welcome`)
3. Initialize **HISTORY** = empty array

**State:**
```
BLUEPRINT:        [welcome, personal-info, address-info, ...]
CURRENT POSITION: "welcome"
HISTORY:          []
```

---

#### **Step 2: User submit "welcome" step**

```http
POST /api/interactions/int-abc-123/submit-step
{
  "stepName": "welcome",
  "data": {}
}
```

**Backend xử lý:**
1. Find current step in **BLUEPRINT**
2. Get `next` from **BLUEPRINT** → `"personal-info"`
3. Add entry to **HISTORY**
4. Update **CURRENT POSITION** = `"personal-info"`

**State:**
```
BLUEPRINT:        [welcome, personal-info, address-info, ...]  (unchanged)
CURRENT POSITION: "personal-info"                              (updated)
HISTORY:          [{stepName: "welcome", completedAt: "..."}]  (appended)
```

---

#### **Step 3: User submit "personal-info" step**

```http
POST /api/interactions/int-abc-123/submit-step
{
  "stepName": "personal-info",
  "data": {
    "fullName": "Nguyen Van A",
    "dob": "1990-01-01"
  }
}
```

**Backend xử lý:**
1. Validate data theo **BLUEPRINT** fields
2. Execute `onSubmit` actions từ **BLUEPRINT**
3. Add entry to **HISTORY** (with data)
4. Update **CURRENT POSITION** = `"address-info"`

**State:**
```
BLUEPRINT:        [welcome, personal-info, address-info, ...]
CURRENT POSITION: "address-info"
HISTORY:          [
                    {stepName: "welcome", ...},
                    {stepName: "personal-info", data: {...}}
                  ]
```

---

## 📊 So sánh 3 loại Steps

| Aspect | BLUEPRINT | CURRENT POSITION | HISTORY |
|--------|-----------|------------------|---------|
| **Location** | `flw_int_def.steps` | `flw_int.step_name` | `flow_case.audit_trail` |
| **Type** | JSONB Array | VARCHAR | JSONB Array |
| **Scope** | Per definition | Per interaction | Per case |
| **Mutable** | ❌ Static (per version) | ✅ Updates frequently | ✅ Append-only |
| **Contains** | All possible steps | 1 step name | Completed steps |
| **Has data?** | ❌ Only config | ❌ Only name | ✅ Submitted data |
| **Purpose** | Template/Config | Current state | Audit trail |

---

## 🎯 API Examples

### Get Current Step (combines all 3)

```http
GET /api/interactions/int-abc-123/current-step

Response:
{
  "interactionId": "int-abc-123",
  "stepName": "address-info",           ← CURRENT POSITION
  "stepStatus": "PENDING",
  "stepDefinition": {                   ← From BLUEPRINT
    "name": "address-info",
    "type": "form",
    "title": "Địa chỉ",
    "fields": [...]
  },
  "stepData": {...},                    ← From HISTORY (if resuming)
  "progress": {
    "currentStepIndex": 3,
    "totalSteps": 7,
    "percentComplete": 42
  }
}
```

### Get Step Blueprint

```http
GET /api/interactions/definitions/kyc-onboarding/steps?version=1

Response:
[
  {"name": "welcome", "type": "info", ...},
  {"name": "personal-info", "type": "form", ...},
  {"name": "address-info", "type": "form", ...},
  ...
]
```

### Get Step History

```http
GET /api/interactions/cases/case-xyz-789/step-history

Response:
[
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
```

---

## 🎨 Analogy

Tưởng tượng bạn đang đi du lịch:

| Concept | Analogy |
|---------|---------|
| **BLUEPRINT** | 📋 **Lịch trình tour** - Danh sách tất cả điểm đến (Hà Nội → Hạ Long → Sapa) |
| **CURRENT POSITION** | 📍 **Vị trí hiện tại** - Bạn đang ở Hạ Long |
| **HISTORY** | 📸 **Album ảnh** - Ảnh chụp ở Hà Nội (đã đi qua) + Hạ Long (đang ở) |

---

## 🚀 Demo

Xem file `demo-steps-example.sql` để có sample data và query examples.

### Run Demo

```bash
# 1. Load sample data
psql -U postgres -d your_database -f src/main/resources/db/demo-steps-example.sql

# 2. Start application
./mvnw spring-boot:run

# 3. Test APIs
curl http://localhost:8080/api/interactions/int-001/current-step
curl http://localhost:8080/api/interactions/definitions/kyc-onboarding/steps
curl http://localhost:8080/api/interactions/cases/c2222222-2222-2222-2222-222222222222/step-history
```

---

## 📝 Key Takeaways

1. **BLUEPRINT** (`flw_int_def.steps`) = Template cho journey
   - Định nghĩa tất cả steps, UI, validation, routing
   - Static, dùng chung cho tất cả users

2. **CURRENT POSITION** (`flw_int.step_name`) = User đang ở đâu
   - Track real-time position trong journey
   - Update mỗi khi user submit step

3. **HISTORY** (`flow_case.audit_trail`) = Lịch sử đã làm gì
   - Audit trail với timestamps và data
   - Append-only, không xóa

4. **Chúng hoạt động cùng nhau:**
   - BLUEPRINT cung cấp config
   - CURRENT POSITION track tiến trình
   - HISTORY lưu lại hành trình

---

## 📚 Related Files

- DTOs: `src/main/java/com/ngoctran/interactionservice/interaction/dto/`
- Service: `StepNavigationService.java`
- Controller: `StepController.java`
- Demo SQL: `src/main/resources/db/demo-steps-example.sql`
