# Camunda 7 Local Docker Setup
Truy cập Camunda Web Apps

- **Camunda Tasklist**: http://localhost:8080/camunda/app/tasklist/
- **Camunda Cockpit**: http://localhost:8080/camunda/app/cockpit/
- **Camunda Admin**: http://localhost:8080/camunda/app/admin/

**Default credentials:**
- Username: `demo`
- Password: `demo`

## 🏗️ Cấu trúc Docker Setup

```
camunda-docker-compose.yml
├── camunda-db (PostgreSQL 13)
│   ├── Database: camunda
│   ├── User: camunda
│   ├── Password: camunda
│   └── Port: 5432
└── camunda (Camunda BPM Platform 7.21.0)
    ├── Port: 8080
    ├── BPMN processes: ./bpmn-processes/
    └── Database: camunda-db
```

## 📁 BPMN Processes

BPMN processes được mount vào container tại `/camunda/conf/bpmn/`
### Sample Process: `onboarding-process.bpmn`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions ...>
  <bpmn:process id="onboarding-process" name="Customer Onboarding Process" isExecutable="true">
    <!-- Process definition -->
  </bpmn:process>
</bpmn:definitions>
```

### Quy trình Onboarding:
1. **Collect Personal Information** (User Task)
2. **Upload Documents** (User Task)
3. **AML/KYC Compliance Check** (Service Task)
4. **Manual Review** (nếu cần - User Task)
5. **Create Account** (Service Task)
6. **Send Welcome Email** (Service Task)

## 🔧 Sử dụng với Spring Boot App

### 1. Cấu hình Database
Trong `application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/camunda
    username: camunda
    password: camunda
    driver-class-name: org.postgresql.Driver

camunda:
  bpm:
    database:
      type: postgres
      schema-update: true
```
### 2. Deploy BPMN Process qua API
```bash
# Deploy onboarding process
curl -X POST "http://localhost:8081/api/bpmn/deploy?processKey=onboarding&processName=Onboarding" \
  -H "Content-Type: application/xml" \
  --data-binary @bpmn-processes/onboarding-process.bpmn
```
### 3. Start Process Instance

```bash
# Start onboarding process
curl -X POST "http://localhost:8081/api/bpmn/start?processDefinitionKey=onboarding&businessKey=customer-123" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123",
    "customerType": "INDIVIDUAL",
    "productType": "SAVINGS_ACCOUNT"
  }'
```
### 4. Signal Process

```bash
# Signal document upload completion
curl -X POST "http://localhost:8081/api/bpmn/signal/9dcc031d-e861-11f0-bd72-7aad302a1eea?signalName=documentsUploaded" \
  -H "Content-Type: application/json" \
  -d '{
    "documentCount": 3,
    "verified": true
  }'
```

## 📊 Database Schema

Camunda tự động tạo các bảng:

- `ACT_HI_*` - History tables
- `ACT_RU_*` - Runtime tables
- `ACT_RE_*` - Repository tables
- `ACT_ID_*` - Identity tables

## 🎯 Next Steps

1. **Start Camunda**: `docker-compose -f camunda-docker-compose.yml up -d`
2. **Access Web UI**: http://localhost:8080/camunda
3. **Deploy Process**: Sử dụng REST API hoặc Spring Boot integration
4. **Monitor Processes**: Sử dụng Cockpit và Tasklist
5. **Integrate with Cases**: Sử dụng CaseService BPMN methods




## 📋 __CHI TIẾT VAI TRÒ CaseDefinitionEntity__

`CaseDefinitionEntity` là __template/mẫu__ định nghĩa cấu trúc và quy trình xử lý cho các case onboarding. Đây là "blueprint" cho việc tạo case instances.

---

## 🏗️ __Core Purpose: Template Management__

### __1. Case Template Definition__

```java
@Entity
@Table(name = "case_definitions")
public class CaseDefinitionEntity {
    @Id
    private String key;                    // Unique template identifier
    private String name;                   // Human-readable name
    private String description;            // Detailed description
    private String version;                // Version control
    private String category;               // Business category
    private boolean active = true;         // Enable/disable template
```

### __2. Workflow Configuration__

```java
private String workflowDefinitionKey;     // BPMN process key
private String dmnDefinitionKey;          // Decision table key
private Map<String, Object> defaultVariables; // Default process variables
private List<String> requiredFields;      // Mandatory fields
private Map<String, ValidationRule> validationRules; // Field validations
```

### __3. SLA & Timeline Configuration__

```java
private Duration totalSla;                // Overall SLA
private Map<String, Duration> stepSlas;   // Per-step SLAs
private List<String> escalationPoints;    // Escalation triggers
private Map<String, String> notifications; // Notification templates
```

---

## 🎯 __Key Responsibilities__

### __1. Case Structure Definition__

- __Fields__: Define required/optional fields cho case
- __Validation__: Business rules và constraints
- __Workflow__: BPMN process template
- __SLA__: Time-based requirements

### __2. Case Creation Template__

```java
// When creating a case:
CaseEntity caseEntity = new CaseEntity();
caseEntity.setCaseDefinitionId(caseDefinition.getKey());

// Apply template defaults:
caseEntity.setDefaultVariables(caseDefinition.getDefaultVariables());
caseEntity.setRequiredFields(caseDefinition.getRequiredFields());
caseEntity.setSla(caseDefinition.getTotalSla());
```

### __3. Workflow Orchestration__

- __Process Selection__: Chọn BPMN process phù hợp
- __Variable Mapping__: Map case data → process variables
- __Step Configuration__: Define workflow steps
- __Integration Points__: External service configurations

### __4. Business Rule Engine__

- __Decision Tables__: DMN definitions cho business logic
- __Validation Rules__: Field-level và cross-field validations
- __Escalation Rules__: SLA breach handling
- __Approval Workflows__: Manual review requirements

---

## 🔗 __Relationship với CaseEntity__

### __One-to-Many Relationship__

```javascript
CaseDefinitionEntity (1) ────→ CaseEntity (Many)
    │                           │
    ├── Template                ├── Instance
    ├── Blueprint               ├── Runtime data
    ├── Definition              ├── Execution state
    └── Configuration           └── Business data
```

### __Template Application__

```java
// Case creation process:
1. Select CaseDefinitionEntity
2. Create CaseEntity instance
3. Apply template configurations
4. Initialize workflow
5. Set SLA timers
6. Configure validations
```

---

## 📊 __Detailed Field Explanations__

### __Core Identification__

```java
private String key;              // "SAVINGS_ACCOUNT_ONBOARDING"
private String name;             // "Savings Account Customer Onboarding"
private String description;      // "Complete onboarding process for savings accounts"
private String version;          // "1.2.0"
private String category;         // "RETAIL_BANKING"
```

### __Workflow Integration__

```java
private String workflowDefinitionKey;    // "onboarding-process"
private String dmnDefinitionKey;         // "onboarding-decisions"
private Map<String, Object> defaultVariables; // {"priority": "NORMAL"}
```

### __Business Configuration__

```java
private List<String> requiredFields;      // ["customerId", "accountType"]
private Map<String, ValidationRule> validationRules;
private Map<String, Object> metadata;     // Template-specific config
```

### __SLA & Quality Management__

```java
private Duration totalSla;                // PT24H (24 hours)
private Map<String, Duration> stepSlas;   // {"data_validation": PT30M}
private List<String> escalationPoints;    // ["manager", "compliance_officer"]
```

### __Integration & Notifications__

```java
private Map<String, String> notifications; // Email/SMS templates
private List<String> integrations;         // External services
private Map<String, Object> apiConfig;     // API configurations
```

---

## 🎯 __Business Value & Use Cases__

### __1. Product-Specific Templates__

```java
// Different templates for different products:
- "SAVINGS_ACCOUNT_ONBOARDING"
- "CURRENT_ACCOUNT_ONBOARDING" 
- "CREDIT_CARD_APPLICATION"
- "LOAN_APPLICATION"
- "JOINT_ACCOUNT_ONBOARDING"
```

### __2. Customer Segment Templates__

```java
// Different complexity levels:
- "SIMPLE_ONBOARDING" (Basic customers)
- "STANDARD_ONBOARDING" (Regular customers)
- "PREMIUM_ONBOARDING" (VIP customers)
- "COMPLEX_ONBOARDING" (High-risk/complex cases)
```

### __3. Regulatory Templates__

```java
// Compliance-based variations:
- "STANDARD_KYC" (Basic KYC)
- "ENHANCED_DUE_DILIGENCE" (EDD required)
- "PEP_SCREENING" (Politically exposed persons)
- "SANCTIONS_CHECKING" (OFAC/UN sanctions)
```

---

## 🔄 __Lifecycle Management__

### __1. Template Creation__

```java
CaseDefinitionEntity template = new CaseDefinitionEntity();
template.setKey("SAVINGS_ACCOUNT_ONBOARDING");
template.setName("Savings Account Onboarding");
template.setWorkflowDefinitionKey("onboarding-process");
template.setTotalSla(Duration.ofHours(24));
// ... configure other properties
caseDefinitionRepository.save(template);
```

### __2. Template Updates__

```java
// Version control for changes:
template.setVersion("1.3.0");
template.setLastModified(Instant.now());
template.setChangeLog("Added biometric verification step");
// Save new version
```

### __3. Template Activation/Deactivation__

```java
// Deactivate old template:
oldTemplate.setActive(false);
oldTemplate.setDeactivatedAt(Instant.now());

// Activate new template:
newTemplate.setActive(true);
newTemplate.setActivatedAt(Instant.now());
```

---

## 🔧 __Integration với BPMN Workflow__

### __Process Definition Mapping__

```xml
<!-- BPMN Process linked to Case Definition -->
<process id="onboarding-process" name="Customer Onboarding" isExecutable="true">
  <!-- Process variables from CaseDefinitionEntity -->
  <property name="caseDefinitionKey" value="${caseDefinition.key}" />
  <property name="totalSla" value="${caseDefinition.totalSla}" />
  
  <!-- Service tasks configured by template -->
  <serviceTask id="dataValidation" name="Validate Data">
    <extensionElements>
      <flowable:class>${caseDefinition.validationDelegate}</flowable:class>
    </extensionElements>
  </serviceTask>
</process>
```

### __Dynamic Workflow Generation__

```java
// Template drives workflow creation:
String processKey = caseDefinition.getWorkflowDefinitionKey();
Map<String, Object> variables = caseDefinition.getDefaultVariables();
// Start process with template configuration
ProcessInstance instance = runtimeService.startProcessInstanceByKey(processKey, variables);
```

---

## 📊 __Query & Management__

### __Template Discovery__

```java
// Find templates by category:
List<CaseDefinitionEntity> retailTemplates = repository.findByCategory("RETAIL_BANKING");

// Find active templates:
List<CaseDefinitionEntity> activeTemplates = repository.findByActiveTrue();

// Find templates by product type:
CaseDefinitionEntity savingsTemplate = repository.findByKey("SAVINGS_ACCOUNT_ONBOARDING");
```

### __Template Analytics__

```java
// Usage statistics:
long savingsCases = caseRepository.countByCaseDefinitionId("SAVINGS_ACCOUNT_ONBOARDING");

// Performance metrics:
Duration avgCompletionTime = analyticsService.getAvgCompletionTime("SAVINGS_ACCOUNT_ONBOARDING");

// SLA compliance:
double slaComplianceRate = analyticsService.getSlaComplianceRate("SAVINGS_ACCOUNT_ONBOARDING");
```

---

## 🛡️ __Compliance & Governance__

### __Audit Trail__

```java
// Template changes are audited:
template.setAuditTrail(List.of(
    new AuditEntry("CREATED", "admin", Instant.now()),
    new AuditEntry("UPDATED", "manager", Instant.now())
));
```

### __Version Control__

```java
// Template versioning:
template.setVersion("2.0.0");
template.setPreviousVersion("1.5.0");
template.setMigrationNotes("Added enhanced KYC requirements");
```

### __Approval Workflow__

```java
// Template changes require approval:
template.setStatus("PENDING_APPROVAL");
template.setRequestedBy("business_analyst");
template.setApprovedBy("compliance_officer");
// Only activate after approval
```

---

## 🎯 __Advanced Features__

### __1. Conditional Logic__

```java
// Template can define conditional workflows:
Map<String, String> conditions = Map.of(
    "highValue", "amount > 100000",
    "vipCustomer", "segment == 'PREMIUM'",
    "international", "country != 'US'"
);
// Apply different SLAs/workflows based on conditions
```

### __2. Dynamic Configuration__

```java
// Runtime configuration from external sources:
template.setApiConfig(loadConfigFromExternalService());
template.setNotificationTemplates(loadTemplatesFromCMS());
template.setValidationRules(loadRulesFromDatabase());
```

### __3. Multi-Tenant Support__

```java
// Different configurations per tenant:
template.setTenantId("BANK_A");
template.setRegionalConfig(getRegionalSettings("US"));
template.setComplianceRules(getLocalComplianceRules("US"));
```

---

## 📈 __Business Impact__

### __Operational Efficiency__

- ✅ __Standardization__: Consistent case handling
- ✅ __Automation__: Reduced manual configuration
- ✅ __Scalability__: Easy template replication
- ✅ __Quality__: Built-in validations và SLAs

### __Business Agility__

- ✅ __Rapid Deployment__: New product onboarding
- ✅ __Market Responsiveness__: Quick template updates
- ✅ __Regulatory Adaptation__: Compliance rule updates
- ✅ __Customer Experience__: Tailored workflows

### __Risk Management__

- ✅ __Compliance__: Built-in regulatory requirements
- ✅ __Auditability__: Template change tracking
- ✅ __Error Prevention__: Validation rules
- ✅ __SLA Enforcement__: Automatic monitoring

---

## 🔄 __Evolution & Migration__

### __Template Migration__

```java
// Migrate cases to new template version:
migrationService.migrateCases(
    "SAVINGS_ACCOUNT_ONBOARDING_V1",
    "SAVINGS_ACCOUNT_ONBOARDING_V2",
    migrationRules
);
```

### __Backward Compatibility__

```java
// Support old template versions:
if (caseDefinition.isDeprecated()) {
    log.warn("Using deprecated template: {}", caseDefinition.getKey());
    // Apply migration or compatibility layer
}
```
__CaseDefinitionEntity là:__

- 🏗️ __Template Engine__ cho case creation
- 🔄 __Workflow Orchestrator__ linking business rules với BPMN
- 📊 __Configuration Manager__ cho SLA, validation, integration
- 🛡️ __Compliance Framework__ với audit trails và governance
- 🚀 __Business Agility Driver__ enabling rapid product deployment

__Đây là backbone của scalable, configurable case management system!__ 🚀



Chi tiết JSON Fields trong CaseEntity__

Tôi đã giải thích chi tiết __7 JSON fields__ trong CaseEntity:

---

## 📋 __TÓM TẮT CÁC JSON FIELDS:__

### __1. `caseData`__ - Core Business Data

- Lưu trữ toàn bộ thông tin business của case
- Flexible schema, không cần migrate DB khi thêm fields
- JSONB indexing cho performance cao

### __2. `auditTrail`__ - Audit History

- Complete audit trail cho compliance
- Track mọi thay đổi với timestamp và actor
- Regulatory requirements cho financial services

### __3. `sla`__ - SLA Tracking

- Monitor Service Level Agreements
- Real-time deadline tracking và alerting
- Performance metrics và SLA compliance

### __4. `workflowState`__ - Workflow State

- Current execution state của BPMN process
- Resume capability sau interruptions
- Debug và monitoring workflow execution

### __5. `epicData`__ - Epic/Saga Data

- Complex multi-step process orchestration
- Saga pattern cho distributed transactions
- Compensation logic cho failure recovery

### __6. `complianceStatus`__ - Compliance Info

- AML/KYC/Fraud/Sanctions status tracking
- Regulatory compliance monitoring
- Risk assessment và decision making

### __7. `jointAccountData`__ - Joint Account Details

- Multi-party account management
- Relationship và ownership tracking
- Complex account structure handling

---

## 🎯 __KEY BENEFITS:__

### __Flexibility:__

- ✅ __Schema-less design__ - Add fields without DB migrations
- ✅ __Business agility__ - Adapt to changing requirements quickly
- ✅ __Version compatibility__ - Backward/forward compatibility

### __Performance:__

- ✅ __JSONB indexing__ - Fast queries trên PostgreSQL
- ✅ __Efficient storage__ - No wasted space cho null values
- ✅ __Scalable__ - Handle complex nested data structures

### __Compliance:__

- ✅ __Complete audit trails__ - Regulatory requirements
- ✅ __Data retention__ - Structured compliance data
- ✅ __Forensic analysis__ - Track what/when/who

### __Business Value:__

- ✅ __Rich analytics__ - Complex reporting capabilities
- ✅ __Process optimization__ - SLA và performance monitoring
- ✅ __Risk management__ - Comprehensive compliance tracking

---

## 🔍 __USAGE PATTERNS:__

### __Query Examples:__

```sql
-- High-risk AML cases
SELECT * FROM flw_case 
WHERE compliance_status->'aml'->>'riskLevel' = 'HIGH';

-- SLA breaches
SELECT * FROM flw_case
WHERE (sla->'overall'->>'breached')::boolean = true;

-- Pending documents
SELECT * FROM flw_case
WHERE jsonb_exists(case_data->'documents', 'PENDING');
```

### __Business Logic:__

```java
// Check compliance status
String amlStatus = complianceStatus.get("aml").get("status");

// Calculate SLA remaining time  
long remainingMs = sla.get("overall").get("remainingHours") * 3600000;

// Access nested case data
String accountType = caseData.get("productSelection").get("accountType");
```

---

## 🚀 __IMPLEMENTATION COMPLETE:__

__CaseEntity với JSON fields tạo nên một:__

- __Flexible data model__ cho complex business workflows
- __Regulatory compliant__ audit và compliance tracking
- __High-performance__ JSONB storage và querying
- __Scalable architecture__ cho enterprise onboarding systems

__Đây là cách hiện đại để handle complex, evolving business data trong financial services!__
