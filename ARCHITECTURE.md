# Architecture Guide

## System Overview

The Custom Workflow Engine is a state machine-based workflow orchestration system designed to replace Camunda BPMN engines with a lightweight, database-driven alternative. It implements complex business processes like digital banking onboarding through:

- **State Machine**: Database-driven workflow definitions
- **Rule Engine**: Conditional transitions based on workflow data
- **Event-Driven**: Asynchronous processing with guaranteed delivery
- **Enterprise Features**: Monitoring, audit, human tasks, error handling

## Core Components

### 1. State Machine Engine

#### OnboardingInstance
```java
@Entity
@Table(name = "onboarding_instance")
public class OnboardingInstance {
    @Id
    private UUID id;
    private String userId;
    private String currentState;  // Current workflow state
    private String flowVersion;   // Workflow version
    private String status;        // ACTIVE, COMPLETED, FAILED
    @Version
    private Long version;         // Optimistic locking
}
```

#### Transition Table
```java
@Entity
@Table(name = "onboarding_transition")
public class Transition {
    @Id
    private String flowVersion;
    @Id
    private String fromState;
    @Id
    private String action;

    private String toState;
    private Boolean isAsync;
    private String[] allowedActors;
    private String conditionsJson;  // JSON conditions
}
```

### 2. Rule Engine

Evaluates transition conditions against workflow context:

```java
@Service
public class RuleEngine {
    public boolean evaluateCondition(String condition, Map<String, Object> context) {
        // Evaluate conditions like "otp_status == SUCCESS"
    }
}
```

#### State Context
Stores workflow data for rule evaluation:

```java
@Entity
@Table(name = "state_context")
public class StateContext {
    @Id
    private UUID instanceId;
    @Column(columnDefinition = "jsonb")
    private String contextData;  // {"otp_status": "SUCCESS", "ekyc_score": 0.85}
}
```

### 3. Onboarding Engine

Central orchestrator that handles all workflow actions:

```java
@Service
public class OnboardingEngine {
    @Transactional
    public void handle(ActionCommand command) {
        // 1. Idempotency check
        // 2. Load instance with locking
        // 3. Resolve transition with rule evaluation
        // 4. Validate permissions
        // 5. Execute step
        // 6. Update state
        // 7. Audit
    }
}
```

### 4. Step Execution System

Handles business logic execution with retry capabilities:

```java
@Service
public class StepExecutor {
    public boolean execute(OnboardingInstance instance,
                          StepExecution execution,
                          Transition transition) {
        // Execute step with retry logic
    }
}

public interface StepHandler {
    StepResult execute(StepContext context);
}
```

### 5. Event-Driven Architecture

#### Outbox Pattern
Guaranteed event delivery to prevent dual writes:

```java
@Service
public class OutboxService {
    @Transactional
    public void storeEvent(String eventId, String topic,
                          Object payload, String eventType) {
        // Store in outbox table
    }

    @Scheduled
    public void publishPendingEvents() {
        // Publish to Kafka
    }
}
```

#### Kafka Integration
Event schemas for workflow communication:

```json
{
  "eventId": "uuid",
  "eventType": "EKYC_CALLBACK",
  "correlation": {
    "instanceId": "uuid",
    "flowVersion": "v1"
  },
  "payload": {
    "result": "APPROVED",
    "score": 0.87
  }
}
```

### 6. Human Task Management

Manual review and approval workflows:

```java
@Entity
@Table(name = "human_task")
public class HumanTask {
    @Id
    private UUID taskId;
    private UUID instanceId;
    private String taskType;     // AML_REVIEW, EKYC_REVIEW
    private String assignedRole; // RISK_OFFICER
    private String status;       // OPEN, CLAIMED, COMPLETED
}
```

### 7. Monitoring & Observability

#### Metrics Service
```java
@Service
public class WorkflowMetricsService {
    public void recordStateTransition(String from, String to, String action) {
        // Micrometer metrics
    }
}
```

#### Incident Management
```java
@Entity
@Table(name = "incident")
public class Incident {
    private UUID incidentId;
    private String errorCode;
    private String severity;  // LOW, MEDIUM, HIGH, CRITICAL
    private String status;    // OPEN, ACKNOWLEDGED, RESOLVED
}
```

## Data Flow

### Normal Workflow Execution

```
User Action → REST API → OnboardingEngine → TransitionResolver
    ↓              ↓              ↓              ↓
Validate    Idempotency    Rule Eval     State Update
    ↓              ↓              ↓              ↓
Step Exec → Event Publish → DB Update → Response
```

### Async Processing

```
Kafka Event → Consumer → OnboardingEngine → StepExecutor
    ↓              ↓              ↓              ↓
Validate     Transition    Business Logic   State Update
    ↓              ↓              ↓              ↓
Context Update → Event Outbox → History Log → Notification
```

### Error Handling

```
Exception → GlobalHandler → Incident Creation → Alert
    ↓              ↓              ↓              ↓
Correlation ID → Error Response → Retry Logic → Recovery
```

## Database Schema

### Core Tables

```sql
-- Workflow instances
CREATE TABLE onboarding_instance (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    current_state VARCHAR(50) NOT NULL,
    flow_version VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL
);

-- Workflow definitions
CREATE TABLE onboarding_transition (
    flow_version VARCHAR(10),
    from_state VARCHAR(50),
    action VARCHAR(50),
    to_state VARCHAR(50),
    conditions_json JSONB,
    PRIMARY KEY (flow_version, from_state, action)
);

-- Execution tracking
CREATE TABLE step_execution (
    instance_id UUID,
    state VARCHAR(50),
    status VARCHAR(20),
    retry_count INTEGER,
    PRIMARY KEY (instance_id, state)
);

-- Workflow context
CREATE TABLE state_context (
    instance_id UUID PRIMARY KEY,
    context_data JSONB
);

-- Audit trail
CREATE TABLE onboarding_history (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID,
    from_state VARCHAR(50),
    to_state VARCHAR(50),
    action VARCHAR(50),
    result VARCHAR(20)
);
```

### Supporting Tables

```sql
-- Human tasks
CREATE TABLE human_task (
    task_id UUID PRIMARY KEY,
    instance_id UUID,
    task_type VARCHAR(50),
    assigned_role VARCHAR(50),
    status VARCHAR(20)
);

-- Incident management
CREATE TABLE incident (
    incident_id UUID PRIMARY KEY,
    instance_id UUID,
    error_code VARCHAR(50),
    severity VARCHAR(10),
    status VARCHAR(20)
);

-- Outbox for events
CREATE TABLE outbox_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE,
    topic VARCHAR(100),
    event_payload JSONB,
    status VARCHAR(20)
);

-- Metrics storage
CREATE TABLE workflow_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100),
    state VARCHAR(50),
    value BIGINT,
    recorded_at TIMESTAMPTZ
);
```

## Configuration

### Application Properties

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/onboarding
  kafka:
    bootstrap-servers: localhost:9092

workflow:
  retry:
    max-attempts: 3
    backoff-multiplier: 2.0
  timeout:
    default-seconds: 300
  outbox:
    publish-interval: 10000  # 10 seconds

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=onboarding
DB_USER=onboarding
DB_PASSWORD=secret

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=onboarding-engine

# Workflow
WORKFLOW_RETRY_MAX_ATTEMPTS=3
WORKFLOW_TIMEOUT_DEFAULT_SECONDS=300
```

## Security Architecture

### Authentication & Authorization

- JWT-based authentication
- Role-based access control (RBAC)
- API key authentication for system calls

### Data Protection

- PII data encryption at rest
- TLS for data in transit
- Audit logging for all operations

### Access Control

```java
@PreAuthorize("hasRole('ADMIN') or @workflowSecurity.isOwner(#instanceId, authentication.name)")
public ResponseEntity<?> performAction(@PathVariable UUID instanceId) {
    // Implementation
}
```

## Performance Characteristics

### Throughput
- State transitions: 1000+ per second
- API responses: <50ms P95
- Database queries: <10ms average

### Scalability
- Horizontal scaling with database partitioning
- Kafka consumer groups for parallel processing
- Redis caching for hot data

### Reliability
- 99.9% uptime SLA
- Circuit breakers for external services
- Graceful degradation under load

## Deployment Architecture

### Microservices Pattern

```
┌─────────────────┐    ┌─────────────────┐
│  API Gateway    │────│ Workflow Engine │
│  (Kong/Nginx)   │    │                 │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────────────────┘
                  │
         ┌─────────────────┐
         │   PostgreSQL    │
         │   + Redis       │
         └─────────────────┘
                  │
         ┌─────────────────┐
         │     Kafka       │
         │   (Event Bus)   │
         └─────────────────┘
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: workflow-engine
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: workflow-engine
        image: workflow-engine:latest
        env:
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: workflow-config
              key: db.host
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

## Monitoring & Alerting

### Key Metrics

- `workflow_instances_active`: Current active workflows
- `workflow_state_transitions_total`: Transition counters
- `workflow_step_execution_duration`: Step execution time
- `workflow_errors_total`: Error counters by type
- `workflow_human_tasks_pending`: Pending manual tasks

### Alert Rules

```yaml
# Prometheus alerting rules
groups:
- name: workflow.alerts
  rules:
  - alert: HighErrorRate
    expr: rate(workflow_errors_total[5m]) > 0.1
    labels:
      severity: critical
  - alert: PendingHumanTasks
    expr: workflow_human_tasks_pending > 100
    labels:
      severity: warning
```

### Logging

Structured logging with correlation IDs:

```json
{
  "timestamp": "2024-01-06T12:00:00Z",
  "level": "INFO",
  "logger": "com.ngoctran.interactionservice.engine.OnboardingEngine",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "State transitioned",
  "instanceId": "550e8400-e29b-41d4-a716-446655440000",
  "fromState": "OTP_VERIFIED",
  "toState": "PROFILE_COMPLETED",
  "action": "NEXT"
}
```

## Extension Points

### Adding New Workflow States

1. Add state to transition table
2. Implement StepHandler
3. Update conditions if needed
4. Add to metrics collection

### Custom Rule Functions

```java
@Component
public class CustomRuleEngine extends RuleEngine {
    public boolean evaluateCustomRule(String rule, Map<String, Object> context) {
        // Custom rule logic
    }
}
```

### External Service Integration

```java
@Service
public class ExternalServiceClient {
    public StepResult callExternalService(StepContext context) {
        // Integration logic
    }
}
```

This architecture provides a solid foundation for complex workflow orchestration while maintaining simplicity, observability, and extensibility.
