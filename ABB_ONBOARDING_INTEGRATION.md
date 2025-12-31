# ABB Onboarding Patterns Integration

This document describes the integration of ABB Onboarding architectural patterns into the Interaction Service.

## Overview

The Interaction Service has been enhanced with ABB Onboarding patterns to provide:

1. **JSON Schema-based Case Definitions** - Dynamic case data management
2. **Interaction Flow Orchestration** - Declarative step-based workflows
3. **Epic & Milestone Tracking** - Progress monitoring and event publishing
4. **Event-driven Architecture** - Decoupled component communication

## Architecture Components

### 1. Case Definition Framework

**Purpose**: Define case data structures using JSON Schema instead of hard-coded classes.

**Key Classes**:
- `CaseDefinitionEntity` - Stores case definition metadata and JSON schema
- `CaseDefinitionService` - Manages case definitions and validation
- `CaseDefinitionRepository` - Data access layer

**Usage**:
```java
// Create a case definition
String schemaJson = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", ... }";
CaseDefinitionEntity caseDef = caseDefinitionService.createCaseDefinition(
    "onboarding-case", "Retail Onboarding", "Case definition for retail onboarding",
    schemaJson, "system"
);

// Validate case data
caseDefinitionService.validateCaseData("onboarding-case", caseDataJson);
```

### 2. Interaction Flow Framework

**Purpose**: Define workflow steps and transitions declaratively using JSON.

**Key Classes**:
- `InteractionDefinitionEntity` - Stores flow definition metadata and JSON flow
- `InteractionDefinitionService` - Manages flows and step transitions
- `InteractionDefinitionRepository` - Data access layer

**Flow Structure**:
```json
{
  "key": "onboarding-flow",
  "steps": [
    {
      "name": "init",
      "entryPoint": true,
      "handlers": [
        {
          "name": "validate-initial-data",
          "transitions": [
            {
              "condition": "success()",
              "step": "next-step"
            }
          ]
        }
      ]
    }
  ]
}
```

### 3. Epic & Milestone System

**Purpose**: Track major milestones and progress within business processes.

**Key Classes**:
- `EpicEntity` - Defines epic categories (e.g., "data-gathering", "verification")
- `MilestoneEntity` - Tracks specific milestones within epics
- `EpicService` & `MilestoneService` - Management services

**Usage**:
```java
// Start a milestone
milestoneService.startMilestone(caseId, "data-gathering", "personal-info-collected",
    "Personal Information Collected", "User submitted personal details", metadata);

// Complete a milestone
milestoneService.completeMilestone(caseId, "personal-info-collected", completionData);
```

## Database Schema

**Enhanced Existing Tables** with ABB patterns (instead of creating new tables):

### `flw_case` (Enhanced)
Added ABB onboarding columns:
- `resume_token` - Secure token for workflow resumption
- `workflow_state` - **Workflow execution state** (current step, variables, context for resume/pause)
- `epic_data` - JSON epic/milestone progress tracking
- `compliance_status` - JSON compliance check results and status
- `joint_account_data` - JSON joint account relationship data
- `bpmn_process_id` - BPMN process instance ID
- `expires_at` - Expiration timestamp for paused workflow cleanup

### **Key Distinction: caseData vs workflowState**

| Field | Purpose | Content | When Updated |
|-------|---------|---------|--------------|
| **`caseData`** | **Business Data** | Customer info, application data, form submissions | User interactions, data collection steps |
| **`workflowState`** | **Execution State** | Current step, execution context, resume data | Workflow engine state changes, pause/resume operations |

**Example:**

**caseData - Business content:**
```json
{
  "customerId": "12345",
  "fullName": "John Doe",
  "email": "john@example.com",
  "applicationData": {},
  "documents": []
}
```

**workflowState - Execution state:**
```json
{
  "currentStep": "document-verification",
  "stepContext": {"attempts": 2, "lastError": "timeout"},
  "executionVariables": {"approved": false},
  "resumePoint": "awaiting-manual-review"
}
```

### `flw_case_def` (Enhanced & Unified)
**Merged InteractionDefinitionEntity functionality** - Now single source for all case/interaction definitions:

**ABB Complex Flows:**
- `interaction_flow_json` - JSON enterprise interaction flows (handlers, transitions, post-executions)

**Simple UI Flows:**
- `simple_steps_json` - JSON simple UI step flows (from old InteractionDefinitionEntity)

**Metadata:**
- `name` - Case definition display name
- `description` - Case definition description
- `status` - ACTIVE/DRAFT/DEPRECATED
- `created_at/updated_at` - Audit timestamps
- `created_by/updated_by` - Audit users

**Migration:** Old `InteractionDefinitionEntity` merged into `CaseDefinitionEntity` for unified definition management.

### New Tables (Supporting)
- `compliance_checks` - Detailed compliance check audit trail
- `joint_accounts` - Joint account relationship management

Run the migration script: `src/main/resources/db/workflow_history_migration.sql`

## Sample Implementation

### Case Definition (JSON Schema)
See: `src/main/resources/case-definitions/onboarding.json`

### Interaction Flow Definition
See: `src/main/resources/interaction-definitions/onboarding-flow.json`

## Integration with Existing Workflows

### Current Temporal Workflows
The existing `OnboardingWorkflow` can be enhanced to use the new patterns:

1. **Case Data Management**: Use case definitions for dynamic data validation
2. **Flow Orchestration**: Replace hard-coded steps with interaction flow definitions
3. **Progress Tracking**: Add milestone tracking throughout the workflow
4. **Event Publishing**: Publish milestone events for external system integration

### Example Integration
```java
@Service
public class EnhancedOnboardingWorkflow {

    @Autowired
    private CaseDefinitionService caseDefinitionService;

    @Autowired
    private InteractionDefinitionService interactionDefinitionService;

    @Autowired
    private MilestoneService milestoneService;

    public void executeOnboarding(String caseId, Map<String, Object> data) {
        // Validate case data against schema
        caseDefinitionService.validateCaseData("onboarding-case", objectMapper.writeValueAsString(data));

        // Get interaction flow
        InteractionDefinitionEntity flow = interactionDefinitionService
            .getActiveInteractionDefinition("onboarding-flow").orElseThrow();

        // Start milestone tracking
        milestoneService.startMilestone(caseId, "data-gathering", "init",
            "Onboarding Started", "Initial onboarding process started", data);

        // Execute workflow steps based on flow definition
        executeFlowSteps(flow, caseId, data);
    }
}
```

## Benefits Achieved

1. **Dynamic Configuration**: Change workflows without code deployment
2. **Better Monitoring**: Real-time progress tracking with milestones
3. **Event-driven**: Loose coupling between components
4. **Compliance Ready**: Built-in audit trails and milestone tracking
5. **Scalable**: Easy to add new case types and flows
6. **Maintainable**: Declarative definitions reduce code complexity

## Next Steps

1. **BPMN Integration**: Add Camunda/Flowable for complex process orchestration
2. **DMN Engine**: Implement decision table support for business rules
3. **Resume/Pause**: Add workflow state persistence for resumability
4. **Joint Accounts**: Implement parallel processing for co-applicants
5. **Compliance Features**: Enhanced AML/KYC integration
6. **Event Bus**: Implement proper event publishing (Kafka/Spring Events)

## Migration Path

1. **Phase 1**: Implement case definitions for existing workflows
2. **Phase 2**: Add interaction flows for step orchestration
3. **Phase 3**: Integrate milestone tracking
4. **Phase 4**: Add event-driven architecture
5. **Phase 5**: Implement advanced features (BPMN, DMN, etc.)

This integration brings the robustness and flexibility of ABB Onboarding patterns to the Interaction Service while maintaining compatibility with existing Temporal workflows.
