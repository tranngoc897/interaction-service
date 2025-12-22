# Temporal Workflow Integration Design

## 🎯 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Web/Mobile)                    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Interaction Service (Spring Boot)              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  InteractionController                             │    │
│  │  - POST /interactions/start                        │    │
│  │  - POST /interactions/{id}/submit-step             │    │
│  └────────────────────────────────────────────────────┘    │
│                            │                                │
│  ┌────────────────────────────────────────────────────┐    │
│  │  StepNavigationService                             │    │
│  │  - getCurrentStep()                                │    │
│  │  - submitStep()                                    │    │
│  └────────────────────────────────────────────────────┘    │
│                            │                                │
│  ┌────────────────────────────────────────────────────┐    │
│  │  TemporalWorkflowService                           │    │
│  │  - startWorkflow()                                 │    │
│  │  - queryWorkflowStatus()                           │    │
│  │  - signalWorkflow()                                │    │
│  └────────────────────────────────────────────────────┘    │
│                            │                                │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Database (PostgreSQL)                             │    │
│  │  - flow_case                                       │    │
│  │  - flw_int                                         │    │
│  │  - flw_process_mapping                             │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Temporal Server                          │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Workflows                                         │    │
│  │  - KYCOnboardingWorkflow                           │    │
│  │  - DocumentVerificationWorkflow                    │    │
│  │  - AddressUpdateWorkflow                           │    │
│  └────────────────────────────────────────────────────┘    │
│                            │                                │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Activities                                        │    │
│  │  - OCRActivity                                     │    │
│  │  - IDVerificationActivity                          │    │
│  │  - NotificationActivity                            │    │
│  │  - InteractionCallbackActivity                     │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              External Services                              │
│  - OCR Service                                              │
│  - ID Verification Service                                  │
│  - Notification Service                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Workflow Flow

### **Scenario: KYC Onboarding**

```
User                Interaction Service         Temporal            External Services
  │                         │                      │                       │
  │ 1. Start KYC            │                      │                       │
  ├────────────────────────▶│                      │                       │
  │                         │                      │                       │
  │                         │ 2. Create Case       │                       │
  │                         │    Create Interaction│                       │
  │                         │    Save to DB        │                       │
  │                         │                      │                       │
  │                         │ 3. Start Workflow    │                       │
  │                         ├─────────────────────▶│                       │
  │                         │                      │                       │
  │                         │ 4. Save Mapping      │                       │
  │                         │    (case_id ↔       │                       │
  │                         │     workflow_id)     │                       │
  │                         │                      │                       │
  │ 5. Return step config   │                      │                       │
  │◀────────────────────────┤                      │                       │
  │                         │                      │                       │
  │ 6. Submit personal-info │                      │                       │
  ├────────────────────────▶│                      │                       │
  │                         │                      │                       │
  │                         │ 7. Update DB         │                       │
  │                         │    (current_step,    │                       │
  │                         │     audit_trail)     │                       │
  │                         │                      │                       │
  │ 8. Return next step     │                      │                       │
  │◀────────────────────────┤                      │                       │
  │                         │                      │                       │
  │ 9. Submit documents     │                      │                       │
  ├────────────────────────▶│                      │                       │
  │                         │                      │                       │
  │                         │ 10. Signal Workflow  │                       │
  │                         │     (documents ready)│                       │
  │                         ├─────────────────────▶│                       │
  │                         │                      │                       │
  │                         │                      │ 11. Execute OCR      │
  │                         │                      ├──────────────────────▶│
  │                         │                      │                       │
  │                         │                      │ 12. OCR Result       │
  │                         │                      │◀──────────────────────┤
  │                         │                      │                       │
  │                         │                      │ 13. Verify ID        │
  │                         │                      ├──────────────────────▶│
  │                         │                      │                       │
  │                         │                      │ 14. Verification OK  │
  │                         │                      │◀──────────────────────┤
  │                         │                      │                       │
  │                         │ 15. Callback         │                       │
  │                         │     (update status)  │                       │
  │                         │◀─────────────────────┤                       │
  │                         │                      │                       │
  │                         │ 16. Update DB        │                       │
  │                         │     (status=APPROVED)│                       │
  │                         │                      │                       │
  │ 17. Poll status         │                      │                       │
  ├────────────────────────▶│                      │                       │
  │                         │                      │                       │
  │ 18. Return APPROVED     │                      │                       │
  │◀────────────────────────┤                      │                       │
```

---

## 📊 Database Schema Integration

### **flw_process_mapping**

```sql
CREATE TABLE flw_process_mapping (
    id VARCHAR(36) PRIMARY KEY,
    engine_type VARCHAR(50) NOT NULL,              -- 'TEMPORAL'
    process_instance_id VARCHAR(128) NOT NULL,     -- Temporal onboarding ID
    process_definition_key VARCHAR(255) NOT NULL,  -- Workflow name
    business_key VARCHAR(255),                     -- User-defined key
    case_id VARCHAR(36) NOT NULL,                  -- FK to flow_case
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,                   -- RUNNING, COMPLETED, FAILED
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_process_case 
        FOREIGN KEY (case_id) 
        REFERENCES flow_case(id)
);

CREATE INDEX idx_process_mapping_case_id ON flw_process_mapping(case_id);
CREATE INDEX idx_process_mapping_workflow_id ON flw_process_mapping(process_instance_id);
CREATE INDEX idx_process_mapping_status ON flw_process_mapping(status);
```

---

## 🎯 Key Components

### **1. Temporal Configuration**
- `TemporalConfig.java` - Spring configuration for Temporal client
- `WorkflowClientFactory.java` - Factory for creating workflow clients
- `WorkerConfiguration.java` - Worker configuration for different task queues

### **2. Workflows**
- `KYCOnboardingWorkflow.java` - Interface
- `KYCOnboardingWorkflowImpl.java` - Implementation
- `DocumentVerificationWorkflow.java`
- `AddressUpdateWorkflow.java`

### **3. Activities**
- `OCRActivity.java` - Document OCR processing
- `IDVerificationActivity.java` - ID verification with external service
- `NotificationActivity.java` - Send notifications
- `InteractionCallbackActivity.java` - Callback to Interaction Service

### **4. Services**
- `TemporalWorkflowService.java` - Main service for workflow operations
- `ProcessMappingService.java` - Manage process mappings
- `WorkflowCallbackService.java` - Handle workflow callbacks

### **5. DTOs**
- `WorkflowStartRequest.java`
- `WorkflowStatusResponse.java`
- `WorkflowSignalRequest.java`

---

## 🔑 Design Principles

### **1. Separation of Concerns**
- **Interaction Service**: UI/UX state management
- **Temporal Workflows**: Business process orchestration
- **Activities**: External service integration

### **2. Idempotency**
- All workflow activities are idempotent
- Use workflow IDs for deduplication
- Store execution state in database

### **3. Error Handling**
- Retry policies for transient failures
- Dead letter queue for permanent failures
- Compensation workflows for rollback

### **4. Observability**
- Temporal UI for workflow monitoring
- Metrics export to Prometheus
- Distributed tracing with OpenTelemetry

---

## 📝 Implementation Files

See the following files for implementation:

1. **Configuration**
   - `config/TemporalConfig.java`
   - `config/WorkerConfiguration.java`

2. **Workflows**
   - `workflow/KYCOnboardingWorkflow.java`
   - `workflow/KYCOnboardingWorkflowImpl.java`

3. **Activities**
   - `activity/OCRActivity.java`
   - `activity/IDVerificationActivity.java`
   - `activity/InteractionCallbackActivity.java`

4. **Services**
   - `service/TemporalWorkflowService.java`
   - `service/ProcessMappingService.java`

5. **Controllers**
   - `controller/WorkflowController.java`

---

## 🚀 Next Steps

1. Review architecture design
2. Implement Temporal configuration
3. Create workflow definitions
4. Implement activities
5. Integrate with Interaction Service
6. Add monitoring and observability
7. Write tests

---

Ready to implement? Let's start! 🎉
