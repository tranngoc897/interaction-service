# Custom Workflow Engine Implementation (No Camunda)

This document describes the implementation of a custom workflow engine for the onboarding process, replacing Camunda with a state machine-based, event-driven architecture as outlined in the provided PDF.

## Architecture Overview

The system implements a **state machine + transition table + event-driven** workflow engine that provides:

- **State Machine**: Finite state machine with defined states and transitions
- **Transition Table**: Configurable workflow definition stored in database
- **Event-Driven**: Async processing with Kafka for external services
- **Retry & Timeout**: Automatic retry with exponential backoff and timeout handling
- **Admin Monitoring**: UI for monitoring, manual actions, and incident management

## Core Components

### 1. Domain Models
- `OnboardingInstance`: Main workflow instance
- `Transition`: Workflow definition (from_state → action → to_state)
- `StepExecution`: Execution tracking with retry logic
- `ProcessedEvent`: Idempotency for actions

### 2. Engine Components
- `OnboardingEngine`: Central workflow controller
- `ActionCommand`: Standardized action representation
- `TransitionResolver`: Resolves valid transitions
- `ActionValidator`: Validates actions and permissions
- `StepExecutor`: Executes business logic with retry handling

### 3. Step Handlers
- `StepHandler`: Interface for business logic execution
- `OtpVerificationHandler`: OTP verification step
- `EkycHandler`: eKYC async request handler

### 4. Event Processing
- `EkycCallbackConsumer`: Kafka consumer for eKYC results
- Event schemas for command/callback/domain events

### 5. Schedulers
- `RetryScheduler`: Automatic retry of failed steps
- `TimeoutScheduler`: Timeout handling for async steps

### 6. APIs
- `OnboardingController`: User-facing onboarding APIs
- REST endpoints for start, status, actions, and available actions

## Database Schema

### Key Tables
- `onboarding_instance`: Workflow instances
- `onboarding_transition`: Workflow definition
- `onboarding_history`: Audit trail
- `step_execution`: Execution tracking
- `processed_event`: Idempotency
- `human_task`: Manual review tasks
- `incident`: Error management

## Workflow States

```
PHONE_ENTERED → OTP_VERIFIED → PROFILE_COMPLETED → DOC_UPLOADED
    ↓
EKYC_PENDING → EKYC_APPROVED → AML_PENDING → AML_CLEARED
    ↓              ↓               ↓
EKYC_REJECTED  EKYC_TIMEOUT   AML_REJECTED
                              AML_TIMEOUT
    ↓
ACCOUNT_CREATED → COMPLETED
```

## Key Features Implemented

### ✅ State Machine
- Configurable transitions in database
- Versioned workflows
- State validation

### ✅ Event-Driven Architecture
- Kafka integration for async processing
- Command/callback event patterns
- Idempotent event processing

### ✅ Retry & Error Handling
- Exponential backoff retry
- Configurable max retries
- Error classification (TRANSIENT/BUSINESS/SYSTEM)

### ✅ Timeout Management
- Automatic timeout for async steps
- Configurable timeout periods
- Timeout action processing

### ✅ Admin Monitoring
- Instance status tracking
- Action history
- Manual intervention capabilities

### ✅ Scalability
- Database-level locking
- Idempotent operations
- Horizontal scaling ready

## Configuration

### Application Properties
```yaml
workflow:
  timeout:
    ekyc: 300  # 5 minutes
    aml: 120   # 2 minutes
  retry:
    max-attempts: 3

scheduler:
  retry:
    interval: 30000  # 30 seconds
  timeout:
    interval: 60000  # 60 seconds
```

### Transition Data
Sample transitions defined in `data.sql`:
- User actions: NEXT, VERIFY_OTP
- System actions: RETRY, TIMEOUT
- Async callbacks: EKYC_CALLBACK_OK, AML_CALLBACK_OK

## API Endpoints

### User APIs
- `POST /api/onboarding/start` - Start onboarding
- `GET /api/onboarding/{id}/status` - Get current status
- `POST /api/onboarding/{id}/action` - Perform action
- `GET /api/onboarding/{id}/actions` - Get available actions

### Admin APIs (Future)
- Dashboard and monitoring
- Manual actions and overrides
- Incident management

## Kafka Topics

- `ekyc-request`: eKYC verification requests
- `ekyc-callback`: eKYC verification results
- `aml-request`: AML check requests
- `aml-callback`: AML check results
- `onboarding-events`: Domain events

## Benefits Over Camunda

1. **Performance**: Lightweight, no BPMN parsing overhead
2. **Control**: Full control over workflow logic
3. **Cost**: No licensing fees
4. **Simplicity**: Direct state machine vs complex BPMN
5. **Observability**: Built-in metrics and monitoring
6. **Scalability**: Database-backed, horizontally scalable

## Implementation Status

### ✅ Completed
- Core domain models and repositories
- OnboardingEngine with action processing
- Step execution with retry logic
- Kafka event processing
- Scheduler for retry and timeout
- Basic user APIs
- Database schema and sample data
- Configuration setup

### 🚧 In Progress / Future
- Admin monitoring UI
- Human task management
- Incident management
- DLQ handling
- Comprehensive metrics
- Integration tests

## Running the Application

1. **Database Setup**:
   ```sql
   -- Run init.sql and data.sql
   ```

2. **Kafka Setup**:
   ```bash
   # Start Kafka and create topics
   ```

3. **Application Start**:
   ```bash
   mvn spring-boot:run
   ```

4. **Test API**:
   ```bash
   # Start onboarding
   curl -X POST "http://localhost:8081/api/onboarding/start?userId=testuser"

   # Check status
   curl "http://localhost:8081/api/onboarding/{id}/status"

   # Perform action
   curl -X POST "http://localhost:8081/api/onboarding/{id}/action" \
        -H "Content-Type: application/json" \
        -d '{"action": "NEXT"}'
   ```

## Comparison with Original Camunda Implementation

| Feature | Camunda | Custom Engine |
|---------|---------|---------------|
| State Management | BPMN Process | Database State Machine |
| External Tasks | External Task Client | Kafka Consumers |
| User Tasks | BPMN User Tasks | Human Task Table |
| History | Built-in | Custom History Table |
| Monitoring | Cockpit | Custom Admin UI |
| Scalability | Good | Excellent |
| Complexity | High | Medium |
| Customization | Limited | Full Control |

## Next Steps

1. **Complete Admin UI**: Build monitoring dashboard
2. **Add Human Tasks**: Implement manual review workflow
3. **Incident Management**: Error tracking and resolution
4. **Testing**: Comprehensive integration tests
5. **Documentation**: API docs and operational guides
6. **Production Deployment**: K8s configuration and monitoring

This implementation provides a solid foundation for a production-ready workflow engine that matches Camunda's capabilities while being more lightweight and controllable.
